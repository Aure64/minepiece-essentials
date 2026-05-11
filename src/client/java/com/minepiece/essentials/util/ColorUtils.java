package com.minepiece.essentials.util;

public final class ColorUtils {
    private ColorUtils() {}

    public static int lerp(int colorA, int colorB, float t) {
        t = Math.max(0, Math.min(1, t));
        int aA = (colorA >> 24) & 0xFF, rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF, bA = colorA & 0xFF;
        int aB = (colorB >> 24) & 0xFF, rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF, bB = colorB & 0xFF;
        int a = (int)(aA + (aB - aA) * t);
        int r = (int)(rA + (rB - rA) * t);
        int g = (int)(gA + (gB - gA) * t);
        int b = (int)(bA + (bB - bA) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int healthGradient(float percent) {
        if (percent > 0.5f) {
            return lerp(0xFFFFFF00, 0xFF00FF00, (percent - 0.5f) * 2);
        }
        return lerp(0xFFFF0000, 0xFFFFFF00, percent * 2);
    }

    public static final int PARCHMENT_BG = 0xE6D4A76A;
    public static final int PARCHMENT_BORDER = 0xFF8B6914;
    public static final int TEXT_DARK = 0xFF3B2300;
    public static final int TEXT_LIGHT = 0xFFF5E6C8;
}
