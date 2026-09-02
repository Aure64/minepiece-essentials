package com.minepiece.essentials.ah;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.i18n.ServerText;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Appends per-unit price lines to an auction-house item tooltip (stacks &gt; 1):
 * {@code " ▪ Prix/u: 1M 实"} under "Prix de vente" and {@code "Prix moyen/u"} under
 * "Prix moyen". The berry coin is the server's currency glyph (U+5B9E) rendered
 * with the {@code fonts:icons} font, kept white so it shows in its natural colour.
 */
public final class AhTooltip {

    private static final String BERRY = "实";                       // currency glyph
    private static final Identifier BERRY_FONT = Identifier.fromNamespaceAndPath("fonts", "icons");
    private static final int LABEL_COLOR = 0xFFFFD24B;             // gold

    private AhTooltip() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> annotate(stack, lines));
    }

    private static void annotate(ItemStack stack, List<Component> lines) {
        if (!ServerDetector.isOnMinePiece()) return;
        var cfg = MinepieceEssentialsClient.getInstance().getConfigManager().config();

        // Snapshot du lore AVANT toute insertion (les ajouts ci-dessous décalent les index).
        List<String> strings = new ArrayList<>(lines.size());
        for (Component t : lines) strings.add(t.getString());

        // 1) Prix par unité (piles > 1).
        int count = stack.getCount();
        if (cfg.ahPricePerUnitEnabled && count > 1) {
            Optional<String> sell = AhPriceParser.perUnit(strings, count, ServerText.SELL_PRICE);
            Optional<String> avg = AhPriceParser.perUnit(strings, count, ServerText.AVG_PRICE);
            int sellIdx = indexOf(strings, ServerText.SELL_PRICE);
            int avgIdx = indexOf(strings, ServerText.AVG_PRICE);
            // Insert the lower line first so the earlier index stays valid.
            if (avg.isPresent()) {
                Component line = perUnitLine("Prix moyen/u: ", avg.get());
                if (avgIdx >= 0) lines.add(avgIdx + 1, line); else lines.add(line);
                com.minepiece.essentials.telemetry.Telemetry.feature("ah_price");
            }
            if (sell.isPresent()) {
                Component line = perUnitLine("Prix/u: ", sell.get());
                if (sellIdx >= 0) lines.add(sellIdx + 1, line); else lines.add(line);
                com.minepiece.essentials.telemetry.Telemetry.feature("ah_price");
            }
        }

        // 2) Écart vs prix moyen (couleur). Ajouté en fin d'infobulle.
        if (cfg.ahPriceColorEnabled) {
            AhPriceBand.fromLore(strings).ifPresent(res -> {
                lines.add(bandLine(res));
                com.minepiece.essentials.telemetry.Telemetry.feature("ah_price");
            });
        }
    }

    /** Ligne « ▪ ▲ +15 % vs moyenne », colorée selon la bande. */
    private static Component bandLine(AhPriceBand.Result res) {
        int pct = res.percent();
        String head = pct > 0 ? "▲ +" : pct < 0 ? "▼ " : "● ";
        return Component.literal(" ▪ " + head + pct + " % vs moyenne").withColor(res.band().color);
    }

    private static Component perUnitLine(String label, String value) {
        return Component.empty()
                .append(Component.literal(" ▪ " + label + value + " ").withColor(LABEL_COLOR))
                .append(Component.literal(BERRY).withColor(0xFFFFFF)
                        .withStyle(s -> s.withFont(new FontDescription.Resource(BERRY_FONT))));
    }

    private static int indexOf(List<String> lines, String[] variants) {
        for (int i = 0; i < lines.size(); i++)
            if (ServerText.matches(lines.get(i), variants)) return i;
        return -1;
    }
}
