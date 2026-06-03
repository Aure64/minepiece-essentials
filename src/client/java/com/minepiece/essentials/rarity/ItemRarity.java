package com.minepiece.essentials.rarity;

import net.minecraft.util.Identifier;

/**
 * Raretés MinePiece. Le glyphe est le codepoint que le resource pack serveur mappe
 * vers l'emblème (présent dans le nom/lore de l'item). La texture est celle du pack
 * (namespace "fonts"), référencée — jamais bundlée. `night` est volontairement absent
 * (codepoint inconnu, cf. spec — à ajouter après dump en jeu).
 */
public enum ItemRarity {
    COMMON   ('孔', "common",    11, 10, 0xFFFFFFFF, 0),
    RARE     ('桥', "rare",      11, 10, 0xFF5599FF, 1),
    EPIC     ('恨', "epic",      11, 10, 0xFFB24BFF, 2),
    LEGENDARY('伴', "legendary", 11, 10, 0xFFFFAA00, 3),
    MYTHIC   ('愈', "mythic",    11, 11, 0xFFFF5577, 4),
    LUNAR    ('灰', "lunar",     12, 10, 0xFF7FE0FF, 5),
    VALENTINE('挑', "valentine", 11, 11, 0xFFFF8FC8, 6);

    public final int glyph;
    public final String key;
    public final int nativeW;
    public final int nativeH;
    public final int color;   // ARGB, pour le bouton de filtre
    public final int rank;    // ordre de tri

    ItemRarity(char glyph, String key, int nativeW, int nativeH, int color, int rank) {
        this.glyph = glyph;
        this.key = key;
        this.nativeW = nativeW;
        this.nativeH = nativeH;
        this.color = color;
        this.rank = rank;
    }

    public Identifier texture() {
        return Identifier.of("fonts", "textures/font/rarity/items/icon/" + key + "_icons.png");
    }
}
