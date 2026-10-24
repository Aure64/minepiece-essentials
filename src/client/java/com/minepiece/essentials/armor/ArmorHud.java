package com.minepiece.essentials.armor;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** HUD panel listing the worn armor pieces (icon + name) and the total stats they grant. */
public class ArmorHud extends HudElement {

    private static final int WIDTH = 170;
    private static final int ROW = 12;          // icon rows are 16 px scaled to 12
    private static final int ICON = 12;
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
        // ArmorScanner est tické par MinepieceEssentialsClient : le profil en dépend même HUD masqué.
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
        boolean setLine = snap.setName() != null;
        String effect = snap.fullSet() ? ArmorSetEffect.summarize(snap.setEffect(),
                Component.translatable("minepiece.ui.armor.effect_xp").getString(),
                Component.translatable("minepiece.ui.armor.effect_xp_profile").getString()) : "";
        int h = 20 + count * ROW + (setLine ? 10 : 0) + 4;
        this.height = h;
        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, title + " (" + count + "/4)", getBackground());

        boolean mirror = isMirrored();
        int y = 20;
        for (ArmorState.WornPiece worn : snap.pieces()) {
            if (mirror) {
                RenderUtils.drawItemIcon(ctx, worn.stack(), WIDTH - 6 - ICON, y - 2, ICON);
                RenderUtils.drawTextFitRight(ctx, worn.piece().name(), WIDTH - 6 - ICON - 4, y + 1, WIDTH - 28, NAME_COLOR);
            } else {
                RenderUtils.drawItemIcon(ctx, worn.stack(), 6, y - 2, ICON);
                RenderUtils.drawTextFit(ctx, worn.piece().name(), 6 + ICON + 4, y + 1, WIDTH - 28, NAME_COLOR);
            }
            y += ROW;
        }

        if (setLine) {
            String key = snap.fullSet() ? "minepiece.ui.armor.full_set" : "minepiece.ui.armor.partial_set";
            String line = Component.translatable(key, snap.setName(), count).getString();
            int lineColor = snap.fullSet() ? SET_COLOR : SET_PARTIAL_COLOR;
            int ew = effect.isEmpty() ? 0 : RenderUtils.textWidth(effect) + 10;
            if (mirror) {
                if (!effect.isEmpty()) RenderUtils.drawText(ctx, effect, 6, y, EFFECT_COLOR);
                RenderUtils.drawTextFitRight(ctx, line, WIDTH - 6, y, WIDTH - 12 - ew, lineColor);
            } else {
                if (!effect.isEmpty()) RenderUtils.drawText(ctx, effect, WIDTH - 6 - RenderUtils.textWidth(effect), y, EFFECT_COLOR);
                RenderUtils.drawTextFit(ctx, line, 6, y, WIDTH - 12 - ew, lineColor);
            }
            y += 10;
        }

    }

}
