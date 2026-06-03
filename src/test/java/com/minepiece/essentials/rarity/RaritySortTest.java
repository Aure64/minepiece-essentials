package com.minepiece.essentials.rarity;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RaritySortTest {

    private static RaritySort.Entry it(int rank, String id, String name) {
        return new RaritySort.Entry(false, rank, id, name);
    }
    private static final RaritySort.Entry EMPTY = new RaritySort.Entry(true, -1, "", "");

    // ---- mode RARITY (rareté seule, identiques regroupés par itemId) ----

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

    // ---- mode RARITY_ALPHA (rareté PUIS alphabétique dans chaque rareté) ----

    @Test
    void rarityAlphaSortsByNameWithinSameRank() {
        // tous rang 3 : noms Banane / Avocat / Cerise → A, B, C
        List<RaritySort.Entry> in = List.of(
                it(3, "x", "Banane"), it(3, "y", "Avocat"), it(3, "z", "Cerise"));
        assertEquals(List.of(1, 0, 2), RaritySort.targetOrder(in, RaritySort.Mode.RARITY_ALPHA, false));
    }

    @Test
    void rarityAlphaKeepsRarityAsPrimary() {
        // index 0: rang4 "Zoro", 1: rang3 "Ace", 2: rang4 "Ace"
        // desc rang : rang4 d'abord (Ace=2 puis Zoro=0), puis rang3 (Ace=1)
        List<RaritySort.Entry> in = List.of(
                it(4, "a", "Zoro"), it(3, "b", "Ace"), it(4, "c", "Ace"));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, RaritySort.Mode.RARITY_ALPHA, false));
    }

    @Test
    void rarityAlphaNameIsCaseInsensitiveAndAlwaysAtoZ() {
        // même si rang desc, le nom reste A→Z dans la rareté
        List<RaritySort.Entry> in = List.of(it(3, "a", "banane"), it(3, "b", "Avocat"));
        assertEquals(List.of(1, 0), RaritySort.targetOrder(in, RaritySort.Mode.RARITY_ALPHA, false));
    }

    @Test
    void rarityAlphaEmptyLast() {
        List<RaritySort.Entry> in = List.of(it(3, "a", "Banane"), EMPTY, it(3, "b", "Avocat"));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, RaritySort.Mode.RARITY_ALPHA, false));
    }
}
