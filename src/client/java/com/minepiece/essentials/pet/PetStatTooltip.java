package com.minepiece.essentials.pet;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.i18n.ServerText;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

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

    private static void annotate(ItemStack stack, List<Text> lines) {
        if (!stack.isOf(Items.RABBIT_FOOT)) return;
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
        }
    }

    private static Rarity readRarity(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return null;
        Matcher m = RARITY_TRACK.matcher(data.copyNbt().toString());
        return m.find() ? Rarity.fromTrack(m.group(1)) : null;
    }

    private static MutableText withQuality(Text line, double quality) {
        int percent = (int) Math.round(quality * 100);
        return Text.empty()
            .append(line)
            .append(Text.literal(" (" + percent + "%)").withColor(QualityColor.of(quality)));
    }

    private static int indexOfSection(List<Text> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (ServerText.matches(lines.get(i).getString(), ServerText.PET_EFFECTS)) return i;
        }
        return -1;
    }

    private static int indexOfLine(List<Text> lines, String needle) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getString().contains(needle)) return i;
        }
        return -1;
    }
}
