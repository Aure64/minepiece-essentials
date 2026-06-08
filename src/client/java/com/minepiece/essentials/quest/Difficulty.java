package com.minepiece.essentials.quest;

/** Daily pass-quest difficulty, with the in-game label and a HUD colour. */
public enum Difficulty {
    FACILE("Facile", 0xFF7CFC55),
    MOYENNE("Moyenne", 0xFFFFD24B),
    DIFFICILE("Difficile", 0xFFFF6B6B),
    PREMIUM("Premium", 0xFF55D6FF),
    PREMIUM_PLUS("Premium+", 0xFFE07FE0),
    AUTRE("?", 0xFFCBC8C7);

    private final String label;
    private final int color;

    Difficulty(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public String label() { return label; }
    public int color() { return color; }

    /** Matches the in-game label (case-insensitive); {@link #AUTRE} if unknown. */
    public static Difficulty fromLabel(String s) {
        if (s != null) {
            String t = s.trim().toLowerCase();
            switch (t) {
                case "easy": return FACILE;
                case "medium": return MOYENNE;
                case "hard": return DIFFICILE;
            }
            for (Difficulty d : values())
                if (d.label.toLowerCase().equals(t)) return d;
        }
        return AUTRE;
    }
}
