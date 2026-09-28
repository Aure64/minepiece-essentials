# Relevé de fond (`network/`) — comment le mod lit les écrans serveur sans les afficher

Depuis la 1.9.0, trois HUDs se remplissent seuls à la connexion : **boss** (`/boss`),
**pets actifs** (`/pets`) et **quêtes du jour** (`/pass quests`). Le mécanisme est
commun et volontairement minimal : **une commande, une lecture, une fermeture. Jamais de clic.**

## Cycle d'un relevé

1. Un appelant (`BossTracker`, `ActivePetsScanner`, `PassQuestScanner`) demande un
   relevé en levant un drapeau `refreshPending`. Il ne l'abaisse **que** si
   `BackgroundGuiRefresh.sendCommand(...)` renvoie `true`.
2. `sendCommand` refuse (renvoie `false`, la demande reste en attente) si :
   - un relevé est déjà en cours (`busy`) ;
   - le cooldown global (`COOLDOWN_MS` = 1,5 s) n'est pas écoulé ;
   - **un écran conteneur est ouvert** (coffre, inventaire, GUI serveur) : le serveur
     remplacerait le menu du joueur. Les écrans du mod (éditeur K, aide H) ne bloquent pas.
3. La commande est envoyée par `connection.sendCommand(...)`, `ServerGuiInterceptor.start()`.
4. Le serveur répond par `ClientboundOpenScreenPacket`. Dans
   `ClientPlayNetworkHandlerMixin.onOpenScreen`, `ServerGuiInterceptor.claimScreen(id)` prend
   l'id **et l'ouverture est annulée (`ci.cancel()`)** : aucun écran n'est construit côté client,
   donc rien ne s'affiche, rien ne clignote, l'écran courant du joueur n'est pas touché.
5. Les paquets `ClientboundContainerSetContentPacket` / `SetSlotPacket` pour cet id sont
   collectés par les mêmes mixins (vanilla les ignore, l'id ne correspondant à aucun menu ouvert).
6. `ServerGuiInterceptor.tick()` livre les items **100 ms après le paquet de contenu complet**
   (`SETTLE_MS`), ou après 600 ms sans paquet complet, puis passe `finished`.
7. `BackgroundGuiRefresh.tick()` voit `finished`, envoie **un seul**
   `ServerboundContainerClosePacket(id)` pour prévenir le serveur, puis `stop()`.

Délais de sécurité : 3 s sans écran ouvert (le serveur a répondu en chat : cooldown,
lobby, permission) → abandon ; 5 s sans contenu → abandon ; 8 s tout compris → reset forcé.

## Règles à ne pas casser

- **`BackgroundGuiRefresh.tick()` est appelé à chaque tick client, hors de toute condition
  MinePiece** (dans `MinepieceEssentialsClient`). Un cycle doit toujours pouvoir se terminer,
  même si la détection serveur lâche en plein milieu.
- **`BackgroundGuiRefresh.reset()` sur JOIN et DISCONNECT** : oubli sans paquet. Sinon un
  relevé coupé par un changement de serveur enverrait un paquet de fermeture périmé sur la
  connexion suivante.
- **Les mixins réseau ignorent la passe Netty.** Les `handle*` vanilla commencent par
  `ensureRunningOnSameThread` : une injection en `HEAD` s'exécute d'abord sur le thread réseau,
  puis sur le thread client. `minepiece$offThread()` filtre la première. L'état de
  l'intercepteur n'est pas synchronisé et ne doit être touché que depuis le thread client.
- **Un seul écran par cycle** : `claimScreen` ne prend que le premier `OpenScreen` reçu.
- **Jamais d'envoi automatique avant qu'une île soit détectée** (`IslandDetector != UNKNOWN`).
  Dans le lobby temporaire, l'adresse contient déjà « minepiece » mais le joueur n'est pas en jeu :
  un relevé y avalait les clics sur la boussole.
- **Aucune automatisation.** Ce mécanisme lit, il n'agit pas. Le flux « clic sur une catégorie
  puis lecture du second écran » a été supprimé en 1.9.0 et ne doit pas revenir.

## Déclencheurs actuels

| Relevé | Quand | Fichier |
|---|---|---|
| `/boss` | 1× à la détection d'île, puis bouton ⟳ du HUD | `boss/BossTracker` |
| `/pets` | 1× à la détection d'île | `pet/ActivePetsScanner` |
| `/pass quests` | 1× à la détection d'île, puis après le reset de minuit | `quest/PassQuestScanner` |

Les trois s'enchaînent en ~3 s (cooldown 1,5 s entre deux). Aucun polling périodique :
c'est un choix, pas un oubli.

## Historique du bug « interfaces qui se ferment / clics avalés » (corrigé en 1.9.0)

Avant 1.9.0, un mixin sur `Minecraft.tick` fermait **n'importe quel** écran conteneur tant qu'un
relevé était en cours ; sans second écran l'intercepteur ne se terminait jamais et attendait le
timeout de 8 s ; rien n'était réinitialisé à la déconnexion ; deux paquets de fermeture partaient
pour le même conteneur ; et les mixins réseau écrivaient depuis le thread Netty. C'est très
probablement le « bug d'interface » remonté par l'administration du serveur. Tout cela est
documenté dans les commits `dda029b`, `3d1248b`, `e594069` et `e51a48c`.
