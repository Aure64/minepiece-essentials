package com.minepiece.essentials.rarity;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RaritySortTest {

    private static RaritySort.Entry r(int rank, String id) {
        return new RaritySort.Entry(false, rank, id, "");
    }
    private static RaritySort.Entry n(String name, String id) {
        return new RaritySort.Entry(false, 0, id, name);
    }
    private static final RaritySort.Entry EMPTY = new RaritySort.Entry(true, -1, "", "");

    // ---- mode RARITY ----

    @Test
    void rarityDescPutsHighestRankFirst() {
        List<RaritySort.Entry> in = List.of(r(1, "a"), r(4, "b"), r(0, "c"));
        assertEquals(List.of(1, 0, 2), RaritySort.targetOrder(in, RaritySort.Mode.RARITY, false));
    }

    @Test
    void rarityAscPutsLowestRankFirst() {
        List<RaritySort.Entry> in = List.of(r(1, "a"), r(4, "b"), r(0, "c"));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, RaritySort.Mode.RARITY, true));
    }

    @Test
    void emptyAlwaysLastInBothDirections() {
        List<RaritySort.Entry> in = List.of(r(4, "a"), EMPTY, r(0, "c"));
        assertEquals(List.of(0, 2, 1), RaritySort.targetOrder(in, RaritySort.Mode.RARITY, false));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, RaritySort.Mode.RARITY, true));
    }

    @Test
    void sameRankGroupedByItemIdThenStable() {
        List<RaritySort.Entry> in = List.of(r(3, "zz"), r(3, "aa"), r(0, "c"));
        assertEquals(List.of(1, 0, 2), RaritySort.targetOrder(in, RaritySort.Mode.RARITY, false));
    }

    // ---- mode ALPHABETICAL ----

    @Test
    void alphaAscIsAtoZ() {
        // index: 0="Banane", 1="Avocat", 2="Cerise"
        List<RaritySort.Entry> in = List.of(n("Banane", "a"), n("Avocat", "b"), n("Cerise", "c"));
        assertEquals(List.of(1, 0, 2), RaritySort.targetOrder(in, RaritySort.Mode.ALPHABETICAL, true));
    }

    @Test
    void alphaDescIsZtoA() {
        List<RaritySort.Entry> in = List.of(n("Banane", "a"), n("Avocat", "b"), n("Cerise", "c"));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, RaritySort.Mode.ALPHABETICAL, false));
    }

    @Test
    void alphaIsCaseInsensitive() {
        List<RaritySort.Entry> in = List.of(n("banane", "a"), n("Avocat", "b"));
        assertEquals(List.of(1, 0), RaritySort.targetOrder(in, RaritySort.Mode.ALPHABETICAL, true));
    }

    @Test
    void alphaEmptyLast() {
        List<RaritySort.Entry> in = List.of(n("Banane", "a"), EMPTY, n("Avocat", "b"));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, RaritySort.Mode.ALPHABETICAL, true));
    }
}
