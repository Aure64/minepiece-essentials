package com.minepiece.essentials.help;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.donate.DonateScreen;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Book-style help popup explaining the mod's features and how to use them.
 * Required actions are highlighted in red. Shown automatically on first launch
 * (until dismissed) and reopenable with the H key.
 */
public class HelpScreen extends Screen {

    private static final int PANEL_W = 350;
    private static final int HEADER_H = 24;
    // Pied de page : rangée dismiss/close, puis rangée dédiée au don (mise en avant, tout en bas).
    private static final int FOOTER_H = 66;
    private static final int TITLE_H = 11;
    private static final int LINE_H = 9;
    private static final int FEATURE_GAP = 5;
    private static final int BTN_H = 20;
    private static final int DONATE_ROW_GAP = 6;

    private static final int GOLD = 0xFFFFD27F;
    private static final int GRAY = 0xFFCBC8C7;
    private static final int RED = 0xFFFF6B6B;

    // Palette "parchemin doré" réservée au bouton de don, pour qu'il attire l'œil.
    private static final int DONATE_FILL = 0xFF8A6A44;
    private static final int DONATE_FILL_HOVER = 0xFFF0A857;
    private static final int DONATE_BORDER_LIGHT = 0xFFFFE9D5;
    private static final int DONATE_BORDER_DARK = 0xFF3A2A1C;
    private static final int DONATE_TEXT = 0xFFFFE9D5;
    private static final int DONATE_TEXT_HOVER = 0xFF3A2A1C;

    /** Glyphe de la monnaie du serveur (berries), rendu avec la police custom du pack. */
    private static final String BERRY = "实";
    private static final Identifier BERRY_FONT = Identifier.fromNamespaceAndPath("fonts", "icons");

    private record Line(String text, boolean important) {}

    private record Feature(String title, Line[] lines) {}

    private static Line info(String t) {
        return new Line(t, false);
    }

