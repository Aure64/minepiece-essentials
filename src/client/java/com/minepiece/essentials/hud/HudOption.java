package com.minepiece.essentials.hud;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Un interrupteur d'un panneau, affiché dans l'onglet Personnaliser de l'éditeur K.
 * Soit stocké dans le layout du panneau ({@link #layout}), soit lié à un champ de
 * la config ({@link #config}). Le rendu du panneau lit la valeur à chaque frame :
 * l'éditeur affiche donc l'aperçu en direct.
 */
public record HudOption(String key, String labelKey, BooleanSupplier get, Consumer<Boolean> set) {

    /** Option propre au panneau, sauvée dans son layout sous {@code key}. */
    public static HudOption layout(HudElement element, String key, String labelKey, boolean def) {
        return new HudOption(key, labelKey, () -> element.option(key, def), v -> element.setOption(key, v));
    }

    /** Option liée à un champ de la config globale. */
    public static HudOption config(String key, String labelKey, BooleanSupplier get, Consumer<Boolean> set) {
        return new HudOption(key, labelKey, get, set);
    }

    public boolean isOn() { return get.getAsBoolean(); }
    public void toggle() { set.accept(!isOn()); }
}
