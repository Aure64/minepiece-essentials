package com.minepiece.essentials.rarity;

/**
 * Sens de tri par bouton, basculé à chaque clic.
 * - bouton rareté : décroissant par défaut (mythique d'abord).
 * - bouton objet (regroupe chaque objet avec ses raretés) : A→Z par défaut.
 */
public final class RaritySortState {
    private boolean rarityDescending = true;
    private boolean itemDescending = false; // false = A→Z

    public boolean rarityDescending() { return rarityDescending; }
    public boolean itemDescending() { return itemDescending; }

    public void toggleRarity() { rarityDescending = !rarityDescending; }
    public void toggleItem() { itemDescending = !itemDescending; }
}
