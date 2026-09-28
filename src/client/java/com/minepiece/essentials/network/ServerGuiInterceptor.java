package com.minepiece.essentials.network;

import com.minepiece.essentials.MinepieceEssentialsClient;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;

/**
 * Passive interceptor: does NOT block screens from opening.
 * Instead, silently collects slot update data and delivers it via callbacks.
 * Screens open normally but are closed automatically after data is collected.
 */
public class ServerGuiInterceptor {
    private static boolean intercepting = false;
    // Data delivered (or timed out): BackgroundGuiRefresh must now close the screen and stop().
    private static boolean finished = false;
    private static int currentSyncId = -1;
    private static final Map<Integer, ItemStack> collectedItems = new HashMap<>();
    private static Consumer<Map<Integer, ItemStack>> callback;
    private static long interceptStartTime = 0;
    private static final long COLLECT_DURATION_MS = 600;

    // For two-step flows (command → first screen → click → second screen)
    private static boolean waitingForSecondScreen = false;
    private static Consumer<Map<Integer, ItemStack>> secondCallback;

    public static boolean isIntercepting() { return intercepting; }
    public static boolean isFinished() { return finished; }

    public static void startIntercept(Consumer<Map<Integer, ItemStack>> onItems) {
        intercepting = true;
        finished = false;
        callback = onItems;
        collectedItems.clear();
        currentSyncId = -1;
        interceptStartTime = 0;
        waitingForSecondScreen = false;
        secondCallback = null;
    }

    public static void onScreenOpen(int syncId) {
        if (!intercepting) return;
        currentSyncId = syncId;
        collectedItems.clear();
        interceptStartTime = System.currentTimeMillis();
        MinepieceEssentialsClient.LOGGER.info("[Interceptor] Screen opened syncId={} waiting2nd={}", syncId, waitingForSecondScreen);
    }

    public static void onSlotUpdate(int syncId, int slot, ItemStack stack) {
        if (!intercepting || syncId != currentSyncId) return;
        collectedItems.put(slot, stack.copy());
    }

    public static void onInventoryUpdate(int syncId, List<ItemStack> items) {
        if (!intercepting || syncId != currentSyncId) return;
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                collectedItems.put(i, items.get(i).copy());
            }
        }
    }

    /**
     * Called every client tick. After enough time has passed collecting slot updates,
     * deliver the items to the callback.
     */
    public static void tick() {
        if (!intercepting || interceptStartTime == 0) return;

        long elapsed = System.currentTimeMillis() - interceptStartTime;

        if (elapsed >= COLLECT_DURATION_MS && !collectedItems.isEmpty()) {
            Map<Integer, ItemStack> result = new HashMap<>(collectedItems);

            if (waitingForSecondScreen) {
                // Deliver second screen data
                if (secondCallback != null) {
                    MinepieceEssentialsClient.LOGGER.info("[Interceptor] Delivering 2nd screen: {} items", result.size());
                    secondCallback.accept(result);
                }
                finished = true;
            } else {
                // Deliver first screen data
                if (callback != null) {
                    MinepieceEssentialsClient.LOGGER.info("[Interceptor] Delivering 1st screen: {} items", result.size());
                    callback.accept(result);
                }
                // The callback may have set up a second screen via prepareForSecondScreen();
                // otherwise this single-screen read is complete.
                if (!waitingForSecondScreen) {
                    finished = true;
                } else {
                    collectedItems.clear();
                }
            }
        }

        // Timeout
        if (!finished && elapsed > 5000) {
            MinepieceEssentialsClient.LOGGER.warn("[Interceptor] Timeout");
            finished = true;
        }
    }

    public static void prepareForSecondScreen(Consumer<Map<Integer, ItemStack>> onItems) {
        waitingForSecondScreen = true;
        secondCallback = onItems;
        collectedItems.clear();
        interceptStartTime = System.currentTimeMillis();
    }

    public static void stop() {
        intercepting = false;
        finished = false;
        currentSyncId = -1;
        callback = null;
        secondCallback = null;
        collectedItems.clear();
        interceptStartTime = 0;
        waitingForSecondScreen = false;
    }

    public static int getExpectedSyncId() { return currentSyncId; }
}
