package com.minepiece.essentials.ah;

import com.minepiece.essentials.config.ModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Couleurs configurables des bandes de prix AH (logique pure). */
class AhPriceColorsTest {

    @Test
    void defaultsMatchHistoricalColors() {
        ModConfig cfg = new ModConfig();
        assertEquals(0xFFFFD24B, AhPriceColors.of(AhPriceBand.Band.CHEAP, cfg));
        assertEquals(0xFF7CFC55, AhPriceColors.of(AhPriceBand.Band.FAIR, cfg));
        assertEquals(0xFFFF5555, AhPriceColors.of(AhPriceBand.Band.EXPENSIVE, cfg));
    }

    @Test
    void parseAcceptsCommonHexForms() {
        assertEquals(0xFF123456, AhPriceColors.parse("#123456", "#000000"));
        assertEquals(0xFF123456, AhPriceColors.parse("123456", "#000000"));
        assertEquals(0xFF123456, AhPriceColors.parse("0x123456", "#000000"));
        assertEquals(0xFFABCDEF, AhPriceColors.parse(" #abcdef ", "#000000"));
        assertEquals("#ABCDEF", AhPriceColors.normalize("abcdef"));
        assertEquals("#00AAFF", AhPriceColors.toHex(0xFF00AAFF));
    }

    @Test
    void unreadableValueFallsBackToDefault() {
        assertEquals(0xFFFFD24B, AhPriceColors.parse("rouge", AhPriceColors.DEFAULT_CHEAP));
        assertEquals(0xFFFFD24B, AhPriceColors.parse(null, AhPriceColors.DEFAULT_CHEAP));
        assertEquals(0xFFFFD24B, AhPriceColors.parse("#FFF", AhPriceColors.DEFAULT_CHEAP));
        assertFalse(AhPriceColors.isValid("#FFF"));
        assertNull(AhPriceColors.normalize("zzz"));
        ModConfig cfg = new ModConfig();
        cfg.ahColorFair = "n'importe quoi";
        assertEquals(0xFF7CFC55, AhPriceColors.of(AhPriceBand.Band.FAIR, cfg));
    }

    @Test
    void setIgnoresInvalidAndNormalizesValid() {
        ModConfig cfg = new ModConfig();
        AhPriceColors.set(AhPriceBand.Band.CHEAP, "pas une couleur", cfg);
        assertEquals(AhPriceColors.DEFAULT_CHEAP, cfg.ahColorCheap);
        AhPriceColors.set(AhPriceBand.Band.CHEAP, "00aaff", cfg);
        assertEquals("#00AAFF", cfg.ahColorCheap);
        assertEquals(0xFF00AAFF, AhPriceColors.of(AhPriceBand.Band.CHEAP, cfg));
    }

    @Test
    void swapAndResetCheapFair() {
        ModConfig cfg = new ModConfig();
        AhPriceColors.swapCheapFair(cfg);
        assertEquals(0xFF7CFC55, AhPriceColors.of(AhPriceBand.Band.CHEAP, cfg));
        assertEquals(0xFFFFD24B, AhPriceColors.of(AhPriceBand.Band.FAIR, cfg));
        assertEquals(0xFFFF5555, AhPriceColors.of(AhPriceBand.Band.EXPENSIVE, cfg));
        AhPriceColors.resetDefaults(cfg);
        assertEquals(AhPriceColors.DEFAULT_CHEAP, cfg.ahColorCheap);
        assertEquals(AhPriceColors.DEFAULT_FAIR, cfg.ahColorFair);
    }
}
