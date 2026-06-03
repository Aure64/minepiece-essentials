package com.minepiece.essentials.rarity;

import java.util.EnumSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/** Filtre de surbrillance, multi-sélection, portée session. */
public final class RarityFilterState {
    private final Set<ItemRarity> active = EnumSet.noneOf(ItemRarity.class);

    public void toggle(ItemRarity r) {
        if (!active.add(r)) active.remove(r);
    }
    public boolean isActive(ItemRarity r) { return active.contains(r); }
    public boolean any() { return !active.isEmpty(); }
    public void clear() { active.clear(); }

    /** true si un filtre est actif et que cette rareté (ou l'absence de rareté) n'en fait pas partie. */
    public boolean isDimmed(@Nullable ItemRarity r) {
        if (active.isEmpty()) return false;
        return r == null || !active.contains(r);
    }
}
