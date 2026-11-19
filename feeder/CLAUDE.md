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

## Coexistence avec referential (POC)
- **Mode B (2026-11-19)** : avec l'option `referential-notify`, toute action `manual-*`
  réussie publie `EntityChangedDTO` (ids touchés, Neo4j) sur `ext.referential.entity.changed`
  (`referential/ReferentialNotifyingMessage`, `ReferentialChanges`) ; referential relit
  ces entités dans Neo4j (resynchro ciblée). Option désactivée par défaut.
- Nouvelle action `manual-*` : vérifier que ses ids sont dans les clés reconnues par
  `ReferentialChanges` (message, `data`, ou `result[].id` d'une création), sinon l'ajouter.
- Échec de publication : `transmission_failed action=…`, aucune donnée personnelle, l'action
  n'échoue pas. La resynchro complète rattrape.
- Les imports ne transmettent rien : referential rejoue les mêmes fichiers.
- Cible (étape 9 du plan, famille par famille) : commandes dédiées avec les valeurs relues après
  écriture (Class et Function réécrivent externalId et id : ne jamais transmettre le
  payload de l'appelant).

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
