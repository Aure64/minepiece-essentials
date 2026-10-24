package com.minepiece.essentials.ah;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.config.ModConfig;

/**
 * Couleurs des bandes de prix AH, configurables dans {@code config.json}
 * ({@code ahColorCheap} / {@code ahColorFair} / {@code ahColorExpensive}, "#RRGGBB")
 * et via l'écran {@link AhColorScreen} (touche K → « Couleurs prix AH »).
 */
public final class AhPriceColors {

    public static final String DEFAULT_CHEAP = "#FFD24B";
    public static final String DEFAULT_FAIR = "#7CFC55";
    public static final String DEFAULT_EXPENSIVE = "#FF5555";

    private AhPriceColors() {}

    /** Couleur ARGB (opaque) de la bande selon la config courante. */
    public static int of(AhPriceBand.Band band) {
        return of(band, MinepieceEssentialsClient.getInstance().getConfigManager().config());
    }

    /** Idem, config explicite (testable). */
    public static int of(AhPriceBand.Band band, ModConfig cfg) {
        return parse(get(band, cfg), band.defaultHex);
    }

    /** Valeur brute ("#RRGGBB") de la bande dans la config. */
    public static String get(AhPriceBand.Band band, ModConfig cfg) {
        return switch (band) {
            case CHEAP -> cfg.ahColorCheap;
            case FAIR -> cfg.ahColorFair;
            case EXPENSIVE -> cfg.ahColorExpensive;
        };
    }

    /** Écrit la couleur d'une bande, normalisée en "#RRGGBB" ; ignoré si illisible. */
    public static void set(AhPriceBand.Band band, String hex, ModConfig cfg) {
        String norm = normalize(hex);
        if (norm == null) return;
        switch (band) {
            case CHEAP -> cfg.ahColorCheap = norm;
            case FAIR -> cfg.ahColorFair = norm;
            case EXPENSIVE -> cfg.ahColorExpensive = norm;
        }
    }

    /** Échange les couleurs « moins cher » et « au prix » (demande joueurs : bonne affaire en vert). */
    public static void swapCheapFair(ModConfig cfg) {
        String cheap = cfg.ahColorCheap;
        cfg.ahColorCheap = cfg.ahColorFair;
        cfg.ahColorFair = cheap;
    }

    /** Retour aux couleurs d'origine. */
    public static void resetDefaults(ModConfig cfg) {
        cfg.ahColorCheap = DEFAULT_CHEAP;
        cfg.ahColorFair = DEFAULT_FAIR;
        cfg.ahColorExpensive = DEFAULT_EXPENSIVE;
    }

    /** "#RRGGBB" (ou "RRGGBB", "0xRRGGBB") → ARGB opaque ; illisible → {@code fallbackHex}. */
    public static int parse(String hex, String fallbackHex) {
        Integer rgb = parseRgb(hex);
        if (rgb == null) rgb = parseRgb(fallbackHex);
        return 0xFF000000 | (rgb == null ? 0xFFFFFF : rgb);
    }

    public static boolean isValid(String hex) {
        return parseRgb(hex) != null;
    }

    /** Forme canonique "#RRGGBB" (majuscules), ou null si illisible. */
    public static String normalize(String hex) {
        Integer rgb = parseRgb(hex);
        return rgb == null ? null : toHex(rgb);
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static Integer parseRgb(String hex) {
        if (hex == null) return null;
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        else if (h.regionMatches(true, 0, "0x", 0, 2)) h = h.substring(2);
        if (h.length() != 6) return null;
        try {
            return Integer.parseInt(h, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
