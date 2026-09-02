package com.minepiece.essentials.boss;

import com.minepiece.essentials.MinepieceEssentialsClient;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Registers one SoundEvent per known boss name.
 * Boss names are normalized: lowercase, spaces → underscores.
 * Each maps to an OGG file at assets/minepiece-essentials/sounds/boss/<normalized>.ogg
 */
public class ModSounds {
    private static final Map<String, SoundEvent> BOSS_SOUNDS = new HashMap<>();

    // All known boss names from the MinePiece server
    private static final String[] KNOWN_BOSSES = {
        "luffy",
        "nightmare_luffy",
        "sabo",
        "trafalgar_law",
        "ace",
        "robin",
        "vander_decken_ix",
        "perona"
    };

    public static void register() {
        for (String bossKey : KNOWN_BOSSES) {
            Identifier id = Identifier.fromNamespaceAndPath("minepiece-essentials", "boss." + bossKey);
            SoundEvent event = SoundEvent.createVariableRangeEvent(id);
            Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
            BOSS_SOUNDS.put(bossKey, event);
            MinepieceEssentialsClient.LOGGER.info("[ModSounds] Registered boss sound: {}", id);
        }
    }

    /**
     * Get the SoundEvent for a boss by its display name (e.g. "Eustass Kid").
     * Returns null if no sound is registered for this boss.
     */
    public static SoundEvent getBossSound(String bossDisplayName) {
        String key = normalize(bossDisplayName);
        return BOSS_SOUNDS.get(key);
    }

    /** Normalize a boss display name to a sound key: lowercase, spaces → underscores */
    public static String normalize(String displayName) {
        return displayName.toLowerCase().trim().replace(" ", "_");
    }
}
