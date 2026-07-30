# Conversation module — CLAUDE.md

## Module overview

`conversation` est le module de messagerie interne d'Edifice (entcore). Il contient :
- Le backend Java (Vert.x) : `backend/src/main/java/org/entcore/conversation/`
- Le frontend React : `frontend/`

Pas de zone AngularJS legacy dans ce module (contrairement à `auth` ou `timeline`) : le front est **React seul**.

## Frontend React

### Stack
- React 18.3.1 + TypeScript 5.9
- Vite 5.4 (dev server, build → `dist/`, base path `/conversation` en prod, vide en dev)
- React Router v6 (`react-router-dom` 6.30)
- TanStack Query v5 (`@tanstack/react-query` + devtools)
- Zustand 4.5 (state management)
- react-hook-form 7.71
- i18next + i18next-http-backend + react-i18next
- @edifice.io/bootstrap + @edifice.io/react (Design System)
- MSW (Mock Service Worker) pour les mocks de dev/tests (`src/mocks/`)

### Commandes
```bash
cd conversation/frontend
pnpm install
pnpm dev            # dev server Vite
pnpm dev:recette     # dev server avec récupération des tokens de thème (get-tokens --auto)
pnpm build           # typecheck + build prod → dist/
pnpm test            # vitest
pnpm test:watch      # vitest --watch
pnpm test:coverage   # vitest run --coverage
pnpm typecheck       # tsc -b --noEmit
pnpm lint            # eslint .
pnpm format          # prettier --write
```

> `pnpm dev` déclenche `predev` → `pnpm get-tokens` (récupération des tokens de thème avant de servir). Le proxy dev cible `http://localhost:8090` par défaut, ou l'environnement de recette si un fichier `.env` est présent (`VITE_RECETTE`, `VITE_ONE_SESSION_ID`, `VITE_XSRF_TOKEN`).

### Mocks / dev offline

- `pnpm dev:mock` (`vite --mode mock`) lance l'app entièrement offline avec les mocks MSW existants, sans dépendre de la recette — utile pour toute vérification visuelle du front dans ce module. Le worker navigateur est démarré depuis `src/mocks/browser.ts` (`setupWorker(...handlers)`), activé dans `main.tsx` quand `import.meta.env.MODE === 'mock'`.
- `src/mocks/handlers.ts` ne contient que les handlers propres au module (`config`/`folder`/`message`) — pas de mocks génériques de session/theme/auth. Si un test a besoin de la session utilisateur, mocker `useEdificeClient` directement dans le test (cf. `MessageList.test.tsx` / `MessagePreview.test.tsx`), pas intercepter les appels HTTP de session.
- **Penser à mettre à jour ces handlers à chaque évolution de l'API** (nouveau champ, nouvel endpoint) pour que `pnpm dev:mock` et les tests restent représentatifs du contrat réel.

### Structure src/
```
src/
├── components/          # Composants UI partagés (MessageActionDropdown, MessageAttachments, MessageRecipientList, SignatureEditor…)
├── features/             # Un dossier par fonctionnalité (app, menu, message, message-edit, message-list, modals)
├── routes/
│   ├── index.tsx         # React Router
│   ├── root/             # Route root
│   ├── pages/            # Pages (Folder.tsx, Message.tsx, OldFormat.tsx)
│   ├── redirections/     # Redirections
│   └── errors/           # not-found, page-error
├── services/
│   ├── api/              # Appels API
│   └── queries/          # Hooks TanStack Query
├── store/                # Stores Zustand
├── hooks/                # Custom hooks (useI18n…)
├── models/                # Types TypeScript
├── providers/             # Wrappers React (QueryClientProvider…)
├── mocks/                 # MSW : handlers (message, folder, config) + setup pour tests/dev
├── config/                # Configuration statique
├── i18n.ts                # Config i18next
└── main.tsx               # Entry point
```

