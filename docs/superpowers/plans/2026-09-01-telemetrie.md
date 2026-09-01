# Télémétrie anonyme — plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mesurer l'audience réelle du mod (joueurs actifs, rétention, adoption des versions, usage des features) via une télémétrie anonyme, désactivable, sans backend à héberger.

**Architecture:** Quatre classes dans un nouveau package `telemetry/`. Trois sont **pures** (aucune dépendance Minecraft) et donc testables unitairement : `TelemetryEvent` (données + JSON), `TelemetryBuffer` (file bornée + dédoublonnage par session), `TelemetryId` (identité anonyme persistée, chemin injecté). La quatrième, `Telemetry`, est la seule à toucher Minecraft et le réseau : elle colle les trois autres ensemble et envoie en HTTP sur un thread daemon.

**Tech Stack:** Java 21, `java.net.http.HttpClient` du JDK (aucune dépendance ajoutée), Gson (déjà présent via `util/JsonHelper`), JUnit 5 (déjà configuré), PostHog Cloud EU.

**Spec:** `docs/superpowers/specs/2026-09-01-telemetrie-design.md`

> **Écarts assumés vs la spec :**
> 1. La spec annonce 3 classes ; ce plan en fait 4. `TelemetryBuffer` est extrait
>    de `Telemetry` pour que la file et le dédoublonnage soient testables sans
>    Minecraft.
> 2. La spec liste « opt-out désactivé ⇒ la file reste vide » parmi les tests
>    unitaires. Ce cas passe par `ConfigManager`, qui a besoin de `FabricLoader` :
>    il n'est pas testable hors du jeu. Il est donc vérifié **en jeu** (Task 8,
>    étape 4) au lieu de `src/test`. Les trois autres tests de la spec sont bien
>    unitaires.

## Global Constraints

- **Projet PostHog :** cloud **EU**, projet id `198863`, token `phc_nfmqDHNYpEDmGAvnb62NS8SodTa6W73QZAbjSFkSVbRj` (write-only, destiné à être embarqué dans un client).
- **Endpoints :** `https://eu.i.posthog.com/i/v0/e/` (unitaire), `https://eu.i.posthog.com/batch/` (lot).
- **Préfixe d'événements obligatoire :** `mp_`. Les deux seuls événements sont `mp_session_start` et `mp_feature_used`.
- **Liste blanche de propriétés, exhaustive :** `mod_version`, `mc_version`, `loader_version`, `java_version`, `os`, `client_language`, `on_minepiece`, `feature`. Toute autre clé doit être rejetée.
- **Jamais envoyé :** pseudo ou UUID Minecraft, IP, chat, coordonnées, inventaire, adresse serveur.
- **Aucune dépendance ajoutée** à `build.gradle`.
- **Jamais bloquant :** thread daemon, timeout 8 s, échec silencieux en `LOGGER.debug`, aucun retry.
- **Opt-out :** `ModConfig.telemetryEnabled`, défaut `true`.
- **Style de code :** commentaires en français comme le reste du repo, `///`-free, `LOGGER` = `MinepieceEssentialsClient.LOGGER`.
- **Libellés de l'éditeur K sans accents** (les clés `minepiece.ui.editor.*` existantes sont toutes en ASCII : « Raretes », « Emblemes » — s'y conformer).
- **Port 1.21.8 :** tout le code de `telemetry/` est identique entre les deux versions ; seul `HudEditScreen` diverge (signature de `mouseClicked`) et doit être modifié à la main.

---

### Task 1 : Valider le format du payload PostHog (sonde jetable)

But : ne pas coder le sérialiseur Java sur une supposition. La doc en ligne est tronquée sur le format `/batch/`.

