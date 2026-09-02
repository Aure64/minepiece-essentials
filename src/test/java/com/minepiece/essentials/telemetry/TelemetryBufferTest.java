package com.minepiece.essentials.telemetry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryBufferTest {

    private static TelemetryEvent feature(String name) {
        return TelemetryEvent.of("mp_feature_used", Map.of("feature", name));
    }

    @Test
    void markFeatureSeenReturnsTrueOnlyOnce() {
        TelemetryBuffer buffer = new TelemetryBuffer();
        assertTrue(buffer.markFeatureSeen("boss_refresh"));
        assertFalse(buffer.markFeatureSeen("boss_refresh"));
        assertFalse(buffer.markFeatureSeen("boss_refresh"));
        assertTrue(buffer.markFeatureSeen("hud_edit"), "une autre feature reste comptée");
    }

    @Test
    void resetSessionForgetsSeenFeatures() {
        TelemetryBuffer buffer = new TelemetryBuffer();
        buffer.markFeatureSeen("boss_refresh");
        buffer.resetSession();
        assertTrue(buffer.markFeatureSeen("boss_refresh"), "nouvelle session, nouveau comptage");
    }

    @Test
    void dropsOldestEventsBeyondCapacity() {
        TelemetryBuffer buffer = new TelemetryBuffer();
        for (int i = 0; i < TelemetryBuffer.MAX_EVENTS + 10; i++) {
            buffer.add(feature("f" + i));
        }
        assertEquals(TelemetryBuffer.MAX_EVENTS, buffer.size(), "la file est bornée");

        List<TelemetryEvent> drained = buffer.drain();
        assertEquals("f10", drained.get(0).properties().get("feature"),
                "ce sont les plus ANCIENS qui sont jetés");
    }

    @Test
    void drainEmptiesTheBuffer() {
        TelemetryBuffer buffer = new TelemetryBuffer();
        buffer.add(feature("hud_edit"));
        assertEquals(1, buffer.drain().size());
        assertEquals(0, buffer.size());
        assertTrue(buffer.drain().isEmpty(), "vider deux fois de suite ne renvoie rien");
    }
}
