package com.minepiece.essentials.ascension;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts {@link AscensionItem} data from a squidcore item's {@code custom_data}
 * SNBT.
 *
 * <p>Ascendable gear carries {@code items:experience-xp = "level:xp:maxXp"}; the
 * next ascension is available when {@code maxXp == 0}. Items with a
 * {@code stats:damage} component are weapons, otherwise fruits. Anything without
 * the experience field is not ascendable.
 */
public final class AscensionParser {

    private static final Pattern EXP =
        Pattern.compile("items:experience-xp\":\"(\\d+):(\\d+):(\\d+)\"");
    private static final Pattern ASCENSION =
        Pattern.compile("items_experience_ascension\":\\{[^}]*\"squidcore:value\":\"(\\d+)\"");
    private static final Pattern RARITY_LORE =
        Pattern.compile("global-rarity-lore[^%]*tracks\\.==([a-z_]+)");
    private static final Pattern RARITY_FIELD =
        Pattern.compile("items:rarity\":\"([a-z_]+)\"");
    private static final Pattern NIVEAU_SUFFIX =
        Pattern.compile("\\s*\\(Niveau\\s*\\d+\\)\\s*$");

    private AscensionParser() {}

    public static Optional<AscensionItem> parse(String displayName, String nbt) {
        if (nbt == null) return Optional.empty();

        Matcher exp = EXP.matcher(nbt);
        if (!exp.find()) return Optional.empty();

        int level = Integer.parseInt(exp.group(1));
        int xp = Integer.parseInt(exp.group(2));
        int maxXp = Integer.parseInt(exp.group(3));
        boolean available = maxXp == 0;

        int ascension = 0;
        Matcher asc = ASCENSION.matcher(nbt);
        if (asc.find()) ascension = Integer.parseInt(asc.group(1));

        AscensionItem.Type type = nbt.contains("stats:damage")
            ? AscensionItem.Type.WEAPON : AscensionItem.Type.FRUIT;

        String rarity = null;
        Matcher r = RARITY_LORE.matcher(nbt);
        if (r.find()) rarity = r.group(1);
        else {
            Matcher rf = RARITY_FIELD.matcher(nbt);
            if (rf.find()) rarity = rf.group(1);
        }

        String name = displayName == null ? "" : NIVEAU_SUFFIX.matcher(displayName).replaceAll("");

        return Optional.of(new AscensionItem(
            type, name, rarity, level, ascension, xp, maxXp, available, RarityColors.color(rarity)));
    }
}
