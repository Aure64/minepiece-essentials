package com.minepiece.essentials.util;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;

public final class RenderUtils {
    private RenderUtils() {}

    /**
     * Draws a square texture (referenced by its resource path, e.g. one provided
     * by the server resource pack) at {@code (x, y)} scaled to {@code size} px.
     * Assumes a 16×16 source. Renders nothing visible if the pack isn't loaded.
     */
    public static void drawIcon(DrawContext ctx, Identifier texture, int x, int y, int size) {
        float s = size / 16f;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(s, s);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, 16, 16, 16, 16);
        ctx.getMatrices().popMatrix();
    }

    /**
     * Dessine une texture de taille native arbitraire ({@code texW}×{@code texH}),
     * mise à l'échelle par {@code scale}, en haut-gauche de {@code (x, y)}.
     */
    public static void drawTextureScaled(DrawContext ctx, Identifier texture,
                                         int x, int y, float scale, int texW, int texH) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, texW, texH, texW, texH);
        ctx.getMatrices().popMatrix();
    }

    /** Parchment-style box with the classic colours. */
    public static void drawParchmentBox(DrawContext ctx, int x, int y, int w, int h) {
        drawParchmentBox(ctx, x, y, w, h, ColorUtils.PARCHMENT_BG, ColorUtils.PARCHMENT_BORDER);
    }

    /**
     * Panel box with explicit fill and border colours. A fully-transparent
     * (alpha 0) fill or border is skipped, so a "transparent" preset draws nothing.
     */
    public static void drawParchmentBox(DrawContext ctx, int x, int y, int w, int h,
                                        int bgColor, int borderColor) {
        if ((bgColor >>> 24) != 0) {
            ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, bgColor);
        }
        if ((borderColor >>> 24) != 0) {
            ctx.fill(x, y, x + w, y + 2, borderColor);
            ctx.fill(x, y + h - 2, x + w, y + h, borderColor);
            ctx.fill(x, y, x + 2, y + h, borderColor);
            ctx.fill(x + w - 2, y, x + w, y + h, borderColor);
            ctx.fill(x, y, x + 4, y + 4, borderColor);
            ctx.fill(x + w - 4, y, x + w, y + 4, borderColor);
            ctx.fill(x, y + h - 4, x + 4, y + h, borderColor);
            ctx.fill(x + w - 4, y + h - 4, x + w, y + h, borderColor);
        }
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

    /**
     * Draws text shrunk uniformly so its full content fits within {@code maxWidth}
     * (never enlarged). Lets long lines stay readable instead of being clipped to "..".
     */
    public static void drawTextFit(DrawContext ctx, String text, int x, int y, int maxWidth, int color) {
        var tr = MinecraftClient.getInstance().textRenderer;
        int w = tr.getWidth(text);
        if (w <= maxWidth || w == 0) {
            ctx.drawText(tr, text, x, y, color, true);
            return;
        }
        float scale = (float) maxWidth / w;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawText(tr, text, 0, 0, color, true);
        ctx.getMatrices().popMatrix();
    }

    public static void drawCenteredText(DrawContext ctx, String text, int centerX, int y, int color) {
        int w = MinecraftClient.getInstance().textRenderer.getWidth(text);
        drawText(ctx, text, centerX - w / 2, y, color);
    }

    public static int textWidth(String text) {
        return MinecraftClient.getInstance().textRenderer.getWidth(text);
    }
}
