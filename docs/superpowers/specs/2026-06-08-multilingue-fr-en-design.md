# Design — Support multilingue (FR + EN)

Date : 2026-06-08

## Problème

Le serveur MinePiece envoie ses textes (lore d'items, écrans GUI, messages système
comme le haki et les complétions) dans la **« Game language »** du joueur, réglée
**côté serveur** via la commande `/lang` (distincte du « Chat language » et de la
langue du client Minecraft). Les parsers du mod matchent des chaînes **françaises**
codées en dur ; pour un joueur dont la Game language n'est pas le français, tous les
parsers échouent **en silence** → plus de boss timers, % pets, progression de pass,
prix AH, etc. (bug confirmé en prod, joueur sungod).

La Game language **n'est pas lisible depuis le client** (réglage serveur par joueur).
Décision : les parsers **matchent FR et EN simultanément** (pas de détection). Le
serveur gère d'autres langues (italien, turc…) ; l'architecture doit permettre de les
ajouter plus tard sans refonte.

## Objectif

1. **Chantier B (parsers bilingues)** — le mod fonctionne identiquement que la Game
   language serveur soit le français ou l'anglais.
2. **Chantier A (UI)** — l'interface du mod (titres HUD, aide, boutons) s'affiche
   dans la langue **du client Minecraft** via les fichiers de langue standard.

Hors périmètre : italien/turc/autres (extensible plus tard) ; détection automatique
de la Game language ; toute lecture du chat joueur (interdite).

## Architecture

### `i18n/ServerText` — registre central des chaînes serveur

Une classe unique regroupe, pour chaque concept, ses variantes **FR + EN**, et expose :

- des **tests** `matchesXxx(String line)` (renvoient vrai si la ligne contient une
  variante connue), pour les cas en `contains` ;
