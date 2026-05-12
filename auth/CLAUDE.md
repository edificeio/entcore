# Auth module — CLAUDE.md

## Module overview

`auth` est le module d'authentification Edifice (entcore). Il contient :
- Le backend Java (Vert.x) : `src/main/java/org/entcore/auth/`
- Le frontend React (WAYF v2) : `frontend/`
- L'ancienne WAYF AngularJS (ressources servies statiquement)

## Frontend React (WAYF v2)

### Stack
- React 18 + TypeScript
- Vite (dev server port 4200, build output `dist/`)
- React Router v6 (basename `/auth` en prod, vide en dev)
- i18next + i18next-http-backend + react-i18next
- TanStack Query v5
- Zustand (state management)
- @edifice.io/bootstrap (Design System)

### Commandes
```bash
cd auth/frontend
pnpm run dev      # dev server :4200
pnpm run build    # build prod → dist/
pnpm run test     # vitest
pnpm run lint     # eslint
```

### Structure src/
```
src/
├── components/         # Composants UI réutilisables — chacun dans son propre dossier
│   ├── App.tsx         # Entry point React
│   ├── Level2Stub/
│   ├── ProviderButton/
│   └── ProviderList/
├── config/             # Configuration statique (wayf.config.ts)
├── hooks/              # Custom hooks (useWayfConfig, useI18n)
├── models/             # Types TypeScript (wayf.ts)
├── providers/          # Wrappers React (QueryClientProvider)
├── routes/
│   ├── index.tsx       # React Router
│   ├── errors/         # Composants d'erreur (not-found, page-error)
│   ├── root/           # Route root
│   └── pages/          # Pages — un fichier .tsx par page
│       └── Wayf.tsx
├── services/           # API calls
├── store/              # Zustand stores
├── i18n.ts             # Config i18next (namespaces: common, auth)
└── main.tsx            # Entry point
```

### Convention de structure (IMPORTANT)
- **Chaque composant** vit dans `components/{NomComposant}/` avec :
  - `NomComposant.tsx` — le composant (fichier nommé après le composant, pas `index.tsx`)
  - `NomComposant.css` — CSS scopé au composant
  - `NomComposant.test.tsx` — tests unitaires si nécessaire
  - `index.ts` — barrel : `export { NomComposant } from './NomComposant';`
- **Les pages** vivent dans `routes/pages/NomPage.tsx` + `NomPage.css` — fichiers plats, pas de dossier
- **CSS scopé par composant** : chaque composant importe son propre CSS ; `Wayf.css` ne contient que le layout de la page (grid, selection, footer)

### Feature flag (WAYF v2)
La nouvelle WAYF React s'affiche si le cookie `wayf-beta=true` est présent.
Géré côté backend dans `SamlController.java` → `GET /saml/wayf`.
En dev, le dev server Vite redirige `/` et `/saml/wayf` vers `wayfv2.html`.

### i18n
- Namespace `common` → `/i18n`
- Namespace `auth` → `/auth/i18n` (contient les clés `wayf.*`)
- Clés WAYF utilisées : `wayf.select.profile`, `wayf.teacher`, `wayf.student`, `wayf.parent`, `wayf.personnel`, `wayf.guest`
- Utiliser `useTranslation('auth')` dans les composants WAYF

### Configuration WAYF v2
Fichier statique `src/config/wayf.config.ts` indexé par `window.location.hostname`.
Fallback sur `DEFAULT_WAYF_CONFIG` si le domaine n'est pas trouvé.
Format JSON validé dans l'US1 :
```json
{
  "wayf-v2": {
    "domaine.fr": {
      "providers": [
        { "i18n": "wayf.teacher", "acs": "/auth/saml/...", "color": "#c53030" },
        { "i18n": "wayf.student", "children": [...] }
      ]
    }
  }
}
```

### Theme assets
- Logo : `/assets/themes/${childTheme}/img/logo.png`
- Background : `/assets/themes/${childTheme}/img/background.png`
- `childTheme` injecté par le backend dans `<script id="saml-wayf">` (Mustache)

## Backend Java

### Routing WAYF
`SamlController.java` → `GET /saml/wayf` :
- Cookie `wayf-beta=true` → render `wayfv2.html` (React)
- Sinon → render `wayf.html` (AngularJS)

### Configuration
- `saml-wayf` dans `auth.conf` → paramètres des providers SAML par domaine
- `skins` → mapping domaine → thème
