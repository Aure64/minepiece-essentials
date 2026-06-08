package com.minepiece.essentials.i18n;

import java.util.regex.Pattern;

/**
 * Mots-clés serveur MinePiece en plusieurs langues (FR + EN). Le serveur envoie son
 * texte dans la « Game language » (réglée via /lang, illisible côté client) : les
 * parsers matchent donc toutes les langues connues simultanément. Ajouter une langue
 * = compléter les alternatives ci-dessous.
 */
public final class ServerText {
    private ServerText() {}

    // AH
    public static final String[] SELL_PRICE = {"Prix de vente", "Selling price"};
    public static final String[] AVG_PRICE  = {"Prix moyen", "Average price"};

    // Pets
    // "Familiar/Familier Effects" = pet équipé ; "Pet Effects" = pet fusionné/passif (même section de stats).
    public static final String[] PET_EFFECTS = {"Familier Effects", "Familiar Effects", "Pet Effects", "Effets Pet"};
    public static final String[] PET_ACTIVE_ACTION = {"Désactiver", "Disable"};
    public static final String[] PET_INACTIVE_ACTION = {"Activer", "Activate"};
    public static final String[] PET_NOT_DISCOVERED = {"Non découvert", "Not discovered"};
    public static final Pattern PET_LEVEL = Pattern.compile("(?:Niveau|Level):\\s*(\\S+)");

    // Parchemins
    public static final String[] SCROLL_NAME = {"parchemin", "scroll"};
    public static final String[] OBJECTIVE = {"Objectif", "Objective"};
    public static final String[] LUNAR = {"lunaire", "lunar"};
    public static final Pattern EXPIRE =
        Pattern.compile("(?:Expire le|Expires on) (\\d{2}/\\d{2}/\\d{4} \\d{1,2}h\\d{2})");

    // Quêtes pass
    public static final Pattern QUEST_NAME =
        Pattern.compile("(?:Qu[eê]te|Quest) #(\\d+)\\s*\\(([^)]+)\\)");
    public static final String[] PROGRESS = {"Progress"};       // couvre FR « Progression »
    public static final String[] STATUS = {"Statut", "Status"};
    public static final String[] NOT_COMPLETED = {"Non compl", "Not compl"};
    public static final String[] QUEST_NAME_FRAGMENT = {"uête #", "uete #", "uest #"};

    // Boss
    public static final Pattern BOSS_COORDS =
        Pattern.compile("(?:Coordonn[eé]es?|Coordinates)\\s*:?\\s*([-\\d]+)\\s+([-\\d]+)\\s+([-\\d]+)");
    public static final Pattern BOSS_RESPAWN =
        Pattern.compile("(?:R[eé]app?arition|Apparition|Respawn)\\s*:?\\s*(?:(\\d+)m)?\\s*(\\d+)s");
    public static final String[] BOSS_MINIBOSS_CATEGORY =
        {"mini-boss", "présents sur cette île", "present on this island", "present in the city"};
    public static final String[] BOSS_SINGLE =
        {"le Boss de cette île", "the Boss of this island"};
    public static final String[] BOSS_VIEW_LOOTS =
        {"voir les loots", "view loots", "view the loots", "view relics", "chasseur", "hunter", "pirate", "monstre", "monster"};
    public static final String[] BOSS_NAV = {"retour", "page", "fermer", "back", "close"};
    public static final String[] BOSS_MOB_KEYWORDS =
        {"marine", "mob", "monstre", "monster", "bandit", "combat", "fight", "ennemi",
         "enemy", "garde", "guard", "pirate", "zombie", "chasseur", "hunter", "soldat", "soldier"};
    /** Intervalle de respawn : "Toutes les 15 Minutes" (FR) ou "(15 Minutes)" (EN). Groupe 1 = nombre de minutes. */
    public static final Pattern BOSS_INTERVAL =
        Pattern.compile("(?:Toutes les\\s*)?(\\d+)\\s*Minutes?");

    // Haki
    public static final String[] HAKI_ACTIVATED = {"Vous avez activé le haki", "You have activated haki"};
    public static final String[] HAKI_READY =
        {"Vous pouvez de nouveau utiliser votre haki", "You can use your haki"};

    /** Vrai si {@code line} contient une des variantes (insensible à la casse). */
    public static boolean matches(String line, String[] variants) {
        if (line == null) return false;
        String lower = line.toLowerCase();
        for (String v : variants) if (lower.contains(v.toLowerCase())) return true;
        return false;
    }
}
