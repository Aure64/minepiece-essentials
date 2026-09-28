package com.minepiece.essentials.network;

import com.minepiece.essentials.MinepieceEssentialsClient;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.item.ItemStack;

/**
 * Relevé silencieux d'un écran serveur : envoie une commande, laisse
 * {@link ServerGuiInterceptor} lire le contenu (l'écran n'est jamais affiché),
 * prévient le serveur de la fermeture. Aucun clic n'est jamais envoyé. Un seul
 * relevé à la fois, cooldown global de {@value #COOLDOWN_MS} ms, jamais pendant
 * qu'un écran conteneur est ouvert (le serveur remplacerait son menu).
 */
public final class BackgroundGuiRefresh {
    public static final long COOLDOWN_MS = 1500;
    private static final long HARD_TIMEOUT_MS = 8000;

    private static long lastRefreshTime = 0;
    private static boolean busy = false;
    private static long busySince = 0;

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
        // on repassera au tick suivant. Les autres écrans (éditeur K…) ne gênent pas.
        if (client.gui.screen() instanceof AbstractContainerScreen<?>) return false;

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

    /** Prévient le serveur que son écran est fermé, puis libère. */
    public static void finish() {
        closeInterceptedScreen();
        ServerGuiInterceptor.stop();
        busy = false;
    }

    /** Oubli sans paquet : changement de serveur / déconnexion. */
    public static void reset() {
        ServerGuiInterceptor.stop();
        busy = false;
    }

    private static void closeInterceptedScreen() {
        Minecraft client = Minecraft.getInstance();
        int syncId = ServerGuiInterceptor.getExpectedSyncId();
        if (client.getConnection() == null || syncId < 0) return;
        // L'ouverture a été annulée côté client : rien à fermer ici, on prévient
        // simplement le serveur (un seul paquet, pour l'id qu'il nous a donné).
        client.getConnection().send(new ServerboundContainerClosePacket(syncId));
    }
}
