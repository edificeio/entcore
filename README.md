# À propos d'Entcore
    
* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt) - Copyright Edifice
* Financeur(s) : Edifice
* Développeur(s) : Edifice
* Description : l’application entcore est un noyau d'ENT modulaire et performant.


Notes d'utilisation
====================

Ent-Core est un [ENT](https://fr.wikipedia.org/wiki/Espace_num%C3%A9rique_de_travail) minimaliste,
modulaire et versatile. Il permet d'implémenter aussi bien des ENT de premier et de second degré.

**Remarques** : _Ces notes proposent une démarche rapide de prise en main (typiquement l'installation d'une machine de développement).
Elles ne détaillent pas l'installation des composants techniques (ex : base de données)._

# Installation rapide

## Récupérer le code

Installer le [Git](http://git-scm.com/) et lancer la commande suivante dans un terminal

	git clone http://code.web-education.net/one/ent-core.git

## Installer les composants techniques

* __JSE 7__
* __Gralde 1.6__ (http://www.gradle.org/downloads)
* __Vert.x 2.O.0__ final (http://vertx.io/downloads.html)
* __Neo4j 2__ (http://www.neo4j.org/download/linux)
* __MongoDB 2.4__ (http://docs.mongodb.org/manual/tutorial/install-mongodb-on-debian/)

## Configurer

Reiseigner le dépôt mandataire de dépendances Maven d'ent-core.
Ajouter au fichier `{VERTX_HOME}/conf/repos.txt` la ligne suivante :

	maven:http://maven.web-education.net/nexus/content/groups/public/

_Remarques_ : L'utilisateur qui lance ent-core doit avoir les droits sur le dossier d'installation de Vert.x,
pour permettre l'installation de modules (Vert.x) systèmes.

Installer le schéma de base neo4j :

	neo4j-shell < {VERSION_ENTCORE}/{NOM_SCHEMA}-schema.cypher

## Compiler et Lancer

Lancer la commande de construction _gradle_ à la racine du dépôt _ent-core_ :

	gradle copyMod

Lancer la plate-forme avec _vert.x_

	vertx runmod org.entcore~infra~{VERSION_ENTCORE}

Accéder à l'application dans un navigateur à l'URL:

	http://localhost:8090

# Développer

## Configuration

Le fichier de configuration général de la plate-forme est `{ENTCORE_HOME}/infra/src/main/resources/mod.json`
Pour personnaliser la configuration et éviter les conflits procéder de la manière suivante :

1. Éditer le ficher `{ENTCORE_HOME}/developer.id` avec votre identifiant de développeur `{devid}`.
   Il doit être exclusivement composé de lettres ([a-z])
2. Copier le fichier de configuration `{ENTCORE_HOME}/infra/src/main/resources/mod.json`
   dans le fichier `{ENTCORE_HOME}/infra/src/main/resources/{devid}.mod.json`
3. Personnaliser le fichier `{devid}.mod.json`

Ent-Core utilisera prioritairement votre fichier de configuration développeur.

## Cycle de vie

Lancer depuis le projet racine :

* `gradle copyMod` : Construit et déploie dans `mods` tous les modules
* `gradle clean` : Supprime tous les éléments créés par `copyMod`
* `gradle :{nom_module}:copyMod` : `copyMod` juste pour `{nom_module}`
* `gradle :{nom_module}:clean` : `clean` juste pour `{nom_module}`
* `gradle integrationTest` : Éxécute les test d'intégrations

## Front AngularJS legacy contre une recette (sans backend local)

Pour les modules buildés par `gulpfile.js` (`directory`, `archive`, `workspace`...), `dev-server.js` sert le dossier `public/` du module depuis le disque et proxifie tout le reste (vues serveur, `ng-app.js`, thème, API, `/assets/version`) vers une recette, avec la session d'un utilisateur connecté.

```shell
npm install
cp .env.template .env        # puis dev-auth-fetcher connect (ou le skill auth-user-frontend)
npm run dev:directory        # ouvre http://localhost:3000/userbook/mon-compte#/edit-me
```

Autre module ou autre page : `MODULE=workspace START_PATH=/workspace/workspace npm run legacy:dev`. Port : variable `PORT` (défaut 3000).

- Les `.ts` sont recompilés (webpack) et les templates `public/template/` rechargés automatiquement dans le navigateur.
- Les clés de `i18n/fr.json` du module sont fusionnées par-dessus celles de la recette.
- Les vues `view-src/` restent celles déployées sur la recette : un changement local n'y apparaît pas. Une feuille de style du module qu'une vue lie mais que la recette n'a pas encore se déclare dans `LOCAL_CSS` (chemins relatifs à `public/`, séparés par des virgules) pour être injectée.
- Une session expirée se traduit par une redirection vers `/auth/login`, signalée dans la console : régénérer le `.env`.

## Débugage

Ajouter la variable d'environement suivante pour debugger sur le port 5000

	export VERTX_OPTS='-Xdebug Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5000'

## Tests de charge

### Description

Les tests de charge sont exécutés par un container k6 qui est paramétré via les variables d'environnement passées au container `load-tester`.

- `ROOT_URL`, URL de l'ENT à appeler
- `DURATION`, duration of the test
- `VUS`, nombre d'utilisateurs concurrents
- `NB_USERS`, nombre d'utilisateurs dont les posts seront utilisés pour le jeu de test
- `DATA_SCHOOL_NAME`, nom de l'école à créer
- `DATA_ROOT_PATH`, chemin absolu vers les jeux de test (ne devrait pas être changé)

### Ajouter un test

Ajouter un fichier js de test dans `tests/src/test/js/stresstest/scenarios/impacted-module/my-scenario.js`

### Lancement

```shell
docker compose run --rm load-tester run file:///home/k6/src/scenarios/impacted-module/my-scenario.js
```