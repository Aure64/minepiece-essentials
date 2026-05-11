package com.minepiece.essentials.boss;

import com.minepiece.essentials.island.Island;

public class BossData {
    public String name;
    public transient Island island;
    public String islandId;
    public int x, y, z;
    public boolean hasCoords;
    public int respawnIntervalSeconds;
    public long lastKnownRespawnTimestamp;
    public int lastKnownTimerSeconds;
    public String type;

    public BossData() {}

    public BossData(String name, Island island) {
        this.name = name;
        this.island = island;
        this.islandId = island.id;
    }

    public int estimateCurrentTimer() {
        if (lastKnownRespawnTimestamp == 0) return -1;
        long elapsed = (System.currentTimeMillis() - lastKnownRespawnTimestamp) / 1000;
        int estimated = lastKnownTimerSeconds - (int) elapsed;
        return Math.max(0, estimated);
    }

    public boolean isAvailable() {
        return estimateCurrentTimer() == 0;
    }

    public String formatTimer() {
        int timer = estimateCurrentTimer();
        if (timer < 0) return "??:??";
        if (timer == 0) return "READY";
        int min = timer / 60;
        int sec = timer % 60;
        return String.format("%dm%02ds", min, sec);
    }

    public double distanceTo(double px, double py, double pz) {
        if (!hasCoords) return -1;
        double dx = px - x, dy = py - y, dz = pz - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
