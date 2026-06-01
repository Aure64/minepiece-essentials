package com.minepiece.essentials.haki;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

/**
 * Display-only haki cooldown timer. Listens to the server chat: when haki is
 * activated a 30 s countdown starts, cleared when the "ready again" message
 * arrives. No automation — purely informational.
 */
public final class HakiTimer {

    private static final String ACTIVATED = "Vous avez activé le haki";
    private static final String READY = "Vous pouvez de nouveau utiliser votre haki";
    private static final int DURATION_TICKS = 30 * 20; // 30 seconds

    private static int remainingTicks = 0;
    private static boolean active = false;

    private HakiTimer() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            if (text.contains(ACTIVATED)) {
                remainingTicks = DURATION_TICKS;
                active = true;
            } else if (text.contains(READY)) {
                remainingTicks = 0;
                active = false;
            }
        });
    }

    public static void tick() {
        if (active && remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks <= 0) active = false;
        }
    }

    public static boolean isActive() {
        return active && remainingTicks > 0;
    }

    public static int remainingSeconds() {
        return (remainingTicks + 19) / 20;
    }

    public static float progress() {
        return 1f - (float) remainingTicks / DURATION_TICKS;
    }
}
