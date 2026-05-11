package com.minepiece.essentials.boss;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.island.Island;
import com.minepiece.essentials.island.IslandDetector;
import com.minepiece.essentials.network.BackgroundGuiRefresh;
import com.minepiece.essentials.util.JsonHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BossTracker {
    private static BossTracker instance;
    private final Map<Island, List<BossData>> bossMap = new ConcurrentHashMap<>();

    public static final Set<Island> TRACKED_ISLANDS = Set.of(
        Island.FUCHSIA,
        Island.DRUM,
        Island.ALABASTA,
        Island.THRILLER_BARK,
        Island.SABAODY,
        Island.ILE_HOMMES_POISSONS,
        Island.DRESSROSA,
        Island.WHOLE_CAKE
    );

    private static final Pattern COORD_PATTERN =
        Pattern.compile("Coordonn[e\u00e9]es?\\s*:?\\s*([-\\d]+)\\s+([-\\d]+)\\s+([-\\d]+)");
    private static final Pattern TIMER_PATTERN =
        Pattern.compile("(?:R[e\u00e9]a?parition|Apparition)\\s*:?\\s*(?:(\\d+)m)?\\s*(\\d+)s");
    private static final Pattern INTERVAL_PATTERN =
        Pattern.compile("Toutes les (\\d+) Minutes?", Pattern.CASE_INSENSITIVE);

    public static BossTracker getInstance() {
        if (instance == null) instance = new BossTracker();
        return instance;
    }

    public void init() {
        // Load saved boss data from previous sessions
        for (Island island : TRACKED_ISLANDS) {
            Path path = MinepieceEssentialsClient.getInstance().getConfigManager()
                    .bossDir().resolve(island.id + ".json");
            BossData[] loaded = JsonHelper.load(path, BossData[].class, new BossData[0]);
            if (loaded.length > 0) {
                List<BossData> bosses = new ArrayList<>(Arrays.asList(loaded));
                // Restore island reference (transient field)
                for (BossData b : bosses) b.island = island;
                bossMap.put(island, bosses);
                MinepieceEssentialsClient.LOGGER.info("[BossTracker] Loaded {} bosses for {}", loaded.length, island.displayName);
            }
        }
    }

    private boolean initialScanDone = false;
    private boolean wasConnected = false;

    // Refresh queue — clicks add to it, tick() polls one at a time respecting cooldown.
    // Each cycle takes ~5s (BGRefresh global cooldown is the bottleneck).
    private static final long PER_ISLAND_SECONDS = 5L;
    private final Deque<Island> refreshQueue = new ArrayDeque<>();

    public void tick() {
        BackgroundGuiRefresh.tick();

        boolean connected = IslandDetector.getInstance().getCurrentIsland() != Island.UNKNOWN;
        if (wasConnected && !connected) {
            initialScanDone = false;
            refreshQueue.clear();
            BossAlertManager.getInstance().reset();
        }
        wasConnected = connected;

        BossAlertManager.getInstance().tick();

        if (!initialScanDone && connected) {
            initialScanDone = true;
        }

        if (!refreshQueue.isEmpty() && !BackgroundGuiRefresh.isBusy() && BackgroundGuiRefresh.isReady()) {
            Island next = refreshQueue.poll();
            if (next != null) {
                doRefresh(next);
            }
        }
    }

    /**
     * Queue an island for refresh. Clicking the refresh button always enqueues —
     * no silent failures from cooldown / busy state.
     */
    public void refreshIsland(Island island) {
        if (refreshQueue.contains(island)) return;
        refreshQueue.add(island);
    }

    public void refreshAllIslands() {
        for (Island island : TRACKED_ISLANDS) {
            if (!refreshQueue.contains(island)) {
                refreshQueue.add(island);
            }
        }
    }

    public void cancelRefreshQueue() {
        refreshQueue.clear();
    }

    public int getQueueSize() { return refreshQueue.size(); }
    public boolean isInQueue(Island island) { return refreshQueue.contains(island); }

    /** Estimated seconds until queue is fully drained. */
    public int getEtaSeconds() {
        int pending = refreshQueue.size();
        long cooldownRemaining = BackgroundGuiRefresh.getCooldownRemainingMs();
        return (int) Math.ceil((pending * PER_ISLAND_SECONDS * 1000L + cooldownRemaining) / 1000.0);
    }

    private void doRefresh(Island island) {
        String command = island.getCommand();
        MinepieceEssentialsClient.LOGGER.info("[BossTracker] Refresh: {} ({})", island.displayName, command);

        BackgroundGuiRefresh.sendCommand(command, items -> {
            int mobSlot = findMobSlot(items);
            MinepieceEssentialsClient.LOGGER.info("[BossTracker] 1st screen: {} items, clicking slot {}", items.size(), mobSlot);

            BackgroundGuiRefresh.clickSlotAndListen(mobSlot, mobItems -> {
                MinepieceEssentialsClient.LOGGER.info("[BossTracker] 2nd screen: {} mob items", mobItems.size());
                parseBossItems(island, mobItems);
            });
        });
    }

    private int findMobSlot(Map<Integer, ItemStack> items) {
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            ItemStack stack = entry.getValue();
            if (stack.isEmpty()) continue;
            String name = stack.getName().getString().toLowerCase();
            if (name.contains("marine") || name.contains("monstre") || name.contains("mob")
                || name.contains("ennemi") || name.contains("combat") || name.contains("bandit")
                || name.contains("pirate") || name.contains("garde") || name.contains("zombie")
                || name.contains("chasseur")) {
                return entry.getKey();
            }
        }
        return 16;
    }

    private void parseBossItems(Island island, Map<Integer, ItemStack> items) {
        List<BossData> bosses = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            ItemStack stack = entry.getValue();
            if (stack == null || stack.isEmpty()) continue;

            String name = stack.getName().getString();
            if (name.isEmpty()) continue;

            String nameLower = name.toLowerCase();
            if (nameLower.contains("retour") || nameLower.contains("page") || nameLower.contains("fermer")) continue;

            BossData boss = new BossData(name, island);

            var tooltip = stack.getTooltip(net.minecraft.item.Item.TooltipContext.DEFAULT,
                    null, net.minecraft.item.tooltip.TooltipType.BASIC);
            for (Text text : tooltip) {
                String line = text.getString();
                Matcher coordMatch = COORD_PATTERN.matcher(line);
                if (coordMatch.find()) {
                    boss.x = Integer.parseInt(coordMatch.group(1));
                    boss.y = Integer.parseInt(coordMatch.group(2));
                    boss.z = Integer.parseInt(coordMatch.group(3));
                    boss.hasCoords = true;
                    boss.type = "mini_boss";
                }
                Matcher timerMatch = TIMER_PATTERN.matcher(line);
                if (timerMatch.find()) {
                    String minStr = timerMatch.group(1);
                    int min = minStr != null ? Integer.parseInt(minStr) : 0;
                    int sec = Integer.parseInt(timerMatch.group(2));
                    boss.lastKnownTimerSeconds = min * 60 + sec;
                    boss.lastKnownRespawnTimestamp = System.currentTimeMillis();
                }
                Matcher intervalMatch = INTERVAL_PATTERN.matcher(line);
                if (intervalMatch.find()) {
                    boss.respawnIntervalSeconds = Integer.parseInt(intervalMatch.group(1)) * 60;
                }
            }

            if (!boss.hasCoords) boss.type = "mob";
            bosses.add(boss);
        }

        bossMap.put(island, bosses);
        saveBossData(island, bosses);
        MinepieceEssentialsClient.LOGGER.info("[BossTracker] {} - {} bosses ({} with coords)",
            island.displayName, bosses.size(),
            bosses.stream().filter(b -> b.hasCoords).count());
    }

    public List<BossData> getBossesForIsland(Island island) {
        return bossMap.getOrDefault(island, Collections.emptyList());
    }

    public Map<Island, List<BossData>> getAllBossData() {
        return Collections.unmodifiableMap(bossMap);
    }

    private void saveBossData(Island island, List<BossData> bosses) {
        Path path = MinepieceEssentialsClient.getInstance().getConfigManager()
                .bossDir().resolve(island.id + ".json");
        JsonHelper.save(path, bosses);
    }
}
