package com.minepiece.essentials.pet;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
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

        int count = snap.pets().size();
        int h = 20 + count * ROW + 4;
        this.height = h;

        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, Component.translatable("minepiece.ui.pets.title").getString() + " (" + count + ")", getBackground());

        int y = 20;
        for (ActivePetsState.ActivePet pet : snap.pets()) {
            int ty = y + 1;
            String lvl = pet.level() >= 20 ? "Max" : (pet.level() > 0 ? "Lv" + pet.level() : "");
            int lvlX = WIDTH - 6 - RenderUtils.textWidth(lvl);
            RenderUtils.drawText(ctx, lvl, lvlX, ty, LEVEL_COLOR);

            // Glyphes de stats (icônes du pack serveur, police portée par le composant)
            // alignés à droite, juste à gauche du niveau.
            Font font = Minecraft.getInstance().font;
            int glyphsX = lvlX;
            for (int i = pet.statGlyphs().size() - 1; i >= 0; i--) {
                Component g = pet.statGlyphs().get(i);
                glyphsX -= font.width(g) + GLYPH_GAP;
                ctx.text(font, g, glyphsX, ty, 0xFFFFFFFF, false);
            }

            int x = 6;
            if (!pet.icon().isEmpty()) {
                drawIcon(ctx, pet.icon(), x, y - 2);
                x += ICON + 4;
            }
            RenderUtils.drawText(ctx, truncate(pet.name(), glyphsX - 6 - x), x, ty, pet.color());
            y += ROW;
        }

    }

    @Override
    public void tick() {
        ActivePetsScanner.tick();
    }

    /** Draws the pet item at 12 px (16 px item scaled by 0.75). */
    private static void drawIcon(GuiGraphicsExtractor ctx, net.minecraft.world.item.ItemStack stack, int x, int y) {
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale(0.75f, 0.75f);
        ctx.item(stack, 0, 0);
        ctx.pose().popMatrix();
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
