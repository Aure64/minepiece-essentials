package com.minepiece.essentials.telemetry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.minepiece.essentials.util.JsonHelper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Un événement de télémétrie et sa sérialisation au format PostHog.
 *
 * <p>Classe volontairement pure (aucun import Minecraft) : c'est elle qui porte
 * la garantie « rien d'identifiant ne part », et cette garantie doit être
 * vérifiable par un test unitaire.
 */
public final class TelemetryEvent {

    /** Préfixe imposé à tous les événements du mod, pour cohabiter avec les autres données du projet. */
    public static final String PREFIX = "mp_";

    /**
     * Seules propriétés autorisées. Toute autre clé fait échouer la construction :
     * c'est le garde-fou qui empêche un pseudo ou des coordonnées de partir par
     * inadvertance lors d'une modification future.
     */
    public static final Set<String> ALLOWED_PROPERTIES = Set.of(
            "mod_version", "mc_version", "loader_version", "java_version",
            "os", "client_language", "on_minepiece", "feature");

    private final String name;
    private final Map<String, Object> properties;
    private final String timestamp;

    private TelemetryEvent(String name, Map<String, Object> properties, String timestamp) {
        this.name = name;
        this.properties = properties;
        this.timestamp = timestamp;
    }

    public static TelemetryEvent of(String name, Map<String, Object> props) {
        if (name == null || !name.startsWith(PREFIX)) {
            throw new IllegalArgumentException("nom d'événement non préfixé " + PREFIX + " : " + name);
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : props.entrySet()) {
            if (!ALLOWED_PROPERTIES.contains(e.getKey())) {
                throw new IllegalArgumentException("propriété hors liste blanche : " + e.getKey());
            }
            copy.put(e.getKey(), e.getValue());
        }
        return new TelemetryEvent(name, copy,
                Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
    }

    public String name() { return name; }
    public Map<String, Object> properties() { return Map.copyOf(properties); }
    public String timestamp() { return timestamp; }

    /** Corps JSON de POST /batch/. */
    public static String batchJson(String apiKey, String distinctId, List<TelemetryEvent> events) {
        JsonArray batch = new JsonArray();
        for (TelemetryEvent e : events) {
            JsonObject props = new JsonObject();
            // distinct_id est accepté à la racine de l'item comme dans properties ;
            // on met les deux pour ne dépendre d'aucune des deux formes.
            props.addProperty("distinct_id", distinctId);
            for (Map.Entry<String, Object> p : e.properties.entrySet()) {
                Object v = p.getValue();
                if (v instanceof Boolean b) props.addProperty(p.getKey(), b);
                else if (v instanceof Number n) props.addProperty(p.getKey(), n);
                else props.addProperty(p.getKey(), String.valueOf(v));
            }
            // $set : les mêmes valeurs deviennent des propriétés de personne, ce qui
            // permet de répondre à « combien de joueurs sont ACTUELLEMENT en 1.6.x ».
            if (e.name.equals("mp_session_start")) {
                JsonObject set = new JsonObject();
                for (Map.Entry<String, Object> p : e.properties.entrySet()) {
                    Object v = p.getValue();
                    if (v instanceof Boolean b) set.addProperty(p.getKey(), b);
                    else set.addProperty(p.getKey(), String.valueOf(v));
                }
                props.add("$set", set);
            }

            JsonObject item = new JsonObject();
            item.addProperty("event", e.name);
            item.addProperty("distinct_id", distinctId);
            item.add("properties", props);
            item.addProperty("timestamp", e.timestamp);
            batch.add(item);
        }

        JsonObject root = new JsonObject();
        root.addProperty("api_key", apiKey);
        root.add("batch", batch);
        return JsonHelper.gson().toJson(root);
    }
}
