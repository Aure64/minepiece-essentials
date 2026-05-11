package com.minepiece.essentials.hud;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.boss.BossTimerHud;
import com.minepiece.essentials.util.ColorUtils;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {
    private HudElement dragging = null;
    private int dragOffsetX, dragOffsetY;

    public HudEditScreen() {
        super(Text.literal("HUD Editor"));
    }

    @Override
    protected void init() {
        HudElementRegistry.setEditMode(true);
    }

    @Override
    public void removed() {
        HudElementRegistry.setEditMode(false);
        // Reset hover state
        for (HudElement element : HudElementRegistry.getElements()) {
            if (element instanceof BossTimerHud bossHud) {
                bossHud.setMousePos(-1, -1);
            }
        }
        MinepieceEssentialsClient.getInstance().getConfigManager().save();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x44000000);

        RenderUtils.drawCenteredText(ctx, "Mode Edition HUD - Clic gauche: deplacer | Clic droit: toggle | Scroll: resize",
                width / 2, 5, 0xFFFFFFFF);
        RenderUtils.drawCenteredText(ctx, "Echap pour sauvegarder et quitter",
                width / 2, 17, 0xFFAAAAAA);

        for (HudElement element : HudElementRegistry.getElements()) {
            // Pass mouse position for hover effects
            if (element instanceof BossTimerHud bossHud) {
                bossHud.setMousePos(mouseX, mouseY);
            }

            int x = element.getX();
            int y = element.getY();
            int w = element.getWidth();
            int h = element.getHeight();

            int borderColor = element.isVisible() ? 0xFF00FF00 : 0xFFFF0000;
            if (element == dragging) borderColor = 0xFFFFFF00;

            ctx.fill(x - 1, y - 1, x + w + 1, y, borderColor);
            ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, borderColor);
            ctx.fill(x - 1, y, x, y + h, borderColor);
            ctx.fill(x + w, y, x + w + 1, y + h, borderColor);

            // Label with visibility status
            String status = element.isVisible() ? " [ON]" : " [OFF]";
            int statusColor = element.isVisible() ? 0xFF00FF00 : 0xFFFF4444;
            RenderUtils.drawText(ctx, element.getId(), x + 2, y - 10, 0xFFFFFFFF);
            RenderUtils.drawText(ctx, status, x + 2 + RenderUtils.textWidth(element.getId()), y - 10, statusColor);

            ctx.fill(x + w - 6, y + h - 6, x + w, y + h, 0xCCFFFFFF);

            ctx.getMatrices().pushMatrix();
            float scale = element.getScale();
            ctx.getMatrices().translate(x, y);
            ctx.getMatrices().scale(scale, scale);
            element.render(ctx, delta);
            ctx.getMatrices().popMatrix();
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 0) {
            for (HudElement element : HudElementRegistry.getElements()) {
                if (element instanceof BossTimerHud bossHud) {
                    if (bossHud.handleClick(mouseX, mouseY)) {
                        return true;
                    }
                }
            }
        }

        for (HudElement element : HudElementRegistry.getElements()) {
            if (element.isMouseOver(mouseX, mouseY)) {
                if (button == 0) {
                    dragging = element;
                    dragOffsetX = (int) mouseX - element.getX();
                    dragOffsetY = (int) mouseY - element.getY();
                    return true;
                } else if (button == 1) {
                    var layout = element.getLayout();
                    layout.visible = !layout.visible;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (dragging != null && click.button() == 0) {
            var layout = dragging.getLayout();
            layout.x = Math.max(0, Math.min(width - dragging.getWidth(), (int) mouseX - dragOffsetX));
            layout.y = Math.max(0, Math.min(height - dragging.getHeight(), (int) mouseY - dragOffsetY));
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (HudElement element : HudElementRegistry.getElements()) {
            if (element.isMouseOver(mouseX, mouseY)) {
                var layout = element.getLayout();
                layout.scale = Math.max(0.5f, Math.min(2.0f, layout.scale + (float) verticalAmount * 0.1f));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() { return false; }
}
