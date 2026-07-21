package com.minepiece.essentials.i18n;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerTextTest {
    @Test void matchesBothLanguages() {
        assertTrue(ServerText.matches(" ▪ Prix de vente: 10M", ServerText.SELL_PRICE));
        assertTrue(ServerText.matches(" ▪ Selling price: 10M", ServerText.SELL_PRICE));
        assertFalse(ServerText.matches(" ▪ Average price: 9K", ServerText.SELL_PRICE));
    }
    @Test void petLevelPatternBilingual() {
        assertTrue(ServerText.PET_LEVEL.matcher("Niveau: 7").find());
        assertTrue(ServerText.PET_LEVEL.matcher("Level: 7").find());
        assertEquals("Max", firstGroup("Level: Max"));
    }
    @Test void expireDateBilingual() {
        assertTrue(ServerText.EXPIRE.matcher("Expire le 13/06/2026 14h47").find());
        assertTrue(ServerText.EXPIRE.matcher("Expires on 13/06/2026 14h47").find());
    }
    @Test void questNameBilingual() {
        var fr = ServerText.QUEST_NAME.matcher("Quête #3 (Difficile)");
        var en = ServerText.QUEST_NAME.matcher("Quest #3 (Hard)");
        assertTrue(fr.find()); assertTrue(en.find());
    }
    @Test void petEffectsSectionVariants() {
        assertTrue(ServerText.matches("Familiar Effects", ServerText.PET_EFFECTS));
        assertTrue(ServerText.matches("Familier Effects", ServerText.PET_EFFECTS));
        assertTrue(ServerText.matches("Pet Effects", ServerText.PET_EFFECTS), "pets fusionnes");
        assertFalse(ServerText.matches("Minion Effects:", ServerText.PET_EFFECTS), "ne doit pas matcher la fin de section");
    }
    @Test void bossRespawnBilingual() {
        assertTrue(ServerText.BOSS_RESPAWN.matcher("Respawn: 0s (15 Minutes)").find());
        assertTrue(ServerText.BOSS_RESPAWN.matcher("Réapparition: 2m 30s").find());
        // EN mini-boss (Nightmare Luffy, Perona, Oz…) : "Spawn:" et non "Respawn:"
        var en = ServerText.BOSS_RESPAWN.matcher("Spawn: 11m 52s (Every 15 Minutes)");
        assertTrue(en.find());
        assertEquals("11", en.group(1));
        assertEquals("52", en.group(2));
    }
    @Test void bossIntervalBilingual() {
        var fr = ServerText.BOSS_INTERVAL.matcher("Toutes les 15 Minutes");
        var en = ServerText.BOSS_INTERVAL.matcher("Respawn: 0s (15 Minutes)");
        assertTrue(fr.find()); assertEquals("15", fr.group(1));
        assertTrue(en.find()); assertEquals("15", en.group(1));
        var enMini = ServerText.BOSS_INTERVAL.matcher("Spawn: 11m 52s (Every 15 Minutes)");
        assertTrue(enMini.find()); assertEquals("15", enMini.group(1));
    }
    private static String firstGroup(String s) {
        var m = ServerText.PET_LEVEL.matcher(s);
        return m.find() ? m.group(1) : null;
    }
}
