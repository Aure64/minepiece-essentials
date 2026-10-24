package com.minepiece.essentials.armor;

import com.minepiece.essentials.pet.PetStat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Squeezes a full-set effect paragraph into one HUD line, e.g.
 * "Get a bonus of xp of 10% on your profile by wearing the full Tamago armor." → "XP profil +10%".
 * Keeps the first number (with its % if any) and the first stat keyword found.
 */
public final class ArmorSetEffect {

    private static final Pattern NUMBER = Pattern.compile("([+-]?\\d+(?:[.,]\\d+)?)\\s*(%?)");
    private static final Pattern XP = Pattern.compile("(?i)\\bxp\\b|exp[ée]rience");
    private static final Pattern PROFILE = Pattern.compile("(?i)\\bprofil(?:e)?\\b");

    private ArmorSetEffect() {}

    public static String summarize(List<String> effect) {
        if (effect == null || effect.isEmpty()) return "";
        String text = String.join(" ", effect).trim();
        Matcher n = NUMBER.matcher(text);
        if (!n.find()) return text.length() > 28 ? text.substring(0, 26) + ".." : text;
        String value = n.group(1).replace(',', '.') + n.group(2);
        if (!value.startsWith("+") && !value.startsWith("-")) value = "+" + value;

        String what;
        PetStat stat = PetStat.fromLabel(text);
        if (stat != null) what = stat.fr();
        else if (XP.matcher(text).find()) what = PROFILE.matcher(text).find() ? "XP profil" : "XP";
        else what = firstWord(text);
        return what + " " + value;
    }

    private static String firstWord(String text) {
        String[] t = text.split("\\s+");
        return t.length > 0 ? t[0] : "";
    }
}
