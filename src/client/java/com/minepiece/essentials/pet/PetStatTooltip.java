package com.minepiece.essentials.pet;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.i18n.ServerText;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Appends a coloured roll-quality percentage to each "Familier Effects" stat
 * line in a pet's ({@code rabbit_foot}) tooltip on the MinePiece server.
 *
 * <p>Rarity comes from the item's {@code custom_data} NBT; the tier and value
 * are parsed from the rendered line ({@link PetEffectParser}). Minion effects
 * and off-table "special" rolls are left untouched.
 */
public final class PetStatTooltip {

    private static final Pattern RARITY_TRACK =
        Pattern.compile("tracks\\.==(COMMON|RARE|EPIC|LEGENDARY|MYTHIC)");
    private static final String SECTION_END = "Minion Effects";

    private PetStatTooltip() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> annotate(stack, lines));
    }

    private static void annotate(ItemStack stack, List<Component> lines) {
        if (!stack.is(Items.RABBIT_FOOT)) return;
        if (!ServerDetector.isOnMinePiece()) return;
        if (!MinepieceEssentialsClient.getInstance().getConfigManager().config().petStatQualityEnabled) {
            return;
        }

        Rarity rarity = readRarity(stack);
        if (rarity == null) return;

        int start = indexOfSection(lines);
        if (start < 0) return;
        int end = indexOfLine(lines, SECTION_END);
        if (end < 0) end = lines.size();

        for (int i = start + 1; i < end; i++) {
            PetEffect effect = PetEffectParser.parse(lines.get(i).getString()).orElse(null);
            if (effect == null) continue;

            OptionalDouble quality = PetStatEvaluator.quality(rarity, effect);
            if (quality.isEmpty()) continue;

            lines.set(i, withQuality(lines.get(i), quality.getAsDouble()));
            com.minepiece.essentials.telemetry.Telemetry.feature("pet_tooltip");
        }
    }

    private static Rarity readRarity(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        Matcher m = RARITY_TRACK.matcher(data.copyTag().toString());
        return m.find() ? Rarity.fromTrack(m.group(1)) : null;
    }

    private static MutableComponent withQuality(Component line, double quality) {
        int percent = (int) Math.round(quality * 100);
        return Component.empty()
            .append(line)
            .append(Component.literal(" (" + percent + "%)").withColor(QualityColor.of(quality)));
    }

    private static int indexOfSection(List<Component> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (ServerText.matches(lines.get(i).getString(), ServerText.PET_EFFECTS)) return i;
        }
        return -1;
    }

    private static int indexOfLine(List<Component> lines, String needle) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getString().contains(needle)) return i;
        }
        return -1;
    }
}
