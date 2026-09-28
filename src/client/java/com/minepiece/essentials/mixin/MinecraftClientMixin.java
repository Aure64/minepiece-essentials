package com.minepiece.essentials.mixin;

import com.minepiece.essentials.network.BackgroundGuiRefresh;
import com.minepiece.essentials.network.ServerGuiInterceptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Masque l'écran intercepté par {@link ServerGuiInterceptor} dès le tick suivant
 * son ouverture — uniquement celui-là (même id de conteneur), jamais un écran
 * ouvert par le joueur. La fermeture côté client envoie le paquet de fermeture ;
 * l'écran du mod ouvert avant le relevé (éditeur K) est remis en place.
 */
@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!ServerGuiInterceptor.isIntercepting()) return;
        int syncId = ServerGuiInterceptor.getExpectedSyncId();
        if (syncId < 0) return;
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.gui.screen() instanceof AbstractContainerScreen<?> cs
                && cs.getMenu().containerId == syncId) {
            ServerGuiInterceptor.markScreenClosedClientSide();
            // Remet l'écran du mod (éditeur K) s'il y en avait un, sinon retour au jeu.
            mc.setScreenAndShow(BackgroundGuiRefresh.screenToRestore());
        }
    }
}
