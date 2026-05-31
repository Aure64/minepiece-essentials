package com.minepiece.essentials.pet;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Appends "stacks to next prestige" and "stacks to max" lines to a pet's
 * ({@code rabbit_foot}) tooltip, using the minion data in NBT and the learned
 * resource XP ratios ({@link ResourceXpStore}). Falls back to raw XP when the
 * resource's ratio has not been seen yet.
 */
public final class MinionTooltip {

    private static final int STACK = 64;
    private static final int COLOR_INFO = 0xF0A857; // warm orange
    private static final int COLOR_MAXED = 0x55FF55; // green
    private static final String PRESTIGE_MARKER = "Prestige:";
    private static final String RESOURCE_MARKER = "Ressource de Minion:";

    private MinionTooltip() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> annotate(stack, lines));
    }

    private static void annotate(ItemStack stack, List<Text> lines) {
        if (!stack.isOf(Items.RABBIT_FOOT)) return;
        if (!ServerDetector.isOnMinePiece()) return;
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().minionCalculatorEnabled) {
            return;
        }

        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return;
        MinionData minion = MinionNbt.parse(data.copyNbt().toString()).orElse(null);
        if (minion == null) return;

        int insertAt = insertIndex(lines);

        if (minion.isMaxed()) {
            lines.add(insertAt, Text.literal("Minion : prestige max ✓").withColor(COLOR_MAXED));
            return;
        }

        double toNext = MinionCost.remainingToNextPrestige(minion.currentXp(), minion.nextXp());
        double toMax = MinionCost.remainingToMax(minion.prestige(), minion.currentXp(), minion.nextXp());

        OptionalDouble xpPer = ResourceXpStore.get().xpPerItem(MinionData.tokenToItemId(minion.foodToken()));
        String resource = resourceName(lines);

        Text line1;
        Text line2;
        if (xpPer.isPresent()) {
            double per = xpPer.getAsDouble() * STACK;
            String res = resource != null ? " de " + resource : "";
            line1 = Text.literal("Prestige suivant : " + fmt(stacks(toNext, per)) + " stacks" + res);
            line2 = Text.literal("Max (P10) : " + fmt(stacks(toMax, per)) + " stacks");
        } else {
            line1 = Text.literal("Prestige suivant : " + fmt((long) Math.ceil(toNext)) + " XP");
            line2 = Text.literal("Max (P10) : " + fmt((long) Math.ceil(toMax))
                + " XP — nourris une fois pour les stacks");
        }
        lines.add(insertAt, line1.copy().withColor(COLOR_INFO));
        lines.add(insertAt + 1, line2.copy().withColor(COLOR_INFO));
    }

    private static long stacks(double xp, double xpPerStack) {
        return (long) Math.ceil(xp / xpPerStack);
    }

    /** Insert just after the prestige bar (the line following "Prestige:"); else at end. */
    private static int insertIndex(List<Text> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getString().contains(PRESTIGE_MARKER)) {
                return Math.min(i + 2, lines.size());
            }
        }
        return lines.size();
    }

    private static String resourceName(List<Text> lines) {
        for (Text line : lines) {
            String s = line.getString();
            int idx = s.indexOf(RESOURCE_MARKER);
            if (idx >= 0) {
                String name = s.substring(idx + RESOURCE_MARKER.length()).trim();
                return name.isEmpty() ? null : name;
            }
        }
        return null;
    }

    private static String fmt(long n) {
        String s = Long.toString(n);
        StringBuilder sb = new StringBuilder();
        int c = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            sb.append(s.charAt(i));
            if (++c % 3 == 0 && i > 0) sb.append(' ');
        }
        return sb.reverse().toString();
    }
}
