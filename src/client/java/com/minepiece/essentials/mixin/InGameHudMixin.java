package com.minepiece.essentials.mixin;

import com.minepiece.essentials.job.JobTracker;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds action-bar overlay messages to the job progress tracker. */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void minepiece$jobProgress(Text message, boolean tinted, CallbackInfo ci) {
        if (message != null) {
            JobTracker.onActionBar(message.getString());
        }
    }
}
