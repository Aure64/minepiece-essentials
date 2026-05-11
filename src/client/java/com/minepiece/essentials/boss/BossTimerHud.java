package com.minepiece.essentials.boss;

import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.island.Island;
import com.minepiece.essentials.island.IslandDetector;
import com.minepiece.essentials.network.BackgroundGuiRefresh;
import com.minepiece.essentials.util.ColorUtils;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.*;

public class BossTimerHud extends HudElement {
    private static final int WIDTH = 190;
    private static final int LINE_HEIGHT = 12;
    private static final int ISLAND_HEADER_HEIGHT = 14;
    private static final int HEADER_HEIGHT = 18;
    private static final int REFRESH_BTN_SIZE = 12;

    // Track clickable positions for click detection (element-local coords)
    private final Map<Island, int[]> refreshButtonPositions = new HashMap<>();
    private final List<BossClickArea> bossClickAreas = new ArrayList<>();

    // Track which bosses have active waypoints
    private final Set<String> activeWaypoints = new HashSet<>();

    // Mouse position for hover effects (set by HudEditScreen)
    private double hoverMouseX = -1, hoverMouseY = -1;

    public BossTimerHud() {
        super("boss_timer", 5, 200, WIDTH, 100);
    }

    public void setMousePos(double mx, double my) {
        this.hoverMouseX = mx;
        this.hoverMouseY = my;
    }

    private boolean isHovered(int localX, int localY, int w, int h) {
        if (hoverMouseX < 0) return false;
        float scale = getScale();
        int screenX = getX() + (int)(localX * scale);
        int screenY = getY() + (int)(localY * scale);
        int screenW = (int)(w * scale);
        int screenH = (int)(h * scale);
        return hoverMouseX >= screenX && hoverMouseX <= screenX + screenW
            && hoverMouseY >= screenY && hoverMouseY <= screenY + screenH;
    }

    @Override
    public void render(DrawContext ctx, float tickDelta) {
        refreshButtonPositions.clear();
        bossClickAreas.clear();

        Island currentIsland = IslandDetector.getInstance().getCurrentIsland();

        List<Island> orderedIslands = new ArrayList<>();
        if (currentIsland != Island.UNKNOWN && BossTracker.TRACKED_ISLANDS.contains(currentIsland)) {
            orderedIslands.add(currentIsland);
        }
        for (Island island : BossTracker.TRACKED_ISLANDS) {
            if (island != currentIsland) {
                orderedIslands.add(island);
            }
        }

        if (orderedIslands.isEmpty()) return;

        Map<Island, List<BossData>> allData = BossTracker.getInstance().getAllBossData();
        int totalLines = 0;
        for (Island island : orderedIslands) {
            List<BossData> bosses = allData.getOrDefault(island, List.of());
            long bossCount = bosses.stream().filter(b -> b.hasCoords).count();
            totalLines += (int) Math.max(bossCount, 1);
        }

        int h = HEADER_HEIGHT + orderedIslands.size() * ISLAND_HEADER_HEIGHT + totalLines * LINE_HEIGHT + 8;
        this.height = h;

        // Add space for scan progress bar if scanning
        boolean scanning = BossTracker.getInstance().isInitialScanInProgress();
        if (scanning) h += 14;

        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, "Boss Timers");

        int y = HEADER_HEIGHT;

        // Initial scan progress bar
        if (scanning) {
            float progress = BossTracker.getInstance().getInitialScanProgress();
            int scanned = BossTracker.getInstance().getInitialScanned();
            int total = BossTracker.getInstance().getInitialTotal();
            String scanText = "Scan: " + scanned + "/" + total;
            RenderUtils.drawText(ctx, scanText, 6, y, 0xFF888888);
            RenderUtils.drawProgressBar(ctx, 6, y + 10, WIDTH - 12, 3, progress, 0xFF44AA44);
            y += 14;
        }

        boolean isBusy = BackgroundGuiRefresh.isBusy();

