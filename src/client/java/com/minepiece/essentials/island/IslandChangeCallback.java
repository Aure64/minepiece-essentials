package com.minepiece.essentials.island;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@FunctionalInterface
public interface IslandChangeCallback {
    Event<IslandChangeCallback> EVENT = EventFactory.createArrayBacked(
        IslandChangeCallback.class,
        listeners -> (previous, current) -> {
            for (IslandChangeCallback listener : listeners) {
                listener.onIslandChanged(previous, current);
            }
        }
    );

    void onIslandChanged(Island previous, Island current);
}
