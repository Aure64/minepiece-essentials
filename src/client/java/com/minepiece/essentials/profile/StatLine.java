package com.minepiece.essentials.profile;

import com.minepiece.essentials.pet.PetStat;
import java.util.Locale;
import java.util.Optional;

/** Right-hand text of a stat line: "+50.5" alone, or "205 (+50.5)" when the profile is known. */
public final class StatLine {
    private StatLine() {}

    public static String right(PetStat stat, double part) {
        Optional<ProfileData> profile = ProfileState.get();
        if (profile.isEmpty() || !profile.get().byStat().containsKey(stat)) return "+" + fmt(part);
        ProfileData p = profile.get();
        String total = fmt(p.byStat().get(stat)) + (p.isPercent(stat) ? "%" : "");
        return total + " (+" + fmt(part) + ")";
    }

    public static String fmt(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) return Long.toString(Math.round(v));
        return String.format(Locale.US, "%.1f", v);
    }
}
