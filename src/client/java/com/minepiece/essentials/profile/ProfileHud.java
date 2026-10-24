package com.minepiece.essentials.profile;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.armor.ArmorState;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.HudOption;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.pet.ActivePetsState;
import com.minepiece.essentials.pet.PetStat;
import com.minepiece.essentials.util.RenderUtils;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * HUD panel with the player's global stats from {@code /profil}, each followed by the
 * share granted by the active pets and by the worn armor: {@code Health  1697 (+0 | +440)}.
 */
public class ProfileHud extends HudElement {

    private static final int WIDTH = 200;
    private static final int GRAY = 0xFFCBC8C7;
    private static final int PETS_COLOR = 0xFFF0B4E0;
    private static final int ARMOR_COLOR = 0xFF9CE07A;
    private static final int DAMAGE_COLOR = 0xFFE0584C;

    @Override
    public boolean supportsMirror() { return true; }

    static final String OPT_LEVEL = "level", OPT_PARTS = "parts", OPT_NONZERO = "nonzero";

    @Override
    public java.util.List<HudOption> options() {
        return java.util.List.of(
                HudOption.layout(this, OPT_LEVEL, "minepiece.ui.option.profile.level", true),
                HudOption.layout(this, OPT_PARTS, "minepiece.ui.option.profile.parts", true),
                HudOption.layout(this, OPT_NONZERO, "minepiece.ui.option.profile.nonzero", false));
    }

    public ProfileHud() {
        super("profile", 5, 300, WIDTH, 60);
    }

    @Override
    public boolean isEnabled() {
        return MinepieceEssentialsClient.getInstance().getConfigManager().config().profileHudEnabled;
    }

    @Override
    public void tick() {
        // ProfileScanner est tické par MinepieceEssentialsClient (il sert aussi aux autres panneaux).
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, float tickDelta) {
        String title = Component.translatable("minepiece.ui.profile.title").getString();
        Optional<ProfileData> data = ProfileState.get();
        if (data.isEmpty()) {
            this.height = 32;
            ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, 32, title, getBackground());
            RenderUtils.drawText(ctx, Component.translatable("minepiece.ui.profile.hint").getString(), 6, 20, GRAY);
            return;
        }
        ProfileData p = data.get();
        Map<PetStat, Double> pets = ActivePetsState.get().totals();
        Map<PetStat, Double> armor = ArmorState.get().totals();

        boolean showLevel = option(OPT_LEVEL, true);
        boolean showParts = option(OPT_PARTS, true);
        boolean nonzeroOnly = option(OPT_NONZERO, false);
        java.util.List<ProfileData.Entry> entries = new java.util.ArrayList<>();
        for (ProfileData.Entry e : p.entries()) {
            double value = e.stat() != null ? ProfileState.total(e.stat()) : e.value();
            if (nonzeroOnly && value == 0.0) continue;
            entries.add(e);
        }

        int rows = entries.size();
        int h = 20 + (showLevel ? 10 : 0) + (showParts ? 10 : 0) + rows * 10 + 4;
        this.height = h;
        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, title, getBackground());

        boolean mirror = isMirrored();
        int y = 20;
        int lvl = PlayerLevel.level() > 0 ? PlayerLevel.level() : p.level();
        double pct = PlayerLevel.level() > 0 ? PlayerLevel.percent() : p.levelPercent();
        String level = Component.translatable("minepiece.ui.profile.level", lvl,
                String.format(java.util.Locale.US, "%.1f", pct), p.ascension()).getString();
        if (showLevel) {
            if (mirror) RenderUtils.drawTextFitRight(ctx, level, WIDTH - 6, y, WIDTH - 12, GRAY);
            else RenderUtils.drawTextFit(ctx, level, 6, y, WIDTH - 12, GRAY);
            y += 10;
        }

        // Légende des parenthèses : (pets | armure), du côté des valeurs.
        String petsLabel = Component.translatable("minepiece.ui.profile.pets").getString();
        String armorLabel = Component.translatable("minepiece.ui.profile.armor").getString();
        String legend = "(" + petsLabel + " | " + armorLabel + ")";
        if (showParts) {
            int cx = mirror ? 6 : WIDTH - 6 - RenderUtils.textWidth(legend);
            cx = draw(ctx, "(", cx, y, GRAY);
            cx = draw(ctx, petsLabel, cx, y, PETS_COLOR);
            cx = draw(ctx, " | ", cx, y, GRAY);
            cx = draw(ctx, armorLabel, cx, y, ARMOR_COLOR);
            draw(ctx, ")", cx, y, GRAY);
            y += 10;
        }

        for (ProfileData.Entry e : entries) {
            PetStat stat = e.stat();
            int color = stat != null ? stat.color() : DAMAGE_COLOR;
            double value = stat != null ? ProfileState.total(stat) : e.value();
            String total = StatLine.fmt(value) + (e.percent() ? "%" : "");
            String a = stat != null ? "+" + StatLine.fmt(armor.getOrDefault(stat, 0.0)) : "";
            String pt = stat != null ? "+" + StatLine.fmt(pets.getOrDefault(stat, 0.0)) : "";
            boolean parts = showParts && stat != null;
            int valuesW = RenderUtils.textWidth(total)
                + (parts ? 4 + RenderUtils.textWidth("(" + pt + " | " + a + ")") : 0);

            int x = mirror ? 6 : WIDTH - 6 - valuesW;
            x = draw(ctx, total, x, y, color);
            if (parts) {
                x += 4;
                x = draw(ctx, "(", x, y, GRAY);
                x = draw(ctx, pt, x, y, PETS_COLOR);
                x = draw(ctx, " | ", x, y, GRAY);
                x = draw(ctx, a, x, y, ARMOR_COLOR);
                draw(ctx, ")", x, y, GRAY);
            }
            if (mirror) RenderUtils.drawTextFitRight(ctx, e.label(), WIDTH - 6, y, WIDTH - 12 - valuesW - 10, color);
            else RenderUtils.drawTextFit(ctx, e.label(), 6, y, WIDTH - 12 - valuesW - 10, color);
            y += 10;
        }
    }

    /** Draws {@code text} at x and returns the x just after it. */
    private static int draw(GuiGraphicsExtractor ctx, String text, int x, int y, int color) {
        RenderUtils.drawText(ctx, text, x, y, color);
        return x + RenderUtils.textWidth(text);
    }
}
