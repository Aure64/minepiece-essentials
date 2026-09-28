package com.minepiece.essentials.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonHelperTest {

    static class Cfg {
        public int value = 7;
        public String name = "x";
    }

    @Test
    void missingFileIsCreatedWithDefaults(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("sub").resolve("cfg.json");
        Cfg loaded = JsonHelper.load(f, Cfg.class, new Cfg());
        assertEquals(7, loaded.value);
        assertTrue(Files.exists(f));
        assertFalse(Files.exists(dir.resolve("sub").resolve("cfg.json.tmp")));
    }

    @Test
    void roundTrip(@TempDir Path dir) {
        Path f = dir.resolve("cfg.json");
        Cfg c = new Cfg();
        c.value = 42;
        c.name = "abc";
        assertTrue(JsonHelper.save(f, c));
        Cfg back = JsonHelper.load(f, Cfg.class, new Cfg());
        assertEquals(42, back.value);
        assertEquals("abc", back.name);
    }

    @Test
    void emptyFileFallsBackToDefaultsAndIsQuarantined(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("cfg.json");
        Files.writeString(f, "");
        Cfg loaded = JsonHelper.load(f, Cfg.class, new Cfg());
        assertEquals(7, loaded.value);
        assertTrue(Files.exists(dir.resolve("cfg.json.bak")));
        assertTrue(Files.readString(f).contains("\"value\": 7"));
    }

    @Test
    void corruptFileFallsBackToDefaultsAndIsQuarantined(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("cfg.json");
        Files.writeString(f, "{\"value\": 12, \"na");
        Cfg loaded = JsonHelper.load(f, Cfg.class, new Cfg());
        assertEquals(7, loaded.value);
        assertEquals("{\"value\": 12, \"na", Files.readString(dir.resolve("cfg.json.bak")));
    }

    @Test
    void unknownKeysAreIgnored(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("cfg.json");
        Files.writeString(f, "{\"value\": 3, \"removedOption\": true}");
        assertEquals(3, JsonHelper.load(f, Cfg.class, new Cfg()).value);
    }
}
