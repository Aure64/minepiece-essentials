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
 *   <li>le pied de page (footer) / en-tête (header) du tab-list contenant
 *       "minepiece" — visible partout, y compris sur l'île perso (/is), où la
 *       boss bar de l'île disparaît (couvre Lunar Client, qui expose une IP
 *       plutôt qu'un hostname et un {@code getCurrentServerEntry()} nul);</li>
 *   <li>a MinePiece island detected from the boss bar — works regardless of how
 *       the player connected.</li>
 * </ol>
 */
public final class ServerDetector {

    private static boolean lastState = false;
    private static String lastReason = "";

    // Dernier header/footer du tab-list reçus via PlayerListHeaderS2CPacket
    // (voir ClientPlayNetworkHandlerMixin#onPlayerListHeader). Lecture seule :
    // on ne fait que stocker ce que le serveur a déjà envoyé.
    private static String lastTabHeader = null;
    private static String lastTabFooter = null;

    // Garantit un seul log de diagnostic par connexion, même si detect() est
    // évalué à chaque frame/tick.
    private static boolean diagnosticLogged = false;

    // Garantit un seul log "tab-list reçu" par connexion, même si le serveur
    // renvoie plusieurs fois le PlayerListHeaderS2CPacket. Sert à prouver que
    // le mixin capture bien le paquet, indépendamment de l'ordre des signaux
    // dans detectSignals() (sur la machine de l'auteur, le signal "address"
    // verrouille la détection avant que le tab-list ne soit consulté, donc le
    // diagnostic existant ne prouve jamais que ce chemin fonctionne).
    private static boolean tabListReceptionLogged = false;

    // Garantit un seul log "tab-list correspond" par connexion : se déclenche
    // la première fois que le tab-list bascule à minepiece=true, même si ce
    // n'est pas au premier paquet (certains serveurs envoient un tab-list vide
    // à la connexion puis le peuplent quelques secondes plus tard).
    private static boolean tabListMatchLogged = false;

