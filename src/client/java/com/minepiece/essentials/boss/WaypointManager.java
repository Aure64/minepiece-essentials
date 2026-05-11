package com.minepiece.essentials.boss;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.island.Island;
import com.minepiece.essentials.island.IslandChangeCallback;
import com.minepiece.essentials.util.JsonHelper;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class WaypointManager {
    private static WaypointManager instance;
    private final Map<Island, List<ManualWaypoint>> manualWaypoints = new HashMap<>();
    private Path xaeroWaypointsDir;
    private boolean xaeroAvailable;

    public static WaypointManager getInstance() {
        if (instance == null) instance = new WaypointManager();
        return instance;
    }

    public void init() {
        xaeroAvailable = FabricLoader.getInstance().isModLoaded("xaerominimap");
        if (xaeroAvailable) {
            // Xaero stores waypoints in xaero/minimap/Multiplayer_<server>/
            xaeroWaypointsDir = FabricLoader.getInstance().getGameDir()
                    .resolve("xaero").resolve("minimap");
            MinepieceEssentialsClient.LOGGER.info("[Waypoint] Xaero dir: {}", xaeroWaypointsDir);
        }
        loadManualWaypoints();

        IslandChangeCallback.EVENT.register((prev, current) -> {
            if (xaeroAvailable) {
                syncWaypointsForIsland(current);
            }
        });
    }

    public void syncWaypointsForIsland(Island island) {
        if (!xaeroAvailable || island == Island.UNKNOWN) return;

        Path serverDir = findServerWaypointDir();
        if (serverDir == null) {
            MinepieceEssentialsClient.LOGGER.warn("[Waypoint] No server waypoint dir found");
            return;
        }

        Path configFile = serverDir.resolve("config.txt");
        MinepieceEssentialsClient.LOGGER.info("[Waypoint] Syncing to: {}", configFile);

        try {
            // Read existing config
            List<String> lines = Files.exists(configFile)
                ? new ArrayList<>(Files.readAllLines(configFile))
                : new ArrayList<>();

            // Remove existing MinePiece waypoints (lines starting with "waypoint:" that contain "MinePiece")
            lines.removeIf(line -> line.startsWith("waypoint:") && line.contains(":gui.xaero_minepiece:"));

            // Add manual waypoints (only the ones the user toggled on)
            List<ManualWaypoint> manuals = manualWaypoints.getOrDefault(island, List.of());
            for (ManualWaypoint wp : manuals) {
                String waypointLine = String.format("waypoint:%s:%s:%d:%d:%d:%d:false:0:gui.xaero_minepiece:false:0",
                        wp.name, wp.name.substring(0, 1).toUpperCase(),
                        wp.x, wp.y, wp.z, wp.color);
                lines.add(waypointLine);
                MinepieceEssentialsClient.LOGGER.info("[Waypoint] Added: {}", waypointLine);
            }

            Files.writeString(configFile, String.join("\n", lines) + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            MinepieceEssentialsClient.LOGGER.error("[Waypoint] Failed to sync", e);
        }
    }

    private Path findServerWaypointDir() {
        if (xaeroWaypointsDir == null || !Files.exists(xaeroWaypointsDir)) return null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(xaeroWaypointsDir, "Multiplayer_*")) {
            for (Path dir : stream) {
                return dir;
            }
        } catch (IOException e) {
            // ignore
        }
        return null;
    }

    public void addManualWaypoint(Island island, String name, int x, int y, int z, int color) {
        manualWaypoints.computeIfAbsent(island, k -> new ArrayList<>())
                .add(new ManualWaypoint(name, x, y, z, color));
        saveManualWaypoints();
        syncWaypointsForIsland(island);
    }

    public void removeManualWaypoint(Island island, String name) {
        List<ManualWaypoint> list = manualWaypoints.get(island);
        if (list != null) {
            list.removeIf(wp -> wp.name.equals(name));
            saveManualWaypoints();
            syncWaypointsForIsland(island);
        }
    }

    private void loadManualWaypoints() {
        for (Island island : Island.values()) {
            Path path = MinepieceEssentialsClient.getInstance().getConfigManager()
                    .waypointDir().resolve(island.id + ".json");
            ManualWaypoint[] loaded = JsonHelper.load(path, ManualWaypoint[].class, new ManualWaypoint[0]);
            if (loaded.length > 0) {
                manualWaypoints.put(island, new ArrayList<>(Arrays.asList(loaded)));
            }
        }
    }

    private void saveManualWaypoints() {
        for (Map.Entry<Island, List<ManualWaypoint>> entry : manualWaypoints.entrySet()) {
            Path path = MinepieceEssentialsClient.getInstance().getConfigManager()
                    .waypointDir().resolve(entry.getKey().id + ".json");
            JsonHelper.save(path, entry.getValue().toArray(new ManualWaypoint[0]));
        }
    }

    public static class ManualWaypoint {
        public String name;
        public int x, y, z;
        public int color;

        public ManualWaypoint() {}

        public ManualWaypoint(String name, int x, int y, int z, int color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
        }
    }
}
