> ⚠️ **Version réservée à Minecraft 26.2**, celle vers laquelle MinePiece migre. Si tu joues encore en 1.21.11, reste en **1.8.0**.
> Prérequis : **Java 25**, Fabric Loader ≥ 0.19.5, Fabric API.

## ✨ Nouveautés

**🚀 Tout se remplit tout seul à la connexion**

Plus besoin d'ouvrir `/boss`, `/pets` ou `/pass quests` à la main : quelques secondes après ton arrivée sur une île, le mod relève silencieusement les **timers de boss**, tes **pets actifs** et tes **quêtes du jour**. Rien ne s'affiche, rien ne clignote, tu continues à jouer. Le mod ne fait que lire l'écran envoyé par le serveur et le refermer : **aucun clic, aucune action** n'est envoyé.

**⏱️ Boss Timers refondu sur le nouveau /boss**

Le serveur regroupe désormais tous les boss dans un seul écran : le mod le lit d'un coup, pour toutes les îles. HUD simplifié : plus de bouton par île, un seul bouton **⟳** dans l'en-tête et une ligne « Mis à jour il y a … ». Les îles restent repliables au clic, un clic sur un boss copie ses coordonnées.

**📜 Quêtes du jour et pets actifs à jour sans rien ouvrir**

Les quêtes du pass sont relevées à la connexion et après le reset de minuit, le panneau des pets actifs à la connexion. Ouvrir les écrans à la main continue de fonctionner.

## 🩹 Corrections

**🖱️ Plus d'interfaces qui se ferment toutes seules**

Pendant un relevé en arrière-plan, le mod pouvait refermer **n'importe quelle** interface ouverte (boussole, coffre, hôtel des ventes) et avaler tes clics pendant plusieurs secondes. Corrigé : l'écran du serveur n'est même plus construit côté client, et tes propres interfaces ne sont jamais touchées. Le mod n'envoie plus rien tant qu'un coffre ou un inventaire est ouvert.

**🛡️ Un fichier de configuration vide ou corrompu ne fait plus planter le jeu**

Il est mis de côté (`.bak`) et remplacé par les valeurs par défaut. Les sauvegardes sont écrites de façon sûre.

## ⚡ Performances

Les scanners (parchemins, pets, quêtes, ascension) n'analysent plus ton inventaire ou l'écran ouvert à chaque tick, seulement quand quelque chose change. Le survol d'un pet ne recopie plus son NBT à chaque image. Un HUD masqué ou désactivé ne consomme plus rien.

## 🔧 Technique

Passage aux mappings Mojang (Yarn n'existe plus après 1.21.11), Java 25, Fabric Loom 1.17.
