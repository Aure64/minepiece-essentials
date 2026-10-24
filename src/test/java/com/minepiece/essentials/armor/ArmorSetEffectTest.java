package com.minepiece.essentials.armor;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmorSetEffectTest {

    @Test
    void xpOnProfile() {
        assertEquals("XP profil +10%", ArmorSetEffect.summarize(List.of(
            "Get a bonus of xp of 10%", "on your profile by wearing", "the full Tamago armor.")));
        assertEquals("XP profil +10%", ArmorSetEffect.summarize(List.of(
            "Gagne 10% d'xp sur ton profil", "en portant l'armure Tamago complète.")));
    }

    @Test
    void statBonus() {
        assertEquals("Force +25", ArmorSetEffect.summarize(List.of("Gain +25 strength while wearing the full set.")));
    }

    @Test
    void emptyAndNoNumber() {
        assertEquals("", ArmorSetEffect.summarize(List.of()));
        assertEquals("Immune to poison.", ArmorSetEffect.summarize(List.of("Immune to poison.")));
    }
}
