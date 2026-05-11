package com.minepiece.essentials.mixin;

import com.minepiece.essentials.island.IslandDetector;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.DrawContext;
import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {
    @Shadow
    private Map<UUID, ClientBossBar> bossBars;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, CallbackInfo ci) {
        for (ClientBossBar bar : bossBars.values()) {
            String text = bar.getName().getString();
            IslandDetector.getInstance().onBossBarUpdate(text);
        }
    }
}
