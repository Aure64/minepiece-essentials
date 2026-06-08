package com.minepiece.essentials.pet;

import java.util.List;
import java.util.Map;

/** Latest snapshot of the active pets and their summed combat-stat bonuses. */
public final class ActivePetsState {

    /** An active pet with its rarity-coloured name and level. */
    public record ActivePet(String name, int color, int level) {}

    public record Snapshot(List<ActivePet> pets, Map<PetStat, Double> totals, Map<PetStat, String> labels) {
        public boolean isEmpty() {
            return pets.isEmpty() && totals.isEmpty();
        }
    }

    private static volatile Snapshot current = new Snapshot(List.of(), Map.of(), Map.of());

    private ActivePetsState() {}

    public static Snapshot get() {
        return current;
    }

    public static void set(Snapshot snapshot) {
        current = snapshot;
    }
}
