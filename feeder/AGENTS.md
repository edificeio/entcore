# feeder — CLAUDE.md

Périmètre : `entcore/feeder/src/main/java/org/entcore/feeder/`. Pour une explication pédagogique, voir
[README.md](README.md) (rapide) et [README.detailed.md](README.detailed.md) (approfondi).

## Vue d'ensemble en une phrase

`feeder` est le module Edifice/entcore qui **importe** des annuaires scolaires externes (AAF/Siecle 2nd
degré, AAF1D 1er degré, CSV, EDT/Pronote, UDT) vers le graphe Neo4j (Structure/User/Class/Group/...), et
**exporte** en sens inverse le contenu du graphe vers le système ministériel Éliot. C'est un **module
100% event-bus, sans aucune route HTTP** : les contrôleurs HTTP qui déclenchent tout ceci vivent dans le
module `directory` (`entcore/directory/.../controllers/{ImportController,TimetableController,DirectoryController}.java`)
et relaient vers `feeder` via le bus d'événements.

## Point d'entrée unique : le bus d'événements

- Verticle : `Feeder` (`Feeder.java`, `extends BusModBase`), déclaré comme `main` dans `src/main/resources/mod.json`, `worker: true`.
- Adresse : `Feeder.FEEDER_ADDRESS = "entcore.feeder"` (config key `address`, `Feeder.java:98`). **Aucune route HTTP dans ce module.**
- Dispatch : un seul `consumer` (`Feeder.java:235-236`) → `handle()` → `handleAction()` (`Feeder.java:388-568`), un gros `switch` sur le champ `"action"` du message.
- Si un import est déjà en cours (`Importer.getInstance().isReady() == false`), tout message est mis en file d'attente (`eventQueue`) sauf les actions `manual-*` si `allow-manual-actions-during-feeds=true`. La file se vide quand `Importer.clear()` repasse `isReady()` à `true`.

### Table des actions principales

| Action | Handler | Effet |
|---|---|---|
| `import`, `import-with-auto-export` | `launchImport` → `validateAndImport` → `doImport` | Import complet : valide d'abord (`launchImportValidation`), n'importe que si `!report.containsErrors()` |
| `import-csv` | `launchImportCsv` | Import CSV déclenché par le cron `CsvImportsLauncher` (bypass partiel du wrapper validate-then-import) |
| `importWithId` / `validateWithId` | via `ValidatorFactory` | Reprend un import CSV préalablement uploadé/mappé (Mongo `imports`, par `_id`) |
| `validate` | `launchImportValidation` | Dry-run, ne persiste rien (sauf le rapport) |
| `columnsMapping` / `classesMapping` | `csvColumnMapping` / `csvClassesMapping` | Étapes de l'assistant CSV (mapping colonnes / classes) |
| `transition` | `launchTransition` | Rejoue toute la structure (voir `Transition`), gardé par `GraphData.isReady()` |
| `export` | `launchExport` | Déclenche `EliotExporter` si `config.exporter == "ELIOT"` |
| `delete-users`, `pre-delete-users`, `erase-timetable-reports` | — | Étapes du cycle de suppression (voir plus bas) |
| `ignore-duplicate`, `list-duplicate`, `merge-duplicate`, `merge-by-keys`, `unmerge-by-logins`, `mark-duplicates`, `automerge-duplicates`, `check-duplicates` | `DuplicateUsers` | Gestion des doublons |
| `manual-init-timetable-structure`, `manual-edt`, `manual-udt` | `AbstractTimetableImporter` / `EDTImporter` / `UDTImporter` | Import EDT/UDT incrémental |
| `manual-create-*`, `manual-update-*`, `manual-remove-*`, `manual-delete-user`, `manual-restore-user`, `manual-add-user(s)`, `manual-relative-student`, `manual-structure-attachment/-detachment`, ... | `ManualFeeder` | CRUD manuel (hors import fichier) |
| défaut | — | `sendError(message, "invalid.action")` |

