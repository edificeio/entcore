# feeder — alimentation du référentiel (Neo4j)

Module **backend seul** (Vert.x). Source de vérité des entités référentielles
(Structure, User, Class, Group, Function, Subject) **tant que le POC referential
est en cours** : Neo4j fait foi, `~/project/sources/referential` (PG) suit.

## Points d'entrée
- `Feeder.handleAction` : switch des actions bus. Catégories : import, `manual-*`,
  cycle de vie, doublons, export, timetable.
- Imports : `aaf/AafFeeder` (2D), `aaf1d/Aaf1dFeeder`, `csv/CsvFeeder`, tous orchestrés
  par `dictionary/structures/Importer`. Étapes AAF = `*ImportProcessing*`, enchaînées
  par `BaseImportProcessing.next()`.
- Opérations manuelles : `ManualFeeder` + `dictionary/structures/{User,Group,Structure,Profile,Subject}`.
- Contraintes d'unicité Neo4j : pas ici, dans `infra/src/main/resources/neo4j/001-schema-index.cypher`.

## Coexistence avec referential (POC) en cible
- **Toute action `manual-*` qui écrit dans Neo4j transmet ensuite une commande à
  referential** (client généré `io.edifice.referential.v1`), avec les valeurs
  **relues après écriture** (id, externalId, login, displayName, activationCode,
  ids des ProfileGroup créés). Ne jamais transmettre le payload de l'appelant :
  Class et Function réécrivent externalId et id.
- Échec de transmission : journaliser (`transmission_failed`, action + ids, aucune
  donnée personnelle), ne pas faire échouer l'action. La resynchro rattrape.
- Les imports ne transmettent rien : referential rejoue les mêmes fichiers.
- Nouvelle action bus : la classer (import / manuelle / cycle de vie / doublons /
  export / timetable) et décider transmise ou resynchro seule.

## Pièges
- **État en mémoire entre étapes** : `GraphData`, `ImporterCachedStructure`
  (décide CREATE vs rien pour Class/FunctionalGroup/FunctionGroup — pas de MERGE),
  `Importer.userImportedExternalId` (lu par `markMissingUsers`), `groupClasses`,
  `StudentImportProcessing.resp`. Modifier une étape peut casser la suivante sans erreur.
- **Logins** : dédoublonnés par une `HashMap` statique par JVM (`Validator.logins`),
  rechargée au démarrage / `reinit-logins`. Un login est réservé même pour un
  utilisateur existant. Le seul garde-fou cluster est la contrainte Neo4j.
- **Checksum** : SHA-1 des valeurs sans noms de champs, dans l'ordre d'un `HashSet`.
  Il conditionne le `SET` : ne pas le réutiliser comme empreinte stable.
- **ExternalId CSV** sans colonne : hash avec seed aléatoire → différent à chaque run.
- **Class.externalId** est forcé à `structure.externalId$nom` (unique) : homonyme = échec.
- **Ids non UUID** : ProfileGroup, FunctionGroup, Subject = `id(n)-timestamp`.
- **Portées destructives larges** : `markMissingUsers` (source + académie entières),
  `createAndLinkSubjects` (source entière en AAF2D), `CleanImportProcessingGlobal`.
- Actions reçues pendant un import : mises en file (`Feeder.handle`), sauf `manual-*`
  si `allow-manual-actions-during-feeds`.

## Commandes
- Build : `./build.sh --module=feeder` depuis la racine entcore.
- k6 : `tests/src/test/js` (ADMC local `tom.mate` / `password`).
