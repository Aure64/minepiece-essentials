package com.minepiece.essentials.island;

import java.util.HashMap;
import java.util.Map;

public enum Island {
    FUCHSIA("Fuchsia", "fuchsia", "east_blue"),
    ORANGE_TOWN("Ville Orange", "orange_town", "east_blue"),
    BARATIE("Baratie", "baratie", "east_blue"),
    ARLONG_PARK("Arlong Park", "arlong_park", "east_blue"),
    LOGUE_TOWN("Logue Town", "logue_town", "east_blue"),
    DRUM("Royaume de Drum", "drum", "grand_line"),
    ALABASTA("Alabasta", "alabasta", "grand_line"),
    JAYA("Jaya", "jaya", "grand_line"),
    SKYPIEA("Skypiea", "skypiea", "grand_line"),
    WATER_SEVEN("Water Seven", "water_seven", "grand_line"),
    ENIES_LOBBY("Enies Lobby", "enies_lobby", "grand_line"),
    THRILLER_BARK("Thriller Bark", "thriller_bark", "grand_line"),
    SABAODY("Sabaody", "sabaody", "grand_line"),
    AMAZON_LILY("Amazon Lily", "amazon_lily", "grand_line"),
    ILE_HOMMES_POISSONS("Ile des Hommes-Poissons", "homme_poissons", "nouveau_monde"),
    PUNK_HAZARD("Punk Hazard", "punk_hazard", "nouveau_monde"),
    DRESSROSA("Dressrosa", "dressrosa", "nouveau_monde"),
    ZOU("Zou", "zou", "nouveau_monde"),
    WHOLE_CAKE("Whole Cake", "whole_cake", "nouveau_monde"),
    KOMUGI("Komugi", "komugi", "nouveau_monde"),
    UNKNOWN("Unknown", "unknown", "unknown");

    public final String displayName;
    public final String id;
    public final String zone;

    private static final Map<String, Island> BOSSBAR_MAP = new HashMap<>();

    Island(String displayName, String id, String zone) {
        this.displayName = displayName;
        this.id = id;
        this.zone = zone;
    }

    // Longueur minimale d'une clé auto-générée à partir du nom d'affichage. En
    // dessous, une île au nom court (ex. "Zou" -> "zou") produirait une clé qui
    // matche n'importe quel texte de boss bar contenant cette séquence par
    // hasard (nom de mob, de joueur, d'événement...), et BOSSBAR_MAP est un
    // HashMap parcouru dans un ordre arbitraire : le mauvais match peut gagner.
    // Les alias explicites ci-dessous (ex. "ile de zou") ne sont pas concernés :
    // ils couvrent le vrai cas d'usage et restent volontairement ajoutés tels quels.
    private static final int MIN_AUTO_KEY_LENGTH = 5;

    static {
        for (Island island : values()) {
            if (island != UNKNOWN) {
                String key = island.displayName.toLowerCase();
                if (key.length() >= MIN_AUTO_KEY_LENGTH) {
                    BOSSBAR_MAP.put(key, island);
                }
            }
        }
        BOSSBAR_MAP.put("archipel des sabaody", SABAODY);
        BOSSBAR_MAP.put("ile des hommes poissons", ILE_HOMMES_POISSONS);
        BOSSBAR_MAP.put("ile des hommes-poissons", ILE_HOMMES_POISSONS);
        BOSSBAR_MAP.put("whole cake island", WHOLE_CAKE);
        BOSSBAR_MAP.put("komugi island", KOMUGI);
        BOSSBAR_MAP.put("royaume de drum", DRUM);
        BOSSBAR_MAP.put("ile de zou", ZOU);
        // "Jaya" (4 lettres) tombe aussi sous MIN_AUTO_KEY_LENGTH : alias explicite
        // pour ne pas perdre la détection de cette île (contrairement à "zou", 4
        // lettres reste un risque de faux positif largement plus faible, mais on
        // applique le même seuil sans exception pour la génération automatique).
        BOSSBAR_MAP.put("jaya", JAYA);
    }

    public static Island fromBossbarText(String text) {
        String lower = text.toLowerCase().trim();
        for (Map.Entry<String, Island> entry : BOSSBAR_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return UNKNOWN;
    }

    public String getCommand() {
        return "/" + id;
    }
}
