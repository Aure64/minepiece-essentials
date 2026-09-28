package com.minepiece.essentials.pet;

import java.util.Optional;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Données lues dans le NBT d'un pet, calculées une fois par instance de pile.
 * Le NBT d'un pet est volumineux (pistes, minion, nourriture…) : le copier et
 * le sérialiser à chaque frame de survol, deux fois (rareté + minion), était
 * le poste le plus coûteux du mod.
 */
public final class PetNbtCache {
    private PetNbtCache() {}

    public record Parsed(Rarity rarity, Optional<MinionData> minion) {
        static final Parsed EMPTY = new Parsed(null, Optional.empty());
    }

    private static final Pattern RARITY_TRACK =
        Pattern.compile("tracks\\.==(COMMON|RARE|EPIC|LEGENDARY|MYTHIC)");
    private static final WeakHashMap<ItemStack, Parsed> CACHE = new WeakHashMap<>();

    public static Parsed of(ItemStack stack) {
        Parsed cached = CACHE.get(stack);
        if (cached != null) return cached;
        Parsed p = compute(stack);
        CACHE.put(stack, p);
        return p;
    }

    private static Parsed compute(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return Parsed.EMPTY;
        String snbt = data.copyTag().toString();
        Matcher m = RARITY_TRACK.matcher(snbt);
        Rarity rarity = m.find() ? Rarity.fromTrack(m.group(1)) : null;
        return new Parsed(rarity, MinionNbt.parse(snbt));
    }
}
