package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calcul pur de l'ordre cible d'un conteneur trié par rareté. */
public final class RaritySort {
    private RaritySort() {}

    /** rank = -1 ⇒ pas de rareté (placé en fin). itemId pour regrouper les identiques. */
    public record Entry(int rank, String itemId) {}

    /**
     * Renvoie la liste des index source dans l'ordre cible (permutation stable).
     * Les entrées sans rareté (rank < 0) finissent toujours en dernier, quel que soit
     * le sens. Au sein d'un même rang, regroupement par itemId (asc) ; tri stable.
     */
    public static List<Integer> targetOrder(List<Entry> entries, boolean descending) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) idx.add(i);

        Comparator<Integer> cmp = (a, b) -> {
            Entry ea = entries.get(a), eb = entries.get(b);
            boolean na = ea.rank() < 0, nb = eb.rank() < 0;
            if (na != nb) return na ? 1 : -1;          // sans-rareté en dernier
            if (!na) {                                  // les deux ont une rareté
                int c = descending ? Integer.compare(eb.rank(), ea.rank())
                                   : Integer.compare(ea.rank(), eb.rank());
                if (c != 0) return c;
                int byId = ea.itemId().compareTo(eb.itemId());
                if (byId != 0) return byId;            // regroupe les identiques
            }
            return Integer.compare(a, b);              // stable
        };
        idx.sort(cmp);
        return idx;
    }
}
