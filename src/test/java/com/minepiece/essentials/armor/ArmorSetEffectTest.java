package com.minepiece.essentials.armor;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmorSetEffectTest {

    private static String en(List<String> l) { return ArmorSetEffect.summarize(l, "XP", "Profile XP"); }
    private static String fr(List<String> l) { return ArmorSetEffect.summarize(l, "XP", "XP profil"); }

    @Test
    void xpOnProfileFollowsTheModLanguage() {
        List<String> text = List.of("Get a bonus of xp of 10%", "on your profile by wearing", "the full Tamago armor.");
        assertEquals("Profile XP +10%", en(text));
        assertEquals("XP profil +10%", fr(text));
        assertEquals("XP profil +10%", fr(List.of("Gagne 10% d'xp sur ton profil", "en portant l'armure Tamago complète.")));
    }

    @Test
    void statKeywordKeepsTheServerWording() {
        assertEquals("Strength +25", en(List.of("Gain +25 strength while wearing the full set.")));
        assertEquals("Force +25", fr(List.of("Gagne +25 force avec le set complet.")));
    }

    @Test
    void emptyAndNoNumber() {
        assertEquals("", en(List.of()));
        assertEquals("Immune to poison.", en(List.of("Immune to poison.")));
    }
}
