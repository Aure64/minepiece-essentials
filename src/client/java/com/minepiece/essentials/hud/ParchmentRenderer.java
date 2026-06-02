package com.minepiece.essentials.hud;

import com.minepiece.essentials.config.HudBackground;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import java.util.List;

public class ParchmentRenderer {

    /** Renders a panel with the classic parchment style. */
    public static void renderPanel(DrawContext ctx, int x, int y, int w, int h, String title) {
        renderPanel(ctx, x, y, w, h, title, HudBackground.PARCHMENT);
    }

    /** Renders a panel using the given background preset's fill/border/text colours. */
    public static void renderPanel(DrawContext ctx, int x, int y, int w, int h, String title,
                                   HudBackground bg) {
        RenderUtils.drawParchmentBox(ctx, x, y, w, h, bg.bgColor(), bg.borderColor());
        if (title != null) {
            RenderUtils.drawCenteredText(ctx, title, x + w / 2, y + 5, bg.textColor());
            if ((bg.borderColor() >>> 24) != 0) {
                ctx.fill(x + 4, y + 16, x + w - 4, y + 17, bg.borderColor());
            }
        }
    }

    public static int renderList(DrawContext ctx, int x, int y, int w,
                                 String title, List<String> lines) {
        return renderList(ctx, x, y, w, title, lines, HudBackground.PARCHMENT);
    }

    public static int renderList(DrawContext ctx, int x, int y, int w,
                                 String title, List<String> lines, HudBackground bg) {
        int lineHeight = 12;
        int totalHeight = 20 + lines.size() * lineHeight + 6;
        renderPanel(ctx, x, y, w, totalHeight, title, bg);
        int textY = y + 20;
        for (String line : lines) {
            RenderUtils.drawText(ctx, line, x + 6, textY, bg.textColor());
            textY += lineHeight;
        }
        return totalHeight;
    }
}
