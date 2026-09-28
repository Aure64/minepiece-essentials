package com.minepiece.essentials.boss;

import com.minepiece.essentials.island.Island;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Relevé réel de l'écran /boss sur dev.minepiece.net (26.2), 2026-09-28. */
class BossScreenParserTest {

    private static BossScreenParser.Entry island(String name, String... lore) {
        return new BossScreenParser.Entry(name, List.of(lore));
    }

    @Test
    void parsesSingleBossIsland() {
        Map<Island, List<BossData>> r = BossScreenParser.parse(List.of(island("Fuchsia",
                "",
                "⚔ Luffy",
                " ▪ Coordonnées: 23 206 10987",
                " ▪ Apparition: 14m 40s (Toutes les 15 Minutes)",
                "",
                "烧 → Plus d'informations")));

        List<BossData> bosses = r.get(Island.FUCHSIA);
        assertNotNull(bosses);
        assertEquals(1, bosses.size());
        BossData luffy = bosses.get(0);
        assertEquals("Luffy", luffy.name);
        assertEquals("fuchsia", luffy.islandId);
        assertTrue(luffy.hasCoords);
        assertEquals(23, luffy.x);
        assertEquals(206, luffy.y);
        assertEquals(10987, luffy.z);
        assertEquals(14 * 60 + 40, luffy.lastKnownTimerSeconds);
        assertEquals(15 * 60, luffy.respawnIntervalSeconds);
        assertTrue(luffy.lastKnownRespawnTimestamp > 0);
    }

    @Test
    void parsesMultiBossIslandAndZeroTimer() {
        Map<Island, List<BossData>> r = BossScreenParser.parse(List.of(island("Thriller Bark",
                "",
                "⚔ Perona",
                " ▪ Coordonnées: -2151 251 7345",
                " ▪ Apparition: 0s (Toutes les 15 Minutes)",
                "",
                "⚔ Oz",
                " ▪ Coordonnées: -2051 203 7304",
                " ▪ Apparition: 0s (Toutes les 15 Minutes)",
                "",
                "⚔ Nightmare Luffy",
                " ▪ Coordonnées: -2153 218 7273",
                " ▪ Apparition: 0s (Toutes les 15 Minutes)",
                "",
                "烧 → Plus d'informations")));

        List<BossData> bosses = r.get(Island.THRILLER_BARK);
        assertEquals(List.of("Perona", "Oz", "Nightmare Luffy"),
                bosses.stream().map(b -> b.name).toList());
        assertEquals(-2151, bosses.get(0).x);
        assertEquals(0, bosses.get(0).lastKnownTimerSeconds);
        assertEquals(0, bosses.get(0).estimateCurrentTimer());
        assertTrue(bosses.get(0).isAvailable());
    }

    @Test
    void mapsServerIslandNamesToEnum() {
        assertEquals(Island.DRUM, Island.fromScreenName("Drum"));
        assertEquals(Island.WHOLE_CAKE, Island.fromScreenName("Whole Cake Island"));
        assertEquals(Island.ILE_HOMMES_POISSONS, Island.fromScreenName("Île des Hommes-Poissons"));
        assertEquals(Island.KOMUGI, Island.fromScreenName("Komugi"));
        assertEquals(Island.UNKNOWN, Island.fromScreenName("Brown Dye"));
        assertEquals(Island.UNKNOWN, Island.fromScreenName("Fermer"));
        assertEquals(Island.UNKNOWN, Island.fromScreenName(""));
    }

    @Test
    void skipsFillerAndNavigationItems() {
        Map<Island, List<BossData>> r = BossScreenParser.parse(List.of(
                island("Brown Dye"),
                island("Fermer"),
                island("Zou", "", "⚔ Jack", " ▪ Coordonnées: 197 715 3000",
                        " ▪ Apparition: 0s (Toutes les 15 Minutes)")));
        assertEquals(1, r.size());
        assertTrue(r.containsKey(Island.ZOU));
    }

    @Test
    void dropsBossWithoutCoordinates() {
        Map<Island, List<BossData>> r = BossScreenParser.parse(List.of(
                island("Alabasta", "⚔ Ressources obtenables", " ▪ 10 军")));
        assertTrue(r.isEmpty());
    }
}
