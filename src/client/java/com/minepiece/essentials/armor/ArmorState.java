package com.minepiece.essentials.armor;

import com.minepiece.essentials.pet.PetStat;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/** Latest snapshot of the worn MinePiece armor and its summed stat bonuses. */
public final class ArmorState {

    /** A worn piece with the stack to draw as icon. */
    public record WornPiece(ItemStack stack, ArmorPiece piece) {}

    /**
     * @param setName   set shared by every worn piece, null if mixed or none
     * @param fullSet   true when all four slots hold pieces of {@code setName}
     * @param setEffect full-set effect text (from any piece), empty if none
     */
    public record Snapshot(List<WornPiece> pieces, Map<PetStat, Double> totals, Map<PetStat, String> labels,
                           String setName, boolean fullSet, List<String> setEffect) {
        public boolean isEmpty() { return pieces.isEmpty(); }
    }

    public static final Snapshot EMPTY = new Snapshot(List.of(), Map.of(), Map.of(), null, false, List.of());

    private static volatile Snapshot current = EMPTY;

    private ArmorState() {}

    public static Snapshot get() { return current; }

    public static void set(Snapshot s) { current = s; }
}
