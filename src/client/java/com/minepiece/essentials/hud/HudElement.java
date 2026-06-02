package com.minepiece.essentials.hud;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.config.HudBackground;
import com.minepiece.essentials.config.LayoutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public abstract class HudElement {
    protected final String id;
    protected int defaultX, defaultY;
    protected int width, height;

    protected HudElement(String id, int defaultX, int defaultY, int width, int height) {
        this.id = id;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.width = width;
        this.height = height;
    }

    public abstract void render(DrawContext context, float tickDelta);
    public abstract void tick();

    public LayoutConfig.ElementLayout getLayout() {
        var profile = MinepieceEssentialsClient.getInstance().getConfigManager()
                .layout().activeProfile();
        return profile.elements.computeIfAbsent(id,
                k -> new LayoutConfig.ElementLayout(defaultX, defaultY));
    }

    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        int maxX = client.getWindow().getScaledWidth() - getWidth();
        return Math.max(0, Math.min(getLayout().x, maxX));
    }
    public int getY() {
        MinecraftClient client = MinecraftClient.getInstance();
        int maxY = client.getWindow().getScaledHeight() - getHeight();
        return Math.max(0, Math.min(getLayout().y, maxY));
    }
    public float getScale() { return getLayout().scale; }
    public boolean isVisible() { return getLayout().visible; }

    /** The panel background preset chosen for this HUD (never null). */
    public HudBackground getBackground() {
        HudBackground bg = getLayout().background;
        return bg == null ? HudBackground.PARCHMENT : bg;
    }

    public void setBackground(HudBackground background) {
        getLayout().background = background;
    }

    public String getId() { return id; }
    public int getWidth() { return (int)(width * getScale()); }
    public int getHeight() { return (int)(height * getScale()); }

    public boolean isMouseOver(double mouseX, double mouseY) {
        int x = getX(), y = getY();
        return mouseX >= x && mouseX <= x + getWidth()
                && mouseY >= y && mouseY <= y + getHeight();
    }
}
