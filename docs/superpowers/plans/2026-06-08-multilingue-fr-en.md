# Support multilingue FR+EN — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Faire fonctionner tous les parsers du mod que la « Game language » serveur soit FR ou EN, et afficher l'UI du mod dans la langue du client Minecraft.

**Architecture:** Un registre central `i18n/ServerText` contient les variantes FR+EN de chaque chaîne serveur et expose des `matches()`/`Pattern` combinés. Chaque parser remplace ses littéraux français par des appels à `ServerText`. L'UI passe par les fichiers de langue Minecraft (`en_us.json`/`fr_fr.json`).

**Tech Stack:** Java 21, Fabric 1.21.11, JUnit 5 (`./gradlew test`).

**Build/test :** `./gradlew test` (logique pure) ; `./gradlew build` (jar). Tous les parsers visés ont une logique pure testable hors client.

---

## File Structure

- **Create** `src/client/java/com/minepiece/essentials/i18n/ServerText.java` — registre des mots-clés multilingues (constantes + `matches`/`firstMatch` + `Pattern`).
- **Create** `src/test/java/com/minepiece/essentials/i18n/ServerTextTest.java` — tests des matchs FR+EN.
- **Modify** parsers : `ah/AhPriceParser`, `ah/AhPriceBand`, `ah/AhTooltip`, `pet/ActivePetsScanner`, `pet/PetStat`, `pet/PetEffectParser`, `quest/ParcheminScanner`, `quest/PassQuestParser`, `quest/PassQuestScanner`, `quest/Difficulty`, `boss/BossTracker`, `haki/HakiTimer`.
- **Modify** tests existants : ajouter cas EN (`AhPriceBandTest`, `PassQuestParserTest`, `PassQuestStateTest`, + nouveau test parchemin si besoin).
- **Modify** UI : `assets/minepiece-essentials/lang/en_us.json` + `fr_fr.json`, et les classes d'affichage (`hud/*Hud`, `help/HelpScreen`, `hud/HudEditScreen`, notifier).
- **Remove (cleanup)** `debug/DebugDumper.java` + le hook dans `MinepieceEssentialsClient`.

---

# PHASE 1 — Parsers bilingues (corrige le bug)

## Task 1 : Registre `ServerText` + helpers

**Files:**
- Create: `src/client/java/com/minepiece/essentials/i18n/ServerText.java`
- Test: `src/test/java/com/minepiece/essentials/i18n/ServerTextTest.java`

- [ ] **Step 1 : Écrire le test d'abord**

```java
package com.minepiece.essentials.i18n;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerTextTest {
    @Test void matchesBothLanguages() {
        assertTrue(ServerText.matches(" ▪ Prix de vente: 10M", ServerText.SELL_PRICE));
        assertTrue(ServerText.matches(" ▪ Selling price: 10M", ServerText.SELL_PRICE));
        assertFalse(ServerText.matches(" ▪ Average price: 9K", ServerText.SELL_PRICE));
    }
    @Test void petLevelPatternBilingual() {
        assertTrue(ServerText.PET_LEVEL.matcher("Niveau: 7").find());
        assertTrue(ServerText.PET_LEVEL.matcher("Level: 7").find());
        assertEquals("Max", firstGroup("Level: Max"));
    }
    @Test void expireDateBilingual() {
        assertTrue(ServerText.EXPIRE.matcher("Expire le 13/06/2026 14h47").find());
        assertTrue(ServerText.EXPIRE.matcher("Expires on 13/06/2026 14h47").find());
    }
    @Test void questNameBilingual() {
        var fr = ServerText.QUEST_NAME.matcher("Quête #3 (Difficile)");
        var en = ServerText.QUEST_NAME.matcher("Quest #3 (Hard)");
        assertTrue(fr.find()); assertTrue(en.find());
    }
    @Test void bossRespawnBilingual() {
        assertTrue(ServerText.BOSS_RESPAWN.matcher("Respawn: 0s (15 Minutes)").find());
        assertTrue(ServerText.BOSS_RESPAWN.matcher("Réapparition: 2m 30s").find());
    }
    private static String firstGroup(String s) {
        var m = ServerText.PET_LEVEL.matcher(s);
        return m.find() ? m.group(1) : null;
    }
}
```

