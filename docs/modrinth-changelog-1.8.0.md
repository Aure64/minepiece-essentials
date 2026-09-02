# Minepiece Essentials 1.8.0 — Correctif île perso + statistiques anonymes

## 🩹 Correction importante

**🏝️ Le mod ne s'éteint plus sur ton île perso (`/is`)**
Certains joueurs — surtout sous **Lunar Client** — voyaient le mod fonctionner sur les îles du serveur mais devenir totalement inactif sur leur `/is` : plus de HUD, plus de timers, plus rien. En cause : sur l'île perso la boss bar d'île disparaît, et c'était le dernier signal dont disposaient ces joueurs pour reconnaître le serveur.
Le mod s'appuie désormais aussi sur le **pied de page de la tab-list**, présent partout, `/is` compris. Si le problème persiste chez toi, ton `logs/latest.log` contient maintenant une ligne `[ServerDetector] diagnostic` qui dit exactement quel signal a échoué — envoie-la, elle suffit à diagnostiquer.

## ✨ Nouveauté

**📊 Statistiques anonymes (désactivables)**
Le mod envoie désormais quelques statistiques **anonymes** pour que je sache combien de joueurs l'utilisent réellement, sur quelles versions, et **quelles fonctions servent vraiment** — c'est ce qui me permet de décider quoi améliorer et quoi arrêter de maintenir.

Ce qui est envoyé : un identifiant **aléatoire** tiré au premier lancement, la version du mod, la version de Minecraft, la version de Java, l'OS, la langue du client, et le nom des fonctions utilisées.

Ce qui n'est **jamais** envoyé : ton pseudo, ton UUID Minecraft, ton adresse IP (le mod demande explicitement à ne pas l'enregistrer), le chat, tes coordonnées, ton inventaire, ta progression. Aucune de ces données ne peut techniquement partir : la liste de ce qui est autorisé est figée dans le code et vérifiée par les tests.

**Pour désactiver** : touche **K**, interrupteur « Statistiques anonymes ». Un message te l'explique au premier lancement.
Hébergement : PostHog, région **Europe**.

**💛 Bouton de don (facultatif)**
Un petit bouton dans l'éditeur de HUD (**K**) et dans l'aide (**H**) permet d'envoyer des berries à l'auteur du mod si tu as envie de soutenir le projet. Trois montants proposés (50K / 100K / 250K) ou le montant de ton choix, et **une confirmation affiche la commande exacte avant tout envoi** — aucun risque d'envoi accidentel. Visible uniquement sur MinePiece, et évidemment totalement facultatif.

---

**Compatibilité :** Minecraft **1.21.11** et **1.21.8** · Fabric Loader ≥ 0.16.0 · Fabric API · Java 21
