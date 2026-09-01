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

    @Test
    void corruptJsonFallsBackToAFreshUsableIdentity() throws Exception {
        Path file = dir.resolve("telemetry.json");
        Files.writeString(file, "{\"installId\": \"abc");

        TelemetryId id = assertDoesNotThrow(() -> TelemetryId.loadOrCreate(file),
                "un fichier corrompu ne doit jamais faire planter le client");

        assertNotNull(id.installId());
        assertDoesNotThrow(() -> UUID.fromString(id.installId()));
    }

    @Test
    void wrongShapeJsonFallsBackToAFreshUsableIdentity() throws Exception {
        Path file = dir.resolve("telemetry.json");
        Files.writeString(file, "[1, 2, 3]");

        TelemetryId id = assertDoesNotThrow(() -> TelemetryId.loadOrCreate(file),
                "un JSON de forme inattendue ne doit pas planter le client");

        assertNotNull(id.installId());
        assertDoesNotThrow(() -> UUID.fromString(id.installId()));
    }

    @Test
    void returnsNullWhenPersistenceFails() throws Exception {
        // Le parent du chemin cible est un fichier régulier, pas un dossier :
        // Files.createDirectories() dans JsonHelper.save() échoue et l'IOException
        // est avalée par save() (voir sa doc). loadOrCreate() ne doit pas rendre
        // une identité jetable dans ce cas.
        Path blocked = dir.resolve("blocked");
        Files.writeString(blocked, "je ne suis pas un dossier");
        Path file = blocked.resolve("telemetry.json");

        TelemetryId id = TelemetryId.loadOrCreate(file);

        assertNull(id, "une identité jamais persistée ne doit pas être utilisable");
    }

    @Test
    void markAnnouncedOnCorruptFileDoesNotThrow() throws Exception {
        Path file = dir.resolve("telemetry.json");
        TelemetryId id = TelemetryId.loadOrCreate(file);
        Files.writeString(file, "{not valid json");

        assertDoesNotThrow(() -> id.markAnnounced(file),
                "un fichier corrompu ne doit pas faire planter markAnnounced non plus");
    }
}
