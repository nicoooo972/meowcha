---
name: content-pack
description: Crée ou met à jour un pack de contenu téléchargeable (DLC) de Meowcha Café — nouveaux chats, recettes, musique, bruitages — sans publier de nouvelle version de l'app. À utiliser quand l'utilisateur demande d'ajouter du contenu au jeu (chats, recettes, sons) plutôt qu'une fonctionnalité de code.
---

# Packs de contenu téléchargeables

Le contenu additionnel (chats, recettes, musique, bruitages) est séparé de l'APK : publié
dans la release GitHub `content` et téléchargé par l'app au lancement (`ContentPacks.kt`).
Ça permet d'ajouter du contenu **sans sortir de nouvelle version de l'app**.

## Structure d'un pack

`packs/<id>/manifest.json` :

```json
{
  "id": "sakura-spring",
  "version": 2,
  "name": "Printemps Sakura",
  "description": "...",
  "cats": [ { "id": "...", "name": "...", "fur": "#RRGGBB", "accent": "#RRGGBB",
              "eyes": "#RRGGBB", "pattern": "PLAIN|TABBY|CALICO|TUXEDO|POINTS",
              "accessory": "NONE|BOW|FLOWER|GLASSES|BELL|CROWN",
              "accessoryColor": "#RRGGBB", "favorite": "<id recette>", "quote": "..." } ],
  "recipes": [ { "id": "...", "name": "...", "steps": ["ESPRESSO", "MILK", ...],
                 "price": 9, "unlockDay": 4 } ],
  "music": { "cafe": "music/....wav", "home": "music/....wav" },
  "sfx": { "add": "sfx/pop.wav", "perfect": "sfx/perfect.wav", "...": "..." }
}
```

- `steps` référence des valeurs de l'enum `Ingredient` (`game/Content.kt`) — ne pas inventer
  un nom qui n'existe pas côté app, `Ingredient.valueOf(...)` plante sinon à l'installation.
- `pattern`/`accessory` idem avec `FurPattern`/`Accessory`.
- Les fichiers audio (`music/`, `sfx/`) sont générés par synthèse dans `tools/make_packs.py` —
  pas de fichiers binaires à fournir à la main, décrire le son voulu et l'ajouter au script.

## Publier une mise à jour de pack

1. Modifier `packs/<id>/manifest.json` (ajouter chats/recettes, ou ajuster les sons dans
   `tools/make_packs.py`).
2. **Incrémenter `version`** dans le manifest — l'app compare cette valeur à ce qu'elle a
   déjà installé (`ContentPacks.installedVersion`) et ne re-télécharge que si c'est plus
   récent. Oublier ce bump = le contenu ne sera jamais proposé aux joueurs qui ont déjà le
   pack.
3. `git add packs/ tools/make_packs.py && git commit && git push` sur la branche courante.
4. Le workflow `.github/workflows/content.yml` se déclenche automatiquement sur les
   changements sous `packs/**` ou `tools/make_packs.py`, reconstruit les zips et met à jour
   la release `content` (`gh run list --workflow content.yml --limit 3` pour suivre).
5. Aucune release d'app à publier, aucun bump de `VERSION` — c'est indépendant.

## Test local (optionnel, nécessite Python + numpy)

```bash
pip install numpy
python tools/make_packs.py dist
```

Génère les zips et `dist/index.json` localement pour vérifier avant de pousser.
