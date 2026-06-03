package com.minepiece.essentials.rarity;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/** Détection de rareté par recherche du codepoint glyphe (langue-agnostique). */
public final class RarityGlyphs {
    private RarityGlyphs() {}

    @Nullable
    public static ItemRarity fromCodepoint(int cp) {
        for (ItemRarity r : ItemRarity.values()) {
            if (r.glyph == cp) return r;
        }
        return null;
    }

    @Nullable
    public static ItemRarity scan(@Nullable String s) {
        if (s == null || s.isEmpty()) return null;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            ItemRarity r = fromCodepoint(cp);
            if (r != null) return r;
            i += Character.charCount(cp);
        }
        return null;
    }

    @Nullable
    public static ItemRarity scanLines(@Nullable List<String> lines) {
        if (lines == null) return null;
        for (String line : lines) {
            ItemRarity r = scan(line);
            if (r != null) return r;
        }
        return null;
    }
}
