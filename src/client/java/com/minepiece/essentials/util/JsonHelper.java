package com.minepiece.essentials.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lecture/écriture JSON robuste.
 *
 * <ul>
 *   <li>Un fichier absent est créé avec la valeur par défaut.</li>
 *   <li>Un fichier vide, tronqué ou corrompu ne fait jamais planter : il est
 *       renommé en {@code *.bak}, la valeur par défaut est retournée et réécrite.</li>
 *   <li>L'écriture passe par un fichier temporaire puis un renommage atomique :
 *       une coupure en pleine sauvegarde ne laisse jamais un fichier vide.</li>
 * </ul>
 */
public final class JsonHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("Minepiece Essentials/Json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonHelper() {}

    public static <T> T load(Path path, Class<T> clazz, T defaultValue) {
        if (!Files.exists(path)) {
            save(path, defaultValue);
            return defaultValue;
        }
        try {
            String json = Files.readString(path);
            T value = json.isBlank() ? null : GSON.fromJson(json, clazz);
            if (value != null) return value;
            LOGGER.warn("{} est vide — valeurs par défaut", path.getFileName());
        } catch (IOException e) {
            LOGGER.warn("Lecture impossible de {} ({}) — valeurs par défaut", path.getFileName(), e.getMessage());
            return defaultValue;
        } catch (RuntimeException e) {
            LOGGER.warn("{} est corrompu ({}) — sauvegardé en .bak, valeurs par défaut",
                    path.getFileName(), e.getMessage());
        }
        quarantine(path);
        save(path, defaultValue);
        return defaultValue;
    }

    /** @return true si le fichier a été écrit. */
    public static boolean save(Path path, Object obj) {
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(tmp, GSON.toJson(obj));
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            LOGGER.warn("Sauvegarde impossible de {} : {}", path.getFileName(), e.getMessage());
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) { }
            return false;
        }
    }

    private static void quarantine(Path path) {
        Path bak = path.resolveSibling(path.getFileName() + ".bak");
        try {
            Files.move(path, bak, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Impossible de renommer {} en .bak : {}", path.getFileName(), e.getMessage());
        }
    }

    public static Gson gson() { return GSON; }
}
