package com.minepiece.essentials.job;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.config.ModConfig;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Live HUD for the active job, fed by {@link JobTracker} from the action bar.
 * Shows the level, an XP progress bar with the percentage, the exact current/
 * needed XP and the remaining XP. Display only — no estimation.
 */
public class JobHud extends HudElement {

    private static final int WIDTH = 130;
    private static final int TITLE_COLOR = 0xFFFFD27F;
    private static final int TEXT_COLOR = 0xFFE9D5C7;
    private static final int SUB_COLOR = 0xFFCBC8C7;
    private static final int BAR_COLOR = 0xFF7CFC55;

    public JobHud() {
        super("job_progress", 5, 250, WIDTH, 62);
    }

    @Override
    public void tick() {}

    @Override
    public void render(DrawContext ctx, float tickDelta) {
        if (!config().jobHudEnabled) return;

        if (!JobTracker.hasData()) {
            int h = 32;
            this.height = h;
            ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, Text.translatable("minepiece.ui.job.title").getString(), getBackground());
            RenderUtils.drawText(ctx, Text.translatable("minepiece.ui.job.harvest_hint").getString(), 8, 20, SUB_COLOR);
            return;
        }

        int h = 62;
        this.height = h;
        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, Text.translatable("minepiece.ui.job.title").getString(), getBackground());

        float p = JobTracker.progress();

        RenderUtils.drawText(ctx, "Niveau " + JobTracker.level(), 8, 20, TITLE_COLOR);
        String pct = String.format(Locale.US, "%.1f%%", p * 100);
        RenderUtils.drawText(ctx, pct, WIDTH - 6 - RenderUtils.textWidth(pct), 20, SUB_COLOR);

        RenderUtils.drawProgressBar(ctx, 8, 31, WIDTH - 16, 6, p, BAR_COLOR);

        String xp = group(JobTracker.current()) + " / " + group(JobTracker.needed());
        RenderUtils.drawText(ctx, xp, 8, 41, TEXT_COLOR);

        String rem = "reste " + group(JobTracker.remaining()) + " XP";
        RenderUtils.drawText(ctx, rem, 8, 51, SUB_COLOR);
    }

    private static ModConfig config() {
        return MinepieceEssentialsClient.getInstance().getConfigManager().config();
    }

    /** Group an integer's digits in threes with a space, e.g. 27142 → "27 142". */
    private static String group(long n) {
        String s = Long.toString(n);
        StringBuilder b = new StringBuilder();
        int c = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            b.append(s.charAt(i));
            if (++c % 3 == 0 && i > 0) b.append(' ');
        }
        return b.reverse().toString();
    }
}
