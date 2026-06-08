package com.minepiece.essentials;

import com.minepiece.essentials.boss.BossTimerHud;
import com.minepiece.essentials.boss.BossTracker;
import com.minepiece.essentials.boss.ModSounds;
import com.minepiece.essentials.boss.WaypointManager;
import com.minepiece.essentials.ascension.AscensionHud;
import com.minepiece.essentials.config.ConfigManager;
import com.minepiece.essentials.haki.HakiHud;
import com.minepiece.essentials.haki.HakiTimer;
import com.minepiece.essentials.help.HelpScreen;
import com.minepiece.essentials.hud.HudEditScreen;
import com.minepiece.essentials.hud.HudElementRegistry;
import com.minepiece.essentials.island.IslandDetector;
import com.minepiece.essentials.job.JobHud;
import com.minepiece.essentials.job.JobTracker;
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
        HudElementRegistry.register(new AscensionHud());
        HudElementRegistry.register(new HakiHud());
        HudElementRegistry.register(new JobHud());
        HudElementRegistry.register(new com.minepiece.essentials.quest.PassQuestHud());

        HakiTimer.init();
        com.minepiece.essentials.quest.PassQuestScanner.init();

        PetStatTooltip.register();
        MinionTooltip.register();
        com.minepiece.essentials.ah.AhTooltip.register();

        com.minepiece.essentials.rarity.RarityHotbarOverlay.register();
        registerRarityScreenHooks();
        registerDebugDumper(); // TEMP : à retirer avant release

        UpdateChecker.init();

        // Reset transient state (queues, last island) on every server join/disconnect.
        // Without this, refreshQueue can persist across reconnects and resume firing.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerDetector.reset();
            IslandDetector.getInstance().reset();
            BossTracker.getInstance().onConnectionChange();
            JobTracker.reset();
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

    /**
     * Branche les clics de la barre rareté sur TOUS les HandledScreen (inventaire E inclus).
     * Le RENDU, lui, passe par {@code ScreenRenderMixin} (dessiné avant drawDeferredElements
     * pour rester sous l'infobulle serveur) — pas par afterRender qui dessinerait par-dessus.
     */
    private void registerRarityScreenHooks() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> hs) {
                net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.allowMouseClick(screen)
                    .register((s, click) ->
                        !com.minepiece.essentials.rarity.RarityScreenOverlay.onClick(hs, click.x(), click.y()));
            }
        });
    }

    /** TEMP — dump de l'écran conteneur ouvert (touche P) pour relever les chaînes serveur. À RETIRER avant release. */
    private void registerDebugDumper() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.afterKeyPress(screen)
                .register((scr, keyInput) -> {
                    if (keyInput.key() == GLFW.GLFW_KEY_P
                            && scr instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> hs) {
                        com.minepiece.essentials.debug.DebugDumper.dump(hs);
                    }
                });
        });
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
