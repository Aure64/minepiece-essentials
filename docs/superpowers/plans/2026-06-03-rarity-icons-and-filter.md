# Emblèmes de rareté + filtre + trieur — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Afficher l'emblème de rareté du resource pack dans le coin de chaque case (inventaire/coffres/GUI serveur), filtrer visuellement par rareté, et trier le conteneur ouvert par rareté.

**Architecture:** Un module `rarity/` qui sépare la **logique pure** (détection par glyphe, calcul d'ordre de tri, états) — testée en JUnit sans Minecraft — de la **couche Minecraft** (lecture des composants d'item, rendu `DrawContext`, clics `clickSlot`). Un mixin sur `HandledScreen` branche le tout : dessin après les slots + routage des clics de la barre. Tout est gated `ServerDetector.isOnMinePiece()`.

**Tech Stack:** Java 21, Fabric 0.19.2 / MC 1.21.11 (yarn 1.21.11+build.5), Mixin, JUnit 5.

---

## File Structure

**Logique pure (testable, aucune dépendance Minecraft) :**
- `rarity/ItemRarity.java` — enum des raretés : glyphe, clé, dimensions natives, couleur, rang.
- `rarity/RarityGlyphs.java` — scan d'une chaîne / liste de chaînes → `ItemRarity`.
- `rarity/RaritySort.java` — calcul de l'ordre cible (pure) à partir d'entrées `(rang, itemId)`.
- `rarity/RarityFilterState.java` — état de session (toggles multiples).
- `rarity/RaritySortState.java` — direction de tri (toggle ASC/DESC).

**Couche Minecraft :**
- `rarity/RarityDetector.java` — `ItemStack → ItemRarity?` via nom + LORE, avec cache.
- `rarity/RaritySorter.java` — `canSort(handler)` + `sort(...)` via `clickSlot`.
- `rarity/RarityScreenOverlay.java` — rendu : emblèmes, voile de filtre, barre de boutons, hitboxes.
- `mixin/HandledScreenMixin.java` — `render` (TAIL) + `mouseClicked` (HEAD).

**Modifs :**
- `config/ModConfig.java` — +3 booléens.
- `src/client/resources/minepiece-essentials.mixins.json` — +`HandledScreenMixin`.

---

## Task 1 : enum `ItemRarity`

**Files:**
- Create: `src/client/java/com/minepiece/essentials/rarity/ItemRarity.java`

- [ ] **Step 1 : écrire l'enum (pas de test dédié — couvert via RarityGlyphs/RaritySort)**

```java
package com.minepiece.essentials.rarity;

import net.minecraft.util.Identifier;

/**
 * Raretés MinePiece. Le glyphe est le codepoint que le resource pack serveur mappe
 * vers l'emblème (présent dans le nom/lore de l'item). La texture est celle du pack
 * (namespace "fonts"), référencée — jamais bundlée. `night` est volontairement absent
 * (codepoint inconnu, cf. spec — à ajouter après dump en jeu).
 */
public enum ItemRarity {
    COMMON   ('孔', "common",    11, 10, 0xFFFFFFFF, 0),
    RARE     ('桥', "rare",      11, 10, 0xFF5599FF, 1),
    EPIC     ('恨', "epic",      11, 10, 0xFFB24BFF, 2),
    LEGENDARY('伴', "legendary", 11, 10, 0xFFFFAA00, 3),
    MYTHIC   ('愈', "mythic",    11, 11, 0xFFFF5577, 4),
    LUNAR    ('灰', "lunar",     12, 10, 0xFF7FE0FF, 5),
    VALENTINE('挑', "valentine", 11, 11, 0xFFFF8FC8, 6);

    public final int glyph;
    public final String key;
    public final int nativeW;
    public final int nativeH;
    public final int color;   // ARGB, pour le bouton de filtre
    public final int rank;    // ordre de tri

    ItemRarity(char glyph, String key, int nativeW, int nativeH, int color, int rank) {
        this.glyph = glyph;
        this.key = key;
        this.nativeW = nativeW;
        this.nativeH = nativeH;
        this.color = color;
        this.rank = rank;
    }

    public Identifier texture() {
        return Identifier.of("fonts", "textures/font/rarity/items/icon/" + key + "_icons.png");
    }
}
```

- [ ] **Step 2 : compiler**