- [ ] **Step 2 : Lancer le test → échoue** (`ServerText` inexistant)

Run: `./gradlew test --tests '*ServerTextTest*'`
Expected: FAIL (compile error / cannot find symbol ServerText)

- [ ] **Step 3 : Écrire `ServerText`**

```java
package com.minepiece.essentials.i18n;

import java.util.regex.Pattern;

/**
 * Mots-clés serveur MinePiece en plusieurs langues (FR + EN). Le serveur envoie son
 * texte dans la « Game language » (réglée via /lang, illisible côté client) : les
 * parsers matchent donc toutes les langues connues simultanément. Ajouter une langue
 * = compléter les alternatives ci-dessous.
 */
public final class ServerText {
    private ServerText() {}

    // AH
    public static final String[] SELL_PRICE = {"Prix de vente", "Selling price"};
    public static final String[] AVG_PRICE  = {"Prix moyen", "Average price"};

    // Pets
    public static final String[] PET_EFFECTS = {"Familier Effects", "Familiar Effects"};
    public static final String[] PET_ACTIVE_ACTION = {"Désactiver", "Disable"};
    public static final String[] PET_INACTIVE_ACTION = {"Activer", "Activate"};
    public static final String[] PET_NOT_DISCOVERED = {"Non découvert", "Not discovered"};
    public static final Pattern PET_LEVEL = Pattern.compile("(?:Niveau|Level):\\s*(\\S+)");

    // Parchemins
    public static final String[] SCROLL_NAME = {"parchemin", "scroll"};
    public static final String[] OBJECTIVE = {"Objectif", "Objective"};
    public static final String[] LUNAR = {"lunaire", "lunar"};
    public static final Pattern EXPIRE =
        Pattern.compile("(?:Expire le|Expires on) (\\d{2}/\\d{2}/\\d{4} \\d{1,2}h\\d{2})");

    // Quêtes pass
    public static final Pattern QUEST_NAME =
        Pattern.compile("(?:Qu[eê]te|Quest) #(\\d+)\\s*\\(([^)]+)\\)");
    public static final String[] PROGRESS = {"Progress"};       // couvre FR « Progression »
    public static final String[] STATUS = {"Statut", "Status"};
    public static final String[] NOT_COMPLETED = {"Non compl", "Not compl"};
    public static final String[] QUEST_NAME_FRAGMENT = {"uête #", "uete #", "uest #"};

    // Boss
    public static final Pattern BOSS_COORDS =
        Pattern.compile("(?:Coordonn[eé]es?|Coordinates)\\s*:?\\s*([-\\d]+)\\s+([-\\d]+)\\s+([-\\d]+)");
    public static final Pattern BOSS_RESPAWN =
        Pattern.compile("(?:R[eé]a?parition|Apparition|Respawn)\\s*:?\\s*(?:(\\d+)m)?\\s*(\\d+)s");
    public static final String[] BOSS_MINIBOSS_CATEGORY =
        {"mini-boss", "présents sur cette île", "present on this island", "present in the city"};
    public static final String[] BOSS_VIEW_LOOTS =
        {"voir les loots", "view loots", "view the loots", "view relics", "chasseur", "hunter", "pirate", "monstre", "monster"};
    public static final String[] BOSS_NAV = {"retour", "page", "fermer", "back", "close"};
    public static final String[] BOSS_MOB_KEYWORDS =
        {"marine", "mob", "monstre", "monster", "bandit", "combat", "fight", "ennemi",
         "enemy", "garde", "guard", "pirate", "zombie", "chasseur", "hunter", "soldat", "soldier"};

    // Haki
    public static final String[] HAKI_ACTIVATED = {"Vous avez activé le haki", "You have activated haki"};
    public static final String[] HAKI_READY =
        {"Vous pouvez de nouveau utiliser votre haki", "You can use your haki"};

    /** Vrai si {@code line} contient une des variantes (insensible à la casse). */
    public static boolean matches(String line, String[] variants) {
        if (line == null) return false;
        String lower = line.toLowerCase();
        for (String v : variants) if (lower.contains(v.toLowerCase())) return true;
        return false;
    }
}
```

