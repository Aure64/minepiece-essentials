package com.minepiece.essentials.telemetry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryEventTest {

    @Test
    void rejectsPropertyOutsideWhitelist() {
        assertThrows(IllegalArgumentException.class,
                () -> TelemetryEvent.of("mp_session_start", Map.of("player_name", "Aure64")),
                "une propriété hors liste blanche doit faire échouer la construction");
    }

    @Test
    void rejectsEventNameWithoutPrefix() {
        assertThrows(IllegalArgumentException.class,
                () -> TelemetryEvent.of("session_start", Map.of()),
                "tous les événements du mod doivent être préfixés mp_");
    }

    @Test
    void acceptsWhitelistedProperties() {
        TelemetryEvent e = TelemetryEvent.of("mp_feature_used", Map.of("feature", "boss_refresh"));
        assertEquals("mp_feature_used", e.name());
        assertEquals("boss_refresh", e.properties().get("feature"));
        assertTrue(e.timestamp().endsWith("Z"), "timestamp ISO-8601 UTC");
    }

    @Test
    void batchJsonCarriesApiKeyAndEveryEvent() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("mod_version", "1.7.2");
        props.put("on_minepiece", true);

        String json = TelemetryEvent.batchJson("phc_test", "install-42", List.of(
                TelemetryEvent.of("mp_session_start", props),
                TelemetryEvent.of("mp_feature_used", Map.of("feature", "hud_edit"))));

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("phc_test", root.get("api_key").getAsString());
        assertEquals(2, root.getAsJsonArray("batch").size());

        JsonObject first = root.getAsJsonArray("batch").get(0).getAsJsonObject();
        assertEquals("mp_session_start", first.get("event").getAsString());
        assertEquals("install-42", first.get("distinct_id").getAsString(),
                "distinct_id à la racine de l'item");
        assertEquals("install-42",
                first.getAsJsonObject("properties").get("distinct_id").getAsString(),
                "et aussi dans properties, pour couvrir les deux formes acceptées");
        assertEquals("1.7.2",
                first.getAsJsonObject("properties").get("mod_version").getAsString());
    }

    @Test
    void batchJsonSetsIpNullOnEveryItem() {
        String json = TelemetryEvent.batchJson("phc_test", "install-42", List.of(
                TelemetryEvent.of("mp_session_start", Map.of("mod_version", "1.7.2")),
                TelemetryEvent.of("mp_feature_used", Map.of("feature", "hud_edit"))));

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        for (var item : root.getAsJsonArray("batch")) {
            JsonObject props = item.getAsJsonObject().getAsJsonObject("properties");
            assertTrue(props.has("$ip"), "$ip doit être présent sur chaque item");
            assertTrue(props.get("$ip").isJsonNull(), "$ip doit être JSON null, jamais l'IP réelle");
        }
    }

    @Test
    void batchJsonDisablesGeoipOnEveryItem() {
        String json = TelemetryEvent.batchJson("phc_test", "install-42", List.of(
                TelemetryEvent.of("mp_session_start", Map.of("mod_version", "1.7.2")),
                TelemetryEvent.of("mp_feature_used", Map.of("feature", "hud_edit"))));

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        for (var item : root.getAsJsonArray("batch")) {
            JsonObject props = item.getAsJsonObject().getAsJsonObject("properties");
            assertTrue(props.has("$geoip_disable"), "$geoip_disable doit être présent sur chaque item");
            assertTrue(props.get("$geoip_disable").getAsBoolean(),
                    "$geoip_disable doit valoir true pour bloquer la géolocalisation par IP");
        }
    }

    @Test
    void setPropertyOnlyOnSessionStart() {
        String json = TelemetryEvent.batchJson("phc_test", "install-42", List.of(
                TelemetryEvent.of("mp_session_start", Map.of("mod_version", "1.7.2")),
                TelemetryEvent.of("mp_feature_used", Map.of("feature", "hud_edit"))));

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject sessionProps = root.getAsJsonArray("batch").get(0).getAsJsonObject().getAsJsonObject("properties");
        JsonObject featureProps = root.getAsJsonArray("batch").get(1).getAsJsonObject().getAsJsonObject("properties");
        assertTrue(sessionProps.has("$set"), "mp_session_start doit porter $set");
        assertFalse(featureProps.has("$set"), "mp_feature_used ne doit pas porter $set");
    }

    @Test
    void batchJsonNeverLeaksIdentity() {
        String json = TelemetryEvent.batchJson("phc_test", "install-42",
                List.of(TelemetryEvent.of("mp_feature_used", Map.of("feature", "help_screen"))));
        assertFalse(json.contains("Aure64"));
        assertFalse(json.toLowerCase().contains("username"));
    }
}
