package com.minepiece.essentials.quest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PassQuestStateTest {

    @Test
    void markCompletedFlipsMatchingQuestOnly() {
        PassQuestState.set(List.of(
                new PassQuest(1, Difficulty.FACILE, "Voter 2 fois", 1, 2, false),
                new PassQuest(2, Difficulty.MOYENNE, "Miner 100 blocs", 0, 100, false)));

        PassQuestState.markCompleted("Voter 2 fois");

        List<PassQuest> q = PassQuestState.get();
        assertTrue(q.get(0).completed());
        assertTrue(q.get(0).isComplete());
        assertEquals(2, q.get(0).current(), "progress should snap to the target");
        assertFalse(q.get(1).completed(), "other quests untouched");
    }

    @Test
    void markCompletedIgnoresUnknownObjective() {
        PassQuestState.set(List.of(
                new PassQuest(1, Difficulty.FACILE, "Voter 2 fois", 1, 2, false)));

        PassQuestState.markCompleted("Quelque chose d'autre");

        assertFalse(PassQuestState.get().get(0).completed());
    }
}
