package com.minepiece.essentials.pet;

import java.util.List;

/** The 11 pet combat stats covered by the wiki LVL-10 reference table. */
public enum PetStat {
    VITALITY("Vie", 0xFFE95353),
    STRENGTH("Force", 0xFFE0584C),
    CRIT_CHANCE("Chance Critique", 0xFFE67E4D),
    CRIT_DAMAGE("Dégâts Critiques", 0xFFD67343),
    POWER("Puissance", 0xFFF1C84B),
    ENERGY("Énergie", 0xFFF0B43D),
    ENERGY_REGEN("Régén. Énergie", 0xFFE8A33D),
    SPEED("Vitesse", 0xFF4FC3E8),
    DEXTERITY("Dextérité", 0xFF54C8C6),
    DEFENSE("Défense", 0xFF56A6D0),
    REGEN("Régén.", 0xFFB07FE0);

    private final String fr;
    private final int color;

    PetStat(String fr, int color) {
        this.fr = fr;
        this.color = color;
    }

    /** French display name. */
    public String fr() {
        return fr;
    }

    /** In-game ARGB colour for this stat. */
    public int color() {
        return color;
    }

    /**
     * Aliases as written in the in-game (French) tooltip, ordered most-specific
     * first so that e.g. "Régénération Énergie" matches {@link #ENERGY_REGEN}
     * before "Énergie"/"Régénération".
     */
    private record Alias(String text, PetStat stat) {}

    private static final List<Alias> ALIASES = List.of(
        new Alias("régénération énergie", ENERGY_REGEN),
        new Alias("chance critique", CRIT_CHANCE),
        new Alias("dégâts critiques", CRIT_DAMAGE),
        new Alias("dégât critique", CRIT_DAMAGE),
        new Alias("régénération", REGEN),
        new Alias("puissance", POWER),
        new Alias("dextérité", DEXTERITY),
        new Alias("défense", DEFENSE),
        new Alias("vitesse", SPEED),
        new Alias("énergie", ENERGY),
        new Alias("force", STRENGTH),
        new Alias("vie", VITALITY)
    );

    /** Finds the stat whose French label appears in {@code text}; null if none. */
    public static PetStat fromFrench(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        for (Alias a : ALIASES) {
            if (lower.contains(a.text())) return a.stat();
        }
        return null;
    }
}
