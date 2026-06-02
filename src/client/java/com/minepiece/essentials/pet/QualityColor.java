package com.minepiece.essentials.pet;

/**
 * Maps a roll quality in {@code [0, 1]} to an RGB colour (official thresholds):
 * red below 50%, yellow 50–80%, green 80% and up.
 */
public final class QualityColor {

    private QualityColor() {}

    public static int of(double quality) {
        if (quality < 0.50) return 0xFF5555; // red
        if (quality < 0.80) return 0xFFFF55; // yellow
        return 0x55FF55;                     // green
    }
}
