package com.minepiece.essentials.mixin;

import com.minepiece.essentials.island.IslandDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;

@Mixin(BossHealthOverlay.class)
public class BossBarHudMixin {
    @Shadow
    private Map<UUID, LerpingBossEvent> events;

    // render() runs every frame. Island detection doesn't need frame precision,
    // so throttle to ~5×/s — this avoids a getString() + map scan every frame.
    @Unique
    private long minepiece$lastBossbarScan;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(GuiGraphics context, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (now - minepiece$lastBossbarScan < 200) return;
        minepiece$lastBossbarScan = now;

        for (LerpingBossEvent bar : events.values()) {
            IslandDetector.getInstance().onBossBarUpdate(bar.getName().getString());
        }
    }
}