Run: `./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3 : commit**

```bash
git add src/client/java/com/minepiece/essentials/rarity/ItemRarity.java
git commit -m "feat(rarity): enum ItemRarity (glyphe, texture pack, couleur, rang)"
```

---

## Task 2 : détection par glyphe (pure, TDD)

**Files:**
- Create: `src/client/java/com/minepiece/essentials/rarity/RarityGlyphs.java`
- Test: `src/test/java/com/minepiece/essentials/rarity/RarityGlyphsTest.java`

- [ ] **Step 1 : écrire le test qui échoue**

```java
package com.minepiece.essentials.rarity;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RarityGlyphsTest {

    @Test
    void detectsEachRarityFromItsGlyph() {
        for (ItemRarity r : ItemRarity.values()) {
            assertEquals(r, RarityGlyphs.scan(String.valueOf((char) r.glyph) + " TEXTE"));
        }
    }

    @Test
    void returnsNullWhenNoGlyph() {
        assertNull(RarityGlyphs.scan("Bouche de Smack"));
        assertNull(RarityGlyphs.scan(""));
        assertNull(RarityGlyphs.scan(null));
    }

    @Test
    void scanLinesReturnsFirstMatch() {
        List<String> lines = List.of("ligne sans glyphe", "伴 LEGENDAIRE", "愈 MYTHIQUE");
        assertEquals(ItemRarity.LEGENDARY, RarityGlyphs.scanLines(lines));
    }

    @Test
    void scanLinesNullWhenNoneMatch() {
        assertNull(RarityGlyphs.scanLines(List.of("a", "b")));
    }
}
```

- [ ] **Step 2 : lancer → échoue (compile error : RarityGlyphs absent)**

Run: `./gradlew test --tests "com.minepiece.essentials.rarity.RarityGlyphsTest" -q`
Expected: échec de compilation / classe introuvable.

- [ ] **Step 3 : implémenter**

```java
package com.minepiece.essentials.rarity;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/** Détection de rareté par recherche du codepoint glyphe (langue-agnostique). */
public final class RarityGlyphs {
    private RarityGlyphs() {}

    @Nullable
    public static ItemRarity fromCodepoint(int cp) {
        for (ItemRarity r : ItemRarity.values()) {
            if (r.glyph == cp) return r;
        }
        return null;
    }

    @Nullable
    public static ItemRarity scan(@Nullable String s) {
        if (s == null || s.isEmpty()) return null;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            ItemRarity r = fromCodepoint(cp);
            if (r != null) return r;
            i += Character.charCount(cp);
        }
        return null;
    }

    @Nullable
    public static ItemRarity scanLines(@Nullable List<String> lines) {
        if (lines == null) return null;
        for (String line : lines) {
            ItemRarity r = scan(line);
            if (r != null) return r;
        }
        return null;
    }
}
```

- [ ] **Step 4 : lancer → passe**

Run: `./gradlew test --tests "com.minepiece.essentials.rarity.RarityGlyphsTest" -q`
Expected: PASS.

- [ ] **Step 5 : commit**

```bash
git add src/client/java/com/minepiece/essentials/rarity/RarityGlyphs.java \
        src/test/java/com/minepiece/essentials/rarity/RarityGlyphsTest.java
