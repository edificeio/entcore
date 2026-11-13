# Boilerplate — frontend

Application React 18 + TypeScript + Vite de l'écosystème Edifice.

## Prérequis

- Node `>=20.19` ou `>=22.12` (voir `engines` dans `package.json`) — l'image de build est en Node 24
- pnpm `10.34.5` (voir `packageManager` dans `package.json`)

## Démarrage

```bash
pnpm install   # installe les dépendances ET les hooks git (script `prepare`)
pnpm dev       # serveur de dev Vite
```

## Scripts utiles

| Script        | Rôle                                  |
| ------------- | ------------------------------------- |
| `pnpm dev`    | Serveur de dev Vite                   |
| `pnpm build`  | Build de production                   |
| `pnpm lint`   | ESLint sur tout le projet             |
| `pnpm format` | Prettier `--write` sur tout le projet |
| `pnpm test`   | Tests Vitest                          |

## Serveur de dev : proxy & i18n local

La config Vite (`vite.config.ts`) s'appuie sur deux plugins maison (`vite/plugins/`) :

- **`createDevProxyConfig`** (`devProxy.ts`) — construit le proxy de dev vers la recette à partir
  d'une simple liste de `routes`. Les identifiants de session sont lus depuis un fichier `.env`
  (`VITE_RECETTE`, `VITE_ONE_SESSION_ID`, `VITE_XSRF_TOKEN`). Sans `.env`, les requêtes ciblent
  `http://localhost:8090` (backend local).
- **`serveLocalI18n`** (`serveLocalI18n.ts`) — voir ci-dessous.

### Travailler sur les traductions sans backend (`serveLocalI18n`)

Par défaut, l'app charge ses traductions depuis la recette (le namespace `boilerplate` est servi sur
`/boilerplate/i18n`, proxifié). Le plugin `serveLocalI18n` permet d'**intercepter cette route en
dev** et de servir un fichier `fr.json` **local** à la place, pour itérer sur les libellés sans
backend qui tourne :

```ts
// vite.config.ts
serveLocalI18n({
  route: '/boilerplate/i18n',
  filePath: resolve(__dirname, '../backend/src/main/resources/i18n/fr.json'),
}),
```

- Si le fichier `filePath` **existe**, il est servi tel quel sur `route` (priorité sur le proxy).
- S'il **n'existe pas** (clone frais sans module `backend`), le plugin laisse passer la requête
  (`next()`) : elle repart vers le proxy comme avant. Aucun réglage à faire.

Pour activer l'i18n local : déposer un `fr.json` au chemin pointé par `filePath` (ou adapter ce
chemin). Le plugin n'agit qu'en dev (`apply: 'serve'`).

## Hooks git (Husky + lint-staged)

Les hooks sont gérés par [Husky](https://typicode.github.io/husky/) (v9, format courant : pas de
`husky.sh` ni de shebang) et [lint-staged](https://github.com/lint-staged/lint-staged).

Le projet npm vit dans `frontend/` alors que la racine git est au-dessus. Les hooks sont donc
stockés dans `frontend/.husky/` et installés par le script `prepare` :

```jsonc
"prepare": "cd .. && husky frontend/.husky || true"
```

`prepare` s'exécute automatiquement à chaque `pnpm install` : un **clone frais réinstalle donc les
hooks** sans étape manuelle. Le `|| true` évite de casser l'install dans un environnement sans git
(CI). Pour désactiver temporairement les hooks : `HUSKY=0 git commit ...`.

### `pre-commit` — rapide, ciblé sur le _staged_

```sh
cd frontend && pnpm run pre-commit   # = lint-staged
```

`lint-staged` n'agit que sur les fichiers **indexés (staged)**, pas sur tout le repo :

```jsonc
"lint-staged": {
  "**/*.{js,ts,tsx}": "eslint --no-warn-ignored",   // bloque le commit si erreur de lint
  "**/*": "prettier --write --ignore-unknown"        // formate et ré-indexe les fichiers
}
```

> ESLint tourne **sans `--fix`** : il vérifie et bloque, mais ne modifie pas le code. Prettier, lui,
> formate (`--write`) et lint-staged ré-indexe automatiquement les fichiers corrigés.

### `pre-push` — build complet

```sh
cd frontend && pnpm run pre-push   # = pnpm run build
```

Le build (lent) est volontairement **sorti du pre-commit** pour garder les commits rapides ; il est
déclenché une seule fois, au push.

### Tests

Les tests **ne tournent pas dans les hooks** : ils sont assurés par la CI. Lance-les à la main avec
`pnpm test`.
