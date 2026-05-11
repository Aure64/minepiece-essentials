package com.minepiece.essentials;

import com.minepiece.essentials.boss.BossTimerHud;
import com.minepiece.essentials.boss.BossTracker;
import com.minepiece.essentials.boss.ModSounds;
import com.minepiece.essentials.boss.WaypointManager;
import com.minepiece.essentials.config.ConfigManager;
import com.minepiece.essentials.hud.HudEditScreen;
import com.minepiece.essentials.hud.HudElementRegistry;
import com.minepiece.essentials.island.IslandDetector;
import com.minepiece.essentials.quest.ParcheminHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinepieceEssentialsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ModConstants.MOD_NAME);
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
        Identifier.of(ModConstants.MOD_ID, "main"));

    private static MinepieceEssentialsClient instance;
    private ConfigManager configManager;

    private KeyBinding editHudKey;
    private KeyBinding placeWaypointKey;

    @Override
    public void onInitializeClient() {
        instance = this;

        configManager = new ConfigManager();
        configManager.load();

        ModSounds.register();

        HudElementRegistry.init();
        IslandDetector.getInstance();

        BossTracker.getInstance().init();
        WaypointManager.getInstance().init();

        HudElementRegistry.register(new BossTimerHud());
        HudElementRegistry.register(new ParcheminHud());

        registerKeybinds();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ServerDetector.isOnMinePiece()) return;

            BossTracker.getInstance().tick();

            while (editHudKey.wasPressed()) {
                client.setScreen(new HudEditScreen());
            }
            while (placeWaypointKey.wasPressed()) {
                if (client.player != null) {
                    var island = IslandDetector.getInstance().getCurrentIsland();
                    WaypointManager.getInstance().addManualWaypoint(
                        island, "Waypoint",
                        (int) client.player.getX(),
                        (int) client.player.getY(),
                        (int) client.player.getZ(), 6);
                }
            }
        });

        LOGGER.info("Minepiece Essentials initialized with {} HUD elements",
                HudElementRegistry.getElements().size());
    }

    private void registerKeybinds() {
        editHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.minepiece-essentials.edit_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));
        placeWaypointKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.minepiece-essentials.waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
    }

    public static MinepieceEssentialsClient getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
}
