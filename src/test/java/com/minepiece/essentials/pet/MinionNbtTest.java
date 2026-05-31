package com.minepiece.essentials.pet;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Extraction of minion progression fields from the squidcore custom_data SNBT. */
class MinionNbtTest {

    // Representative slice of a real pet's custom_data (Jack, prestige 8).
    private static final String JACK =
        "{\"squidcore:pet_components_progression-minion_level\":{\"squidcore:type\":\"string\",\"squidcore:value\":\"8\"},"
      + "\"squidcore:pet_components_progression-minion_xp\":{\"squidcore:type\":\"string\",\"squidcore:value\":\"1881.0\"},"
      + "\"squidcore:pet_components_progression-minion_next-xp\":{\"squidcore:type\":\"string\",\"squidcore:value\":\"64000.0\"},"
      + "\"squidcore:custom_pets_minion-food-name\":{\"squidcore:type\":\"component-wrapper\",\"squidcore:value\":'{\"italic\":false,\"text\":\"%i18n_pets.minion-food.minion-food-name.value.tracks.==NETHER_WART%\"}'}}";

    @Test
    void parsesAllMinionFields() {
        Optional<MinionData> d = MinionNbt.parse(JACK);
        assertTrue(d.isPresent());
        assertEquals(8, d.get().prestige());
        assertEquals(1881.0, d.get().currentXp(), 1e-6);
        assertEquals(64000.0, d.get().nextXp(), 1e-6);
        assertEquals("NETHER_WART", d.get().foodToken());
    }

    @Test
    void minionXpDoesNotMatchNextXpField() {
        // The minion_xp regex must not accidentally read the next-xp value.
        assertEquals(1881.0, MinionNbt.parse(JACK).orElseThrow().currentXp(), 1e-6);
    }

    @Test
    void returnsEmptyWhenNoMinionData() {
        assertTrue(MinionNbt.parse("{\"some\":\"other\"}").isEmpty());
    }

    @Test
    void mapsFoodTokenToItemId() {
        assertEquals("minecraft:nether_wart", MinionData.tokenToItemId("NETHER_WART"));
        assertEquals("minecraft:iron_ingot", MinionData.tokenToItemId("IRON_INGOT"));
    }
}
