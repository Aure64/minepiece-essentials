package com.minepiece.essentials.pet;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;

import java.util.Map;

/** HUD panel listing the active pets and the total combat stats they grant. */
public class ActivePetsHud extends HudElement {

    private static final int WIDTH = 150;
    private static final int NAME_COLOR = 0xFFFFE9D5;

    public ActivePetsHud() {
        super("active_pets", 5, 120, WIDTH, 60);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta) {
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().petPanelEnabled) {
            return;
        }
        ActivePetsState.Snapshot snap = ActivePetsState.get();
        if (snap.isEmpty()) return;

        int names = snap.petNames().size();
        int stats = snap.totals().size();
        int h = 20 + names * 10 + 6 + stats * 10 + 4;
        this.height = h;

        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, "Pets actifs (" + names + ")");

        int y = 20;
        for (String name : snap.petNames()) {
            RenderUtils.drawText(ctx, truncate(name), 6, y, NAME_COLOR);
            y += 10;
        }

        y += 6;
        for (PetStat stat : PetStat.values()) {
            Double value = snap.totals().get(stat);
            if (value == null || value == 0.0) continue;
            RenderUtils.drawText(ctx, stat.fr(), 6, y, stat.color());
            String v = "+" + fmt(value);
            RenderUtils.drawText(ctx, v, WIDTH - 6 - RenderUtils.textWidth(v), y, stat.color());
            y += 10;
        }
    }

    @Override
    public void tick() {
        ActivePetsScanner.tick();
    }

    private static String truncate(String name) {
        if (RenderUtils.textWidth(name) <= WIDTH - 12) return name;
        String s = name;
        while (s.length() > 1 && RenderUtils.textWidth(s + "..") > WIDTH - 12) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "..";
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) return Long.toString(Math.round(v));
        return String.format(java.util.Locale.US, "%.1f", v);
    }
}
