# Emblèmes de rareté + filtre de cases — Design

**Date :** 2026-06-03
**Module :** `com.minepiece.essentials.rarity`
**Statut :** validé en brainstorming, prêt pour plan d'implémentation

## Contexte & contrainte

Mod **public** MinePiece Essentials. Ligne rouge : **aucune automatisation ni injection
d'inputs**. Cette feature est **100 % passive** — elle lit l'état du jeu (lore des items)
et **dessine** par-dessus les cases. Elle ne déplace, ne clique et ne range **aucun** item.

> Le « trieur de coffre automatique » demandé initialement est **hors périmètre** ici
> (il simulerait des clics = automatisation). Il appartient au mod privé. On le remplace
> par une **aide visuelle au tri** : emblèmes + filtre de surbrillance.

## Objectif

1. **Emblèmes de rareté** affichés dans le coin de chaque case d'item, dans
   l'inventaire joueur, les coffres/conteneurs et les GUI serveur (AH, shops).
2. **Barre de filtres** : boutons (1 par rareté) ; activer une ou plusieurs raretés
   estompe visuellement les cases qui ne correspondent pas. Purement visuel.

## Décisions de design (issues du brainstorming)

| Sujet | Décision |
|---|---|
| Marqueur | Le **vrai emblème** du resource pack serveur (celui à gauche de « LEGENDAIRE », etc.) |
| Source des textures | **Référencées** par `Identifier`, jamais bundlées (comme `king_haki`) → pas de souci IP |
| Écrans | Inventaire joueur + coffres/conteneurs + GUI serveur. **Pas** la hotbar. |
| Pilotage du filtre | **Petits boutons de filtre** dessinés à côté du conteneur |
| Filtre multi | **Toggles multiples** (ex. légendaire + mythique vifs en même temps) |
| Fallback hors MinePiece | **Rien** (pack absent → emblèmes ne chargent pas → on ne dessine rien) |
| Position emblème | Coin **haut-gauche** de la case (le nombre est en bas-droite → pas de collision) |
| Persistance du filtre | État de session uniquement (non sauvegardé) |

## Détection de rareté

Les emblèmes du pack sont des **glyphes de police** ; chaque rareté a un codepoint
présent dans la **ligne de rareté du lore** de l'item. Mapping confirmé (cohérent avec
`ParcheminScanner`) :

| Glyphe | Codepoint | Rareté | Texture (`fonts:`) |
|---|---|---|---|
| 孔 | U+5B54 | commun | `textures/font/rarity/items/icon/common_icons.png` |
| 桥 | U+6865 | rare | `…/rare_icons.png` |
| 恨 | U+6068 | épique | `…/epic_icons.png` |
| 伴 | U+4F34 | légendaire | `…/legendary_icons.png` |
| 愈 | U+6108 | mythique | `…/mythic_icons.png` |
| 灰 | U+7070 | lunaire | `…/lunar_icons.png` |
| 挑 | U+6311 | valentine | `…/valentine_icons.png` |
| (?) | — | night | `…/night_icons.png` (16×16 ; codepoint à confirmer en jeu) |

**Méthode :** lire le composant **`DataComponentTypes.LORE`** de l'`ItemStack` (et non
`getTooltip(...)`, trop lourd), parcourir les lignes, chercher le premier codepoint connu.
Premier match → `ItemRarity`. Aucun match → `null` (item ignoré).

## Composants

### `ItemRarity` (enum)
- Valeurs : `COMMON, RARE, EPIC, LEGENDARY, MYTHIC, LUNAR, VALENTINE, NIGHT`.
- Champs : codepoint (glyphe), `Identifier` de la texture emblème, couleur
  (réutiliser `ascension.RarityColors`), libellé d'affichage.
- `static ItemRarity fromGlyph(int codepoint)`.

### `RarityDetector`
- `static @Nullable ItemRarity detect(ItemStack stack)` : lit le LORE, scanne les glyphes.
- **Cache** par stack pour éviter le rescan chaque frame (clé = identité/hash de la stack ;
  invalidé quand la stack du slot change). Coût négligeable.

