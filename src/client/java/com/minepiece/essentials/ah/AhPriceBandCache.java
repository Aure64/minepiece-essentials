package com.minepiece.essentials.ah;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * ItemStack → AhPriceBand.Result via le lore, avec cache par pile (même approche que
 * {@code rarity.RarityDetector}). Évite de reparser tout le lore et de le parcourir deux
 * fois (prix de vente puis prix moyen) à chaque frame pour chaque slot d'un conteneur.
 *
 * <p>Thread-confiné au thread client : {@code rarity.RarityScreenOverlay} l'appelle
 * uniquement depuis le rendu d'écran (render), donc pas de synchronisation. Le WeakHashMap
 * laisse l'entrée être collectée avec la pile (nouvelle pile après un refresh serveur, etc.).
 */
public final class AhPriceBandCache {
    private AhPriceBandCache() {}

    // Optional.empty() sert de sentinelle « pas de bande » ; distinct de « pas encore
    // calculé » (absence de clé dans la map, cf. get() ci-dessous).
    private static final WeakHashMap<ItemStack, Optional<AhPriceBand.Result>> CACHE = new WeakHashMap<>();

    public static Optional<AhPriceBand.Result> get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        Optional<AhPriceBand.Result> cached = CACHE.get(stack);
        if (cached != null) return cached;

        Optional<AhPriceBand.Result> computed = AhPriceBand.fromLore(loreStrings(stack));
        CACHE.put(stack, computed);
        return computed;
    }

    /** Lignes de lore d'un item en texte brut (sans assembler toute l'infobulle). */
    private static List<String> loreStrings(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return List.of();
        List<String> out = new ArrayList<>(lore.lines().size());
        for (Text t : lore.lines()) out.add(t.getString());
        return out;
    }
}