git commit -m "feat(rarity): détection de rareté par glyphe (+ tests)"
```

---

## Task 3 : calcul de l'ordre de tri (pure, TDD)

**Files:**
- Create: `src/client/java/com/minepiece/essentials/rarity/RaritySort.java`
- Test: `src/test/java/com/minepiece/essentials/rarity/RaritySortTest.java`

Modèle : une entrée par slot du conteneur. `rank = -1` ⇒ pas de rareté (toujours en fin).
La sortie est la **liste des index source** dans l'ordre cible (permutation), stable.

- [ ] **Step 1 : écrire le test qui échoue**

```java
package com.minepiece.essentials.rarity;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RaritySortTest {

    private static RaritySort.Entry e(int rank, String id) { return new RaritySort.Entry(rank, id); }

    @Test
    void descPutsHighestRankFirst() {
        // index: 0=rare(1), 1=mythic(4), 2=common(0)
        List<RaritySort.Entry> in = List.of(e(1, "a"), e(4, "b"), e(0, "c"));
        assertEquals(List.of(1, 0, 2), RaritySort.targetOrder(in, true));
    }

    @Test
    void ascPutsLowestRankFirst() {
        List<RaritySort.Entry> in = List.of(e(1, "a"), e(4, "b"), e(0, "c"));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, false));
    }

    @Test
    void noRarityAlwaysLastInBothDirections() {
        // index 1 has no rarity (-1)
        List<RaritySort.Entry> in = List.of(e(4, "a"), e(-1, "x"), e(0, "c"));
        assertEquals(List.of(0, 2, 1), RaritySort.targetOrder(in, true));
        assertEquals(List.of(2, 0, 1), RaritySort.targetOrder(in, false));
    }

    @Test
    void sameRankGroupedByItemIdThenStable() {
        // two legendaries (rank 3): ids "zz" and "aa" → grouped, sorted by id, stable
        List<RaritySort.Entry> in = List.of(e(3, "zz"), e(3, "aa"), e(0, "c"));
        // desc: rank3 first, within rank by id asc -> aa(1), zz(0); then common c(2)
        assertEquals(List.of(1, 0, 2), RaritySort.targetOrder(in, true));
    }
}
```

- [ ] **Step 2 : lancer → échoue**

Run: `./gradlew test --tests "com.minepiece.essentials.rarity.RaritySortTest" -q`
Expected: échec (classe absente).

- [ ] **Step 3 : implémenter**

```java
package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calcul pur de l'ordre cible d'un conteneur trié par rareté. */
public final class RaritySort {
    private RaritySort() {}

    /** rank = -1 ⇒ pas de rareté (placé en fin). itemId pour regrouper les identiques. */
    public record Entry(int rank, String itemId) {}

    /**
     * Renvoie la liste des index source dans l'ordre cible (permutation stable).
     * Les entrées sans rareté (rank < 0) finissent toujours en dernier, quel que soit
     * le sens. Au sein d'un même rang, regroupement par itemId (asc) ; tri stable.
     */
    public static List<Integer> targetOrder(List<Entry> entries, boolean descending) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) idx.add(i);

        Comparator<Integer> cmp = (a, b) -> {
            Entry ea = entries.get(a), eb = entries.get(b);
            boolean na = ea.rank() < 0, nb = eb.rank() < 0;
            if (na != nb) return na ? 1 : -1;          // sans-rareté en dernier
            if (!na) {                                  // les deux ont une rareté
                int c = descending ? Integer.compare(eb.rank(), ea.rank())
                                   : Integer.compare(ea.rank(), eb.rank());
                if (c != 0) return c;
                int byId = ea.itemId().compareTo(eb.itemId());
                if (byId != 0) return byId;            // regroupe les identiques
            }
            return Integer.compare(a, b);              // stable
        };
        idx.sort(cmp);
        return idx;
    }
}
```

- [ ] **Step 4 : lancer → passe**

Run: `./gradlew test --tests "com.minepiece.essentials.rarity.RaritySortTest" -q`
Expected: PASS.

- [ ] **Step 5 : commit**

```bash
git add src/client/java/com/minepiece/essentials/rarity/RaritySort.java \
        src/test/java/com/minepiece/essentials/rarity/RaritySortTest.java
