package com.minepiece.essentials.telemetry;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ModConstants;
import com.minepiece.essentials.ServerDetector;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Télémétrie anonyme : combien de joueurs utilisent le mod, sur quelles versions,
 * et quelles features leur servent. Aucune donnée identifiante ne part (la
 * garantie est portée par la liste blanche de {@link TelemetryEvent}).
 *
 * <p>Désactivable dans l'éditeur de HUD (touche K). Toute méthode publique est
 * sans effet quand l'opt-out est actif, pour que les points d'appel dans le
 * reste du mod restent de simples one-liners.
 *
 * <p>Ne doit JAMAIS dégrader le jeu : envoi sur un thread daemon, timeouts
 * courts, échec silencieux, aucun retry.
 */
public final class Telemetry {

    private static final String API_KEY = "phc_nfmqDHNYpEDmGAvnb62NS8SodTa6W73QZAbjSFkSVbRj";
    private static final String BATCH_URL = "https://eu.i.posthog.com/batch/";
    private static final long FLUSH_INTERVAL_MS = 60_000;
    /** Délai après la connexion avant d'émettre mp_session_start : ServerDetector a besoin de se stabiliser. */
    private static final long SESSION_DELAY_MS = 5_000;

    private static final TelemetryBuffer BUFFER = new TelemetryBuffer();
    private static TelemetryId identity;
    private static HttpClient http;

    private static long joinedAt = 0;
    private static boolean sessionSent = false;
    private static long lastFlush = 0;

    private Telemetry() {}

    public static boolean isEnabled() {
        var mgr = MinepieceEssentialsClient.getInstance().getConfigManager();
        return mgr != null && mgr.config().telemetryEnabled;
    }

    public static void setEnabled(boolean enabled) {
        var mgr = MinepieceEssentialsClient.getInstance().getConfigManager();
        mgr.config().telemetryEnabled = enabled;
        mgr.save();
        if (!enabled) BUFFER.drain(); // on jette ce qui n'est pas encore parti
    }

    public static void init() {
        try {
            identity = TelemetryId.loadOrCreate(
                    MinepieceEssentialsClient.getInstance().getConfigManager().telemetryFile());
            http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        } catch (Exception e) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] init ignoré : {}", e.toString());
        }
    }

    public static void onJoinedServer() {
        joinedAt = System.currentTimeMillis();
        sessionSent = false;
        BUFFER.resetSession();
    }

    public static void onDisconnected() {
        flush();
    }

    /** Vrai si le message d'information a déjà été affiché sur cette installation. */
    public static boolean wasAnnounced() {
        return identity == null || identity.announced();
    }

    public static void markAnnounced() {
        if (identity == null) return;
        identity.markAnnounced(
                MinepieceEssentialsClient.getInstance().getConfigManager().telemetryFile());
        // relecture pour que wasAnnounced() reflète l'état persisté
        identity = TelemetryId.loadOrCreate(
                MinepieceEssentialsClient.getInstance().getConfigManager().telemetryFile());
    }

    /** Signale l'usage d'une feature. Au plus un événement par feature et par session. */
    public static void feature(String name) {
        if (!isEnabled() || identity == null) return;
        if (!BUFFER.markFeatureSeen(name)) return;
        try {
            BUFFER.add(TelemetryEvent.of("mp_feature_used", Map.of("feature", name)));
        } catch (IllegalArgumentException e) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] feature rejetée : {}", e.getMessage());
        }
    }

    /** Appelé chaque tick client : émet mp_session_start puis vide la file périodiquement. */
    public static void tick() {
        if (!isEnabled() || identity == null) return;

        long now = System.currentTimeMillis();
        if (!sessionSent && joinedAt > 0 && now - joinedAt >= SESSION_DELAY_MS) {
            sessionSent = true;
            sendSessionStart();
        }
        if (now - lastFlush >= FLUSH_INTERVAL_MS) {
            lastFlush = now;
            flush();
        }
    }

    private static void sendSessionStart() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("mod_version", modVersion());
        props.put("mc_version", FabricLoader.getInstance().getModContainer("minecraft")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
        props.put("loader_version", FabricLoader.getInstance().getModContainer("fabricloader")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
        props.put("java_version", System.getProperty("java.version", "unknown"));
        props.put("os", System.getProperty("os.name", "unknown").toLowerCase().split(" ")[0]);
        props.put("client_language", MinecraftClient.getInstance().options.language);
        props.put("on_minepiece", ServerDetector.isOnMinePiece());
        try {
            BUFFER.add(TelemetryEvent.of("mp_session_start", props));
        } catch (IllegalArgumentException e) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] session rejetée : {}", e.getMessage());
        }
        flush();
    }

    private static String modVersion() {
        return FabricLoader.getInstance().getModContainer(ModConstants.MOD_ID)
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    /** Envoie ce qui est en attente, sur un thread daemon. Échec = perte silencieuse, jamais de retry. */
    private static void flush() {
        if (!isEnabled() || identity == null || http == null) return;
        List<TelemetryEvent> events = BUFFER.drain();
        if (events.isEmpty()) return;

        String body = TelemetryEvent.batchJson(API_KEY, identity.installId(), events);
        Thread t = new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(BATCH_URL))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", ModConstants.MOD_ID)
                        .timeout(Duration.ofSeconds(8))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                http.send(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                MinepieceEssentialsClient.LOGGER.debug("[Telemetry] envoi ignoré : {}", e.toString());
            }
        }, "minepiece-essentials-telemetry");
        t.setDaemon(true);
        t.start();
    }
}
