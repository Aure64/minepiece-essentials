package com.minepiece.essentials.ah;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses MinePiece auction-house prices and computes a per-unit price.
 *
 * <p>AH item lore carries a listing line {@code " ▪ Prix de vente: 10M 实"} (and a
 * separate {@code "Prix moyen"} average to ignore). Prices are abbreviated with
 * K/M/B(/T) suffixes and may have decimals ({@code 1.5M}, {@code 875.75K}).
 */
public final class AhPriceParser {

    private static final String SELL_LINE = "Prix de vente";
    private static final Pattern NUMBER =
        Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([KkMmBbTt])?");

    private AhPriceParser() {}

    /** First abbreviated number in the string (e.g. "10M" → 1e7), or empty. */
    public static OptionalDouble parseAbbreviated(String s) {
        if (s == null) return OptionalDouble.empty();
        Matcher m = NUMBER.matcher(s);
        if (!m.find()) return OptionalDouble.empty();
        double n = Double.parseDouble(m.group(1));
        double mult = switch (m.group(2) == null ? "" : m.group(2).toUpperCase()) {
            case "K" -> 1_000d;
            case "M" -> 1_000_000d;
            case "B" -> 1_000_000_000d;
            case "T" -> 1_000_000_000_000d;
            default -> 1d;
        };
        return OptionalDouble.of(n * mult);
    }

    /** Compact representation, e.g. 1_000_000 → "1M", 112_500 → "112.5K". */
    public static String format(double v) {
        if (v >= 1_000_000_000d) return trim(v / 1_000_000_000d) + "B";
        if (v >= 1_000_000d) return trim(v / 1_000_000d) + "M";
        if (v >= 1_000d) return trim(v / 1_000d) + "K";
        return Long.toString(Math.round(v));
    }

    /** The per-unit "Prix de vente" label (e.g. "1M"), when count &gt; 1. */
    public static Optional<String> perUnit(List<String> lore, int count) {
        return perUnit(lore, count, SELL_LINE);
    }

    /** Per-unit label from the lore line containing {@code keyword} ÷ count, when count &gt; 1. */
    public static Optional<String> perUnit(List<String> lore, int count, String keyword) {
        if (count <= 1 || lore == null) return Optional.empty();
        for (String line : lore) {
            if (line.contains(keyword)) {
                OptionalDouble price = parseAbbreviated(line);
                if (price.isPresent() && price.getAsDouble() > 0) {
                    return Optional.of(format(price.getAsDouble() / count));
                }
            }
        }
        return Optional.empty();
    }

    /** Rounds to 2 decimals and strips trailing zeros: 1.0 → "1", 112.50 → "112.5". */
    private static String trim(double n) {
        double r = Math.round(n * 100d) / 100d;
        if (r == Math.floor(r)) return Long.toString((long) r);
        String s = Double.toString(r);
        // Drop a trailing zero from the 2-decimal form (e.g. "1.50" → "1.5").
        if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