- [ ] **Step 4 : Lancer le test → passe**

Run: `./gradlew test --tests '*ServerTextTest*'`
Expected: PASS

- [ ] **Step 5 : Commit**

```bash
git add src/client/java/com/minepiece/essentials/i18n/ServerText.java src/test/java/com/minepiece/essentials/i18n/ServerTextTest.java
git commit -m "feat(i18n): registre ServerText multilingue FR+EN"
```

---

## Task 2 : Haki bilingue

**Files:** Modify `src/client/java/com/minepiece/essentials/haki/HakiTimer.java`

- [ ] **Step 1 : Remplacer les constantes et les tests `contains`**

Dans `HakiTimer`, supprimer les constantes `ACTIVATED`/`READY` et remplacer leur usage :

```java
// avant : if (text.contains(ACTIVATED)) { ... } else if (text.contains(READY)) { ... }
import com.minepiece.essentials.i18n.ServerText;
...
if (ServerText.matches(text, ServerText.HAKI_ACTIVATED)) {
    remainingSeconds = 30;
    active = true;
} else if (ServerText.matches(text, ServerText.HAKI_READY)) {
    remainingSeconds = 0;
    active = false;
}
```

(Conserver la logique de countdown existante autour.)

- [ ] **Step 2 : Build**

Run: `./gradlew compileClientJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3 : Commit**

```bash
git add src/client/java/com/minepiece/essentials/haki/HakiTimer.java
git commit -m "feat(i18n): haki timer bilingue (You have activated/can use haki)"
```

---

## Task 3 : AH bilingue

**Files:** Modify `ah/AhPriceParser.java`, `ah/AhPriceBand.java`, `ah/AhTooltip.java`
Test: `src/test/java/com/minepiece/essentials/ah/AhPriceBandTest.java`

- [ ] **Step 1 : Test EN dans `AhPriceBandTest`** (ajouter)

```java
@Test
void fromLoreReadsEnglishPrices() {
    java.util.List<String> lore = java.util.List.of(
        "Legendary sword",
        " ▪ Selling price: 11.5M 实",
        " ▪ Average price: 10M");
    AhPriceBand.Result r = AhPriceBand.fromLore(lore).orElseThrow();
    assertEquals(AhPriceBand.Band.EXPENSIVE, r.band());
    assertEquals(15, r.percent());
}
```

- [ ] **Step 2 : Lancer → échoue** (parser ne reconnaît que le FR)

Run: `./gradlew test --tests '*AhPriceBandTest*'`
Expected: FAIL sur `fromLoreReadsEnglishPrices`

- [ ] **Step 3 : `AhPriceBand` — remplacer les constantes par `ServerText`**

Dans `AhPriceBand.priceOf`, remplacer le paramètre `String keyword` par `String[] variants` et l'usage :

```java
import com.minepiece.essentials.i18n.ServerText;
...
public static Optional<Result> fromLore(List<String> lore) {
    if (lore == null) return Optional.empty();
    OptionalDouble sell = priceOf(lore, ServerText.SELL_PRICE);
    OptionalDouble avg = priceOf(lore, ServerText.AVG_PRICE);
    if (sell.isEmpty() || avg.isEmpty()) return Optional.empty();
    return of(sell.getAsDouble(), avg.getAsDouble());
}
private static OptionalDouble priceOf(List<String> lore, String[] variants) {
    for (String line : lore) {
        if (ServerText.matches(line, variants)) {
            OptionalDouble p = AhPriceParser.parseAbbreviated(line);
            if (p.isPresent()) return p;
        }
    }
    return OptionalDouble.empty();
}
```

Supprimer les constantes `SELL_LINE`/`AVG_LINE` de `AhPriceBand`.

- [ ] **Step 4 : `AhPriceParser.perUnit` — accepter des variantes**

Ajouter une surcharge prenant `String[] variants` (garder l'ancienne `String keyword` pour compat si utilisée, sinon la remplacer) :

```java
import com.minepiece.essentials.i18n.ServerText;
...
public static Optional<String> perUnit(List<String> lore, int count, String[] variants) {
    if (count <= 1 || lore == null) return Optional.empty();
    for (String line : lore) {
        if (ServerText.matches(line, variants)) {
            OptionalDouble price = parseAbbreviated(line);
            if (price.isPresent() && price.getAsDouble() > 0)
                return Optional.of(format(price.getAsDouble() / count));
        }
    }
    return Optional.empty();
}
```

- [ ] **Step 5 : `AhTooltip` — utiliser `ServerText`**

Remplacer `SELL_LINE`/`AVG_LINE` (String) par `ServerText.SELL_PRICE`/`AVG_PRICE`, et `indexOf(strings, SELL_LINE)` par une version qui matche les variantes :

```java
import com.minepiece.essentials.i18n.ServerText;
...
Optional<String> sell = AhPriceParser.perUnit(strings, count, ServerText.SELL_PRICE);
Optional<String> avg = AhPriceParser.perUnit(strings, count, ServerText.AVG_PRICE);
int sellIdx = indexOf(strings, ServerText.SELL_PRICE);
int avgIdx = indexOf(strings, ServerText.AVG_PRICE);
...
private static int indexOf(List<String> lines, String[] variants) {
    for (int i = 0; i < lines.size(); i++)
        if (ServerText.matches(lines.get(i), variants)) return i;
    return -1;
}
```

- [ ] **Step 6 : Lancer tous les tests AH → passent**

Run: `./gradlew test --tests '*AhPriceBandTest*'`
Expected: PASS (FR + EN)

- [ ] **Step 7 : Commit**

```bash
git add src/client/java/com/minepiece/essentials/ah/ src/test/java/com/minepiece/essentials/ah/AhPriceBandTest.java
git commit -m "feat(i18n): AH bilingue (Selling/Average price)"
```

---

## Task 4 : Pets — actions, niveau, section, noms de stats

**Files:** Modify `pet/ActivePetsScanner.java`, `pet/PetStat.java`, `pet/PetEffectParser.java`

- [ ] **Step 1 : `PetStat` — ajouter les noms EN**

Remplacer le champ `fr` par une liste d'alias multilingues. Ajouter les alias EN dans `ALIASES` (ordre most-specific first) :

```java
new Alias("energy regen", ENERGY_REGEN),
new Alias("régénération énergie", ENERGY_REGEN),
new Alias("critical chance", CRIT_CHANCE),
new Alias("chance critique", CRIT_CHANCE),
new Alias("critical damage", CRIT_DAMAGE),
new Alias("dégâts critiques", CRIT_DAMAGE),
new Alias("dégât critique", CRIT_DAMAGE),
new Alias("regen", REGEN),
new Alias("régénération", REGEN),
new Alias("power", POWER),
new Alias("puissance", POWER),
new Alias("dexterity", DEXTERITY),
new Alias("dextérité", DEXTERITY),
new Alias("defense", DEFENSE),
new Alias("défense", DEFENSE),
new Alias("speed", SPEED),
new Alias("vitesse", SPEED),
new Alias("strength", STRENGTH),
new Alias("force", STRENGTH),
new Alias("energy", ENERGY),
new Alias("énergie", ENERGY),
new Alias("health", VITALITY),
new Alias("life", VITALITY),
new Alias("vie", VITALITY)
```

Renommer `fromFrench` → `fromLabel` (ou garder `fromFrench` comme alias appelant). Mettre à jour l'appel dans `PetEffectParser.parse` (`PetStat.fromFrench(...)` → `PetStat.fromLabel(...)`).

> ⚠ Attention à l'ordre : `strength` avant `energy`/`health`? Non — la collision dangereuse est « energy » vs « energy regen » (regen d'abord, déjà placé) et « critical chance » vs « critical damage » (distincts). « force » EN n'existe pas comme stat → ok.

- [ ] **Step 2 : `ActivePetsScanner` — actions/section/niveau via `ServerText`**

```java
import com.minepiece.essentials.i18n.ServerText;
// supprimer ACTIVE_ACTION / INACTIVE_ACTION / SECTION_START / NIVEAU
...
if (containsAny(tip, ServerText.PET_ACTIVE_ACTION) || containsAny(tip, ServerText.PET_INACTIVE_ACTION))
    isPetsScreen = true;
