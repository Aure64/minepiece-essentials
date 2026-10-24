package com.minepiece.essentials.armor;

import com.minepiece.essentials.i18n.ServerText;
import com.minepiece.essentials.pet.PetStat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses an armor piece's name + lore (as rendered strings) into an {@link ArmorPiece}.
 *
 * <p>Stat lines sit right under the name, one per line, shaped like
 * {@code "犹 Health +110"} (a pack glyph, the stat label, a signed value). The block
 * ends at the first blank line. A later paragraph headed "Full Armor Effect" (FR:
 * "Effet d'armure complète") describes the set bonus and names the set.
 * Anything without a recognised stat line is not MinePiece armor → empty.
 */
public final class ArmorStatParser {

    private static final String[] SET_EFFECT_HEADER = {
        "Full Armor Effect", "Effet d'armure complète", "Effet Armure Complète", "Effet de set"
    };
    private static final Pattern VALUE = Pattern.compile("\\+\\s*(-?\\d+(?:[.,]\\d+)?)");
    private static final Pattern SET_EN = Pattern.compile("(?i)(?:of|the|entire|complete|full)?\\s*([A-Z][\\w-]*?)(?:'s)?\\s+armor");
    private static final Pattern SET_FR = Pattern.compile("(?i)armure\\s+(?:complète\\s+)?(?:de\\s+|d')?([A-Z][\\w-]*)");
    private static final Pattern LATIN = Pattern.compile("[A-Za-z\\u00C0-\\u024F]");

    private ArmorStatParser() {}

    public static Optional<ArmorPiece> parse(List<String> nameAndLore) {
        if (nameAndLore == null || nameAndLore.size() < 2) return Optional.empty();
        String name = nameAndLore.get(0).trim();

        Map<PetStat, Double> stats = new EnumMap<>(PetStat.class);
        Map<PetStat, String> labels = new EnumMap<>(PetStat.class);
        int i = 1;
        for (; i < nameAndLore.size(); i++) {
            String line = nameAndLore.get(i);
            if (line.isBlank()) break;
            int plus = line.indexOf('+');
            if (plus < 0) break;
            PetStat stat = PetStat.fromLabel(line.substring(0, plus));
            if (stat == null) break;
            Matcher m = VALUE.matcher(line);
            if (!m.find()) break;
            double value = Double.parseDouble(m.group(1).replace(',', '.'));
            stats.merge(stat, value, Double::sum);
            labels.putIfAbsent(stat, latinWords(line.substring(0, plus)));
        }
        if (stats.isEmpty()) return Optional.empty();

        List<String> setEffect = new ArrayList<>();
        for (int j = i; j < nameAndLore.size(); j++) {
            if (ServerText.matches(nameAndLore.get(j), SET_EFFECT_HEADER)) {
                for (int k = j + 1; k < nameAndLore.size(); k++) {
                    String l = nameAndLore.get(k);
                    if (l.isBlank()) break;
                    setEffect.add(l.trim());
                }
                break;
            }
        }

        String setName = setNameFrom(setEffect);
        if (setName == null) setName = firstWord(name);

        return Optional.of(new ArmorPiece(name, setName, stats, labels, List.copyOf(setEffect)));
    }

    /** Set name quoted in the effect text: "Tamago's armor" / "the entire Tamago armor" / "l'armure Tamago". */
    static String setNameFrom(List<String> effect) {
        if (effect.isEmpty()) return null;
        String text = String.join(" ", effect);
        Matcher en = SET_EN.matcher(text);
        if (en.find()) return en.group(1);
        Matcher fr = SET_FR.matcher(text);
        if (fr.find()) return fr.group(1);
        return null;
    }

    private static String firstWord(String name) {
        String[] toks = name.split("\\s+");
        if (toks.length == 0 || toks[0].isEmpty()) return null;
        String w = toks[0];
        if (w.endsWith("'s")) w = w.substring(0, w.length() - 2);
        return w.isEmpty() ? null : w;
    }

    /** Keeps only tokens with latin letters (drops the pack glyph), e.g. "犹 Health " → "Health". */
    private static String latinWords(String s) {
        StringBuilder sb = new StringBuilder();
        for (String t : s.trim().split("\\s+")) {
            if (LATIN.matcher(t).find()) { if (sb.length() > 0) sb.append(' '); sb.append(t); }
        }
        return sb.toString();
    }
}
