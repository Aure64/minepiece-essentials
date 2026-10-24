package com.minepiece.essentials.config;

import java.util.HashSet;
import java.util.Set;

public class ModConfig {

    public boolean bossAlertEnabled = true;
    public int bossAlertThresholdSeconds = 30;
    public boolean bossAutoRefreshEnabled = true;   // relevé /boss silencieux toutes les 5 min + après un respawn
    // Interrupteur global des relevés en arrière-plan (/boss, /pets, /pass quests, /profil).
    // Coupé : plus aucune commande envoyée automatiquement ; le bouton ⟳ manuel reste actif.
    public boolean backgroundRefreshEnabled = true;

    public boolean notificationSoundsEnabled = true;

    public boolean petStatQualityEnabled = true;

    public boolean minionCalculatorEnabled = true;

    public boolean petPanelEnabled = true;

    public boolean armorHudEnabled = true;

    public boolean profileHudEnabled = true;

    public boolean helpDismissed = false;

    // Escape hatch: force the mod active even if server auto-detection fails
    // (e.g. joined by direct IP, or a server address alias we don't recognise).
    public boolean forceMinePieceDetection = false;

    // Island ids collapsed in the Boss Timers HUD (header shown, bosses hidden).
    public Set<String> collapsedBossIslands = new HashSet<>();
    // Bosses hidden from the Boss Timers HUD and its alerts ("islandId:bossName", Shift+clic in K).
    public Set<String> hiddenBosses = new HashSet<>();
    // Compact Boss Timers HUD: no island headers, one flat list sorted by timer.
    public boolean bossCompactMode = false;

    public boolean ascensionHudEnabled = true;

    public boolean hakiTimerEnabled = true;

    public boolean jobHudEnabled = true;

    public boolean passQuestHudEnabled = true;

    public boolean ahPricePerUnitEnabled = true;
    public boolean ahPriceColorEnabled = true;     // liseré couleur + écart % vs moyenne (AH)
    // Couleurs des bandes de prix AH, "#RRGGBB" (voir AhPriceColors). Illisible → défaut.
    public String ahColorCheap = com.minepiece.essentials.ah.AhPriceColors.DEFAULT_CHEAP;         // moins cher que la moyenne
    public String ahColorFair = com.minepiece.essentials.ah.AhPriceColors.DEFAULT_FAIR;           // au prix (±10 %)
    public String ahColorExpensive = com.minepiece.essentials.ah.AhPriceColors.DEFAULT_EXPENSIVE; // trop cher

    public boolean rarityIconsEnabled = true;      // emblèmes dans les coffres/conteneurs
    public boolean rarityInventoryEnabled = true;  // emblèmes dans l'inventaire joueur (E)
    public boolean rarityHotbarEnabled = true;     // emblèmes sur la hotbar (en jeu)
    public boolean rarityFilterEnabled = true;
    public boolean raritySorterEnabled = true;

    // Télémétrie anonyme (voir docs/superpowers/specs/2026-09-01-telemetrie-design.md).
    public boolean telemetryEnabled = true;
}
