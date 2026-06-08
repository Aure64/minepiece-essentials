package com.minepiece.essentials.ah;

import com.minepiece.essentials.i18n.ServerText;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Classe la "qualité" du prix d'une annonce de l'hôtel des ventes en comparant le
 * prix de vente au prix moyen du marché. Logique pure (testable), sans rendu.
 *
 * <p>Vert (« au prix ») dans une bande de ±10 % autour de la moyenne ; jaune en
 * dessous (bonne affaire), rouge au-dessus (trop cher).
 */
public final class AhPriceBand {

    /** Bande de prix, avec sa couleur ARGB (réutilisée pour le liseré et le texte). */
    public enum Band {
        CHEAP(0xFFFFD24B),     // jaune — moins cher que la moyenne
        FAIR(0xFF7CFC55),      // vert — au prix
        EXPENSIVE(0xFFFF5555); // rouge — trop cher

        public final int color;
        Band(int color) { this.color = color; }
    }

    /** Bande retenue + écart relatif (prix vente vs prix moyen). */
    public record Result(Band band, double ratio) {
        /** Pourcentage signé arrondi à l'entier (ex. +15, -8). */
        public int percent() { return (int) Math.round(ratio * 100); }
    }

    /** Demi-largeur de la bande « au prix » (±10 %). Bornes incluses dans le vert. */
    private static final double THRESHOLD = 0.10;

    private AhPriceBand() {}

    /** Bande pour un couple (prix vente, prix moyen). Vide si une valeur ≤ 0. */
    public static Optional<Result> of(double sell, double avg) {
        if (avg <= 0 || sell <= 0) return Optional.empty();
        double ratio = (sell - avg) / avg;
        Band band;
        if (ratio < -THRESHOLD) band = Band.CHEAP;
        else if (ratio > THRESHOLD) band = Band.EXPENSIVE;
        else band = Band.FAIR;
        return Optional.of(new Result(band, ratio));
    }

    /** Extrait « Prix de vente » et « Prix moyen » du lore et calcule la bande. */
    public static Optional<Result> fromLore(List<String> lore) {
        if (lore == null) return Optional.empty();
        OptionalDouble sell = priceOf(lore, ServerText.SELL_PRICE);
        OptionalDouble avg = priceOf(lore, ServerText.AVG_PRICE);
        if (sell.isEmpty() || avg.isEmpty()) return Optional.empty();
        return of(sell.getAsDouble(), avg.getAsDouble());
    }

    private static OptionalDouble priceOf(List<String> lore, String[] variants) {
        for (String line : lore) {
            if (ServerText.matches(line, variants)) {
                OptionalDouble p = AhPriceParser.parseAbbreviated(line);
                if (p.isPresent()) return p;
            }
        }
        return OptionalDouble.empty();
    }
}
