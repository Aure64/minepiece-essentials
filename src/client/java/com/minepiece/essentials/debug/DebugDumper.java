package com.minepiece.essentials.debug;

import com.minepiece.essentials.MinepieceEssentialsClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/**
 * OUTIL DE DEBUG TEMPORAIRE — à RETIRER avant release.
 *
 * <p>Vide l'écran conteneur ouvert dans {@code latest.log} (titre + pour chaque
 * slot : nom, lignes de lore, NBT {@code custom_data}). Sert à relever les chaînes
 * exactes que le serveur envoie dans une autre langue du client (anglais, etc.)
 * pour rendre les parsers bilingues. Branché sur la touche P dans
 * {@code MinepieceEssentialsClient#registerDebugDumper}.
 */
public final class DebugDumper {
    private DebugDumper() {}

    public static void dump(HandledScreen<?> screen) {
        var log = MinepieceEssentialsClient.LOGGER;
        String title = screen.getTitle() == null ? "?" : screen.getTitle().getString();
        var slots = screen.getScreenHandler().slots;
        log.info("===== [DUMP] debut — ecran='{}' ({} slots) =====", title, slots.size());
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            ItemStack st = slot.getStack();
            if (st.isEmpty()) continue;
            log.info("[DUMP] slot {} nom='{}'", i, st.getName().getString());
            LoreComponent lore = st.get(DataComponentTypes.LORE);
            if (lore != null) {
                int n = 0;
                for (Text line : lore.lines()) {
                    log.info("[DUMP]   lore[{}]='{}'", n++, line.getString());
                }
            }
            NbtComponent data = st.get(DataComponentTypes.CUSTOM_DATA);
            if (data != null) {
                log.info("[DUMP]   nbt={}", data.copyNbt().toString());
            }
        }
        log.info("===== [DUMP] fin — ecran='{}' =====", title);
    }
}
