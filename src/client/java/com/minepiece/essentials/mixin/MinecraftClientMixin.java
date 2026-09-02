package com.minepiece.essentials.mixin;

import com.minepiece.essentials.network.ServerGuiInterceptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (ServerGuiInterceptor.isIntercepting()
                && ((Minecraft)(Object)this).gui.screen() instanceof AbstractContainerScreen<?>) {
            ((Minecraft)(Object)this).setScreenAndShow(null);
        }
    }
}
