package com.minepiece.essentials.hud;

import com.minepiece.essentials.ServerDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;

public class HudElementRegistry {
    private static final List<HudElement> elements = new ArrayList<>();
    private static boolean editMode = false;

    public static void register(HudElement element) {
        elements.add(element);
    }

    public static void init() {
        HudRenderCallback.EVENT.register((context, renderTickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) return;
            if (!ServerDetector.isOnMinePiece()) return;

            float tickDelta = renderTickCounter.getTickProgress(true);
            for (HudElement element : elements) {
                if (element.isVisible()) {
                    context.getMatrices().pushMatrix();
                    float scale = element.getScale();
                    context.getMatrices().translate(element.getX(), element.getY());
                    context.getMatrices().scale(scale, scale);
                    element.render(context, tickDelta);
                    context.getMatrices().popMatrix();
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (!ServerDetector.isOnMinePiece()) return;
            for (HudElement element : elements) {
                element.tick();
            }
        });
    }

    public static List<HudElement> getElements() { return elements; }
    public static boolean isEditMode() { return editMode; }
    public static void setEditMode(boolean mode) { editMode = mode; }
}