- des **`Pattern` combinés précompilés** (`(?:fr|en)…`) `static final`, pour les cas
  regex (niveau pet, coordonnées, respawn, date d'expiration, nom de quête…).

Les parsers référencent `ServerText` au lieu de littéraux. Ajouter une langue =
compléter les listes/alternatives dans ce seul fichier.

Principe anti-collision : préférer la variante la plus spécifique ; quand un mot FR
**contient** déjà la racine EN (ou inverse), matcher la racine commune une seule fois
(ex. « Progress » couvre FR « Progression » ; « compl » couvre « complété » et
« completed »).

### Correspondance FR → EN (relevée par dump du serveur en Game language = English)

| Concept | FR | EN |
|---------|----|----|
| AH prix vente | `Prix de vente` | `Selling price` |
| AH prix moyen | `Prix moyen` | `Average price` (valeur `?` si aucune moyenne → ignorée) |
| Pet section | `Familier Effects` | `Familiar Effects` |
| Pet actif (action) | `Désactiver` | `Disable` |
| Pet inactif (action) | `Activer` | `Activate` |
| Pet niveau | `Niveau:` / `Max` | `Level:` / `Level: Max` |
| Pet stats | `Force`,`Puissance`,`Défense`,`Chance Critique`,`Vie`,`Dextérité`,`Vitesse`,`Dégâts Critiques`,`Régén. Énergie` | `Strength`,`Power`,`Defense`,`Critical Chance`,`Health`(/`Life`),`Dexterity`,`Speed`,`Critical Damage`,`Energy Regen` |
| Pet stat masquée | `Non découvert` | `Not discovered` |
| Parchemin (nom) | `Parchemin` | `Scroll` |
| Parchemin expiration | `Expire le JJ/MM/AAAA HHhMM` | `Expires on JJ/MM/AAAA HHhMM` (format date identique) |
| Objectif | `Objectif` | `Objective` |
| Rareté spéciale | `lunaire` | `Lunar` |
| Quête pass (nom) | `Quête #N (Facile/Moyenne/Difficile)` | `Quest #N (Easy/Medium/Hard)` |
| Pass progression | `Progression` | `Progress` |
| Pass statut | `Statut` | `Status` |
| Pass non complété | `Non compl…` | `Not compl…` |
| Boss coordonnées | `Coordonnées:` | `Coordinates:` |
| Boss respawn | `Réapparition/Apparition: …m …s` | `Respawn: …s (N Minutes)` |
| Boss catégorie mini-boss | `présents sur cette île` / `mini-boss` | `present on this island` / `present in the city` / `Mini-Bosses` |
| Boss unique | `le Boss de cette île` | `the Boss of this island` |
| Boss loot/voir | `voir les loots` | `View loots` / `View the loots` / `View relics` |
| Boss nav | `retour` / `page` / `fermer` | `Back` / `Page` / `Close` |
| Boss mots-clés mobs | `marine`,`mob`,`monstre`,`bandit`,`combat`,`ennemi`,`garde`,`pirate`,`zombie`,`chasseur` | `marine(s)`,`mob`,`monster(s)`,`bandit`,`fight`,`enemy`,`guard`,`pirate`,`zombie`,`hunter`,`soldier` |
| Haki activé | `Vous avez activé le haki` | `You have activated haki` |
| Haki prêt | `Vous pouvez de nouveau utiliser votre haki` | `You can use your haki` |
| Îles (noms propres) | identiques (`Baratie`, `Whole Cake Island`…) | — rien à faire |

### Fichiers parsers à refactorer (chantier B)

- `ah/AhPriceParser`, `ah/AhPriceBand`, `ah/AhTooltip` (constantes SELL/AVG)
- `pet/ActivePetsScanner` (SECTION_START, ACTIVE/INACTIVE_ACTION, regex NIVEAU)
- `pet/PetStatTooltip`, `pet/PetEffectParser`, `pet/PetStat` (noms de stats, « Non découvert »)
- `quest/ParcheminScanner` (parchemin/scroll, EXPIRE, Objectif, lunaire) — garder les glyphes de rareté (indépendants de la langue)
- `quest/PassQuestParser`, `quest/PassQuestScanner`, `quest/Difficulty` (nom de quête, Progress/Status/Not compl, alias EN de difficulté)
- `boss/BossTracker` (Coordonnées, Respawn, catégories, nav, mots-clés mobs)
- `haki/HakiTimer` (ACTIVATED/READY)

### Chantier A — UI (langue du client MC)

Externaliser les chaînes d'**affichage** vers `assets/.../lang/en_us.json` + `fr_fr.json`
(déjà présents) avec `Text.translatable` :
- titres HUD : `Métier`, `Quêtes du jour`, `Pets actifs`, `Boss Timers`, `Ascensions`,
  `Parchemins`, `Haki`, `Prêt !`, `Récolte pour voir`, `Ouvre /pass, onglet Quêtes`…
- `help/HelpScreen` (toutes les entrées), `hud/HudEditScreen` (onglets/boutons/toggles
  dont « Couleur prix AH »), notifier de mise à jour.
- Les lignes qu'on **ajoute** aux infobulles AH (`Prix/u`, `… vs moyenne`) : clés lang.

L'UI suit la **langue du client MC** (pas le `/lang` serveur) — comportement voulu et
documenté : si un jour client EN + Game language FR, l'UI mod est EN et les parsers
marchent quand même (ils matchent les deux).

## Tests

- `i18n/ServerTextTest` : chaque `matchesXxx` / `Pattern` reconnaît la variante FR **et**
  EN, et rejette le bruit.
- Ajouter des cas **EN** aux tests existants : `AhPriceBandTest`, `PassQuestParserTest`,
  `PassQuestStateTest`, `PetEffectParser`/stat tests, `AscensionParserTest` (inchangé),
  + nouveau test parchemin EN si pertinent.
- Logique pure uniquement ; le rendu se vérifie en jeu.

## Plan d'implémentation (phasé)

1. **Phase 1 — B** : créer `ServerText` ; refactorer chaque parser + tests EN. C'est la
   correction du bug. Buildable et testable indépendamment.
2. **Phase 2 — A** : fichiers de langue + `Text.translatable` pour l'UI.
3. **Nettoyage** : retirer le dumper de debug (`debug/DebugDumper` + hook touche P dans
   `MinepieceEssentialsClient`) **avant release**.

## Flous à confirmer en jeu (non bloquants)

1. **Tournure exacte de complétion de quête pass** en EN (le dump n'avait que celle du
   parchemin : `You have completed a scroll …`). On part sur `You have completed the
   quest: <objectif>` ; le garde `contains("compl")` couvre déjà FR+EN, et le regex
   accepte `(?:quête|quest)\s*:`.
2. **4 noms de stats pets** non vus dans le dump (`Dexterity`, `Speed`, `Critical
   Damage`, `Energy Regen`) → traductions standard, matching tolérant ; à vérifier sur
   un pet réel.
