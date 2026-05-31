package com.minepiece.essentials.pet;

import java.util.List;
import java.util.Map;

/** Latest snapshot of the active pets and their summed combat-stat bonuses. */
public final class ActivePetsState {

    public record Snapshot(List<String> petNames, Map<PetStat, Double> totals) {
        public boolean isEmpty() {
            return petNames.isEmpty() && totals.isEmpty();
        }
    }

    private static volatile Snapshot current = new Snapshot(List.of(), Map.of());

    private ActivePetsState() {}

    public static Snapshot get() {
        return current;
    }

    public static void set(Snapshot snapshot) {
        current = snapshot;
    }
}
