package com.minepiece.essentials.ascension;

import com.minepiece.essentials.i18n.ServerText;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses an evolving tool (hoe, pickaxe, rod… "of Monkey D. Luffy") from its rendered
 * name + lore. Unlike fruits and weapons, tools keep no {@code items:experience-xp} NBT:
 * their progress is the "Progression" paragraph, a bar framed by the current and the
 * target value, e.g. {@code "0 ▮▮▮▮▮ 5000"}. Anything without that paragraph → empty.
 */
public final class EvoToolParser {

    private static final String[] PROGRESSION = {"Progression", "Progrès", "Progress"};
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d\\s'.,]*\\d|\\d");

    private EvoToolParser() {}

    public static Optional<AscensionItem> parse(List<String> nameAndLore) {
        if (nameAndLore == null || nameAndLore.size() < 2) return Optional.empty();
        for (int i = 1; i < nameAndLore.size(); i++) {
            if (!ServerText.matches(nameAndLore.get(i), PROGRESSION)) continue;
            // Numbers on the header line itself, else on the next non-blank line (the bar).
            long[] nums = numbers(nameAndLore.get(i));
            for (int j = i + 1; nums == null && j < nameAndLore.size() && j <= i + 2; j++) {
                if (nameAndLore.get(j).isBlank()) continue;
                nums = numbers(nameAndLore.get(j));
            }
            if (nums == null) return Optional.empty();
            int xp = (int) Math.min(Integer.MAX_VALUE, nums[0]);
            int max = (int) Math.min(Integer.MAX_VALUE, nums[1]);
            boolean ready = max > 0 && xp >= max;
            return Optional.of(new AscensionItem(AscensionItem.Type.TOOL, nameAndLore.get(0).trim(),
                null, 0, 0, xp, max, ready, RarityColors.color(null)));
        }
        return Optional.empty();
    }

    /** First and last integer of the line, or null if fewer than two. */
    private static long[] numbers(String line) {
        Matcher m = NUMBER.matcher(line);
        long first = -1, last = -1;
        int n = 0;
        while (m.find()) {
            String digits = m.group().replaceAll("[^0-9]", "");
            if (digits.isEmpty()) continue;
            long v = Long.parseLong(digits);
            if (n++ == 0) first = v;
            last = v;
        }
        return n >= 2 ? new long[]{first, last} : null;
    }
}