git commit -m "feat(rarity): calcul pur de l'ordre de tri par rareté (+ tests)"
```

---

## Task 4 : états de session filtre + direction (pure, TDD)

**Files:**
- Create: `src/client/java/com/minepiece/essentials/rarity/RarityFilterState.java`
- Create: `src/client/java/com/minepiece/essentials/rarity/RaritySortState.java`
- Test: `src/test/java/com/minepiece/essentials/rarity/RarityStateTest.java`

- [ ] **Step 1 : écrire le test qui échoue**

```java
package com.minepiece.essentials.rarity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RarityStateTest {

    @Test
    void filterTogglesMultiple() {
        RarityFilterState f = new RarityFilterState();
        assertFalse(f.any());
        f.toggle(ItemRarity.MYTHIC);
        f.toggle(ItemRarity.LEGENDARY);
        assertTrue(f.isActive(ItemRarity.MYTHIC));
        assertTrue(f.isActive(ItemRarity.LEGENDARY));
        f.toggle(ItemRarity.MYTHIC);
        assertFalse(f.isActive(ItemRarity.MYTHIC));
        assertTrue(f.any());
    }

    @Test
    void dimmedWhenFilterActiveAndNotMatching() {
        RarityFilterState f = new RarityFilterState();
        assertFalse(f.isDimmed(ItemRarity.MYTHIC)); // aucun filtre → rien d'estompé
        assertFalse(f.isDimmed(null));
        f.toggle(ItemRarity.MYTHIC);
        assertFalse(f.isDimmed(ItemRarity.MYTHIC)); // correspond
        assertTrue(f.isDimmed(ItemRarity.RARE));    // ne correspond pas
        assertTrue(f.isDimmed(null));               // item sans rareté estompé
    }

    @Test
    void clearResetsFilter() {
        RarityFilterState f = new RarityFilterState();
        f.toggle(ItemRarity.RARE);
        f.clear();
        assertFalse(f.any());
    }

    @Test
    void sortDirectionToggles() {
        RaritySortState s = new RaritySortState();
        assertTrue(s.descending()); // défaut : mythique d'abord
        s.toggle();
        assertFalse(s.descending());
        s.toggle();
        assertTrue(s.descending());
    }
}
```

- [ ] **Step 2 : lancer → échoue**

Run: `./gradlew test --tests "com.minepiece.essentials.rarity.RarityStateTest" -q`
Expected: échec (classes absentes).

- [ ] **Step 3 : implémenter les deux classes**

`RarityFilterState.java` :
```java
package com.minepiece.essentials.rarity;

import java.util.EnumSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/** Filtre de surbrillance, multi-sélection, portée session. */
public final class RarityFilterState {
    private final Set<ItemRarity> active = EnumSet.noneOf(ItemRarity.class);

    public void toggle(ItemRarity r) {
        if (!active.add(r)) active.remove(r);
    }
    public boolean isActive(ItemRarity r) { return active.contains(r); }
    public boolean any() { return !active.isEmpty(); }
    public void clear() { active.clear(); }

    /** true si un filtre est actif et que cette rareté (ou l'absence de rareté) n'en fait pas partie. */
    public boolean isDimmed(@Nullable ItemRarity r) {
        if (active.isEmpty()) return false;
        return r == null || !active.contains(r);
    }
}
```

`RaritySortState.java` :
```java
package com.minepiece.essentials.rarity;

/** Direction de tri, toggle à chaque clic. Défaut : décroissant (mythique d'abord). */
public final class RaritySortState {
    private boolean descending = true;
    public boolean descending() { return descending; }
    public void toggle() { descending = !descending; }
}
```

- [ ] **Step 4 : lancer → passe**

Run: `./gradlew test --tests "com.minepiece.essentials.rarity.RarityStateTest" -q`
Expected: PASS.

- [ ] **Step 5 : commit**

```bash
git add src/client/java/com/minepiece/essentials/rarity/RarityFilterState.java \
        src/client/java/com/minepiece/essentials/rarity/RaritySortState.java \
        src/test/java/com/minepiece/essentials/rarity/RarityStateTest.java
git commit -m "feat(rarity): états session filtre (multi) + direction de tri (+ tests)"
```

---

## Task 5 : config (3 booléens)

**Files:**
- Modify: `src/client/java/com/minepiece/essentials/config/ModConfig.java`

- [ ] **Step 1 : ajouter les champs**

Après `public boolean ahPricePerUnitEnabled = true;` ajouter :
```java

    public boolean rarityIconsEnabled = true;
    public boolean rarityFilterEnabled = true;
    public boolean raritySorterEnabled = true;
```

- [ ] **Step 2 : compiler**

Run: `./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3 : commit**

```bash
git add src/client/java/com/minepiece/essentials/config/ModConfig.java
git commit -m "feat(rarity): toggles config (icônes, filtre, trieur)"
```

---

## Task 6 : `RarityDetector` (MC, lecture + cache)

**Files:**
- Create: `src/client/java/com/minepiece/essentials/rarity/RarityDetector.java`

Pas de test JUnit (dépend de Minecraft) — vérifié en jeu (Task 11). Le cache utilise
l'identité de l'`ItemStack` (les piles du `ScreenHandler` sont stables entre les frames).

- [ ] **Step 1 : implémenter**

