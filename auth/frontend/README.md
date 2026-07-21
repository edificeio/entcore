# auth/frontend — WAYF v2

Frontend React (WAYF v2) du module d'authentification Edifice. Voir `/Volumes/Work/entcore/auth/CLAUDE.md` pour le contexte complet (structure, feature flag, i18n, configuration WAYF).

## Prérequis

- Node >= 20
- pnpm 9 (`packageManager: pnpm@9.12.2`)

## Installation

```bash
cd auth/frontend
pnpm install
```

## Configuration (.env)

Le dev server Vite proxifie les routes API/assets (`/assets`, `/theme`, `/i18n`, `/auth`, `/session`, etc.) vers un environnement de recette. Ce proxy a besoin d'un fichier `.env` rempli pour s'authentifier auprès de la recette — **sans lui, les assets (logos, thèmes, i18n distant...) ne seront pas chargés**.

Copier le template et le remplir :

```bash
cp env.template .env
```

```
VITE_XSRF_TOKEN=      # token XSRF d'une session recette
VITE_ONE_SESSION_ID=  # oneSessionId d'une session recette
VITE_RECETTE=         # URL de la recette, ex. https://recette-xxx.opendigitaleducation.com/
```

Ces valeurs proviennent d'une session authentifiée sur une recette Edifice (cookies `XSRF-TOKEN` et `oneSessionId`). La skill `frontend-toolkit:auth-user-frontend` peut générer ce `.env` automatiquement.

Sans `.env` (ou fichier vide), le proxy retombe sur `http://localhost:8090` sans authentification.

## Lancer en dev

```bash
pnpm run dev
```

Serveur disponible sur `http://localhost:4200`. En dev, `/` et `/saml/wayf` sont redirigés vers `wayfv2.html`.

## Autres commandes

```bash
pnpm run build          # build prod → dist/
pnpm run preview         # preview du build (port 4300)
pnpm run test            # tests (vitest)
pnpm run test:watch      # tests en mode watch
pnpm run test:coverage   # coverage
pnpm run lint            # eslint
pnpm run typecheck       # tsc
pnpm run format          # prettier --write
```
