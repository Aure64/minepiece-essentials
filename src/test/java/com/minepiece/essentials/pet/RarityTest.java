package com.minepiece.essentials.pet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Rarity extracted from the squidcore NBT track tokens (e.g. {@code tracks.==LEGENDARY}). */
class RarityTest {

    @Test
    void parsesKnownTrackTokens() {
        assertEquals(Rarity.COMMON, Rarity.fromTrack("COMMON"));
        assertEquals(Rarity.RARE, Rarity.fromTrack("RARE"));
        assertEquals(Rarity.EPIC, Rarity.fromTrack("EPIC"));
        assertEquals(Rarity.LEGENDARY, Rarity.fromTrack("LEGENDARY"));
        assertEquals(Rarity.MYTHIC, Rarity.fromTrack("MYTHIC"));
    }

    @Test
    void returnsNullForUnknownToken() {
        assertNull(Rarity.fromTrack("BOGUS"));
    }
}
