# Stack, conventions, patterns

## Backend

### Stack

- Langage : Java 8
- Framework : Vert.x 4
- Bases de données :
    - Mongodb 7
    - Neo4J 3.2 (bridé en version 2.3 pour le langage cypher)
    - Postgresql 14
- Tests d'intégration :
    - framework : k6
    - emplacement : commun à tous les modules dans tests/src/test/js/it/scenarios/

### On privilégie

- utilisation des io.vertx.core.Future au lieu des io.vertx.core.Handler. 
- usage des DTOs au lieu des JsonObject. Ces DTOs sont immutables.
- voir @https://github.com/edificeio/web-utils/blob/master/AGENTS.md pour les montées de version dues schémas PostgreSQL.

### Build

Le backend est buildé avec la commande `./build.sh buildBackend`.

## Frontend

### Typologie

Quatre familles observées
Famille	Outil	Où	Modules
AngularJS legacy	webpack + gulp (orchestré depuis la racine)	<module>/src/main/resources/public/ts/	archive, workspace, directory
React	Vite + pnpm (standalone)	<module>/frontend/	conversation, portal
Double front	AngularJS legacy + React, les deux zones dans le même module	les deux zones	auth, timeline
Angular moderne (v14, Angular CLI)	ng (angular.json) + gulp pour le packaging	admin/src/main/ts/	admin
Backend seul	— (pas de front)	—	app-registry, audience, cas, communication, feeder, infra

# Architecture, navigation

Chaque module a son dossier et embarque son front.
Certains modules ont un dossier frontend et backend séparés.
Un module est le seul à pouvoir manipuler ses données dans sa base de données. Il doit fournir aux autres modules une interface via l'eventbus si ceux-ci veulent les manipuler.

# Interdits, limites, pièges

✅ Les classes des DTOs sont annotées de `@JsonIgnoreProperties(ignoreUnknown = true)` pour éviter les problèmes de désérialisation.
✅ Voir @https://github.com/edificeio/web-utils/blob/master/AGENTS.md pour les limitations de montées de version des schémas SQL.
❌ Il ne faut plus utiliser les méthodes dépréciées de UserUtils.
❌ Un module ne doit pas accéder directement à la base de données d'un autre module.
✅ Pour les zones AngularJS legacy : maintenance sans modernisation, sauf demande explicite.
❌ Ne pas tenter d'unifier les chaînes de build front (webpack/gulp legacy vs Vite vs Angular CLI) : ce sont trois écosystèmes distincts, assumés comme tels dans ce repo.
❌ Ne pas présumer qu'un module ressemble à un autre : les fronts diffèrent fortement d'un module à l'autre (cf. table des familles).
❌ Ne pas confondre admin (Angular moderne) avec de l'AngularJS legacy — les réflexes/conventions ne sont pas les mêmes.