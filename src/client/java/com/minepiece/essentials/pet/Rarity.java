package com.minepiece.essentials.pet;

/** Pet rarity tiers, as named in the squidcore item NBT track tokens. */
public enum Rarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY,
    MYTHIC;

    /** Parses a track token such as {@code "LEGENDARY"} (case-insensitive); null if unknown. */
    public static Rarity fromTrack(String token) {
        if (token == null) return null;
        for (Rarity r : values()) {
            if (r.name().equalsIgnoreCase(token.trim())) return r;
        }
        return null;
    }
}
