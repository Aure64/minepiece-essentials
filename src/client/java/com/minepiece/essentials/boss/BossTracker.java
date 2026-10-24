package com.minepiece.essentials.boss;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.island.Island;
import com.minepiece.essentials.island.IslandDetector;
import com.minepiece.essentials.network.BackgroundGuiRefresh;
import com.minepiece.essentials.util.JsonHelper;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Timers de boss. Source unique : l'écran {@code /boss} du serveur, qui liste
 * toutes les îles et leurs boss en un seul écran (un envoi, aucun clic —
 * lecture puis fermeture). Voir {@link BossScreenParser} pour le format.
 */
public class BossTracker {
    private static BossTracker instance;
    private final Map<Island, List<BossData>> bossMap = new ConcurrentHashMap<>();

    public static final String BOSS_COMMAND = "/boss";

    // Ordered (LinkedHashSet) so the HUD can group region islands consecutively;
    // Whole Cake & Komugi are adjacent (both in the Totto Land region).
    public static final Set<Island> TRACKED_ISLANDS = Collections.unmodifiableSet(
        new LinkedHashSet<>(List.of(
            Island.FUCHSIA,
            Island.DRUM,
            Island.ALABASTA,
            Island.THRILLER_BARK,
            Island.SABAODY,
            Island.ILE_HOMMES_POISSONS,
            Island.DRESSROSA,
            Island.ZOU,
            Island.WHOLE_CAKE,
            Island.KOMUGI
        )));

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

    // Un seul refresh couvre toutes les îles : la file est un simple drapeau.
    private boolean refreshPending = false;
    private static final long REFRESH_SECONDS = 5L;
    private volatile long lastRefreshMillis = 0L;

    // Relevé automatique : toutes les AUTO_REFRESH_MS, plus un relevé EXPIRY_DELAY_MS après
    // qu'un timer tombe à zéro (le serveur relance le compteur à la mort du boss). Jamais
    // plus d'un envoi automatique par MIN_AUTO_GAP_MS. Le mécanisme est invisible pour le
    // joueur (voir docs/architecture-background-refresh.md) ; sendCommand refuse de lui-même
    // tant qu'un écran conteneur est ouvert.
    static final long AUTO_REFRESH_MS = 5 * 60_000L;
    static final long EXPIRY_DELAY_MS = 20_000L;
    static final long MIN_AUTO_GAP_MS = 60_000L;
    private static final int EXPIRY_CHECK_TICKS = 20;
    private long lastAutoAttemptMillis = 0L;
    private long expiryRefreshAt = 0L;
    private final Set<String> previouslyAvailable = new HashSet<>();
    private int autoTicks;

    /** Epoch millis of the last successful /boss read, 0 if none this session. */
    public long getLastRefreshMillis() { return lastRefreshMillis; }

    /** Une fois par tick client, depuis {@code MinepieceEssentialsClient}. */
    public void tick() {
        boolean connected = IslandDetector.getInstance().getCurrentIsland() != Island.UNKNOWN;
        if (wasConnected && !connected) {
            initialScanDone = false;
            refreshPending = false;
            BossAlertManager.getInstance().reset();
        }
        wasConnected = connected;

        BossAlertManager.getInstance().tick();

        // Premier relevé automatique une fois en jeu sur une île connue.
        if (!initialScanDone && connected) {
            initialScanDone = true;
            refreshPending = true;
        }

        if (connected && initialScanDone && !refreshPending) tickAutoRefresh();

        // Reste en attente tant que l'envoi est refusé (occupé, cooldown, écran
        // conteneur ouvert).
        if (refreshPending && !BackgroundGuiRefresh.isBusy() && BackgroundGuiRefresh.isReady()) {
            if (doRefresh()) {
                refreshPending = false;
                lastAutoAttemptMillis = System.currentTimeMillis();
            }
        }
    }

    /** Relevé périodique + relevé après expiration d'un timer, si activé dans la config. */
    private void tickAutoRefresh() {
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().bossAutoRefreshEnabled) return;
        long now = System.currentTimeMillis();
        long since = now - Math.max(lastRefreshMillis, lastAutoAttemptMillis);

