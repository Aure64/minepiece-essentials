package com.minepiece.essentials.pet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minion prestige cost maths. The cost to go from prestige p to p+1 is
 * {@code base * 2^(p-1)} where {@code base} is the rarity's prestige-1 cost,
 * derived live from the pet's own NBT ({@code base = nextXp / 2^(prestige-1)}).
 */
class MinionCostTest {

    @Test
    void remainingToNextPrestigeIsNextMinusCurrent() {
        assertEquals(62119.0, MinionCost.remainingToNextPrestige(1881.0, 64000.0), 1e-6);
    }

    @Test
    void jackRemainingToMax() {
        // Prestige 8, xp 1881, next 64000 (Legendary, base 500) → 190119
        assertEquals(190119.0, MinionCost.remainingToMax(8, 1881.0, 64000.0), 1e-6);
    }

    @Test
    void luffyRemainingToMax() {
        // Prestige 9, xp 34560.5, next 128000 (base 500) → 93439.5
        assertEquals(93439.5, MinionCost.remainingToMax(9, 34560.5, 128000.0), 1e-6);
    }

    @Test
    void freshLegendaryRemainingToMaxIsFullCurve() {
        // Prestige 1, xp 0, next 500 → base 500 × 511 = 255500
        assertEquals(255500.0, MinionCost.remainingToMax(1, 0.0, 500.0), 1e-6);
    }

    @Test
    void atPrestigeNineToMaxEqualsToNextPrestige() {
        double toMax = MinionCost.remainingToMax(9, 1000.0, 128000.0);
        double toNext = MinionCost.remainingToNextPrestige(1000.0, 128000.0);
        assertEquals(toNext, toMax, 1e-6);
    }

    @Test
    void maxedMinionHasZeroRemaining() {
        assertEquals(0.0, MinionCost.remainingToMax(10, 0.0, 0.0), 1e-9);
    }
}
