package com.minepiece.essentials.pet;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Summing active pets' combat-stat values per stat. */
class PetStatSumTest {

    @Test
    void sumsSameStatAndKeepsOthersSeparate() {
        Map<PetStat, Double> totals = PetStatSum.sum(List.of(
            new PetEffect(20, PetStat.VITALITY, 100.0, "Health"),
            new PetEffect(15, PetStat.VITALITY, 50.0, "Health"),
            new PetEffect(20, PetStat.STRENGTH, 10.0, "Strength")));

        assertEquals(150.0, totals.get(PetStat.VITALITY), 1e-6);
        assertEquals(10.0, totals.get(PetStat.STRENGTH), 1e-6);
        assertFalse(totals.containsKey(PetStat.POWER));
    }

    @Test
    void emptyInputGivesEmptyTotals() {
        assertTrue(PetStatSum.sum(List.of()).isEmpty());
    }
}
