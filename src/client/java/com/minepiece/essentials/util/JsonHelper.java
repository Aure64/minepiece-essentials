package com.minepiece.essentials.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonHelper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonHelper() {}

    public static <T> T load(Path path, Class<T> clazz, T defaultValue) {
        if (!Files.exists(path)) {
            save(path, defaultValue);
            return defaultValue;
        }
        try {
            String json = Files.readString(path);
            return GSON.fromJson(json, clazz);
        } catch (IOException e) {
            return defaultValue;
        }
    }

    public static void save(Path path, Object obj) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(obj));
        } catch (IOException e) {
            // logged by caller
        }
    }

    public static Gson gson() { return GSON; }
}