if (!containsAny(tip, ServerText.PET_ACTIVE_ACTION)) continue;
...
// levelOf :
for (String line : tip) {
    Matcher m = ServerText.PET_LEVEL.matcher(line);
    if (m.find()) {
        try { return Integer.parseInt(m.group(1)); }
        catch (NumberFormatException e) { return MAX_LEVEL; } // "Max"
    }
}
...
// combatStats : remplacer lines.get(i).contains(SECTION_START) par
ServerText.matches(lines.get(i), ServerText.PET_EFFECTS)
```

`SECTION_END` (« Minion Effects ») est identique FR/EN → inchangé. Ajouter le helper :

```java
private static boolean containsAny(List<String> lines, String[] variants) {
    for (String l : lines) if (ServerText.matches(l, variants)) return true;
    return false;
}
```

(Remplacer les anciens `containsLine(tip, X)` par `containsAny`.)

- [ ] **Step 3 : `PetEffectParser` — ignorer « Not discovered »**

`parse` renvoie déjà empty si pas de stat reconnue. Aucun changement requis sauf l'appel `PetStat.fromLabel`. (Le filtrage « Non découvert/Not discovered » est implicite : pas de `+value` exploitable ou stat inconnue → empty.)

- [ ] **Step 4 : Build**

Run: `./gradlew compileClientJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5 : Commit**