```java
package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/** ItemStack → ItemRarity via le nom + le lore (glyphe du pack), avec cache par pile. */
public final class RarityDetector {
    private RarityDetector() {}

    // Sentinelle pour cacher "aucune rareté" sans confondre avec absence d'entrée.
    private static final ItemRarity NONE = null;
    private static final Object MISS = new Object();
    private static final WeakHashMap<ItemStack, Object> CACHE = new WeakHashMap<>();

    @Nullable
    public static ItemRarity detect(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return NONE;
        Object cached = CACHE.get(stack);
        if (cached != null) return cached == MISS ? NONE : (ItemRarity) cached;

        ItemRarity r = compute(stack);
        CACHE.put(stack, r == null ? MISS : r);
        return r;
    }

    @Nullable
    private static ItemRarity compute(ItemStack stack) {
        // 1) nom affiché (inclut le custom name serveur)
        ItemRarity r = RarityGlyphs.scan(stack.getName().getString());
        if (r != null) return r;

        // 2) lignes de lore
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            List<String> lines = new ArrayList<>();
            for (Text line : lore.lines()) lines.add(line.getString());
            r = RarityGlyphs.scanLines(lines);
        }
        return r;
    }
}
```

- [ ] **Step 2 : compiler**

Run: `./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL. (Si `LoreComponent.lines()` n'existe pas sous ce mapping,
vérifier le nom exact via la classe `net.minecraft.component.type.LoreComponent` dans les
sources décompilées — méthode renvoyant `List<Text>`.)

- [ ] **Step 3 : commit**

```bash
git add src/client/java/com/minepiece/essentials/rarity/RarityDetector.java
git commit -m "feat(rarity): RarityDetector (nom + lore, cache par pile)"
```

---

## Task 7 : `RaritySorter` (MC, canSort + clicks)

**Files:**
- Create: `src/client/java/com/minepiece/essentials/rarity/RaritySorter.java`

Sécurité : `canSort` n'autorise QUE les conteneurs de stockage standards
(`GenericContainerScreenHandler`, `ShulkerBoxScreenHandler`) → jamais l'AH/shop.
Le tri ne touche que les `containerSize` premiers slots (le conteneur, pas l'inventaire joueur).

- [ ] **Step 1 : implémenter**

```java
package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/** Tri du conteneur ouvert par rareté, via des clics d'inventaire simulés. */
public final class RaritySorter {
    private RaritySorter() {}

    /** Nombre de slots du conteneur (haut), ou -1 si non triable. */
    public static int containerSize(ScreenHandler h) {
        if (h instanceof GenericContainerScreenHandler g) return g.getRows() * 9;
        if (h instanceof ShulkerBoxScreenHandler) return 27;
        return -1;
    }

    public static boolean canSort(HandledScreen<?> screen) {
        return containerSize(screen.getScreenHandler()) > 0;
    }

    public static void sort(HandledScreen<?> screen, boolean descending) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        ScreenHandler h = screen.getScreenHandler();
        int n = containerSize(h);
        if (n <= 0 || n > h.slots.size()) return;

        // Snapshot des piles du conteneur (références stables).
        ItemStack[] cur = new ItemStack[n];
        List<RaritySort.Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ItemStack s = h.getSlot(i).getStack();
            cur[i] = s;
            if (s.isEmpty()) {
                entries.add(new RaritySort.Entry(-1, ""));
            } else {
                ItemRarity r = RarityDetector.detect(s);
                String id = s.getItem().toString();
                entries.add(new RaritySort.Entry(r == null ? -1 : r.rank, id));
            }
        }

        // Ordre cible : la pile qui doit finir au slot i.
        List<Integer> order = RaritySort.targetOrder(entries, descending);
        ItemStack[] desired = new ItemStack[n];
        for (int i = 0; i < n; i++) desired[i] = cur[order.get(i)];

        // Tri-sélection par swaps de 3 clics PICKUP, en miroir du modèle `cur`.
        int syncId = h.syncId;
        for (int i = 0; i < n; i++) {
            if (cur[i] == desired[i]) continue;
            int j = -1;
            for (int k = i + 1; k < n; k++) {
                if (cur[k] == desired[i]) { j = k; break; }
            }
            if (j < 0) continue; // robustesse : introuvable (resync), on saute
            // swap(i, j) : pickup i, click j, place i
            mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, j, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.PICKUP, mc.player);
            ItemStack tmp = cur[i]; cur[i] = cur[j]; cur[j] = tmp;
        }
    }
}
```

- [ ] **Step 2 : compiler**

Run: `./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL. (Vérifier les noms : `ScreenHandler.slots` (champ public
`DefaultedList<Slot>`), `ScreenHandler.getSlot(int)`, `ScreenHandler.syncId` (champ public),
`GenericContainerScreenHandler.getRows()`. Si un nom diffère, corriger via les sources MC.)