**Files:** aucun (sonde en ligne de commande, rien n'est commité).

**Interfaces:**
- Consumes: rien.
- Produces: la forme exacte du corps `/batch/` confirmée, notamment l'emplacement de `distinct_id` et la syntaxe de `$set`. Les tâches 2 et 5 s'y conforment.

- [ ] **Step 1: Envoyer un événement unitaire**

```bash
curl -s -X POST https://eu.i.posthog.com/i/v0/e/ \
  -H 'Content-Type: application/json' \
  -d '{"api_key":"phc_nfmqDHNYpEDmGAvnb62NS8SodTa6W73QZAbjSFkSVbRj",
       "event":"mp_probe",
       "distinct_id":"probe-install-1",
       "properties":{"mod_version":"probe","$set":{"mod_version":"probe"}},
       "timestamp":"'"$(date -u +%Y-%m-%dT%H:%M:%SZ)"'"}'
```

Attendu : `{"status":"Ok"}` ou `{"status":1}`.

- [ ] **Step 2: Envoyer un lot de deux événements**

```bash
curl -s -X POST https://eu.i.posthog.com/batch/ \
  -H 'Content-Type: application/json' \
  -d '{"api_key":"phc_nfmqDHNYpEDmGAvnb62NS8SodTa6W73QZAbjSFkSVbRj",
       "batch":[
         {"event":"mp_probe","distinct_id":"probe-install-1",
          "properties":{"distinct_id":"probe-install-1","feature":"probe_a"},
          "timestamp":"2026-09-01T12:00:00Z"},
         {"event":"mp_probe","distinct_id":"probe-install-1",
          "properties":{"distinct_id":"probe-install-1","feature":"probe_b"},
          "timestamp":"2026-09-01T12:00:01Z"}]}'
```

Note : `distinct_id` est volontairement présent **aux deux emplacements** (racine de l'item et dans `properties`). PostHog accepte les deux formes pour l'endpoint unitaire ; les mettre toutes les deux supprime le risque.

- [ ] **Step 3: Vérifier que les 3 événements sont bien arrivés**

Via l'accès PostHog de la session (l'ingestion prend quelques secondes) :

```
call read-data-schema {"query": {"kind": "events"}}
```

Attendu : `mp_probe` apparaît dans la liste. Puis vérifier les propriétés :

```
call read-data-schema {"query": {"kind": "event_properties", "event_name": "mp_probe"}}
```

Attendu : `feature` et `mod_version` présents.

- [ ] **Step 4: Noter le format retenu**

Si l'une des deux formes échoue, **corriger les Global Constraints de ce plan** avant de continuer. Aucun commit à cette étape.

> Les événements `mp_probe` resteront dans le projet. Ils sont inoffensifs (nom distinct des deux vrais événements) et servent de trace de la validation.

---

### Task 2 : `TelemetryEvent` — données et sérialisation

**Files:**
- Create: `src/client/java/com/minepiece/essentials/telemetry/TelemetryEvent.java`
- Test: `src/test/java/com/minepiece/essentials/telemetry/TelemetryEventTest.java`

**Interfaces:**
- Consumes: rien (classe pure, aucun import Minecraft).
- Produces:
  - `TelemetryEvent.of(String name, Map<String,Object> props)` → `TelemetryEvent`, lève `IllegalArgumentException` si une clé est hors liste blanche ou si le nom ne commence pas par `mp_`.
  - `String name()`, `Map<String,Object> properties()`, `String timestamp()`
  - `static String batchJson(String apiKey, String distinctId, List<TelemetryEvent> events)` → le corps JSON de `/batch/`
  - `static final Set<String> ALLOWED_PROPERTIES`

- [ ] **Step 1: Écrire les tests qui échouent**

```java
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
    void batchJsonNeverLeaksIdentity() {
        String json = TelemetryEvent.batchJson("phc_test", "install-42",
                List.of(TelemetryEvent.of("mp_feature_used", Map.of("feature", "help_screen"))));
        assertFalse(json.contains("Aure64"));
        assertFalse(json.toLowerCase().contains("username"));
    }
}
```

- [ ] **Step 2: Lancer les tests et vérifier qu'ils échouent**

Run: `./gradlew test --tests '*TelemetryEventTest*'`
Expected: FAIL — la classe `TelemetryEvent` n'existe pas (erreur de compilation).

- [ ] **Step 3: Écrire l'implémentation minimale**

```java
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
```

- [ ] **Step 4: Lancer les tests et vérifier qu'ils passent**

Run: `./gradlew test --tests '*TelemetryEventTest*'`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add src/client/java/com/minepiece/essentials/telemetry/TelemetryEvent.java \
        src/test/java/com/minepiece/essentials/telemetry/TelemetryEventTest.java
git commit -m "feat(telemetry): evenement et serialisation PostHog avec liste blanche"
```

---

### Task 3 : `TelemetryBuffer` — file bornée et dédoublonnage

**Files:**
- Create: `src/client/java/com/minepiece/essentials/telemetry/TelemetryBuffer.java`
- Test: `src/test/java/com/minepiece/essentials/telemetry/TelemetryBufferTest.java`

**Interfaces:**
- Consumes: `TelemetryEvent` (Task 2).
- Produces:
  - `void add(TelemetryEvent e)`
  - `boolean markFeatureSeen(String feature)` → `true` la première fois seulement
  - `List<TelemetryEvent> drain()` → vide la file et la renvoie
  - `int size()`
  - `void resetSession()` → oublie les features déjà vues
  - `static final int MAX_EVENTS = 100`

- [ ] **Step 1: Écrire les tests qui échouent**

```java
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
```

- [ ] **Step 2: Lancer les tests et vérifier qu'ils échouent**

Run: `./gradlew test --tests '*TelemetryBufferTest*'`
Expected: FAIL — `TelemetryBuffer` n'existe pas.

- [ ] **Step 3: Écrire l'implémentation minimale**

```java
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
```

- [ ] **Step 4: Lancer les tests et vérifier qu'ils passent**

Run: `./gradlew test --tests '*TelemetryBufferTest*'`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add src/client/java/com/minepiece/essentials/telemetry/TelemetryBuffer.java \
        src/test/java/com/minepiece/essentials/telemetry/TelemetryBufferTest.java
git commit -m "feat(telemetry): file bornee et dedoublonnage par session"
```

---

### Task 4 : `TelemetryId` — identité anonyme persistée

**Files:**
- Create: `src/client/java/com/minepiece/essentials/telemetry/TelemetryId.java`
- Test: `src/test/java/com/minepiece/essentials/telemetry/TelemetryIdTest.java`

**Interfaces:**
- Consumes: `util/JsonHelper` (déjà présent).
- Produces:
  - `static TelemetryId loadOrCreate(Path file)` → génère et écrit au premier appel, relit ensuite
  - `String installId()`
  - `boolean announced()` / `void markAnnounced(Path file)`

Le chemin est **injecté** (et non lu depuis `FabricLoader`) précisément pour que la classe soit testable avec un répertoire temporaire.

- [ ] **Step 1: Écrire les tests qui échouent**

```java
package com.minepiece.essentials.telemetry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryIdTest {

    @TempDir
    Path dir;

    @Test
    void generatesAndPersistsAnIdOnFirstUse() {
        Path file = dir.resolve("telemetry.json");
        TelemetryId first = TelemetryId.loadOrCreate(file);

        assertNotNull(first.installId());
        assertDoesNotThrow(() -> UUID.fromString(first.installId()), "c'est un UUID");
        assertTrue(Files.exists(file), "l'identité est persistée");
        assertFalse(first.announced(), "le message de premier lancement n'a pas encore ete affiche");
    }

    @Test
    void reusesTheSameIdOnSubsequentLoads() {
        Path file = dir.resolve("telemetry.json");
        String id = TelemetryId.loadOrCreate(file).installId();
        assertEquals(id, TelemetryId.loadOrCreate(file).installId(),
                "l'identifiant doit survivre au redemarrage, sinon la retention est fausse");
    }

    @Test
    void markAnnouncedSurvivesReload() {
        Path file = dir.resolve("telemetry.json");
        TelemetryId id = TelemetryId.loadOrCreate(file);
        id.markAnnounced(file);

        assertTrue(TelemetryId.loadOrCreate(file).announced(),
                "le message de premier lancement ne doit apparaitre qu'une fois");
    }

    @Test
    void twoInstallsGetDifferentIds() {
        assertNotEquals(
                TelemetryId.loadOrCreate(dir.resolve("a.json")).installId(),
                TelemetryId.loadOrCreate(dir.resolve("b.json")).installId());
    }
}
```

- [ ] **Step 2: Lancer les tests et vérifier qu'ils échouent**

Run: `./gradlew test --tests '*TelemetryIdTest*'`
Expected: FAIL — `TelemetryId` n'existe pas.

- [ ] **Step 3: Écrire l'implémentation minimale**

```java
package com.minepiece.essentials.telemetry;

import com.minepiece.essentials.util.JsonHelper;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Identité anonyme de l'installation.
 *
 * <p>Un UUID aléatoire, généré au premier lancement et persisté à côté de la
 * config. Il n'est dérivé d'aucune donnée du joueur : supprimer le fichier ou
 * réinstaller le mod donne une nouvelle identité, ce qui est le comportement
 * voulu.
 *
 * <p>Le chemin est passé en paramètre plutôt que lu depuis FabricLoader, pour
 * que la classe reste testable hors de Minecraft.
 */
public final class TelemetryId {

    /** Forme sérialisée du fichier telemetry.json. */
    private static final class Stored {
        String installId;
        String firstSeen;
        boolean announced;
    }

    private final String installId;
    private final boolean announced;

    private TelemetryId(String installId, boolean announced) {
        this.installId = installId;
        this.announced = announced;
    }

    public static TelemetryId loadOrCreate(Path file) {
        Stored stored = JsonHelper.load(file, Stored.class, null);
        if (stored == null || stored.installId == null || stored.installId.isBlank()) {
            stored = new Stored();
            stored.installId = UUID.randomUUID().toString();
            stored.firstSeen = java.time.LocalDate.now().toString();
            stored.announced = false;
            JsonHelper.save(file, stored);
        }
        return new TelemetryId(stored.installId, stored.announced);
    }

    public String installId() { return installId; }
    public boolean announced() { return announced; }

    public void markAnnounced(Path file) {
        Stored stored = JsonHelper.load(file, Stored.class, null);
        if (stored == null) return;
        stored.announced = true;
        JsonHelper.save(file, stored);
    }
}
```

> Attention : `JsonHelper.load` **écrit la valeur par défaut** quand le fichier
> n'existe pas. Passer `null` en défaut évite d'écrire un fichier vide avant
> d'avoir généré l'UUID — vérifier que `JsonHelper.save(path, null)` ne lève pas.
> Si c'est le cas, tester `Files.exists(file)` avant l'appel.

- [ ] **Step 4: Lancer les tests et vérifier qu'ils passent**

Run: `./gradlew test --tests '*TelemetryIdTest*'`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add src/client/java/com/minepiece/essentials/telemetry/TelemetryId.java \
        src/test/java/com/minepiece/essentials/telemetry/TelemetryIdTest.java
git commit -m "feat(telemetry): identite anonyme persistee par installation"
```

---

### Task 5 : `Telemetry` — façade, envoi HTTP, opt-out

**Files:**
- Create: `src/client/java/com/minepiece/essentials/telemetry/Telemetry.java`
- Modify: `src/client/java/com/minepiece/essentials/config/ModConfig.java` (ajout d'un champ à la fin)
- Modify: `src/client/java/com/minepiece/essentials/config/ConfigManager.java` (ajout d'un accesseur de chemin)

**Interfaces:**
- Consumes: `TelemetryEvent`, `TelemetryBuffer`, `TelemetryId` (Tasks 2-4), `ServerDetector.isOnMinePiece()`, `ConfigManager`.
- Produces:
  - `static void init()` — à appeler dans `onInitializeClient`
  - `static void onJoinedServer()` — à appeler sur `ClientPlayConnectionEvents.JOIN`
  - `static void onDisconnected()` — à appeler sur `DISCONNECT`
  - `static void feature(String name)` — depuis les points d'usage (Task 7)
  - `static void tick()` — depuis `END_CLIENT_TICK`, pilote l'envoi différé
  - `static boolean isEnabled()` / `static void setEnabled(boolean)` — pour le toggle (Task 6)

- [ ] **Step 1: Ajouter le flag de config et le chemin**

Dans `ModConfig.java`, à la fin de la classe :

```java
    // Télémétrie anonyme (voir docs/superpowers/specs/2026-09-01-telemetrie-design.md).
    public boolean telemetryEnabled = true;
```

Dans `ConfigManager.java`, à côté des autres accesseurs de chemin :

```java
    public Path telemetryFile() { return CONFIG_DIR.resolve("telemetry.json"); }
```

- [ ] **Step 2: Écrire la façade**

```java
package com.minepiece.essentials.telemetry;

import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.ModConstants;
import com.minepiece.essentials.ServerDetector;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Télémétrie anonyme : combien de joueurs utilisent le mod, sur quelles versions,
 * et quelles features leur servent. Aucune donnée identifiante ne part (la
 * garantie est portée par la liste blanche de {@link TelemetryEvent}).
 *
 * <p>Désactivable dans l'éditeur de HUD (touche K). Toute méthode publique est
 * sans effet quand l'opt-out est actif, pour que les points d'appel dans le
 * reste du mod restent de simples one-liners.
 *
 * <p>Ne doit JAMAIS dégrader le jeu : envoi sur un thread daemon, timeouts
 * courts, échec silencieux, aucun retry.
 */
public final class Telemetry {

    private static final String API_KEY = "phc_nfmqDHNYpEDmGAvnb62NS8SodTa6W73QZAbjSFkSVbRj";
    private static final String BATCH_URL = "https://eu.i.posthog.com/batch/";
    private static final long FLUSH_INTERVAL_MS = 60_000;
    /** Délai après la connexion avant d'émettre mp_session_start : ServerDetector a besoin de se stabiliser. */
    private static final long SESSION_DELAY_MS = 5_000;

    private static final TelemetryBuffer BUFFER = new TelemetryBuffer();
    private static TelemetryId identity;
    private static HttpClient http;

    private static long joinedAt = 0;
    private static boolean sessionSent = false;
    private static long lastFlush = 0;

    private Telemetry() {}

    public static boolean isEnabled() {
        var mgr = MinepieceEssentialsClient.getInstance().getConfigManager();
        return mgr != null && mgr.config().telemetryEnabled;
    }

    public static void setEnabled(boolean enabled) {
        var mgr = MinepieceEssentialsClient.getInstance().getConfigManager();
        mgr.config().telemetryEnabled = enabled;
        mgr.save();
        if (!enabled) BUFFER.drain(); // on jette ce qui n'est pas encore parti
    }

    public static void init() {
        try {
            identity = TelemetryId.loadOrCreate(
                    MinepieceEssentialsClient.getInstance().getConfigManager().telemetryFile());
            http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        } catch (Exception e) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] init ignoré : {}", e.toString());
        }
    }

    public static void onJoinedServer() {
        joinedAt = System.currentTimeMillis();
        sessionSent = false;
        BUFFER.resetSession();
    }

    public static void onDisconnected() {
        flush();
    }

    /** Signale l'usage d'une feature. Au plus un événement par feature et par session. */
    public static void feature(String name) {
        if (!isEnabled() || identity == null) return;
        if (!BUFFER.markFeatureSeen(name)) return;
        try {
            BUFFER.add(TelemetryEvent.of("mp_feature_used", Map.of("feature", name)));
        } catch (IllegalArgumentException e) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] feature rejetée : {}", e.getMessage());
        }
    }

    /** Appelé chaque tick client : émet mp_session_start puis vide la file périodiquement. */
    public static void tick() {
        if (!isEnabled() || identity == null) return;

        long now = System.currentTimeMillis();
        if (!sessionSent && joinedAt > 0 && now - joinedAt >= SESSION_DELAY_MS) {
            sessionSent = true;
            sendSessionStart();
        }
        if (now - lastFlush >= FLUSH_INTERVAL_MS) {
            lastFlush = now;
            flush();
        }
    }

    private static void sendSessionStart() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("mod_version", modVersion());
        props.put("mc_version", FabricLoader.getInstance().getModContainer("minecraft")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
        props.put("loader_version", FabricLoader.getInstance().getModContainer("fabricloader")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
        props.put("java_version", System.getProperty("java.version", "unknown"));
        props.put("os", System.getProperty("os.name", "unknown").toLowerCase().split(" ")[0]);
        props.put("client_language", MinecraftClient.getInstance().options.language);
        props.put("on_minepiece", ServerDetector.isOnMinePiece());
        try {
            BUFFER.add(TelemetryEvent.of("mp_session_start", props));
        } catch (IllegalArgumentException e) {
            MinepieceEssentialsClient.LOGGER.debug("[Telemetry] session rejetée : {}", e.getMessage());
        }
        flush();
    }

    private static String modVersion() {
        return FabricLoader.getInstance().getModContainer(ModConstants.MOD_ID)
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    /** Envoie ce qui est en attente, sur un thread daemon. Échec = perte silencieuse, jamais de retry. */
    private static void flush() {
        if (!isEnabled() || identity == null || http == null) return;
        List<TelemetryEvent> events = BUFFER.drain();
        if (events.isEmpty()) return;

        String body = TelemetryEvent.batchJson(API_KEY, identity.installId(), events);
        Thread t = new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(BATCH_URL))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", ModConstants.MOD_ID)
                        .timeout(Duration.ofSeconds(8))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                http.send(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                MinepieceEssentialsClient.LOGGER.debug("[Telemetry] envoi ignoré : {}", e.toString());
            }
        }, "minepiece-essentials-telemetry");
        t.setDaemon(true);
        t.start();
    }
}
```

- [ ] **Step 3: Vérifier que ça compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, et les tests des tâches 2-4 toujours verts.

- [ ] **Step 4: Commit**

```bash
git add src/client/java/com/minepiece/essentials/telemetry/Telemetry.java \
        src/client/java/com/minepiece/essentials/config/ModConfig.java \
        src/client/java/com/minepiece/essentials/config/ConfigManager.java
