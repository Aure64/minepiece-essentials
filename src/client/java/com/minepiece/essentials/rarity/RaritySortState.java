package com.minepiece.essentials.rarity;

/**
 * Sens de tri par bouton, basculé à chaque clic. Les deux boutons trient par rareté ;
 * RARITY_ALPHA range en plus alphabétiquement (A→Z) dans chaque rareté.
 * Défaut : décroissant (mythique d'abord).
 */
public final class RaritySortState {
    private boolean rarityDescending = true;
    private boolean rarityAlphaDescending = true;

    public boolean rarityDescending() { return rarityDescending; }
    public boolean rarityAlphaDescending() { return rarityAlphaDescending; }

    public void toggleRarity() { rarityDescending = !rarityDescending; }
    public void toggleRarityAlpha() { rarityAlphaDescending = !rarityAlphaDescending; }
}
