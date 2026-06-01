package com.minepiece.essentials.update;

import com.google.gson.JsonObject;
import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ModConstants;
import com.minepiece.essentials.util.JsonHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Checks GitHub for a newer release on startup (off-thread) and, once the player
 * is in game, prints a one-time clickable chat notice if an update is available.
 *
 * <p>Deliberately <b>not</b> gated to MinePiece: a player whose server detection
 * is failing still gets told to update — which is exactly when they need it.
 */
public final class UpdateChecker {

    private static final String API =
        "https://api.github.com/repos/Aure64/minepiece-essentials/releases/latest";
    private static final String RELEASES_URL =
        "https://github.com/Aure64/minepiece-essentials/releases/latest";

    private static volatile String latestVersion; // e.g. "1.2.0"; null if unknown
    private static boolean notified = false;

    private UpdateChecker() {}

    public static void init() {
        Thread t = new Thread(UpdateChecker::fetch, "minepiece-essentials-update-check");
        t.setDaemon(true);
        t.start();
    }

    private static void fetch() {
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(API))
                .header("User-Agent", ModConstants.MOD_ID)
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(8)).GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return;
            JsonObject json = JsonHelper.gson().fromJson(resp.body(), JsonObject.class);
            if (json == null || !json.has("tag_name")) return;
            String tag = json.get("tag_name").getAsString().trim();
            if (tag.regionMatches(true, 0, "v", 0, 1)) tag = tag.substring(1);
            latestVersion = tag;
        } catch (Exception e) {
            MinepieceEssentialsClient.LOGGER.debug("[Update] check skipped: {}", e.toString());
        }
    }

    /** Call each client tick; prints the notice once, when an update exists and the player is in game. */
    public static void tickNotify() {
        if (notified || latestVersion == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        notified = true;
        String current = currentVersion();
        if (!isNewer(latestVersion, current)) return;

        client.player.sendMessage(Text.literal("[Minepiece Essentials] ").withColor(0xF0A857)
            .append(Text.literal("Mise à jour disponible : ").withColor(0xFFFFFF))
            .append(Text.literal("v" + latestVersion).withColor(0x7CFC55))
            .append(Text.literal("  (tu as v" + current + ")").withColor(0x999999)), false);

        Text link = Text.literal(RELEASES_URL).styled(s -> s
            .withColor(TextColor.fromRgb(0x55AAFF))
            .withUnderline(true)
            .withClickEvent(new ClickEvent.OpenUrl(URI.create(RELEASES_URL))));
        client.player.sendMessage(
            Text.literal("Télécharger : ").withColor(0xCCCCCC).append(link), false);
    }

    private static String currentVersion() {
        return FabricLoader.getInstance().getModContainer(ModConstants.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    /** True if {@code latest} is a strictly higher version than {@code current} (build suffixes ignored). */
    static boolean isNewer(String latest, String current) {
        int[] a = parse(latest);
        int[] b = parse(current);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int[] parse(String version) {
        String[] parts = version.split("[.+\\-]");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }
}
