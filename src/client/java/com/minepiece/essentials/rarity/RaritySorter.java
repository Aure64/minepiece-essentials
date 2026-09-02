package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;

/** Tri du conteneur ouvert par rareté, via des clics d'inventaire simulés. */
public final class RaritySorter {
    private RaritySorter() {}

    /** Nombre de slots du conteneur (haut), ou -1 si non triable. */
    public static int containerSize(AbstractContainerMenu h) {
        if (h instanceof ChestMenu g) return g.getRowCount() * 9;
        if (h instanceof ShulkerBoxMenu) return 27;
        return -1;
    }

    public static boolean canSort(AbstractContainerScreen<?> screen) {
        return containerSize(screen.getMenu()) > 0;
    }

    public static void sort(AbstractContainerScreen<?> screen, RaritySort.Mode mode, boolean ascending) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        AbstractContainerMenu h = screen.getMenu();
        int n = containerSize(h);
        if (n <= 0 || n > h.slots.size()) return;

        // Snapshot des piles du conteneur (références stables).
        ItemStack[] cur = new ItemStack[n];
        List<RaritySort.Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ItemStack s = h.getSlot(i).getItem();
            cur[i] = s;
            if (s.isEmpty()) {
                entries.add(new RaritySort.Entry(true, -1, "", ""));
            } else {
                ItemRarity r = RarityDetector.detect(s);
                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                String name = s.getHoverName().getString();
                entries.add(new RaritySort.Entry(false, r == null ? -1 : r.rank, id, name));
            }
        }

        // Ordre cible : la pile qui doit finir au slot i.
        List<Integer> order = RaritySort.targetOrder(entries, mode, ascending);
        ItemStack[] desired = new ItemStack[n];
        for (int i = 0; i < n; i++) desired[i] = cur[order.get(i)];

        // Tri-sélection par swaps de 3 clics PICKUP, en miroir du modèle `cur`.
        int syncId = h.containerId;
        for (int i = 0; i < n; i++) {
            if (cur[i] == desired[i]) continue;
            int j = -1;
            for (int k = i + 1; k < n; k++) {
                if (cur[k] == desired[i]) { j = k; break; }
            }
            if (j < 0) continue; // robustesse : introuvable (resync), on saute
            // swap(i, j) : pickup i, click j, place i
            mc.gameMode.handleInventoryMouseClick(syncId, i, 0, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(syncId, j, 0, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(syncId, i, 0, ClickType.PICKUP, mc.player);
            ItemStack tmp = cur[i]; cur[i] = cur[j]; cur[j] = tmp;
        }
    }
}
