package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.config.ModConfig;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** Rendu de l'overlay rareté sur un HandledScreen + gestion des clics de la barre. */
public final class RarityScreenOverlay {
    private RarityScreenOverlay() {}

    public static final RarityFilterState FILTER = new RarityFilterState();
    public static final RaritySortState SORT = new RaritySortState();

    private static final int BTN = 14;       // taille d'un bouton
    private static final int GAP = 2;        // espace entre boutons
    private static final int PAD = 4;        // marge barre <-> conteneur

    /** Hitbox d'un bouton : type SORT (toggle direction), CLEAR (reset), ou une rareté. */
    public record Hit(int x, int y, int w, int h, ItemRarity rarity, boolean sort, boolean clear) {}

    private static final List<Hit> HITS = new ArrayList<>();

    private static ModConfig cfg() {
        return MinepieceEssentialsClient.getInstance().getConfigManager().config();
    }

    // ---- Rendu (appelé en TAIL de HandledScreen.render) ----
    public static void render(HandledScreen<?> screen, DrawContext ctx, int bgX, int bgY) {
        HITS.clear();
        if (!ServerDetector.isOnMinePiece()) return;
        ModConfig c = cfg();

        // 1) Emblèmes + voile sur chaque slot.
        boolean icons = c.rarityIconsEnabled;
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

        // 2) Barre verticale à droite : raretés présentes + Trier + Clear.
        Set<ItemRarity> present = EnumSet.noneOf(ItemRarity.class);
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemRarity r = RarityDetector.detect(slot.getStack());
            if (r != null) present.add(r);
        }
        if (present.isEmpty()) return;

        int barX = bgX + screen.getScreenHandler().slots.stream()
                .mapToInt(s -> s.x).max().orElse(0) + 16 + PAD; // bord droit des slots + marge
        // approximation simple : à droite du fond. Ajustable en test.
        barX = bgX + 176 + PAD; // largeur de fond vanilla standard
        int y = bgY;

        if (c.rarityFilterEnabled) {
            for (ItemRarity r : ItemRarity.values()) {
                if (!present.contains(r)) continue;
                boolean act = FILTER.isActive(r);
                int border = act ? 0xFFFFFFFF : 0xFF000000;
                ctx.fill(barX - 1, y - 1, barX + BTN + 1, y + BTN + 1, border);
                ctx.fill(barX, y, barX + BTN, y + BTN, r.color);
                if (RarityDetector.detect(ItemStack.EMPTY) == null && r != null) {
                    float sc = 10f / Math.max(r.nativeW, r.nativeH);
                    RenderUtils.drawTextureScaled(ctx, r.texture(), barX + 2, y + 2, sc,
                            r.nativeW, r.nativeH);
                }
                HITS.add(new Hit(barX, y, BTN, BTN, r, false, false));
                y += BTN + GAP;
            }
            if (FILTER.any()) {
                ctx.fill(barX, y, barX + BTN, y + BTN, 0xFF802020);
                RenderUtils.drawText(ctx, "x", barX + 4, y + 3, 0xFFFFFFFF);
                HITS.add(new Hit(barX, y, BTN, BTN, null, false, true));
                y += BTN + GAP;
            }
        }

        // Bouton Trier (seulement sur conteneur de stockage).
        if (c.raritySorterEnabled && RaritySorter.canSort(screen)) {
            y += GAP;
            ctx.fill(barX, y, barX + BTN, y + BTN, 0xFF2A2A2A);
            RenderUtils.drawText(ctx, SORT.descending() ? "↓" : "↑",
                    barX + 4, y + 3, 0xFFFFFFFF); // flèche ↓ / ↑
            HITS.add(new Hit(barX, y, BTN, BTN, null, true, false));
        }
    }

    // ---- Clic (appelé en HEAD de HandledScreen.mouseClicked) ----
    /** @return true si le clic a été consommé par la barre. */
    public static boolean onClick(HandledScreen<?> screen, double mx, double my) {
        if (!ServerDetector.isOnMinePiece()) return false;
        for (Hit h : HITS) {
            if (mx >= h.x() && mx < h.x() + h.w() && my >= h.y() && my < h.y() + h.h()) {
                if (h.sort()) {
                    SORT.toggle();
                    RaritySorter.sort(screen, SORT.descending());
                } else if (h.clear()) {
                    FILTER.clear();
                } else if (h.rarity() != null) {
                    FILTER.toggle(h.rarity());
                }
                return true;
            }
        }
        return false;
    }
}
