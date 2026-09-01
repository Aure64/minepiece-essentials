# Télémétrie anonyme — design

**Date :** 2026-09-01
**Statut :** validé, prêt pour le plan d'implémentation

## Contexte

Aujourd'hui la seule mesure d'audience du mod est le compteur de téléchargements
Modrinth (221 downloads sur 30 jours, 155 pour la 1.21.11 et 98 pour la 1.21.8).
Ce chiffre ne dit rien de ce qui intéresse vraiment l'auteur :

- combien de joueurs utilisent réellement le mod chaque jour / chaque mois ;
- combien reviennent après une semaine (rétention) ;
- à quelle vitesse une nouvelle version est adoptée, et combien de joueurs restent
  bloqués sur une vieille version ;
- si le port 1.21.8 mérite d'être maintenu ;
- **quelles features servent** — le mod en compte une douzaine et aucune n'est mesurée.

## Objectif

Répondre à ces questions avec une télémétrie **anonyme**, **désactivable** et
**sans infrastructure à héberger**.

## Non-objectifs

- Identifier un joueur, même indirectement. Aucun pseudo, aucun UUID Mojang.
- Mesurer quoi que ce soit du gameplay (progression, richesse, inventaire).
- Héberger un backend. La micromachine de l'auteur est explicitement écartée :
  IP publique, domaine, HTTPS et maintenance pour un mod de loisir, le rapport
  bénéfice/coût est mauvais.
- Bloquer ou ralentir le jeu, dans quelque cas que ce soit.

## Approche retenue : PostHog Cloud EU, en HTTP brut

PostHog expose un endpoint public d'ingestion :

```
POST https://eu.i.posthog.com/i/v0/e/      (événement unique)
POST https://eu.i.posthog.com/batch/       (lot d'événements)
Content-Type: application/json
{"api_key": "phc_…", "event": "…", "distinct_id": "…", "properties": {…}, "timestamp": "…"}
```

Conséquences :

- **aucune dépendance ajoutée** au jar — on réutilise le `java.net.http.HttpClient`
  du JDK, déjà employé par `update/UpdateChecker` ;
- la clé de projet `phc_…` est *write-only* et prévue pour être embarquée dans un
  client ; sa présence dans le jar public n'est pas une fuite de secret ;
- région **EU**, donc pas de transfert hors UE à documenter.

Projet cible : le projet PostHog existant de l'organisation « Hirofolio » sur le
cloud EU. Si l'auteur crée un projet dédié au mod pour ne pas mélanger les
données, seule la constante contenant la clé `phc_…` change.

Alternative écartée : Cloudflare Worker + D1. Gratuit et auto-hébergé lui aussi,
mais sans dashboard — il aurait fallu construire les courbes de rétention à la
main en SQL, pour trois à quatre fois le travail.

## Architecture

Un seul package nouveau, `telemetry/`, avec trois unités à responsabilité unique :

### `TelemetryId`

Gère l'identité anonyme. Au premier lancement, génère un `UUID.randomUUID()` et le
persiste dans `config/minepiece-essentials/telemetry.json` :

```json
{ "installId": "b3f1…", "firstSeen": "2026-09-01", "announced": false }
```

`announced` sert au message de chat unique du premier lancement. L'identifiant est
lié à **une installation**, pas à une personne : réinstaller le mod ou supprimer le
fichier donne une nouvelle identité, et c'est voulu.

### `TelemetryEvent`

Objet de données pur (nom, propriétés, timestamp) et sa sérialisation JSON.
Aucune dépendance Minecraft → **testable unitairement** dans `src/test`.

### `Telemetry`

Façade et cycle de vie. API publique volontairement minuscule :

```java
Telemetry.init();                  // au démarrage du client
Telemetry.onJoinedServer();        // quand le joueur rejoint un serveur
Telemetry.feature("boss_refresh"); // depuis les points d'usage
Telemetry.shutdown();              // à la fermeture du client
```

Tout appel est **sans effet** si l'opt-out est actif. Les points d'appel dans le
reste du mod sont donc de simples one-liners sans condition.

## Données envoyées

### Événement `session_start`

Émis **une fois par session de jeu**, non pas au lancement mais ~5 s après que le
joueur a rejoint un serveur — c'est le seul moment où `ServerDetector` a une
réponse fiable sur `on_minepiece`.

| Propriété | Exemple | Pourquoi |
|---|---|---|
| `mod_version` | `1.7.2` | adoption des releases, joueurs sur vieille version |
| `mc_version` | `1.21.11` | arbitrer le maintien du port 1.21.8 |
| `loader_version` | `0.16.10` | diagnostiquer les rapports de bug |
| `java_version` | `21.0.4` | idem |
| `os` | `windows` | idem |
| `client_language` | `fr_fr` | prioriser les traductions |
| `on_minepiece` | `true` | distinguer les vrais joueurs des curieux |