        for (Island island : orderedIslands) {
            boolean isCurrent = island == currentIsland;
            int headerColor = isCurrent ? 0xFFFFAA00 : 0xFF8B6914;
            String islandName = isCurrent ? "> " + island.displayName : island.displayName;

            RenderUtils.drawText(ctx, islandName, 4, y, headerColor);

            // Refresh button — large click area
            int btnX = WIDTH - 20;
            int btnY = y - 1;
            int clickW = REFRESH_BTN_SIZE + 8;
            int clickH = REFRESH_BTN_SIZE + 4;
            boolean btnHovered = isHovered(btnX - 3, btnY - 2, clickW, clickH);
            int btnColor = isBusy ? 0xFF666666 : (btnHovered ? 0xFF88FF88 : 0xFF44AA44);
            // Button background on hover
            if (btnHovered && !isBusy) {
                ctx.fill(btnX - 2, btnY - 1, btnX + REFRESH_BTN_SIZE + 2, btnY + REFRESH_BTN_SIZE + 1, 0x66FFFFFF);
            }
            // Draw refresh circle icon
            int cx = btnX + REFRESH_BTN_SIZE / 2;
            int cy = btnY + REFRESH_BTN_SIZE / 2;
            int r = REFRESH_BTN_SIZE / 2 - 1;
            for (int angle = 0; angle < 300; angle += 10) {
                double rad = Math.toRadians(angle);
                int px = cx + (int)(r * Math.cos(rad));
                int py = cy + (int)(r * Math.sin(rad));
                ctx.fill(px, py, px + 2, py + 2, btnColor);
            }
            ctx.fill(cx + r, cy - 2, cx + r + 2, cy + 1, btnColor);
            ctx.fill(cx + r - 2, cy - 3, cx + r, cy - 1, btnColor);

            refreshButtonPositions.put(island, new int[]{btnX - 3, btnY - 2, clickW, clickH});

            ctx.fill(4, y + 10, WIDTH - 4, y + 11, headerColor);
            y += ISLAND_HEADER_HEIGHT;

            List<BossData> bosses = allData.getOrDefault(island, List.of()).stream()
                    .filter(b -> b.hasCoords)
                    .sorted(Comparator.comparingInt(BossData::estimateCurrentTimer))
                    .toList();

            if (bosses.isEmpty()) {
                RenderUtils.drawText(ctx, "  (cliquer \u27f3 pour scanner)", 4, y, 0xFF888888);
                y += LINE_HEIGHT;
            } else {
                for (BossData boss : bosses) {
                    String timer = boss.formatTimer();
                    int timerColor;
                    if (boss.isAvailable()) {
                        timerColor = 0xFF00CC00;
                        if (System.currentTimeMillis() % 1000 < 500) {
                            timerColor = 0xFF00FF44;
                        }
                    } else if (boss.estimateCurrentTimer() < 30) {
                        timerColor = 0xFFFFAA00;
                    } else {
                        timerColor = 0xFFCC0000;
                    }

                    // Boss name — underlined if waypoint is active
                    String bossKey = island.id + ":" + boss.name;
                    boolean hasWaypoint = activeWaypoints.contains(bossKey);

                    String name = boss.name;
                    if (RenderUtils.textWidth(name) > WIDTH - 65) {
                        while (RenderUtils.textWidth(name + "..") > WIDTH - 65 && name.length() > 3) {
                            name = name.substring(0, name.length() - 1);
                        }
                        name += "..";
                    }

                    boolean bossHovered = isHovered(4, y, WIDTH - 20, LINE_HEIGHT);
                    int nameColor = hasWaypoint ? 0xFF44CCFF : ColorUtils.TEXT_DARK;
                    String prefix = hasWaypoint ? "  \u25C6 " : "  ";

                    // Hover highlight
                    if (bossHovered) {
                        ctx.fill(4, y, WIDTH - 20, y + LINE_HEIGHT, 0x33FFFFFF);
                        nameColor = hasWaypoint ? 0xFF88DDFF : 0xFF6B5300;
                    }

                    RenderUtils.drawText(ctx, prefix + name, 4, y, nameColor);

                    // Underline if waypoint active
                    if (hasWaypoint) {
                        int textW = RenderUtils.textWidth(prefix + name);
                        ctx.fill(4, y + 9, 4 + textW, y + 10, 0xFF44CCFF);
                    }

                    // Timer
                    int timerWidth = RenderUtils.textWidth(timer);
                    RenderUtils.drawText(ctx, timer, WIDTH - timerWidth - 6, y, timerColor);

                    // Store click area for this boss line
                    bossClickAreas.add(new BossClickArea(4, y, WIDTH - 20, LINE_HEIGHT, boss, island));

                    y += LINE_HEIGHT;
                }
            }
        }
    }

    public boolean handleClick(double mouseX, double mouseY) {
        int hudX = getX();
        int hudY = getY();
        float scale = getScale();

        // Check refresh buttons
        if (!BackgroundGuiRefresh.isBusy()) {
            for (Map.Entry<Island, int[]> entry : refreshButtonPositions.entrySet()) {
                int[] btn = entry.getValue();
                int btnScreenX = hudX + (int)(btn[0] * scale);
                int btnScreenY = hudY + (int)(btn[1] * scale);
                int btnW = (int)(btn[2] * scale);
                int btnH = (int)(btn[3] * scale);

                if (mouseX >= btnScreenX && mouseX <= btnScreenX + btnW
                    && mouseY >= btnScreenY && mouseY <= btnScreenY + btnH) {
                    BossTracker.getInstance().refreshIsland(entry.getKey());
                    return true;
                }
            }
        }

        // Check boss name clicks — toggle waypoint
        for (BossClickArea area : bossClickAreas) {
            int areaScreenX = hudX + (int)(area.x * scale);
            int areaScreenY = hudY + (int)(area.y * scale);
            int areaW = (int)(area.w * scale);
            int areaH = (int)(area.h * scale);

            if (mouseX >= areaScreenX && mouseX <= areaScreenX + areaW
                && mouseY >= areaScreenY && mouseY <= areaScreenY + areaH) {
                toggleBossWaypoint(area.boss, area.island);
                return true;
            }
        }

        return false;
    }

    private void toggleBossWaypoint(BossData boss, Island island) {
        if (!boss.hasCoords) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String bossKey = island.id + ":" + boss.name;

        if (activeWaypoints.contains(bossKey)) {
            activeWaypoints.remove(bossKey);
            // Send chat message to confirm removal
            client.player.sendMessage(
                net.minecraft.text.Text.literal("\u00a7c[MinePiece] \u00a77Waypoint retir\u00e9: \u00a7f" + boss.name),
                false);
        } else {
            activeWaypoints.add(bossKey);
            // Copy coords to clipboard
            String coords = boss.x + " " + boss.y + " " + boss.z;
            client.keyboard.setClipboard(coords);
            // Send chat message with coords
            client.player.sendMessage(
                net.minecraft.text.Text.literal(
                    "\u00a7a[MinePiece] \u00a77Waypoint: \u00a7f" + boss.name +
                    " \u00a77[\u00a7b" + boss.x + " " + boss.y + " " + boss.z +
                    "\u00a77] \u00a78(coords copi\u00e9es)"),
                false);
        }
    }

    @Override
    public void tick() {
        BossTracker.getInstance().tick();
    }

    private record BossClickArea(int x, int y, int w, int h, BossData boss, Island island) {}
}
