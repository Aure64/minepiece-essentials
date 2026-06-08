package com.minepiece.essentials.ah;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Classement de la bande de prix AH (logique pure). */
class AhPriceBandTest {

    @Test
    void aboveTenPercentIsExpensive() {
        AhPriceBand.Result r = AhPriceBand.of(11_500_000, 10_000_000).orElseThrow();
        assertEquals(AhPriceBand.Band.EXPENSIVE, r.band());
        assertEquals(15, r.percent());
    }

    @Test
    void belowMinusTenPercentIsCheap() {
        AhPriceBand.Result r = AhPriceBand.of(8_500_000, 10_000_000).orElseThrow();
        assertEquals(AhPriceBand.Band.CHEAP, r.band());
        assertEquals(-15, r.percent());
    }

    @Test
    void withinBandIsFair() {
        assertEquals(AhPriceBand.Band.FAIR, AhPriceBand.of(10_000_000, 10_000_000).orElseThrow().band());
        assertEquals(AhPriceBand.Band.FAIR, AhPriceBand.of(10_400_000, 10_000_000).orElseThrow().band());
    }

    @Test
    void exactBoundariesAreFair() {
        // +10% et -10% pile sont inclus dans le vert.
        assertEquals(AhPriceBand.Band.FAIR, AhPriceBand.of(11_000_000, 10_000_000).orElseThrow().band());
        assertEquals(AhPriceBand.Band.FAIR, AhPriceBand.of(9_000_000, 10_000_000).orElseThrow().band());
    }

    @Test
    void zeroOrMissingAverageHasNoBand() {
        assertTrue(AhPriceBand.of(10_000_000, 0).isEmpty());
        assertTrue(AhPriceBand.of(0, 10_000_000).isEmpty());
    }

    @Test
    void fromLoreReadsBothPrices() {
        List<String> lore = List.of(
            "Sabre légendaire",
            " ▪ Prix de vente: 11.5M 实",
            " ▪ Prix moyen: 10M 实");
        AhPriceBand.Result r = AhPriceBand.fromLore(lore).orElseThrow();
        assertEquals(AhPriceBand.Band.EXPENSIVE, r.band());
        assertEquals(15, r.percent());
    }

    @Test
    void fromLoreEmptyWhenAverageMissing() {
        List<String> lore = List.of(" ▪ Prix de vente: 11.5M 实");
        assertTrue(AhPriceBand.fromLore(lore).isEmpty());
    }

    @Test
    void fromLoreReadsEnglishPrices() {
        java.util.List<String> lore = java.util.List.of(
            "Legendary sword",
            " ▪ Selling price: 11.5M 实",
            " ▪ Average price: 10M");
        AhPriceBand.Result r = AhPriceBand.fromLore(lore).orElseThrow();
        assertEquals(AhPriceBand.Band.EXPENSIVE, r.band());
        assertEquals(15, r.percent());
    }
}
