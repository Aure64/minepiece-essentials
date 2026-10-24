package com.minepiece.essentials.profile;

import java.util.Locale;

/** Number formatting shared by the stat panels: integers as such, else one decimal. */
public final class StatLine {
    private StatLine() {}

    public static String fmt(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) return Long.toString(Math.round(v));
        return String.format(Locale.US, "%.1f", v);
    }
}
