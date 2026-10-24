## 🩹 Corrections

**🖥️ Le menu /boss ne s'ouvre plus à l'écran après un changement de serveur**

Juste après un changement de serveur, le serveur répond lentement et le mod n'attendait son écran que 3 secondes : passé ce délai, l'écran arrivait quand même et s'affichait. Le mod attend maintenant 10 s, laisse 5 s de grâce après chaque changement de serveur avant son premier relevé, et si un écran arrive encore plus tard sans que tu aies rien fait entre-temps, il est refermé sans être affiché. Pendant l'attente, la moindre action de ta part (clic sur un bloc, une entité, un objet, une commande) annule le relevé : ton coffre ne sera jamais capturé.

**🔘 Interrupteur « Relevés en arrière-plan »**

Dans l'éditeur K : coupe tous les relevés automatiques (/boss, /pets, /pass quests, /profil). Le bouton ⟳ du HUD boss reste utilisable.

**🧩 Éditeur K adapté aux petites fenêtres**

Les interrupteurs passent sur deux colonnes quand la hauteur manque, et les libellés ne débordent plus des boutons.
