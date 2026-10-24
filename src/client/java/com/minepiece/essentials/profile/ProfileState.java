package com.minepiece.essentials.profile;

import java.util.Optional;

/** Latest {@code /profil} read, if any this session. */
public final class ProfileState {
    private static volatile ProfileData current;
    private static volatile long readAtMillis;

    private ProfileState() {}

    public static Optional<ProfileData> get() { return Optional.ofNullable(current); }
    public static long readAtMillis() { return readAtMillis; }

    public static void set(ProfileData data) {
        current = data;
        readAtMillis = System.currentTimeMillis();
    }

    public static void clear() { current = null; readAtMillis = 0; }
}
