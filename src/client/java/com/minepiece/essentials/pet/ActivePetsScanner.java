package com.minepiece.essentials.pet;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans the open /pets screen and publishes the combat-stat total of the
 * <em>currently active</em> pets (those whose tooltip offers "Désactiver") to
 * {@link ActivePetsState}.
 *
 * <p>The active set is <b>replaced</b> on every scan that finds active pets, so
 * deactivating or swapping pets is reflected immediately and stale pets are
 * never kept. Pages with no active pets leave the last total untouched.
 */
public final class ActivePetsScanner {

    private static final String ACTIVE_ACTION = "Désactiver";
    private static final String INACTIVE_ACTION = "Activer";
    private static final String SECTION_START = "Familier Effects";
    private static final String SECTION_END = "Minion Effects";
    private static final int SCAN_INTERVAL = 4; // ticks

    private static int ticks;
    private static List<String> lastLoggedNames = List.of();

    private ActivePetsScanner() {}

    public static void tick() {
        if (ticks++ % SCAN_INTERVAL != 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HandledScreen<?> screen && ServerDetector.isOnMinePiece()) {
            scan(screen, client);
        }
    }

    private static void scan(HandledScreen<?> screen, MinecraftClient client) {
        boolean isPetsScreen = false;
        Map<String, List<PetEffect>> active = new LinkedHashMap<>();

        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (!stack.isOf(Items.RABBIT_FOOT)) continue;

            List<String> tip = tooltipLines(stack, client);
            if (containsLine(tip, ACTIVE_ACTION) || containsLine(tip, INACTIVE_ACTION)) {
                isPetsScreen = true;
            }
            if (containsLine(tip, ACTIVE_ACTION)) {
                active.put(stack.getName().getString(), combatStats(tip));
            }
        }

        // Not the /pets screen, or a page with no active pets → keep the last total.
        if (!isPetsScreen || active.isEmpty()) return;

        List<PetEffect> all = new ArrayList<>();
        active.values().forEach(all::addAll);
        List<String> names = new ArrayList<>(active.keySet());
        ActivePetsState.set(new ActivePetsState.Snapshot(names, PetStatSum.sum(all)));

        if (!names.equals(lastLoggedNames)) {
            lastLoggedNames = names;
            MinepieceEssentialsClient.LOGGER.info("[ActivePets] {} actif(s): {}", names.size(), names);
        }
    }

    private static List<PetEffect> combatStats(List<String> lines) {
        int start = -1;
        int end = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(SECTION_START)) {
                start = i;
            } else if (start >= 0 && lines.get(i).contains(SECTION_END)) {
                end = i;
                break;
            }
        }
        List<PetEffect> out = new ArrayList<>();
        if (start < 0) return out;
        for (int i = start + 1; i < end; i++) {
            PetEffectParser.parse(lines.get(i)).ifPresent(out::add);
        }
        return out;
    }

    private static List<String> tooltipLines(ItemStack stack, MinecraftClient client) {
        List<String> out = new ArrayList<>();
        for (Text line : stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC)) {
            out.add(line.getString());
        }
        return out;
    }

    private static boolean containsLine(List<String> lines, String needle) {
        for (String l : lines) {
            if (l.contains(needle)) return true;
        }
        return false;
    }
}
