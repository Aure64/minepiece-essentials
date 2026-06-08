package com.minepiece.essentials.quest;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.i18n.ServerText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ParcheminScanner {
    private List<QuestInfo> activeQuests = new ArrayList<>();
    // Match (0/1133) or similar
    private static final Pattern PROGRESS_PATTERN =
        Pattern.compile("\\((\\d+)/(\\d+)\\)");
    // EXPIRE_PATTERN moved to ServerText.EXPIRE (bilingual: "Expire le" / "Expires on")
    private static final DateTimeFormatter EXPIRE_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy H'h'mm");

    private long lastLogTime = 0;

    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        List<QuestInfo> found = new ArrayList<>();
        var inventory = client.player.getInventory();

        boolean shouldLog = System.currentTimeMillis() - lastLogTime > 30000;
        if (shouldLog) lastLogTime = System.currentTimeMillis();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;

            String name = stack.getName().getString();

            if (!ServerText.matches(name, ServerText.SCROLL_NAME)) continue;

            if (shouldLog) {
                MinepieceEssentialsClient.LOGGER.info("[ParcheminScan] Found parchemin: '{}' in slot {}", name, i);
            }

            QuestInfo quest = parseParchemin(stack, shouldLog);
            if (quest != null) {
                found.add(quest);
            }
        }

        activeQuests = found;
    }

    private QuestInfo parseParchemin(ItemStack stack, boolean shouldLog) {
        QuestInfo quest = new QuestInfo();
        quest.name = stack.getName().getString();

        // Detect rarity from name
        if (ServerText.matches(quest.name, ServerText.LUNAR)) {
            quest.rarity = "LUNAIRE";
        } else {
            // Detect rarity from Unicode prefix character in name
            // 伴 = Légendaire, 孔 = Commun, etc.
            // Also detect from lore Unicode markers:
            // 伴叹 = LEGENDAIRE, 孔宜 = COMMUN, 灰捕 = LUNAIRE
            // 恨繁 = EPIQUE, 桥淡 = RARE, 愈潮 = MYTHIQUE
            String rawName = quest.name;
            if (rawName.contains("\u4f34")) quest.rarity = "LEGENDAIRE";      // 伴
            else if (rawName.contains("\u5b54")) quest.rarity = "COMMUN";      // 孔
            else if (rawName.contains("\u7070")) quest.rarity = "LUNAIRE";     // 灰
            else if (rawName.contains("\u6068")) quest.rarity = "EPIQUE";      // 恨
            else if (rawName.contains("\u6865")) quest.rarity = "RARE";        // 桥
            else if (rawName.contains("\u6108")) quest.rarity = "MYTHIQUE";    // 愈
        }

        if (shouldLog) {
            MinepieceEssentialsClient.LOGGER.info("[ParcheminScan] Parchemin '{}' -> rarity={}", quest.name, quest.rarity);
        }

        var tooltip = stack.getTooltip(net.minecraft.item.Item.TooltipContext.DEFAULT,
                null, net.minecraft.item.tooltip.TooltipType.BASIC);

        for (Text text : tooltip) {
            String line = text.getString();

            if (shouldLog) {
                MinepieceEssentialsClient.LOGGER.info("[ParcheminScan]   lore: '{}'", line);
            }

            // Progress: (0/1133)
            Matcher progressMatch = PROGRESS_PATTERN.matcher(line);
            if (progressMatch.find()) {
                quest.current = Integer.parseInt(progressMatch.group(1));
                quest.target = Integer.parseInt(progressMatch.group(2));
            }

            // Expiration date: "Expire le 23/03/2026 11h04" / "Expires on 23/03/2026 11h04"
            Matcher expireMatch = ServerText.EXPIRE.matcher(line);
            if (expireMatch.find()) {
                try {
                    LocalDateTime expireDate = LocalDateTime.parse(expireMatch.group(1), EXPIRE_FORMAT);
                    long secondsRemaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), expireDate);
                    quest.remainingSeconds = (int) Math.max(0, secondsRemaining);
                    quest.timerReadTime = System.currentTimeMillis();
                } catch (Exception e) {
                    // ignore parse errors
                }
            }

            // Rarity detection from lore Unicode markers
            if (line.contains("\u4f34\u53f9")) quest.rarity = "LEGENDAIRE";     // 伴叹
            else if (line.contains("\u5b54\u5b9c")) quest.rarity = "COMMUN";    // 孔宜
            else if (line.contains("\u7070\u6355")) quest.rarity = "LUNAIRE";   // 灰捕
            else if (line.contains("\u6068\u7e41")) quest.rarity = "EPIQUE";    // 恨繁
            else if (line.contains("\u6865\u6de1")) quest.rarity = "RARE";      // 桥淡
            else if (line.contains("\u6108\u6f6e")) quest.rarity = "MYTHIQUE";  // 愈潮

            // Objective: any line containing (X/Y) progress pattern is the objective.
            // Strip the trailing "(X/Y)" — the counter is rendered separately, so
            // keeping it here just wastes width and forces the text to be truncated.
            if (quest.objective == null && PROGRESS_PATTERN.matcher(line).find()) {
                quest.objective = PROGRESS_PATTERN.matcher(line).replaceAll("").trim();
            }
            // Also catch "Objectif:" label line
            if (ServerText.matches(line, ServerText.OBJECTIVE) && !line.contains("(")) {
                // Next line with progress will be caught above
            }
        }

        // If we found progress but no objective, use the name as objective
        if (quest.objective == null && quest.target > 0) {
            quest.objective = quest.name;
        }

        return quest.objective != null ? quest : null;
    }

    public List<QuestInfo> getActiveQuests() { return activeQuests; }

    public static class QuestInfo {
        public String name;
        public String objective;
        public String rarity = "COMMUN";
        public int current;
        public int target;
        public int remainingSeconds;
        public long timerReadTime;

        public int getRarityColor() {
            return switch (rarity) {
                case "RARE" -> 0xFF5555FF;
                case "EPIQUE" -> 0xFFAA00AA;
                case "LEGENDAIRE" -> 0xFFFFAA00;
                case "MYTHIQUE" -> 0xFFFF5555;
                case "LUNAIRE" -> 0xFFFF3355;  // Rouge/rose comme le fond du parchemin lunaire
                case "SPECIAL" -> 0xFFFF66AA;  // Rose vif pour les events
                default -> 0xFF55FF55; // COMMUN = green
            };
        }

        public int estimateRemainingSeconds() {
            if (timerReadTime == 0) return remainingSeconds;
            long elapsed = (System.currentTimeMillis() - timerReadTime) / 1000;
            return Math.max(0, remainingSeconds - (int) elapsed);
        }

        public float progress() {
            if (target <= 0) return 0;
            return (float) current / target;
        }

        public boolean isCompleted() { return current >= target; }
    }
}
