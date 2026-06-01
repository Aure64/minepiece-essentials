package com.minepiece.essentials.mixin;

import com.minepiece.essentials.island.IslandDetector;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    // render() runs every frame. Island detection doesn't need frame precision,
    // so throttle to ~5×/s — this avoids a getString() + map scan every frame.
    @Unique
    private long minepiece$lastBossbarScan;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (now - minepiece$lastBossbarScan < 200) return;
        minepiece$lastBossbarScan = now;

        for (ClientBossBar bar : bossBars.values()) {
            IslandDetector.getInstance().onBossBarUpdate(bar.getName().getString());
        }
    }
}
