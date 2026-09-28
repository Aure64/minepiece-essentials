package com.minepiece.essentials.network;

import com.minepiece.essentials.MinepieceEssentialsClient;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.item.ItemStack;

/**
 * Relevé silencieux d'un écran serveur : envoie une commande, laisse
 * {@link ServerGuiInterceptor} lire le contenu, referme. Aucun clic n'est
 * jamais envoyé. Un seul relevé à la fois, cooldown global de
 * {@value #COOLDOWN_MS} ms, jamais pendant qu'un écran joueur est ouvert.
 */
public final class BackgroundGuiRefresh {
    public static final long COOLDOWN_MS = 5000;
    private static final long HARD_TIMEOUT_MS = 8000;

    private static long lastRefreshTime = 0;
    private static boolean busy = false;
    private static long busySince = 0;
    /** Écran du mod ouvert au moment de l'envoi (éditeur K…), remis en place à la fin. */
    private static Screen screenToRestore = null;

    /** L'écran à remettre après fermeture de l'écran intercepté (null = aucun). */
    public static Screen screenToRestore() { return screenToRestore; }

    private BackgroundGuiRefresh() {}

    public static boolean isBusy() { return busy; }

    /** True when the global cooldown has elapsed since the last refresh start. */
    public static boolean isReady() {
        return System.currentTimeMillis() - lastRefreshTime >= COOLDOWN_MS;
    }

    /** Milliseconds remaining before another refresh can fire. 0 if ready. */
    public static long getCooldownRemainingMs() {
        return Math.max(0, COOLDOWN_MS - (System.currentTimeMillis() - lastRefreshTime));
    }

    /**
     * Envoie la commande et livre les items de l'écran qui s'ouvre.
     * @return false si rien n'est parti (occupé, cooldown, écran joueur ouvert) —
     *         l'appelant garde sa demande en attente.
     */
    public static boolean sendCommand(String command, Consumer<Map<Integer, ItemStack>> onItems) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null || busy) return false;
        if (!isReady()) return false;
        // Ne jamais voler un écran conteneur au joueur (coffre, inventaire, GUI serveur) :
        // on repassera au tick suivant. Un écran du mod (éditeur K) est mémorisé et
        // restauré une fois l'écran serveur lu.
        Screen current = client.gui.screen();
        if (current instanceof AbstractContainerScreen<?>) return false;
        screenToRestore = current;

        long now = System.currentTimeMillis();
        busy = true;
        busySince = now;
        lastRefreshTime = now;

        ServerGuiInterceptor.start(items -> {
            MinepieceEssentialsClient.LOGGER.info("[BGRefresh] Got {} items for {}", items.size(), command);
            onItems.accept(items);
        });

        String cmd = command.startsWith("/") ? command.substring(1) : command;
        client.player.connection.sendCommand(cmd);
        return true;
    }

    /** Thread client, chaque tick — toujours appelé, même hors MinePiece. */
    public static void tick() {
        if (!busy) return;
        ServerGuiInterceptor.tick();

        if (ServerGuiInterceptor.isFinished() || !ServerGuiInterceptor.isIntercepting()) {
            finish();
            return;
        }
        if (System.currentTimeMillis() - busySince > HARD_TIMEOUT_MS) {
            MinepieceEssentialsClient.LOGGER.warn("[BGRefresh] Hard timeout — forcing reset");
            finish();
        }
    }

    /** Ferme l'écran intercepté (et seulement lui), puis libère. */
    public static void finish() {
        closeInterceptedScreen();
        ServerGuiInterceptor.stop();
        busy = false;
        screenToRestore = null;
    }

    /** Oubli sans paquet : changement de serveur / déconnexion. */
    public static void reset() {
        ServerGuiInterceptor.stop();
        busy = false;
        screenToRestore = null;
    }

    private static void closeInterceptedScreen() {
        Minecraft client = Minecraft.getInstance();
        int syncId = ServerGuiInterceptor.getExpectedSyncId();
        if (client.getConnection() == null || syncId < 0) return;

        if (client.gui.screen() instanceof AbstractContainerScreen<?> cs
                && cs.getMenu().containerId == syncId) {
            // Remplacer un écran conteneur envoie lui-même le paquet de fermeture.
            client.setScreenAndShow(screenToRestore);
        } else if (!ServerGuiInterceptor.wasScreenClosedClientSide()) {
            // L'écran n'est plus affiché mais personne n'a prévenu le serveur.
            client.getConnection().send(new ServerboundContainerClosePacket(syncId));
        }
    }
}
