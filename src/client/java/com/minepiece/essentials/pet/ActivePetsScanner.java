package com.minepiece.essentials.pet;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.i18n.ServerText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/**
 * Scans the open /pets screen and publishes the combat-stat total of the
 * <em>currently active</em> pets (those whose tooltip offers "Désactiver") to
 * {@link ActivePetsState}.
 *
 * <p>The active set is <b>replaced</b> on every scan that finds active pets, so
 * deactivating or swapping pets is reflected immediately and stale pets are
 * never kept. Pages with no active pets leave the last total untouched.
 */
public final class ActivePetsScanner {

    private static final String SECTION_END = "Minion Effects";
    private static final int SCAN_INTERVAL = 4; // ticks

    private static final Pattern RARITY_TRACK =
        Pattern.compile("tracks\\.==(COMMON|RARE|EPIC|LEGENDARY|MYTHIC)");
    private static final int MAX_LEVEL = 20;
    private static final int DEFAULT_COLOR = 0xFFFFE9D5;

    private static int ticks;
    private static List<String> lastLoggedNames = List.of();

    private ActivePetsScanner() {}

    public static void tick() {
        if (ticks++ % SCAN_INTERVAL != 0) return;

        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() instanceof AbstractContainerScreen<?> screen && ServerDetector.isOnMinePiece()) {
            scan(screen, client);
        }
    }

    private static void scan(AbstractContainerScreen<?> screen, Minecraft client) {
        boolean isPetsScreen = false;
        List<ActivePetsState.ActivePet> activePets = new ArrayList<>();
        List<PetEffect> allStats = new ArrayList<>();

        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (!stack.is(Items.RABBIT_FOOT)) continue;

            List<String> tip = tooltipLines(stack, client);
            if (containsAny(tip, ServerText.PET_ACTIVE_ACTION) || containsAny(tip, ServerText.PET_INACTIVE_ACTION)) {
                isPetsScreen = true;
            }
            if (!containsAny(tip, ServerText.PET_ACTIVE_ACTION)) continue;

            String nbt = nbt(stack);
            int color = nameColor(stack.getHoverName());
            if (color == 0) color = rarityColor(nbt); // fallback when the name has no explicit colour
            activePets.add(new ActivePetsState.ActivePet(stack.getHoverName().getString(), color, levelOf(tip)));
            allStats.addAll(combatStats(tip));
        }

        // Not the /pets screen, or a page with no active pets → keep the last total.
        if (!isPetsScreen || activePets.isEmpty()) return;

        ActivePetsState.set(new ActivePetsState.Snapshot(activePets, PetStatSum.sum(allStats), PetStatSum.labels(allStats)));

        List<String> names = activePets.stream().map(ActivePetsState.ActivePet::name).toList();
        if (!names.equals(lastLoggedNames)) {
            lastLoggedNames = names;
            MinepieceEssentialsClient.LOGGER.info("[ActivePets] {} actif(s): {}", names.size(), names);
        }
    }

    private static String nbt(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().toString();
    }

    /** First explicit colour in the pet's name Text, as ARGB; 0 if none. */
    private static int nameColor(Component name) {
        int[] found = {0};
        name.visit((style, str) -> {
            TextColor c = style.getColor();
            if (c != null && found[0] == 0) found[0] = 0xFF000000 | c.getValue();
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    /** Fallback colour derived from the rarity token in NBT. */
    private static int rarityColor(String nbt) {
        Matcher m = RARITY_TRACK.matcher(nbt);
        if (m.find()) {
            Rarity rarity = Rarity.fromTrack(m.group(1));
            if (rarity != null) return rarity.color();
        }
        return DEFAULT_COLOR;
    }

    /** Pet level from the tooltip "Niveau:"/"Level:" line; "Max" → {@link #MAX_LEVEL}, 0 if absent. */
    private static int levelOf(List<String> tip) {
        for (String line : tip) {
            Matcher m = ServerText.PET_LEVEL.matcher(line);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException e) {
                    return MAX_LEVEL; // "Max"
                }
            }
        }
        return 0;
    }

    private static List<PetEffect> combatStats(List<String> lines) {
        int start = -1;
        int end = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            if (ServerText.matches(lines.get(i), ServerText.PET_EFFECTS)) {
                start = i;
            } else if (start >= 0 && lines.get(i).contains(SECTION_END)) {
                end = i;
                break;
            }
        }
        List<PetEffect> out = new ArrayList<>();
        if (start < 0) return out;
        for (int i = start + 1; i < end; i++) {
            PetEffectParser.parse(lines.get(i)).ifPresent(out::add);
        }
        return out;
    }

    private static List<String> tooltipLines(ItemStack stack, Minecraft client) {
        List<String> out = new ArrayList<>();
        for (Component line : stack.getTooltipLines(Item.TooltipContext.EMPTY, client.player, TooltipFlag.NORMAL)) {
            out.add(line.getString());
        }
        return out;
    }

    private static boolean containsAny(List<String> lines, String[] variants) {
        for (String l : lines) if (ServerText.matches(l, variants)) return true;
        return false;
    }
}
