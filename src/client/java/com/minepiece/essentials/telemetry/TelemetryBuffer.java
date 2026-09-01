package com.minepiece.essentials.telemetry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * File d'attente des événements en instance d'envoi, plus la mémoire des features
 * déjà signalées dans la session courante.
 *
 * <p>Pure (aucun Minecraft, aucun réseau) pour rester testable. Synchronisée :
 * les événements sont produits par le thread client et consommés par le thread
 * d'envoi.
 */
public final class TelemetryBuffer {

    /** Au-delà, les plus anciens sont jetés : un joueur hors ligne ne doit pas gonfler la mémoire. */
    public static final int MAX_EVENTS = 100;

    private final Deque<TelemetryEvent> queue = new ArrayDeque<>();
    private final Set<String> seenFeatures = new HashSet<>();

    public synchronized void add(TelemetryEvent e) {
        while (queue.size() >= MAX_EVENTS) {
            queue.pollFirst();
        }
        queue.addLast(e);
    }

    /** {@code true} la première fois qu'une feature est vue dans cette session. */
    public synchronized boolean markFeatureSeen(String feature) {
        return seenFeatures.add(feature);
    }

    public synchronized List<TelemetryEvent> drain() {
        List<TelemetryEvent> out = new ArrayList<>(queue);
        queue.clear();
        return out;
    }

    public synchronized int size() { return queue.size(); }

    public synchronized void resetSession() { seenFeatures.clear(); }
}
