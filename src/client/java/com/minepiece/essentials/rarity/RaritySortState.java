package com.minepiece.essentials.rarity;

/**
 * Direction de tri par mode, basculée à chaque clic du bouton correspondant.
 * Défauts : rareté décroissante (mythique d'abord), alphabétique croissant (A→Z).
 */
public final class RaritySortState {
    private boolean rarityDescending = true;
    private boolean alphaAscending = true;

    public boolean rarityDescending() { return rarityDescending; }
    public boolean alphaAscending() { return alphaAscending; }

    public void toggleRarity() { rarityDescending = !rarityDescending; }
    public void toggleAlpha() { alphaAscending = !alphaAscending; }
}
