package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calcul pur de l'ordre cible d'un conteneur trié (par rareté ou alphabétique). */
public final class RaritySort {
    private RaritySort() {}

    /**
     * RARITY : clé primaire = rareté (identiques regroupés par itemId).
     * ITEM : clé primaire = nom de l'objet (alphabétique), secondaire = rareté → chaque
     * objet regroupé avec TOUTES ses raretés à la suite (ex. Boulon épique collé au Boulon
     * légendaire).
     */
    public enum Mode { RARITY, ITEM }

    /**
     * Une entrée par slot. {@code empty} = slot vide (toujours placé en fin).
     * {@code rank} = rang de rareté, {@code name} = nom affiché, {@code itemId} = id de
     * registre pour regrouper les identiques.
     */
    public record Entry(boolean empty, int rank, String itemId, String name) {}

    /**
     * Renvoie la liste des index source dans l'ordre cible (permutation stable).
     * Les slots vides finissent toujours en dernier. {@code ascending} contrôle le sens
     * de la clé primaire (RARITY : commun d'abord ; ITEM : A→Z). En mode ITEM, au sein
     * d'un même objet les raretés sont toujours triées de la plus haute à la plus basse.
     */
    public static List<Integer> targetOrder(List<Entry> entries, Mode mode, boolean ascending) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) idx.add(i);

        Comparator<Integer> cmp = (a, b) -> {
            Entry ea = entries.get(a), eb = entries.get(b);
            if (ea.empty() != eb.empty()) return ea.empty() ? 1 : -1; // vides en dernier
            if (!ea.empty()) {
                if (mode == Mode.RARITY) {
                    int byRank = ascending ? Integer.compare(ea.rank(), eb.rank())
                                           : Integer.compare(eb.rank(), ea.rank());
                    if (byRank != 0) return byRank;
                } else { // ITEM : nom d'abord, puis rareté décroissante
                    int byName = ea.name().compareToIgnoreCase(eb.name());
                    if (ascending) { if (byName != 0) return byName; }
                    else           { if (byName != 0) return -byName; }
                    int byRank = Integer.compare(eb.rank(), ea.rank()); // plus haute rareté d'abord
                    if (byRank != 0) return byRank;
                }
                int byId = ea.itemId().compareTo(eb.itemId());          // regroupe les identiques
                if (byId != 0) return byId;
            }
            return Integer.compare(a, b);              // stable
        };
        idx.sort(cmp);
        return idx;
    }
}