```bash
git add src/client/java/com/minepiece/essentials/pet/
git commit -m "feat(i18n): pets bilingues (actions, niveau, section, noms de stats EN)"
```

---

## Task 5 : Parchemins bilingues

**Files:** Modify `quest/ParcheminScanner.java`

- [ ] **Step 1 : Nom scroll, expire, objectif, lunaire via `ServerText`**

```java
import com.minepiece.essentials.i18n.ServerText;
...
// détection item (ligne ~45) :
if (!ServerText.matches(name, ServerText.SCROLL_NAME)) continue;   // "parchemin"/"scroll"
...
// rareté lunaire (ligne ~66) :
if (ServerText.matches(nameLower, ServerText.LUNAR)) quest.rarity = "LUNAIRE";
...
// EXPIRE_PATTERN remplacé par ServerText.EXPIRE :
Matcher expireMatch = ServerText.EXPIRE.matcher(line);
...
// objectif (ligne ~130) :
if (ServerText.matches(line, ServerText.OBJECTIVE) && !line.contains("(")) { /* idem */ }
```

Garder les glyphes de rareté (`伴`…) — indépendants de la langue. Garder `PROGRESS_PATTERN` `(X/Y)`.

- [ ] **Step 2 : Build**

Run: `./gradlew compileClientJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3 : Commit**

```bash
git add src/client/java/com/minepiece/essentials/quest/ParcheminScanner.java
git commit -m "feat(i18n): parchemins bilingues (scroll/Expires on/Objective/Lunar)"
```

---

## Task 6 : Quêtes pass bilingues

**Files:** Modify `quest/PassQuestParser.java`, `quest/PassQuestScanner.java`, `quest/Difficulty.java`
Test: `src/test/java/com/minepiece/essentials/quest/PassQuestParserTest.java`

- [ ] **Step 1 : Test EN dans `PassQuestParserTest`**

```java
@Test
void parsesEnglishQuest() {
    var lore = java.util.List.of(
        "💡 Objective", " ▪ Defeat 1 Boss (Sabaody)",
        "❦ Progress", "0 s m m e 1", "Status: Not completed");
    PassQuest q = PassQuestParser.parse("Quest #1 (Easy)", lore).orElseThrow();
    assertEquals(1, q.number());
    assertEquals(Difficulty.FACILE, q.difficulty());
    assertEquals("Defeat 1 Boss (Sabaody)", q.objective());
    assertEquals(1, q.target());
    assertFalse(q.completed());
}
```

- [ ] **Step 2 : Lancer → échoue**

Run: `./gradlew test --tests '*PassQuestParserTest*'`
Expected: FAIL (nom « Quest # » non reconnu / difficulté / progress)

- [ ] **Step 3 : `PassQuestParser` — patterns/keywords bilingues**

```java
import com.minepiece.essentials.i18n.ServerText;
// NAME -> ServerText.QUEST_NAME ; COMPLETED chat :
private static final Pattern COMPLETED = Pattern.compile("(?:qu[eê]te|quest)\\s*:\\s*(.+)$");
...
Matcher nm = ServerText.QUEST_NAME.matcher(name);
...
} else if (ServerText.matches(line, ServerText.PROGRESS)) {   // "Progress"/"Progression"
    expectProgress = true;
} else if (expectProgress && INT.matcher(line).find()) { ... }
else if (ServerText.matches(line, ServerText.STATUS)) {       // "Statut"/"Status"
    completed = !ServerText.matches(line, ServerText.NOT_COMPLETED);
}
```

- [ ] **Step 4 : `Difficulty.fromLabel` — alias EN**

Ajouter une table d'alias EN→enum :

```java
public static Difficulty fromLabel(String s) {
    if (s != null) {
        String t = s.trim().toLowerCase();
        switch (t) {
            case "easy": return FACILE;
            case "medium": return MOYENNE;
            case "hard": return DIFFICILE;
        }
        for (Difficulty d : values())
            if (d.label.toLowerCase().equals(t)) return d;
    }
    return AUTRE;
}
```

- [ ] **Step 5 : `PassQuestScanner` — fragment de nom**

```java
import com.minepiece.essentials.i18n.ServerText;
// ligne 65 :
if (!ServerText.matches(name, ServerText.QUEST_NAME_FRAGMENT)) continue; // "uête #"/"uete #"/"uest #"
```

- [ ] **Step 6 : Lancer → passent**

Run: `./gradlew test --tests '*PassQuest*'`
Expected: PASS (FR + EN)

- [ ] **Step 7 : Commit**

```bash
git add src/client/java/com/minepiece/essentials/quest/ src/test/java/com/minepiece/essentials/quest/PassQuestParserTest.java
git commit -m "feat(i18n): quêtes pass bilingues (Quest #/Progress/Status/Easy-Medium-Hard)"
```

---

## Task 7 : Boss bilingue

**Files:** Modify `boss/BossTracker.java`

- [ ] **Step 1 : Coordonnées / respawn via `ServerText`**

Remplacer les deux `Pattern` locaux (lignes ~36/38) par `ServerText.BOSS_COORDS` / `ServerText.BOSS_RESPAWN`.

- [ ] **Step 2 : Catégories / nav / mots-clés mobs**

- Lignes ~155-158 (mots-clés mobs) : remplacer la chaîne de `name.contains("marine") || …` par `ServerText.matches(name, ServerText.BOSS_MOB_KEYWORDS)`.
- Lignes ~186-188 (catégorie) : pour la branche mini-boss, `ServerText.matches(l, ServerText.BOSS_MINIBOSS_CATEGORY)` ; pour l'autre branche (loot/voir), `ServerText.matches(l, ServerText.BOSS_VIEW_LOOTS)`.
- Ligne ~204 (nav à ignorer) : `if (ServerText.matches(nameLower, ServerText.BOSS_NAV)) continue;`
- Constantes `"Mini-Boss(es) présents"`, `"le Boss de cette île"`, `"présents sur cette île"` (lignes ~162-186) : remplacer par `ServerText.matches(..., ServerText.BOSS_MINIBOSS_CATEGORY)` / un nouveau `ServerText.BOSS_SINGLE` (`{"le Boss de cette île","the Boss of this island"}` — l'ajouter dans `ServerText`).

> Conserver toute la logique de localisation de slots/catégories ; on ne change QUE les comparaisons de chaînes. Vérifier chaque `contains(...)` français restant du fichier.

- [ ] **Step 3 : Build**

Run: `./gradlew compileClientJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4 : Commit**

