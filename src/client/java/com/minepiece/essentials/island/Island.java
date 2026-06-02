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

    static {
        for (Island island : values()) {
            if (island != UNKNOWN) {
                BOSSBAR_MAP.put(island.displayName.toLowerCase(), island);
            }
        }
        BOSSBAR_MAP.put("archipel des sabaody", SABAODY);
        BOSSBAR_MAP.put("ile des hommes poissons", ILE_HOMMES_POISSONS);
        BOSSBAR_MAP.put("ile des hommes-poissons", ILE_HOMMES_POISSONS);
        BOSSBAR_MAP.put("whole cake island", WHOLE_CAKE);
        BOSSBAR_MAP.put("komugi island", KOMUGI);
        BOSSBAR_MAP.put("royaume de drum", DRUM);
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