    private static Line important(String t) {
        return new Line(t, true);
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private static Feature[] buildFeatures() {
        return new Feature[]{
            new Feature(tr("minepiece.ui.help.boss.title"), new Line[]{
                info(tr("minepiece.ui.help.boss.line1")),
                important(tr("minepiece.ui.help.boss.line2")),
                important(tr("minepiece.ui.help.boss.line3")),
                info(tr("minepiece.ui.help.boss.line4")),
                info(tr("minepiece.ui.help.boss.line5")),
            }),
            new Feature(tr("minepiece.ui.help.ascensions.title"), new Line[]{
                info(tr("minepiece.ui.help.ascensions.line1")),
                info(tr("minepiece.ui.help.ascensions.line2")),
            }),
            new Feature(tr("minepiece.ui.help.rarity.title"), new Line[]{
                info(tr("minepiece.ui.help.rarity.line1")),
                info(tr("minepiece.ui.help.rarity.line2")),
            }),
            new Feature(tr("minepiece.ui.help.haki.title"), new Line[]{
                info(tr("minepiece.ui.help.haki.line1")),
            }),
            new Feature(tr("minepiece.ui.help.scrolls.title"), new Line[]{
                info(tr("minepiece.ui.help.scrolls.line1")),
            }),
            new Feature(tr("minepiece.ui.help.pet_quality.title"), new Line[]{
                info(tr("minepiece.ui.help.pet_quality.line1")),
                important(tr("minepiece.ui.help.pet_quality.line2")),
            }),
            new Feature(tr("minepiece.ui.help.minion_calc.title"), new Line[]{
                info(tr("minepiece.ui.help.minion_calc.line1")),
                important(tr("minepiece.ui.help.minion_calc.line2")),
                important(tr("minepiece.ui.help.minion_calc.line3")),
            }),
            new Feature(tr("minepiece.ui.help.active_pets.title"), new Line[]{
                info(tr("minepiece.ui.help.active_pets.line1")),
                important(tr("minepiece.ui.help.active_pets.line2")),
            }),
            new Feature(tr("minepiece.ui.help.active_armor.title"), new Line[]{
                info(tr("minepiece.ui.help.active_armor.line1")),
                info(tr("minepiece.ui.help.active_armor.line2")),
            }),
            new Feature(tr("minepiece.ui.help.profile.title"), new Line[]{
                info(tr("minepiece.ui.help.profile.line1")),
                important(tr("minepiece.ui.help.profile.line2")),
            }),
            new Feature(tr("minepiece.ui.help.job.title"), new Line[]{
                info(tr("minepiece.ui.help.job.line1")),
            }),
            new Feature(tr("minepiece.ui.help.daily_quests.title"), new Line[]{
                info(tr("minepiece.ui.help.daily_quests.line1")),
                important(tr("minepiece.ui.help.daily_quests.line2")),
                info(tr("minepiece.ui.help.daily_quests.line3")),
                info(tr("minepiece.ui.help.daily_quests.line4")),
            }),
            new Feature(tr("minepiece.ui.help.hud_editor.title"), new Line[]{
                important(tr("minepiece.ui.help.hud_editor.line1")),
                info(tr("minepiece.ui.help.hud_editor.line2")),
                info(tr("minepiece.ui.help.hud_editor.line3")),
                info(tr("minepiece.ui.help.hud_editor.line4")),
            }),
            new Feature(tr("minepiece.ui.help.this_help.title"), new Line[]{
                info(tr("minepiece.ui.help.this_help.line1")),
            }),
        };
    }

    // Hauteur de contenu fixe : les features sont réparties en pages (◀ ▶ en bas).
    private static final int CONTENT_H = 150;
    private static final int NAV_H = 22;
    private static final int PANEL_H = HEADER_H + CONTENT_H + NAV_H + FOOTER_H;
    private static final int TEXT_W = PANEL_W - 32;

    /** Une feature avec ses lignes déjà repliées à la largeur du panneau. */
    private record Block(String title, java.util.List<Line> lines) {
        int height() { return TITLE_H + lines.size() * LINE_H + FEATURE_GAP; }
    }

    private java.util.List<java.util.List<Block>> pages = java.util.List.of();
    private int page;

    /** Replie les lignes trop longues puis remplit les pages sans couper une feature. */
    private static java.util.List<java.util.List<Block>> paginate(Feature[] features) {
        Font font = Minecraft.getInstance().font;
        java.util.List<java.util.List<Block>> out = new java.util.ArrayList<>();
        java.util.List<Block> current = new java.util.ArrayList<>();
        int used = 0;
        for (Feature f : features) {
            java.util.List<Line> lines = new java.util.ArrayList<>();
            for (Line l : f.lines()) {
                for (net.minecraft.util.FormattedCharSequence seq : font.split(Component.literal(l.text()), TEXT_W)) {
                    StringBuilder sb = new StringBuilder();
                    seq.accept((i, style, cp) -> { sb.appendCodePoint(cp); return true; });
                    lines.add(new Line(sb.toString(), l.important()));
                }
            }
            Block b = new Block(f.title(), lines);
            if (!current.isEmpty() && used + b.height() > CONTENT_H) {
                out.add(current);
                current = new java.util.ArrayList<>();
                used = 0;
            }
            current.add(b);
            used += b.height();
        }
        if (!current.isEmpty()) out.add(current);
        return out;
    }

    private int left;
    private int top;

    public HelpScreen() {
        super(Component.literal("Minepiece Essentials — Aide"));
    }

    @Override
    protected void init() {
        left = (this.width - PANEL_W) / 2;
        top = (this.height - PANEL_H) / 2;
        pages = paginate(buildFeatures());
        page = Math.min(page, pages.size() - 1);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xB0000000);
        ParchmentRenderer.renderPanel(ctx, left, top, PANEL_W, PANEL_H, "Minepiece Essentials");

        int y = top + HEADER_H;
        if (!pages.isEmpty()) {
            for (Block b : pages.get(page)) {
                RenderUtils.drawText(ctx, "» " + b.title(), left + 12, y, GOLD);
                y += TITLE_H;
                for (Line line : b.lines()) {
                    RenderUtils.drawText(ctx, line.text(), left + 20, y, line.important() ? RED : GRAY);
                    y += LINE_H;
                }
                y += FEATURE_GAP;
            }
        }

        // Navigation ◀ n/N ▶ sous le contenu.
        int navY = navRowY();
        if (pages.size() > 1) {
            drawButton(ctx, left + 12, navY, NAV_BTN_W, "◀", mouseX, mouseY);
            drawButton(ctx, left + PANEL_W - 12 - NAV_BTN_W, navY, NAV_BTN_W, "▶", mouseX, mouseY);
        }
        RenderUtils.drawCenteredText(ctx, (page + 1) + " / " + Math.max(1, pages.size()), left + PANEL_W / 2, navY + 6, GRAY);

        int btnY = topRowY();
        drawButton(ctx, left + 12, btnY, 140, tr("minepiece.ui.help.btn_dismiss"), mouseX, mouseY);
        drawButton(ctx, left + PANEL_W - 12 - 90, btnY, 90, tr("minepiece.ui.help.btn_close"), mouseX, mouseY);

        // Bouton de don : rangée dédiée tout en bas du panneau, traitement doré pour se distinguer.
        if (ServerDetector.isOnMinePiece()) {
            drawDonateButton(ctx, left + 12, donateRowY(), PANEL_W - 24, tr("minepiece.ui.donate.btn_donate"), mouseX, mouseY);
        }
    }

