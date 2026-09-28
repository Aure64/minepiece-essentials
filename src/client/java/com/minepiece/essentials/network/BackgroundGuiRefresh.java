package com.minepiece.essentials.network;

import com.minepiece.essentials.MinepieceEssentialsClient;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public class BackgroundGuiRefresh {
    private static long lastRefreshTime = 0;
    private static boolean busy = false;
    private static long busySince = 0;
    private static final long HARD_TIMEOUT_MS = 8000; // Force reset after 8 seconds

    public static boolean isBusy() { return busy; }

    /** True when the global cooldown has elapsed since the last refresh start. */
    public static boolean isReady() {
        return System.currentTimeMillis() - lastRefreshTime >= 5000;
    }

    /** Milliseconds remaining before another refresh can fire. 0 if ready. */
    public static long getCooldownRemainingMs() {
        long elapsed = System.currentTimeMillis() - lastRefreshTime;
        return Math.max(0, 5000 - elapsed);
    }

    /**
     * Send a command and collect GUI items passively from slot updates.
     * The screen is blocked from opening via mixin cancel on onOpenScreen.
     */
    public static boolean sendCommand(String command, Consumer<Map<Integer, ItemStack>> onItems) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || busy) return false;

        long now = System.currentTimeMillis();
        long cooldown = 5000;
        if (now - lastRefreshTime < cooldown) return false;

        busy = true;
        busySince = now;
        lastRefreshTime = now;

        ServerGuiInterceptor.startIntercept(items -> {
            MinepieceEssentialsClient.LOGGER.info("[BGRefresh] Got {} items for {}", items.size(), command);
            onItems.accept(items);
        });

        String cmd = command.startsWith("/") ? command.substring(1) : command;
        client.player.connection.sendCommand(cmd);
        return true;
    }

    /**
     * After receiving first screen items, click a slot to open a sub-menu.
     */
    public static void clickSlotAndListen(int slot, Consumer<Map<Integer, ItemStack>> onItems) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !busy) return;

        int syncId = ServerGuiInterceptor.getExpectedSyncId();
        if (syncId < 0) {
            MinepieceEssentialsClient.LOGGER.warn("[BGRefresh] No syncId for clickSlot");
            finish();
            return;
        }

        MinepieceEssentialsClient.LOGGER.info("[BGRefresh] Clicking slot {} on syncId {}", slot, syncId);

        // Prepare to collect the second screen's items
        ServerGuiInterceptor.prepareForSecondScreen(onItems);

        // Send click packet
        client.getConnection().send(
            new ServerboundContainerClickPacket(
                syncId, 0, (short) slot, (byte) 0,
                ContainerInput.PICKUP,
                new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>(),
                HashedStack.EMPTY));
    }

    /**
     * Called every client tick to drive the interceptor.
     */
    public static void tick() {
        if (!busy) return;
        ServerGuiInterceptor.tick();

        // If interceptor finished, mark us as not busy
        if (!ServerGuiInterceptor.isIntercepting() && busy) {
            closeCurrentScreen();
            busy = false;
        }

        // Hard timeout — force reset if stuck
        if (busy && System.currentTimeMillis() - busySince > HARD_TIMEOUT_MS) {
            MinepieceEssentialsClient.LOGGER.warn("[BGRefresh] Hard timeout — forcing reset");
            finish();
        }
    }

    private static void closeCurrentScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() != null) {
            int syncId = ServerGuiInterceptor.getExpectedSyncId();
            if (syncId >= 0) {
                // Tell server we closed the screen
                client.getConnection().send(new ServerboundContainerClosePacket(syncId));
            }
            // Also close any screen the client might have open
            if (client.gui.screen() != null) {
                client.setScreenAndShow(null);
            }
        }
    }

    public static void finish() {
        closeCurrentScreen();
        ServerGuiInterceptor.stop();
        busy = false;
    }
}
