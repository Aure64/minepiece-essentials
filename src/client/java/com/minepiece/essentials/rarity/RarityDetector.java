package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/** ItemStack → ItemRarity via le nom + le lore (glyphe du pack), avec cache par pile. */
public final class RarityDetector {
    private RarityDetector() {}

    // Sentinelle pour cacher "aucune rareté" sans confondre avec absence d'entrée.
    private static final ItemRarity NONE = null;
    private static final Object MISS = new Object();
    private static final WeakHashMap<ItemStack, Object> CACHE = new WeakHashMap<>();

    @Nullable
    public static ItemRarity detect(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return NONE;
        Object cached = CACHE.get(stack);
        if (cached != null) return cached == MISS ? NONE : (ItemRarity) cached;

        ItemRarity r = compute(stack);
        CACHE.put(stack, r == null ? MISS : r);
        return r;
    }

    @Nullable
    private static ItemRarity compute(ItemStack stack) {
        // 1) nom affiché (inclut le custom name serveur)
        ItemRarity r = RarityGlyphs.scan(stack.getName().getString());
        if (r != null) return r;

        // 2) lignes de lore
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            List<String> lines = new ArrayList<>();
            for (Text line : lore.lines()) lines.add(line.getString());
            r = RarityGlyphs.scanLines(lines);
        }
        return r;
    }
}
