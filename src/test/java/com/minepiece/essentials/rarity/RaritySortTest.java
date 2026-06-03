package com.minepiece.essentials.rarity;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RaritySortTest {

    private static RaritySort.Entry it(int rank, String id, String name) {
        return new RaritySort.Entry(false, rank, id, name);
    }
    private static final RaritySort.Entry EMPTY = new RaritySort.Entry(true, -1, "", "");

    // ---- mode RARITY (rareté en primaire, identiques regroupés par itemId) ----

    @Test
    void rarityDescPutsHighestRankFirst() {
        List<RaritySort.Entry> in = List.of(it(1, "a", ""), it(4, "b", ""), it(0, "c", ""));
        assertEquals(List.of(1, 0, 2), RaritySort.targetOrder(in, RaritySort.Mode.RARITY, false));
    }

    @Test
    void rarityAscPutsLowestRankFirst() {
        List<RaritySort.Entry> in = List.of(it(1, "a", ""), it(4, "b", ""), it(0, "c", ""));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, RaritySort.Mode.RARITY, true));
    }

    @Test
    void emptyAlwaysLastInBothDirections() {
        List<RaritySort.Entry> in = List.of(it(4, "a", ""), EMPTY, it(0, "c", ""));
        assertEquals(List.of(0, 2, 1), RaritySort.targetOrder(in, RaritySort.Mode.RARITY, false));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, RaritySort.Mode.RARITY, true));
    }

    @Test
    void sameRankGroupedByItemId() {
        List<RaritySort.Entry> in = List.of(it(3, "zz", ""), it(3, "aa", ""), it(0, "c", ""));
        assertEquals(List.of(1, 0, 2), RaritySort.targetOrder(in, RaritySort.Mode.RARITY, false));
    }

    // ---- mode ITEM (objet en primaire, ses raretés à la suite) ----

    @Test
    void itemGroupsSameObjectWithRaritiesConsecutiveHighestFirst() {
        // 0 = Boulon épique (rang 2), 1 = Boulon légendaire (rang 3), 2 = Ancre commune (rang 0)
        List<RaritySort.Entry> in = List.of(
                it(2, "boulon", "Boulon"), it(3, "boulon", "Boulon"), it(0, "ancre", "Ancre"));
        // A→Z : Ancre, puis le groupe Boulon (légendaire avant épique)
        assertEquals(List.of(2, 1, 0), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, true));
    }

    @Test
    void itemWithinObjectHigherRarityFirstRegardlessOfDirection() {
        List<RaritySort.Entry> in = List.of(it(2, "boulon", "Boulon"), it(3, "boulon", "Boulon"));
        // dans les deux sens, la plus haute rareté (index 1) reste devant
        assertEquals(List.of(1, 0), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, true));
        assertEquals(List.of(1, 0), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, false));
    }

    @Test
    void itemDescNameZtoA() {
        List<RaritySort.Entry> in = List.of(
                it(2, "boulon", "Boulon"), it(3, "boulon", "Boulon"), it(0, "ancre", "Ancre"));
        // Z→A : groupe Boulon (légendaire, épique) puis Ancre
        assertEquals(List.of(1, 0, 2), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, false));
    }

    @Test
    void itemNameCaseInsensitive() {
        List<RaritySort.Entry> in = List.of(it(0, "a", "banane"), it(0, "b", "Avocat"));
        assertEquals(List.of(1, 0), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, true));
    }

    @Test
    void itemEmptyLast() {
        List<RaritySort.Entry> in = List.of(it(0, "a", "Banane"), EMPTY, it(0, "b", "Avocat"));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, true));
    }
}
