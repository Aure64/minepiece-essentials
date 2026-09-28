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
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinepieceEssentialsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ModConstants.MOD_NAME);
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "main"));

    private static MinepieceEssentialsClient instance;
    private ConfigManager configManager;

    private KeyMapping editHudKey;
    private KeyMapping helpKey;

    private boolean pendingHelp = false;
    private boolean helpShownThisSession = false;
    private boolean pendingTelemetryNotice = false;

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

        UpdateChecker.init();
        com.minepiece.essentials.telemetry.Telemetry.init();

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
            com.minepiece.essentials.telemetry.Telemetry.onJoinedServer();
            if (!com.minepiece.essentials.telemetry.Telemetry.wasAnnounced()) {
                pendingTelemetryNotice = true;
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            IslandDetector.getInstance().reset();
            BossTracker.getInstance().onConnectionChange();
            com.minepiece.essentials.telemetry.Telemetry.onDisconnected();
        });

        registerKeybinds();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Not gated to MinePiece: players whose detection fails still get notified to update.
            UpdateChecker.tickNotify();

            // Idem : la télémétrie doit partir même si la détection MinePiece échoue.
            com.minepiece.essentials.telemetry.Telemetry.tick();
            if (pendingTelemetryNotice && client.player != null) {
                pendingTelemetryNotice = false;
                client.player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable("minepiece.telemetry.notice")
                        .withColor(0xF0A857));
                com.minepiece.essentials.telemetry.Telemetry.markAnnounced();
            }

            // Auto-learn minion resource XP ratios from the feeding screen.
            MinionFeedLearner.tick();

            if (!ServerDetector.isOnMinePiece()) return;

            BossTracker.getInstance().tick();

            if (pendingHelp && client.gui.screen() == null && client.player != null) {
                client.setScreenAndShow(new HelpScreen());
                pendingHelp = false;
                helpShownThisSession = true;
            }

            while (helpKey.consumeClick()) {
                // Comptée ici uniquement : c'est une pression volontaire de H, à
                // distinguer de l'ouverture automatique au premier lancement (pendingHelp).
                com.minepiece.essentials.telemetry.Telemetry.feature("help_screen");
                client.setScreenAndShow(new HelpScreen());
            }
            while (editHudKey.consumeClick()) {
                client.setScreenAndShow(new HudEditScreen());
            }
        });

        LOGGER.info("Minepiece Essentials initialized with {} HUD elements",
                HudElementRegistry.getElements().size());
    }

    /**
     * Branche les clics de la barre rareté sur TOUS les AbstractContainerScreen (inventaire E inclus).
     * Le RENDU, lui, passe par {@code ScreenRenderMixin} (dessiné avant renderDeferredElements
     * pour rester sous l'infobulle serveur) — pas par afterRender qui dessinerait par-dessus.
     */
    private void registerRarityScreenHooks() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> hs) {
                net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.allowMouseClick(screen)
                    .register((s, click) ->
                        !com.minepiece.essentials.rarity.RarityScreenOverlay.onClick(hs, click.x(), click.y()));
            }
        });
    }

    private void registerKeybinds() {
        // TEMP : dumper de debug (touche P dans un conteneur) — à retirer avant release
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((mc, screen, sw, sh) ->
            net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.afterKeyPress(screen)
                .register((scr, keyEvent) -> {
                    if (keyEvent.input() == GLFW.GLFW_KEY_P
                            && scr instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> cs) {
                        com.minepiece.essentials.debug.DebugDumper.dump(cs);
                    }
                }));
        editHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.minepiece-essentials.edit_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));
        helpKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.minepiece-essentials.help", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
    }

    public static MinepieceEssentialsClient getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
}
