# Meowcha Café — guide pour Claude Code

Jeu Android Kotlin/Compose (voir `README.md` pour la description produit). Ce fichier couvre
ce qu'un agent doit savoir pour travailler efficacement dans ce repo.

## Build & vérification

Pas de JDK/Android Studio garanti dans l'environnement de l'agent. Avant de considérer un
changement Kotlin comme sûr :
- relire le diff attentivement (parenthèses/accolades équilibrées, imports ajoutés) ;
- si un JDK 17 est disponible, lancer `./gradlew compileDebugKotlin` ou `assembleDebug` ;
- sinon, le dire explicitement à l'utilisateur plutôt que de prétendre avoir vérifié.

## Workflow de version (important, ne pas improviser)

Une nouvelle version se développe sur une **branche nommée exactement comme la version**
(ex. `1.3.0`), créée depuis la branche par défaut ou la version précédente :

1. `VERSION` (racine) → nouveau numéro SemVer.
2. `CHANGELOG.md` → nouvelle section `## X.Y.Z (en cours)` en tête, refermer l'ancienne
   section en cours en lui retirant `(en cours)`.
3. Commit, `git push -u origin X.Y.Z`.
4. La CI (`.github/workflows/build.yml`) construit l'APK :
   - sur une branche autre que la branche par défaut → **pré-release bêta** `vX.Y.Z-beta`,
     remplacée à chaque push (jamais marquée "latest").
   - sur la branche par défaut avec un `VERSION` inédit → **vraie release** `vX.Y.Z`,
     marquée "latest".
5. Pour sortir une version en stable une fois testée en bêta : fast-forward la branche par
   défaut sur la branche de version (`git checkout <défaut> && git merge --ff-only X.Y.Z`),
   puis push. Utiliser `--ff-only` : si ça refuse, la branche par défaut a divergé, s'arrêter
   et regarder pourquoi plutôt que de forcer un merge.
- Utiliser le skill **`release`** pour dérouler ce flux et surveiller le build CI.

## Mise à jour automatique de l'app (`AppUpdater.kt`)

L'app vérifie **la toute dernière release de la liste `GET /releases`** (pas l'endpoint
`/releases/latest`), donc une pré-release bêta plus récente qu'une release stable sera bien
proposée comme mise à jour. Pratique pour tester le flux sur une branche de version avant de
la fusionner en stable — pas besoin d'attendre une vraie release pour valider que le
téléchargement/installation marche.

## Packs de contenu téléchargeables (DLC)

Séparés de l'APK : `packs/<id>/manifest.json` + `tools/make_packs.py`, publiés dans la release
GitHub `content` par `.github/workflows/content.yml`. Ne nécessite pas de nouvelle version de
l'app pour ajouter du contenu. Voir le skill **`content-pack`**.

## Repères dans le code

- `game/GameViewModel.kt` — boucle de jeu (file d'attente, patience, notation, combo, VIP,
  Rush du matin, objectifs). `DayState` est la source de vérité de la journée en cours.
- `game/Content.kt` — données statiques (ingrédients, recettes, chats, mugs, décors) ; le
  contenu DLC vient se greffer dessus via `Cats.setExtra()` / `Recipes.setExtra()`.
- `data/ContentPacks.kt` — téléchargement/installation des packs DLC.
- `data/AppUpdater.kt` — vérification/téléchargement/installation des mises à jour de l'app.
- `data/Accounts.kt`, `data/Database.kt` — comptes locaux (une base Room par utilisateur).
- `ui/Screens.kt` — tous les écrans et la navigation (`Screen` enum).
- `ui/Cafe3D.kt` — salle du café en perspective, parallaxe au gyroscope.
- `ui/Drawings.kt` — chats et mugs dessinés en Canvas (pas d'images).
- `ui/Components.kt` — composants réutilisables (`Title`, `CuteButton`, `SoftCard`, etc.) ;
  toute nouvelle UI doit s'appuyer dessus plutôt que redéfinir un style ad hoc.

## Conventions

- Tout le texte utilisateur est en français.
- Pas de dépendance réseau/JSON tierce : `org.json` (inclus dans Android) suffit partout où
  c'est utilisé (`ContentPacks.kt`, `AppUpdater.kt`).
- Les nouveaux champs de data class avec valeur par défaut se rajoutent **en fin de liste de
  paramètres** pour ne pas casser les appels positionnels existants.
