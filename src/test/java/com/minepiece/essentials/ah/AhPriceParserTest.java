package com.minepiece.essentials.ah;

import com.minepiece.essentials.i18n.ServerText;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AhPriceParserTest {

    @Test
    void parsesAbbreviatedNumbers() {
        assertEquals(10_000_000d, AhPriceParser.parseAbbreviated("10M 实").orElseThrow(), 0.5);
        assertEquals(1_500_000d, AhPriceParser.parseAbbreviated("1.5M").orElseThrow(), 0.5);
        assertEquals(875_750d, AhPriceParser.parseAbbreviated("875.75K").orElseThrow(), 0.5);
        assertEquals(500_000d, AhPriceParser.parseAbbreviated("500K").orElseThrow(), 0.5);
        assertEquals(2_000_000_000d, AhPriceParser.parseAbbreviated("2B").orElseThrow(), 0.5);
        assertEquals(1234d, AhPriceParser.parseAbbreviated("1234").orElseThrow(), 0.5);
        assertTrue(AhPriceParser.parseAbbreviated("aucun prix").isEmpty());
    }

    @Test
    void formatsCompact() {
        assertEquals("1M", AhPriceParser.format(1_000_000));
        assertEquals("1.5M", AhPriceParser.format(1_500_000));
        assertEquals("112.5K", AhPriceParser.format(112_500));
        assertEquals("500K", AhPriceParser.format(500_000));
        assertEquals("750", AhPriceParser.format(750));
    }

    @Test
    void perUnitFromSellPriceLine() {
        // 2 candies at 2M -> 1M/u (the example from the request)
        assertEquals(Optional.of("1M"), AhPriceParser.perUnit(
                List.of(" ▪ Prix de vente: 2M 实", " ▪ Prix moyen: 1.8M"), 2));
        // 225K / 2 = 112.5K
        assertEquals(Optional.of("112.5K"), AhPriceParser.perUnit(
                List.of(" ▪ Prix de vente: 225K 实"), 2));
    }

    @Test
    void perUnitOfAveragePriceLine() {
        // "Prix moyen" 385.41K / 8 = 48.18K (the average per unit).
        assertEquals(Optional.of("48.18K"), AhPriceParser.perUnit(
                List.of(" ▪ Prix de vente: 400K 实", " ▪ Prix moyen: 385.41K"), 8, ServerText.AVG_PRICE));
    }

    @Test
    void noPerUnitForSingleItem() {
        assertTrue(AhPriceParser.perUnit(List.of(" ▪ Prix de vente: 2M 实"), 1).isEmpty());
    }

    @Test
    void ignoresAveragePriceLine() {
        // Must use "Prix de vente", never "Prix moyen".
        assertTrue(AhPriceParser.perUnit(List.of(" ▪ Prix moyen: 10M"), 5).isEmpty());
    }
}
