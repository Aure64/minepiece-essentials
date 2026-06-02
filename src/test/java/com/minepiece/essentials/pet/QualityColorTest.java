package com.minepiece.essentials.pet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Official roll-quality colour thresholds: red below 50%, yellow 50–80%,
 * green 80% and up.
 */
class QualityColorTest {

    @Test
    void belowFiftyIsRed() {
        assertEquals(0xFF5555, QualityColor.of(0.0));
        assertEquals(0xFF5555, QualityColor.of(0.499));
    }

    @Test
    void fiftyToEightyIsYellow() {
        assertEquals(0xFFFF55, QualityColor.of(0.50));
        assertEquals(0xFFFF55, QualityColor.of(0.799));
    }

    @Test
    void atOrAboveEightyIsGreen() {
        assertEquals(0x55FF55, QualityColor.of(0.80));
        assertEquals(0x55FF55, QualityColor.of(1.0));
    }
}
