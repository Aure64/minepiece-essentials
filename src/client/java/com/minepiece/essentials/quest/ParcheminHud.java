package com.minepiece.essentials.quest;

import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.ColorUtils;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import java.util.Comparator;
import java.util.List;

public class ParcheminHud extends HudElement {
    private static final int WIDTH = 170;
    private final ParcheminScanner parcheminScanner = new ParcheminScanner();

    // Sorted snapshot, rebuilt in tick() (20×/s) instead of every render frame.
    private List<ParcheminScanner.QuestInfo> sorted = List.of();

    public ParcheminHud() {
        super("parchemins", 5, 300, WIDTH, 100);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta) {
        List<ParcheminScanner.QuestInfo> parchemins = sorted;
        if (parchemins.isEmpty()) return;

        int h = 20 + parchemins.size() * 24 + 4;
        this.height = h;
        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, "Parchemins", getBackground());

        int lineY = 20;
        for (var quest : parchemins) {
            int rarityColor = quest.getRarityColor();

            // Counter right-aligned on the objective line; the objective fills the
            // space to its left, shrinking to fit so it never overlaps the counter.
            String progText = quest.current + "/" + quest.target;
            int progX = WIDTH - 6 - RenderUtils.textWidth(progText);
            RenderUtils.drawText(ctx, progText, progX, lineY, rarityColor);
            RenderUtils.drawTextFit(ctx, quest.objective, 6, lineY, progX - 10, rarityColor);

            float progress = quest.progress();
            int barColor = quest.isCompleted() ? 0xFF00CC00 : rarityColor;
            RenderUtils.drawProgressBar(ctx, 6, lineY + 10, WIDTH - 12, 5, progress, barColor);

            lineY += 24;
        }
    }

    @Override
    public void tick() {
        parcheminScanner.tick();
        List<ParcheminScanner.QuestInfo> active = parcheminScanner.getActiveQuests();
        sorted = active.isEmpty() ? List.of() : active.stream()
                .sorted(Comparator.comparingInt(ParcheminScanner.QuestInfo::estimateRemainingSeconds))
                .toList();
    }
}
