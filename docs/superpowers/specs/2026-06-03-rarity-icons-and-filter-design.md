# Emblèmes de rareté + filtre de cases — Design

**Date :** 2026-06-03
**Module :** `com.minepiece.essentials.rarity`
**Statut :** validé en brainstorming, prêt pour plan d'implémentation

## Contexte & contrainte

Mod **public** MinePiece Essentials. La ligne rouge habituelle est « aucune automatisation »,
mais une **exception assumée** a été décidée le 2026-06-03 (cf. CLAUDE.md) : le **trieur de
coffre par rareté** est autorisé ici, à titre de QoL, avec **rollback prévu** si un staff
RIVRS le juge interdit.

Donc deux natures de code dans ce module :
- **Affichage passif** (emblèmes + filtre de surbrillance) — lit le lore et dessine.
- **Trieur** (la seule automatisation tolérée) — réordonne le conteneur ouvert via des
  clics d'inventaire simulés, déclenché **uniquement** par un clic explicite de l'utilisateur
  sur le bouton « Trier ».

> Règlement serveur (vérifié) : le tri d'inventaire/coffre n'est **pas** nommé en interdit ;
> l'article G vise les automatisations à avantage (AutoSell/AutoMine/AutoPêche…). Zone grise
> assumée. Si retrait demandé → supprimer au moins la partie tri du module `rarity/`.

## Objectif

1. **Emblèmes de rareté** affichés dans le coin de chaque case d'item, dans
   l'inventaire joueur, les coffres/conteneurs et les GUI serveur (AH, shops).
2. **Barre de filtres** : boutons (1 par rareté) ; activer une ou plusieurs raretés
   estompe visuellement les cases qui ne correspondent pas. Purement visuel.
