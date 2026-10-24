package com.minepiece.essentials.profile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Live player level from the server boss bar ("… Niveau 97 (58.59%) …"), which alternates
 * with the zone name: the last value seen is kept. A level change asks for a new
 * {@code /profil} read (stats grow with the level).
 */
public final class PlayerLevel {
    private static final Pattern LEVEL = Pattern.compile("(?:Niveau|Level)\\s+(\\d+)\\s*\\(([\\d.,]+)%\\)");

    private static volatile int level;
    private static volatile double percent;

    private PlayerLevel() {}

    /** Called from the boss bar mixin with the bar's plain text. */
    public static void onBossBarText(String text) {
        if (text == null) return;
        Matcher m = LEVEL.matcher(text);
        if (!m.find()) return;
        int lvl = Integer.parseInt(m.group(1));
        double pct = Double.parseDouble(m.group(2).replace(',', '.'));
        if (level != 0 && lvl != level) ProfileScanner.refreshSoon();
        level = lvl;
        percent = pct;
    }

    /** 0 if never seen this session. */
    public static int level() { return level; }
    public static double percent() { return percent; }

    public static void reset() { level = 0; percent = 0; }
}
