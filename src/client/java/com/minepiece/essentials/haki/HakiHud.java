package com.minepiece.essentials.haki;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;

/** Small HUD showing the remaining haki cooldown after activation. */
public class HakiHud extends HudElement {

    private static final int W = 130;
    private static final int COLOR_TEXT = 0xFFCBC8C7;
    private static final int COLOR_COOLDOWN = 0xFF9B30FF;
    private static final int COLOR_READY = 0xFF00CC00;
    private static final int READY_DISPLAY_TICKS = 5 * 20;

    private int readyDisplayTicks = 0;

    public HakiHud() {
        super("haki_timer", 5, 160, W, 32);
    }

    @Override
    public void tick() {
        HakiTimer.tick();
        if (HakiTimer.isActive()) {
            readyDisplayTicks = READY_DISPLAY_TICKS;
        } else if (readyDisplayTicks > 0) {
            readyDisplayTicks--;
        }
    }

    @Override
    public void render(DrawContext ctx, float tickDelta) {
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().hakiTimerEnabled) {
            return;
        }
        if (!HakiTimer.isActive() && readyDisplayTicks <= 0) return;

        int h = 38;
        this.height = h;
        ParchmentRenderer.renderPanel(ctx, 0, 0, W, h, "Haki");

        int barW = W - 24;
        int barX = (W - barW) / 2; // centred
        int textY = 20;
        int barY = 30;
        if (HakiTimer.isActive()) {
            RenderUtils.drawCenteredText(ctx, HakiTimer.remainingSeconds() + "s", W / 2, textY, COLOR_TEXT);
            // Draining bar: full on activation, empties as the cooldown elapses.
            RenderUtils.drawProgressBar(ctx, barX, barY, barW, 5, 1f - HakiTimer.progress(), COLOR_COOLDOWN);
        } else {
            RenderUtils.drawCenteredText(ctx, "Prêt !", W / 2, textY, COLOR_READY);
            RenderUtils.drawProgressBar(ctx, barX, barY, barW, 5, 1f, COLOR_READY);
        }
    }
}
