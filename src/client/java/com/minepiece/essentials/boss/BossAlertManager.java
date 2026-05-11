package com.minepiece.essentials.boss;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.island.Island;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import java.util.*;

/**
 * Plays the boss's own laugh/voice when they reach the alert threshold (30s by default).
 * Each boss has its own sound mapped by name.
 */
public class BossAlertManager {
    private static BossAlertManager instance;

    // Tracks which bosses already triggered their alert (island:bossName)
    private final Set<String> alerted = new HashSet<>();

    // Global cooldown to avoid spamming multiple sounds at once
    private long lastSoundTime = 0;
    private static final long SOUND_COOLDOWN_MS = 5_000L;

    public static BossAlertManager getInstance() {
        if (instance == null) instance = new BossAlertManager();
        return instance;
    }

    public void tick() {
        var config = MinepieceEssentialsClient.getInstance().getConfigManager().config();
        if (!config.bossAlertEnabled || !config.notificationSoundsEnabled) return;

        int threshold = config.bossAlertThresholdSeconds;

        Map<Island, List<BossData>> allData = BossTracker.getInstance().getAllBossData();
        for (Map.Entry<Island, List<BossData>> entry : allData.entrySet()) {
            Island island = entry.getKey();
            for (BossData boss : entry.getValue()) {
                if (!boss.hasCoords) continue;

                String key = island.id + ":" + boss.name;
                int timer = boss.estimateCurrentTimer();

                if (timer > 0 && timer <= threshold && !alerted.contains(key)) {
                    alerted.add(key);
                    playBossSound(boss.name);
                } else if (timer > threshold || timer < 0) {
                    alerted.remove(key);
                }
            }
        }
    }

    private void playBossSound(String bossName) {
        // Global cooldown
        long now = System.currentTimeMillis();
        if (now - lastSoundTime < SOUND_COOLDOWN_MS) return;

        SoundEvent sound = ModSounds.getBossSound(bossName);
        if (sound == null) {
            MinepieceEssentialsClient.LOGGER.debug("[BossAlert] No sound for boss: {} (key: {})",
                bossName, ModSounds.normalize(bossName));
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getSoundManager() == null) return;

        lastSoundTime = now;
        client.getSoundManager().play(PositionedSoundInstance.ui(sound, 1.0f));
        MinepieceEssentialsClient.LOGGER.info("[BossAlert] Playing sound for boss: {}", bossName);
    }

    public void reset() {
        alerted.clear();
        lastSoundTime = 0;
    }
}
