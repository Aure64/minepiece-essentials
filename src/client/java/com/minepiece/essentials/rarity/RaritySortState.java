package com.minepiece.essentials.rarity;

/**
 * Sens de tri par bouton, basculé à chaque clic.
 * - bouton rareté : décroissant par défaut (mythique d'abord).
 * - bouton objet (regroupe chaque objet avec ses raretés ; noms toujours A→Z) :
 *   ne bascule QUE le sens des raretés dans chaque groupe, plus rare d'abord par défaut.
 */
public final class RaritySortState {
    private boolean rarityDescending = true;
    private boolean itemRarityDescending = true; // true = plus rare d'abord dans chaque groupe

    public boolean rarityDescending() { return rarityDescending; }
    public boolean itemRarityDescending() { return itemRarityDescending; }

    public void toggleRarity() { rarityDescending = !rarityDescending; }
    public void toggleItem() { itemRarityDescending = !itemRarityDescending; }
}