```bash
git add src/client/java/com/minepiece/essentials/boss/BossTracker.java src/client/java/com/minepiece/essentials/i18n/ServerText.java
git commit -m "feat(i18n): boss bilingue (Coordinates/Respawn/catégories/nav/mobs)"
```

---

## Task 8 : Vérif build + tests Phase 1

- [ ] **Step 1 : Suite complète**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, tous tests PASS

- [ ] **Step 2 : Déploiement test 1.21.11 (manuel)**

```bash
cp build/libs/minepiece-essentials-1.6.1.jar "/mnt/c/Users/Aurelien/AppData/Roaming/PrismLauncher/instances/1.21.11/minecraft/mods/" && sync
```

Test en jeu : passer le serveur en `/lang en`, vérifier AH (couleurs + prix/u), pets (%), boss timers, parchemins HUD, pass HUD, haki timer. Puis `/lang fr` → tout marche aussi.

---

# PHASE 2 — UI (langue du client MC)

## Task 9 : Externaliser l'UI vers les fichiers de langue

**Files:** Modify `assets/minepiece-essentials/lang/en_us.json` + `fr_fr.json`, `hud/*Hud`, `help/HelpScreen`, `hud/HudEditScreen`, notifier.

- [ ] **Step 1 : Ajouter les clés** dans `fr_fr.json` et `en_us.json`, ex. :

