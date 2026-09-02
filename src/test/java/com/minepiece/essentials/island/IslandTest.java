package com.minepiece.essentials.island;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Couvre fromBossbarText(), en particulier la régression où la clé auto-générée
 * "zou" (3 lettres) matchait n'importe quel texte de boss bar contenant cette
 * séquence par hasard — voir MIN_AUTO_KEY_LENGTH dans Island.
 */
class IslandTest {

    @Test
    void explicitZouAliasStillMatches() {
        assertEquals(Island.ZOU, Island.fromBossbarText("Ile de Zou"));
    }

    @Test
    void shortAutoKeyDoesNotFalsePositiveOnUnrelatedText() {
        // "zou" est un sous-mot de "zoukini" : avant le correctif, la clé
        // auto-générée à 3 lettres aurait fait matcher ce texte sans rapport.
        assertNotEquals(Island.ZOU, Island.fromBossbarText("Vous recevez un zoukini"));
    }

    @Test
    void unrelatedTextResolvesToUnknown() {
        assertEquals(Island.UNKNOWN, Island.fromBossbarText("Bienvenue sur le serveur"));
    }

    @Test
    void longDisplayNameStillAutoRegistered() {
        // "Baratie" (7 lettres) reste bien enregistré automatiquement.
        assertEquals(Island.BARATIE, Island.fromBossbarText("Vous arrivez a Baratie"));
    }

    @Test
    void explicitAliasWithSpacingVariantMatches() {
        assertEquals(Island.ILE_HOMMES_POISSONS, Island.fromBossbarText("Ile des Hommes-Poissons"));
    }

    @Test
    void shortJayaNameStillMatchesViaExplicitAlias() {
        // "Jaya" (4 lettres) tombe aussi sous le seuil d'auto-génération ; doit
        // rester détectable via son alias explicite.
        assertEquals(Island.JAYA, Island.fromBossbarText("Vous arrivez a Jaya"));
    }
}
