package com.minepiece.essentials.quest;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.i18n.ServerText;
import com.minepiece.essentials.network.BackgroundGuiRefresh;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
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
    public static final String PASS_COMMAND = "/pass quest";

    private static final int SCAN_INTERVAL = 4; // ticks
    private static int ticks;
    // Day the current snapshot was scanned; daily quests reset at local midnight.
    private static LocalDate scannedDay;
    // Silent fetch requested (connection, midnight reset); sent when BGRefresh is free.
    private static boolean refreshPending;
    private static boolean wasOnMinePiece;

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
        boolean inWorld = onMinePiece && client.player != null && client.level != null;
        // Fresh connection → one silent fetch once the player is actually in the world.
        if (inWorld && !wasOnMinePiece) refreshPending = true;
        wasOnMinePiece = inWorld;

        if (refreshPending && inWorld && !BackgroundGuiRefresh.isBusy() && BackgroundGuiRefresh.isReady()) {
            MinepieceEssentialsClient.LOGGER.info("[PassQuestScanner] Refresh via {}", PASS_COMMAND);
            boolean sent = BackgroundGuiRefresh.sendCommand(PASS_COMMAND, items -> {
                boolean ok = scan(items.values(), null);
                MinepieceEssentialsClient.LOGGER.info("[PassQuestScanner] {} screen: {} items, quests={}",
                        PASS_COMMAND, items.size(), ok);
            });
            if (sent) refreshPending = false;
        }

        if (client.gui.screen() instanceof AbstractContainerScreen<?> screen && onMinePiece) {
            List<ItemStack> stacks = new ArrayList<>();
            for (Slot slot : screen.getMenu().slots) stacks.add(slot.getItem());
            scan(stacks, client);
        }
    }

    /** Asks for a silent re-fetch of the quests screen (sent when the background channel is free). */
    public static void refresh() {
        refreshPending = true;
    }

    /** @return true if the stacks were the quests screen and the snapshot was updated. */
    static boolean scan(Iterable<ItemStack> stacks, Minecraft client) {
        Map<Integer, PassQuest> byNumber = new LinkedHashMap<>();

        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            String name = stack.getHoverName().getString();
            if (!ServerText.matches(name, ServerText.QUEST_NAME_FRAGMENT)) continue; // "Quête #N" / "Quest #N"
            PassQuestParser.parse(name, lines(stack, client))
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

    /** Name + lore lines, the same shape as the tooltip the parser was written against. */
    private static List<String> lines(ItemStack stack, Minecraft client) {
        if (client != null) return tooltipLines(stack, client);
        List<String> out = new ArrayList<>();
        out.add(stack.getHoverName().getString());
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) out.add(line.getString());
        }
        return out;
    }

    private static List<String> tooltipLines(ItemStack stack, Minecraft client) {
        List<String> out = new ArrayList<>();
        for (Component line : stack.getTooltipLines(Item.TooltipContext.EMPTY, client.player, TooltipFlag.NORMAL)) {
            out.add(line.getString());
        }
        return out;
    }
}
