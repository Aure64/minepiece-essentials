package com.minepiece.essentials.telemetry;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ModConstants;
import com.minepiece.essentials.ServerDetector;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
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
    /** Repli si ServerDetector ne détecte jamais MinePiece (autre serveur, ou détection
     * qui échoue) : on envoie quand même mp_session_start, avec on_minepiece=false,
     * pour compter la session une seule fois. Assez long pour laisser le temps au
     * rechargement de resource pack (queue + pack serveur) de se terminer sur MinePiece. */
    private static final long SESSION_FALLBACK_MS = 60_000;

    /** En dessous de ce délai depuis le dernier mp_session_start, un nouveau JOIN
     * (hop de proxy, reconnexion) n'est pas compté comme une nouvelle session. */
    private static final long SESSION_DEDUP_MS = 15 * 60_000;

    private static final TelemetryBuffer BUFFER = new TelemetryBuffer();
    private static TelemetryId identity;
    private static HttpClient http;

    private static long joinedAt = 0;
    private static boolean sessionSent = false;
    private static long lastFlush = 0;
    private static long lastSessionSentAt = 0;

    private Telemetry() {}

    public static boolean isEnabled() {
        var mgr = MinepieceEssentialsClient.getInstance().getConfigManager();
        return mgr != null && mgr.config().telemetryEnabled;
    }

    public static void setEnabled(boolean enabled) {
        try {
            var mgr = MinepieceEssentialsClient.getInstance().getConfigManager();
            if (mgr == null) return;
            mgr.config().telemetryEnabled = enabled;
            mgr.save();
            if (!enabled) {
                BUFFER.drain(); // on jette ce qui n'est pas encore parti
            } else if (identity == null) {
                // Le joueur réactive dans la même session : init() avait sauté la
                // création d'identité à cause de l'opt-out. On la fait maintenant,
                // sans quoi plus rien ne repartirait avant un redémarrage.
                identity = TelemetryId.loadOrCreate(mgr.telemetryFile());
                if (http == null) {
                    http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
                }
            }
        } catch (Throwable t) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] setEnabled ignoré : {}", t.toString());
        }
    }

    public static void init() {
        try {
            if (!isEnabled()) return; // pas de création d'UUID pour un joueur qui a déjà refusé
            identity = TelemetryId.loadOrCreate(
                    MinepieceEssentialsClient.getInstance().getConfigManager().telemetryFile());
            http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        } catch (Throwable t) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] init ignoré : {}", t.toString());
        }
    }

    public static void onJoinedServer() {
        try {
            joinedAt = System.currentTimeMillis();
            sessionSent = false;
            // BUFFER.resetSession() n'a lieu que dans sendSessionStart(), et seulement
            // si la session est réellement comptée : sinon un hop de proxy réarmerait
            // les événements mp_feature_used déjà envoyés pour rien.
        } catch (Throwable t) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] onJoinedServer ignoré : {}", t.toString());
        }
    }

    public static void onDisconnected() {
        try {
            flush();
        } catch (Throwable t) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] onDisconnected ignoré : {}", t.toString());
        }
    }

    /** Vrai si le message d'information a déjà été affiché sur cette installation. */
    public static boolean wasAnnounced() {
        return identity == null || identity.announced();
    }

    public static void markAnnounced() {
        try {
            if (identity == null) return;
            identity.markAnnounced(
                    MinepieceEssentialsClient.getInstance().getConfigManager().telemetryFile());
            // relecture pour que wasAnnounced() reflète l'état persisté
            identity = TelemetryId.loadOrCreate(
                    MinepieceEssentialsClient.getInstance().getConfigManager().telemetryFile());
        } catch (Throwable t) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] markAnnounced ignoré : {}", t.toString());
        }
    }

    /** Signale l'usage d'une feature. Au plus un événement par feature et par session. */
    public static void feature(String name) {
        try {
            if (!isEnabled() || identity == null) return;
            if (!BUFFER.markFeatureSeen(name)) return;
            BUFFER.add(TelemetryEvent.of("mp_feature_used", Map.of("feature", name)));
        } catch (Throwable t) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] feature ignorée : {}", t.toString());
        }
    }

    /** Appelé chaque tick client : émet mp_session_start puis vide la file périodiquement. */
    public static void tick() {
        try {
            if (!isEnabled() || identity == null) return;

            long now = System.currentTimeMillis();
            if (!sessionSent && joinedAt > 0) {
                // On envoie dès que ServerDetector confirme MinePiece (souvent bien avant
                // le repli), sinon on retombe sur le délai fixe pour ne pas perdre les
                // joueurs d'autres serveurs. Un délai fixe seul est faux ici : la queue
                // et le resource pack serveur de MinePiece rechargent le monde (world
                // devient temporairement null) et peuvent traverser tout délai court.
                boolean onMinePiece = ServerDetector.isOnMinePiece();
                if (onMinePiece) {
                    sessionSent = true;
                    sendSessionStart(true);
                } else if (now - joinedAt >= SESSION_FALLBACK_MS) {
                    sessionSent = true;
                    sendSessionStart(false);
                }
            }
            if (now - lastFlush >= FLUSH_INTERVAL_MS) {
                lastFlush = now;
                flush();
            }
        } catch (Throwable t) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] tick ignoré : {}", t.toString());
        }
    }

    private static void sendSessionStart(boolean onMinePiece) {
        long now = System.currentTimeMillis();
        if (lastSessionSentAt != 0 && now - lastSessionSentAt < SESSION_DEDUP_MS) {
            // Hop de proxy / reconnexion rapprochée : ni nouvelle session comptée,
            // ni réarmement des mp_feature_used déjà envoyés sur cette session.
            return;
        }
        lastSessionSentAt = now;
        BUFFER.resetSession();

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("mod_version", modVersion());
        props.put("mc_version", FabricLoader.getInstance().getModContainer("minecraft")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
        props.put("loader_version", FabricLoader.getInstance().getModContainer("fabricloader")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
        props.put("java_version", System.getProperty("java.version", "unknown"));
        props.put("os", System.getProperty("os.name", "unknown").toLowerCase().split(" ")[0]);
        props.put("client_language", Minecraft.getInstance().options.languageCode);
        props.put("on_minepiece", onMinePiece);
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
