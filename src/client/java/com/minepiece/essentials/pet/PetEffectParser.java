package com.minepiece.essentials.pet;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a rendered "Familier Effects" stat line, e.g.
 * {@code "S.1.5.E 犹 Vie + 105.65"}, into a {@link PetEffect}.
 *
 * <p>The leading token encodes the unlock tier as its digits
 * ({@code S.1.5.E} → 15, {@code S.2.0.E} → 20, {@code S.1.E} → 1). The stat name
 * and value sit around the {@code +}. Lines without a {@code +}, an unknown
 * stat, or no tier digits (headers, money, "Non découvert") yield empty.
 */
public final class PetEffectParser {

    private static final Pattern VALUE = Pattern.compile("\\+\\s*(-?\\d+(?:[.,]\\d+)?)");

    private PetEffectParser() {}

    public static Optional<PetEffect> parse(String line) {
        if (line == null) return Optional.empty();

        int plus = line.indexOf('+');
        if (plus < 0) return Optional.empty();

        String[] tokens = line.trim().split("\\s+");
        if (tokens.length == 0) return Optional.empty();
        int tier = digitsOf(tokens[0]);
        if (tier <= 0) return Optional.empty();

        PetStat stat = PetStat.fromFrench(line.substring(0, plus));
        if (stat == null) return Optional.empty();

        Matcher m = VALUE.matcher(line);
        if (!m.find()) return Optional.empty();
        double value = Double.parseDouble(m.group(1).replace(',', '.'));

        return Optional.of(new PetEffect(tier, stat, value));
    }

    /** Concatenates the digits in a token: {@code "S.1.5.E"} → 15, {@code "S.1.E"} → 1. */
    private static int digitsOf(String token) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c >= '0' && c <= '9') sb.append(c);
        }
        return sb.isEmpty() ? 0 : Integer.parseInt(sb.toString());
    }
}
