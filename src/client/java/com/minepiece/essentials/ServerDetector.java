package com.minepiece.essentials;

import com.minepiece.essentials.island.Island;
import com.minepiece.essentials.island.IslandDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Decides whether the mod's features should be active (i.e. the player is on the
 * MinePiece server).
 *
 * <p>Detection uses several independent signals so it survives different join
 * methods and launchers (notably Lunar Client + Direct Connect, where the saved
 * server entry is {@code null}):
 * <ol>
 *   <li>a manual config override ({@code forceMinePieceDetection});</li>
 *   <li>the saved-server address containing "minepiece";</li>
 *   <li>the live connection hostname containing "minepiece" (covers Direct Connect);</li>
 *   <li>a MinePiece island detected from the boss bar — works regardless of how
 *       the player connected.</li>
 * </ol>
 */
public final class ServerDetector {

    private static boolean lastState = false;
    private static String lastReason = "";

    // Once any signal confirms MinePiece on a connection, stay active until the
    // next join/disconnect. This survives the personal island (/is), where the
    // island boss bar disappears and players who joined via a non-"minepiece"
    // host would otherwise have the whole mod switch off.
    private static boolean latched = false;

    // isOnMinePiece() is polled every render frame AND every tick; detect() does
    // string work (toLowerCase, hostname lookup). The answer only changes on
    // connect/disconnect/island-change, so cache it for a short window — this
    // cuts the per-frame allocations without any noticeable detection lag.
    private static final long CACHE_TTL_MS = 250;
    private static boolean cachedResult = false;
    // 0 (not Long.MIN_VALUE) so the first `now - cachedAt` can't overflow — with
    // MIN_VALUE it wrapped negative, stayed < TTL forever and never recomputed.
    private static long cachedAt = 0;

    private ServerDetector() {}

    public static boolean isOnMinePiece() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < CACHE_TTL_MS) {
            return cachedResult;
        }
        cachedAt = now;

        boolean result = detect();
        if (result != lastState) {
            lastState = result;
            MinepieceEssentialsClient.LOGGER.info("[ServerDetector] {} — {}",
                result ? "active" : "inactive", lastReason);
        }
        cachedResult = result;
        return result;
    }

    /** Clears the per-connection latch + cache. Call on every server join. */
    public static void reset() {
        latched = false;
        cachedAt = 0;
        cachedResult = false;
        lastState = false;
    }

    private static boolean detect() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            lastReason = "no world";
            return false;
        }

        // Stay active once confirmed this connection (survives /is, where the
        // island boss bar disappears).
        if (latched) {
            lastReason = "latched";
            return true;
        }

        boolean ok = detectSignals(client);
        if (ok) latched = true;
        return ok;
    }

    private static boolean detectSignals(MinecraftClient client) {
        MinepieceEssentialsClient mod = MinepieceEssentialsClient.getInstance();
        if (mod != null && mod.getConfigManager() != null
                && mod.getConfigManager().config().forceMinePieceDetection) {
            lastReason = "forced by config";
            return true;
        }

        ServerInfo info = client.getCurrentServerEntry();
        if (info != null && info.address != null && info.address.toLowerCase().contains("minepiece")) {
            lastReason = "address " + info.address;
            return true;
        }

        String connHost = connectionHost(client);
        if (connHost != null && connHost.contains("minepiece")) {
            lastReason = "connection " + connHost;
            return true;
        }

        if (IslandDetector.getInstance().getCurrentIsland() != Island.UNKNOWN) {
            lastReason = "island detected";
            return true;
        }

        lastReason = info != null && info.address != null
            ? "address " + info.address
            : (connHost != null ? "connection " + connHost : "no server entry");
        return false;
    }

    /** The hostname of the live connection (lower-cased), or null. Covers Direct Connect. */
    private static String connectionHost(MinecraftClient client) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) return null;
        try {
            SocketAddress address = handler.getConnection().getAddress();
            if (address instanceof InetSocketAddress isa) {
                return isa.getHostString().toLowerCase();
            }
        } catch (Exception ignored) {
            // some connection types (e.g. local) don't expose an inet address
        }
        return null;
    }
}
