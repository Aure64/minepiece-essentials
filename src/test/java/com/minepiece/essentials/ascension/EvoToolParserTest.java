package com.minepiece.essentials.ascension;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Evolving tool tooltip (Hoe of Monkey D. Luffy, 2026-10-24): stats, a "Progression" bar, rarity. */
class EvoToolParserTest {

    @Test
    void parsesProgressionBarUnderHeader() {
        AscensionItem t = EvoToolParser.parse(List.of(
            "Hoe of Monkey D. Luffy",
            "",
            "ъ Productivity +3",
            "ы Fortune +1",
            "",
            "Ъ Progression",
            "0 ▮▮▮▮▮▮▮▮▮▮ 5000",
            "",
            "Ы LEGENDARY")).orElseThrow();
        assertEquals(AscensionItem.Type.TOOL, t.type());
        assertEquals("Hoe of Monkey D. Luffy", t.name());
        assertEquals(0, t.xp());
        assertEquals(5000, t.maxXp());
        assertFalse(t.available());
        assertEquals(5000, t.xpToNextLevel());
    }

    @Test
    void readyWhenBarIsFull() {
        AscensionItem t = EvoToolParser.parse(List.of("Pioche de Luffy", "Ъ Progression", "5 000 ▮▮▮ 5 000")).orElseThrow();
        assertEquals(5000, t.xp());
        assertTrue(t.available());
    }

    @Test
    void ignoresItemsWithoutProgression() {
        assertTrue(EvoToolParser.parse(List.of("Tamago's Hat", "犹 Health +110")).isEmpty());
        assertTrue(EvoToolParser.parse(List.of("Hoe", "Ъ Progression", "(aucune donnée)")).isEmpty());
    }
}
