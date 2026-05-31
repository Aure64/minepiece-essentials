package com.minepiece.essentials.pet;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts {@link MinionData} from the squidcore {@code custom_data} SNBT string. */
public final class MinionNbt {

    private static final Pattern PRESTIGE = strField("progression-minion_level");
    private static final Pattern CURRENT_XP = strField("progression-minion_xp");
    private static final Pattern NEXT_XP = strField("progression-minion_next-xp");
    private static final Pattern FOOD =
        Pattern.compile("minion-food-name.*?tracks\\.==([A-Z_]+)");

    private MinionNbt() {}

    private static Pattern strField(String key) {
        return Pattern.compile(Pattern.quote(key)
            + "\":\\{\"squidcore:type\":\"string\",\"squidcore:value\":\"([0-9.]+)\"");
    }

    public static Optional<MinionData> parse(String snbt) {
        if (snbt == null) return Optional.empty();

        Integer prestige = intOf(PRESTIGE, snbt);
        if (prestige == null) return Optional.empty();

        double currentXp = doubleOf(CURRENT_XP, snbt);
        double nextXp = doubleOf(NEXT_XP, snbt);
        String food = strOf(FOOD, snbt);

        return Optional.of(new MinionData(prestige, currentXp, nextXp, food));
    }

    private static Integer intOf(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? (int) Double.parseDouble(m.group(1)) : null;
    }

    private static double doubleOf(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    private static String strOf(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }
}
