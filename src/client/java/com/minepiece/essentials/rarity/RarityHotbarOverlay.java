package com.minepiece.essentials.rarity;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.util.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameMode;

/** Emblèmes de rareté dessinés sur la hotbar pendant le jeu (aucun écran ouvert). */
public final class RarityHotbarOverlay {
    private RarityHotbarOverlay() {}

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
    }

    private static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if (!ServerDetector.isOnMinePiece()) return;
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().rarityHotbarEnabled) return;
        if (mc.interactionManager != null
                && mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) return;

        // Géométrie vanilla de la hotbar : largeur 182, slots de 20px, marge interne 3.
        int left = ctx.getScaledWindowWidth() / 2 - 91;
        int top = ctx.getScaledWindowHeight() - 22 + 3;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack st = inv.getStack(i);
            if (st.isEmpty()) continue;
            ItemRarity r = RarityDetector.detect(st);
            if (r == null) continue;
            int sx = left + 3 + i * 20;
            float scale = 8f / Math.max(r.nativeW, r.nativeH);
            RenderUtils.drawTextureScaled(ctx, r.texture(), sx, top, scale, r.nativeW, r.nativeH);
        }
    }
}
