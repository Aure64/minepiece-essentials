package com.minepiece.essentials.config;

import com.minepiece.essentials.ModConstants;
import com.minepiece.essentials.util.JsonHelper;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Config + layouts. {@link #save()} ne fait que marquer « à écrire » : l'écriture
 * réelle est regroupée ({@link #tick()}, au plus une fois par seconde) ou forcée
 * par {@link #flush()} (fermeture de l'éditeur, arrêt du client). Évite une
 * écriture disque synchrone à chaque clic dans l'éditeur.
 */
public class ConfigManager {
    private static final Path CONFIG_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve(ModConstants.CONFIG_DIR);
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("config.json");
    private static final Path LAYOUT_PATH = CONFIG_DIR.resolve("layouts.json");
    private static final long FLUSH_DELAY_MS = 1000;

    private ModConfig config;
    private LayoutConfig layout;
    private boolean dirty;
    private long dirtySince;

    public void load() {
        config = JsonHelper.load(CONFIG_PATH, ModConfig.class, new ModConfig());
        layout = JsonHelper.load(LAYOUT_PATH, LayoutConfig.class, new LayoutConfig());
        // Gson accepte un "null" explicite là où le code attend une collection.
        if (config.collapsedBossIslands == null) config.collapsedBossIslands = new HashSet<>();
        if (layout.activeProfile == null || layout.activeProfile.isBlank()) layout.activeProfile = "default";
        if (layout.profiles == null) layout.profiles = new HashMap<>();
        for (LayoutConfig.Profile profile : layout.profiles.values()) {
            if (profile.elements == null) profile.elements = new HashMap<>();
            profile.elements.values().removeIf(e -> e == null);
            for (LayoutConfig.ElementLayout e : profile.elements.values()) {
                if (e.background == null) e.background = HudBackground.PARCHMENT;
                if (e.scale <= 0) e.scale = 1.0f;
            }
        }
    }

    /** Marque la config comme modifiée ; écrite au prochain {@link #tick()} ou {@link #flush()}. */
    public void save() {
        if (!dirty) dirtySince = System.currentTimeMillis();
        dirty = true;
    }

    /** Thread client, chaque tick. */
    public void tick() {
        if (dirty && System.currentTimeMillis() - dirtySince >= FLUSH_DELAY_MS) flush();
    }

    /** Écrit immédiatement si quelque chose a changé. */
    public void flush() {
        if (!dirty) return;
        dirty = false;
        JsonHelper.save(CONFIG_PATH, config);
        JsonHelper.save(LAYOUT_PATH, layout);
    }

    public ModConfig config() { return config; }
    public LayoutConfig layout() { return layout; }
    public Path dataDir() { return CONFIG_DIR.resolve("data"); }
    public Path bossDir() { return CONFIG_DIR.resolve("bosses"); }
    public Path waypointDir() { return CONFIG_DIR.resolve("waypoints"); }
    public Path telemetryFile() { return CONFIG_DIR.resolve("telemetry.json"); }
}
