package com.minepiece.essentials.armor;

import com.minepiece.essentials.pet.PetStat;
import com.minepiece.essentials.profile.ProfileScanner;
import com.minepiece.essentials.util.ItemText;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Reads the four worn armor slots of the local player and publishes their summed
 * stat bonuses to {@link ArmorState}. Purely local: no screen, no command. The lore
 * is only reparsed when a slot's stack instance or count changes.
 */
public final class ArmorScanner {

    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final int SCAN_INTERVAL = 4; // ticks

    private static int ticks;
    private static long lastFingerprint;

    private ArmorScanner() {}

    public static void tick() {
        if (ticks++ % SCAN_INTERVAL != 0) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            lastFingerprint = 0;
            ArmorState.set(ArmorState.EMPTY);
            return;
        }
        long fp = 1469598103934665603L;
        List<ItemStack> stacks = new ArrayList<>(SLOTS.length);
        for (EquipmentSlot slot : SLOTS) {
            ItemStack s = player.getItemBySlot(slot);
            stacks.add(s);
            int id = s.isEmpty() ? 0 : System.identityHashCode(s);
            fp = (fp ^ id) * 1099511628211L;
            fp = (fp ^ s.getCount()) * 1099511628211L;
        }
        if (fp == lastFingerprint) return;
        boolean first = lastFingerprint == 0;
        lastFingerprint = fp;
        ArmorState.set(scan(stacks));
        if (!first) ProfileScanner.refreshSoon(); // les stats globales ont bougé
    }

    static ArmorState.Snapshot scan(List<ItemStack> stacks) {
        List<ArmorState.WornPiece> pieces = new ArrayList<>();
        Map<PetStat, Double> totals = new EnumMap<>(PetStat.class);
        Map<PetStat, String> labels = new EnumMap<>(PetStat.class);
        String setName = null;
        boolean sameSet = true;
        List<String> setEffect = List.of();

        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) { sameSet = false; continue; }
            Optional<ArmorPiece> parsed = ArmorStatParser.parse(ItemText.nameAndLore(stack));
            if (parsed.isEmpty()) { sameSet = false; continue; }
            ArmorPiece p = parsed.get();
            pieces.add(new ArmorState.WornPiece(stack, p));
            p.stats().forEach((stat, v) -> totals.merge(stat, v, Double::sum));
            p.labels().forEach(labels::putIfAbsent);
            if (p.setName() == null) sameSet = false;
            else if (setName == null) setName = p.setName();
            else if (!setName.equalsIgnoreCase(p.setName())) sameSet = false;
            if (setEffect.isEmpty() && !p.setEffect().isEmpty()) setEffect = p.setEffect();
        }
        if (pieces.isEmpty()) return ArmorState.EMPTY;
        boolean fullSet = sameSet && pieces.size() == SLOTS.length && setName != null;
        return new ArmorState.Snapshot(List.copyOf(pieces), totals, labels, sameSet ? setName : null, fullSet, setEffect);
    }
}
