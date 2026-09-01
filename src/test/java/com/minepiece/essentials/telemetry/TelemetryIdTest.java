package com.minepiece.essentials.telemetry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryIdTest {

    @TempDir
    Path dir;

    @Test
    void generatesAndPersistsAnIdOnFirstUse() {
        Path file = dir.resolve("telemetry.json");
        TelemetryId first = TelemetryId.loadOrCreate(file);

        assertNotNull(first.installId());
        assertDoesNotThrow(() -> UUID.fromString(first.installId()), "c'est un UUID");
        assertTrue(Files.exists(file), "l'identité est persistée");
        assertFalse(first.announced(), "le message de premier lancement n'a pas encore ete affiche");
    }

    @Test
    void reusesTheSameIdOnSubsequentLoads() {
        Path file = dir.resolve("telemetry.json");
        String id = TelemetryId.loadOrCreate(file).installId();
        assertEquals(id, TelemetryId.loadOrCreate(file).installId(),
                "l'identifiant doit survivre au redemarrage, sinon la retention est fausse");
    }

    @Test
    void markAnnouncedSurvivesReload() {
        Path file = dir.resolve("telemetry.json");
        TelemetryId id = TelemetryId.loadOrCreate(file);
        id.markAnnounced(file);

        assertTrue(TelemetryId.loadOrCreate(file).announced(),
                "le message de premier lancement ne doit apparaitre qu'une fois");
    }

    @Test
    void twoInstallsGetDifferentIds() {
        assertNotEquals(
                TelemetryId.loadOrCreate(dir.resolve("a.json")).installId(),
                TelemetryId.loadOrCreate(dir.resolve("b.json")).installId());
    }
}
