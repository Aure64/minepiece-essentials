package com.minepiece.essentials.profile;

import com.minepiece.essentials.i18n.ServerText;
import com.minepiece.essentials.pet.PetStat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the "Information" item of the {@code /profil} screen (dump of 2026-10-24):
 * <pre>
 *   ❦ Progression / ▪ Ascension: 1 / ▪ Level: 97 (67.41%) / 485712 ▮▮▮ 720550
 *   🧪 Statistics / 犹 Health +1697 / … / 丈 Regeneration +21
 *   ⚔ Obtaining / …
 * </pre>
 * Needs the Statistics header and at least one stat line; anything else → empty.
 */
public final class ProfileParser {

    private static final Pattern VALUE = Pattern.compile("\\+\\s*(-?\\d+(?:[.,]\\d+)?)\\s*(%?)");
    private static final Pattern INT = Pattern.compile("\\d+");
    private static final Pattern LATIN = Pattern.compile("[A-Za-z\\u00C0-\\u024F]");

    private ProfileParser() {}

    public static Optional<ProfileData> parse(List<String> nameAndLore) {
        if (nameAndLore == null || nameAndLore.isEmpty()) return Optional.empty();
        int ascension = 0, level = 0;
        double pct = 0;
        long xp = 0, maxXp = 0;
        int statsStart = -1;

        for (int i = 1; i < nameAndLore.size(); i++) {
            String line = nameAndLore.get(i);
            Matcher a = ServerText.PROFILE_ASCENSION.matcher(line);
            if (a.find()) ascension = Integer.parseInt(a.group(1));
            Matcher l = ServerText.PROFILE_LEVEL.matcher(line);
            if (l.find()) {
                level = Integer.parseInt(l.group(1));
                pct = Double.parseDouble(l.group(2).replace(',', '.'));
                // The xp bar follows: "<xp> <bar glyphs> <maxXp>".
                for (int j = i + 1; j < Math.min(nameAndLore.size(), i + 4); j++) {
                    List<Long> nums = new ArrayList<>();
                    Matcher n = INT.matcher(nameAndLore.get(j));
                    while (n.find()) nums.add(Long.parseLong(n.group()));
                    if (nums.size() >= 2) { xp = nums.get(0); maxXp = nums.get(nums.size() - 1); break; }
                }
            }
            if (ServerText.matches(line, ServerText.PROFILE_STATS)) { statsStart = i + 1; break; }
        }
        if (statsStart < 0) return Optional.empty();

        List<ProfileData.Entry> entries = new ArrayList<>();
        Map<PetStat, Double> byStat = new EnumMap<>(PetStat.class);
        Set<PetStat> percent = EnumSet.noneOf(PetStat.class);
        for (int i = statsStart; i < nameAndLore.size(); i++) {
            String line = nameAndLore.get(i);
            if (ServerText.matches(line, ServerText.PROFILE_END)) break;
            int plus = line.indexOf('+');
            if (plus < 0) continue;
            Matcher m = VALUE.matcher(line);
            if (!m.find()) continue;
            double value = Double.parseDouble(m.group(1).replace(',', '.'));
            boolean isPct = !m.group(2).isEmpty();
            String label = latinWords(line.substring(0, plus));
            PetStat stat = PetStat.fromLabel(label);
            entries.add(new ProfileData.Entry(label, stat, value, isPct));
            if (stat != null) {
                byStat.merge(stat, value, Double::sum);
                if (isPct) percent.add(stat);
            }
        }
        if (entries.isEmpty()) return Optional.empty();
        return Optional.of(new ProfileData(ascension, level, pct, xp, maxXp,
            List.copyOf(entries), byStat, percent));
    }

    private static String latinWords(String s) {
        StringBuilder sb = new StringBuilder();
        for (String t : s.trim().split("\\s+")) {
            if (LATIN.matcher(t).find()) { if (sb.length() > 0) sb.append(' '); sb.append(t); }
        }
        return sb.toString();
    }
}
