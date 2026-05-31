package com.minepiece.essentials.pet;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quality computation, validated against real maxed pets from the MinePiece
 * server (values read from item NBT).
 *
 * <p>Quality is {@code value / max(rarity, tier, stat)}, where the max comes
 * from the "Stats Pet Max" table (per rarity × level × stat).
 */
class PetStatEvaluatorTest {

    private static double quality(Rarity rarity, PetStat stat, double value, int tier) {
        OptionalDouble q = PetStatEvaluator.quality(rarity, new PetEffect(tier, stat, value));
        assertTrue(q.isPresent(), "expected a quality for " + stat);
        return q.getAsDouble();
    }

    @Test
    void trafalgarStrengthIsNearPerfect() {
        // Force 19.86 at tier 20 → /20 → ~0.993
        assertEquals(0.993, quality(Rarity.LEGENDARY, PetStat.STRENGTH, 19.86, 20), 0.002);
    }

    @Test
    void eustassStrengthIsHigh() {
        // Force 18.15 at tier 20 → /20 → ~0.9075
        assertEquals(0.9075, quality(Rarity.LEGENDARY, PetStat.STRENGTH, 18.15, 20), 0.002);
    }

    @Test
    void zoroHealthIsMid() {
        // Vie 105.65 at tier 15 → /150 → ~0.704
        assertEquals(0.704, quality(Rarity.LEGENDARY, PetStat.VITALITY, 105.65, 15), 0.002);
    }

    @Test
    void zoroCriticalChanceIsHigh() {
        // Chance Crit 3.1 at tier 15 → /3.75 → ~0.827
        assertEquals(0.827, quality(Rarity.LEGENDARY, PetStat.CRIT_CHANCE, 3.1, 15), 0.002);
    }

    @Test
    void luffyEnergyIsHigh() {
        // Énergie 180.24 at tier 20 → /200 → ~0.901
        assertEquals(0.901, quality(Rarity.LEGENDARY, PetStat.ENERGY, 180.24, 20), 0.002);
    }

    @Test
    void luffyPowerIsNearPerfect() {
        // Puissance 19.85 at tier 20 → /20 → ~0.9925 (this was the "0%" bug)
        assertEquals(0.9925, quality(Rarity.LEGENDARY, PetStat.POWER, 19.85, 20), 0.002);
    }

    @Test
    void luffyCriticalDamageIsHigh() {
        // Dégâts Critiques 26.52 at tier 20 → /30 → ~0.884 (no longer off-table)
        assertEquals(0.884, quality(Rarity.LEGENDARY, PetStat.CRIT_DAMAGE, 26.52, 20), 0.002);
    }

    @Test
    void perfectRollIsHundredPercent() {
        assertEquals(1.0, quality(Rarity.LEGENDARY, PetStat.POWER, 20.0, 20), 1e-9);
    }

    @Test
    void usoppCriticalChanceAtTier5() {
        // Chance Critique 1.01 at tier 5 (Legendary) → max 2.5×5/10 = 1.25 → ~0.808
        assertEquals(0.808, quality(Rarity.LEGENDARY, PetStat.CRIT_CHANCE, 1.01, 5), 0.003);
    }

    @Test
    void maxScalesLinearlyWithTier() {
        // A perfect roll is the L10 value × tier/10 for any tier.
        assertEquals(1.0, quality(Rarity.MYTHIC, PetStat.CRIT_DAMAGE, 3.125, 5), 1e-9);
        assertEquals(1.0, quality(Rarity.MYTHIC, PetStat.CRIT_DAMAGE, 12.5, 20), 1e-9);
    }
}
