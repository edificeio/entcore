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

# Convention de codage

* Indentation : TAB
* Java : code style standard

# Guide de contribution

* Message de commit
 * Langue : Anglais
 * préfix : [<ticket>], [Fix <ticket>], [Tmp], [Doc]

