package com.minepiece.essentials.config;

import com.minepiece.essentials.ModConstants;
import com.minepiece.essentials.util.JsonHelper;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;

public class ConfigManager {
    private static final Path CONFIG_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve(ModConstants.CONFIG_DIR);
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("config.json");
    private static final Path LAYOUT_PATH = CONFIG_DIR.resolve("layouts.json");

    private ModConfig config;
    private LayoutConfig layout;

    public void load() {
        config = JsonHelper.load(CONFIG_PATH, ModConfig.class, new ModConfig());
        layout = JsonHelper.load(LAYOUT_PATH, LayoutConfig.class, new LayoutConfig());
    }

    public void save() {
        JsonHelper.save(CONFIG_PATH, config);
        JsonHelper.save(LAYOUT_PATH, layout);
    }

    public ModConfig config() { return config; }
    public LayoutConfig layout() { return layout; }
    public Path dataDir() { return CONFIG_DIR.resolve("data"); }
    public Path bossDir() { return CONFIG_DIR.resolve("bosses"); }
    public Path waypointDir() { return CONFIG_DIR.resolve("waypoints"); }
}
