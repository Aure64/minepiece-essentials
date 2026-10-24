package com.minepiece.essentials.profile;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.island.Island;
import com.minepiece.essentials.island.IslandDetector;
import com.minepiece.essentials.network.BackgroundGuiRefresh;
import com.minepiece.essentials.util.ItemText;
import com.minepiece.essentials.util.StackFingerprint;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Keeps {@link ProfileState} filled: a silent {@link #PROFILE_COMMAND} read once per
 * connection, again (debounced) when the worn armor changes, and whenever the player
 * opens the screen themselves. Read, close, never click — see
 * docs/architecture-background-refresh.md.
 */
public final class ProfileScanner {

    public static final String PROFILE_COMMAND = "/profil";
    private static final int SCAN_INTERVAL = 4; // ticks
    private static final long ARMOR_DEBOUNCE_MS = 4_000L;

    private static int ticks;
    private static boolean refreshPending;
    private static long notBeforeMillis;
    private static boolean wasInWorld;
    private static long lastFingerprint;
    private static AbstractContainerScreen<?> lastScreen;

    private ProfileScanner() {}

    /** Asks for a silent re-read (e.g. after an armor change); debounced. */
    public static void refreshSoon() {
        refreshPending = true;
        notBeforeMillis = System.currentTimeMillis() + ARMOR_DEBOUNCE_MS;
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        boolean onMinePiece = ServerDetector.isOnMinePiece();
        boolean inWorld = onMinePiece && client.player != null && client.level != null
                && IslandDetector.getInstance().getCurrentIsland() != Island.UNKNOWN;
        if (inWorld && !wasInWorld) { refreshPending = true; notBeforeMillis = 0; }
        if (!inWorld && wasInWorld) ProfileState.clear();
        wasInWorld = inWorld;

        if (refreshPending && inWorld && System.currentTimeMillis() >= notBeforeMillis
                && !BackgroundGuiRefresh.isBusy() && BackgroundGuiRefresh.isReady()) {
            boolean sent = BackgroundGuiRefresh.sendCommand(PROFILE_COMMAND, items -> {
                boolean ok = scan(new ArrayList<>(items.values()));
                MinepieceEssentialsClient.LOGGER.info("[Profile] {} screen: {} items, parsed={}",
                        PROFILE_COMMAND, items.size(), ok);
            });
            if (sent) {
                refreshPending = false;
                MinepieceEssentialsClient.LOGGER.info("[Profile] Refresh via {}", PROFILE_COMMAND);
            }
        }

        if (ticks++ % SCAN_INTERVAL != 0) return;
        if (client.gui.screen() instanceof AbstractContainerScreen<?> screen && onMinePiece) {
            long fp = StackFingerprint.ofSlots(screen.getMenu().slots);
            if (screen == lastScreen && fp == lastFingerprint) return;
            lastScreen = screen;
            lastFingerprint = fp;
            List<ItemStack> stacks = new ArrayList<>();
            for (Slot slot : screen.getMenu().slots) stacks.add(slot.getItem());
            scan(stacks);
        } else {
            lastScreen = null;
        }
    }

    /** @return true if an "Information" item with the profile stats was found. */
    private static boolean scan(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            Optional<ProfileData> data = ProfileParser.parse(ItemText.nameAndLore(stack));
            if (data.isPresent()) {
                ProfileState.set(data.get());
                return true;
            }
        }
        return false;
    }
}
