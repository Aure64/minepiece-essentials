package com.minepiece.essentials;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Couvre uniquement la correspondance pure du signal "tab-list" (header/footer),
 * sans dépendance à un client Minecraft en cours d'exécution.
 */
class ServerDetectorTest {

    @Test
    void footerContainingMinepieceMatches() {
        assertTrue(ServerDetector.tabListMatches(null, "PLAY.MINEPIECE.NET"));
    }

    @Test
    void matchIsCaseInsensitive() {
        assertTrue(ServerDetector.tabListMatches(null, "play.MinePiece.net"));
    }

    @Test
    void headerOnlyMatches() {
        assertTrue(ServerDetector.tabListMatches("Bienvenue sur MinePiece !", null));
    }

    @Test
    void bothNullOrEmptyDoNotMatch() {
        assertFalse(ServerDetector.tabListMatches(null, null));
        assertFalse(ServerDetector.tabListMatches("", ""));
    }

    @Test
    void otherServerFooterDoesNotMatch() {
        assertFalse(ServerDetector.tabListMatches(null, "PLAY.HYPIXEL.NET"));
    }
}