git commit -m "feat(telemetry): facade d'envoi PostHog et flag d'opt-out"
```

---

### Task 6 : Branchement dans le client, message de premier lancement, toggle K

**Files:**
- Modify: `src/client/java/com/minepiece/essentials/MinepieceEssentialsClient.java`
- Modify: `src/client/java/com/minepiece/essentials/hud/HudEditScreen.java`
- Modify: `src/client/resources/assets/minepiece-essentials/lang/fr_fr.json`
- Modify: `src/client/resources/assets/minepiece-essentials/lang/en_us.json`

**Interfaces:**
- Consumes: `Telemetry.init/onJoinedServer/onDisconnected/tick/isEnabled/setEnabled` (Task 5).
- Produces: `Telemetry.feature(...)` devient appelable depuis n'importe où (Task 7).

- [ ] **Step 1: Brancher le cycle de vie**

Dans `MinepieceEssentialsClient.onInitializeClient()`, juste après `UpdateChecker.init();` :

```java
        com.minepiece.essentials.telemetry.Telemetry.init();
```

Dans le handler `ClientPlayConnectionEvents.JOIN` existant, à la fin du bloc :

```java
            com.minepiece.essentials.telemetry.Telemetry.onJoinedServer();
```

Dans le handler `DISCONNECT` existant, à la fin :

```java
            com.minepiece.essentials.telemetry.Telemetry.onDisconnected();
