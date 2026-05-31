package com.minepiece.essentials.config;

public class ModConfig {
    public boolean islandDetectorEnabled = true;
    public boolean bossTrackerEnabled = true;
    public boolean questTrackerEnabled = true;

    public String detectionMethod = "both";

    public int bossRefreshIntervalSeconds = 180;
    public boolean bossAlertEnabled = true;
    public int bossAlertThresholdSeconds = 30;

    public int globalRefreshCooldownMs = 5000;

    public boolean notificationSoundsEnabled = true;

    public boolean petStatQualityEnabled = true;

    public boolean minionCalculatorEnabled = true;

    public boolean petPanelEnabled = true;

    public boolean helpDismissed = false;
}
