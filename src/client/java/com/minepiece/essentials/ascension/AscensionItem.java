package com.minepiece.essentials.ascension;

/**
 * An ascendable item (fruit or weapon) read from the player's inventory.
 *
 * @param type      fruit or weapon
 * @param name      display name (without the trailing "(Niveau N)")
 * @param rarity    rarity token (e.g. "legendary", "nightmare"); may be null
 * @param level     current level
 * @param ascension number of ascensions already done
 * @param xp        current xp toward the next level
 * @param maxXp     xp needed for the next level (0 when an ascension is available)
 * @param available whether the next ascension is available now
 * @param color     ARGB display colour (auto-detected from the item name, else by rarity)
 */
public record AscensionItem(Type type, String name, String rarity, int level, int ascension,
                            int xp, int maxXp, boolean available, int color) {
    public enum Type { FRUIT, WEAPON }

    /** XP left before the next level (0 when an ascension is available). */
    public int xpToNextLevel() {
        return Math.max(0, maxXp - xp);
    }

    /** Same item with the display colour overridden (e.g. the in-game name colour). */
    public AscensionItem withColor(int newColor) {
        return new AscensionItem(type, name, rarity, level, ascension, xp, maxXp, available, newColor);
    }
}
