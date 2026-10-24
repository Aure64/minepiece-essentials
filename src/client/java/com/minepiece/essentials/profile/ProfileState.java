package com.minepiece.essentials.profile;

import com.minepiece.essentials.armor.ArmorState;
import com.minepiece.essentials.pet.ActivePetsState;
import com.minepiece.essentials.pet.PetStat;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Latest {@code /profil} read, plus the <em>base</em> of each stat: the server total minus
 * what the active pets and the worn armor granted at read time. Between two reads the
 * displayed total is recomputed locally as {@code base + current pets + current armor},
 * so swapping a piece or a pet shows up at once ({@link #total(PetStat)}).
 */
public final class ProfileState {
    private static volatile ProfileData current;
    private static volatile Map<PetStat, Double> base = Map.of();
    private static volatile long readAtMillis;

    private ProfileState() {}

    public static Optional<ProfileData> get() { return Optional.ofNullable(current); }
    public static long readAtMillis() { return readAtMillis; }

    public static void set(ProfileData data) {
        Map<PetStat, Double> pets = ActivePetsState.get().totals();
        Map<PetStat, Double> armor = ArmorState.get().totals();
        Map<PetStat, Double> b = new EnumMap<>(PetStat.class);
        data.byStat().forEach((stat, total) ->
            b.put(stat, total - pets.getOrDefault(stat, 0.0) - armor.getOrDefault(stat, 0.0)));
        base = b;
        current = data;
        readAtMillis = System.currentTimeMillis();
    }

    /** Current total for a known stat: base + what pets and armor grant right now. */
    public static double total(PetStat stat) {
        return base.getOrDefault(stat, 0.0)
            + ActivePetsState.get().totals().getOrDefault(stat, 0.0)
            + ArmorState.get().totals().getOrDefault(stat, 0.0);
    }

    public static void clear() { current = null; base = Map.of(); readAtMillis = 0; }
}
