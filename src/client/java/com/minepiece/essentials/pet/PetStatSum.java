package com.minepiece.essentials.pet;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** Sums combat-stat effect values per stat (the bonus active pets grant). */
public final class PetStatSum {

    private PetStatSum() {}

    public static Map<PetStat, Double> sum(Collection<PetEffect> effects) {
        Map<PetStat, Double> totals = new EnumMap<>(PetStat.class);
        for (PetEffect e : effects) {
            totals.merge(e.stat(), e.value(), Double::sum);
        }
        return totals;
    }
}
