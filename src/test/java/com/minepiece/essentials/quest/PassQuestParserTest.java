package com.minepiece.essentials.quest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PassQuestParserTest {

    @Test
    void parsesRealEasyQuest() {
        PassQuest q = PassQuestParser.parse("Quête #1 (Facile)", List.of(
                "Quête #1 (Facile)",
                "💡 Objectif",
                " ▪ Préparer 1 plat",
                "❦ Progression",
                "0 s m m m m m m m m e   1",
                "Statut : Non complétée"
        )).orElseThrow();

        assertEquals(1, q.number());
        assertEquals(Difficulty.FACILE, q.difficulty());
        assertEquals("Préparer 1 plat", q.objective());
        assertEquals(0, q.current());
        assertEquals(1, q.target());
        assertFalse(q.completed());
        assertFalse(q.isComplete());
    }

    @Test
    void parsesCompletedHardQuest() {
        PassQuest q = PassQuestParser.parse("Quête #3 (Difficile)", List.of(
                "💡 Objectif",
                " ▪ Gagner 5 donjons",
                "❦ Progression",
                "5 s m m e 5",
                "Statut : Complétée"
        )).orElseThrow();

        assertEquals(3, q.number());
        assertEquals(Difficulty.DIFFICILE, q.difficulty());
        assertEquals("Gagner 5 donjons", q.objective());
        assertEquals(5, q.current());
        assertEquals(5, q.target());
        assertTrue(q.completed());
        assertTrue(q.isComplete());
    }

    @Test
    void keepsDigitsInObjectiveAndReadsLargeTarget() {
        PassQuest q = PassQuestParser.parse("Quête #2 (Moyenne)", List.of(
                "💡 Objectif",
                " ▪ Miner 1133 blocs",
                "❦ Progression",
                "0 s m e 1133",
                "Statut : Non complétée"
        )).orElseThrow();

        assertEquals(Difficulty.MOYENNE, q.difficulty());
        assertEquals("Miner 1133 blocs", q.objective());
        assertEquals(0, q.current());
        assertEquals(1133, q.target());
    }

    @Test
    void extractsCompletedObjectiveFromChat() {
        assertEquals(java.util.Optional.of("Voter 2 fois"),
                PassQuestParser.completedObjective("Vous avez complété la quête: Voter 2 fois"));
        assertTrue(PassQuestParser.completedObjective("VinsmokeDSanji > ah d'accord").isEmpty());
    }

    @Test
    void ignoresNonQuestItems() {
        assertTrue(PassQuestParser.parse("Informations",
                List.of("Complétez les différentes quêtes")).isEmpty());
        assertTrue(PassQuestParser.parse("Relance", List.of("距 → Relancer")).isEmpty());
    }
}
