package com.minepiece.essentials.rarity;

/** Direction de tri, toggle à chaque clic. Défaut : décroissant (mythique d'abord). */
public final class RaritySortState {
    private boolean descending = true;
    public boolean descending() { return descending; }
    public void toggle() { descending = !descending; }
}
