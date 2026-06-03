package com.minepiece.essentials.hud;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.boss.BossTimerHud;
import com.minepiece.essentials.config.HudBackground;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {
    private static final int BTN_W = 180;
    private static final int BTN_H = 16;
    private static final int TAB_W = 120;
    private static final int TAB_H = 16;
    private static final int TAB_GAP = 4;

    private enum Tab { PLACEMENT, CUSTOMIZE }
    private Tab tab = Tab.PLACEMENT;

    private HudElement dragging = null;
    private int dragOffsetX, dragOffsetY;

    private int tabsX, tabsY;
    private int resetBtnX, resetBtnY;
    private long resetMsgUntil = 0;

    // Toggles de la fonctionnalité Raretés (onglet Placement).
    private int rarityTogglesX, rarityTogglesY;
    private static final int TOGGLE_STEP = BTN_H + 4;

    public HudEditScreen() {
        super(Text.literal("HUD Editor"));
    }

    @Override
    protected void init() {
        HudElementRegistry.setEditMode(true);
        tabsY = 26;
        tabsX = (width - (TAB_W * 2 + TAB_GAP)) / 2;
        resetBtnX = (width - BTN_W) / 2;
        resetBtnY = tabsY + TAB_H + 6;
        rarityTogglesX = (width - BTN_W) / 2;
        rarityTogglesY = resetBtnY + BTN_H + 28;
    }

    // --- geometry helpers ----------------------------------------------------

    private int placementTabX() { return tabsX; }
    private int customizeTabX() { return tabsX + TAB_W + TAB_GAP; }

    private boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean overResetButton(double mx, double my) {
        return inBox(mx, my, resetBtnX, resetBtnY, BTN_W, BTN_H);
    }

    private void resetPlacement() {
        // Clearing the profile drops every saved position/scale/visibility/style;
        // each HUD's getLayout() then recreates it at its default. Fixes HUDs that
        // ended up off-screen on very small or very large resolutions.
        var cfg = MinepieceEssentialsClient.getInstance().getConfigManager();
        cfg.layout().activeProfile().elements.clear();
        cfg.save();
        resetMsgUntil = System.currentTimeMillis() + 2500;
    }

    @Override
    public void removed() {
        HudElementRegistry.setEditMode(false);
        for (HudElement element : HudElementRegistry.getElements()) {
            if (element instanceof BossTimerHud bossHud) {
                bossHud.setMousePos(-1, -1);
            }
        }
        MinepieceEssentialsClient.getInstance().getConfigManager().save();
    }

    // --- rendering -----------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x44000000);

        String hint = tab == Tab.PLACEMENT
                ? "Placement - Clic gauche: deplacer | Clic droit: ON/OFF | Scroll: taille"
                : "Personnaliser - Clic sur un HUD: change son fond (Parchemin -> Sombre -> Transparent)";
        RenderUtils.drawCenteredText(ctx, hint, width / 2, 5, 0xFFFFFFFF);
        RenderUtils.drawCenteredText(ctx, "Echap pour sauvegarder et quitter", width / 2, 16, 0xFFAAAAAA);

        drawTabs(ctx, mouseX, mouseY);

        if (tab == Tab.PLACEMENT) {
            renderPlacement(ctx, mouseX, mouseY, delta);
        } else {
            renderCustomize(ctx, mouseX, mouseY, delta);
        }
    }

    private void drawTabs(DrawContext ctx, int mouseX, int mouseY) {
        drawTab(ctx, placementTabX(), "Placement", tab == Tab.PLACEMENT,
                inBox(mouseX, mouseY, placementTabX(), tabsY, TAB_W, TAB_H));
        drawTab(ctx, customizeTabX(), "Personnaliser", tab == Tab.CUSTOMIZE,
                inBox(mouseX, mouseY, customizeTabX(), tabsY, TAB_W, TAB_H));
    }

    private void drawTab(DrawContext ctx, int x, String label, boolean active, boolean hover) {
        int bg = active ? 0xFF6A4A2C : (hover ? 0xFF4A3422 : 0xFF2A1E14);
        ctx.fill(x, tabsY, x + TAB_W, tabsY + TAB_H, bg);
        ctx.fill(x, tabsY, x + TAB_W, tabsY + 1, 0xFF8A6A44);
        RenderUtils.drawCenteredText(ctx, label, x + TAB_W / 2, tabsY + 4,
                active ? 0xFFFFE9D5 : 0xFFBFae9C);
    }

    private void renderPlacement(DrawContext ctx, int mouseX, int mouseY, float delta) {
        for (HudElement element : HudElementRegistry.getElements()) {
            if (element instanceof BossTimerHud bossHud) {
                bossHud.setMousePos(mouseX, mouseY);
            }

            int x = element.getX(), y = element.getY();
            int w = element.getWidth(), h = element.getHeight();

            int borderColor = element.isVisible() ? 0xFF00FF00 : 0xFFFF0000;
            if (element == dragging) borderColor = 0xFFFFFF00;

            ctx.fill(x - 1, y - 1, x + w + 1, y, borderColor);
            ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, borderColor);
            ctx.fill(x - 1, y, x, y + h, borderColor);
            ctx.fill(x + w, y, x + w + 1, y + h, borderColor);

            String status = element.isVisible() ? " [ON]" : " [OFF]";
            int statusColor = element.isVisible() ? 0xFF00FF00 : 0xFFFF4444;
            RenderUtils.drawText(ctx, element.getId(), x + 2, y - 10, 0xFFFFFFFF);
            RenderUtils.drawText(ctx, status, x + 2 + RenderUtils.textWidth(element.getId()), y - 10, statusColor);

            ctx.fill(x + w - 6, y + h - 6, x + w, y + h, 0xCCFFFFFF);

            drawElement(ctx, element, delta);
        }

        boolean hover = overResetButton(mouseX, mouseY);
        ctx.fill(resetBtnX, resetBtnY, resetBtnX + BTN_W, resetBtnY + BTN_H, hover ? 0xFF6A4A2C : 0xFF3A2A1C);
        ctx.fill(resetBtnX, resetBtnY, resetBtnX + BTN_W, resetBtnY + 1, 0xFF8A6A44);
        ctx.fill(resetBtnX, resetBtnY + BTN_H - 1, resetBtnX + BTN_W, resetBtnY + BTN_H, 0xFF8A6A44);
        RenderUtils.drawCenteredText(ctx, "Reinitialiser le placement", width / 2, resetBtnY + 4, 0xFFFFE9D5);

        if (System.currentTimeMillis() < resetMsgUntil) {
            RenderUtils.drawCenteredText(ctx, "Placement reinitialise !", width / 2, resetBtnY + BTN_H + 4, 0xFF7CFC55);
        }

        // Toggles de la fonctionnalité Raretés.
        var cfg = MinepieceEssentialsClient.getInstance().getConfigManager().config();
        RenderUtils.drawCenteredText(ctx, "Raretes", width / 2, rarityTogglesY - 12, 0xFFFFE9D5);
        drawToggle(ctx, mouseX, mouseY, 0, "Emblemes - coffres", cfg.rarityIconsEnabled);
        drawToggle(ctx, mouseX, mouseY, 1, "Emblemes - inventaire", cfg.rarityInventoryEnabled);
        drawToggle(ctx, mouseX, mouseY, 2, "Emblemes - hotbar", cfg.rarityHotbarEnabled);
        drawToggle(ctx, mouseX, mouseY, 3, "Barre de filtre", cfg.rarityFilterEnabled);
        drawToggle(ctx, mouseX, mouseY, 4, "Boutons de tri", cfg.raritySorterEnabled);
    }

    private void drawToggle(DrawContext ctx, int mouseX, int mouseY, int index, String label, boolean on) {
        int y = rarityTogglesY + index * TOGGLE_STEP;
        boolean hover = inBox(mouseX, mouseY, rarityTogglesX, y, BTN_W, BTN_H);
        ctx.fill(rarityTogglesX, y, rarityTogglesX + BTN_W, y + BTN_H, hover ? 0xFF6A4A2C : 0xFF3A2A1C);
        ctx.fill(rarityTogglesX, y, rarityTogglesX + BTN_W, y + 1, 0xFF8A6A44);
        ctx.fill(rarityTogglesX, y + BTN_H - 1, rarityTogglesX + BTN_W, y + BTN_H, 0xFF8A6A44);
        RenderUtils.drawCenteredText(ctx, label + (on ? "  [ON]" : "  [OFF]"),
                width / 2, y + 4, on ? 0xFF7CFC55 : 0xFFFF6666);
    }

    private void renderCustomize(DrawContext ctx, int mouseX, int mouseY, float delta) {
        for (HudElement element : HudElementRegistry.getElements()) {
            int x = element.getX(), y = element.getY();
            int w = element.getWidth(), h = element.getHeight();

            // Faint clickable outline so even an empty HUD can be targeted.
            boolean hover = element.isMouseOver(mouseX, mouseY);
            int border = hover ? 0xFFFFFF66 : 0x66FFFFFF;
            ctx.fill(x - 1, y - 1, x + w + 1, y, border);
            ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, border);
            ctx.fill(x - 1, y, x, y + h, border);
            ctx.fill(x + w, y, x + w + 1, y + h, border);

            RenderUtils.drawText(ctx, element.getId() + " : " + element.getBackground().label(),
                    x + 2, y - 10, hover ? 0xFFFFFF66 : 0xFFFFFFFF);

            drawElement(ctx, element, delta);
        }
    }

    /** Renders a HUD at its position with its scale applied. */
    private void drawElement(DrawContext ctx, HudElement element, float delta) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(element.getX(), element.getY());
        ctx.getMatrices().scale(element.getScale(), element.getScale());
        element.render(ctx, delta);
        ctx.getMatrices().popMatrix();
    }

    // --- input ---------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        // Tab switching is available in both tabs.
        if (button == 0 && inBox(mouseX, mouseY, placementTabX(), tabsY, TAB_W, TAB_H)) {
            tab = Tab.PLACEMENT;
            return true;
        }
        if (button == 0 && inBox(mouseX, mouseY, customizeTabX(), tabsY, TAB_W, TAB_H)) {
            tab = Tab.CUSTOMIZE;
            return true;
        }

        if (tab == Tab.CUSTOMIZE) {
            return clickCustomize(mouseX, mouseY, button);
        }
        return clickPlacement(mouseX, mouseY, button, click, doubled);
    }

    private boolean clickCustomize(double mouseX, double mouseY, int button) {
        for (HudElement element : HudElementRegistry.getElements()) {
            if (element.isMouseOver(mouseX, mouseY)) {
                // Left cycles forward, right cycles backward (= forward ×2 of 3).
                HudBackground bg = element.getBackground().next();
                if (button == 1) bg = bg.next();
                element.setBackground(bg);
                MinepieceEssentialsClient.getInstance().getConfigManager().save();
                return true;
            }
        }
        return false;
    }

    private boolean clickPlacement(double mouseX, double mouseY, int button, Click click, boolean doubled) {
        if (button == 0 && overResetButton(mouseX, mouseY)) {
            resetPlacement();
            return true;
        }

        if (button == 0) {
            for (int i = 0; i < 5; i++) {
                int ty = rarityTogglesY + i * TOGGLE_STEP;
                if (inBox(mouseX, mouseY, rarityTogglesX, ty, BTN_W, BTN_H)) {
                    var mgr = MinepieceEssentialsClient.getInstance().getConfigManager();
                    var cfg = mgr.config();
                    switch (i) {
                        case 0 -> cfg.rarityIconsEnabled = !cfg.rarityIconsEnabled;
                        case 1 -> cfg.rarityInventoryEnabled = !cfg.rarityInventoryEnabled;
                        case 2 -> cfg.rarityHotbarEnabled = !cfg.rarityHotbarEnabled;
                        case 3 -> cfg.rarityFilterEnabled = !cfg.rarityFilterEnabled;
                        default -> cfg.raritySorterEnabled = !cfg.raritySorterEnabled;
                    }
                    mgr.save();
                    return true;
                }
            }
        }

        if (button == 0) {
            for (HudElement element : HudElementRegistry.getElements()) {
                if (element instanceof BossTimerHud bossHud && bossHud.handleClick(mouseX, mouseY)) {
                    return true;
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
        if (tab != Tab.PLACEMENT) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
