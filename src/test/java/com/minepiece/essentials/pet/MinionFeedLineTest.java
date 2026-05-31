package com.minepiece.essentials.pet;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Parsing of the feeding-screen line, e.g. {@code "Andesite x64 - 128.0 Exp"}. */
class MinionFeedLineTest {

    @Test
    void parsesAndesiteRatioTwo() {
        Optional<MinionFeedLine.Feed> f = MinionFeedLine.parse("Andesite x64 - 128.0 Exp");
        assertTrue(f.isPresent());
        assertEquals("Andesite", f.get().resourceName());
        assertEquals(64, f.get().count());
        assertEquals(128.0, f.get().totalExp(), 1e-6);
        assertEquals(2.0, f.get().xpPerItem(), 1e-6);
    }

    @Test
    void parsesNetherWartRatioOne() {
        Optional<MinionFeedLine.Feed> f = MinionFeedLine.parse("Nether Wart x13 - 13.0 Exp");
        assertTrue(f.isPresent());
        assertEquals("Nether Wart", f.get().resourceName());
        assertEquals(1.0, f.get().xpPerItem(), 1e-6);
    }

    @Test
    void rejectsTotalLine() {
        assertTrue(MinionFeedLine.parse("Total: 128.0").isEmpty());
    }

    @Test
    void rejectsArbitraryLine() {
        assertTrue(MinionFeedLine.parse("Nourrir votre Minion").isEmpty());
    }
}