```

Dans `END_CLIENT_TICK`, juste après `UpdateChecker.tickNotify();` — **avant** le `if (!ServerDetector.isOnMinePiece()) return;`, sinon la télémétrie ne partirait jamais pour un joueur dont la détection échoue :

```java
            com.minepiece.essentials.telemetry.Telemetry.tick();
```

- [ ] **Step 2: Ajouter les libellés**

Dans `fr_fr.json`, après la ligne `minepiece.ui.editor.ah_price_color` (libellé **sans accents**, comme ses voisins) :

```json
    "minepiece.ui.editor.telemetry": "Statistiques anonymes",
    "minepiece.telemetry.notice": "[Minepiece Essentials] Ce mod envoie des statistiques anonymes (version du mod, version de Minecraft, features utilisees) pour savoir combien de joueurs l'utilisent. Aucun pseudo, aucune donnee de jeu. Desactivable dans l'editeur de HUD (touche K).",
```

Dans `en_us.json`, au même endroit :

```json
    "minepiece.ui.editor.telemetry": "Anonymous stats",
    "minepiece.telemetry.notice": "[Minepiece Essentials] This mod sends anonymous stats (mod version, Minecraft version, features used) to measure how many players use it. No username, no gameplay data. You can turn it off in the HUD editor (key K).",
