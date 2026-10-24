package com.minepiece.essentials.profile;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.armor.ArmorState;
import com.minepiece.essentials.hud.HudElement;
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

        int rows = p.entries().size();
        int h = 20 + 10 + 10 + rows * 10 + 4;
        this.height = h;
        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, title, getBackground());

        int y = 20;
        int lvl = PlayerLevel.level() > 0 ? PlayerLevel.level() : p.level();
        double pct = PlayerLevel.level() > 0 ? PlayerLevel.percent() : p.levelPercent();
        String level = Component.translatable("minepiece.ui.profile.level", lvl,
                String.format(java.util.Locale.US, "%.1f", pct), p.ascension()).getString();
        RenderUtils.drawTextFit(ctx, level, 6, y, WIDTH - 12, GRAY);
        y += 10;

        // Légende des parenthèses : (pets | armure)
        String legend = "(" + Component.translatable("minepiece.ui.profile.pets").getString()
                + " | " + Component.translatable("minepiece.ui.profile.armor").getString() + ")";
        int lw = RenderUtils.textWidth(legend);
        int lx = WIDTH - 6 - lw;
        RenderUtils.drawText(ctx, "(", lx, y, GRAY);
        int cx = lx + RenderUtils.textWidth("(");
        String petsLabel = Component.translatable("minepiece.ui.profile.pets").getString();
        RenderUtils.drawText(ctx, petsLabel, cx, y, PETS_COLOR);
        cx += RenderUtils.textWidth(petsLabel);
        RenderUtils.drawText(ctx, " | ", cx, y, GRAY);
        cx += RenderUtils.textWidth(" | ");
        String armorLabel = Component.translatable("minepiece.ui.profile.armor").getString();
        RenderUtils.drawText(ctx, armorLabel, cx, y, ARMOR_COLOR);
        cx += RenderUtils.textWidth(armorLabel);
        RenderUtils.drawText(ctx, ")", cx, y, GRAY);
        y += 10;

        for (ProfileData.Entry e : p.entries()) {
            PetStat stat = e.stat();
            int color = stat != null ? stat.color() : DAMAGE_COLOR;
            String total = StatLine.fmt(e.value()) + (e.percent() ? "%" : "");

            // Right part, drawn right-to-left: ")" armor " | " pets "(" then the total.
            int x = WIDTH - 6;
            if (stat != null) {
                String a = "+" + StatLine.fmt(armor.getOrDefault(stat, 0.0));
                String pt = "+" + StatLine.fmt(pets.getOrDefault(stat, 0.0));
                x -= RenderUtils.textWidth(")"); RenderUtils.drawText(ctx, ")", x, y, GRAY);
                x -= RenderUtils.textWidth(a); RenderUtils.drawText(ctx, a, x, y, ARMOR_COLOR);
                x -= RenderUtils.textWidth(" | "); RenderUtils.drawText(ctx, " | ", x, y, GRAY);
                x -= RenderUtils.textWidth(pt); RenderUtils.drawText(ctx, pt, x, y, PETS_COLOR);
                x -= RenderUtils.textWidth("("); RenderUtils.drawText(ctx, "(", x, y, GRAY);
                x -= 4;
            }
            x -= RenderUtils.textWidth(total);
            RenderUtils.drawText(ctx, total, x, y, color);
            RenderUtils.drawTextFit(ctx, e.label(), 6, y, x - 10, color);
            y += 10;
        }
    }
}
