package com.minepiece.essentials.rarity;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RaritySortTest {

    private static RaritySort.Entry e(int rank, String id) { return new RaritySort.Entry(rank, id); }

    @Test
    void descPutsHighestRankFirst() {
        // index: 0=rare(1), 1=mythic(4), 2=common(0)
        List<RaritySort.Entry> in = List.of(e(1, "a"), e(4, "b"), e(0, "c"));
        assertEquals(List.of(1, 0, 2), RaritySort.targetOrder(in, true));
    }

    @Test
    void ascPutsLowestRankFirst() {
        List<RaritySort.Entry> in = List.of(e(1, "a"), e(4, "b"), e(0, "c"));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, false));
    }

    @Test
    void noRarityAlwaysLastInBothDirections() {
        // index 1 has no rarity (-1)
        List<RaritySort.Entry> in = List.of(e(4, "a"), e(-1, "x"), e(0, "c"));
        assertEquals(List.of(0, 2, 1), RaritySort.targetOrder(in, true));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, false));
    }

    @Test
    void sameRankGroupedByItemIdThenStable() {
        // two legendaries (rank 3): ids "zz" and "aa" → grouped, sorted by id, stable
        List<RaritySort.Entry> in = List.of(e(3, "zz"), e(3, "aa"), e(0, "c"));
        // desc: rank3 first, within rank by id asc -> aa(1), zz(0); then common c(2)
        assertEquals(List.of(1, 0, 2), RaritySort.targetOrder(in, true));
    }
}
