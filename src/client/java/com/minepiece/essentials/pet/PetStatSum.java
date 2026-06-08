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

    /**
     * Returns a representative server-side display label for each {@link PetStat} present
     * in the given effects (the first non-empty label encountered for that stat).
     */
    public static Map<PetStat, String> labels(Collection<PetEffect> effects) {
        Map<PetStat, String> result = new EnumMap<>(PetStat.class);
        for (PetEffect e : effects) {
            if (!e.label().isBlank()) {
                result.putIfAbsent(e.stat(), e.label());
            }
        }
        return result;
    }
}
