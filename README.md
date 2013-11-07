Notes d'utilisation
====================

# Installer

* __Gralde__ 1.4
* __Vert.x__ 2-SNAPSHOT (construire à partir des sources. Attention le script de construction ne copie pas `vertx-platform-2.0.0-SNAPSHOT.jar` dans la version releasée. Il faut le faire manuellement. J'ai peut-être raté qqc)
* __Neo4j__ 1.9
* __Gradle Support__ 1.1.9: plugin Netbean

# Développer

One-Core est une application Vert.x modulaire. Chaque application ou composant technique est packagé dans un module indépendant. On distingue 3 type de modules :

* les modules `Controller`
* les modules `BusMod`
* les modules `Infra`

Remote Debug : 
	 export VERTX\_OPTS='-Xdebug Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000'

__Squelette d'application__

<pre>
.
|-- build.gradle
|-- data
|   `-- dev
|-- directory
|   |-- build.gradle
|   `-- src
|-- gradle
|   `-- maven.gradle
|-- history
|   |-- build.gradle
|   `-- src
|-- infra
|   |-- build.gradle
|   `-- src
|-- mods
|-- neo4j-persistor
|   |-- build.gradle
|   |-- README.md
|   `-- src
|-- README.md
|-- settings.gradle
|-- sync
|   |-- build.gradle
|   |-- gradle
|   |-- README.md
|   `-- src
`-- tracer
    |-- build.gradle
    |-- README.md
    `-- src
</pre>

## A propos des modules

__Dépendances__ :

	infra
	infra -> sync
	infra -> directory
	infra -> history
	tracer
	neo4j-persistor

__Configuration__ :

{mon_module}/src/main/resources/mod.json

__Commandes__ (depuis le projet racine) :

	 `gradle copyMod` : Construit et déploie dans `mods` tous les modules
	 `gradle clean` : Supprime tous les éléments créés par `copyMod`
	 `gradle :{mon_module}:copyMod` : `copyMod` juste pour `{mon_module}` 
	 `gradle :{mon_module}:clean` : `clean` juste pour `{mon_module}`
	 `vertx runmod edu.one.core~infra~0.1.0-SNAPSHOT` : lancer l'application

# Configuration

Le fichier de configuration des application est `mod.json`. Pour définir un configuration personnalisée procéder de la manière suivante :

* à la racine du projet créer un fichier `developer.id` avec votre identifiant de développeur. Cette identifiant ne doit contenir que des lettre [a-z]. 
* copier le fichiers de configuration `mod.json` dans le fichier `{developer.id}.mod.json`

One-Core utilisera prioritairement votre fichier de configuration développeur.

## Paramètres de configuration à personnaliser ##

Paramètres à personnaliser dans les fichier de configuration `{developer.id}.mod.json` des différents modules :

* Dans le module infra

Configuration neo4j :
Le paramètre "server-uri" doit être une chaîne vide si Neo embarqué est utilisé ; pour Neo en mode serveur indiquer l'adresse du serveur. Le "datastore-path" est le chemin des données Neo en local.
<pre>
"server-uri" : "",
"datastore-path" : "/path/to/local/neo/data"
</pre>
Configuration du Tracer :
<pre>"log-path": "/path/to/logs/folder"</pre>
Configuration Wordpress :

    - Changer le "host" si nécessaire, et dans le paramètre "wp-plugin-url" remplacer {wp-site-name} par le nom du dossier du site wordpress
<pre>
"host":"localhost",
"wp-plugin-url":"/{wp-site-name}/?api=1&method=one_",
</pre>
    - Au premier lancement :
<pre>"profile-groups" : {}</pre>
    - Après le premier lancement :
<pre>"profile-groups" : { "PERSEDUCNAT" : "4", "PERSRELELEVE" : "5", "ELEVE" : "6" } //remplacer les 3 ids par les ids des groupes créés automatiquement dans Wordpress</pre>

* Dans le module history

Le chemin du dossier contenant les fichiers de log, le même que le "log-path" du module Tracer :
<pre>"log-path": "/path/to/logs/folder/"</pre>

* Dans le module directory

Le chemin du dossier contenant les fichiers csv pour l'import BE1D :
<pre>"test-be1d-folder" : "/path/to/be1d/folder/"</pre>

* Dans le module sync

Le chemin du dossier contenant les fichiers XML pour l'import AAF :
<pre>"input-files-folder" : "/path/to/aaf/folder/"</pre>


# Convention de codage

* Indentation : TAB
* Java : code style standard

# Guide de contribution

* Message de commit
 * Langue : Anglais
 * préfix : [<ticket>], [Fix <ticket>], [Tmp], [Doc]

# Migration de données

## Exemple d'execution de script MongoDb

  mongo localhost:27017/dbName migration/1.2.0/1.2.0-mongodb.js

