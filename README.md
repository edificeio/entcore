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
* __Vert.x 2.O.0__ final (https://vertx.io/download/)
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

## Débugage

Ajouter la variable d'environement suivante pour debugger sur le port 5000

	export VERTX_OPTS='-Xdebug Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5000'
