package com.minepiece.essentials.pet;

import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/** Latest snapshot of the active pets and their summed combat-stat bonuses. */
public final class ActivePetsState {

    /**
     * An active pet with its rarity-coloured name and level.
     *
     * @param icon        the pet item, drawn as a small head in the HUD (may be empty)
     * @param rarityGlyph pack glyph of the pet's rarity, "" if unknown
     * @param statGlyphs  pack glyphs of its stat effects, in tooltip order
     */
    public record ActivePet(String name, int color, int level, ItemStack icon, String rarityGlyph, List<String> statGlyphs) {
        public ActivePet(String name, int color, int level) {
            this(name, color, level, ItemStack.EMPTY, "", List.of());
        }
    }

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
