package com.minepiece.essentials.hud;

import com.minepiece.essentials.util.ColorUtils;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import java.util.List;

public class ParchmentRenderer {

    public static void renderPanel(DrawContext ctx, int x, int y, int w, int h, String title) {
        RenderUtils.drawParchmentBox(ctx, x, y, w, h);
        if (title != null) {
            RenderUtils.drawCenteredText(ctx, title, x + w / 2, y + 5, ColorUtils.TEXT_DARK);
            ctx.fill(x + 4, y + 16, x + w - 4, y + 17, ColorUtils.PARCHMENT_BORDER);
        }
    }

    public static int renderList(DrawContext ctx, int x, int y, int w,
                                  String title, List<String> lines) {
        int lineHeight = 12;
        int totalHeight = 20 + lines.size() * lineHeight + 6;
        renderPanel(ctx, x, y, w, totalHeight, title);
        int textY = y + 20;
        for (String line : lines) {
            RenderUtils.drawText(ctx, line, x + 6, textY, ColorUtils.TEXT_DARK);
            textY += lineHeight;
        }
        return totalHeight;
    }
}
