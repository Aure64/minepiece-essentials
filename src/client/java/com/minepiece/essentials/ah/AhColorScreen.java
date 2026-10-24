package com.minepiece.essentials.ah;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.config.ConfigManager;
import com.minepiece.essentials.config.ModConfig;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Réglage des couleurs de prix de l'hôtel des ventes. Une ligne par bande
 * (moins cher / au prix / trop cher) : explication, aperçu du liseré et du texte
 * de l'infobulle tels qu'ils apparaîtront, palette cliquable, champ hexa libre.
 * Chaque changement est appliqué immédiatement (aperçu + config).
 */
public class AhColorScreen extends Screen {

    /** Palette proposée : couleurs lisibles sur fond sombre de menu. */
    private static final int[] PALETTE = {
            0xFFD24B, 0x7CFC55, 0xFF5555, 0xFFAA00, 0x55FFFF, 0x5599FF,
            0xFF55FF, 0xAA00AA, 0xFFFFFF, 0xAAAAAA, 0x00AA00, 0xAA0000,
    };
    private static final AhPriceBand.Band[] BANDS = AhPriceBand.Band.values();

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 232;
    private static final int ROW_H = 46;
    private static final int SWATCH = 12;          // taille d'une case de palette
    private static final int SWATCH_X = 178;       // relatif au panneau
    private static final int HEX_X = SWATCH_X + PALETTE.length * SWATCH + 8;
    private static final int HEX_W = 56;
    private static final int BTN_W = 120, BTN_H = 20;

    private static final int TEXT = 0xFFFFE9D5;
    private static final int TEXT_DIM = 0xFFC9B99A;
    private static final int TITLE = 0xFFF0A857;
    private static final int ERROR = 0xFFFF5555;

    private final Screen parent;
    private final Map<AhPriceBand.Band, EditBox> hexFields = new EnumMap<>(AhPriceBand.Band.class);

    private int panelX, panelY;

    public AhColorScreen(Screen parent) {
        super(Component.translatable("minepiece.ui.ahcolors.title"));
        this.parent = parent;
    }

    private static ConfigManager mgr() {
        return MinepieceEssentialsClient.getInstance().getConfigManager();
    }

    private static ModConfig cfg() {
        return mgr().config();
    }

    private int rowY(int i) {
        return panelY + 44 + i * ROW_H;
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;
        hexFields.clear();

        for (int i = 0; i < BANDS.length; i++) {
            AhPriceBand.Band band = BANDS[i];
            EditBox box = new EditBox(font, panelX + HEX_X, rowY(i) + 14, HEX_W, 16,
                    Component.translatable("minepiece.ui.ahcolors.hex"));
            box.setMaxLength(7);
            box.setValue(AhPriceColors.get(band, cfg()));
            box.setResponder(text -> {
                if (AhPriceColors.isValid(text)) {
                    AhPriceColors.set(band, text, cfg());
                    mgr().save();
                }
            });
            addRenderableWidget(box);
            hexFields.put(band, box);
        }

        int by = panelY + PANEL_H - BTN_H - 10;
        int gap = 6;
        int bx = width / 2 - (BTN_W * 3 + gap * 2) / 2;
        addRenderableWidget(Button.builder(Component.translatable("minepiece.ui.ahcolors.swap"), b -> {
            AhPriceColors.swapCheapFair(cfg());
            mgr().save();
            refreshFields();
        }).bounds(bx, by, BTN_W, BTN_H).build());
        addRenderableWidget(Button.builder(Component.translatable("minepiece.ui.ahcolors.reset"), b -> {
            AhPriceColors.resetDefaults(cfg());
            mgr().save();
            refreshFields();
        }).bounds(bx + BTN_W + gap, by, BTN_W, BTN_H).build());
        addRenderableWidget(Button.builder(Component.translatable("minepiece.ui.ahcolors.close"), b -> onClose())
                .bounds(bx + 2 * (BTN_W + gap), by, BTN_W, BTN_H).build());
    }

