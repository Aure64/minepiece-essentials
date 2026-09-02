package com.minepiece.essentials.pet;

import com.minepiece.essentials.ServerDetector;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

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
        Minecraft client = Minecraft.getInstance();
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        if (!ServerDetector.isOnMinePiece()) return;

        MinionFeedLine.Feed feed = findFeed(screen, client);
        if (feed == null) return;

        String itemId = findResourceItemId(screen, feed.resourceName());
        if (itemId != null) {
            ResourceXpStore.get().record(itemId, feed.xpPerItem());
        }
    }

    private static MinionFeedLine.Feed findFeed(AbstractContainerScreen<?> screen, Minecraft client) {
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !stack.getHoverName().getString().equals(FEED_ITEM_NAME)) continue;
            for (Component line : stack.getTooltipLines(Item.TooltipContext.EMPTY, client.player, TooltipFlag.NORMAL)) {
                Optional<MinionFeedLine.Feed> feed = MinionFeedLine.parse(line.getString());
                if (feed.isPresent()) return feed.get();
            }
        }
        return null;
    }

    private static String findResourceItemId(AbstractContainerScreen<?> screen, String resourceName) {
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (stack.getHoverName().getString().equals(resourceName)) {
                return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            }
        }
        return null;
    }
}
