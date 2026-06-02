package com.minepiece.essentials.quest;

/**
 * One daily pass quest read from the /pass quests screen.
 *
 * @param number     the "Quête #N" index
 * @param difficulty Facile / Moyenne / Difficile / …
 * @param objective  the objective text (e.g. "Préparer 1 plat")
 * @param current    current progress
 * @param target     target progress
 * @param completed  whether the "Statut" line marks it complete
 */
public record PassQuest(int number, Difficulty difficulty, String objective,
                        int current, int target, boolean completed) {

    /** Complete by status, or progress having reached the target. */
    public boolean isComplete() {
        return completed || (target > 0 && current >= target);
    }
}
