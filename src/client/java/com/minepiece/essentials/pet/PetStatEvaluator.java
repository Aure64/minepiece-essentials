package com.minepiece.essentials.pet;

import java.util.OptionalDouble;

/**
 * Computes a roll's quality in {@code [0, 1]} as {@code value / max}, where the
 * max comes from the "Stats Pet Max" table for the effect's rarity, tier and
 * stat (1 = a perfect roll).
 *
 * <p>Returns empty when the tier is undefined for the rarity, or when the value
 * lands far above the max (a server "special" effect that ignores the table).
 */
public final class PetStatEvaluator {

    /** Above this fraction of the max, the value is treated as off-table. */
    private static final double OFF_TABLE = 1.5;

    private PetStatEvaluator() {}

    public static OptionalDouble quality(Rarity rarity, PetEffect effect) {
        if (rarity == null || effect == null) return OptionalDouble.empty();

        OptionalDouble max = PetStatTable.max(rarity, effect.tier(), effect.stat());
        if (max.isEmpty() || max.getAsDouble() <= 0) return OptionalDouble.empty();

        double q = effect.value() / max.getAsDouble();
        if (q < 0 || q > OFF_TABLE) return OptionalDouble.empty();
        return OptionalDouble.of(Math.min(1.0, q));
    }
}
