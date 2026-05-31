package com.minepiece.essentials.pet;

import com.minepiece.essentials.ServerDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.Optional;

/**
 * Watches the minion feeding screen and learns each resource's XP-per-item ratio
 * from the "Nourrir votre Minion" item's {@code "<Resource> xN - Y Exp"} line,
 * caching it (by item id) into {@link ResourceXpStore}.
 */
public final class MinionFeedLearner {

    private static final String FEED_ITEM_NAME = "Nourrir votre Minion";

    private MinionFeedLearner() {}

    /** Call once per client tick; cheap no-op unless a feeding screen is open. */
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
        if (!ServerDetector.isOnMinePiece()) return;

        MinionFeedLine.Feed feed = findFeed(screen, client);
        if (feed == null) return;

        String itemId = findResourceItemId(screen, feed.resourceName());
        if (itemId != null) {
            ResourceXpStore.get().record(itemId, feed.xpPerItem());
        }
    }

    private static MinionFeedLine.Feed findFeed(HandledScreen<?> screen, MinecraftClient client) {
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || !stack.getName().getString().equals(FEED_ITEM_NAME)) continue;
            for (Text line : stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC)) {
                Optional<MinionFeedLine.Feed> feed = MinionFeedLine.parse(line.getString());
                if (feed.isPresent()) return feed.get();
            }
        }
        return null;
    }

    private static String findResourceItemId(HandledScreen<?> screen, String resourceName) {
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            if (stack.getName().getString().equals(resourceName)) {
                return Registries.ITEM.getId(stack.getItem()).toString();
            }
        }
        return null;
    }
}
