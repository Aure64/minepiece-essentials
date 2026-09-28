package com.minepiece.essentials.network;

import com.minepiece.essentials.MinepieceEssentialsClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;

/**
 * Collecte passivement le contenu du prochain écran conteneur ouvert par le
 * serveur (après une commande envoyée par {@link BackgroundGuiRefresh}).
 *
 * <p>Cycle : {@link #start} → {@link #claimScreen} (l'ouverture est annulée côté
 * client : aucun écran ne s'affiche, aucun clignotement) → paquets de contenu
 * → livraison au callback dès réception → {@link #isFinished}.
 * {@link BackgroundGuiRefresh} prévient alors le serveur de la fermeture et appelle {@link #stop}.
 *
 * <p>Tout est confiné au thread client : les mixins réseau filtrent le thread
 * Netty avant d'appeler ici.
 */
public final class ServerGuiInterceptor {
    /** Marge après le paquet de contenu complet pour laisser passer d'éventuels set-slot. */
    private static final long SETTLE_MS = 100;
    /** Sans paquet de contenu complet, on livre ce qu'on a après ce délai. */
    private static final long COLLECT_DURATION_MS = 600;
    /** Délai max entre l'envoi de la commande et l'ouverture de l'écran. */
    private static final long OPEN_DEADLINE_MS = 3000;
    /** Délai max entre l'ouverture et la livraison (sécurité). */
    private static final long SCREEN_TIMEOUT_MS = 5000;

    private static boolean intercepting = false;
    private static boolean finished = false;
    private static int currentSyncId = -1;
    private static long startTime = 0;
    private static long screenOpenTime = 0;
    private static long contentReceivedAt = 0;
    private static final Map<Integer, ItemStack> collectedItems = new HashMap<>();
    private static Consumer<Map<Integer, ItemStack>> callback;

    private ServerGuiInterceptor() {}

    public static boolean isIntercepting() { return intercepting; }
    /** Items livrés (ou abandon) : l'appelant doit fermer l'écran puis {@link #stop}. */
    public static boolean isFinished() { return finished; }
    public static int getExpectedSyncId() { return currentSyncId; }

    public static void start(Consumer<Map<Integer, ItemStack>> onItems) {
        stop();
        intercepting = true;
        callback = onItems;
        startTime = System.currentTimeMillis();
    }

    /**
     * Appelé (thread client) quand le serveur ouvre un écran pendant l'interception.
     * @return true si cet écran est le nôtre : l'appelant annule son ouverture client.
     */
    public static boolean claimScreen(int syncId) {
        if (!intercepting || finished || currentSyncId >= 0) return false; // un seul écran par cycle
        currentSyncId = syncId;
        collectedItems.clear();
        screenOpenTime = System.currentTimeMillis();
        MinepieceEssentialsClient.LOGGER.info("[Interceptor] Screen claimed syncId={}", syncId);
        return true;
    }

    public static void onSlotUpdate(int syncId, int slot, ItemStack stack) {
        if (!intercepting || syncId != currentSyncId) return;
        collectedItems.put(slot, stack.copy());
    }

    public static void onInventoryUpdate(int syncId, List<ItemStack> items) {
        if (!intercepting || syncId != currentSyncId) return;
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) collectedItems.put(i, items.get(i).copy());
        }
        if (contentReceivedAt == 0) contentReceivedAt = System.currentTimeMillis();
    }

    /** Thread client, chaque tick. */
    public static void tick() {
        if (!intercepting || finished) return;
        long now = System.currentTimeMillis();

        if (currentSyncId < 0) {
            // Pas d'écran : le serveur a répondu en chat (cooldown, lobby…) ou ne répond pas.
            if (now - startTime > OPEN_DEADLINE_MS) {
                MinepieceEssentialsClient.LOGGER.warn("[Interceptor] No screen opened within {} ms", OPEN_DEADLINE_MS);
                finished = true;
            }
            return;
        }

        long elapsed = now - screenOpenTime;
        boolean settled = contentReceivedAt > 0 && now - contentReceivedAt >= SETTLE_MS;
        if ((settled || elapsed >= COLLECT_DURATION_MS) && !collectedItems.isEmpty()) {
            Map<Integer, ItemStack> result = new HashMap<>(collectedItems);
            MinepieceEssentialsClient.LOGGER.info("[Interceptor] Delivering {} items", result.size());
            finished = true;
            if (callback != null) callback.accept(result);
        } else if (elapsed > SCREEN_TIMEOUT_MS) {
            MinepieceEssentialsClient.LOGGER.warn("[Interceptor] Timeout waiting for items");
            finished = true;
        }
    }

    public static void stop() {
        intercepting = false;
        finished = false;
        currentSyncId = -1;
        startTime = 0;
        screenOpenTime = 0;
        contentReceivedAt = 0;
        callback = null;
        collectedItems.clear();
    }
}