### Convention de structure
- Organisation par **feature** (`features/<nom>/`) plutôt que par type de fichier : chaque feature regroupe son composant principal, ses sous-composants et ses hooks.
- Les **pages** vivent dans `routes/pages/` (fichiers plats, ex. `Message.tsx`, `Folder.tsx`).
- Les tests unitaires sont colocalisés (`*.test.tsx` à côté du composant/hook testé).

### i18n
- Config dans `src/i18n.ts`, hook `useI18n` dans `src/hooks/`.

## Backend Java

### Structure
`backend/src/main/java/org/entcore/conversation/`
- `controllers/` — `ApiController`, `ConversationController`, `TaskController`
- `service/` (+ `service/impl/`) — logique métier
- `filters/` — filtres de sécurité/droits
- `cron/` — tâches planifiées (`PurgeMessages` : purge des anciens messages)
- `util/` — utilitaires

### Transformation de contenu (tiptap transformer)

Le contenu riche des messages (et de toute future donnée éditée avec le même éditeur — ex. message d'absence, `IMPULS-6109`) **ne doit jamais être stocké tel quel**. Il passe systématiquement par un transformer de contenu externe (`fr.wseduc:content-transformer`, lib externe non vendée dans ce repo — pas de source consultable localement, seulement les points d'appel ci-dessous).

- **Client** : `IContentTransformerClient`, instancié dans `Conversation.java:73-76` via `ContentTransformerFactoryProvider.getFactory("conversation", contentTransformerConfig).create()`. Un `IContentTransformerEventRecorder` est instancié à côté pour l'enregistrement des transformations (`ContentTransformerEventRecorderFactory`).
- **Appel type** : `SqlConversationService.transformMessageContent` (`SqlConversationService.java:1075-1094`). Construit un `ContentTransformerRequest` avec `expectedFormats = new HashSet<>(Arrays.asList(ContentTransformerFormat.HTML, ContentTransformerFormat.JSON))` — **les deux formats de sortie sont déjà demandables en une seule transformation**, en entrée aussi bien HTML que JSON tiptap.
- **Usage actuel (messages classiques)** : `updateMessageWithTransformedContent` (`SqlConversationService.java:227-246`), appelé par `create`/`update`/`draft`, ne lit que `transformerResponse.getCleanHtml()` et `getContentVersion()` — la sortie JSON de la réponse est demandée mais **jetée**, car ce module stocke aujourd'hui le corps des messages en HTML seul (`messages."body"` en `TEXT`).
- **Règle pour toute nouvelle fonctionnalité stockant du contenu riche** : ne jamais faire confiance à un HTML ou JSON fourni par le client comme valeur finale de stockage. Toujours repasser par ce transformer, et **exploiter la sortie dont on a besoin** (HTML seul aujourd'hui pour les messages ; JSON **et** HTML si les deux doivent être persistés, comme pour le message d'absence — voir `conversation/specs/IMPULS-6109/CONTRAT-API-IMPULS-6109-US1.md`, §2.3). Un champ équivalent fourni par le client (ex. un `bodyHtml` envoyé alors que le stockage doit être dérivé du JSON) est accepté par tolérance de forme mais **explicitement ignoré**, jamais persisté tel quel.
- Le getter exact pour récupérer la sortie JSON de `ContentTransformerResponse` n'a pas encore été utilisé dans ce repo (aucun appelant existant ne le lit) — à vérifier au moment de coder une fonctionnalité qui en a besoin.

### Build

Module Maven déclaré dans le reactor racine sous `conversation/backend` (voir `pom.xml` racine). Build via `./build.sh --module=conversation` depuis la racine du bundle, ou `mvn clean install` directement dans `backend/`.

## Specs

`specs/` contient les Feature Specs techniques du module (ex. `FS-IMPULS-6108-structure-sender.md`) — à consulter pour le contexte fonctionnel des évolutions récentes avant de modifier le code correspondant.
