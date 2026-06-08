package com.minepiece.essentials.haki;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Small always-on HUD: shows the remaining haki cooldown, or "Prêt !" when ready. */
public class HakiHud extends HudElement {

    private static final int W = 130;
    private static final int COLOR_TEXT = 0xFFCBC8C7;
    private static final int COLOR_READY = 0xFF00CC00;
    private static final int ICON_SIZE = 10;
    // King (Conqueror's) haki icon — provided by the MinePiece server resource pack.
    private static final Identifier KING_HAKI =
            Identifier.of("items", "textures/global/haki/king_haki.png");

    public HakiHud() {
        super("haki_timer", 5, 160, W, 32);
    }

    @Override
    public void tick() {
        HakiTimer.tick();
    }

    @Override
    public void render(DrawContext ctx, float tickDelta) {
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().hakiTimerEnabled) {
            return;
        }

        int h = 30;
        this.height = h;
        // Panel only — the title is drawn below so the logo + "Haki" stay grouped.
        ParchmentRenderer.renderPanel(ctx, 0, 0, W, h, null, getBackground());

        // Header row: logo + "Haki", centred as a single group and vertically aligned.
        final int GAP = 3;
        String hakiTitle = Text.translatable("minepiece.ui.haki.title").getString();
        int titleW = RenderUtils.textWidth(hakiTitle);
        int groupX = (W - (ICON_SIZE + GAP + titleW)) / 2;
        int rowY = 5;
        RenderUtils.drawIcon(ctx, KING_HAKI, groupX, rowY - 1, ICON_SIZE);
        RenderUtils.drawText(ctx, hakiTitle, groupX + ICON_SIZE + GAP, rowY, getBackground().textColor());

        int textY = 18;
        if (HakiTimer.isActive()) {
            RenderUtils.drawCenteredText(ctx, HakiTimer.remainingSeconds() + "s", W / 2, textY, COLOR_TEXT);
        } else {
            RenderUtils.drawCenteredText(ctx, Text.translatable("minepiece.ui.haki.ready").getString(), W / 2, textY, COLOR_READY);
        }
    }
}
