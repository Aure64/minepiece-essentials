package com.minepiece.essentials.config;

import java.util.HashSet;
import java.util.Set;

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

    // Escape hatch: force the mod active even if server auto-detection fails
    // (e.g. joined by direct IP, or a server address alias we don't recognise).
    public boolean forceMinePieceDetection = false;

    // Island ids collapsed in the Boss Timers HUD (header shown, bosses hidden).
    public Set<String> collapsedBossIslands = new HashSet<>();

    public boolean ascensionHudEnabled = true;

    public boolean hakiTimerEnabled = true;

    public boolean jobHudEnabled = true;

    public boolean passQuestHudEnabled = true;

    public boolean ahPricePerUnitEnabled = true;
    public boolean ahPriceColorEnabled = true;     // liseré couleur + écart % vs moyenne (AH)

    public boolean rarityIconsEnabled = true;      // emblèmes dans les coffres/conteneurs
    public boolean rarityInventoryEnabled = true;  // emblèmes dans l'inventaire joueur (E)
    public boolean rarityHotbarEnabled = true;     // emblèmes sur la hotbar (en jeu)
    public boolean rarityFilterEnabled = true;
    public boolean raritySorterEnabled = true;

    // Télémétrie anonyme (voir docs/superpowers/specs/2026-09-01-telemetrie-design.md).
    public boolean telemetryEnabled = true;
}