Les mêmes valeurs sont aussi envoyées en `$set` (propriétés de personne), pour que
PostHog puisse répondre à « combien de joueurs sont **actuellement** en 1.6.x ».

### Événement `feature_used`

Une propriété : `feature`. **Une seule émission par feature et par session** — le
but est de mesurer l'usage, pas le volume, et ça borne le trafic à quelques
dizaines d'événements par joueur et par jour.

Features instrumentées : `boss_refresh`, `boss_waypoint`, `hud_edit`,
`rarity_filter`, `rarity_sort`, `pet_tooltip`, `minion_calc`, `parchment_hud`,
`job_hud`, `haki_hud`, `ah_price`, `help_screen`.

### Ce qui n'est jamais envoyé

Pseudo ou UUID Minecraft, adresse IP (PostHog est configuré pour ne pas la
conserver), contenu du chat, coordonnées, inventaire, adresse du serveur autre que
le booléen `on_minepiece`. La liste des clés autorisées est **codée en dur** et
vérifiée par un test — une propriété hors liste fait échouer le build.

## Robustesse

Contrainte absolue : la télémétrie ne doit jamais dégrader le jeu.

- Envoi sur un **thread daemon** dédié, jamais sur le thread client.
- File d'attente bornée (100 événements) ; au-delà, on jette les plus anciens.
  Un joueur hors ligne pendant des heures ne doit pas accumuler de la mémoire.
- Vidage de la file toutes les 60 s via `/batch/`, plus un dernier envoi
  best-effort à la fermeture.
- Timeouts de 8 s, **échec silencieux** : une exception réseau part en
  `LOGGER.debug` et rien d'autre. PostHog indisponible = le mod ne le remarque pas.
- Aucun retry agressif : un lot perdu est perdu. Les statistiques d'audience
  tolèrent parfaitement quelques pertes.

## Opt-out et transparence

- `ModConfig.telemetryEnabled = true` (activé par défaut).
- Un **7ᵉ toggle** dans l'éditeur **K**, sous les toggles Raretés. Attention :
  la boucle de clic de `HudEditScreen` est bornée en dur à `i < 6` et son `switch`
  se termine par un `default ->` — les deux doivent être étendus, sinon le nouveau
  toggle s'affiche mais bascule le mauvais réglage.
- **Message de chat au premier lancement uniquement** (`announced`), disant ce qui
  est collecté et comment couper. Pas de spam aux lancements suivants.
- Onglet **Disclosures** de Modrinth à remplir, et une ligne dans la description
  du projet.

Cadre : la règle Modrinth **1.11** interdit d'envoyer des données à un serveur
distant *« without clear disclosure »*. Elle impose donc la **divulgation**, pas
l'opt-out. Le toggle est néanmoins conservé : il coûte une quinzaine de lignes,
il couvre le droit d'opposition RGPD (identifiant persistant, joueurs européens),
et il permet de répondre en une phrase à la première accusation d'espionnage.

## Fichiers touchés

| Fichier | Nature |
|---|---|
| `telemetry/TelemetryId.java` | nouveau |
| `telemetry/TelemetryEvent.java` | nouveau |
| `telemetry/Telemetry.java` | nouveau |
| `config/ModConfig.java` | + `telemetryEnabled` |
| `hud/HudEditScreen.java` | + toggle (étendre la boucle et le `switch`) |
| `MinepieceEssentialsClient.java` | init, join, shutdown |
| ~10 fichiers de features | un one-liner `Telemetry.feature(…)` chacun |
| `lang/fr_fr.json`, `lang/en_us.json` | libellés du toggle + message de chat |

Puis copie vers `Public_QoL_Minepiece-1.21.8/`. Aucune des API utilisées ne
diverge entre les deux versions, **sauf** `HudEditScreen` (signature de
`mouseClicked`) qui est déjà version-spécifique : y reporter le toggle à la main.

## Tests

Unitaires (`src/test`, sans Minecraft) :

- sérialisation JSON d'un `TelemetryEvent` conforme au format PostHog ;
- `feature()` appelée dix fois ne produit **qu'un** événement ;
- toute propriété hors de la liste blanche est rejetée ;
- opt-out désactivé ⇒ la file reste vide.

En jeu : lancer le client, vérifier que `session_start` arrive dans PostHog avec
les bonnes propriétés, puis basculer le toggle et vérifier qu'il n'arrive plus
rien. Le dépouillement peut se faire directement depuis Claude Code (accès PostHog
branché sur cette session).

## Hors périmètre

Le **bouton de don berry** dans l'écran K (clic → saisie d'un montant →
confirmation → `/pay Aure64 <montant>`) est une feature indépendante, validée
séparément et implémentée sans spec dédiée.