### `RarityIconRenderer`
- `static void drawEmblem(DrawContext ctx, ItemRarity r, int slotX, int slotY)` :
  dessine la texture du pack dans le coin haut-gauche, taille ~7–8 px, via `DrawContext`
  (même approche que `RenderUtils.drawIcon` / `HakiHud`).
- Si la texture n'existe pas (pack absent) → ne dessine rien (fallback « rien »).

### `RarityFilterState`
- `Set<ItemRarity>` actif (toggles multiples), portée session.
- `toggle(r)`, `isActive(r)`, `boolean any()`, `boolean isDimmed(ItemRarity r)`
  (true si un filtre est actif **et** que `r` n'en fait pas partie ; les items sans
  rareté sont estompés quand au moins un filtre est actif).

### `RarityFilterBar`
- Dessine une rangée de boutons (un par rareté **présente dans le conteneur ouvert**),
  à droite du conteneur (sinon au-dessus si pas la place). Bouton actif mis en évidence.
- Bouton « ✕ » pour tout réinitialiser.
- Renvoie les hitboxes pour que le mixin route les clics.

### `HandledScreenMixin` (mixin client)
Cible `HandledScreen` (couvre `InventoryScreen`, `GenericContainerScreen`, GUI serveur).
- **Après** le rendu des slots :
  1. pour chaque slot non vide → `RarityDetector.detect` → si filtre actif et estompé,
     poser un voile sombre sur la case ; puis `RarityIconRenderer.drawEmblem`.
  2. dessiner `RarityFilterBar`.
- `mouseClicked` : si le clic tombe sur une hitbox de la barre → `toggle` et **annuler**
  l'événement (sinon laisser passer). Aucun autre clic n'est touché.

### `ModConfig` (ajouts)
- `rarityIconsEnabled` (def. true)
- `rarityFilterEnabled` (def. true)
- `rarityIconScale` (def. 1.0)
- exposés dans le menu config existant, sous la logique de gating MinePiece.

## Flux de données

```
ouverture HandledScreen
  └─ render() (après slots)
       └─ pour chaque Slot non vide:
            RarityDetector.detect(stack)  ── cache ──> ItemRarity?
            ├─ filtre actif & estompé  → voile sombre
            └─ ItemRarity != null      → drawEmblem(coin HG)
       └─ RarityFilterBar.render(raretés présentes, état)
  └─ mouseClicked()
       └─ hit bouton filtre → RarityFilterState.toggle() ; cancel
```

## Gestion d'erreurs / cas limites

- **Pack absent / hors MinePiece** : textures introuvables → rien dessiné. La barre de
  filtres n'apparaît pas si aucune rareté détectée.
- **Item sans rareté** : ignoré pour l'emblème ; estompé si un filtre est actif.
- **`night`** : codepoint inconnu pour l'instant → à confirmer via dump en jeu ; gérer
  proprement l'absence (pas de crash).
- **GUI serveur custom** : la barre peut chevaucher la mise en page → position calculée à
  droite du conteneur ; option de repli au-dessus. Vérifier sur l'AH en jeu.
- **Perf** : LORE lu une fois par stack puis caché ; pas de `getTooltip` en boucle.

## Tests

- **Détection** : unit test `RarityDetector` avec des `ItemStack` montés avec des lignes de
  lore contenant chaque glyphe → vérifie le bon `ItemRarity` (et `null` sans glyphe).
- **État filtre** : unit test `RarityFilterState` (toggle multiple, `isDimmed`).
- **Rendu** : test manuel en jeu (build → deploy Prism → ouvrir inventaire/coffre/AH),
  selon le workflow de test habituel (cf. memory testing-workflow).

## Hors périmètre (YAGNI)

- Tri/déplacement automatique d'items (= automatisation, interdit ici).
- Emblèmes sur la hotbar.
- Persistance du filtre entre sessions.
- Support multi-langue de la détection (la détection par glyphe est langue-agnostique).
