package com.minepiece.essentials.util;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Empreinte bon marché d'un ensemble de piles : identité de chaque instance +
 * quantité. Le client remplace l'instance d'une pile à chaque mise à jour
 * serveur (contenu, lore, NBT), donc « même empreinte » = « rien n'a changé »
 * et les scanners peuvent sauter leur passe de parsing.
 */
public final class StackFingerprint {
    private StackFingerprint() {}

    public static long of(Container container) {
        long h = 1469598103934665603L;
        for (int i = 0, n = container.getContainerSize(); i < n; i++) {
            h = mix(h, container.getItem(i));
        }
        return h;
    }

    public static long ofSlots(List<Slot> slots) {
        long h = 1469598103934665603L;
        for (Slot slot : slots) h = mix(h, slot.getItem());
        return h;
    }

    private static long mix(long h, ItemStack stack) {
        int id = stack.isEmpty() ? 0 : System.identityHashCode(stack);
        h = (h ^ id) * 1099511628211L;
        h = (h ^ stack.getCount()) * 1099511628211L;
        return h;
    }
}
