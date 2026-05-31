package com.minepiece.essentials.pet;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Parsing of rendered "Familier Effects" stat lines from the /pets tooltip. */
class PetEffectParserTest {

    @Test
    void parsesTier15HealthLine() {
        Optional<PetEffect> e = PetEffectParser.parse("S.1.5.E 犹 Vie + 105.65");
        assertTrue(e.isPresent());
        assertEquals(15, e.get().tier());
        assertEquals(PetStat.VITALITY, e.get().stat());
        assertEquals(105.65, e.get().value(), 1e-6);
    }

    @Test
    void parsesTier20StrengthLine() {
        Optional<PetEffect> e = PetEffectParser.parse("S.2.0.E 乘 Force + 19.86");
        assertTrue(e.isPresent());
        assertEquals(20, e.get().tier());
        assertEquals(PetStat.STRENGTH, e.get().stat());
        assertEquals(19.86, e.get().value(), 1e-6);
    }

    @Test
    void parsesTier10Level() {
        Optional<PetEffect> e = PetEffectParser.parse("S.1.0.E 厚 Puissance + 13.82");
        assertTrue(e.isPresent());
        assertEquals(10, e.get().tier());
        assertEquals(PetStat.POWER, e.get().stat());
    }

    @Test
    void distinguishesCriticalDamageFromCriticalChance() {
        assertEquals(PetStat.CRIT_DAMAGE,
            PetEffectParser.parse("S.2.0.E 绍 Dégâts Critiques + 18.17").orElseThrow().stat());
        assertEquals(PetStat.CRIT_CHANCE,
            PetEffectParser.parse("S.1.5.E 奖 Chance Critique + 3.1").orElseThrow().stat());
    }

    @Test
    void rejectsMoneyLine() {
        assertTrue(PetEffectParser.parse("S.1.E 5.500 实/h").isEmpty());
    }

    @Test
    void rejectsUndiscoveredLine() {
        assertTrue(PetEffectParser.parse("S.2.0.E t (Non découvert)").isEmpty());
    }

    @Test
    void rejectsSectionHeader() {
        assertTrue(PetEffectParser.parse("Familier Effects").isEmpty());
    }
}