- [ ] **Step 3 : commit**

```bash
git add src/client/java/com/minepiece/essentials/rarity/RaritySorter.java
git commit -m "feat(rarity): RaritySorter (canSort liste blanche + tri par clickSlot)"
```

---

## Task 8 : helper de rendu texture non-16

**Files:**
- Modify: `src/client/java/com/minepiece/essentials/util/RenderUtils.java`

- [ ] **Step 1 : ajouter la méthode** (après `drawIcon`)

```java

    /**
     * Dessine une texture de taille native arbitraire ({@code texW}×{@code texH}),
     * mise à l'échelle par {@code scale}, en haut-gauche de {@code (x, y)}.
     */
    public static void drawTextureScaled(DrawContext ctx, Identifier texture,
                                         int x, int y, float scale, int texW, int texH) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, texW, texH, texW, texH);
        ctx.getMatrices().popMatrix();
    }
```

- [ ] **Step 2 : compiler**

Run: `./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3 : commit**

```bash
git add src/client/java/com/minepiece/essentials/util/RenderUtils.java
git commit -m "feat(rarity): RenderUtils.drawTextureScaled (textures non-16px)"
```

---

## Task 9 : `RarityScreenOverlay` (rendu emblèmes + voile + barre + hitboxes)

**Files:**
- Create: `src/client/java/com/minepiece/essentials/rarity/RarityScreenOverlay.java`

Tient l'état de session (filtre + direction) et expose les hitboxes pour le routage des clics.
La barre est verticale, collée à droite du fond du conteneur.

- [ ] **Step 1 : implémenter**

```java
package com.minepiece.essentials.rarity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.MinepieceEssentialsClient;
import com.minepiece.essentials.config.ModConfig;
import com.minepiece.essentials.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** Rendu de l'overlay rareté sur un HandledScreen + gestion des clics de la barre. */
public final class RarityScreenOverlay {
    private RarityScreenOverlay() {}

    public static final RarityFilterState FILTER = new RarityFilterState();
    public static final RaritySortState SORT = new RaritySortState();

    private static final int BTN = 14;       // taille d'un bouton
    private static final int GAP = 2;        // espace entre boutons
    private static final int PAD = 4;        // marge barre <-> conteneur

    /** Hitbox d'un bouton : type SORT (toggle direction), CLEAR (reset), ou une rareté. */
    public record Hit(int x, int y, int w, int h, ItemRarity rarity, boolean sort, boolean clear) {}

    private static final List<Hit> HITS = new ArrayList<>();

    private static ModConfig cfg() {
        return MinepieceEssentialsClient.getInstance().getConfigManager().config();
    }

    // ---- Rendu (appelé en TAIL de HandledScreen.render) ----
    public static void render(HandledScreen<?> screen, DrawContext ctx, int bgX, int bgY) {
        HITS.clear();
        if (!ServerDetector.isOnMinePiece()) return;
        ModConfig c = cfg();

        // 1) Emblèmes + voile sur chaque slot.
        boolean icons = c.rarityIconsEnabled;
        boolean filterOn = c.rarityFilterEnabled && FILTER.any();
        if (icons || filterOn) {
            for (Slot slot : screen.getScreenHandler().slots) {
                ItemStack st = slot.getStack();
                if (st.isEmpty()) continue;
                ItemRarity r = RarityDetector.detect(st);
                int sx = bgX + slot.x, sy = bgY + slot.y;
                if (filterOn && FILTER.isDimmed(r)) {
                    ctx.fill(sx, sy, sx + 16, sy + 16, 0xB0101010); // voile sombre
                }
                if (icons && r != null) {
                    float scale = 8f / Math.max(r.nativeW, r.nativeH); // ~8px de haut
                    RenderUtils.drawTextureScaled(ctx, r.texture(), sx + 1, sy, scale,
                            r.nativeW, r.nativeH);
                }
            }
        }

        // 2) Barre verticale à droite : raretés présentes + Trier + Clear.
        Set<ItemRarity> present = EnumSet.noneOf(ItemRarity.class);
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemRarity r = RarityDetector.detect(slot.getStack());
            if (r != null) present.add(r);
        }
        if (present.isEmpty()) return;