```

- [ ] **Step 3: Afficher le message une seule fois**

Dans `MinepieceEssentialsClient`, ajouter un champ :

```java
    private boolean pendingTelemetryNotice = false;
```

Dans le handler `JOIN`, après l'appel à `Telemetry.onJoinedServer()` :

```java
            if (!com.minepiece.essentials.telemetry.Telemetry.wasAnnounced()) {
                pendingTelemetryNotice = true;
            }
```

Dans `END_CLIENT_TICK`, après `Telemetry.tick();` :

```java
            if (pendingTelemetryNotice && client.player != null) {
                pendingTelemetryNotice = false;
                client.player.sendMessage(
                    net.minecraft.text.Text.translatable("minepiece.telemetry.notice")
                        .withColor(0xF0A857), false);
                com.minepiece.essentials.telemetry.Telemetry.markAnnounced();
            }
```

Et dans `Telemetry`, ajouter les deux méthodes correspondantes :

```java
    /** Vrai si le message d'information a déjà été affiché sur cette installation. */
    public static boolean wasAnnounced() {
        return identity == null || identity.announced();
    }

    public static void markAnnounced() {
        if (identity == null) return;
        identity.markAnnounced(
                MinepieceEssentialsClient.getInstance().getConfigManager().telemetryFile());
        // relecture pour que wasAnnounced() reflète l'état persisté
        identity = TelemetryId.loadOrCreate(
                MinepieceEssentialsClient.getInstance().getConfigManager().telemetryFile());
    }
