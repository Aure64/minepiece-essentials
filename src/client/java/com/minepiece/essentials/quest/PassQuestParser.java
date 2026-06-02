package com.minepiece.essentials.quest;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a daily pass-quest item (name + tooltip lore) into a {@link PassQuest}.
 *
 * <p>Lore shape on the MinePiece /pass quests screen:
 * <pre>
 *   💡 Objectif
 *    ▪ &lt;objective&gt;
 *   ❦ Progression
 *   &lt;current&gt; &lt;bar glyphs&gt; &lt;target&gt;
 *   Statut : Non complétée | Complétée
 * </pre>
 * The progress bar itself is rendered with custom glyphs, but the current/target
 * numbers sit at its ends, so we read the first and last integers of that line.
 */
public final class PassQuestParser {

    private static final Pattern NAME = Pattern.compile("Qu[eê]te #(\\d+)\\s*\\(([^)]+)\\)");
    private static final Pattern INT = Pattern.compile("\\d+");
    private static final String BULLET = "▪"; // ▪
    // Chat line shown on completion: "Vous avez complété la quête: <objective>".
    private static final Pattern COMPLETED = Pattern.compile("qu[eê]te\\s*:\\s*(.+)$");

    private PassQuestParser() {}

    /** The objective from a "Vous avez complété la quête: …" chat line, if any. */
    public static Optional<String> completedObjective(String chatLine) {
        if (chatLine == null || !chatLine.toLowerCase().contains("compl")) return Optional.empty();
        Matcher m = COMPLETED.matcher(chatLine);
        return m.find() ? Optional.of(m.group(1).trim()) : Optional.empty();
    }

    public static Optional<PassQuest> parse(String name, List<String> lore) {
        Matcher nm = NAME.matcher(name);
        if (!nm.find()) return Optional.empty();

        int number = Integer.parseInt(nm.group(1));
        Difficulty difficulty = Difficulty.fromLabel(nm.group(2));

        String objective = null;
        int current = 0, target = 0;
        boolean completed = false;
        boolean expectProgress = false;

        for (String raw : lore) {
            String line = raw.trim();
            if (objective == null && line.startsWith(BULLET)) {
                objective = line.substring(BULLET.length()).trim();
            } else if (line.contains("Progression")) {
                expectProgress = true;
            } else if (expectProgress && INT.matcher(line).find()) {
                int[] ct = firstAndLastInt(line);
                current = ct[0];
                target = ct[1];
                expectProgress = false;
            } else if (line.contains("Statut")) {
                completed = !line.contains("Non compl");
            }
        }

        if (objective == null) return Optional.empty();
        return Optional.of(new PassQuest(number, difficulty, objective, current, target, completed));
    }

    private static int[] firstAndLastInt(String s) {
        Matcher m = INT.matcher(s);
        int first = 0, last = 0;
        boolean got = false;
        while (m.find()) {
            int v = Integer.parseInt(m.group());
            if (!got) { first = v; got = true; }
            last = v;
        }
        return new int[]{first, last};
    }
}
