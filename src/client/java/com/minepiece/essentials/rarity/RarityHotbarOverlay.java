package com.minepiece.essentials.rarity;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.util.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Emblèmes de rareté dessinés sur la hotbar pendant le jeu (aucun écran ouvert). */
public final class RarityHotbarOverlay {
    private RarityHotbarOverlay() {}

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
    }

    private static void render(GuiGraphics ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!ServerDetector.isOnMinePiece()) return;
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().rarityHotbarEnabled) return;
        if (mc.gameMode != null
                && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        // Géométrie vanilla de la hotbar : largeur 182, slots de 20px, marge interne 3.
        int left = ctx.guiWidth() / 2 - 91;
        int top = ctx.guiHeight() - 22 + 3;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            ItemRarity r = RarityDetector.detect(st);
            if (r == null) continue;
            int sx = left + 3 + i * 20;
            float scale = 8f / Math.max(r.nativeW, r.nativeH);
            RenderUtils.drawTextureScaled(ctx, r.texture(), sx, top, scale, r.nativeW, r.nativeH);
        }
    }
}
