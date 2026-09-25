---
name: release
description: Prépare et publie une nouvelle version de Meowcha Café (bump VERSION, changelog, branche dédiée, push, suivi du build CI). À utiliser quand l'utilisateur demande de "sortir une version", "passer en X.Y.Z", "publier", ou de fusionner une bêta en stable.
---

# Publier une version de Meowcha Café

Ce repo a un workflow de version précis (voir aussi `CLAUDE.md`). Ne pas improviser une
variante : suivre ces étapes dans l'ordre.

## 1. Démarrer une nouvelle version (bêta de test)

1. Vérifier l'état git (`git status`, `git branch --show-current`) — ne jamais bump une
   version sur un arbre de travail sale sans le signaler à l'utilisateur.
2. `git checkout -b X.Y.Z` depuis la branche courante (généralement la version précédente
   ou la branche par défaut).
3. Mettre à jour `VERSION` (juste le numéro, une ligne).
4. Dans `CHANGELOG.md` : ajouter une section `## X.Y.Z (en cours)` en tête, lister les
   changements en une ligne par item (public-facing, en français, pas de détail
   d'implémentation). Si la section juste en dessous porte encore `(en cours)`, lui retirer
   ce suffixe (elle est maintenant sortie).
5. Commit avec un message clair, `git push -u origin X.Y.Z`.
6. Attendre/surveiller le run CI `Build APK` sur cette branche (`gh run list --branch X.Y.Z
   --limit 3`). Un push sur une branche non-défaut publie une **pré-release bêta**
   `vX.Y.Z-beta` (jamais "latest", remplacée à chaque nouveau push sur la même branche).
7. Confirmer à l'utilisateur avec le lien de la release bêta (`gh release view vX.Y.Z-beta`).

## 2. Promouvoir une bêta testée en release stable

Seulement quand l'utilisateur le demande explicitement (jamais automatiquement — c'est une
action visible publiquement, à confirmer).

1. `git fetch origin`, comparer la branche par défaut et la branche de version
   (`git merge-base origin/<défaut> X.Y.Z`) : si elles ne partagent pas leur ancêtre commun
   attendu, s'arrêter et expliquer avant de continuer.
2. `git checkout <défaut> && git merge --ff-only X.Y.Z`. Ne **jamais** utiliser
   `--no-ff`/résoudre un conflit à l'aveugle ici : si `--ff-only` échoue, la branche par
   défaut a divergé pendant ce temps — s'arrêter et demander.
3. `git push origin <défaut>`.
4. Surveiller le build CI comme à l'étape 1 : cette fois ça publie la **vraie release**
   `vX.Y.Z` marquée "latest" (deux jobs se déclenchent : `Build APK`, et `Content packs` si
   des fichiers sous `packs/` ont aussi changé).
5. Confirmer avec le lien (`gh release view vX.Y.Z`).

## Points d'attention

- Ne jamais sauter la mise à jour de `CHANGELOG.md` : c'est ce que lisent les joueuses/joueurs
  et ça sert d'historique de decision.
- Le `versionCode` Android est dérivé automatiquement de `VERSION` (voir `app/build.gradle.kts`)
  — ne pas le toucher à la main.
- Pas de JDK garanti dans l'environnement de l'agent : si aucune vérification de compilation
  n'a pu être faite, le dire avant de pousser, pas après.
- `AppUpdater.kt` regarde la release la plus récente **quel que soit son statut bêta/stable**
  (voir `CLAUDE.md`) : pousser une bêta suffit déjà à tester le flux de mise à jour en jeu,
  pas besoin d'attendre la promotion en stable pour ça.
