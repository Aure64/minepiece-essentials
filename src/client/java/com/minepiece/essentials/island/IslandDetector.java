package com.minepiece.essentials.island;

import com.minepiece.essentials.MinepieceEssentialsClient;

public class IslandDetector {
    private Island currentIsland = Island.UNKNOWN;
    private static IslandDetector instance;

    public static IslandDetector getInstance() {
        if (instance == null) instance = new IslandDetector();
        return instance;
    }

    private long lastBossBarLog = 0;

    public void onBossBarUpdate(String text) {
        // Log bossbar text periodically to debug detection
        if (System.currentTimeMillis() - lastBossBarLog > 5000) {
            lastBossBarLog = System.currentTimeMillis();
            MinepieceEssentialsClient.LOGGER.debug("[IslandDetector] Bossbar text: '{}'", text);
        }

        Island detected = Island.fromBossbarText(text);
        if (detected != Island.UNKNOWN && detected != currentIsland) {
            Island previous = currentIsland;
            currentIsland = detected;
            IslandChangeCallback.EVENT.invoker().onIslandChanged(previous, currentIsland);
            MinepieceEssentialsClient.LOGGER.info("Island changed: {} -> {}",
                previous.displayName, currentIsland.displayName);
        }
    }

    public Island getCurrentIsland() { return currentIsland; }

    public void reset() {
        currentIsland = Island.UNKNOWN;
    }
}
