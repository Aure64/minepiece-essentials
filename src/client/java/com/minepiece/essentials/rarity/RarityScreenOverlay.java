package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.config.ModConfig;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/** Rendu de l'overlay rareté sur un HandledScreen + gestion des clics de la barre. */
public final class RarityScreenOverlay {
    private RarityScreenOverlay() {}

    public static final RarityFilterState FILTER = new RarityFilterState();
    public static final RaritySortState SORT = new RaritySortState();

    private static final int BTN = 14;       // taille d'un bouton
    private static final int GAP = 2;        // espace entre boutons
    private static final int PAD = 4;        // marge barre <-> conteneur

    enum HitKind { FILTER, CLEAR, SORT_RARITY, SORT_ITEM }

    /** Hitbox d'un bouton, avec son infobulle. */
    record Hit(int x, int y, int w, int h, ItemRarity rarity, HitKind kind, String tip) {}

    private static final List<Hit> HITS = new ArrayList<>();

    private static ModConfig cfg() {
        return MinepieceEssentialsClient.getInstance().getConfigManager().config();
    }

    // ---- Rendu (appelé en TAIL de HandledScreen.render) ----
    public static void render(HandledScreen<?> screen, DrawContext ctx,
                              int bgX, int bgY, int mouseX, int mouseY) {
        HITS.clear();
        if (!ServerDetector.isOnMinePiece()) return;
        ModConfig c = cfg();

        // 1) Emblèmes + voile sur chaque slot.
        // L'inventaire joueur (E) et les coffres ont chacun leur toggle.
        boolean isPlayerInv = screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen;
        boolean icons = isPlayerInv ? c.rarityInventoryEnabled : c.rarityIconsEnabled;
        boolean filterOn = c.rarityFilterEnabled && FILTER.any();
        if (icons || filterOn) {
            for (Slot slot : screen.getScreenHandler().slots) {
                ItemStack st = slot.getStack();
                if (st.isEmpty()) continue;
                ItemRarity r = RarityDetector.detect(st);
                int sx = bgX + slot.x, sy = bgY + slot.y;
                if (filterOn && FILTER.isDimmed(r)) {
                    ctx.fill(sx, sy, sx + 16, sy + 16, 0xB0101010); // voile sombre
                }
                if (icons && r != null) {
                    float scale = 8f / Math.max(r.nativeW, r.nativeH); // ~8px de haut
                    RenderUtils.drawTextureScaled(ctx, r.texture(), sx + 1, sy, scale,
                            r.nativeW, r.nativeH);
                }
            }
        }

        // 2) Barre verticale à droite. Collée au bord du fond (176px vanilla).
        int barX = bgX + 176 + PAD;
        int y = bgY;

        // Boutons de filtre : une rareté présente = un bouton.
        if (c.rarityFilterEnabled) {
            Set<ItemRarity> present = EnumSet.noneOf(ItemRarity.class);
            for (Slot slot : screen.getScreenHandler().slots) {
                ItemRarity r = RarityDetector.detect(slot.getStack());
                if (r != null) present.add(r);
            }
            for (ItemRarity r : ItemRarity.values()) {
                if (!present.contains(r)) continue;
                boolean act = FILTER.isActive(r);
                int border = act ? 0xFFFFFFFF : 0xFF000000;
                ctx.fill(barX - 1, y - 1, barX + BTN + 1, y + BTN + 1, border);
                ctx.fill(barX, y, barX + BTN, y + BTN, r.color);
                float sc = 10f / Math.max(r.nativeW, r.nativeH);
                RenderUtils.drawTextureScaled(ctx, r.texture(), barX + 2, y + 2, sc,
                        r.nativeW, r.nativeH);
                HITS.add(new Hit(barX, y, BTN, BTN, r, HitKind.FILTER, "Filtrer : " + r.label));
                y += BTN + GAP;
            }
            if (FILTER.any()) {
                ctx.fill(barX, y, barX + BTN, y + BTN, 0xFF802020);
                RenderUtils.drawText(ctx, "x", barX + 4, y + 3, 0xFFFFFFFF);
                HITS.add(new Hit(barX, y, BTN, BTN, null, HitKind.CLEAR, "Réinitialiser le filtre"));
                y += BTN + GAP;
            }
        }

        // Boutons de tri (seulement sur un conteneur de stockage).
        if (c.raritySorterEnabled && RaritySorter.canSort(screen)) {
            if (y > bgY) y += GAP; // petit séparateur sous les filtres

            // Tri par rareté
            ctx.fill(barX, y, barX + BTN, y + BTN, 0xFF3A2E10);
            RenderUtils.drawText(ctx, SORT.rarityDescending() ? "↓" : "↑", barX + 4, y + 3, 0xFFFFD27F);
            String rTip = SORT.rarityDescending()
                    ? "Trier par rareté : mythique → commun"
                    : "Trier par rareté : commun → mythique";
            HITS.add(new Hit(barX, y, BTN, BTN, null, HitKind.SORT_RARITY, rTip));
            y += BTN + GAP;

            // Regrouper par objet : noms toujours A→Z, bascule le sens des raretés du groupe.
            ctx.fill(barX, y, barX + BTN, y + BTN, 0xFF2A1038);
            RenderUtils.drawText(ctx, "A" + (SORT.itemRarityDescending() ? "↓" : "↑"),
                    barX + 1, y + 3, 0xFFD9B8FF);
            String aTip = "Regrouper par objet (A→Z) — raretés : "
                    + (SORT.itemRarityDescending() ? "plus rare → moins rare"
                                                   : "moins rare → plus rare");
            HITS.add(new Hit(barX, y, BTN, BTN, null, HitKind.SORT_ITEM, aTip));
            y += BTN + GAP;
        }

        // 3) Infobulle du bouton survolé. Dessinée AVANT drawDeferredElements (via le mixin
        // ScreenRenderMixin) : la bulle native est différée puis vidée par-dessus nos emblèmes,
        // exactement comme l'infobulle d'objet du serveur.
        for (Hit h : HITS) {
            if (mouseX >= h.x() && mouseX < h.x() + h.w()
                    && mouseY >= h.y() && mouseY < h.y() + h.h()) {
                ctx.drawTooltip(MinecraftClient.getInstance().textRenderer,
                        Text.literal(h.tip()), mouseX, mouseY);
                break;
            }
        }
    }

    // ---- Clic (appelé en HEAD de HandledScreen.mouseClicked) ----
    /** @return true si le clic a été consommé par la barre. */
    public static boolean onClick(HandledScreen<?> screen, double mx, double my) {
        if (!ServerDetector.isOnMinePiece()) return false;
        for (Hit h : HITS) {
            if (mx >= h.x() && mx < h.x() + h.w() && my >= h.y() && my < h.y() + h.h()) {
                switch (h.kind()) {
                    case FILTER -> FILTER.toggle(h.rarity());
                    case CLEAR -> FILTER.clear();
                    case SORT_RARITY -> {
                        // trie dans le sens affiché, puis bascule l'indicateur.
                        RaritySorter.sort(screen, RaritySort.Mode.RARITY, !SORT.rarityDescending());
                        SORT.toggleRarity();
                    }
                    case SORT_ITEM -> {
                        // noms A→Z (fixe) ; ascending = sens des raretés dans le groupe.
                        RaritySorter.sort(screen, RaritySort.Mode.ITEM, !SORT.itemRarityDescending());
                        SORT.toggleItem();
                    }
                }
                return true;
            }
        }
        return false;
    }
}
