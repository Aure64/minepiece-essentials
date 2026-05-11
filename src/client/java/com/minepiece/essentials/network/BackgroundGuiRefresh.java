package com.minepiece.essentials.network;

import com.minepiece.essentials.MinepieceEssentialsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import java.util.Map;
import java.util.function.Consumer;

public class BackgroundGuiRefresh {
    private static long lastRefreshTime = 0;
    private static boolean busy = false;
    private static long busySince = 0;
    private static final long HARD_TIMEOUT_MS = 8000; // Force reset after 8 seconds

    public static boolean isBusy() { return busy; }

    /**
     * Send a command and collect GUI items passively from slot updates.
     * The screen is blocked from opening via mixin cancel on onOpenScreen.
     */
    public static void sendCommand(String command, Consumer<Map<Integer, ItemStack>> onItems) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || busy) return;

        long now = System.currentTimeMillis();
        long cooldown = 5000;
        if (now - lastRefreshTime < cooldown) return;

        busy = true;
        busySince = now;
        lastRefreshTime = now;

        ServerGuiInterceptor.startIntercept(items -> {
            MinepieceEssentialsClient.LOGGER.info("[BGRefresh] Got {} items for {}", items.size(), command);
            onItems.accept(items);
        });

        String cmd = command.startsWith("/") ? command.substring(1) : command;
        client.player.networkHandler.sendChatCommand(cmd);
    }

    /**
     * After receiving first screen items, click a slot to open a sub-menu.
     */
    public static void clickSlotAndListen(int slot, Consumer<Map<Integer, ItemStack>> onItems) {
        MinecraftClient client = MinecraftClient.getInstance();
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
        client.getNetworkHandler().sendPacket(
            new ClickSlotC2SPacket(
                syncId, 0, (short) slot, (byte) 0,
                SlotActionType.PICKUP,
                new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>(),
                ItemStackHash.EMPTY));
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) {
            int syncId = ServerGuiInterceptor.getExpectedSyncId();
            if (syncId >= 0) {
                // Tell server we closed the screen
                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
            }
            // Also close any screen the client might have open
            if (client.currentScreen != null) {
                client.setScreen(null);
            }
        }
    }

    public static void finish() {
        closeCurrentScreen();
        ServerGuiInterceptor.stop();
        busy = false;
    }
}