```

> Le message s'affiche même quand l'opt-out est actif : il explique justement
> qu'on peut couper. Il ne s'affiche qu'une fois par installation.

- [ ] **Step 4: Ajouter le 7ᵉ toggle dans l'éditeur K**

Dans `HudEditScreen.render(...)`, après la ligne du toggle `ah_price_color` :

```java
        drawToggle(ctx, mouseX, mouseY, 6, Text.translatable("minepiece.ui.editor.telemetry").getString(),
                cfg.telemetryEnabled);
```

Dans `clickPlacement(...)`, **la boucle est bornée en dur à 6 et le `switch` se termine par un `default`** — les deux doivent changer :

```java
            for (int i = 0; i < 7; i++) {                       // était : i < 6
                int ty = rarityTogglesY + i * TOGGLE_STEP;
                if (inBox(mouseX, mouseY, rarityTogglesX, ty, BTN_W, BTN_H)) {
                    var mgr = MinepieceEssentialsClient.getInstance().getConfigManager();
                    var cfg = mgr.config();
                    switch (i) {
                        case 0 -> cfg.rarityIconsEnabled = !cfg.rarityIconsEnabled;
                        case 1 -> cfg.rarityInventoryEnabled = !cfg.rarityInventoryEnabled;
                        case 2 -> cfg.rarityHotbarEnabled = !cfg.rarityHotbarEnabled;
                        case 3 -> cfg.rarityFilterEnabled = !cfg.rarityFilterEnabled;
                        case 4 -> cfg.raritySorterEnabled = !cfg.raritySorterEnabled;
                        case 5 -> cfg.ahPriceColorEnabled = !cfg.ahPriceColorEnabled;   // était : default
                        default -> com.minepiece.essentials.telemetry.Telemetry.setEnabled(
                                !cfg.telemetryEnabled);
                    }
                    mgr.save();
                    return true;
                }
            }
