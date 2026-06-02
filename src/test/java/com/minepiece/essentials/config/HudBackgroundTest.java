package com.minepiece.essentials.config;

import com.minepiece.essentials.util.ColorUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudBackgroundTest {

    @Test
    void cyclesParchmentDarkTransparentAndWraps() {
        assertEquals(HudBackground.DARK, HudBackground.PARCHMENT.next());
        assertEquals(HudBackground.TRANSPARENT, HudBackground.DARK.next());
        assertEquals(HudBackground.PARCHMENT, HudBackground.TRANSPARENT.next());
    }

    @Test
    void parchmentKeepsDarkText() {
        assertEquals(ColorUtils.TEXT_DARK, HudBackground.PARCHMENT.textColor());
    }

    @Test
    void darkAndTransparentUseLightText() {
        // So neutral text stays readable once the parchment is gone.
        assertEquals(ColorUtils.TEXT_LIGHT, HudBackground.DARK.textColor());
        assertEquals(ColorUtils.TEXT_LIGHT, HudBackground.TRANSPARENT.textColor());
    }

    @Test
    void transparentDrawsNoFillOrBorder() {
        assertEquals(0, HudBackground.TRANSPARENT.bgColor() >>> 24, "fill must be fully transparent");
        assertEquals(0, HudBackground.TRANSPARENT.borderColor() >>> 24, "border must be fully transparent");
    }

    @Test
    void darkIsTranslucentNotOpaque() {
        int alpha = HudBackground.DARK.bgColor() >>> 24;
        assertTrue(alpha > 0 && alpha < 255, "dark preset should be semi-transparent, got alpha=" + alpha);
    }
}
