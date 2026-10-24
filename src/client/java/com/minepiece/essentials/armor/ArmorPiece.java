package com.minepiece.essentials.armor;

import com.minepiece.essentials.pet.PetStat;
import java.util.List;
import java.util.Map;

/**
 * One worn armor piece as read from its lore.
 *
 * @param name      display name without formatting
 * @param setName   armor set the piece belongs to (from the "Full Armor Effect"
 *                  paragraph, else the first word of the name); null if unknown
 * @param stats     stat bonuses in lore order
 * @param labels    server display label per stat (e.g. "Health" / "Vie")
 * @param setEffect lines of the full-set effect paragraph, empty if none
 */
public record ArmorPiece(String name, String setName, Map<PetStat, Double> stats,
                         Map<PetStat, String> labels, List<String> setEffect) {}
