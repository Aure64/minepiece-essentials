package com.minepiece.essentials.quest;

import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.i18n.ServerText;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

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
 */
public final class PassQuestScanner {

    private static final int SCAN_INTERVAL = 4; // ticks
    private static int ticks;
    // Day the current snapshot was scanned; daily quests reset at local midnight.
    private static LocalDate scannedDay;

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
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HandledScreen<?> screen && ServerDetector.isOnMinePiece()) {
            scan(screen, client);
        }
    }

    private static void scan(HandledScreen<?> screen, MinecraftClient client) {
        Map<Integer, PassQuest> byNumber = new LinkedHashMap<>();

        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            String name = stack.getName().getString();
            if (!ServerText.matches(name, ServerText.QUEST_NAME_FRAGMENT)) continue; // "Quête #N" / "Quest #N"
            PassQuestParser.parse(name, tooltipLines(stack, client))
                    .ifPresent(q -> byNumber.putIfAbsent(q.number(), q));
        }

        if (byNumber.isEmpty()) return; // not the quests screen

        List<PassQuest> quests = byNumber.values().stream()
                .sorted(Comparator.comparingInt(PassQuest::number))
                .toList();
        PassQuestState.set(quests);
        scannedDay = LocalDate.now();
    }

    private static List<String> tooltipLines(ItemStack stack, MinecraftClient client) {
        List<String> out = new ArrayList<>();
        for (Text line : stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC)) {
            out.add(line.getString());
        }
        return out;
    }
}
