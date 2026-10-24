package com.minepiece.essentials.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerLevelTest {
    @Test
    void readsLevelAndPercentFromTheBossBar() {
        PlayerLevel.reset();
        PlayerLevel.onBossBarText("eb者 17990eb蓝 Niveau 97 (58.59%)eb侧");
        assertEquals(97, PlayerLevel.level());
        assertEquals(58.59, PlayerLevel.percent(), 1e-9);
        PlayerLevel.onBossBarText("Village de Fuchsia"); // zone segment: keep last value
        assertEquals(97, PlayerLevel.level());
        PlayerLevel.onBossBarText("Level 98 (0.10%)");
        assertEquals(98, PlayerLevel.level());
    }
}