```

> Sans le passage de `default` à `case 5`, cliquer le nouveau toggle basculerait
> le réglage « Couleur prix AH ».

- [ ] **Step 5: Vérifier la compilation et lancer toute la suite de tests**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, tous les tests verts.

- [ ] **Step 6: Commit**

```bash
git add src/client/java/com/minepiece/essentials/MinepieceEssentialsClient.java \
        src/client/java/com/minepiece/essentials/hud/HudEditScreen.java \
        src/client/java/com/minepiece/essentials/telemetry/Telemetry.java \
        src/client/resources/assets/minepiece-essentials/lang/
git commit -m "feat(telemetry): branchement client, message d'information et toggle K"
```

---

### Task 7 : Instrumenter les 12 features

Un seul appel par point d'usage. `Telemetry.feature(...)` gère l'opt-out et le dédoublonnage, donc **aucune condition** ne doit entourer ces appels.

**Files (Modify):**

| Feature | Fichier | Où |
|---|---|---|
| `boss_refresh` | `boss/BossTracker.java` | début de `refreshIsland(Island)` |
| `boss_waypoint` | `boss/WaypointManager.java` | méthode de création d'un waypoint |
| `hud_edit` | `hud/HudEditScreen.java` | `init()` |
| `help_screen` | `help/HelpScreen.java` | `init()` |
| `rarity_filter` | `rarity/RarityScreenOverlay.java` | au clic sur une rareté |
| `rarity_sort` | `rarity/RarityScreenOverlay.java` | au clic sur un bouton de tri |
| `pet_tooltip` | `pet/PetStatTooltip.java` | quand une infobulle de pet est enrichie |
| `minion_calc` | `pet/MinionTooltip.java` | quand une infobulle de minion est enrichie |
| `parchment_hud` | `quest/ParcheminHud.java` | premier rendu non vide |
| `job_hud` | `job/JobHud.java` | premier rendu non vide |
| `haki_hud` | `haki/HakiHud.java` | premier rendu non vide |
| `ah_price` | `ah/AhTooltip.java` | quand une infobulle AH est enrichie |

**Interfaces:**
- Consumes: `Telemetry.feature(String)` (Task 5).
- Produces: rien.

- [ ] **Step 1: Ajouter les appels**

Dans chaque fichier ci-dessus, à l'endroit indiqué :

```java
com.minepiece.essentials.telemetry.Telemetry.feature("boss_refresh");
```

(en remplaçant le nom par celui de la ligne correspondante).

Pour les trois HUD (`parchment_hud`, `job_hud`, `haki_hud`), placer l'appel **après** la condition qui décide qu'il y a quelque chose à afficher — sinon on compterait un affichage vide.

- [ ] **Step 2: Vérifier que les noms correspondent exactement à la spec**

Run:

```bash
grep -rho 'Telemetry\.feature("[a-z_]*")' src/client/java | sort -u
```

Expected : exactement les 12 noms du tableau, sans doublon ni faute de frappe.

- [ ] **Step 3: Vérifier la compilation**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/client/java/com/minepiece/essentials/
git commit -m "feat(telemetry): instrumentation des 12 features"
```

