package com.minepiece.essentials.telemetry;

import com.minepiece.essentials.util.JsonHelper;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Identité anonyme de l'installation.
 *
 * <p>Un UUID aléatoire, généré au premier lancement et persisté à côté de la
 * config. Il n'est dérivé d'aucune donnée du joueur : supprimer le fichier ou
 * réinstaller le mod donne une nouvelle identité, ce qui est le comportement
 * voulu.
 *
 * <p>Le chemin est passé en paramètre plutôt que lu depuis FabricLoader, pour
 * que la classe reste testable hors de Minecraft.
 */
public final class TelemetryId {

    /** Forme sérialisée du fichier telemetry.json. */
    private static final class Stored {
        String installId;
        String firstSeen;
        boolean announced;
    }

    private final String installId;
    private final boolean announced;

    private TelemetryId(String installId, boolean announced) {
        this.installId = installId;
        this.announced = announced;
    }

    public static TelemetryId loadOrCreate(Path file) {
        Stored stored = loadSafely(file);
        if (stored == null || stored.installId == null || stored.installId.isBlank()) {
            stored = new Stored();
            stored.installId = UUID.randomUUID().toString();
            stored.firstSeen = java.time.LocalDate.now().toString();
            stored.announced = false;
            JsonHelper.save(file, stored);
        }
        return new TelemetryId(stored.installId, stored.announced);
    }

    public String installId() { return installId; }
    public boolean announced() { return announced; }

    public void markAnnounced(Path file) {
        Stored stored = loadSafely(file);
        if (stored == null) return;
        stored.announced = true;
        JsonHelper.save(file, stored);
    }

    /**
     * Lit le fichier via JsonHelper en absorbant toute exception non-vérifiée
     * (JSON tronqué/corrompu, forme inattendue). La télémétrie ne doit jamais
     * faire planter le client : un fichier corrompu est traité comme absent.
     */
    private static Stored loadSafely(Path file) {
        try {
            return JsonHelper.load(file, Stored.class, null);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