3. **Trieur de coffre** : un bouton « Trier » qui réordonne le conteneur ouvert par
   rareté. Un seul bouton, qui **bascule croissant ⇄ décroissant** à chaque clic.

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
| Trieur — cible | **Conteneur ouvert uniquement** (pas l'inventaire perso) |
| Trieur — ordre | Un bouton « Trier » qui **toggle croissant ⇄ décroissant** ; libellé/flèche reflète l'état |
| Trieur — sécurité | Bouton **affiché seulement sur un vrai conteneur de stockage** (coffre/shulker/`GenericContainerScreen` standard) ; **jamais** sur un GUI menu serveur (AH/shop) où un clic auto pourrait acheter/valider |
| Trieur — placement non-raretés | Items sans rareté regroupés à la fin ; stacks identiques regroupés |

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
- **Bouton « Trier »** intégré à la barre (visible seulement sur un conteneur de stockage,
  cf. `RaritySorter.canSort`). Affiche une flèche ↑/↓ selon `RaritySortState.direction`.

### `RaritySortState`
- `enum Direction { ASC, DESC }`, état de session, défaut `DESC` (mythique d'abord).
- `toggle()` : bascule la direction (appelé à chaque clic du bouton « Trier »).

### `RaritySorter` (la seule automatisation du module)
- `static boolean canSort(HandledScreen<?> screen)` : true **uniquement** pour un vrai
  conteneur de stockage (ex. `GenericContainerScreenHandler`/`ShulkerBoxScreenHandler` ;
  pas l'inventaire de craft, **pas** les GUI menu serveur). Liste blanche de types de
  `ScreenHandler` → garantit qu'on ne clique jamais dans un shop/AH.
- `static void sort(HandledScreen<?> screen, Direction dir)` :
  1. Lit les slots du **conteneur** (partie haute, hors inventaire joueur) : pour chaque
     slot, `(ItemStack, ItemRarity?)`.
  2. Calcule l'ordre cible : tri par rang de rareté (selon `dir`), non-raretés à la fin,
     stacks identiques regroupés, ordre stable secondaire (id d'item + count).
  3. Réalise la permutation par **swaps** : `client.interactionManager.clickSlot(syncId,
     slotId, 0, SlotActionType.PICKUP, player)` (prise → dépose) pour amener chaque item à
     sa position cible. Algorithme type tri-sélection sur les slots du conteneur.
  4. Ne touche **jamais** aux slots de l'inventaire joueur ni au curseur en fin (curseur
     vide à la fin).
- Robustesse : borne le nombre de clics (taille du conteneur), s'arrête proprement si la
  taille/els slots changent en cours (resync serveur).

### `HandledScreenMixin` (mixin client)
Cible `HandledScreen` (couvre `InventoryScreen`, `GenericContainerScreen`, GUI serveur).
- **Après** le rendu des slots :
  1. pour chaque slot non vide → `RarityDetector.detect` → si filtre actif et estompé,
     poser un voile sombre sur la case ; puis `RarityIconRenderer.drawEmblem`.
  2. dessiner `RarityFilterBar`.
- `mouseClicked` : si le clic tombe sur une hitbox de la barre :
  - bouton de rareté → `RarityFilterState.toggle(r)` ;
  - bouton « Trier » → `RaritySortState.toggle()` puis `RaritySorter.sort(screen, dir)` ;
  - bouton « ✕ » → reset filtre ;
  - puis **annuler** l'événement. Aucun autre clic n'est touché (drag/dépose manuelle
    de l'utilisateur intacts).

### `ModConfig` (ajouts)
- `rarityIconsEnabled` (def. true)
- `rarityFilterEnabled` (def. true)
- `raritySorterEnabled` (def. true) — masque le bouton « Trier » si false
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
       └─ RarityFilterBar.render(raretés présentes, état, bouton Trier si canSort)
  └─ mouseClicked()
       ├─ hit bouton filtre → RarityFilterState.toggle() ; cancel
       └─ hit bouton Trier  → RaritySortState.toggle()
                               └─ RaritySorter.sort() → clickSlot(PICKUP) × N ; cancel
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
- **Trieur — sécurité GUI serveur** : `canSort` = liste blanche de `ScreenHandler` de
  stockage. Sur un AH/shop, le bouton « Trier » **n'apparaît pas** → impossible de cliquer
  par erreur sur un item d'achat.
- **Trieur — resync** : si le serveur renvoie un état pendant le tri (slots changent), on
  arrête la séquence proprement (pas de clic sur slot invalide), curseur laissé vide.
- **Trieur — conteneurs en lecture seule / restreints** : si un slot refuse la prise, on
  passe au suivant sans bloquer.

## Tests

- **Détection** : unit test `RarityDetector` avec des `ItemStack` montés avec des lignes de
  lore contenant chaque glyphe → vérifie le bon `ItemRarity` (et `null` sans glyphe).
- **État filtre** : unit test `RarityFilterState` (toggle multiple, `isDimmed`).
- **Tri (logique pure)** : unit test sur le calcul de l'**ordre cible** (`RaritySorter`
  factorisé : une fonction pure `List<slot> → List<slot>` testable sans Minecraft) pour
  ASC/DESC, non-raretés à la fin, regroupement des stacks identiques.
- **`canSort`** : test que les types menu serveur sont exclus (au minimum vérification
  manuelle de la liste blanche).
- **Rendu + tri réel** : test manuel en jeu (build → deploy Prism → ouvrir inventaire /
  coffre / shulker / AH), selon le workflow de test habituel (cf. memory testing-workflow).
  Vérifier : bouton Trier absent sur l'AH, présent sur un coffre ; tri correct ; toggle
  croissant/décroissant ; aucun item perdu ; curseur vide après tri.

## Hors périmètre (YAGNI)

- Tri de l'inventaire **joueur** (le trieur ne touche que le conteneur ouvert).
- Tri sur les GUI menu serveur (AH/shop) — volontairement exclu pour sécurité.
- Emblèmes sur la hotbar.
- Persistance du filtre / de la direction de tri entre sessions.
- Support multi-langue de la détection (la détection par glyphe est langue-agnostique).
- Critères de tri autres que la rareté (alpha, quantité…) — possibles plus tard.
