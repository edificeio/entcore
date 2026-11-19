# feeder

> Explication approfondie : [README.detailed.md](README.detailed.md). Référence technique dense (pour
> travailler sur le code sans le relire en entier) : [CLAUDE.md](CLAUDE.md).

Module qui alimente l'annuaire Edifice (structures, classes, groupes, utilisateurs dans Neo4j) à partir
de sources externes, et peut aussi exporter ce même annuaire en sens inverse.

## Ce que ça importe

- **AAF** (Siecle, 2nd degré — collèges/lycées) : fichiers XML fournis par l'académie.
- **AAF1D** (1er degré — écoles primaires) : même famille de format, dictionnaire et quelques champs différents.
- **CSV** : import manuel/générique via un assistant web (un fichier par type de compte : élève, enseignant, personnel, responsable, invité).
- **EDT/Pronote, UDT** : emplois du temps (stockés à part, en MongoDB).

## Et en sens inverse

- **Export Éliot** : relit le graphe et régénère un flux XML au format Siecle, à destination du système ministériel.

## Comment ça se pilote

Ce module n'a **aucune route HTTP** : tout passe par le bus d'événements Vert.x, adresse
`entcore.feeder`, avec un champ `"action"` (`import`, `validate`, `export`, `transition`, `manual-*`,
actions liées aux doublons...). Les routes HTTP qui envoient ces messages vivent dans le module
`directory` (assistant d'import CSV, import de planning, déclenchement d'export).

Quel format traiter est décidé par un champ `"feeder"` du message (`AAF`, `AAF1D`, `CSV`, `PRONOTE`),
qui sélectionne l'implémentation correspondante dans une simple table de correspondance.

## Le principe clé

**Quel que soit le format d'entrée, tout converge vers le même moteur générique**
(`dictionary/structures/Importer.java`) : un `JsonObject` avec un jeu fixe de champs
(`externalId`, `structures`, `classes`, `groups`, `relative`, `functions`...) est validé, dédupliqué,
et écrit dans Neo4j par lots. Comprendre ce moteur une fois permet de comprendre n'importe quel format
source rapidement — voir le [README détaillé](README.detailed.md) pour le déroulé complet.

## Points à connaître avant de commencer

- **Un seul import actif à la fois, toutes sources confondues** (le moteur est un singleton) — un
  second import arrivant pendant qu'un premier tourne est mis en attente, pas rejeté.
- **Les classes et groupes n'ont jamais de fichier dédié** : ils naissent d'un simple champ
  `structureExternalId$nomDeClasse` porté par un élève ou un enseignant.
- **Une ligne/un enregistrement invalide n'interrompt pas tout l'import** : il est ignoré et noté dans
  le rapport, le reste continue.
- Un compte absent d'un import est **marqué comme disparu**, puis pré-supprimé (perd ses accès), puis
  supprimé définitivement — en plusieurs étapes espacées de plusieurs semaines, jamais immédiatement.

## Pour tester

Le module `entcore/tests` fournit un générateur programmatique de fichiers AAF de test
(`entcore/tests/src/test/js/it/scenarios/directory/_aaf-generator.ts`) pour construire des jeux de
données réalistes (plusieurs établissements, classes, relations enseignant/classe, fratries...).
