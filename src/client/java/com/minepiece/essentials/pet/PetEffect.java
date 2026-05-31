package com.minepiece.essentials.pet;

/**
 * A single rolled pet effect parsed from a tooltip line.
 *
 * @param tier  the level milestone the effect unlocks at (5, 10, 15 or 20);
 *              the wiki ranges are LVL-10, so the scaling factor is {@code tier/10}
 * @param stat  which combat stat
 * @param value the displayed (level-scaled) value
 */
public record PetEffect(int tier, PetStat stat, double value) {}
