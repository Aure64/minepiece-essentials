package com.minepiece.essentials.mixin;

import com.minepiece.essentials.rarity.RarityScreenOverlay;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dessine l'overlay rareté et route les clics de la barre. */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow protected int x; // coin haut-gauche du fond
    @Shadow protected int y;

    @Inject(method = "render", at = @At("TAIL"))
    private void minepiece$rarityOverlay(DrawContext ctx, int mouseX, int mouseY,
                                         float delta, CallbackInfo ci) {
        RarityScreenOverlay.render((HandledScreen<?>) (Object) this, ctx, this.x, this.y, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void minepiece$rarityClick(Click click, boolean doubled,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (RarityScreenOverlay.onClick((HandledScreen<?>) (Object) this, click.x(), click.y())) {
            cir.setReturnValue(true);
        }
    }
}
