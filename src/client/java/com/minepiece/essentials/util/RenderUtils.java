package com.minepiece.essentials.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class RenderUtils {
    private RenderUtils() {}

    /**
     * Draws a square texture (referenced by its resource path, e.g. one provided
     * by the server resource pack) at {@code (x, y)} scaled to {@code size} px.
     * Assumes a 16×16 source. Renders nothing visible if the pack isn't loaded.
     */
    public static void drawIcon(GuiGraphics ctx, Identifier texture, int x, int y, int size) {
        float s = size / 16f;
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale(s, s);
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, 16, 16, 16, 16);
        ctx.pose().popMatrix();
    }

    /**
     * Dessine une texture de taille native arbitraire ({@code texW}×{@code texH}),
     * mise à l'échelle par {@code scale}, en haut-gauche de {@code (x, y)}.
     */
    public static void drawTextureScaled(GuiGraphics ctx, Identifier texture,
                                         int x, int y, float scale, int texW, int texH) {
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale(scale, scale);
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, texW, texH, texW, texH);
        ctx.pose().popMatrix();
    }

    /** Parchment-style box with the classic colours. */
    public static void drawParchmentBox(GuiGraphics ctx, int x, int y, int w, int h) {
        drawParchmentBox(ctx, x, y, w, h, ColorUtils.PARCHMENT_BG, ColorUtils.PARCHMENT_BORDER);
    }

    /**
     * Panel box with explicit fill and border colours. A fully-transparent
     * (alpha 0) fill or border is skipped, so a "transparent" preset draws nothing.
     */
    public static void drawParchmentBox(GuiGraphics ctx, int x, int y, int w, int h,
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

    public static void drawProgressBar(GuiGraphics ctx, int x, int y, int w, int h,
                                        float progress, int color) {
        ctx.fill(x, y, x + w, y + h, 0x80000000);
        int fillWidth = (int)(w * Math.max(0, Math.min(1, progress)));
        ctx.fill(x, y, x + fillWidth, y + h, color);
    }

    public static void drawText(GuiGraphics ctx, String text, int x, int y, int color) {
        ctx.drawString(Minecraft.getInstance().font, text, x, y, color, true);
    }

    /**
     * Draws text shrunk uniformly so its full content fits within {@code maxWidth}
     * (never enlarged). Lets long lines stay readable instead of being clipped to "..".
     */
    public static void drawTextFit(GuiGraphics ctx, String text, int x, int y, int maxWidth, int color) {
        var tr = Minecraft.getInstance().font;
        int w = tr.width(text);
        if (w <= maxWidth || w == 0) {
            ctx.drawString(tr, text, x, y, color, true);
            return;
        }
        float scale = (float) maxWidth / w;
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale(scale, scale);
        ctx.drawString(tr, text, 0, 0, color, true);
        ctx.pose().popMatrix();
    }

    public static void drawCenteredText(GuiGraphics ctx, String text, int centerX, int y, int color) {
        int w = Minecraft.getInstance().font.width(text);
        drawText(ctx, text, centerX - w / 2, y, color);
    }

    public static int textWidth(String text) {
        return Minecraft.getInstance().font.width(text);
    }
}
