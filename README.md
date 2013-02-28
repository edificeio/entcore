Notes d'utilisation
====================

# Installer

* __Gralde__ 1.4
* __Vert.x__ 2-SNAPSHOT (construire à partir des sources. Attention le script de construction ne copie pas `vertx-platform-2.0.0-SNAPSHOT.jar` dans la version releasée. Il faut le faire manuellement. J'ai peut-être raté qqc)
* __Neo4j__ 1.9

# Développer

One-core est une application Vert.x qui contient plusieurs verticle spécialisés. Elle est packagée sous forme de module Vert.x. 

__Squelette d'application__

<pre>
|-- build.gradle
|-- gradle
|-- gradle.properties
|-- README.md
`-- src
    |-- main
    |   |-- java
    |   |   `-- edu.one.core.*
    |   `-- resources
    |       `-- mod.json
    `-- test
</pre>

Conventions : indentation par TAB, nommage en anglais, message de commit en anglais
Configuration : `mod.json`
Construction : `gradle copyMod`
Lancement de vert.x : `vertx runmod edu.one-core-0.1.0-SNAPSHOT`

