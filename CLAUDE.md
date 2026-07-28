# entcore — bundle multi-modules (Maven + fronts mixtes)

Bundle de **15 modules Maven** déclarés à la racine (`pom.xml`) : `admin`, `app-registry`, `archive`, `audience`, `auth`, `cas`, `communication`, `conversation/backend`, `directory`, `feeder`, `infra`, `portal/backend`, `timeline`, `workspace`, `tests`. Chaque module est un **module Vert.x** packagé par Maven (parent `org.entcore:ent-core`, lui-même hérité de `io.edifice:app-parent`). Backend **Java/Vert.x (ent-core)**. Les fronts sont **hétérogènes selon le module** — il n'y a pas de convention front unique dans ce repo.

## Architecture

- **Maven multi-projet** : `pom.xml` racine + un `pom.xml` par module.
- **Module `infra`** : runtime cœur qui bootstrappe les autres.
- **Libs partagées** (dépendances versionnées avec le bundle, pas des modules du reactor) : `org.entcore:common`, `org.entcore:session`.
- **`conversation` et `portal`** ont leur backend Java sous `<module>/backend/` (déclarés dans le reactor en tant que `conversation/backend`, `portal/backend`) — structure plus récente. Les modules legacy (`admin`, `auth`, `timeline`, `archive`, `workspace`, `directory`…) gardent le Java directement sous `<module>/src/main/java/`.

## Fronts : quatre familles observées

| Famille | Outil | Où | Modules |
| --- | --- | --- | --- |
| **AngularJS legacy** | webpack + gulp (orchestré depuis la racine) | `<module>/src/main/resources/public/ts/` | `archive`, `workspace`, `directory` |
| **React** | Vite + pnpm (standalone) | `<module>/frontend/` | `conversation`, `portal` |
| **Double front** | AngularJS legacy **+** React, les deux zones dans le même module | les deux zones | `auth`, `timeline` |
| **Angular moderne (v14, Angular CLI)** | `ng` (angular.json) + gulp pour le packaging | `admin/src/main/ts/` | `admin` |
| **Backend seul** | — (pas de front) | — | `app-registry`, `audience`, `cas`, `communication`, `feeder`, `infra` |

> `admin` n'est **ni** de l'AngularJS legacy **ni** du React : c'est de l'Angular moderne (Angular 14, généré via Angular CLI). Une quatrième famille à part entière — ne pas le traiter comme les autres modules legacy.
>
> Toujours **identifier la famille de front du module** avant d'y toucher.
>
> `auth` a déjà son propre [`auth/CLAUDE.md`](auth/CLAUDE.md) détaillant sa zone React (WAYF v2) et le routing legacy/React — s'y référer plutôt que dupliquer.
> `timeline`, autre module double front, n'a pas encore de CLAUDE.md imbriqué — à créer sur le modèle `double-front/` (zone React + zone AngularJS avec garde-fous) si des tâches y touchent prochainement.

## Commandes

| But | Commande |
| --- | --- |
| Build complet | `./build.sh` (options réelles : `--module=<module>` / `-m=<module>`, `--springboard=<env>` / `-s=<env>` (défaut `recette`), `--no-docker`) |
| Build d'un module | `./build.sh --module=<module>` ou `mvn clean install` dans le module |
| Front React d'un module (`auth`, `conversation`, `portal`, `timeline`) | `cd <module>/frontend && pnpm install && pnpm dev` |
| Front Angular moderne (`admin`) | `cd admin/src/main/ts && pnpm install && pnpm dev` (`ng serve --host 0.0.0.0`) ; `pnpm build` / `pnpm build-prod` ; `pnpm test` (`ng test`) ; `pnpm lint` |
| Front AngularJS legacy (`archive`, `workspace`, `directory`) | orchestré par `gulp` / `build.sh` à la racine (`gulpfile.js`) |

> Pas de lockfile front unique à la racine : chaque front React/Angular a son propre `node_modules` et son propre gestionnaire (pnpm, versions différentes selon le module — ex. `auth/frontend` en pnpm 9.12.2, `conversation/frontend` en pnpm 8.6.6). Le `pom.xml` racine garantit la cohérence des versions Maven ; il n'y a pas d'équivalent pour les fronts.

## Travailler dans UN module

1. Repérer le type de module dans la table ci-dessus (back seul / AngularJS legacy / React / double front / Angular moderne).
2. Builder **le module ciblé** (`--module=`) plutôt que tout le bundle quand c'est possible.
3. Respecter les conventions **propres au module** (et son CLAUDE.md imbriqué s'il en a un, comme `auth`).

## À faire / à éviter

- ✅ Garder les changements **circonscrits à un module**.
- ✅ Pour les zones AngularJS legacy : maintenance sans modernisation, sauf demande explicite.
- ❌ Ne pas tenter d'unifier les chaînes de build front (webpack/gulp legacy vs Vite vs Angular CLI) : ce sont trois écosystèmes distincts, assumés comme tels dans ce repo.
- ❌ Ne pas présumer qu'un module ressemble à un autre : les fronts diffèrent fortement d'un module à l'autre (cf. table des familles).
- ❌ Ne pas confondre `admin` (Angular moderne) avec de l'AngularJS legacy — les réflexes/conventions ne sont pas les mêmes.
