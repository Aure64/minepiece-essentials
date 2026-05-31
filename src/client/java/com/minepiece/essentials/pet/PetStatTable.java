package com.minepiece.essentials.pet;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * The "Stats Pet Max" reference: the maximum value a stat can reach for a given
 * rarity, at a given pet level. A roll's quality is its value divided by this
 * maximum.
 *
 * <p>The wiki gives the maxima at level 10; in game they scale linearly with the
 * pet level (the "LVL X" badge), so {@code max(level) = maxL10 * level / 10}.
 * This holds for every level (5, 11, 15, 20, …), not just the tabulated ones.
 *
 * <p>Each L10 row is ordered to match {@link PetStat#ordinal()}: Vie, Force,
 * Chance Critique, Dégâts Critiques, Puissance, Énergie, Régén énergie, Vitesse,
 * Dextérité, Défense, Régén.
 */
public final class PetStatTable {

    private static final Map<Rarity, double[]> MAX_L10 = new EnumMap<>(Rarity.class);

    private PetStatTable() {}

    static {
        //                       Vie    Force  CritCh  CritDmg Puis   Énerg  EnReg  Vit    Dext   Déf    Regen
        MAX_L10.put(Rarity.COMMON,    new double[]{25,  2.5,  0.625, 3.75,  2.5,  25,  2.5,  1.25, 1.25, 2.5,  2.5});
        MAX_L10.put(Rarity.RARE,      new double[]{50,  5,    1.25,  7.5,   5,    50,  5,    2.5,  2.5,  5,    5});
        MAX_L10.put(Rarity.EPIC,      new double[]{75,  7.5,  1.875, 11.25, 7.5,  75,  7.5,  3.75, 3.75, 7.5,  7.5});
        MAX_L10.put(Rarity.LEGENDARY, new double[]{100, 10,   2.5,   15,    10,   100, 10,   5,    5,    10,   10});
        MAX_L10.put(Rarity.MYTHIC,    new double[]{150, 12.5, 3.125, 6.25,  12.5, 150, 12.5, 6.25, 6.25, 12.5, 12.5});
    }

    /** Maximum value for the rarity/level/stat, or empty if the rarity is unknown or level &le; 0. */
    public static OptionalDouble max(Rarity rarity, int level, PetStat stat) {
        double[] row = rarity == null ? null : MAX_L10.get(rarity);
        if (row == null || level <= 0) return OptionalDouble.empty();
        return OptionalDouble.of(row[stat.ordinal()] * level / 10.0);
    }
}
