package com.minepiece.essentials.help;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Book-style help popup explaining the mod's features and how to use them.
 * Required actions are highlighted in red. Shown automatically on first launch
 * (until dismissed) and reopenable with the H key.
 */
public class HelpScreen extends Screen {

    private static final int PANEL_W = 350;
    private static final int HEADER_H = 24;
    private static final int FOOTER_H = 40;
    private static final int TITLE_H = 11;
    private static final int LINE_H = 9;
    private static final int FEATURE_GAP = 5;
    private static final int BTN_H = 20;

    private static final int GOLD = 0xFFFFD27F;
    private static final int GRAY = 0xFFCBC8C7;
    private static final int RED = 0xFFFF6B6B;

    private record Line(String text, boolean important) {}

    private record Feature(String title, Line[] lines) {}

    private static Line info(String t) {
        return new Line(t, false);
    }

    private static Line important(String t) {
        return new Line(t, true);
    }

    private static final Feature[] FEATURES = {
        new Feature("Boss Timers", new Line[]{
            info("Respawns par île, alertes sonores & waypoints."),
            important("Clique le bouton Refresh pour actualiser les timers."),
            important("Ne bouge pas pendant un « Refresh All » !"),
        }),
        new Feature("Parchemins", new Line[]{
            info("Objectifs de quêtes affichés en HUD (scan auto)."),
        }),
        new Feature("Qualité des pets", new Line[]{
            info("Affiche le % de roll de chaque stat dans le tooltip."),
            important("Ouvre /pets et survole un pet pour le voir."),
        }),
        new Feature("Calculateur de minions", new Line[]{
            info("Stacks restants pour max, dans le tooltip du minion."),
            important("Ouvre l'écran de nourrissage une fois par ressource"),
            important("(le mod apprend l'XP par unité)."),
        }),
        new Feature("Panneau pets actifs", new Line[]{
            info("Total des stats données par tes pets actifs."),
            important("Fais /pets une fois pour remplir le panneau."),
        }),
        new Feature("Éditeur de HUD", new Line[]{
            important("Touche K"),
            info("pour déplacer/redimensionner les panneaux."),
        }),
        new Feature("Cette aide", new Line[]{
            info("Touche H pour rouvrir ce guide à tout moment."),
        }),
    };

    private static final int PANEL_H;

    static {
        int h = HEADER_H;
        for (Feature f : FEATURES) {
            h += TITLE_H + f.lines().length * LINE_H + FEATURE_GAP;
        }
        PANEL_H = h + FOOTER_H;
    }

    private int left;
    private int top;

    public HelpScreen() {
        super(Text.literal("Minepiece Essentials — Aide"));
    }

    @Override
    protected void init() {
        left = (this.width - PANEL_W) / 2;
        top = (this.height - PANEL_H) / 2;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xB0000000);
        ParchmentRenderer.renderPanel(ctx, left, top, PANEL_W, PANEL_H, "Minepiece Essentials");

        int y = top + HEADER_H;
        for (Feature f : FEATURES) {
            RenderUtils.drawText(ctx, "» " + f.title(), left + 12, y, GOLD);
            y += TITLE_H;
            for (Line line : f.lines()) {
                RenderUtils.drawText(ctx, line.text(), left + 20, y, line.important() ? RED : GRAY);
                y += LINE_H;
            }
            y += FEATURE_GAP;
        }

        int btnY = top + PANEL_H - 28;
        drawButton(ctx, left + 12, btnY, 150, "Ne plus afficher", mouseX, mouseY);
        drawButton(ctx, left + PANEL_W - 12 - 100, btnY, 100, "Fermer", mouseX, mouseY);
    }

    private void drawButton(DrawContext ctx, int x, int y, int w, String label, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + BTN_H;
        ctx.fill(x, y, x + w, y + BTN_H, hover ? 0xFF5A4632 : 0xFF3A2A1C);
        ctx.fill(x, y, x + w, y + 1, 0xFF8A6A44);
        ctx.fill(x, y + BTN_H - 1, x + w, y + BTN_H, 0xFF8A6A44);
        RenderUtils.drawCenteredText(ctx, label, x + w / 2, y + 6, 0xFFFFE9D5);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            int btnY = top + PANEL_H - 28;
            if (inside(click, left + 12, btnY, 150)) {
                var cfg = MinepieceEssentialsClient.getInstance().getConfigManager();
                cfg.config().helpDismissed = true;
                cfg.save();
                close();
                return true;
            }
            if (inside(click, left + PANEL_W - 12 - 100, btnY, 100)) {
                close();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean inside(Click click, int x, int y, int w) {
        return click.x() >= x && click.x() <= x + w && click.y() >= y && click.y() <= y + BTN_H;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
