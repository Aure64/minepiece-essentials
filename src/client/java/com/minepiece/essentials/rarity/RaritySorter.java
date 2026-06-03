package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/** Tri du conteneur ouvert par rareté, via des clics d'inventaire simulés. */
public final class RaritySorter {
    private RaritySorter() {}

    /** Nombre de slots du conteneur (haut), ou -1 si non triable. */
    public static int containerSize(ScreenHandler h) {
        if (h instanceof GenericContainerScreenHandler g) return g.getRows() * 9;
        if (h instanceof ShulkerBoxScreenHandler) return 27;
        return -1;
    }

    public static boolean canSort(HandledScreen<?> screen) {
        return containerSize(screen.getScreenHandler()) > 0;
    }

    public static void sort(HandledScreen<?> screen, RaritySort.Mode mode, boolean ascending) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        ScreenHandler h = screen.getScreenHandler();
        int n = containerSize(h);
        if (n <= 0 || n > h.slots.size()) return;

        // Snapshot des piles du conteneur (références stables).
        ItemStack[] cur = new ItemStack[n];
        List<RaritySort.Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ItemStack s = h.getSlot(i).getStack();
            cur[i] = s;
            if (s.isEmpty()) {
                entries.add(new RaritySort.Entry(true, -1, "", ""));
            } else {
                ItemRarity r = RarityDetector.detect(s);
                String id = Registries.ITEM.getId(s.getItem()).toString();
                String name = s.getName().getString();
                entries.add(new RaritySort.Entry(false, r == null ? -1 : r.rank, id, name));
            }
        }

        // Ordre cible : la pile qui doit finir au slot i.
        List<Integer> order = RaritySort.targetOrder(entries, mode, ascending);
        ItemStack[] desired = new ItemStack[n];
        for (int i = 0; i < n; i++) desired[i] = cur[order.get(i)];

        // Tri-sélection par swaps de 3 clics PICKUP, en miroir du modèle `cur`.
        int syncId = h.syncId;
        for (int i = 0; i < n; i++) {
            if (cur[i] == desired[i]) continue;
            int j = -1;
            for (int k = i + 1; k < n; k++) {
                if (cur[k] == desired[i]) { j = k; break; }
            }
            if (j < 0) continue; // robustesse : introuvable (resync), on saute
            // swap(i, j) : pickup i, click j, place i
            mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, j, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.PICKUP, mc.player);
            ItemStack tmp = cur[i]; cur[i] = cur[j]; cur[j] = tmp;
        }
    }
}
