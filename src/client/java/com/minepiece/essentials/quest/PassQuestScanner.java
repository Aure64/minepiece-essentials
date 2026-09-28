package com.minepiece.essentials.quest;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.i18n.ServerText;
import com.minepiece.essentials.island.Island;
import com.minepiece.essentials.island.IslandDetector;
import com.minepiece.essentials.network.BackgroundGuiRefresh;
import com.minepiece.essentials.util.ItemText;
import com.minepiece.essentials.util.StackFingerprint;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans the open /pass quests screen and publishes the daily quests to
 * {@link PassQuestState}. Each quest appears on several decorative slots, so we
 * de-duplicate by quest number. Screens with no quest items are ignored (the
 * last snapshot is kept).
 *
 * <p>The screen is also fetched silently through {@link BackgroundGuiRefresh}
 * (command {@link #PASS_COMMAND}, read, close — no click) once per connection
 * and after the daily midnight reset, so the HUD fills without the player
 * opening anything.
 */
public final class PassQuestScanner {

    /** Opens the daily quests tab of the pass directly. */
    public static final String PASS_COMMAND = "/pass quests";

    private static final int SCAN_INTERVAL = 4; // ticks
    private static int ticks;
    // Day the current snapshot was scanned; daily quests reset at local midnight.
    private static LocalDate scannedDay;
    // Silent fetch requested (connection, midnight reset); sent when BGRefresh is free.
    private static boolean refreshPending;
    private static boolean wasOnMinePiece;
    private static long lastFingerprint;
    private static AbstractContainerScreen<?> lastScreen;

    private PassQuestScanner() {}

    /** Listens to chat for "Vous avez complété la quête: …" to flip quests live. */
    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                PassQuestParser.completedObjective(message.getString())
                        .ifPresent(PassQuestState::markCompleted));
    }

    public static void tick() {
        if (ticks++ % SCAN_INTERVAL != 0) return;

        // Daily reset: at local midnight, drop the snapshot so the HUD prompts
        // the player to reopen /pass for the new day's quests.
        if (scannedDay != null && !LocalDate.now().equals(scannedDay)) {
            PassQuestState.set(List.of());
            scannedDay = null;
            refreshPending = true;
        }

        boolean onMinePiece = ServerDetector.isOnMinePiece();
        Minecraft client = Minecraft.getInstance();
        // "In world" = an island is detected. The address alone is not enough: the
        // temporary lobby before the real server already matches "minepiece", and a
        // background fetch there swallows the player's clicks (compass GUI).
        boolean inWorld = onMinePiece && client.player != null && client.level != null
                && IslandDetector.getInstance().getCurrentIsland() != Island.UNKNOWN;
        // Fresh connection → one silent fetch once the player is actually in the world.
        if (inWorld && !wasOnMinePiece) refreshPending = true;
        wasOnMinePiece = inWorld;

        if (refreshPending && inWorld && !BackgroundGuiRefresh.isBusy() && BackgroundGuiRefresh.isReady()) {
            boolean sent = BackgroundGuiRefresh.sendCommand(PASS_COMMAND, items -> {
                boolean ok = scan(items.values());
                MinepieceEssentialsClient.LOGGER.info("[PassQuestScanner] {} screen: {} items, quests={}",
                        PASS_COMMAND, items.size(), ok);
            });
            if (sent) {
                refreshPending = false;
                MinepieceEssentialsClient.LOGGER.info("[PassQuestScanner] Refresh via {}", PASS_COMMAND);
            }
        }

        if (client.gui.screen() instanceof AbstractContainerScreen<?> screen && onMinePiece) {
            long fp = StackFingerprint.ofSlots(screen.getMenu().slots);
            if (screen == lastScreen && fp == lastFingerprint) return; // écran inchangé
            lastScreen = screen;
            lastFingerprint = fp;
            List<ItemStack> stacks = new ArrayList<>();
            for (Slot slot : screen.getMenu().slots) stacks.add(slot.getItem());
            scan(stacks);
        } else {
            lastScreen = null;
        }
    }

    /** Asks for a silent re-fetch of the quests screen (sent when the background channel is free). */
    public static void refresh() {
        refreshPending = true;
    }

    /** @return true if the stacks were the quests screen and the snapshot was updated. */
    static boolean scan(Iterable<ItemStack> stacks) {
        Map<Integer, PassQuest> byNumber = new LinkedHashMap<>();

        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            String name = stack.getHoverName().getString();
            if (!ServerText.matches(name, ServerText.QUEST_NAME_FRAGMENT)) continue; // "Quête #N" / "Quest #N"
            PassQuestParser.parse(name, ItemText.nameAndLore(stack))
                    .ifPresent(q -> byNumber.putIfAbsent(q.number(), q));
        }

        if (byNumber.isEmpty()) return false; // not the quests screen

        List<PassQuest> quests = byNumber.values().stream()
                .sorted(Comparator.comparingInt(PassQuest::number))
                .toList();
        PassQuestState.set(quests);
        scannedDay = LocalDate.now();
        return true;
    }

}