Upload de fichiers AAF bruts (tests d'intégration) : consumers **local only**, actifs seulement si env var `ALLOW_UPLOAD_AAF_FILES=true`, relayés par les routes `/api/internal/*` du module `directory` (`TaskController`, aussi désactivées sans la variable) :
- `feeder.upload-aaf` → `Feeder.uploadAAFFiles(...)` : écrit sous `<racine>/<subPath>/` après sanitation anti path-traversal, y dépose le marqueur `.uploaded-by-it` et inscrit `subPath` dans `<racine>/importDirectories.json` (sans cette inscription, l'import AAF ne lit pas le dossier). `<racine>` = `Feeder.getFilesDirectory(feeder)` (`imports.<feeder>.files`, sinon `import-files`) quand la requête porte `feeder` (`AAF`/`AAF1D`), sinon `import-files`.
- `feeder.clean-aaf` → `Feeder.cleanAAFFiles(...)` : supprime un dossier (ou tous) **créé par un upload** ; refuse sans le marqueur, refuse un symlink, un `subPath` autre qu'un nom simple, et un import en cours (409). Logique de chemins dans `utils/AAFUploadDirectories` (testée par `AAFUploadDirectoriesTest`).
- Import synchrone : `POST /directory/api/internal/trigger-import {feeder, wait:true}` envoie l'action `import` et répond **à la fin** avec un résumé du `Report` (`directory/util/ImportReportSummary`), contrairement à `import-with-auto-export`/`POST /directory/import` qui répondent immédiatement. La réponse attend aussi la fin du **post-import** (fusion INE, détection/fusion des doublons, règles de communication) via `waitPostImport` (action bus `import`, défaut `false` : comportement historique inchangé ; `true` par défaut côté `trigger-import wait:true`). `PostImport.execute(source, structures, onEnd)` rappelle `onEnd` après ces étapes ; les tâches lancées ensuite sans callback (tenant, groupes manuels, groupes fonctionnels) ne sont pas attendues.
- Piège : avec `subPath` inscrit dans `importDirectories.json`, `BaseImportProcessing.initAcademyPrefix` préfixe les externalId du graphe par `<subPath>-` ; `AAF` et `AAF1D` partagent `importDirectories.json` si leurs `imports.<feeder>.files` ne sont pas distincts.
- Scénarios k6 qui s'en servent : `tests/src/test/js/it/scenarios/feeder/README.md`.
- Liens `RELATED` élève↔parent à l'import d'un élève (`Importer.createOrUpdateStudent`, requêtes de relations) : quand le fichier donne au moins un responsable, les liens vers les parents **absents du fichier** sont supprimés, mais seulement si `r.source = <source importée>` ou si le lien n'a pas de source **et** que le parent a lui-même la source importée. Un lien sans source vers un parent d'une autre source (parent créé par CSV, ENABLING-1377) est donc conservé ; avant ce garde-fou, le second import AAF1D le supprimait. Contrepartie : un lien sans source vers un parent d'une autre source n'est plus nettoyé automatiquement. Scénario de non-régression : `feeder/parent-csv-children-link-maintained.ts`.

### Le registre `Feed`

```java
public interface Feed {
    void launch(Importer importer, Handler<Message<JsonObject>> handler) throws Exception;
    void launch(Importer importer, String path, Handler<Message<JsonObject>> handler) throws Exception;
    void launch(Importer importer, String path, JsonObject mappings, Handler<Message<JsonObject>> handler) throws Exception;
    String getFeederSource();
}
```
`Feeder.feeds: Map<String, Feed>` (`Feeder.java:101`), rempli au démarrage :
```java
feeds.put("AAF",   new AafFeeder(vertx, getFilesDirectory("AAF")));
feeds.put("AAF1D", new Aaf1dFeeder(vertx, getFilesDirectory("AAF1D")));
feeds.put("CSV",   new CsvFeeder(vertx));
feeds.put("PRONOTE", new EDTFeederLauncher(edtUtils, mode));   // seulement si config.edt.pronote-private-key présent
```
Dispatch = simple lookup : `feeds.get(message.body().getString("feeder", config("feeder","AAF")))`. **Ajouter une nouvelle source = implémenter `Feed` (ou `PartialFeed` pour un import partiel/un seul fichier) et l'enregistrer dans cette map.** Il n'y a pas d'enum/factory au-delà de ça.

Note : les imports EDT (webdav)/UDT (webdav)/CSV (cron) ont leurs propres lanceurs cron (`ImporterTask`, `CsvImportsLauncher`, `UDTWebDAVImportsLauncher`) qui, au final, envoient le même message `{"action":"import","feeder":...}` sur `entcore.feeder` — pas de code dupliqué pour le chemin de persistance.

## Le moteur générique (`dictionary/structures/`)

### `Importer` — singleton, un seul import à la fois **sur toute la plateforme**

`Importer.getInstance()` (holder pattern, `Importer.java:101-107`). **Un seul run actif à un instant T, tous feeders confondus** (AAF, AAF1D, CSV, EDT initial, ...) — c'est la contrainte structurante la plus importante à connaître.

Cycle de vie d'un run :
1. **`init(neo4j, vertx, source, acceptLanguage, blockCreateByIne, supportPersEducnat1D2D, checkStudentsRelationships, handler)`** (`Importer.java:109-191`) : `isInUse.set(true)`, nouveau `Report`, nouveau `TransactionHelper(neo4j, source, 1000)`, `GraphData.loadData(...)` (précharge **tout** Structure/Profile/Class/Group/Function depuis Neo4j dans des caches **statiques** partagés — `Importer.structures/profiles/...` ne sont que des alias vers ces mêmes maps). Selon les flags, précharge aussi champs disciplinaires (CSV), liste des INE bloqués, données 1D↔2D, ou un prefetch shardé (16 voies) des structures/classes/modules déjà liés à chaque élève (`checkStudentsRelationships`, pour éviter des écritures redondantes).
2. Le `Feed` spécifique (AAF/CSV/...) parse son format et appelle les méthodes génériques ci-dessous, une fois par structure/utilisateur.
3. **`isReady()`** = `!isInUse.get()` — c'est ce que `Feeder.handle()` regarde pour mettre en file les messages concurrents.
4. **`persist(handler)`** (`:322-340`) = commit de la transaction Neo4j en cours puis **`reinitTransaction()`** immédiat (nouvelle transaction) — checkpoint périodique. `flush(handler)` (`:342-346`) envoie les statements bufferisés sans clore la transaction.
5. **`clear()`** (`:307-316`) : vide `structures/profiles/userImportedExternalId/structuresImportedExternalId/groupClasses`, `report=null`, `transactionHelper=null`, `isInUse.set(false)`. **Effet de bord important : comme `structures`/`profiles` sont les mêmes objets que les caches statiques `GraphData`, `Importer.clear()` vide aussi `GraphData` au passage.**

### Les `createOrUpdateX` — le contrat d'objet générique partagé par TOUTES les sources

| Méthode | Schéma JSON | Notes |
|---|---|---|
| `createOrUpdateStructure(JsonObject)` | `Structure.json` | `Importer.java:357-421` |
| `createOrUpdateProfile(JsonObject)` | `Profile.json` | `:428-450` |
| `createOrUpdateFieldOfStudy(JsonObject)` | `FieldOfStudy.json` | `:452-474` |
| `createOrUpdateModule(JsonObject)` | `Module.json` | `:476-498` |
| `createOrUpdateUser(JsonObject, linkStudent, linkRelativeWithoutChild, linkGroups)` | `User.json` | `:500-575` — malgré le nom générique, c'est le chemin du profil **Relative** |
| `createOrUpdateGuest(JsonObject, linkClasses)` | `User.json` | `:577-640` — profil **Guest** ; **pas de chemin AAF vers cette méthode**, seuls CSV/EDT peuvent créer des Guest |
| `createOrUpdateStudent(JsonObject, profileExternalId, module, fieldOfStudy, linkClasses, linkGroups, relative, nodeQueries, relationshipQueries)` | `Student.json` | `:660-842` |
| `createOrUpdatePersonnel(JsonObject, profileExternalId, structuresByFunctions, linkClasses, linkGroups, nodeQueries, relationshipQueries)` | `Personnel.json` | `:654-658`, délègue à `PersEducNat.createOrUpdatePersonnel` |

**N'importe quelle source (AAF XML, AAF1D XML, CSV, EDT) doit converger vers un `JsonObject` avec exactement ces noms de champs logiques** (`externalId`, `structures`, `classes`, `groups`, `relative`, `functions`, ...), indépendamment des noms d'attributs source. C'est le point d'extension le plus important : pour supporter un nouveau format, il "suffit" d'écrire le code qui construit ce `JsonObject`, tout le reste (validation, Neo4j, dédoublonnage, rapport) est déjà là.

Chaque `createOrUpdateX` : valide via un `Validator` construit depuis `dictionary/schema/*.json` (sections `validate`/`generate`/`required`/`modifiable`) → si erreur, `report.addIgnored(profile, error, object)` et **l'enregistrement entier est abandonné, silencieusement** (pas d'exception) → sinon `MERGE (n {externalId}) ON CREATE SET ... WITH n WHERE n.checksum IS NULL OR n.checksum <> {checksum} SET ...` (un checksum SHA-256/MD5 sur les champs validés fait qu'un enregistrement inchangé ne déclenche aucune écriture Neo4j).

### Cache de structures en mémoire — `ImporterStructure` / `ImporterCachedStructure`

- `ImporterStructure extends Structure` : route juste `getTransaction()` vers la transaction *courante* de `Importer.getInstance()` (utile car `reinitTransaction()` change de transaction en cours de run sans invalider les objets déjà en cache).
- `ImporterCachedStructure` : ajoute des `Set` synchronisés (`classes`, `groups`, `classesGroups`) et rend `createClassIfAbsent`/`createFunctionalGroupIfAbsent`/`createFunctionGroupIfAbsent`/`createHeadTeacherGroupIfAbsent`/`createDirectionGroupIfAbsent` **idempotents en JVM** : le `MERGE` Neo4j n'est émis que la première fois qu'un externalId de classe/groupe est vu, ensuite c'est un simple `Set.contains`. **C'est le mécanisme qui permet à AAF/CSV de "créer" des classes/groupes juste en écrivant `structureExternalId$nom` dans un champ élève/enseignant, sans jamais déclarer ces objets explicitement.**

### `GraphData` — cache statique, à ne pas confondre avec `Importer.isReady()`

`GraphData` (`structures`, `profiles`, `structuresByUAI`, `externalIdMapping` — 4 `ConcurrentMap` **statiques**) :
- `loadData(neo4j, handler)` : 2 requêtes Cypher (structures+groupes+classes ; profils+fonctions), peuple les caches.
- `isReady()` = `structures.isEmpty() && profiles.isEmpty()` (donc `true` = cache **vide**, pas "prêt à traiter" au sens intuitif).
- `clear()` vide les 4 maps.

C'est le verrou utilisé par `"transition"` (`Feeder.launchTransition`, `Feeder.java:767-805`) : une transition ne démarre que si `GraphData.isReady()==true` (sinon remise en file), et son callback appelle systématiquement `GraphData.clear()` puis `checkEventQueue()`. Un import normal (`Importer.init`) appelle aussi `GraphData.loadData`, et `Importer.clear()` vide les mêmes maps par ricochet — **une transition et un import ne doivent jamais se chevaucher**, c'est ce double mécanisme (`Importer.isReady()` + `GraphData.isReady()`) qui l'empêche.

### Transactions Neo4j — batching à 1000, pas de config

Pas de "grande transaction unique" ni de taille configurable : **1000 statements en dur**, partout :
- `Importer.init()` (`:116`) et `Importer.reinitTransaction()` (`:352`) : `new TransactionHelper(neo4j, source, 1000)`.
- `TransactionHelper.add(Statement)` bufferise ; tous les 1000 statements, flush vers Neo4j (`executeTransaction(..., commit=false)`) **dans la transaction serveur déjà ouverte** (ouverte paresseusement au premier envoi).
- `commit(handler)` = flush final + `commit=true` (ferme la transaction). `flush(handler)` = flush avec `commit=false` (transaction reste ouverte).
- `Importer.persist()` = `commit()` + `reinitTransaction()` immédiat → c'est le mécanisme de checkpoint périodique (ex. `CsvFeeder` l'appelle une fois par structure ; `Importer.removeEmptyClasses(batchSize)` chaîne des `commit()` par lot pour éviter un OOM Neo4j sur un run incrémental massif).
- `TransactionManager` (autre singleton) gère un **second** jeu de transactions nommées ad hoc (`ConcurrentMap<String, TransactionHelper>`, ex. `"GraphDataUpdate"` pour `Transition`), indépendant de la transaction de run d'`Importer`, batché aussi à 1000.

### `Report` (`utils/Report.java`)

Un `Report` par run, `JsonObject result` fixe :
- `errors` (clé `"error.<fichier>"`) → bloquant, vérifié par `containsErrors()` (gate avant tout `doImport`/persistance réelle).
- `softErrors` → non-bloquant, surtout utilisé par la validation CSV ligne à ligne.
- `ignored` → `{reason, object}`, alimenté par chaque `createOrUpdateX` quand la validation schema échoue.
- `files`, `profiles`, `mappings`, `usersExternalIds`.
- Persisté en Mongo (collection `imports`) sauf flag `not-persist-report`. Renvoyé sur le bus comme reply de l'action déclenchante. Peut déclencher un email (config `sendReport`, calcule un diff créés/modifiés/disparus sur la fenêtre de temps du run, template `email/report.html`).

### Doublons — `DuplicateUsers`

- Structure Neo4j : `(u1:User)-[:DUPLICATE {score}]-(u2:User)`.
- Détection (`markDuplicates`) : pour chaque profil, users modifiés depuis le dernier run (timestamp sur `(:System {name:'Starter'}).lastSearchDuplicates`), recherche floue Lucene (`~0.6`) sur nom/prénom via `node_auto_index`, score = base 2 + 1 par match exact (nom, prénom, date naissance, email). Retenu si `score > 3` et (un des deux a disparu OU sources "compatibles" — XOR contre `notDeduplicateSource=["AAF","AAF1D"]`, deux comptes de la même fédération ne se matchent pas entre eux par défaut) et aucun `ignoreDuplicates` mutuel et au moins un côté non activé.
- `mergeDuplicate` : survivant choisi par statut d'activation + priorité de source (`AAF < AAF1D < CSV < EDT < UDT < SSO < MANUAL` par défaut, ordre configurable), peut préserver certaines relations du perdant (`RelationshipsToKeepPerUser`).
- `mergeBykeys`/`unmergeByLogins` : mécanisme **séparé**, fusion de comptes Relative partageant une `mergeKey` manuelle.
- `autoMergeDuplicatesInStructure` : auto-fusion seulement si `score >= 5` (+ éventuellement restreint à la même structure).
- `mergeSameINE` : fusionne les élèves partageant le même INE (gated par `execute-merge-ine`).

### Cycle disparition → pré-suppression → suppression

1. **Fin de chaque import** : `Importer.markMissingUsers(...)` calcule `usersEnBaseDeCetteSource − usersVusDansCeRun` → `SET u.disappearanceDate=now` sur les manquants (si pas déjà mis) ; ceux qui réapparaissent → `REMOVE u.disappearanceDate` (+ re-check des doublons).
2. **Cron `pre-delete-cron`** (défaut `0 0 3 * * ? *`, délai `pre-delete-user-delay`, ~90j) : `User.PreDeleteTask` sauvegarde les relations (`Backup` node) puis `preDelete()` → `u.deleteDate=now`, supprime la plupart des relations (`IN|COMMUNIQUE|...|RELATED|DUPLICATE|TEACHES_FOS|TEACHES|HAS_FUNCTION`), rattache l'utilisateur à `:DeleteGroup`. **C'est le moment où l'utilisateur perd effectivement ses accès/groupes**, le nœud existe toujours.
3. **Cron `delete-cron`** (défaut `0 0 2 * * ? *`, délai `delete-user-delay`) : `User.DeleteTask` supprime réellement (par lots de 1000, pagés) les users de `:DeleteGroup` dont `deleteDate` est assez ancien.

`manual-restore-user` / `restorePreDeletedUsers` peuvent annuler les étapes 1-2 tant que la suppression finale n'a pas eu lieu.

## Les formats source

### Comparatif rapide

| | AAF (2D) | AAF1D (1D) | CSV | EDT/Pronote | UDT |
|---|---|---|---|---|---|
| Feed key | `AAF` | `AAF1D` | `CSV` | `PRONOTE` (import initial) | — (pas dans `feeds`) |
| Fichiers attendus | `*EtabEducNat_NNNN.xml`, `*_Eleve_NNNN.xml`, `*PersRelEleve_NNNN.xml`, `*PersEducNat_NNNN.xml`, `*MefEducNat_NNNN.xml`, `*MatiereEducNat_NNNN.xml` | mêmes noms de fichiers que 2D (regex réutilisées telles quelles) | `Structure/{Teacher,Personnel,Student,Relative,Guest}.csv`, `;` délimité | export Pronote chiffré (RSA+AES+zlib) | zip de `UDCal_00..24.xml` + `semaines.xml` |
| Dossier mapping | `dictionary/mapping/aaf/*.json` | `dictionary/mapping/aaf1d/*.json` | **aucun** — mapping en dur dans `ProfileColumnsMapper.java` | `dictionary/mapping/edt/PersEducNat.json` (sync incrémentale seulement) | aucun |
| Profils créés | Student, Teacher, Personnel, Relative (**pas de Guest**) | idem AAF | Student, Teacher, Personnel, Relative, **Guest** | Student, Teacher, Personnel, Relative | — |
| Classes/groupes | valeur `structureExternalId$nom` sur l'élève/enseignant (2 champs `$`) | même principe mais **5 champs `$`**, clé = préfixe académie partagé (pas par structure) | même convention `structureExternalId$nom` que AAF | — | — |
| Champ disciplinaire/MEF | oui (`MefEducNat`/`MatiereEducNat`, `ENTAuxEnsClassesMatieres` etc.) | **absent, aucun équivalent** | non | non | non |
| Moteur générique `Importer` | oui | oui | oui | oui **seulement pour l'import initial** (`EDTFeeder`) | **non** |
| Où sont les cours | — | — | — | Mongo `courses` (import incrémental `EDTImporter`) | Mongo `courses` (`UDTImporter`) |

### AAF (2nd degré) — voir aussi le rapport détaillé antérieur sur ce module si disponible

Pipeline (fichiers découverts par regex sur nom de fichier, **pas** en lisant `categoriePersonne`) :
```
StructureImportProcessing (EtabEducNat)
 → FieldOfStudyImportProcessing (MatiereEducNat)
 → ModuleImportProcessing (MefEducNat)
 → StudentImportProcessing (Eleve, passe 1 : nœud User seul)
 → UserImportProcessing (PersRelEleve — ne crée un Relative QUE s'il est référencé par un élève)
 → PersonnelImportProcessing (PersEducNat, passe 1)
 → PersonnelImportProcessing2 (relations)
 → StudentImportProcessing2 (relations)
 → CleanImportProcessing → CleanImportProcessingGlobal
```
Points clés à retenir (déjà documentés en détail ailleurs) :
- Classes/groupes n'existent **jamais** comme entité XML propre : ils naissent de `ENTEleveClasses`/`ENTAuxEnsClassesMatieres` etc., format `<structureExternalId>$<nom>[$<fieldOfStudy>]`.
- `ENTElevePersRelEleve` : format `<relativeExternalId>$?$?$<qualité>$?$?` (6 champs `$`), seul le champ d'indice 3 (`"1"` = autorité parentale, toujours gardé ; `"2"` = personne en charge, gardé seulement si `import-person-in-charge=true`) et le premier champ sont lus.
- `ENTPersonFonctions` : `<structureExternalId>$<code>$<nom>$<positionCode>$<positionNom>`, 5 champs stricts.
- Encodage forcé en UTF-8 à la lecture (`InputSource.setEncoding("UTF-8")`), même si le prologue XML dit `ISO-8859-15`.

### AAF1D (1er degré / primaire)

- Réutilise **exactement les mêmes regex de nom de fichier** que AAF 2D (`EtabEducNat`, `_Eleve_`, `PersRelEleve`, `PersEducNat`) — seul le dossier `dictionary/mapping/aaf1d/*.json` change l'interprétation des champs.
- Aucune étape MEF/matière (pas de `FieldOfStudyImportProcessing`/`ModuleImportProcessing` en 1D).
- Encodage classes/groupes/fonctions élève : **5 champs `$`**, clé construite avec un préfixe académie partagé — différent du 2D (2 champs, clé par structure). Attention si on écrit un générateur de test commun aux deux.
- `parseRelativeField1d` ne filtre pas par code d'autorité parentale (contrairement au 2D) — tout relative référencé est gardé.
- Support "1D/2D" : si `support-perseducnat-1d-2d=true` et source `AAF1D`, `PersonnelImportProcessing1d.process()` fusionne les `functions` d'un personnel 2D déjà en base (même externalId) — modélise un enseignant intervenant en primaire et en secondaire.
- Détection profil enseignant : simple `contains("ENS")`/`contains("DOC")` sur les fonctions (plus permissif que le 2D qui cherche `"$ENS$"`/`"$DOC$"`).
- **Dead code confirmé** : `CleanImportProcessingGlobal1d` n'est jamais instancié ; le pipeline 1D retombe sur `aaf.CleanImportProcessingGlobal` (2D) en fin de chaîne via `CleanImportProcessing.start()` non surchargé.
- Champs 1D-only sans équivalent 2D : `ENTEcoleRPI`, `ENTEcoleCirconscription`, `ENTEcoleSecteur`, `ENTEcoleCommune` (structure), `ENTEleveRestaurantScolaire`, `ENTEleveGarderieMatin/Soir`, `ENTEleveEtudesSurveillees`, `ENTEleveTypeClasse` (élève) — vocabulaire spécifique au primaire (cantine, garderie, études surveillées, RPI = regroupement pédagogique intercommunal).

### CSV

- **Pas de dossier `dictionary/mapping/csv/`** — le mapping colonne→champ est codé en dur dans `csv/ProfileColumnsMapper.java` (`defaultInit()`), avec normalisation des en-têtes (minuscule, accents/espaces/apostrophes/BOM retirés). Un mapping personnalisé (assistant d'import) peut le surcharger.
- Un fichier par profil, nommage strict : `Teacher.csv`, `Personnel.csv`, `Student.csv`, `Relative.csv`, `Guest.csv` (sensible à la casse), sous `<nomStructure>[@externalId][_UAI][_overrideClass]/`. Délimiteur `;`. Détection d'encodage automatique (BOM UTF-8, sinon `juniversalchardet`, défaut ISO-8859-1).
- Contrairement à AAF, **CSV peut créer des comptes Guest**.
- `classes`/`groups` : même convention `structureExternalId$nom` que AAF, créées à la volée via `createClassIfAbsent`/`createFunctionalGroupIfAbsent`.
- Deux passes distinctes :
  - **Validation** (`CsvValidator`, dry-run, aucune écriture Neo4j hors lecture) — utilisée par l'assistant (`/wizard/validate`) mais **aussi systématiquement ré-exécutée avant tout commit réel** (`Feeder.validateAndImport()` valide toujours avant d'importer, même pour l'action `"import"` toute seule).
  - **Import** (`CsvFeeder`, écrit réellement via `Importer`).
- Erreurs : accumulate-and-continue. Une ligne invalide → `report.addIgnored(...)`/`addErrorByFile(...)`, la ligne suivante continue d'être traitée. Seule une erreur de **fichier** (I/O, parse cassé) interrompt le reste du batch.
- Endpoints HTTP (module `directory`, `ImportController.java`) : `/wizard`, `/wizard/column/mapping`, `/wizard/classes/mapping`, `/wizard/validate[/:id]`, `/wizard/import[/:id]`, `/wizard/update/:id/:profile[/:line]`, `/import/:userType/class/:classId`.

### Export (Éliot)

Sens inverse : relit le graphe Neo4j et régénère des fichiers XML au même format `ficAlimMENESR` que l'AAF entrant, à destination du système ministériel Éliot.

Chaîne (`export/eliot/`) : `EliotExporter → EleveExportProcessing (Student) → PersRelEleveExportProcessing (Relative) → PersEducNatExportProcessing (Personnel/Teacher) → PersHorsAAFExportProcessing (Guest — "personnes hors AAF") → EtabEducNatExportProcessing (Structure) → PorteurENTExportProcessing (Tenant) → PersALExportProcessing (ADMIN_LOCAL)`.

- Chaque étape utilise un dictionnaire `dictionary/export/eliot/*.json` (même esprit que les mapping AAF, mais en sens inverse : champ graphe → attribut AAF de sortie) et `AAFUtil.convert(value, converter)`.
- `AAFUtil.siecleConverter` (import ⇄ export du champ `relative`/quality) existe mais **n'est référencé par aucun mapping d'export actuellement livré** — code atteignable mais non câblé.
- Déclenché par action `"export"` sur `entcore.feeder`, lui-même déclenché par `POST /export` (module `directory`) ou automatiquement après un import cron si `auto-export=true` (délai `auto-export-delay`, défaut 30 min).
- N'est actif que si `config.exporter == "ELIOT"`.

### Timetable (EDT/Pronote, UDT)

Écrit les cours dans **MongoDB** (collection `courses`), pas seulement Neo4j.
- **Import initial d'une structure** depuis Pronote (`EDTFeederLauncher`+`EDTFeeder`, feed key `PRONOTE`) : passe par le moteur générique `Importer` comme AAF/CSV, car il s'agit de créer Structure/User/Class/Group.
- **Synchronisation incrémentale** (`EDTImporter`, `UDTImporter`, tous deux `extends AbstractTimetableImporter`) : pipeline **séparé**, n'implémente pas `Feed`, écrit du Cypher à la main via `TransactionHelper`/`TransactionManager` et fait des upserts Mongo pour les cours — ne passe pas par `Importer`. Réutilise quand même `Importer.markMissingUsers`/`restorePreDeletedUsers` et le mapper `PersEducNat` pour réconcilier les attributs enseignant/personnel (`dictionary/mapping/edt/PersEducNat.json`).
- Rapport dédié `TimetableReport` (gabarit `template/timetable-report.txt`, Mongo `timetableImports`, fichier original attaché via `Storage`) — différent du `utils/Report` générique.
- Déclencheurs HTTP (module `directory`, `TimetableController.java`) : `PUT /timetable/init/:structureId`, `POST /timetable/import/:structureId` (et variantes), `POST /timetable/feeder/pronote/:structureId`.
- UDT en plus : `UDTWebDAVImportsLauncher`, cron qui scanne un dossier WebDAV et pousse les zips trouvés.

## Configuration — clés utiles

Fichier statique unique : `src/main/resources/mod.json` (main/worker/address/neo4j-address/import-files/feeder par défaut = `BE1D`). Tout le reste vient du JSON de config passé au déploiement du verticle.

| Clé | Défaut | Effet |
|---|---|---|
| `address` | `entcore.feeder` | Adresse bus consommée |
| `feeder` | `AAF` | Source par défaut si le message n'a pas de champ `"feeder"` |
| `import-files` | — | Racine des fichiers AAF/AAF1D sur disque |
| `imports` | — | Bloc par type : `{cron, files, auto-export}` |
| `import-cron` | — | Cron global si `imports` absent |
| `auto-export` / `auto-export-delay` | `false` / `1800000` | Export auto après import |
| `allow-manual-actions-during-feeds` | `false` | Autorise les `manual-*` pendant un import en cours |
| `delete-cron` / `pre-delete-cron` | `0 0 2 * * ? *` / `0 0 3 * * ? *` | Cron suppression / pré-suppression |
| `delete-user-delay` / `pre-delete-user-delay` | ~90j | Délai avant l'étape suivante |
| `delete-delay` | `240` | Pacing entre lots de 1000 lors du hard-delete |
| `pre-delete` | — | Override par profil `{Personnel:{cron,delay}, ...}` |
| `timetable-report-erase-cron` / `-after-seconds` | `0 0 4 * * ? *` | Rétention des rapports timetable |
| `reinit-login-cron` | — | Resync du cache d'unicité de login |
| `ad-login-alias-validator` | `false` | Regex de login-alias plus stricte (compat AD) |
| `exporter` | — | `"ELIOT"` active `EliotExporter` |
| `export-path` / `export-destination` / `concat-export` / `delete-export` | — | Config Éliot |
| `edt` | — | `{pronote-private-key, pronote-partner-name, mode}` — active le feed `PRONOTE` |
| `udt.webdav` | — | `{path, cron, from, to}` |
| `csv` | — | `{path, cron, config}` |
| `csvMappings` | — | Mapping colonnes par défaut passé à `CsvValidator` |
| `block-create-by-ine` | `false` | Bloque la création d'élève si INE déjà utilisé ailleurs |
| `support-perseducnat-1d-2d` | `false` | Active la fusion fonctions 1D/2D |
| `check-exists-relationships` | `false` | Active le prefetch "skip si inchangé" |
| `timetable` / `autoMergeOnlyInSameStructure` | `true`/`true` | Args constructeur `DuplicateUsers` |
| `execute-merge-ine` | `false` | Active `mergeSameINE` en post-import |
| `sendReport` | — | `[{to,cc,bcc,project,sources[],academyPrefix}]` — emails de rapport |
| `notify-apps-after-import` | `true` | Déclenche `ApplicationUtils.afterImport` |
| `import-person-in-charge` (AAF) | — | Autorise le champ qualité `"2"` dans `ENTElevePersRelEleve` |
| `validation-file-path` (AAF) | — | Fichier de référence externe consulté pendant l'import |

## Exceptions

- `TransactionException` — transaction nommée déjà utilisée (`TransactionManager.begin`). Toujours convertie en reply d'erreur bus ou loguée, jamais de code HTTP (il n'y a pas d'HTTP ici).
- `ValidationException` — entrée invalide hors du mécanisme générique schema/`Validator` (mapping CSV inconnu, données de groupe manuel invalides, clé/hash EDT invalide, "no validator available" dans `ValidatorFactory`).

## Pièges connus / choses à vérifier avant de toucher au code

- **Un seul import actif à la fois, tous feeders confondus** (`Importer` singleton) — un test qui lance deux imports "en parallèle" va en fait sérialiser via la file d'attente.
- `GraphData` et `Importer` partagent les mêmes maps statiques `structures`/`profiles` : vider l'un vide l'autre. Ne pas supposer qu'ils sont indépendants.
- Aucune route HTTP ici : si vous cherchez "où est appelé tel comportement depuis le front", regardez dans `entcore/directory/.../controllers/`, pas dans ce module.
- Les classes/groupes n'ont jamais d'entité XML dédiée en AAF/AAF1D/CSV — ne cherchez pas un fichier "Classe.xml", ils naissent des champs `$`-encodés des élèves/enseignants.
- Format `$`-encodé **différent entre AAF (2 champs) et AAF1D (5 champs)** pour les classes/groupes élève — un générateur de test ou un outil commun doit distinguer les deux.
- `CleanImportProcessingGlobal1d` est du code mort (jamais instancié) — ne pas supposer qu'il tourne réellement en fin de pipeline 1D.
- Le `Report` généré côté validation CSV n'empêche pas nécessairement l'import : c'est `report.containsErrors()` (erreurs dures) qui bloque, pas `softErrors`.
- `AAFUtil.siecleConverter` existe mais n'est câblé dans aucun mapping d'export livré (`"converter":"siecle"` n'apparaît dans aucun `dictionary/export/eliot/*.json` actuel) — ne pas supposer qu'il s'exécute en pratique.
- Le taux de batching Neo4j (1000 statements) est codé en dur à plusieurs endroits, pas une seule constante centrale — si vous le changez, grep `new TransactionHelper(` pour trouver tous les sites.

## Index "je veux modifier X"

| Je veux... | Fichiers à ouvrir |
|---|---|
| Ajouter/modifier un attribut AAF (2D) | `dictionary/mapping/aaf/*.json` + la classe `aaf/*ImportProcessing*.java` correspondante |
| Ajouter/modifier un attribut AAF1D | `dictionary/mapping/aaf1d/*.json` + `aaf1d/*.java` |
| Ajouter une colonne CSV reconnue | `csv/ProfileColumnsMapper.java` (`defaultInit`) |
| Changer une règle de validation (champ requis, regex) | `dictionary/schema/{Structure,Student,Personnel,User,Module,FieldOfStudy}.json` + `utils/Validator.java` (patterns) |
| Comprendre/modifier le scoring de doublons | `dictionary/structures/DuplicateUsers.java` |
| Changer la politique de suppression/rétention | `dictionary/users/User.java` (`PreDeleteTask`/`DeleteTask`) + clés config `*-delete-*` |
| Ajouter une nouvelle source d'import | implémenter `Feed` (ou `PartialFeed`), l'enregistrer dans `Feeder.feeds` |
| Modifier l'export Éliot | `export/eliot/*.java` + `dictionary/export/eliot/*.json` |
| Modifier l'import de classes de temps (EDT/UDT) | `timetable/edt/*` ou `timetable/udt/*`, selon le format ; `AbstractTimetableImporter` pour la logique incrémentale partagée |
| Ajouter une action event-bus | `Feeder.handleAction()` (`Feeder.java`, gros `switch`) |
