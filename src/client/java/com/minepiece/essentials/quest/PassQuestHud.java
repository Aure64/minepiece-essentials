package com.minepiece.essentials.quest;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** HUD listing the daily pass quests with their objective and progress. */
public class PassQuestHud extends HudElement {

    private static final int WIDTH = 178;
    private static final int LINE_HEIGHT = 12;
    private static final int DONE_COLOR = 0xFF7CFC55;

    public PassQuestHud() {
        super("pass_quests", 5, 360, WIDTH, 60);
    }

    @Override
    public void tick() {
        PassQuestScanner.tick();
    }

    @Override
    public void render(DrawContext ctx, float tickDelta) {
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().passQuestHudEnabled) {
            return;
        }

        List<PassQuest> quests = PassQuestState.get();
        if (quests.isEmpty()) {
            this.height = 32;
            ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, 32, "Quêtes du jour", getBackground());
            RenderUtils.drawText(ctx, "Ouvre /pass, onglet Quêtes", 6, 20, getBackground().textColor());
            return;
        }

        int h = 20 + quests.size() * LINE_HEIGHT + 4;
        this.height = h;
        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, "Quêtes du jour", getBackground());

        int y = 20;
        for (PassQuest q : quests) {
            boolean done = q.isComplete();
            int color = done ? DONE_COLOR : q.difficulty().color();
            String prog = q.current() + "/" + q.target();
            int progW = RenderUtils.textWidth(prog);

            String obj = fit(q.objective(), WIDTH - 12 - progW - 4);
            RenderUtils.drawText(ctx, obj, 6, y, color);
            RenderUtils.drawText(ctx, prog, WIDTH - 6 - progW, y, color);

            y += LINE_HEIGHT;
        }
    }

    /** Truncates the text with a trailing ".." so it fits within {@code maxWidth} px. */
    private static String fit(String text, int maxWidth) {
        if (RenderUtils.textWidth(text) <= maxWidth) return text;
        String s = text;
        while (s.length() > 1 && RenderUtils.textWidth(s + "..") > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "..";
    }
}
