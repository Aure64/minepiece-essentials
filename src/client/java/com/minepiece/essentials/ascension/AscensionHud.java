package com.minepiece.essentials.ascension;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.ItemText;
import com.minepiece.essentials.util.RenderUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.minepiece.essentials.util.StackFingerprint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * HUD listing the player's ascendable fruits and weapons with their level, and
 * highlighting the ones whose next ascension is available. Reads straight from
 * the player inventory, so it stays live without opening any screen.
 */
public class AscensionHud extends HudElement {

    @Override
    public boolean isEnabled() {
        return MinepieceEssentialsClient.getInstance().getConfigManager().config().ascensionHudEnabled;
    }

    private static final int WIDTH = 188;
    private static final int SCAN_INTERVAL = 10; // ticks
    private static final int HEADER_COLOR = 0xFFFFAA00;
    private static final int NAME_COLOR = 0xFFE9D5C7;
    private static final int LEVEL_COLOR = 0xFFCBC8C7;
    private static final int READY_COLOR = 0xFF7CFC55;

    /** An ascendable item with the stack to draw as icon. */
    private record Row(AscensionItem item, ItemStack stack) {}

    private static final int ROW = 12;
    private static final int ICON = 12;

    private List<Row> items = List.of();
    private int ticks;
    private long lastFingerprint;
    private boolean hasFruit;
    private boolean hasWeapon;
    private boolean hasTool;

    public AscensionHud() {
        super("ascensions", 5, 360, WIDTH, 60);
    }

    @Override
    public void tick() {
        if (ticks++ % SCAN_INTERVAL != 0) return;
        LocalPlayer player = Minecraft.getInstance().player;
        long fp = player == null ? 0 : StackFingerprint.of(player.getInventory());
        if (fp == lastFingerprint) return; // inventaire inchangé
        lastFingerprint = fp;
        items = player == null ? List.of() : scan(player);

        // Precompute the section flags once per scan instead of streaming twice
        // every render frame.
        boolean f = false, w = false, t = false;
        for (Row r : items) {
            switch (r.item().type()) {
                case FRUIT -> f = true;
                case WEAPON -> w = true;
                case TOOL -> t = true;
            }
        }
        hasFruit = f;
        hasWeapon = w;
        hasTool = t;
    }

    private static List<Row> scan(LocalPlayer player) {
        List<Row> fruits = new ArrayList<>();
        List<Row> weapons = new ArrayList<>();
        List<Row> tools = new ArrayList<>();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null) continue;
            Optional<AscensionItem> parsed = AscensionParser.parse(stack.getHoverName().getString(), data.copyTag().toString());
            if (parsed.isEmpty()) parsed = EvoToolParser.parse(ItemText.nameAndLore(stack));
            parsed.ifPresent(item -> {
                int detected = colorOf(stack.getHoverName());
                AscensionItem coloured = detected != 0 ? item.withColor(detected) : item;
                Row row = new Row(coloured, stack.copy());
                switch (coloured.type()) {
                    case FRUIT -> fruits.add(row);
                    case WEAPON -> weapons.add(row);
                    case TOOL -> tools.add(row);
                }
            });
        }
        List<Row> all = new ArrayList<>(fruits);
        all.addAll(weapons);
        all.addAll(tools);
        return all;
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, float tickDelta) {
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().ascensionHudEnabled) {
            return;
        }
        List<Row> snapshot = items;
        if (snapshot.isEmpty()) return;

        boolean mirror = isMirrored();
        boolean fruitHeaderDrawn = false;
        boolean weaponHeaderDrawn = false;
        boolean toolHeaderDrawn = false;
        int headers = (hasFruit ? 1 : 0) + (hasWeapon ? 1 : 0) + (hasTool ? 1 : 0);
        int h = 20 + snapshot.size() * ROW + headers * 10 + 4;
        this.height = h;

        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, Component.translatable("minepiece.ui.ascensions.title").getString(), getBackground());

        int y = 20;
        for (Row row : snapshot) {
            AscensionItem item = row.item();
            String header = null;
            if (item.type() == AscensionItem.Type.FRUIT && !fruitHeaderDrawn) {
                header = Component.translatable("minepiece.ui.ascensions.fruits").getString();
                fruitHeaderDrawn = true;
            } else if (item.type() == AscensionItem.Type.WEAPON && !weaponHeaderDrawn) {
                header = Component.translatable("minepiece.ui.ascensions.weapons").getString();
                weaponHeaderDrawn = true;
            } else if (item.type() == AscensionItem.Type.TOOL && !toolHeaderDrawn) {
                header = Component.translatable("minepiece.ui.ascensions.tools").getString();
                toolHeaderDrawn = true;
            }
            if (header != null) {
                if (mirror) RenderUtils.drawText(ctx, header, WIDTH - 6 - RenderUtils.textWidth(header), y, HEADER_COLOR);
                else RenderUtils.drawText(ctx, header, 6, y, HEADER_COLOR);
                y += 10;
            }

            String right;
            if (item.type() == AscensionItem.Type.TOOL) {
                right = item.available() ? item.maxXp() + " ▲" : item.xp() + "/" + item.maxXp();
            } else {
                right = item.available()
                    ? "Lv" + item.level() + " ▲"
                    : "Lv" + item.level() + "  " + item.xpToNextLevel() + "xp";
            }
            int rightColor = item.available() ? READY_COLOR : LEVEL_COLOR;
            int ty = y + 1;
            if (mirror) {
                RenderUtils.drawText(ctx, right, 6, ty, rightColor);
                int textEnd = WIDTH - 6 - ICON - 4;
                RenderUtils.drawItemIcon(ctx, row.stack(), WIDTH - 6 - ICON, y - 2, ICON);
                int budget = textEnd - (6 + RenderUtils.textWidth(right)) - 6;
                RenderUtils.drawTextFitRight(ctx, truncate(item.name(), budget), textEnd, ty, budget, item.color());
            } else {
                int rightX = WIDTH - 6 - RenderUtils.textWidth(right);
                RenderUtils.drawText(ctx, right, rightX, ty, rightColor);
                RenderUtils.drawItemIcon(ctx, row.stack(), 6, y - 2, ICON);
                int x = 6 + ICON + 4;
                RenderUtils.drawText(ctx, truncate(item.name(), rightX - 6 - x), x, ty, item.color());
            }
            y += ROW;
        }
    }

    /** First explicit colour in the item's name Text, as ARGB; 0 if none (use the rarity fallback). */
    private static int colorOf(Component text) {
        int[] found = {0};
        text.visit((style, str) -> {
            TextColor c = style.getColor();
            if (c != null && found[0] == 0) found[0] = 0xFF000000 | c.getValue();
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static String truncate(String name, int budget) {
        if (RenderUtils.textWidth(name) <= budget) return name;
        String s = name;
        while (s.length() > 1 && RenderUtils.textWidth(s + "..") > budget) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "..";
    }
}
