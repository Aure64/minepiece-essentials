package com.minepiece.essentials.pet;

import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Latest snapshot of the active pets and their summed combat-stat bonuses. */
public final class ActivePetsState {

    /**
     * An active pet with its rarity-coloured name and level.
     *
     * @param icon       the pet item, drawn as a small head in the HUD (may be empty)
     * @param statGlyphs the stat icons of its effects, in tooltip order: the pack glyph
     *                   <em>with the font style it carries in the lore</em> (a bare
     *                   String would render as a CJK character in the default font)
     */
    public record ActivePet(String name, int color, int level, ItemStack icon, List<Component> statGlyphs) {
        public ActivePet(String name, int color, int level) {
            this(name, color, level, ItemStack.EMPTY, List.of());
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
        Snapshot before = current;
        current = snapshot;
        // Pets activés/désactivés → les stats globales changent : relire /profil.
        if (!names(before).equals(names(snapshot))) com.minepiece.essentials.profile.ProfileScanner.refreshSoon();
    }

    private static List<String> names(Snapshot s) {
        return s.pets().stream().map(ActivePet::name).toList();
    }
}
