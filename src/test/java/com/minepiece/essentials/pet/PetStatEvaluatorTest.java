package com.minepiece.essentials.pet;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quality computation, validated against real pets from the MinePiece server
 * (values read from item NBT).
 *
 * <p>Quality is the roll's position between the minimum and maximum for its
 * rarity, level and stat: {@code (value - min) / (max - min)} (0 = worst roll,
 * 1 = best roll). Both bounds scale linearly with the pet level ({@code ×tier/10}).
 */
class PetStatEvaluatorTest {

    private static double quality(Rarity rarity, PetStat stat, double value, int tier) {
        OptionalDouble q = PetStatEvaluator.quality(rarity, new PetEffect(tier, stat, value));
        assertTrue(q.isPresent(), "expected a quality for " + stat);
        return q.getAsDouble();
    }

    // --- min/max normalisation behaviour -------------------------------------

    @Test
    void rollAtMinimumIsZeroPercent() {
        // Puissance Legendary L10 min = 5; a value sitting on the floor is 0%.
        assertEquals(0.0, quality(Rarity.LEGENDARY, PetStat.POWER, 5.0, 10), 1e-9);
    }

    @Test
    void rollAtMaximumIsHundredPercent() {
        // Puissance Legendary L10 max = 10.
        assertEquals(1.0, quality(Rarity.LEGENDARY, PetStat.POWER, 10.0, 10), 1e-9);
    }

    @Test
    void rollAtMidpointIsFiftyPercent() {
        // Puissance Legendary L10 spans [5, 10]; 7.5 is exactly half — the
        // example the request was based on.
        assertEquals(0.5, quality(Rarity.LEGENDARY, PetStat.POWER, 7.5, 10), 1e-9);
    }

    @Test
    void boundsScaleWithTier() {
        // At tier 20 the band doubles: Puissance Legendary → [10, 20].
        assertEquals(0.0, quality(Rarity.LEGENDARY, PetStat.POWER, 10.0, 20), 1e-9);
        assertEquals(1.0, quality(Rarity.LEGENDARY, PetStat.POWER, 20.0, 20), 1e-9);
        assertEquals(0.5, quality(Rarity.LEGENDARY, PetStat.POWER, 15.0, 20), 1e-9);
    }

    // --- real pets -----------------------------------------------------------

    @Test
    void trafalgarStrengthIsNearPerfect() {
        // Force 19.86, tier 20, Legendary band [10, 20] → 9.86/10.
        assertEquals(0.986, quality(Rarity.LEGENDARY, PetStat.STRENGTH, 19.86, 20), 0.001);
    }

    @Test
    void eustassStrengthIsHigh() {
        // Force 18.15, tier 20 → 8.15/10.
        assertEquals(0.815, quality(Rarity.LEGENDARY, PetStat.STRENGTH, 18.15, 20), 0.001);
    }

    @Test
    void zoroHealthIsModest() {
        // Vie 105.65, tier 15, Legendary band [75, 150] → 30.65/75.
        assertEquals(0.4087, quality(Rarity.LEGENDARY, PetStat.VITALITY, 105.65, 15), 0.001);
    }

    @Test
    void zoroCriticalChanceIsHigh() {
        // Chance Crit 3.1, tier 15, Legendary band [1.875, 3.75] → 1.225/1.875.
        assertEquals(0.6533, quality(Rarity.LEGENDARY, PetStat.CRIT_CHANCE, 3.1, 15), 0.001);
    }

    @Test
    void luffyEnergyIsHigh() {
        // Énergie 180.24, tier 20, Legendary band [100, 200] → 80.24/100.
        assertEquals(0.8024, quality(Rarity.LEGENDARY, PetStat.ENERGY, 180.24, 20), 0.001);
    }

    @Test
    void luffyPowerIsNearPerfect() {
        // Puissance 19.85, tier 20, band [10, 20] → 9.85/10.
        assertEquals(0.985, quality(Rarity.LEGENDARY, PetStat.POWER, 19.85, 20), 0.001);
    }

    @Test
    void usoppCriticalChanceAtTier5() {
        // Chance Crit 1.01, tier 5, Legendary band [0.625, 1.25] → 0.385/0.625.
        assertEquals(0.616, quality(Rarity.LEGENDARY, PetStat.CRIT_CHANCE, 1.01, 5), 0.001);
    }

    // --- crit damage (post-rebalance scale) ----------------------------------

    @Test
    void legendaryCritDamageSpansSevenFiveToFifteen() {
        // Dégâts Critiques Legendary L10 band [7.5, 15] (the wiki "lvl 20" table
        // shows [15, 30] = these ×2).
        assertEquals(0.0, quality(Rarity.LEGENDARY, PetStat.CRIT_DAMAGE, 7.5, 10), 1e-9);
        assertEquals(0.5, quality(Rarity.LEGENDARY, PetStat.CRIT_DAMAGE, 11.25, 10), 1e-9);
        assertEquals(1.0, quality(Rarity.LEGENDARY, PetStat.CRIT_DAMAGE, 15.0, 10), 1e-9);
    }

    @Test
    void realLuffyCritDamageIsHighRoll() {
        // Real maxed pet: Dégâts Critiques 26.52 at tier 20, Legendary band
        // [15, 30] → 11.52/15 ≈ 0.768 (was wrongly 0% before the ÷2 fix).
        assertEquals(0.768, quality(Rarity.LEGENDARY, PetStat.CRIT_DAMAGE, 26.52, 20), 0.001);
    }

    @Test
    void mythicCritDamageSpansToEighteenSevenFive() {
        // Mythic L10 band [9.375, 18.75].
        assertEquals(1.0, quality(Rarity.MYTHIC, PetStat.CRIT_DAMAGE, 18.75, 10), 1e-9);
        assertEquals(0.0, quality(Rarity.MYTHIC, PetStat.CRIT_DAMAGE, 9.375, 10), 1e-9);
    }

    // --- edges ---------------------------------------------------------------

    @Test
    void belowMinimumClampsToZero() {
        // A roll under the floor (rounding/old data) clamps, never goes negative.
        assertEquals(0.0, quality(Rarity.LEGENDARY, PetStat.POWER, 8.0, 20), 1e-9);
    }

    @Test
    void specialRollFarAboveMaxIsOffTable() {
        // Server "special" effects ignore the table; we show no % for them.
        OptionalDouble q = PetStatEvaluator.quality(
                Rarity.LEGENDARY, new PetEffect(10, PetStat.POWER, 100.0));
        assertTrue(q.isEmpty());
    }
}