    private void refreshFields() {
        for (AhPriceBand.Band band : BANDS) {
            hexFields.get(band).setValue(AhPriceColors.get(band, cfg()));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
            for (int i = 0; i < BANDS.length; i++) {
                int sy = rowY(i) + 16;
                for (int p = 0; p < PALETTE.length; p++) {
                    int sx = panelX + SWATCH_X + p * SWATCH;
                    if (click.x() >= sx && click.x() < sx + SWATCH && click.y() >= sy && click.y() < sy + SWATCH) {
                        AhPriceColors.set(BANDS[i], AhPriceColors.toHex(PALETTE[p]), cfg());
                        mgr().save();
                        hexFields.get(BANDS[i]).setValue(AhPriceColors.get(BANDS[i], cfg()));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    /** Fond plat + panneau parchemin (voir DonateScreen pour le « blur once per frame »). */
    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xB0000000);
        ParchmentRenderer.renderPanel(ctx, panelX, panelY, PANEL_W, PANEL_H, null);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        ctx.centeredText(font, title, cx, panelY + 10, TITLE);
        ctx.centeredText(font, Component.translatable("minepiece.ui.ahcolors.subtitle").getString(),
                cx, panelY + 24, TEXT_DIM);

        ModConfig c = cfg();
        for (int i = 0; i < BANDS.length; i++) {
            AhPriceBand.Band band = BANDS[i];
            int y = rowY(i);
            int color = AhPriceColors.of(band, c);
            String key = "minepiece.ui.ahcolors." + band.name().toLowerCase(java.util.Locale.ROOT);

            // Ligne 1 : nom de la bande + explication.
            RenderUtils.drawText(ctx, Component.translatable(key).getString(), panelX + 10, y, TEXT);
            RenderUtils.drawText(ctx, Component.translatable(key + ".desc").getString(),
                    panelX + 10 + RenderUtils.textWidth(Component.translatable(key).getString()) + 6, y, TEXT_DIM);

            // Ligne 2 : aperçu = faux slot avec liseré + ligne d'infobulle, comme dans l'AH.
            int sx = panelX + 12, sy = y + 14;
            ctx.fill(sx, sy, sx + 16, sy + 16, 0xFF2B2B2B);
            ctx.fill(sx + 3, sy + 3, sx + 13, sy + 13, 0xFF6F5A3C);
            drawPriceBorder(ctx, sx, sy, color);
            RenderUtils.drawText(ctx, previewText(band), sx + 24, sy + 4, color);

            // Palette : la case correspondant à la couleur courante est encadrée.
            int py = y + 16;
            for (int p = 0; p < PALETTE.length; p++) {
                int px = panelX + SWATCH_X + p * SWATCH;
                boolean selected = (color & 0xFFFFFF) == PALETTE[p];
                boolean hover = mouseX >= px && mouseX < px + SWATCH && mouseY >= py && mouseY < py + SWATCH;
                ctx.fill(px, py, px + SWATCH, py + SWATCH, selected ? 0xFFFFFFFF : hover ? 0xFFC9B99A : 0xFF1A1A1A);
                ctx.fill(px + 1, py + 1, px + SWATCH - 1, py + SWATCH - 1, 0xFF000000 | PALETTE[p]);
            }

            // Champ hexa : signale une valeur illisible.
            EditBox box = hexFields.get(band);
            if (box != null && !AhPriceColors.isValid(box.getValue())) {
                RenderUtils.drawText(ctx, Component.translatable("minepiece.ui.ahcolors.invalid").getString(),
                        panelX + HEX_X, y + 32, ERROR);
            }
        }
    }

    /** Même format que la ligne ajoutée à l'infobulle par AhTooltip. */
    private static String previewText(AhPriceBand.Band band) {
        return switch (band) {
            case CHEAP -> "▼ -15 % vs moyenne";
            case FAIR -> "● 0 % vs moyenne";
            case EXPENSIVE -> "▲ +15 % vs moyenne";
        };
    }

    private static void drawPriceBorder(GuiGraphicsExtractor ctx, int x, int y, int color) {
        ctx.fill(x - 1, y - 1, x + 17, y, color);
        ctx.fill(x - 1, y + 16, x + 17, y + 17, color);
        ctx.fill(x - 1, y, x, y + 16, color);
        ctx.fill(x + 16, y, x + 17, y + 16, color);
    }

    @Override
    public void onClose() {
        mgr().flush();
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