---

### Task 8 : Vérification en jeu

**Files:** aucun (validation).

- [ ] **Step 1: Déployer sur l'instance de test 1.21.11**

```bash
./gradlew build
cp build/libs/minepiece-essentials-*.jar \
   "/mnt/c/Users/Aurelien/AppData/Roaming/PrismLauncher/instances/1.21.11/minecraft/mods/"
sync
```

Vérifier qu'il ne reste **qu'un seul** jar du mod dans le dossier (Minecraft fermé — sinon la suppression de l'ancien échoue avec « Permission denied »).

- [ ] **Step 2: Lancer le jeu, se connecter à MinePiece, ouvrir K puis H**

Attendu en jeu : le message d'information s'affiche **une seule fois** au premier lancement, et le toggle « Statistiques anonymes » apparaît en 7ᵉ position dans l'éditeur K.

- [ ] **Step 3: Vérifier l'arrivée des données dans PostHog**

Environ une minute après (intervalle de vidage), via l'accès PostHog de la session :

```
call read-data-schema {"query": {"kind": "events"}}
```

Attendu : `mp_session_start` et `mp_feature_used` présents. Puis :

```
call read-data-schema {"query": {"kind": "event_properties", "event_name": "mp_session_start"}}
```

Attendu : les 7 propriétés de la spec, et **aucune autre**.

- [ ] **Step 4: Vérifier l'opt-out**

Basculer le toggle sur OFF dans K, se déconnecter, se reconnecter, jouer une minute. Attendu : aucun nouvel événement dans PostHog. Vérifier aussi `latest.log` : aucune exception liée à la télémétrie.

- [ ] **Step 5: Vérifier que PostHog injoignable ne casse rien**

Couper le réseau (ou bloquer `eu.i.posthog.com` dans le fichier hosts), lancer le jeu, jouer. Attendu : aucun freeze, aucun crash, seulement des lignes `LOGGER.debug`.

---

### Task 9 : Port 1.21.8, disclosure Modrinth et release

**Files:**
- Copy: tout `telemetry/`, `ModConfig.java`, `ConfigManager.java`, `MinepieceEssentialsClient.java` et les fichiers de features vers `/home/aurelien/claude_project/Public_QoL_Minepiece-1.21.8/`
- Modify à la main : `HudEditScreen.java` du dossier 1.21.8 (signature de `mouseClicked` différente)
- Create: `docs/modrinth-changelog-<version>.md`

- [ ] **Step 1: Copier les fichiers partagés**

```bash
S=/home/aurelien/claude_project/Public_QoL_Minepiece
D=/home/aurelien/claude_project/Public_QoL_Minepiece-1.21.8
cp -r "$S/src/client/java/com/minepiece/essentials/telemetry" \
      "$D/src/client/java/com/minepiece/essentials/"
cp -r "$S/src/test/java/com/minepiece/essentials/telemetry" \
      "$D/src/test/java/com/minepiece/essentials/"
```

Puis les fichiers modifiés un à un, **sauf `HudEditScreen.java`**.

- [ ] **Step 2: Reporter le toggle à la main dans le `HudEditScreen` 1.21.8**

Même changement qu'en Task 6 Step 4 (le `drawToggle` d'index 6, la boucle `i < 7`, le `case 5`), mais dans la version dont `mouseClicked` a la signature `(double, double, int)`.

- [ ] **Step 3: Construire et tester les deux dossiers**

```bash
cd /home/aurelien/claude_project/Public_QoL_Minepiece && ./gradlew build
cd /home/aurelien/claude_project/Public_QoL_Minepiece-1.21.8 && ./gradlew build
```

Expected: les deux BUILD SUCCESSFUL, tests verts des deux côtés.

- [ ] **Step 4: Vérifier au runtime sur l'instance 1.21.8**

Déployer dans `…/instances/1.21.8 (second)/minecraft/mods/` et vérifier `latest.log` : aucune erreur d'application de mixin, et le toggle présent dans K.

- [ ] **Step 5: Remplir l'onglet Disclosures de Modrinth**

Action manuelle de l'auteur, sur les deux versions du projet. Y indiquer : envoi de statistiques anonymes à PostHog (cloud EU), aucune donnée identifiante, désactivable en jeu. Ajouter la même mention dans la description du projet. **Requis par la règle Modrinth 1.11.**

- [ ] **Step 6: Bump de version, changelog et commit**

```bash
# les DEUX gradle.properties
git add -A
git commit -m "chore(release): v<version>"
```
