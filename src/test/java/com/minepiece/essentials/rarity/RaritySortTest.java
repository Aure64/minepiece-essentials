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

    // ---- mode ITEM (objet en primaire ; noms TOUJOURS A→Z ; ascending = sens des raretés) ----

    @Test
    void itemGroupsObjectsAtoZ_rarityDescWithinGroup() {
        // 0 = Boulon épique (rang 2), 1 = Boulon légendaire (rang 3), 2 = Ancre commune (rang 0)
        List<RaritySort.Entry> in = List.of(
                it(2, "boulon", "Boulon"), it(3, "boulon", "Boulon"), it(0, "ancre", "Ancre"));
        // ascending=false : noms A→Z (Ancre puis Boulon), raretés plus rare d'abord (lég. avant épique)
        assertEquals(List.of(2, 1, 0), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, false));
    }

    @Test
    void itemGroupsObjectsAtoZ_rarityAscWithinGroup() {
        List<RaritySort.Entry> in = List.of(
                it(2, "boulon", "Boulon"), it(3, "boulon", "Boulon"), it(0, "ancre", "Ancre"));
        // ascending=true : noms toujours A→Z, mais raretés moins rare d'abord (épique avant lég.)
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, true));
    }

    @Test
    void itemNameAlwaysAtoZ_bothDirections() {
        List<RaritySort.Entry> in = List.of(it(0, "a", "Banane"), it(0, "b", "Avocat"));
        // Avocat avant Banane quel que soit le sens (les noms ne s'inversent jamais)
        assertEquals(List.of(1, 0), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, true));
        assertEquals(List.of(1, 0), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, false));
    }

    @Test
    void itemWithinGroupRarityDirectionFlips() {
        List<RaritySort.Entry> in = List.of(it(2, "boulon", "Boulon"), it(3, "boulon", "Boulon"));
        assertEquals(List.of(1, 0), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, false)); // plus rare d'abord
        assertEquals(List.of(0, 1), RaritySort.targetOrder(in, RaritySort.Mode.ITEM, true));  // moins rare d'abord
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
