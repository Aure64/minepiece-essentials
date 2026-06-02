package com.minepiece.essentials.pet;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;

import java.util.Map;

/** HUD panel listing the active pets and the total combat stats they grant. */
public class ActivePetsHud extends HudElement {

    private static final int WIDTH = 170;
    private static final int LEVEL_COLOR = 0xFFCBC8C7;

    public ActivePetsHud() {
        super("active_pets", 5, 120, WIDTH, 60);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta) {
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().petPanelEnabled) {
            return;
        }
        ActivePetsState.Snapshot snap = ActivePetsState.get();
        if (snap.isEmpty()) {
            this.height = 32;
            ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, 32, "Pets actifs", getBackground());
            RenderUtils.drawText(ctx, "Ouvre /pets pour afficher", 6, 20, 0xFFCBC8C7);
            return;
        }

        int count = snap.pets().size();
        int stats = snap.totals().size();
        int h = 20 + count * 10 + 6 + stats * 10 + 4;
        this.height = h;

        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, "Pets actifs (" + count + ")", getBackground());

        int y = 20;
        for (ActivePetsState.ActivePet pet : snap.pets()) {
            String lvl = pet.level() >= 20 ? "Max" : (pet.level() > 0 ? "Lv" + pet.level() : "");
            int lvlX = WIDTH - 6 - RenderUtils.textWidth(lvl);
            RenderUtils.drawText(ctx, lvl, lvlX, y, LEVEL_COLOR);
            RenderUtils.drawText(ctx, truncate(pet.name(), lvlX - 8), 6, y, pet.color());
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

    private static String truncate(String name, int budget) {
        if (RenderUtils.textWidth(name) <= budget) return name;
        String s = name;
        while (s.length() > 1 && RenderUtils.textWidth(s + "..") > budget) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "..";
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) return Long.toString(Math.round(v));
        return String.format(java.util.Locale.US, "%.1f", v);
    }
}
