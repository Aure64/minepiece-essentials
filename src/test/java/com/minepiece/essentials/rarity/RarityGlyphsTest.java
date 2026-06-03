package com.minepiece.essentials.rarity;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RarityGlyphsTest {

    @Test
    void detectsEachRarityFromItsGlyph() {
        for (ItemRarity r : ItemRarity.values()) {
            assertEquals(r, RarityGlyphs.scan(String.valueOf((char) r.glyph) + " TEXTE"));
        }
    }

    @Test
    void returnsNullWhenNoGlyph() {
        assertNull(RarityGlyphs.scan("Bouche de Smack"));
        assertNull(RarityGlyphs.scan(""));
        assertNull(RarityGlyphs.scan(null));
    }

    @Test
    void scanLinesReturnsFirstMatch() {
        List<String> lines = List.of("ligne sans glyphe", "伴 LEGENDAIRE", "愈 MYTHIQUE");
        assertEquals(ItemRarity.LEGENDARY, RarityGlyphs.scanLines(lines));
    }

    @Test
    void scanLinesNullWhenNoneMatch() {
        assertNull(RarityGlyphs.scanLines(List.of("a", "b")));
    }
}
