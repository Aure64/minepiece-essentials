package com.minepiece.essentials.pet;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.HudOption;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** HUD panel listing the active pets and the total combat stats they grant. */
public class ActivePetsHud extends HudElement {

    @Override
    public boolean isEnabled() {
        return MinepieceEssentialsClient.getInstance().getConfigManager().config().petPanelEnabled;
    }

    private static final int WIDTH = 170;
    private static final int ROW = 12;   // 12 px rows: room for the 12 px head
    private static final int ICON = 12;
    private static final int GLYPH_GAP = 3;
    private static final int LEVEL_COLOR = 0xFFCBC8C7;

    @Override
    public boolean supportsMirror() { return true; }

    static final String OPT_HEAD = "head", OPT_GLYPHS = "glyphs", OPT_TOTALS = "totals";

    @Override
    public java.util.List<HudOption> options() {
        return java.util.List.of(
                HudOption.layout(this, OPT_HEAD, "minepiece.ui.option.pets.head", true),
                HudOption.layout(this, OPT_GLYPHS, "minepiece.ui.option.pets.glyphs", true),
                HudOption.layout(this, OPT_TOTALS, "minepiece.ui.option.pets.totals", false));
    }

    public ActivePetsHud() {
        super("active_pets", 5, 120, WIDTH, 60);
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, float tickDelta) {
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().petPanelEnabled) {
            return;
        }
        ActivePetsState.Snapshot snap = ActivePetsState.get();
        if (snap.isEmpty()) {
            this.height = 32;
            ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, 32, Component.translatable("minepiece.ui.pets.title").getString(), getBackground());
            RenderUtils.drawText(ctx, Component.translatable("minepiece.ui.pets.hint").getString(), 6, 20, 0xFFCBC8C7);
            return;
        }

        boolean showHead = option(OPT_HEAD, true);
        boolean showGlyphs = option(OPT_GLYPHS, true);
        boolean showTotals = option(OPT_TOTALS, false);
        java.util.List<PetStat> totalStats = new java.util.ArrayList<>();
        if (showTotals) {
            for (PetStat stat : PetStat.values()) {
                Double v = snap.totals().get(stat);
                if (v != null && v != 0.0) totalStats.add(stat);
            }
        }
        int count = snap.pets().size();
        int h = 20 + count * ROW + (totalStats.isEmpty() ? 0 : 6 + totalStats.size() * 10) + 4;
        this.height = h;

        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, Component.translatable("minepiece.ui.pets.title").getString() + " (" + count + ")", getBackground());

        boolean mirror = isMirrored();
        Font font = Minecraft.getInstance().font;
        int y = 20;
        for (ActivePetsState.ActivePet pet : snap.pets()) {
            int ty = y + 1;
            String lvl = pet.level() >= 20 ? "Max" : (pet.level() > 0 ? "Lv" + pet.level() : "");
            if (mirror) {
                // Niveau puis glyphes de gauche à droite ; tête à droite, nom aligné à droite.
                int x = 6;
                RenderUtils.drawText(ctx, lvl, x, ty, LEVEL_COLOR);
                x += RenderUtils.textWidth(lvl);
                if (showGlyphs) for (Component g : pet.statGlyphs()) {
                    x += GLYPH_GAP;
                    ctx.text(font, g, x, ty, 0xFFFFFFFF, false);
                    x += font.width(g);
                }
                int right = WIDTH - 6;
                if (showHead && !pet.icon().isEmpty()) {
                    RenderUtils.drawItemIcon(ctx, pet.icon(), right - ICON, y - 2, ICON);
                    right -= ICON + 4;
                }
                RenderUtils.drawText(ctx, truncateRight(pet.name(), right - x - 6, right), 0, ty, pet.color());
            } else {
                int lvlX = WIDTH - 6 - RenderUtils.textWidth(lvl);
                RenderUtils.drawText(ctx, lvl, lvlX, ty, LEVEL_COLOR);
                // Glyphes de stats (icônes du pack serveur, police portée par le composant)
                // alignés à droite, juste à gauche du niveau.
                int glyphsX = lvlX;
                if (showGlyphs) for (int i = pet.statGlyphs().size() - 1; i >= 0; i--) {
                    Component g = pet.statGlyphs().get(i);
                    glyphsX -= font.width(g) + GLYPH_GAP;
                    ctx.text(font, g, glyphsX, ty, 0xFFFFFFFF, false);
                }
                int x = 6;
                if (showHead && !pet.icon().isEmpty()) {
                    RenderUtils.drawItemIcon(ctx, pet.icon(), x, y - 2, ICON);
                    x += ICON + 4;
                }
                RenderUtils.drawText(ctx, truncate(pet.name(), glyphsX - 6 - x), x, ty, pet.color());
            }
            y += ROW;
        }

        // Totaux des stats données par les pets (option « Totaux des stats »), comme avant 1.10.
        if (!totalStats.isEmpty()) {
            y += 4;
            ctx.fill(6, y, WIDTH - 6, y + 1, 0x40000000);
            y += 2;
            for (PetStat stat : totalStats) {
                String name = snap.labels().get(stat);
                if (name == null || name.isBlank()) name = stat.fr();
                String v = "+" + fmt(snap.totals().get(stat));
                if (mirror) {
                    RenderUtils.drawText(ctx, v, 6, y, stat.color());
                    RenderUtils.drawText(ctx, name, WIDTH - 6 - RenderUtils.textWidth(name), y, stat.color());
                } else {
                    RenderUtils.drawText(ctx, name, 6, y, stat.color());
                    RenderUtils.drawText(ctx, v, WIDTH - 6 - RenderUtils.textWidth(v), y, stat.color());
                }
                y += 10;
            }
        }
    }

    /** Truncates to {@code budget} px and left-pads so the text ends at {@code rightX} (drawn at x=0). */
    private static String truncateRight(String name, int budget, int rightX) {
        String t = truncate(name, budget);
        int pad = rightX - RenderUtils.textWidth(t);
        StringBuilder sb = new StringBuilder();
        int spaceW = Math.max(1, RenderUtils.textWidth(" "));
        for (int i = 0; i < pad / spaceW; i++) sb.append(' ');
        return sb + t;
    }

    @Override
    public void tick() {
        ActivePetsScanner.tick();
    }

    private static String truncate(String name, int budget) {
        if (RenderUtils.textWidth(name) <= budget) return name;
        String s = name;
        while (s.length() > 1 && RenderUtils.textWidth(s + "..") > budget) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "..";
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) return Long.toString(Math.round(v));
        return String.format(java.util.Locale.US, "%.1f", v);
    }
}
