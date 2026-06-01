package com.minepiece.essentials;

import com.minepiece.essentials.boss.BossTimerHud;
import com.minepiece.essentials.boss.BossTracker;
import com.minepiece.essentials.boss.ModSounds;
import com.minepiece.essentials.boss.WaypointManager;
import com.minepiece.essentials.config.ConfigManager;
import com.minepiece.essentials.help.HelpScreen;
import com.minepiece.essentials.hud.HudEditScreen;
import com.minepiece.essentials.hud.HudElementRegistry;
import com.minepiece.essentials.island.IslandDetector;
import com.minepiece.essentials.pet.ActivePetsHud;
import com.minepiece.essentials.pet.MinionFeedLearner;
import com.minepiece.essentials.pet.MinionTooltip;
import com.minepiece.essentials.pet.PetStatTooltip;
import com.minepiece.essentials.quest.ParcheminHud;
import com.minepiece.essentials.update.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
    private KeyBinding helpKey;

    private boolean pendingHelp = false;
    private boolean helpShownThisSession = false;

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
        HudElementRegistry.register(new ActivePetsHud());

        PetStatTooltip.register();
        MinionTooltip.register();

        UpdateChecker.init();

        // Reset transient state (queues, last island) on every server join/disconnect.
        // Without this, refreshQueue can persist across reconnects and resume firing.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            IslandDetector.getInstance().reset();
            BossTracker.getInstance().onConnectionChange();
            if (!configManager.config().helpDismissed && !helpShownThisSession) {
                pendingHelp = true;
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            IslandDetector.getInstance().reset();
            BossTracker.getInstance().onConnectionChange();
        });

        registerKeybinds();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Not gated to MinePiece: players whose detection fails still get notified to update.
            UpdateChecker.tickNotify();

            // Auto-learn minion resource XP ratios from the feeding screen.
            MinionFeedLearner.tick();

            if (!ServerDetector.isOnMinePiece()) return;

            BossTracker.getInstance().tick();

            if (pendingHelp && client.currentScreen == null && client.player != null) {
                client.setScreen(new HelpScreen());
                pendingHelp = false;
                helpShownThisSession = true;
            }

            while (helpKey.wasPressed()) {
                client.setScreen(new HelpScreen());
            }
            while (editHudKey.wasPressed()) {
                client.setScreen(new HudEditScreen());
            }
        });

        LOGGER.info("Minepiece Essentials initialized with {} HUD elements",
                HudElementRegistry.getElements().size());
    }

    private void registerKeybinds() {
        editHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.minepiece-essentials.edit_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));
        helpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.minepiece-essentials.help", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
    }

    public static MinepieceEssentialsClient getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
}
