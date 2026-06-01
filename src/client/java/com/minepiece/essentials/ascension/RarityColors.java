package com.minepiece.essentials.ascension;

/** Maps a MinePiece item rarity token to an ARGB display colour. */
public final class RarityColors {

    private static final int DEFAULT = 0xFFE9D5C7;

    private RarityColors() {}

    public static int color(String rarity) {
        if (rarity == null) return DEFAULT;
        return switch (rarity.toLowerCase()) {
            case "common", "commun" -> 0xFFFFFFFF;
            case "uncommon", "peu commun" -> 0xFF55FF55;
            case "rare" -> 0xFF5599FF;
            case "epic", "epique", "épique" -> 0xFFB24BFF;
            case "legendary", "legendaire", "légendaire" -> 0xFFFFAA00;
            case "mythic", "mythique" -> 0xFFFF5577;
            case "nightmare" -> 0xFFC050FF;
            default -> DEFAULT;
        };
    }
}
