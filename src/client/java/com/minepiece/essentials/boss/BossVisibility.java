package com.minepiece.essentials.boss;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.island.Island;
import java.util.HashSet;
import java.util.Set;

/** Bosses the player chose to hide (Shift+clic in the HUD editor); persisted in the config. */
public final class BossVisibility {
    private BossVisibility() {}

    public static String key(Island island, BossData boss) {
        return island.id + ":" + boss.name;
    }

    private static Set<String> hiddenSet() {
        var config = MinepieceEssentialsClient.getInstance().getConfigManager().config();
        if (config.hiddenBosses == null) config.hiddenBosses = new HashSet<>();
        return config.hiddenBosses;
    }

    public static boolean isHidden(Island island, BossData boss) {
        return hiddenSet().contains(key(island, boss));
    }

    public static void toggle(Island island, BossData boss) {
        Set<String> hidden = hiddenSet();
        String k = key(island, boss);
        if (!hidden.remove(k)) hidden.add(k);
        MinepieceEssentialsClient.getInstance().getConfigManager().save();
    }
}