        int barX = bgX + screen.getScreenHandler().slots.stream()
                .mapToInt(s -> s.x).max().orElse(0) + 16 + PAD; // bord droit des slots + marge
        // approximation simple : à droite du fond. Ajustable en test.
        barX = bgX + 176 + PAD; // largeur de fond vanilla standard
        int y = bgY;

        if (c.rarityFilterEnabled) {
            for (ItemRarity r : ItemRarity.values()) {
                if (!present.contains(r)) continue;
                boolean act = FILTER.isActive(r);
                int border = act ? 0xFFFFFFFF : 0xFF000000;
                ctx.fill(barX - 1, y - 1, barX + BTN + 1, y + BTN + 1, border);
                ctx.fill(barX, y, barX + BTN, y + BTN, r.color);
                if (RarityDetector.detect(ItemStack.EMPTY) == null && r != null) {
                    float sc = 10f / Math.max(r.nativeW, r.nativeH);
                    RenderUtils.drawTextureScaled(ctx, r.texture(), barX + 2, y + 2, sc,
                            r.nativeW, r.nativeH);
                }
                HITS.add(new Hit(barX, y, BTN, BTN, r, false, false));
                y += BTN + GAP;
            }
            if (FILTER.any()) {
                ctx.fill(barX, y, barX + BTN, y + BTN, 0xFF802020);
                RenderUtils.drawText(ctx, "x", barX + 4, y + 3, 0xFFFFFFFF);
                HITS.add(new Hit(barX, y, BTN, BTN, null, false, true));
                y += BTN + GAP;
            }
        }

        // Bouton Trier (seulement sur conteneur de stockage).
        if (c.raritySorterEnabled && RaritySorter.canSort(screen)) {
            y += GAP;
            ctx.fill(barX, y, barX + BTN, y + BTN, 0xFF2A2A2A);
            RenderUtils.drawText(ctx, SORT.descending() ? "↓" : "↑",
                    barX + 4, y + 3, 0xFFFFFFFF); // flèche ↓ / ↑
            HITS.add(new Hit(barX, y, BTN, BTN, null, true, false));
        }
    }

    // ---- Clic (appelé en HEAD de HandledScreen.mouseClicked) ----
    /** @return true si le clic a été consommé par la barre. */
    public static boolean onClick(HandledScreen<?> screen, double mx, double my) {
        if (!ServerDetector.isOnMinePiece()) return false;
        for (Hit h : HITS) {
            if (mx >= h.x() && mx < h.x() + h.w() && my >= h.y() && my < h.y() + h.h()) {
                if (h.sort()) {
                    SORT.toggle();
                    RaritySorter.sort(screen, SORT.descending());
                } else if (h.clear()) {
                    FILTER.clear();
                } else if (h.rarity() != null) {
                    FILTER.toggle(h.rarity());
                }
                return true;
            }
        }
        return false;
    }
}
```

> Note : le calcul de `barX` retombe sur la largeur de fond vanilla (176 px). C'est
> volontairement simple ; on l'ajuste visuellement au test en jeu (Task 11).

- [ ] **Step 2 : compiler**

Run: `./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL. (Vérifier `Slot.x`/`Slot.y` (champs publics int), accessibles.)

- [ ] **Step 3 : commit**

```bash
git add src/client/java/com/minepiece/essentials/rarity/RarityScreenOverlay.java
git commit -m "feat(rarity): overlay (emblèmes, voile filtre, barre droite, hitboxes)"
```

---

## Task 10 : mixin `HandledScreen` + enregistrement

**Files:**
- Create: `src/client/java/com/minepiece/essentials/mixin/HandledScreenMixin.java`
- Modify: `src/client/resources/minepiece-essentials.mixins.json`

- [ ] **Step 1 : écrire le mixin**

