package com.minepiece.essentials.config;

import java.util.HashMap;
import java.util.Map;

public class LayoutConfig {
    public String activeProfile = "default";
    public Map<String, Profile> profiles = new HashMap<>();

    public LayoutConfig() {
        profiles.put("default", new Profile());
    }

    public Profile activeProfile() {
        return profiles.computeIfAbsent(activeProfile, k -> new Profile());
    }

    public static class Profile {
        public Map<String, ElementLayout> elements = new HashMap<>();
    }

    public static class ElementLayout {
        public int x;
        public int y;
        public float scale = 1.0f;
        public boolean visible = true;
        public HudBackground background = HudBackground.PARCHMENT;
        // Miroir : icônes à droite, texte aligné à droite (panneau posé au bord droit de l'écran).
        public boolean mirrored = false;

        public ElementLayout() {}

        public ElementLayout(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
