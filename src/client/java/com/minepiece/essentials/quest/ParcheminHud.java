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
        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, "Parchemins");

        int lineY = 20;
        for (var quest : parchemins) {
            int rarityColor = quest.getRarityColor();

            String obj = quest.objective;
            if (RenderUtils.textWidth(obj) > WIDTH - 12) {
                obj = obj.substring(0, Math.min(obj.length(), 20)) + "..";
            }
            RenderUtils.drawText(ctx, obj, 6, lineY, rarityColor);

            float progress = quest.progress();
            int barColor = quest.isCompleted() ? 0xFF00CC00 : rarityColor;
            RenderUtils.drawProgressBar(ctx, 6, lineY + 10, WIDTH - 50, 5, progress, barColor);

            String progText = quest.current + "/" + quest.target;
            RenderUtils.drawText(ctx, progText, WIDTH - 40, lineY + 7, rarityColor);

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
