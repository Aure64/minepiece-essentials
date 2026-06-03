package com.minepiece.essentials.rarity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RarityStateTest {

    @Test
    void filterTogglesMultiple() {
        RarityFilterState f = new RarityFilterState();
        assertFalse(f.any());
        f.toggle(ItemRarity.MYTHIC);
        f.toggle(ItemRarity.LEGENDARY);
        assertTrue(f.isActive(ItemRarity.MYTHIC));
        assertTrue(f.isActive(ItemRarity.LEGENDARY));
        f.toggle(ItemRarity.MYTHIC);
        assertFalse(f.isActive(ItemRarity.MYTHIC));
        assertTrue(f.any());
    }

    @Test
    void dimmedWhenFilterActiveAndNotMatching() {
        RarityFilterState f = new RarityFilterState();
        assertFalse(f.isDimmed(ItemRarity.MYTHIC)); // aucun filtre → rien d'estompé
        assertFalse(f.isDimmed(null));
        f.toggle(ItemRarity.MYTHIC);
        assertFalse(f.isDimmed(ItemRarity.MYTHIC)); // correspond
        assertTrue(f.isDimmed(ItemRarity.RARE));    // ne correspond pas
        assertTrue(f.isDimmed(null));               // item sans rareté estompé
    }

    @Test
    void clearResetsFilter() {
        RarityFilterState f = new RarityFilterState();
        f.toggle(ItemRarity.RARE);
        f.clear();
        assertFalse(f.any());
    }

    @Test
    void sortDirectionToggles() {
        RaritySortState s = new RaritySortState();
        assertTrue(s.descending()); // défaut : mythique d'abord
        s.toggle();
        assertFalse(s.descending());
        s.toggle();
        assertTrue(s.descending());
    }
}