    private static final int NAV_BTN_W = 40;

    /** Rangée de navigation entre pages, juste sous le contenu. */
    private int navRowY() {
        return top + HEADER_H + CONTENT_H;
    }

    /** Rangée du haut (dismiss/close). */
    private int topRowY() {
        return top + PANEL_H - FOOTER_H + 12;
    }

    private void turnPage(int delta) {
        if (pages.isEmpty()) return;
        page = Math.floorMod(page + delta, pages.size());
    }

    /** Rangée dédiée au don, sous la rangée dismiss/close. */
    private int donateRowY() {
        return topRowY() + BTN_H + DONATE_ROW_GAP;
    }

    /** Bouton de don : palette dorée/parchemin, bordure claire/sombre, glyphe berries. */
    private void drawDonateButton(GuiGraphicsExtractor ctx, int x, int y, int w, String label, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + BTN_H;
        int fill = hover ? DONATE_FILL_HOVER : DONATE_FILL;
        int textColor = hover ? DONATE_TEXT_HOVER : DONATE_TEXT;

        ctx.fill(x, y, x + w, y + BTN_H, fill);
        ctx.fill(x, y, x + w, y + 1, DONATE_BORDER_LIGHT);
        ctx.fill(x, y + BTN_H - 1, x + w, y + BTN_H, DONATE_BORDER_DARK);
        ctx.fill(x, y, x + 1, y + BTN_H, DONATE_BORDER_LIGHT);
        ctx.fill(x + w - 1, y, x + w, y + BTN_H, DONATE_BORDER_DARK);

        Font tr = Minecraft.getInstance().font;
        Component text = Component.literal(label + " ").append(Component.literal(BERRY)
                .withStyle(s -> s.withFont(new FontDescription.Resource(BERRY_FONT))));
        int tw = tr.width(text);
        ctx.text(tr, text, x + (w - tw) / 2, y + (BTN_H - tr.lineHeight) / 2, textColor, false);
    }

    private void drawButton(GuiGraphicsExtractor ctx, int x, int y, int w, String label, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + BTN_H;
        ctx.fill(x, y, x + w, y + BTN_H, hover ? 0xFF5A4632 : 0xFF3A2A1C);
        ctx.fill(x, y, x + w, y + 1, 0xFF8A6A44);
        ctx.fill(x, y + BTN_H - 1, x + w, y + BTN_H, 0xFF8A6A44);
        RenderUtils.drawCenteredText(ctx, label, x + w / 2, y + 6, 0xFFFFE9D5);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
            if (pages.size() > 1) {
                if (inside(click, left + 12, navRowY(), NAV_BTN_W)) { turnPage(-1); return true; }
                if (inside(click, left + PANEL_W - 12 - NAV_BTN_W, navRowY(), NAV_BTN_W)) { turnPage(1); return true; }
            }
            int btnY = topRowY();
            if (inside(click, left + 12, btnY, 140)) {
                var cfg = MinepieceEssentialsClient.getInstance().getConfigManager();
                cfg.config().helpDismissed = true;
                cfg.save();
                onClose();
                return true;
            }
            if (inside(click, left + PANEL_W - 12 - 90, btnY, 90)) {
                onClose();
                return true;
            }
            // Le don reste gate sur MinePiece à la fois pour l'affichage (render) et le clic.
            if (ServerDetector.isOnMinePiece() && inside(click, left + 12, donateRowY(), PANEL_W - 24)) {
                Minecraft.getInstance().setScreenAndShow(new DonateScreen(this));
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean inside(MouseButtonEvent click, int x, int y, int w) {
        return click.x() >= x && click.x() <= x + w && click.y() >= y && click.y() <= y + BTN_H;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent key) {
        if (key.input() == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) { turnPage(-1); return true; }
        if (key.input() == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) { turnPage(1); return true; }
        return super.keyPressed(key);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (dy != 0) { turnPage(dy < 0 ? 1 : -1); return true; }
        return super.mouseScrolled(mouseX, mouseY, dx, dy);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
