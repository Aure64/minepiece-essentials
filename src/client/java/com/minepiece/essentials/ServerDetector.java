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

    private ServerDetector() {}

    public static boolean isOnMinePiece() {
        boolean result = detect();
        if (result != lastState) {
            lastState = result;
            MinepieceEssentialsClient.LOGGER.info("[ServerDetector] {} — {}",
                result ? "active" : "inactive", lastReason);
        }
        return result;
    }

    private static boolean detect() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            lastReason = "no world";
            return false;
        }

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
