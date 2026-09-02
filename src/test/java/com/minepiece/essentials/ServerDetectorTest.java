package com.minepiece.essentials;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Couvre uniquement la correspondance pure du signal tab-list (header/footer),
 * sans dependance a un client Minecraft en cours d'execution.
 *
 * <p>Les chaines en petites capitales Unicode sont ecrites en sequences
 * d'echappement pour que ce fichier reste ASCII-safe.
 */
class ServerDetectorTest {

    // "PLAY.MINEPIECE.NET" en petites capitales Unicode, tel qu'envoye par le
    // vrai footer du serveur (voir le log de diagnostic qui a motive ce
    // correctif).
    private static final String SMALL_CAPS_PLAY_MINEPIECE_NET =
        "\u1d18\u029f\u1d00\u028f.\u1d0d\u026a\u0274\u1d07\u1d18\u026a\u1d07\u1d04\u1d07.\u0274\u1d07\u1d1b";

    // "VOTE" en petites capitales Unicode (utilise dans le footer reel).
    private static final String SMALL_CAPS_VOTE = "\u1d20\u1d0f\u1d1b\u1d07";

    // Footer reel intercepte par le diagnostic temporaire (commit 8fbd6d2),
    // reconstruit a partir des echappements loggues :
    // "\nVote and earn rewards for free with the /VOTE\n \nPLAY.MINEPIECE.NET\n(spawn-66fa5ce9)\n"
    private static final String REAL_WORLD_FOOTER =
        "\nVote and earn rewards for free with the /" + SMALL_CAPS_VOTE + "\n \n"
        + SMALL_CAPS_PLAY_MINEPIECE_NET + "\n(spawn-66fa5ce9)\n";

    // "HYPIXEL.NET" en petites capitales Unicode (le "x" n'a pas de petite
    // capitale et reste en ASCII), pour verifier qu'un autre serveur ne matche
    // pas.
    private static final String SMALL_CAPS_HYPIXEL_NET =
        "\u029c\u028f\u1d18\u026ax\u1d07\u029f.\u0274\u1d07\u1d1b";

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

    @Test
    void smallCapsFooterMatches() {
        assertTrue(ServerDetector.tabListMatches(null, SMALL_CAPS_PLAY_MINEPIECE_NET));
    }

    @Test
    void realWorldSmallCapsFooterMatches() {
        assertTrue(ServerDetector.tabListMatches(null, REAL_WORLD_FOOTER));
    }

    @Test
    void smallCapsOtherServerDoesNotMatch() {
        assertFalse(ServerDetector.tabListMatches(null, SMALL_CAPS_HYPIXEL_NET));
    }

    @Test
    void smallCapsHeaderDoesNotThrowWhenFooterNull() {
        assertFalse(ServerDetector.tabListMatches(SMALL_CAPS_HYPIXEL_NET, null));
    }
}
