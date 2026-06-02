package com.minepiece.essentials.quest;

import java.util.ArrayList;
import java.util.List;

/** Latest snapshot of the daily pass quests read from the /pass quests screen. */
public final class PassQuestState {

    private static volatile List<PassQuest> current = List.of();

    private PassQuestState() {}

    public static List<PassQuest> get() {
        return current;
    }

    public static void set(List<PassQuest> quests) {
        current = quests;
    }

    /**
     * Marks the quest with the given objective complete (progress snapped to the
     * target) — driven by the "Vous avez complété la quête: …" chat message, so
     * completion shows live without reopening the screen. No-op if no match.
     */
    public static void markCompleted(String objective) {
        if (objective == null) return;
        String target = objective.trim();
        List<PassQuest> snapshot = current;
        List<PassQuest> updated = new ArrayList<>(snapshot.size());
        boolean changed = false;
        for (PassQuest q : snapshot) {
            if (!q.completed() && q.objective().equalsIgnoreCase(target)) {
                updated.add(new PassQuest(q.number(), q.difficulty(), q.objective(),
                        Math.max(q.current(), q.target()), q.target(), true));
                changed = true;
            } else {
                updated.add(q);
            }
        }
        if (changed) current = List.copyOf(updated);
    }
}
