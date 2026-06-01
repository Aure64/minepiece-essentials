package com.minepiece.essentials.job;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks the active job's progress, read live from the action bar. During
 * gathering the server shows e.g. {@code "+2.77 实  +0.92 蓝   蓝 11868/39060"},
 * where {@code 蓝} (U+84DD) is the job-XP glyph and {@code 11868/39060} is the
 * current/needed XP for the current level. The progress is an absolute value, so
 * re-reads are idempotent — no accumulation, no double-count risk.
 */
public final class JobTracker {

    private static final char JOB_GLYPH = '蓝'; // 蓝
    private static final Pattern PROGRESS = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    private static volatile long current = -1;
    private static volatile long needed = -1;
    private static volatile int level = -1;
    private static volatile long lastUpdate = 0;

    private JobTracker() {}

    public static void onActionBar(String text) {
        if (text == null || text.indexOf(JOB_GLYPH) < 0) return;
        Matcher m = PROGRESS.matcher(text);
        if (!m.find()) return;
        try {
            long cur = Long.parseLong(m.group(1));
            long need = Long.parseLong(m.group(2));
            if (need <= 0 || cur > need) return; // sanity
            current = cur;
            needed = need;
            level = LevelsTable.levelForNeeded(need);
            lastUpdate = System.currentTimeMillis();
        } catch (NumberFormatException ignored) {
            // not a job progress line
        }
    }

    public static boolean hasData() { return needed > 0; }
    public static long current() { return current; }
    public static long needed() { return needed; }
    public static long remaining() { return Math.max(0, needed - current); }
    public static int level() { return level; }
    public static float progress() { return needed > 0 ? (float) current / needed : 0f; }
    public static long lastUpdate() { return lastUpdate; }

    public static void reset() {
        current = -1;
        needed = -1;
        level = -1;
        lastUpdate = 0;
    }
}