```java
package com.minepiece.essentials.mixin;

import com.minepiece.essentials.rarity.RarityScreenOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dessine l'overlay rareté et route les clics de la barre. */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow protected int x; // coin haut-gauche du fond
    @Shadow protected int y;

    @Inject(method = "render", at = @At("TAIL"))
    private void minepiece$rarityOverlay(DrawContext ctx, int mouseX, int mouseY,
                                         float delta, CallbackInfo ci) {
        RarityScreenOverlay.render((HandledScreen<?>) (Object) this, ctx, this.x, this.y);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void minepiece$rarityClick(double mouseX, double mouseY, int button,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (RarityScreenOverlay.onClick((HandledScreen<?>) (Object) this, mouseX, mouseY)) {
            cir.setReturnValue(true);
        }
    }
}
```

- [ ] **Step 2 : enregistrer le mixin** — dans `minepiece-essentials.mixins.json`, ajouter
`"HandledScreenMixin"` à la liste `client` :

```json
    "client": [
        "BossBarHudMixin",
        "ClientPlayNetworkHandlerMixin",
        "HandledScreenMixin",
        "InGameHudMixin",
        "MinecraftClientMixin"
    ],
```

- [ ] **Step 3 : build complet**

Run: `./gradlew build -q`
Expected: BUILD SUCCESSFUL (compile + tests + jar). Si le mixin ne s'applique pas (nom de
méthode `render`/`mouseClicked` introuvable sous ce mapping), vérifier les signatures dans
`HandledScreen` décompilé et ajuster le `method =`.

- [ ] **Step 4 : commit**

```bash
git add src/client/java/com/minepiece/essentials/mixin/HandledScreenMixin.java \
        src/client/resources/minepiece-essentials.mixins.json
git commit -m "feat(rarity): mixin HandledScreen (overlay + clics barre)"
```

---

## Task 11 : test en jeu (manuel)

**Files:** aucun (validation runtime).

- [ ] **Step 1 : lancer le client de test**

Run: `./gradlew runClient`
(ou build → déploiement Prism selon le workflow habituel — cf. memory testing-workflow.)

- [ ] **Step 2 : checklist en jeu sur MinePiece**

  - Ouvrir l'inventaire (E) → emblèmes en haut-gauche des items à rareté ; rien sur les autres.
  - Ouvrir un coffre → emblèmes OK ; **barre de boutons à droite** ; bouton **Trier** présent.
  - Cliquer une rareté → cases des autres raretés **estompées** ; multi-sélection OK ; « x » reset.
  - Cliquer **Trier** → le coffre se range par rareté ; re-cliquer → ordre inversé ; flèche ↓/↑.
  - Vérifier : **aucun item perdu**, curseur vide après tri.
  - Ouvrir l'**AH** (image 4) : emblèmes/filtre OK mais **bouton Trier ABSENT** (sécurité).
  - Hors MinePiece (serveur quelconque) → **rien** ne s'affiche.
  - Ajuster `barX` dans `RarityScreenOverlay` si la barre chevauche / déborde de l'écran.

- [ ] **Step 3 : commit d'éventuels ajustements**

```bash
git add -A && git commit -m "fix(rarity): ajustements visuels après test en jeu"
```

---

## Self-Review (effectuée)

- **Couverture spec** : emblèmes (T1/6/8/9), filtre multi (T4/9), trieur toggle (T3/4/7/9),
  garde-fou AH (T7 `canSort`, vérifié T11), fallback hors MinePiece (T9 gating), config (T5),
  position barre à droite (T9), perf cache (T6), tests purs (T2/3/4). ✔
- **Placeholders** : aucun TODO ; tout le code est fourni. Les seuls points « à vérifier »
  sont des noms de mappings yarn, validés par `./gradlew build`/`runClient` (T6/7/9/10) —
  ce sont des vérifications, pas des trous de spec.
- **Cohérence des types** : `ItemRarity` (rank/glyph/texture/color) utilisé tel quel partout ;
  `RaritySort.Entry(int,String)`, `RarityFilterState`/`RaritySortState` cohérents entre tâches
  et tests ; `RarityScreenOverlay.FILTER/SORT` réutilisés par le mixin.
- **Risques connus** : mappings MC version-spécifiques (méthodes `render`/`mouseClicked`,
  `LoreComponent.lines()`, `Slot.x/y`, `ScreenHandler.slots/syncId/getSlot`,
  `GenericContainerScreenHandler.getRows()`) — à confirmer au build. `night` non géré (spec).
