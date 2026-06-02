package com.minepiece.essentials.ascension;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HUD listing the player's ascendable fruits and weapons with their level, and
 * highlighting the ones whose next ascension is available. Reads straight from
 * the player inventory, so it stays live without opening any screen.
 */
public class AscensionHud extends HudElement {

    private static final int WIDTH = 188;
    private static final int SCAN_INTERVAL = 10; // ticks
    private static final int HEADER_COLOR = 0xFFFFAA00;
    private static final int NAME_COLOR = 0xFFE9D5C7;
    private static final int LEVEL_COLOR = 0xFFCBC8C7;
    private static final int READY_COLOR = 0xFF7CFC55;

    private List<AscensionItem> items = List.of();
    private int ticks;
    private boolean hasFruit;
    private boolean hasWeapon;

    public AscensionHud() {
        super("ascensions", 5, 360, WIDTH, 60);
    }

    @Override
    public void tick() {
        if (ticks++ % SCAN_INTERVAL != 0) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        items = player == null ? List.of() : scan(player);

        // Precompute the section flags once per scan instead of streaming twice
        // every render frame.
        boolean f = false, w = false;
        for (AscensionItem it : items) {
            if (it.type() == AscensionItem.Type.FRUIT) f = true;
            else w = true;
            if (f && w) break;
        }
        hasFruit = f;
        hasWeapon = w;
    }

    private static List<AscensionItem> scan(ClientPlayerEntity player) {
        List<AscensionItem> fruits = new ArrayList<>();
        List<AscensionItem> weapons = new ArrayList<>();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (data == null) continue;
            AscensionParser.parse(stack.getName().getString(), data.copyNbt().toString())
                .ifPresent(item -> {
                    int detected = colorOf(stack.getName());
                    AscensionItem coloured = detected != 0 ? item.withColor(detected) : item;
                    (coloured.type() == AscensionItem.Type.FRUIT ? fruits : weapons).add(coloured);
                });
        }
        List<AscensionItem> all = new ArrayList<>(fruits);
        all.addAll(weapons);
        return all;
    }

    @Override
    public void render(DrawContext ctx, float tickDelta) {
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().ascensionHudEnabled) {
            return;
        }
        List<AscensionItem> snapshot = items;
        if (snapshot.isEmpty()) return;

        boolean fruitHeaderDrawn = false;
        boolean weaponHeaderDrawn = false;
        int headers = (hasFruit ? 1 : 0) + (hasWeapon ? 1 : 0);
        int h = 20 + (snapshot.size() + headers) * 10 + 4;
        this.height = h;

        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, "Ascensions", getBackground());

        int y = 20;
        for (AscensionItem item : snapshot) {
            if (item.type() == AscensionItem.Type.FRUIT && !fruitHeaderDrawn) {
                RenderUtils.drawText(ctx, "Fruits", 6, y, HEADER_COLOR);
                fruitHeaderDrawn = true;
                y += 10;
            } else if (item.type() == AscensionItem.Type.WEAPON && !weaponHeaderDrawn) {
                RenderUtils.drawText(ctx, "Armes", 6, y, HEADER_COLOR);
                weaponHeaderDrawn = true;
                y += 10;
            }

            String right = item.available()
                ? "Lv" + item.level() + " ▲"
                : "Lv" + item.level() + "  " + item.xpToNextLevel() + "xp";
            int rightX = WIDTH - 6 - RenderUtils.textWidth(right);
            RenderUtils.drawText(ctx, right, rightX, y, item.available() ? READY_COLOR : LEVEL_COLOR);

            RenderUtils.drawText(ctx, truncate(item.name(), rightX - 8), 8, y, item.color());
            y += 10;
        }
    }

    /** First explicit colour in the item's name Text, as ARGB; 0 if none (use the rarity fallback). */
    private static int colorOf(Text text) {
        int[] found = {0};
        text.visit((style, str) -> {
            TextColor c = style.getColor();
            if (c != null && found[0] == 0) found[0] = 0xFF000000 | c.getRgb();
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