    // Horodatage de la première évaluation avec un monde chargé (0 = pas encore
    // vu). Sert de référence pour la période de grâce du diagnostic : les
    // signaux (notamment le tab-list et la boss bar) peuvent mettre jusqu'à
    // quelques secondes à arriver après le chargement du monde, donc logguer
    // au tout premier tick donnerait un faux négatif systématique.
    private static long worldSeenAt = 0;
    private static final long DIAGNOSTIC_GRACE_MS = 5000;

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
        lastTabHeader = null;
        lastTabFooter = null;
        diagnosticLogged = false;
        worldSeenAt = 0;
        tabListReceptionLogged = false;
        tabListMatchLogged = false;
    }

    /**
     * Appelé par {@code ClientPlayNetworkHandlerMixin} à chaque réception d'un
     * {@code PlayerListHeaderS2CPacket}. Stocke simplement le texte déjà envoyé
     * par le serveur — aucune action, aucun envoi.
     */
    public static void onTabListHeaderFooter(String header, String footer) {
        lastTabHeader = header;
        lastTabFooter = footer;

        // Preuve indépendante que le paquet arrive bien et que le mixin se
        // déclenche, sans dépendre de l'ordre d'évaluation des signaux dans
        // detectSignals(). Une seule ligne par connexion (le paquet peut être
        // renvoyé plusieurs fois par le serveur) ; le texte brut n'est jamais
        // loggé (peut contenir des pseudos et du texte serveur).
        boolean matches = tabListMatches(header, footer);

        if (!tabListReceptionLogged) {
            tabListReceptionLogged = true;
            boolean headerEmpty = header == null || header.isEmpty();
            boolean footerEmpty = footer == null || footer.isEmpty();
            MinepieceEssentialsClient.LOGGER.info(
                "[ServerDetector] tab-list reçu — minepiece={} (header vide={}, footer vide={})",
                matches, headerEmpty, footerEmpty);
        }

        // Log séparé, une seule fois par connexion, la première fois que le
        // tab-list bascule à "true" — permet de distinguer "jamais peuplé" de
        // "peuplé après N secondes" sans dépendre du premier paquet.
        if (matches && !tabListMatchLogged) {
            tabListMatchLogged = true;
            MinepieceEssentialsClient.LOGGER.info("[ServerDetector] tab-list correspond — minepiece=true");
        }
    }

    private static boolean detect() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            lastReason = "no world";
            return false;
        }
        if (worldSeenAt == 0) {
            worldSeenAt = System.currentTimeMillis();
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
        boolean forced = mod != null && mod.getConfigManager() != null
                && mod.getConfigManager().config().forceMinePieceDetection;

        ServerInfo info = client.getCurrentServerEntry();
        boolean addressMatch = info != null && info.address != null && info.address.toLowerCase().contains("minepiece");

        String connHost = connectionHost(client);
        boolean connectionMatch = connHost != null && connHost.contains("minepiece");

        boolean tabListMatch = tabListMatches(lastTabHeader, lastTabFooter);

        boolean islandMatch = IslandDetector.getInstance().getCurrentIsland() != Island.UNKNOWN;

        // On logue au premier moment où la réponse est significative : soit dès
        // qu'un signal réussit (état au moment du succès), soit une fois la
        // période de grâce écoulée si la détection échoue toujours (état final).
        // Logguer dès le premier tick donnerait un faux négatif systématique,
        // les signaux tab-list/boss bar n'étant pas forcément déjà arrivés.
        boolean anyMatch = forced || addressMatch || connectionMatch || tabListMatch || islandMatch;
        boolean graceElapsed = System.currentTimeMillis() - worldSeenAt >= DIAGNOSTIC_GRACE_MS;
        if (!diagnosticLogged && (anyMatch || graceElapsed)) {
            diagnosticLogged = true;
            MinepieceEssentialsClient.LOGGER.info(
                "[ServerDetector] diagnostic — config={}, address={}, connection={}, tabList={}, island={}",
                forced, addressMatch, connectionMatch, tabListMatch, islandMatch);
        }

        if (forced) {
            lastReason = "forced by config";
            return true;
        }

        if (addressMatch) {
            lastReason = "address " + info.address;
            return true;
        }

        if (connectionMatch) {
            lastReason = "connection " + connHost;
            return true;
        }

        if (tabListMatch) {
            lastReason = "tab-list header/footer";
            return true;
        }

        if (islandMatch) {
            lastReason = "island detected";
            return true;
        }

        lastReason = info != null && info.address != null
            ? "address " + info.address
            : (connHost != null ? "connection " + connHost : "no server entry");
        return false;
    }

    /**
     * Le pied de page (et l'en-tête) du tab-list contiennent "PLAY.MINEPIECE.NET"
     * sur toutes les cartes du serveur, y compris l'île perso. Le serveur écrit
     * ce texte en petites capitales Unicode (ex. "ᴘʟᴀʏ.ᴍɪɴᴇᴘɪᴇᴄᴇ.ɴᴇᴛ") : ce sont
     * de vraies lettres, mais {@code Character.toLowerCase} ne les convertit
     * pas vers l'ASCII correspondant, donc on normalise explicitement via
     * {@link #normalizeSmallCaps} avant de chercher "minepiece". Extrait en
     * méthode pure (aucune dépendance Minecraft) pour être testable
     * unitairement.
     */
    static boolean tabListMatches(String header, String footer) {
        String combined = (header != null ? header : "") + (footer != null ? footer : "");
        return normalizeSmallCaps(combined).contains("minepiece");
    }

    /**
     * Normalise une chaîne en minuscules ASCII, en convertissant au passage les
     * petites capitales Unicode (ex. "ᴍ" U+1D0D) vers leur lettre ASCII
     * minuscule équivalente ("m"). {@code Normalizer.normalize(..., NFKD)} ne
     * suffit pas ici : ces caractères n'ont pas de décomposition de
     * compatibilité. Tout caractère hors de la table est laissé tel quel après
     * {@code Character.toLowerCase}. Méthode pure, testable unitairement.
     */
    static String normalizeSmallCaps(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char mapped = SMALL_CAPS_TO_ASCII.getOrDefault(c, Character.toLowerCase(c));
            sb.append(mapped);
        }
        return sb.toString();
    }

    // Table des petites capitales Unicode (small capitals) utilisées par le
    // serveur vers leur lettre ASCII minuscule. Il n'existe pas de petite
    // capitale pour "x" (le serveur utilise le "x" ASCII normal).
    private static final java.util.Map<Character, Character> SMALL_CAPS_TO_ASCII = java.util.Map.ofEntries(
        java.util.Map.entry('ᴀ', 'a'),
        java.util.Map.entry('ʙ', 'b'),
        java.util.Map.entry('ᴄ', 'c'),
        java.util.Map.entry('ᴅ', 'd'),
        java.util.Map.entry('ᴇ', 'e'),
        java.util.Map.entry('ꜰ', 'f'),
        java.util.Map.entry('ɢ', 'g'),
        java.util.Map.entry('ʜ', 'h'),
        java.util.Map.entry('ɪ', 'i'),
        java.util.Map.entry('ᴊ', 'j'),
        java.util.Map.entry('ᴋ', 'k'),
        java.util.Map.entry('ʟ', 'l'),
        java.util.Map.entry('ᴍ', 'm'),
        java.util.Map.entry('ɴ', 'n'),
        java.util.Map.entry('ᴏ', 'o'),
        java.util.Map.entry('ᴘ', 'p'),
        java.util.Map.entry('ꞯ', 'q'),
        java.util.Map.entry('ʀ', 'r'),
        java.util.Map.entry('ꜱ', 's'),
        java.util.Map.entry('ᴛ', 't'),
        java.util.Map.entry('ᴜ', 'u'),
        java.util.Map.entry('ᴠ', 'v'),
        java.util.Map.entry('ᴡ', 'w'),
        java.util.Map.entry('ʏ', 'y'),
        java.util.Map.entry('ᴢ', 'z')
    );

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
