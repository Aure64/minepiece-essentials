package com.minepiece.essentials.mixin;

import com.minepiece.essentials.rarity.RarityScreenOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dessine l'overlay rareté juste AVANT {@code drawDeferredElements()} dans
 * {@code Screen.renderWithTooltip}. Ainsi nos emblèmes/barre s'affichent au-dessus des
 * items mais EN DESSOUS de l'infobulle serveur (vidée ensuite par drawDeferredElements).
 *
 * <p>On cible la classe de base {@code Screen} (et non HandledScreen.render) pour couvrir
 * aussi l'inventaire joueur (E), dont la sous-classe court-circuiterait un inject sur render.
 */
@Mixin(Screen.class)
public class ScreenRenderMixin {

    @Inject(
        method = "renderWithTooltip",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/gui/DrawContext;drawDeferredElements()V"))
    private void minepiece$renderRarityOverlay(DrawContext ctx, int mouseX, int mouseY,
                                               float delta, CallbackInfo ci) {
        if (!((Object) this instanceof HandledScreen<?> hs)) return;
        HandledScreenAccessor acc = (HandledScreenAccessor) hs;
        RarityScreenOverlay.render(hs, ctx, acc.minepiece$getBgX(), acc.minepiece$getBgY(),
                mouseX, mouseY);
    }
}
