package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calcul pur de l'ordre cible d'un conteneur trié (par rareté ou alphabétique). */
public final class RaritySort {
    private RaritySort() {}

    public enum Mode { RARITY, ALPHABETICAL }

    /**
     * Une entrée par slot. {@code empty} = slot vide (toujours placé en fin).
     * {@code rank} = rang de rareté (mode RARITY), {@code name} = nom affiché (mode
     * ALPHABETICAL), {@code itemId} = id de registre pour regrouper les identiques.
     */
    public record Entry(boolean empty, int rank, String itemId, String name) {}

    /**
     * Renvoie la liste des index source dans l'ordre cible (permutation stable).
     * Les slots vides finissent toujours en dernier, quel que soit le mode/sens.
     * - RARITY : par rang (ascending = commun d'abord), puis itemId, stable.
     * - ALPHABETICAL : par nom insensible à la casse (ascending = A→Z), puis itemId, stable.
     */
    public static List<Integer> targetOrder(List<Entry> entries, Mode mode, boolean ascending) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) idx.add(i);

        Comparator<Integer> cmp = (a, b) -> {
            Entry ea = entries.get(a), eb = entries.get(b);
            if (ea.empty() != eb.empty()) return ea.empty() ? 1 : -1; // vides en dernier
            if (!ea.empty()) {
                int primary;
                if (mode == Mode.RARITY) {
                    primary = ascending ? Integer.compare(ea.rank(), eb.rank())
                                        : Integer.compare(eb.rank(), ea.rank());
                } else {
                    int byName = ea.name().compareToIgnoreCase(eb.name());
                    primary = ascending ? byName : -byName;
                }
                if (primary != 0) return primary;
                int byId = ea.itemId().compareTo(eb.itemId());
                if (byId != 0) return byId;            // regroupe les identiques
            }
            return Integer.compare(a, b);              // stable
        };
        idx.sort(cmp);
        return idx;
    }
}
