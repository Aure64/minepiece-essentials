package com.minepiece.essentials.config;

import com.minepiece.essentials.util.ColorUtils;

/**
 * A HUD panel background style, chosen per element in the HUD editor. Each preset
 * bundles its fill, border and neutral-text colours so that text stays readable
 * whatever the background (dark/transparent presets switch to light text).
 *
 * <p>Persisted by name in {@link LayoutConfig.ElementLayout}; unknown/missing
 * values fall back to {@link #PARCHMENT}.
 */
public enum HudBackground {

    /** The classic parchment look: tan fill, gold border, dark text. */
    PARCHMENT("Parchemin", ColorUtils.PARCHMENT_BG, ColorUtils.PARCHMENT_BORDER, ColorUtils.TEXT_DARK),

    /** Translucent black panel with a gold border and light text. */
    DARK("Sombre translucide", 0xC0000000, ColorUtils.PARCHMENT_BORDER, ColorUtils.TEXT_LIGHT),

    /** No panel at all — just light text (with the usual shadow). */
    TRANSPARENT("Transparent", 0x00000000, 0x00000000, ColorUtils.TEXT_LIGHT);

    private final String label;
    private final int bgColor;
    private final int borderColor;
    private final int textColor;

    HudBackground(String label, int bgColor, int borderColor, int textColor) {
        this.label = label;
        this.bgColor = bgColor;
        this.borderColor = borderColor;
        this.textColor = textColor;
    }

    /** Human-facing French name, shown in the HUD editor. */
    public String label() {
        return label;
    }

    public int bgColor() {
        return bgColor;
    }

    public int borderColor() {
        return borderColor;
    }

    /** Colour for neutral body/title text on this background. */
    public int textColor() {
        return textColor;
    }

    /** The next preset, wrapping around — used to cycle in the editor. */
    public HudBackground next() {
        HudBackground[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** Parses a stored name, falling back to {@link #PARCHMENT} for unknown/null. */
    public static HudBackground fromName(String name) {
        if (name == null) return PARCHMENT;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return PARCHMENT;
        }
    }
}
