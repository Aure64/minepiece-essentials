package com.minepiece.essentials.boss;

import com.minepiece.essentials.i18n.ServerText;
import com.minepiece.essentials.island.Island;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Parse l'écran {@code /boss} de MinePiece : un item par île, dont le lore liste
 * chaque boss sous la forme
 * <pre>
 * ⚔ Luffy
 *  ▪ Coordonnées: 23 206 10987
 *  ▪ Apparition: 14m 40s (Toutes les 15 Minutes)
 * </pre>
 * Pur Java (aucun type Minecraft) pour rester testable.
 */
public final class BossScreenParser {
    private BossScreenParser() {}

    /** Un item de l'écran : son nom affiché et ses lignes de lore. */
    public record Entry(String name, List<String> lore) {}

    private static final String BOSS_MARKER = "⚔";

    /** Îles reconnues et leurs boss (seuls ceux avec coordonnées sont gardés). */
    public static Map<Island, List<BossData>> parse(Collection<Entry> entries) {
        Map<Island, List<BossData>> out = new LinkedHashMap<>();
        for (Entry entry : entries) {
            Island island = Island.fromScreenName(entry.name());
            if (island == Island.UNKNOWN) continue;
            List<BossData> bosses = parseIsland(island, entry.lore());
            if (!bosses.isEmpty()) out.put(island, bosses);
        }
        return out;
    }

    static List<BossData> parseIsland(Island island, List<String> lore) {
        List<BossData> bosses = new ArrayList<>();
        BossData current = null;
        long now = System.currentTimeMillis();
        for (String raw : lore) {
            String line = raw == null ? "" : raw.strip();
            if (line.startsWith(BOSS_MARKER)) {
                String name = line.substring(BOSS_MARKER.length()).strip();
                current = name.isEmpty() ? null : new BossData(name, island);
                if (current != null) {
                    current.type = "boss";
                    bosses.add(current);
                }
                continue;
            }
            if (current == null) continue;

            Matcher coords = ServerText.BOSS_COORDS.matcher(line);
            if (coords.find()) {
                current.x = Integer.parseInt(coords.group(1));
                current.y = Integer.parseInt(coords.group(2));
                current.z = Integer.parseInt(coords.group(3));
                current.hasCoords = true;
            }
            Matcher respawn = ServerText.BOSS_RESPAWN.matcher(line);
            if (respawn.find()) {
                int min = respawn.group(1) != null ? Integer.parseInt(respawn.group(1)) : 0;
                int sec = Integer.parseInt(respawn.group(2));
                current.lastKnownTimerSeconds = min * 60 + sec;
                current.lastKnownRespawnTimestamp = now;
            }
            Matcher interval = ServerText.BOSS_INTERVAL.matcher(line);
            if (interval.find()) {
                current.respawnIntervalSeconds = Integer.parseInt(interval.group(1)) * 60;
            }
        }
        bosses.removeIf(b -> !b.hasCoords);
        return bosses;
    }
}