```json
// fr_fr.json
"minepiece.hud.job": "Métier",
"minepiece.hud.quests": "Quêtes du jour",
"minepiece.hud.pets": "Pets actifs",
"minepiece.hud.boss": "Boss Timers",
"minepiece.hud.ascensions": "Ascensions",
"minepiece.hud.scrolls": "Parchemins",
"minepiece.haki.ready": "Prêt !",
"minepiece.job.harvest_hint": "Récolte pour voir"
// en_us.json : mêmes clés, valeurs anglaises ("Job", "Daily quests", "Active pets", ...)
```

- [ ] **Step 2 : Remplacer les littéraux d'affichage** par `Text.translatable("minepiece.hud.job").getString()` (ou rendu direct `Text`). Couvrir : `JobHud` (`Métier`/`Récolte pour voir`), `PassQuestHud` (`Quêtes du jour`/placeholder/terminées), `ActivePetsHud`, `BossTimerHud`, `AscensionHud`, `ParcheminHud`, `HakiHud` (`Haki`/`Prêt !`), `HelpScreen` (toutes entrées), `HudEditScreen` (onglets/boutons/toggles), notifier de MAJ.

- [ ] **Step 3 : Build + commit**

```bash
./gradlew build
git add src/client/resources/assets/ src/client/java/com/minepiece/essentials/
git commit -m "feat(i18n): UI du mod traduite via fichiers de langue (suit le client MC)"
```

---

# CLEANUP

## Task 10 : Retirer le dumper de debug

**Files:** Delete `debug/DebugDumper.java` ; Modify `MinepieceEssentialsClient.java`

- [ ] **Step 1 : Supprimer** `src/client/java/com/minepiece/essentials/debug/DebugDumper.java`, la méthode `registerDebugDumper()` et son appel `registerDebugDumper();` dans `MinepieceEssentialsClient`.

- [ ] **Step 2 : Build + commit**

```bash
rm src/client/java/com/minepiece/essentials/debug/DebugDumper.java
./gradlew build
git add -A
git commit -m "chore: retrait du dumper de debug (relevé multilingue terminé)"
```

---

## Flous à confirmer en jeu (rappel)
- Tournure exacte de complétion de quête pass EN (regex `(?:quête|quest)\s*:` + garde `compl` couvre les deux).
- 4 noms de stats pets (`Dexterity`/`Speed`/`Critical Damage`/`Energy Regen`) — alias ajoutés, à vérifier sur un pet réel.
