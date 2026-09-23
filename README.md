# 🐱☕ Meowcha Café

Un jeu Android tout mignon et tout rose inspiré de *Good Coffee, Great Coffee* :
des chats viennent au café, tu prépares leur boisson ingrédient par ingrédient, et tu gagnes
des pièces pour acheter de jolis mugs.

## 📥 Télécharger
Dernier APK : **[Releases → meowcha-cafe.apk](../../releases/latest)**
(lien direct : `https://github.com/nicoooo972/meowcha/releases/latest/download/meowcha-cafe.apk`)

## 🎮 Comment jouer
- Un chat arrive et commande une boisson. Touche sa bulle pour voir la recette.
- Ajoute les ingrédients **dans le bon ordre** puis appuie sur **Servir**.
  - ⭐⭐⭐ recette exacte · ⭐⭐ bons ingrédients · ⭐ à moitié
- Sers vite pour de meilleurs pourboires ! Si la patience tombe à zéro, le chat s'en va 😿
- Chaque jour débloque de nouveaux ingrédients et recettes (matcha, sakura, guimauves…).
- **Boutique** : achète des mugs (pois, cœurs, pattes, oreilles de chat, sakura…) qui augmentent les pourboires.
- **Album** : un polaroïd de chaque chat rencontré ; gagne leurs cœurs pour découvrir leur boisson préférée.

## 🛠 Technique
- Kotlin + Jetpack Compose, tous les chats et mugs sont dessinés en code (Canvas), aucune image externe.
- Sauvegarde locale avec Room (SQLite) : pièces, jour, mugs, chats rencontrés, historique.
- Build via GitHub Actions (`.github/workflows/build.yml`) qui publie une Release avec l'APK à chaque push.

## 🔢 Versioning & mises à jour
- La version est dans le fichier [`VERSION`](VERSION) au format `MAJEUR.MINEUR.CORRECTIF` (SemVer).
  Le `versionCode` Android en est déduit (`MAJ*10000 + MIN*100 + CORR`), il augmente donc toujours.
- Pour publier une mise à jour : incrémente `VERSION`, ajoute une entrée dans `CHANGELOG.md`, push.
  La CI crée automatiquement le tag `vX.Y.Z` et la Release avec l'APK.
  Un push sans changement de version construit seulement l'APK (artefact), sans release.
- Chaque nouvelle version se développe sur une branche à son nom (ex. `1.1.0`) : la CI y construit
  un APK de test (onglet Actions → artefact) sans publier. La release est publiée une fois la
  branche fusionnée dans la branche par défaut.

### Clé de signature (à faire une fois)
Pour qu'une mise à jour s'installe par-dessus l'ancienne (en gardant la progression), l'APK doit
toujours être signé avec la même clé :
```bash
keytool -genkeypair -keystore meowcha.jks -alias meowcha -keyalg RSA -keysize 2048 -validity 36500
base64 -w0 meowcha.jks   # à copier dans le secret KEYSTORE_BASE64
```
Puis dans GitHub → Settings → Secrets and variables → Actions, ajoute :
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` (`meowcha`), `KEY_PASSWORD`.
Garde bien le fichier `.jks` : sans lui, plus de mises à jour possibles.
