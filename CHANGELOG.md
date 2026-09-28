# Changelog

Les notes des versions antérieures sont disponibles sur les
[Releases GitHub](https://github.com/Aure64/minepiece-essentials/releases) et sur Modrinth.

## 1.9.0 — Minecraft 26.2

> ⚠️ Cette version est **réservée à Minecraft 26.2** (la version vers laquelle MinePiece migre).
> Si tu joues encore en 1.21.11, reste en **1.8.0**. Il faut **Java 25**, Fabric Loader ≥ 0.19.5 et Fabric API.

### ✨ Nouveautés

**🚀 Tout se remplit tout seul à la connexion**
Plus besoin d'ouvrir `/boss`, `/pets` ou `/pass quests` à la main : quelques secondes après ton arrivée sur une île, le mod relève silencieusement les **timers de boss**, tes **pets actifs** et tes **quêtes du jour**. Rien ne s'affiche, rien ne clignote, tu peux continuer à jouer. Le mod ne fait que lire l'écran envoyé par le serveur et le refermer : **aucun clic, aucune action** n'est envoyé.

**⏱️ Boss Timers refondu sur le nouveau `/boss`**
Le serveur regroupe désormais tous les boss dans un seul écran `/boss` : le mod le lit d'un coup, pour toutes les îles. Le HUD est simplifié : plus de bouton par île, un seul bouton **⟳** dans l'en-tête et une ligne « Mis à jour il y a … ». Les îles restent repliables au clic, et un clic sur un boss copie ses coordonnées.

**📜 Quêtes du jour et pets actifs à jour sans rien ouvrir**
Les quêtes du pass sont relevées à la connexion et après le reset de minuit ; le panneau des pets actifs à la connexion. Ouvrir les écrans à la main continue de fonctionner.

### 🩹 Corrections

**🖱️ Plus d'interfaces qui se ferment toutes seules**
Pendant un relevé en arrière-plan, le mod pouvait refermer **n'importe quelle** interface ouverte (boussole, coffre, hôtel des ventes) et avaler tes clics pendant plusieurs secondes. C'est corrigé : l'écran du serveur n'est même plus construit côté client, et tes propres interfaces ne sont jamais touchées. Le mod n'envoie plus rien tant qu'un coffre ou un inventaire est ouvert.

**🛡️ Un fichier de configuration vide ou corrompu ne fait plus planter le jeu**
Il est mis de côté (`.bak`) et remplacé par les valeurs par défaut. Les sauvegardes sont désormais écrites de façon sûre.

### ⚡ Performances

Les scanners (parchemins, pets, quêtes, ascension) n'analysent plus ton inventaire ou l'écran ouvert à chaque tick, seulement quand quelque chose change. Le survol d'un pet ne recopie plus son NBT à chaque image. Un HUD masqué ou désactivé ne consomme plus rien. Fini les centaines de lignes de log du scan des parchemins.

### 🔧 Technique

Passage aux **mappings Mojang** (Yarn n'existe plus après 1.21.11), Java 25, Fabric Loom 1.17. Voir `UPGRADE.md`.

## 1.8.0

### ✨ Nouveautés

**🐘 L'île de Zou est suivie**
La nouvelle île **Zou** (`/zou`) apparaît dans le HUD **Boss Timers**, entre Dressrosa et Whole Cake, avec les **coordonnées** et le **timer de respawn** de ses mini-boss : **Jack** et les pirates Minks & Jack. Ouvre le HUD des boss et clique sur la flèche de rafraîchissement de Zou pour récupérer ses timers.

### 🩹 Corrections

**🏝️ Le mod ne s'éteint plus sur ton île perso (`/is`)**
Certains joueurs, surtout sous **Lunar Client**, voyaient le mod fonctionner sur les îles du serveur mais devenir totalement inactif sur leur `/is` : plus de HUD, plus de timers, plus rien. La raison : sur l'île perso la boss bar d'île disparaît, et c'était le dernier signal dont disposaient ces joueurs pour reconnaître le serveur.

Le mod s'appuie désormais aussi sur le **pied de page de la tab-list**, présent partout, `/is` compris. Si le problème persiste chez toi, ton `logs/latest.log` contient maintenant une ligne `[ServerDetector] diagnostic` qui indique quel signal a échoué. Envoie-la moi, elle suffit à diagnostiquer.

**⚡ Moins de lag quand un coffre est ouvert**
Le liseré de prix de l'hôtel des ventes réanalysait le contenu de chaque case à chaque image affichée. C'est désormais mis en cache. Le gain se voit surtout dans les grands coffres et à l'hôtel des ventes, sur les machines modestes.

### 📊 Statistiques anonymes (désactivables)

Le mod envoie désormais quelques statistiques **anonymes**, pour que je sache combien de joueurs l'utilisent réellement, sur quelles versions, et **quelles fonctions servent vraiment**. C'est ce qui me permet de décider quoi améliorer et quoi arrêter de maintenir.

Ce qui est envoyé : un identifiant **aléatoire** tiré au premier lancement, la version du mod, la version de Minecraft, la version de Fabric Loader, la version de Java, l'OS, la langue du client, et le nom des fonctions utilisées.

Ce qui n'est **jamais** envoyé : ton pseudo, ton UUID Minecraft, ton adresse IP (le mod demande explicitement à ne pas l'enregistrer ni la géolocaliser), le chat, tes coordonnées, ton inventaire, ta progression. Aucune de ces données ne peut techniquement partir : la liste de ce qui est autorisé est figée dans le code et vérifiée par les tests.

**Pour désactiver** : touche **K**, interrupteur « Statistiques anonymes ». Un message te l'explique au premier lancement.
Hébergement : PostHog, région **Europe**.

### 💛 Bouton de don (facultatif)

Un petit bouton dans l'éditeur de HUD (**K**) et dans l'aide (**H**) permet d'envoyer des berries à l'auteur du mod si tu as envie de soutenir le projet. Trois montants proposés (50K, 100K, 250K) ou le montant de ton choix, et **une confirmation affiche la commande exacte avant tout envoi**. Visible uniquement sur MinePiece, et totalement facultatif.

## 1.7.1

### 🩹 Correction

**⏱️ Timers des mini-boss en anglais**
Avec la Game language du serveur en anglais (`/lang`), les boss timers ne fonctionnaient que pour le boss principal de l'île (ex. Luffy) : les mini-boss (Nightmare Luffy, Perona, Oz…) affichent `Spawn:` là où le boss unique affiche `Respawn:`, et le mod ne reconnaissait que la seconde forme. Tous les timers fonctionnent désormais en anglais comme en français.

Merci au joueur Ozan qui a signalé (et diagnostiqué !) le bug. 🙏

## 1.7.0

### ✨ Nouveautés

**🌍 Le mod fonctionne maintenant en anglais comme en français**
Si tu joues avec la **Game language** du serveur en anglais (`/lang`), tout le mod fonctionne désormais : boss timers,
% des pets, progression des quêtes de pass, parchemins, prix de l'hôtel des ventes, timer haki… Avant, ces fonctions
ne marchaient qu'en français et restaient vides en anglais. Les parsers reconnaissent maintenant **les deux langues
automatiquement** (aucun réglage à faire).
L'**interface du mod** (titres des HUD, aide, boutons) suit la **langue de ton client Minecraft**.

**🎨 Couleurs de prix à l'hôtel des ventes**
Dans le `/ah`, chaque annonce est entourée d'un **liseré coloré** selon son prix face à la moyenne du marché :
- 🟢 **vert** = au prix (±10 % de la moyenne)
- 🟡 **jaune** = en dessous (bonne affaire)
- 🔴 **rouge** = au-dessus (trop cher)

Et au survol d'un objet, une ligne indique l'**écart exact en %** vs la moyenne. Activable/désactivable dans l'éditeur
de HUD (touche **K**).

---

**Compatibilité :** Minecraft **1.21.11** et **1.21.8** · Fabric Loader ≥ 0.16.0 · Fabric API · Java 21

## 1.6.1

Patch correctif de la 1.6.0 (qui introduisait les raretés dans l'inventaire). Cette version regroupe les deux.

### 🐛 Corrections (1.6.1)

**📜 Parchemins — objectif affiché en entier**
Le texte des objectifs n'est plus tronqué (« Récoltez 76 pommes de terre … ») : il s'affiche complet, en se réduisant
légèrement si besoin. Compteur aligné à droite, barre de progression pleine largeur.

**⚔️ Ascensions — armes sans dégâts bien classées**
Les armes à distance comme le **Kabuto Noir d'Usopp** apparaissaient par erreur dans la section *Fruits*. Le
classement fruit/arme ne dépend plus de la statistique de dégâts : ces objets sont maintenant rangés dans **Armes**.

### ✨ Pour rappel — nouveautés 1.6.0 : raretés dans l'inventaire 💎

- **Emblèmes de rareté** affichés dans le coin de chaque objet : coffres/conteneurs, inventaire (**E**) et hotbar.
Interrupteur On/Off par emplacement dans l'éditeur de HUD (**K**).
- **Barre de filtre par rareté** à droite des conteneurs : estompe les objets non correspondants. Bouton **✕** pour
réinitialiser.
- **Tri des coffres** (coffres & shulkers) : *Trier par rareté* (regroupe par palier, sens inversable) ou *Regrouper
par objet* (A→Z, toutes les raretés d'un objet à la suite).

> Les emblèmes proviennent du pack de ressources du serveur : ils ne s'affichent que sur **MinePiece**.

---

**Compatibilité :** Minecraft **1.21.11** et **1.21.8** · Fabric Loader ≥ 0.16.0 · Fabric API · Java 21

## 1.6.0

### ✨ Nouvelles fonctionnalités
- **Emblèmes de rareté dans l'inventaire** — les petits logos de rareté (commun → mythique, et les raretés spéciales) s'affichent directement dans le coin de chaque case d'objet : dans les **coffres / conteneurs**, dans ton **inventaire** (touche E) et sur la **hotbar** en jeu. Chaque contexte a son propre **interrupteur On/Off** dans l'éditeur (touche **K**). *(Emblèmes fournis par le pack du serveur : visibles uniquement sur MinePiece.)*
- **Barre de filtre par rareté** — une barre verticale à droite des conteneurs : clique une ou plusieurs raretés pour **estomper** les objets qui ne correspondent pas, bouton **✕** pour tout réinitialiser. Pur affichage, aucune action sur les objets.
- **Tri des coffres** — deux boutons (présents uniquement sur les **coffres** et **shulkers**, jamais sur l'HV ou les shops) :
- **Trier par rareté** (↓/↑) — regroupe les objets par palier de rareté, du plus rare au plus commun (rappuie pour inverser).
- **Regrouper par objet** (A↓/A↑) — range les objets par ordre **alphabétique (toujours A→Z)** en plaçant **toutes les raretés d'un même objet à la suite** (ton boulon épique collé à ton boulon légendaire) ; le bouton bascule juste le sens des raretés à l'intérieur de chaque groupe.
- **Infobulles** sur chaque bouton de la barre pour expliquer son effet.

### Prérequis
Minecraft 1.21.11 ou 1.21.8 · Fabric Loader ≥ 0.16.0 · Fabric API · Java 21

## 1.5.1

### ✨ Nouveautés
- **Île Komugi** ajoutée au suivi des boss, avec son mini-boss **Charlotte Perospero** (coords + timer de respawn).

### 🩹 Corrections
- **Détection des mini-boss améliorée** : sur les îles dont l'item de catégorie ne porte pas un nom standard (ex. Komugi « Soldats biscuits »), le mod trouve désormais la liste des mini-boss (avec coords/timers) au lieu de tomber sur le mauvais menu. Corrige aussi des mini-boss manquants sur Whole Cake (Capone Bege).

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
