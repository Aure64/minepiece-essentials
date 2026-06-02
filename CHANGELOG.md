# Changelog

Les notes des versions antérieures sont disponibles sur les
[Releases GitHub](https://github.com/Aure64/minepiece-essentials/releases) et sur Modrinth.

## 1.5.0

### ✨ Nouvelles fonctionnalités
- **Prix à l'unité dans l'Hôtel des Ventes** — sur un lot de plusieurs items, le tooltip affiche le **prix de vente à l'unité** et le **prix moyen à l'unité** (ex. lot de 8 à 400K → « Prix/u: 50K 🪙 »), avec l'icône berry du serveur.
- **Support de Minecraft 1.21.8** — le mod est maintenant disponible aussi pour les clients 1.21.8 (en plus de 1.21.11).

### 🔧 Améliorations
- **HUD Quêtes du jour** : les quêtes complétées disparaissent au fur et à mesure ; quand tout est terminé, une simple ligne « Quêtes du jour terminées » ; remise à zéro automatique à minuit (réouvre /pass pour les quêtes du jour suivant).

### Prérequis
Minecraft 1.21.11 ou 1.21.8 · Fabric Loader ≥ 0.16.0 · Fabric API · Java 21

## 1.4.0

Grosse mise à jour : qualité des pets recalibrée, fonds de HUD personnalisables,
HUD des quêtes du jour, et un correctif important pour l'île personnelle.

### ✨ Nouvelles fonctionnalités
- **Fonds de HUD personnalisables** — nouvel onglet **« Personnaliser »** dans l'éditeur (touche **K**) : choisis le fond de chaque panneau parmi **Parchemin**, **Sombre translucide** ou **Transparent**. La couleur du texte s'adapte automatiquement pour rester lisible.
- **Quêtes du jour** — nouveau HUD listant tes quêtes de pass journalières (objectif + progression). Ouvre `/pass`, onglet **Quêtes**, pour le remplir. La **complétion est détectée en direct** (message de chat) : la quête passe verte sans rouvrir l'écran.
- **Logo Haki des Rois** sur le HUD haki (icône fournie par le pack du serveur).

### 🔧 Améliorations
- **Qualité des pets recalibrée** — le pourcentage de roll est désormais la position de la valeur **entre le minimum et le maximum** de la rareté (`(valeur − min) / (max − min)`), comme le calcul utilisé par les joueurs avancés. Couleurs : **rouge < 50 %**, **jaune 50–80 %**, **vert ≥ 80 %**. Inclut la correction de l'échelle des **Dégâts Critiques** (rebalance serveur).
- **HUD Haki** — affiché en permanence (compte à rebours ou « Prêt ! »), barre de progression retirée pour un rendu épuré.
- **Guide d'aide (touche H)** — entrées ajoutées pour les quêtes du jour et les onglets de l'éditeur ; précise qu'il n'y a pas de rafraîchissement automatique des quêtes.

### 🩹 Corrections
- **Mod désactivé sur l'île personnelle (`/is`)** — pour les joueurs connectés via un hôte ne contenant pas « minepiece » (IP, proxy, certains launchers), le mod se coupait entièrement sur le `/is` (plus aucun HUD ni touche K), faute de boss bar d'île. La détection reste maintenant **verrouillée sur « actif »** pour toute la session une fois MinePiece confirmé.

### Prérequis
Minecraft 1.21.11 · Fabric Loader ≥ 0.16.0 · Fabric API · Java 21
