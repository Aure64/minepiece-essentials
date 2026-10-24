package com.minepiece.essentials.armor;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.pet.PetStat;
import com.minepiece.essentials.profile.StatLine;
import com.minepiece.essentials.util.RenderUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** HUD panel listing the worn armor pieces (icon + name) and the total stats they grant. */
public class ArmorHud extends HudElement {

    private static final int WIDTH = 170;
    private static final int ROW = 12;          // icon rows are 16 px scaled to 12
    private static final int NAME_COLOR = 0xFFFFE9D5;
    private static final int SET_COLOR = 0xFF9CE07A;
    private static final int SET_PARTIAL_COLOR = 0xFFCBC8C7;
    private static final int EFFECT_COLOR = 0xFFB8D8C0;

    public ArmorHud() {
        super("active_armor", 5, 240, WIDTH, 60);
    }

    @Override
    public boolean isEnabled() {
        return MinepieceEssentialsClient.getInstance().getConfigManager().config().armorHudEnabled;
    }

    @Override
    public void tick() {
        ArmorScanner.tick();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, float tickDelta) {
        ArmorState.Snapshot snap = ArmorState.get();
        String title = Component.translatable("minepiece.ui.armor.title").getString();
        if (snap.isEmpty()) {
            this.height = 32;
            ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, 32, title, getBackground());
            RenderUtils.drawText(ctx, Component.translatable("minepiece.ui.armor.hint").getString(), 6, 20, SET_PARTIAL_COLOR);
            return;
        }

        int count = snap.pieces().size();
        int stats = (int) snap.totals().values().stream().filter(v -> v != 0.0).count();
        boolean setLine = snap.setName() != null;
        String effect = snap.fullSet() ? ArmorSetEffect.summarize(snap.setEffect(),
                Component.translatable("minepiece.ui.armor.effect_xp").getString(),
                Component.translatable("minepiece.ui.armor.effect_xp_profile").getString()) : "";
        int h = 20 + count * ROW + (setLine ? 10 : 0) + 6 + stats * 10 + 4;
        this.height = h;
        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, title + " (" + count + "/4)", getBackground());

        int y = 20;
        for (ArmorState.WornPiece worn : snap.pieces()) {
            drawIcon(ctx, worn, 6, y - 2);
            RenderUtils.drawTextFit(ctx, worn.piece().name(), 22, y + 1, WIDTH - 28, NAME_COLOR);
            y += ROW;
        }

        if (setLine) {
            String key = snap.fullSet() ? "minepiece.ui.armor.full_set" : "minepiece.ui.armor.partial_set";
            String line = Component.translatable(key, snap.setName(), count).getString();
            int ex = WIDTH - 6;
            if (!effect.isEmpty()) {
                ex = WIDTH - 6 - RenderUtils.textWidth(effect);
                RenderUtils.drawText(ctx, effect, ex, y, EFFECT_COLOR);
            }
            RenderUtils.drawTextFit(ctx, line, 6, y, ex - 10, snap.fullSet() ? SET_COLOR : SET_PARTIAL_COLOR);
            y += 10;
        }

        y += 6;
        for (PetStat stat : PetStat.values()) {
            Double value = snap.totals().get(stat);
            if (value == null || value == 0.0) continue;
            String name = snap.labels().get(stat);
            if (name == null || name.isBlank()) name = stat.fr();
            String v = StatLine.right(stat, value);
            int vx = WIDTH - 6 - RenderUtils.textWidth(v);
            RenderUtils.drawText(ctx, v, vx, y, stat.color());
            RenderUtils.drawTextFit(ctx, name, 6, y, vx - 10, stat.color());
            y += 10;
        }
    }

    /** Draws the piece's item at 12 px (16 px item scaled by 0.75). */
    private static void drawIcon(GuiGraphicsExtractor ctx, ArmorState.WornPiece worn, int x, int y) {
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale(0.75f, 0.75f);
        ctx.item(worn.stack(), 0, 0);
        ctx.pose().popMatrix();
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) return Long.toString(Math.round(v));
        return String.format(java.util.Locale.US, "%.1f", v);
    }
}
