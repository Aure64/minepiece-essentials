package com.minepiece.essentials.util;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

public final class RenderUtils {
    private RenderUtils() {}

    public static void drawParchmentBox(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, ColorUtils.PARCHMENT_BG);
        ctx.fill(x, y, x + w, y + 2, ColorUtils.PARCHMENT_BORDER);
        ctx.fill(x, y + h - 2, x + w, y + h, ColorUtils.PARCHMENT_BORDER);
        ctx.fill(x, y, x + 2, y + h, ColorUtils.PARCHMENT_BORDER);
        ctx.fill(x + w - 2, y, x + w, y + h, ColorUtils.PARCHMENT_BORDER);
        ctx.fill(x, y, x + 4, y + 4, ColorUtils.PARCHMENT_BORDER);
        ctx.fill(x + w - 4, y, x + w, y + 4, ColorUtils.PARCHMENT_BORDER);
        ctx.fill(x, y + h - 4, x + 4, y + h, ColorUtils.PARCHMENT_BORDER);
        ctx.fill(x + w - 4, y + h - 4, x + w, y + h, ColorUtils.PARCHMENT_BORDER);
    }

    public static void drawProgressBar(DrawContext ctx, int x, int y, int w, int h,
                                        float progress, int color) {
        ctx.fill(x, y, x + w, y + h, 0x80000000);
        int fillWidth = (int)(w * Math.max(0, Math.min(1, progress)));
        ctx.fill(x, y, x + fillWidth, y + h, color);
    }

    public static void drawText(DrawContext ctx, String text, int x, int y, int color) {
        ctx.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, color, true);
    }

    public static void drawCenteredText(DrawContext ctx, String text, int centerX, int y, int color) {
        int w = MinecraftClient.getInstance().textRenderer.getWidth(text);
        drawText(ctx, text, centerX - w / 2, y, color);
    }

    public static int textWidth(String text) {
        return MinecraftClient.getInstance().textRenderer.getWidth(text);
    }
}
