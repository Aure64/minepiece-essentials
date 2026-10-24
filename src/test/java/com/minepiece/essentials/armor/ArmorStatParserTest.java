package com.minepiece.essentials.armor;

import com.minepiece.essentials.pet.PetStat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Parsing of an armor piece's rendered name + lore (format seen on Tamago's set, 2026-10-24). */
class ArmorStatParserTest {

    private static final List<String> HAT = List.of(
        "Tamago's Hat",
        "犹 Health +110",
        "乘 Defense +17",
        "ъ Energy Regeneration +12",
        "ы Power +9",
        "",
        "Ъ Full Armor Effect",
        "Get a bonus xp of 10%",
        "on your profile by wearing",
        "the complete set of Tamago's armor.",
        "",
        "Ы Obtaining",
        "This hat can be obtained",
        "in the Chocolate Crate."
    );

    @Test
    void parsesStatsInLoreOrder() {
        ArmorPiece p = ArmorStatParser.parse(HAT).orElseThrow();
        assertEquals("Tamago's Hat", p.name());
        assertEquals(110, p.stats().get(PetStat.VITALITY), 1e-9);
        assertEquals(17, p.stats().get(PetStat.DEFENSE), 1e-9);
        assertEquals(12, p.stats().get(PetStat.ENERGY_REGEN), 1e-9);
        assertEquals(9, p.stats().get(PetStat.POWER), 1e-9);
        assertEquals(4, p.stats().size());
        assertEquals("Health", p.labels().get(PetStat.VITALITY));
        assertEquals("Energy Regeneration", p.labels().get(PetStat.ENERGY_REGEN));
    }

    @Test
    void readsSetNameAndEffectFromFullArmorParagraph() {
        ArmorPiece p = ArmorStatParser.parse(HAT).orElseThrow();
        assertEquals("Tamago", p.setName());
        assertEquals(3, p.setEffect().size());
        assertEquals("Get a bonus xp of 10%", p.setEffect().get(0));
    }

    @Test
    void parsesVestWithCriticalHitChanceAndDecimalValue() {
        List<String> vest = List.of(
            "Tamago Vest",
            "犹 Health +110",
            "乘 Defense +17",
            "ъ Dexterity +12",
            "ы Critical Hit Chance +4.5",
            "",
            "Ъ Full Armor Effect",
            "Gain a xp bonus of 10%",
            "on your profile while wearing",
            "the entire Tamago armor.");
        ArmorPiece p = ArmorStatParser.parse(vest).orElseThrow();
        assertEquals(4.5, p.stats().get(PetStat.CRIT_CHANCE), 1e-9);
        assertEquals(12, p.stats().get(PetStat.DEXTERITY), 1e-9);
        assertEquals("Tamago", p.setName());
    }

    @Test
    void fallsBackToFirstWordOfNameWithoutSetParagraph() {
        ArmorPiece p = ArmorStatParser.parse(List.of("Marine's Boots", "犹 Health +10")).orElseThrow();
        assertEquals("Marine", p.setName());
        assertTrue(p.setEffect().isEmpty());
    }

    @Test
    void rejectsItemsWithoutStatLines() {
        assertEquals(Optional.empty(), ArmorStatParser.parse(List.of("Diamond Helmet")));
        assertEquals(Optional.empty(), ArmorStatParser.parse(List.of("Parchemin", "Objectif: tuer 10 mobs")));
        assertEquals(Optional.empty(), ArmorStatParser.parse(List.of("Fruit", "", "犹 Health +10")));
    }

    @Test
    void frenchLabelsAndCommaDecimals() {
        ArmorPiece p = ArmorStatParser.parse(List.of(
            "Veste de Tamago",
            "犹 Vie +110",
            "ы Chance Critique +4,5",
            "",
            "Ъ Effet d'armure complète",
            "Gagne 10% d'xp en portant",
            "l'armure Tamago complète.")).orElseThrow();
        assertEquals(110, p.stats().get(PetStat.VITALITY), 1e-9);
        assertEquals(4.5, p.stats().get(PetStat.CRIT_CHANCE), 1e-9);
        assertEquals("Tamago", p.setName());
    }
}
