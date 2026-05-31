package com.minepiece.essentials.pet;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ModConstants;
import com.minepiece.essentials.util.JsonHelper;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Persisted, auto-learned table of how much XP one unit of each minion resource
 * is worth (item id → xp per item). Populated from the feeding screen as the
 * player plays, and read by {@link MinionTooltip} to convert XP into stacks.
 */
public final class ResourceXpStore {

    private static final class Data {
        Map<String, Double> resources = new HashMap<>();
    }

    private static ResourceXpStore instance;

    private final Path path;
    private final Data data;

    private ResourceXpStore() {
        this.path = FabricLoader.getInstance().getConfigDir()
            .resolve(ModConstants.CONFIG_DIR).resolve("minion-resource-xp.json");
        Data loaded = JsonHelper.load(path, Data.class, new Data());
        if (loaded.resources == null) loaded.resources = new HashMap<>();
        this.data = loaded;
    }

    public static ResourceXpStore get() {
        if (instance == null) instance = new ResourceXpStore();
        return instance;
    }

    public OptionalDouble xpPerItem(String itemId) {
        Double v = itemId == null ? null : data.resources.get(itemId);
        return v == null ? OptionalDouble.empty() : OptionalDouble.of(v);
    }

    /** Records a learned ratio, persisting only when it is new or changed. */
    public void record(String itemId, double xpPerItem) {
        if (itemId == null || xpPerItem <= 0) return;
        Double current = data.resources.get(itemId);
        if (current != null && Math.abs(current - xpPerItem) < 1e-9) return;
        data.resources.put(itemId, xpPerItem);
        JsonHelper.save(path, data);
        MinepieceEssentialsClient.LOGGER.info("[Minion] Learned {} = {} XP/item", itemId, xpPerItem);
    }
}
