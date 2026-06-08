# Design — Surbrillance prix AH (liseré couleur + ligne infobulle)

Date : 2026-06-08

## Objectif

Dans le menu /ah (et tout conteneur affichant des annonces), aider à juger un prix
d'un coup d'œil : entourer chaque case d'un **liseré coloré** selon l'écart entre le
prix de vente de l'annonce et le prix moyen du marché, et ajouter une **ligne dans
l'infobulle** (au survol) avec le pourcentage exact.

Fonctionnalité **passive** (lecture du lore + affichage), conforme au mod public.

## Déclenchement

- S'applique à chaque case (`Slot`) d'un `HandledScreen` dont l'item porte **à la fois**
  les lignes de lore `Prix de vente` **et** `Prix moyen`. Cela limite l'effet aux items
  d'annonce AH sans détecter le titre de l'écran.
- Gated par `ServerDetector.isOnMinePiece()` et par le réglage `ahPriceColorEnabled`.

## Logique couleur

Écart relatif : `ratio = (prixVente − prixMoyen) / prixMoyen`.

| Bande | Condition | Couleur | Sens |
|-------|-----------|---------|------|
| Bonne affaire | `ratio < −0.10` | jaune | moins cher que la moyenne |
| Au prix | `−0.10 ≤ ratio ≤ +0.10` | vert | proche de la moyenne |
| Trop cher | `ratio > +0.10` | rouge | au-dessus, « ne pas acheter » |

Les bornes ±10 % sont inclusives côté vert.

## Rendu

1. **Liseré** : un cadre de 1–2 px de la couleur de la bande dessiné autour de la case
   16×16, par-dessus le rendu des items, dans la boucle de slots existante de
   `RarityScreenOverlay.render` (`sx = bgX + slot.x`, `sy = bgY + slot.y`). Technique
   identique aux bordures de boutons de filtre rareté (`ctx.fill` des 4 côtés). L'item
   reste entièrement net (pas de voile par-dessus).
2. **Infobulle** : via le `ItemTooltipCallback` existant dans `AhTooltip`, ajouter une
   ligne `▪ ▲ +15 % vs moyenne` (▲ si plus cher / ▼ si moins cher), colorée selon la
   bande. Affichée dès que prix vente + prix moyen sont présents (indépendant du
   nombre dans la pile). **Rendu du % à affiner en jeu** (notamment affichage ou non
   du petit écart dans la bande verte).

## Interaction avec l'overlay rareté

- Le liseré prix et l'emblème de rareté (coin) coexistent : bordure vs coin, pas de
  conflit visuel.
- Si le filtre rareté estompe une case (voile sombre), le voile passe **par-dessus** le
  liseré (l'item estompé reste visiblement filtré).

## Config / toggles

- Nouveau champ `ahPriceColorEnabled` dans `ModConfig` (défaut : activé).
- Toggle dans l'éditeur **K**, à côté des toggles rareté / Prix/u.
- Indépendant de `ahPricePerUnitEnabled` (le « Prix/u » existant).

## Cas limites

- Pas de couleur ni de ligne si : `Prix moyen` absent ou = 0, `Prix de vente` absent,
  ou un seul des deux présent.
- Calcul en `double` ; pourcentage arrondi à l'entier pour l'affichage.
- Prix abrégés (K/M/B/T, décimaux) déjà gérés par `AhPriceParser.parseAbbreviated`.

## Découpage / fichiers

- `ah/AhPriceBand` (nouveau) : logique pure. `band(double sell, double avg)` →
  `{ Band kind (CHEAP/FAIR/EXPENSIVE), double ratio, int color }`. Réutilise
  `AhPriceParser` pour extraire les deux prix d'une liste de lore.
- `ah/AhTooltip` : ajoute la ligne infobulle (réutilise `AhPriceBand`).
- `rarity/RarityScreenOverlay` (ou un petit `ah/AhPriceOverlay` appelé depuis la même
  boucle) : dessine le liseré par case.
- `config/ModConfig` + éditeur K : nouveau toggle.

## Tests

Logique pure (`AhPriceBandTest`) :
- au-dessus de +10 % → EXPENSIVE/rouge ; en dessous de −10 % → CHEAP/jaune ;
  dans la bande → FAIR/vert.
- bornes exactes (−10 % et +10 % → vert).
- prix moyen 0 / absent → pas de bande.
- extraction des deux prix depuis une liste de lore réaliste (abréviations + glyphe `实`).

Le rendu (liseré, infobulle) se vérifie en jeu.

## Hors périmètre (YAGNI)

- Pas de % écrit dans chaque case (rejeté : surcharge la grille).
- Pas de fond plein translucide (rejeté au profit du liseré, plus net).
- Pas de seuils configurables : ±10 % en dur (on ajustera si besoin).
