package com.minepiece.essentials.debug;

import com.minepiece.essentials.MinepieceEssentialsClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/**
 * OUTIL DE DEBUG TEMPORAIRE — à RETIRER avant release.
 *
 * <p>Vide l'écran conteneur ouvert dans {@code latest.log} (titre + pour chaque
 * slot : nom, lignes de lore, NBT {@code custom_data}). Touche P.
 */
public final class DebugDumper {
    private DebugDumper() {}

    public static void dump(AbstractContainerScreen<?> screen) {
        var log = MinepieceEssentialsClient.LOGGER;
        String title = screen.getTitle() == null ? "?" : screen.getTitle().getString();
        var slots = screen.getMenu().slots;
        log.info("===== [DUMP] debut — ecran='{}' ({} slots) =====", title, slots.size());
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            ItemStack st = slot.getItem();
            if (st.isEmpty()) continue;
            log.info("[DUMP] slot {} item={} x{} nom='{}'", i, st.getItem(), st.getCount(), st.getHoverName().getString());
            ItemLore lore = st.get(DataComponents.LORE);
            if (lore != null) {
                int n = 0;
                for (Component line : lore.lines()) {
                    log.info("[DUMP]   lore[{}]='{}'", n++, line.getString());
                }
            }
            CustomData data = st.get(DataComponents.CUSTOM_DATA);
            if (data != null) {
                log.info("[DUMP]   nbt={}", data.copyTag().toString());
            }
        }
        log.info("===== [DUMP] fin — ecran='{}' =====", title);
    }
}
