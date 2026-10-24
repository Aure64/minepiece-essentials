package com.minepiece.essentials.profile;

import com.minepiece.essentials.pet.PetStat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The player's global stats as shown by {@code /profil} (item "Information").
 *
 * @param ascension    ascension count
 * @param level        player level
 * @param levelPercent progress in the current level, 0–100
 * @param xp           xp in the current level
 * @param maxXp        xp needed for the next level
 * @param entries      every stat line in screen order (incl. ones with no {@link PetStat}, e.g. Damage)
 * @param byStat       value per known stat
 * @param percentStats stats the screen shows with a % sign (e.g. Critical Chance)
 */
public record ProfileData(int ascension, int level, double levelPercent, long xp, long maxXp,
                          List<Entry> entries, Map<PetStat, Double> byStat, Set<PetStat> percentStats) {

    /** One stat line: {@code label} as displayed, {@code stat} null if unknown to the mod. */
    public record Entry(String label, PetStat stat, double value, boolean percent) {}

    public boolean isPercent(PetStat stat) { return percentStats.contains(stat); }
}
