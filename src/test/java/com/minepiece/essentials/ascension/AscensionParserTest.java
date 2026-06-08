package com.minepiece.essentials.ascension;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Parsing fruit/weapon ascension info from squidcore item NBT. */
class AscensionParserTest {

    private static final String FRUIT_NBT =
        "{\"items:experience-xp\":\"89:0:0\","
      + "\"items:internal-name\":\"nightmare-fruit\","
      + "\"squidcore:custom_items_global-rarity-lore\":{\"squidcore:value\":\"%i18n.global-rarity-lore.value.tracks.==nightmare%\"},"
      + "\"squidcore:items_experience_ascension\":{\"squidcore:type\":\"string\",\"squidcore:value\":\"5\"}}";

    private static final String WEAPON_NBT =
        "{\"items:experience-xp\":\"1:0:450\","
      + "\"items:internal-name\":\"dressrosa_weapon_doflamingo_gun\","
      + "\"squidcore:custom_items_global-rarity-lore\":{\"squidcore:value\":\"%i18n.global-rarity-lore.value.tracks.==legendary%\"},"
      + "\"squidcore:items_experience_ascension\":{\"squidcore:type\":\"string\",\"squidcore:value\":\"0\"},"
      + "\"stats:damage\":10.4d,\"stats:has_stats\":1b}";

    private static final String MAXED_WEAPON_NBT =
        "{\"items:experience-xp\":\"89:0:0\",\"items:internal-name\":\"alabasta-crocodile-hook\","
      + "\"squidcore:items_experience_ascension\":{\"squidcore:type\":\"string\",\"squidcore:value\":\"5\"},"
      + "\"stats:damage\":50.0d}";

    @Test
    void parsesFruitWithAscensionAvailable() {
        AscensionItem item = AscensionParser.parse("Fruit du Nightmare (Niveau 89)", FRUIT_NBT).orElseThrow();
        assertEquals(AscensionItem.Type.FRUIT, item.type());
        assertEquals(89, item.level());
        assertEquals(5, item.ascension());
        assertTrue(item.available());
        assertEquals("Fruit du Nightmare", item.name(), "should strip the (Niveau N) suffix");
        assertEquals("nightmare", item.rarity());
    }

    @Test
    void parsesWeaponRarity() {
        assertEquals("legendary", AscensionParser.parse("x", WEAPON_NBT).orElseThrow().rarity());
    }

    @Test
    void parsesWeaponNotYetAvailable() {
        AscensionItem item = AscensionParser.parse("Pistolet de Doflamingo", WEAPON_NBT).orElseThrow();
        assertEquals(AscensionItem.Type.WEAPON, item.type());
        assertEquals(1, item.level());
        assertEquals(450, item.xpToNextLevel(), "maxXp 450 - xp 0");
        assertFalse(item.available());
    }

    @Test
    void weaponWithMaxXpZeroIsAvailable() {
        assertTrue(AscensionParser.parse("Crochet de Crocodile", MAXED_WEAPON_NBT).orElseThrow().available());
    }

    @Test
    void typeIsWeaponWhenItHasDamageStat() {
        assertEquals(AscensionItem.Type.WEAPON, AscensionParser.parse("x", WEAPON_NBT).orElseThrow().type());
        assertEquals(AscensionItem.Type.FRUIT, AscensionParser.parse("x", FRUIT_NBT).orElseThrow().type());
    }

    // Usopp's Kabuto is a ranged weapon with no stats:damage component — it used to
    // fall through to FRUIT. Type is now decided by the (non-fruit) name.
    private static final String KABUTO_NBT =
        "{\"items:experience-xp\":\"89:0:0\",\"items:internal-name\":\"skypiea-usopp-kabuto\","
      + "\"squidcore:items_experience_ascension\":{\"squidcore:type\":\"string\",\"squidcore:value\":\"4\"}}";

    @Test
    void rangedWeaponWithoutDamageStatIsWeapon() {
        AscensionItem item = AscensionParser.parse("Kabuto Noir d'Usopp (Niveau 89)", KABUTO_NBT).orElseThrow();
        assertEquals(AscensionItem.Type.WEAPON, item.type());
    }

    @Test
    void fruitNamedItemIsFruitEvenWithoutInternalName() {
        String nbt = "{\"items:experience-xp\":\"19:0:0\"}";
        assertEquals(AscensionItem.Type.FRUIT,
            AscensionParser.parse("Fruit du Château", nbt).orElseThrow().type());
    }

    @Test
    void stripsEnglishLevelSuffix() {
        AscensionItem item = AscensionParser.parse("Fruit du Nightmare (Level 89)", FRUIT_NBT).orElseThrow();
        assertEquals("Fruit du Nightmare", item.name(), "should strip the (Level N) suffix too");
    }

    @Test
    void nonAscendableItemReturnsEmpty() {
        Optional<AscensionItem> item = AscensionParser.parse("Cristal",
            "{\"items:internal-name\":\"crystal\",\"items:rarity\":\"rare\"}");
        assertTrue(item.isEmpty());
    }
}