        if (autoTicks++ % EXPIRY_CHECK_TICKS == 0 && expiryRefreshAt == 0L) {
            boolean expired = false;
            for (List<BossData> bosses : bossMap.values()) {
                for (BossData b : bosses) {
                    String key = b.islandId + "/" + b.name;
                    boolean available = b.isAvailable();
                    if (available && b.lastKnownRespawnTimestamp > 0 && previouslyAvailable.add(key)) {
                        // Déjà disponible au chargement : pas une expiration vue en direct.
                        if (b.lastKnownTimerSeconds > 0) expired = true;
                    } else if (!available) {
                        previouslyAvailable.remove(key);
                    }
                }
            }
            if (expired) expiryRefreshAt = now + EXPIRY_DELAY_MS;
        }

        boolean periodic = since >= AUTO_REFRESH_MS;
        boolean afterExpiry = expiryRefreshAt > 0L && now >= expiryRefreshAt && since >= MIN_AUTO_GAP_MS;
        if (periodic || afterExpiry) {
            refreshPending = true;
            expiryRefreshAt = 0L;
            MinepieceEssentialsClient.LOGGER.info("[BossTracker] Auto refresh ({})", periodic ? "periodic" : "boss spawned");
        }
    }

    /** Demande un refresh (toutes les îles d'un coup). Jamais d'échec silencieux. */
    public void refresh() {
        com.minepiece.essentials.telemetry.Telemetry.feature("boss_refresh");
        refreshPending = true;
        manualRequested = true;
    }

    // Vrai quand le relevé en attente vient du bouton ⟳ : il passe outre l'interrupteur global.
    private boolean manualRequested = false;

    /** Conservé pour le HUD : /boss renvoie toutes les îles, un refresh suffit. */
    public void refreshIsland(Island island) { refresh(); }

    public void refreshAllIslands() { refresh(); }

    public void cancelRefreshQueue() { refreshPending = false; manualRequested = false; }

    public void onConnectionChange() {
        refreshPending = false;
        initialScanDone = false;
        wasConnected = false;
        lastAutoAttemptMillis = 0L;
        expiryRefreshAt = 0L;
        previouslyAvailable.clear();
        BossAlertManager.getInstance().reset();
    }

    public boolean isRefreshPending() { return refreshPending; }
    public int getQueueSize() { return refreshPending ? 1 : 0; }
    public boolean isInQueue(Island island) { return refreshPending; }

    /** Estimated seconds until the pending refresh completes. */
    public int getEtaSeconds() {
        if (!refreshPending) return 0;
        long cooldownRemaining = BackgroundGuiRefresh.getCooldownRemainingMs();
        return (int) Math.ceil((REFRESH_SECONDS * 1000L + cooldownRemaining) / 1000.0);
    }

    /** @return true si la commande est partie. */
    private boolean doRefresh() {
        boolean sent = BackgroundGuiRefresh.sendCommand(BOSS_COMMAND, items -> {
            MinepieceEssentialsClient.LOGGER.info("[BossTracker] /boss screen: {} items", items.size());
            applyScreen(items);
        }, manualRequested);
        if (sent) {
            manualRequested = false;
            MinepieceEssentialsClient.LOGGER.info("[BossTracker] Refresh via {}", BOSS_COMMAND);
        }
        return sent;
    }

    private void applyScreen(Map<Integer, ItemStack> items) {
        List<BossScreenParser.Entry> entries = new ArrayList<>();
        for (ItemStack stack : items.values()) {
            if (stack == null || stack.isEmpty()) continue;
            String name = stack.getHoverName().getString();
            if (name.isEmpty()) continue;
            List<String> lore = new ArrayList<>();
            ItemLore lc = stack.get(DataComponents.LORE);
            if (lc != null) {
                for (Component line : lc.lines()) lore.add(line.getString());
            }
            entries.add(new BossScreenParser.Entry(name, lore));
        }

        Map<Island, List<BossData>> parsed = BossScreenParser.parse(entries);
        if (parsed.isEmpty()) {
            MinepieceEssentialsClient.LOGGER.warn("[BossTracker] /boss screen not recognised ({} items) — keeping previous data", items.size());
            return;
        }
        for (Map.Entry<Island, List<BossData>> e : parsed.entrySet()) {
            bossMap.put(e.getKey(), e.getValue());
            saveBossData(e.getKey(), e.getValue());
        }
        lastRefreshMillis = System.currentTimeMillis();
        MinepieceEssentialsClient.LOGGER.info("[BossTracker] {} islands, {} bosses updated",
            parsed.size(), parsed.values().stream().mapToInt(List::size).sum());
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
