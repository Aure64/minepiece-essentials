package com.minepiece.essentials.boss;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.hud.HudElement;
import com.minepiece.essentials.hud.ParchmentRenderer;
import com.minepiece.essentials.island.Island;
import com.minepiece.essentials.island.IslandDetector;
import com.minepiece.essentials.network.BackgroundGuiRefresh;
import com.minepiece.essentials.util.ColorUtils;
import com.minepiece.essentials.util.RenderUtils;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class BossTimerHud extends HudElement {
    private static final int WIDTH = 190;
    private static final int LINE_HEIGHT = 12;
    private static final int ISLAND_HEADER_HEIGHT = 14;
    private static final int HEADER_HEIGHT = 18;
    private static final int REFRESH_BTN_SIZE = 12;
    private static final int STATUS_HEIGHT = 11;

    private static void drawRefreshIcon(GuiGraphicsExtractor ctx, int x, int y, int color) {
        int cx = x + REFRESH_BTN_SIZE / 2;
        int cy = y + REFRESH_BTN_SIZE / 2;
        int r = REFRESH_BTN_SIZE / 2 - 1;
        for (int angle = 0; angle < 300; angle += 10) {
            double rad = Math.toRadians(angle);
            int px = cx + (int)(r * Math.cos(rad));
            int py = cy + (int)(r * Math.sin(rad));
            ctx.fill(px, py, px + 2, py + 2, color);
        }
        ctx.fill(cx + r, cy - 2, cx + r + 2, cy + 1, color);
        ctx.fill(cx + r - 2, cy - 3, cx + r, cy - 1, color);
    }

    /** "12s", "3min", "1h05" */
    private static String formatAgo(long millis) {
        long s = Math.max(0, millis / 1000);
        if (s < 60) return s + "s";
        long m = s / 60;
        if (m < 60) return m + "min";
        return String.format("%dh%02d", m / 60, m % 60);
    }

    /** Tracked islands ordered for display: the current island first, then the rest. */
    private static List<Island> orderedIslands(Island current) {
        List<Island> ordered = new ArrayList<>();
        if (current != Island.UNKNOWN && BossTracker.TRACKED_ISLANDS.contains(current)) {
            ordered.add(current);
        }
        for (Island island : BossTracker.TRACKED_ISLANDS) {
            if (island != current) ordered.add(island);
        }
        return ordered;
    }

    // Track clickable positions for click detection (element-local coords)
    private final Map<Island, int[]> islandHeaderClickAreas = new HashMap<>();
    private final List<BossClickArea> bossClickAreas = new ArrayList<>();
    private int[] refreshAllButtonPos = null;

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
    public void render(GuiGraphicsExtractor ctx, float tickDelta) {
        islandHeaderClickAreas.clear();
        bossClickAreas.clear();
        refreshAllButtonPos = null;

        Island currentIsland = IslandDetector.getInstance().getCurrentIsland();

        List<Island> orderedIslands = orderedIslands(currentIsland);
        if (orderedIslands.isEmpty()) return;

        Map<Island, List<BossData>> allData = BossTracker.getInstance().getAllBossData();
        int totalLines = 0;
        for (Island island : orderedIslands) {
            if (isCollapsed(island)) continue;
            List<BossData> bosses = allData.getOrDefault(island, List.of());
            long bossCount = bosses.stream().filter(b -> b.hasCoords).count();
            totalLines += (int) Math.max(bossCount, 1);
        }

        BossTracker tracker = BossTracker.getInstance();
        boolean pending = tracker.isRefreshPending();
        boolean isBusy = BackgroundGuiRefresh.isBusy();

        int h = HEADER_HEIGHT + STATUS_HEIGHT + orderedIslands.size() * ISLAND_HEADER_HEIGHT + totalLines * LINE_HEIGHT + 8;
        this.height = h;

        ParchmentRenderer.renderPanel(ctx, 0, 0, WIDTH, h, Component.translatable("minepiece.ui.boss.title").getString(), getBackground());

        // Single refresh button — top-right of header (one /boss read covers every island).
        int btnX = WIDTH - REFRESH_BTN_SIZE - 8;
        int btnY = 3;
        int clickW = REFRESH_BTN_SIZE + 8;
        int clickH = REFRESH_BTN_SIZE + 4;
        boolean btnHovered = isHovered(btnX - 3, btnY - 2, clickW, clickH);
        int btnColor;
        if (pending || isBusy) btnColor = 0xFFFFAA00;   // yellow — refresh in flight
        else if (btnHovered) btnColor = 0xFF88FF88;
        else btnColor = 0xFF44AA44;
        if (btnHovered) {
            ctx.fill(btnX - 2, btnY - 1, btnX + REFRESH_BTN_SIZE + 2, btnY + REFRESH_BTN_SIZE + 1, 0x66FFFFFF);
        }
        drawRefreshIcon(ctx, btnX, btnY, btnColor);
        refreshAllButtonPos = new int[]{btnX - 3, btnY - 2, clickW, clickH};

        int y = HEADER_HEIGHT;

        // Status line: refreshing / updated X ago / no data yet
        String status;
        int statusColor;
        if (pending || isBusy) {
            status = Component.translatable("minepiece.ui.boss.refreshing").getString();
            statusColor = 0xFFAA6600;
        } else if (tracker.getLastRefreshMillis() > 0) {
            status = Component.translatable("minepiece.ui.boss.updated", formatAgo(System.currentTimeMillis() - tracker.getLastRefreshMillis())).getString();
            statusColor = 0xFF888888;
        } else {
            status = Component.translatable("minepiece.ui.boss.never").getString();
            statusColor = 0xFF888888;
        }
        RenderUtils.drawText(ctx, status, 6, y, statusColor);
        y += STATUS_HEIGHT;

        for (Island island : orderedIslands) {
            boolean isCurrent = island == currentIsland;
            boolean collapsed = isCollapsed(island);
            int headerColor = isCurrent ? 0xFFFFAA00 : 0xFF8B6914;
            String islandName = (collapsed ? "▶ " : "▼ ")
                    + (isCurrent ? "> " : "") + island.displayName;

            // Header is clickable (in HUD edit mode) to collapse/expand the island.
            int headerW = WIDTH - 4;
            if (isHovered(2, y - 1, headerW, ISLAND_HEADER_HEIGHT)) {
                ctx.fill(2, y - 1, 2 + headerW, y - 1 + ISLAND_HEADER_HEIGHT, 0x22FFFFFF);
            }
            islandHeaderClickAreas.put(island, new int[]{2, y - 1, headerW, ISLAND_HEADER_HEIGHT});

            RenderUtils.drawText(ctx, islandName, 4, y, headerColor);

            ctx.fill(4, y + 10, WIDTH - 4, y + 11, headerColor);
            y += ISLAND_HEADER_HEIGHT;

            if (collapsed) continue;

            List<BossData> bosses = allData.getOrDefault(island, List.of()).stream()
                    .filter(b -> b.hasCoords)
                    .sorted(Comparator.comparingInt(BossData::estimateCurrentTimer))
                    .toList();

            if (bosses.isEmpty()) {
                RenderUtils.drawText(ctx, "  " + Component.translatable("minepiece.ui.boss.nodata").getString(), 4, y, 0xFF888888);
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
                    int nameColor = hasWaypoint ? 0xFF44CCFF : getBackground().textColor();
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

        // Refresh-all button — also acts as cancel when queue is active
        if (refreshAllButtonPos != null) {
            int[] btn = refreshAllButtonPos;
            int btnScreenX = hudX + (int)(btn[0] * scale);
            int btnScreenY = hudY + (int)(btn[1] * scale);
            int btnW = (int)(btn[2] * scale);
            int btnH = (int)(btn[3] * scale);
            if (mouseX >= btnScreenX && mouseX <= btnScreenX + btnW
                && mouseY >= btnScreenY && mouseY <= btnScreenY + btnH) {
                if (BossTracker.getInstance().isRefreshPending()) {
                    BossTracker.getInstance().cancelRefreshQueue();
                } else {
                    BossTracker.getInstance().refresh();
                }
                return true;
            }
        }

        // Island header clicks — collapse/expand the island
        for (Map.Entry<Island, int[]> entry : islandHeaderClickAreas.entrySet()) {
            int[] area = entry.getValue();
            int areaScreenX = hudX + (int)(area[0] * scale);
            int areaScreenY = hudY + (int)(area[1] * scale);
            int areaW = (int)(area[2] * scale);
            int areaH = (int)(area[3] * scale);
            if (mouseX >= areaScreenX && mouseX <= areaScreenX + areaW
                && mouseY >= areaScreenY && mouseY <= areaScreenY + areaH) {
                toggleIslandCollapsed(entry.getKey());
                return true;
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

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String bossKey = island.id + ":" + boss.name;

        if (activeWaypoints.contains(bossKey)) {
            activeWaypoints.remove(bossKey);
            // Send chat message to confirm removal
            client.player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("\u00a7c[MinePiece] \u00a77Waypoint retir\u00e9: \u00a7f" + boss.name));
        } else {
            activeWaypoints.add(bossKey);
            // Copy coords to clipboard
            String coords = boss.x + " " + boss.y + " " + boss.z;
            client.keyboardHandler.setClipboard(coords);
            // Send chat message with coords
            client.player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "\u00a7a[MinePiece] \u00a77Waypoint: \u00a7f" + boss.name +
                    " \u00a77[\u00a7b" + boss.x + " " + boss.y + " " + boss.z +
                    "\u00a77] \u00a78(coords copi\u00e9es)"));
        }
    }

    @Override
    public void tick() {
        BossTracker.getInstance().tick();
    }

    private static Set<String> collapsedSet() {
        var config = MinepieceEssentialsClient.getInstance().getConfigManager().config();
        if (config.collapsedBossIslands == null) config.collapsedBossIslands = new HashSet<>();
        return config.collapsedBossIslands;
    }

    private boolean isCollapsed(Island island) {
        return collapsedSet().contains(island.id);
    }

    private void toggleIslandCollapsed(Island island) {
        Set<String> collapsed = collapsedSet();
        if (!collapsed.remove(island.id)) {
            collapsed.add(island.id);
        }
        MinepieceEssentialsClient.getInstance().getConfigManager().save();
    }

    private record BossClickArea(int x, int y, int w, int h, BossData boss, Island island) {}
}
