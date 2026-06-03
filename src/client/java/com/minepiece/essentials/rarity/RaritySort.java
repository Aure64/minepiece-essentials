package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calcul pur de l'ordre cible d'un conteneur trié (par rareté ou alphabétique). */
public final class RaritySort {
    private RaritySort() {}

    /**
     * RARITY : par rareté seulement (identiques regroupés par itemId).
     * RARITY_ALPHA : par rareté, PUIS alphabétique (A→Z) dans chaque rareté — chaque
     * type d'item côte à côte au sein de sa gamme.
     */
    public enum Mode { RARITY, RARITY_ALPHA }

    /**
     * Une entrée par slot. {@code empty} = slot vide (toujours placé en fin).
     * {@code rank} = rang de rareté, {@code name} = nom affiché, {@code itemId} = id de
     * registre pour regrouper les identiques.
     */
    public record Entry(boolean empty, int rank, String itemId, String name) {}

    /**
     * Renvoie la liste des index source dans l'ordre cible (permutation stable).
     * Les slots vides finissent toujours en dernier. {@code rankAscending} contrôle le
     * sens des raretés (true = commun d'abord). Dans RARITY_ALPHA, le nom est toujours
     * trié A→Z à l'intérieur d'une rareté.
     */
    public static List<Integer> targetOrder(List<Entry> entries, Mode mode, boolean rankAscending) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) idx.add(i);

        Comparator<Integer> cmp = (a, b) -> {
            Entry ea = entries.get(a), eb = entries.get(b);
            if (ea.empty() != eb.empty()) return ea.empty() ? 1 : -1; // vides en dernier
            if (!ea.empty()) {
                int byRank = rankAscending ? Integer.compare(ea.rank(), eb.rank())
                                           : Integer.compare(eb.rank(), ea.rank());
                if (byRank != 0) return byRank;
                if (mode == Mode.RARITY_ALPHA) {
                    int byName = ea.name().compareToIgnoreCase(eb.name()); // toujours A→Z
                    if (byName != 0) return byName;
                }
                int byId = ea.itemId().compareTo(eb.itemId());            // regroupe les identiques
                if (byId != 0) return byId;
            }
            return Integer.compare(a, b);              // stable
        };
        idx.sort(cmp);
        return idx;
    }
}
