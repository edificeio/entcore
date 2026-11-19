---
name: referential-acl-module
description: >
  Passer un module entcore (Vert.x) en ACL « referential » : ses lectures du référentiel (bus directory)
  sont rejouées en arrière-plan sur le service referential (Quarkus/PostgreSQL) et comparées à la réponse
  Neo4j, écarts journalisés sans valeur. Utiliser dès que l'utilisateur demande de « migrer un module vers
  referential », de « brancher l'ACL » ou la « couche de comparaison » sur un module, de choisir ou d'outiller
  un module pilote du POC referential, ou d'implémenter dans referential l'équivalent d'une action du bus
  directory. Ne pas utiliser pour les imports (feeder) ni pour la resynchro Neo4j → PG.
---

# Passer un module en ACL referential

Référence vivante : `~/project/sources/referential/docs/poc-migration-feeder.md` (étapes 5 et 6, décisions D1–D8,
arbitrages A1–A15, Q7–Q9). Premier module passé : **conversation** (commit `833fbdbcb`, classes
`conversation/backend/.../util/{ReferentialAcl,AclComparator}.java`, scénario
`tests/src/test/js/it/scenarios/conversation/referential-acl.ts`). La lire avant de commencer.

Principe : **Neo4j fait foi**. Le module sert toujours la réponse de directory ; un échantillon de ses lectures
est rejoué sur referential **après** la réponse, et les écarts sont journalisés. Aucune donnée personnelle dans
les logs ni dans ce qui est affiché à l'utilisateur pendant le travail (noms de champs, comptes, ids techniques
seulement).

## 1. Inventorier les lectures du module

1. Lister comment le module lit le référentiel :
   - requêtes au bus `directory` / `entcore.directory` (`"action"` envoyée, classe qui envoie) ;
   - Cypher en direct dans le module (labels User, Structure, Class, Group…) ;
   - helpers de `edifice-entcore-libs/common` (`UserUtils.findVisible*` passe par le module communication).
2. Le module convient si ses lectures passent par **un seul point d'accès** (une classe, quelques appels au bus),
   s'il n'écrit pas le référentiel, ne lit ni doublons ni visibilité (non modélisés), et s'il est **utilisé en
   recette et en préprod** (sinon la mesure D7 n'a rien à comparer : cas a été écarté pour cette raison). Mesurer le
   trafic par les données du module (lignes de ses tables, ex. `audience.reactions`) ou les accès dans les logs ;
   une recette locale vide n'écarte pas un module : le scénario k6 crée le trafic.
3. Ne décorer que ces appels au bus. Le Cypher direct et la visibilité restent hors ACL : les noter.

## 2. Spécifier chaque lecture

Pour chaque action : trouver le handler. Deux adresses existent : `directory` (`DirectoryController`,
`@BusAddress("directory")`, un `case` par action) et `entcore.directory` (consumer brut dans `Directory.java`,
ex. `get-users-displayNames`). Puis le service et la requête Cypher. Noter :
- paramètres du message (et leurs défauts : `fieldMask`, `itself`…) ;
- forme exacte de la réponse (tableau de lignes, ou objet `{id: valeur}`), champs et types ;
- règles cachées : `OPTIONAL MATCH` (champ nul), lignes répétées pour un même id, tri, `DISTINCT`, filtres de label ;
  un handler qui échoue sur une donnée (ex. `Collectors.toMap` sur une valeur nulle) ne répond pas : rien à comparer,
  à noter comme écart prévisible.

Puis l'équivalent PG (schéma `referential`, migrations `src/main/resources/db/migration`) et les **écarts prévisibles** :
- champ absent de PG → le modéliser (migration `V<n>__…sql` + `ResyncMappings` pour que la resynchro et le comparateur
  le couvrent) ou le classer en écart expliqué. **Arbitrage de Vincent** : poser la question, ne pas trancher seul ;
- donnée hors référentiel (préférences `UserAppConf`…) → écart expliqué, non comparé ;
- ids : toujours les ids Neo4j (`neo4j_id`, Q7) ;
- présupprimés (A15) : Neo4j garde le nœud `User` (seul `deleteDate` est posé) mais retire ses liens `IN`,
  `RELATED`, `TEACHES_FOS`, `TEACHES`, `HAS_FUNCTION`. Filtrer `delete_date IS NULL` quand la requête passe par ces
  liens (appartenances, structures via les groupes de profil) ; garder les présupprimés quand elle lit le nœud
  directement (par id).

Consigner la spécification dans la doc du POC (tableau lecture → requête Neo4j → champs → équivalent PG → écarts).

## 3. Servir la lecture dans referential

- Requête SQL dans `repositories/DirectoryReadRepository` qui construit **le JSON exact** de directory
  (`json_build_object`, `json_agg … ORDER BY`, `COALESCE(…, '[]')`), renvoyé tel quel.
- Route `POST /api/directory-bus/<action>` dans `controller/DirectoryBusController` : même nom que l'action, même
  corps que le message du bus (sans `"action"`). Les deux adresses partagent cet espace : vérifier qu'aucune route
  n'existe déjà sous ce nom (sinon suffixer par l'adresse).
- Test `@QuarkusTest` + `@TestTransaction`, jeu SQL inséré pendant la méthode de test (directement ou par un
  `seed()` qu'elle appelle ; pas en `@BeforeEach`, hors transaction), ids préfixés propres au test ; vérifier la forme
  et les cas limites (exclusions, présupprimés, champs nuls). Les tests utilisent le PostgreSQL de la recette
  (`localhost:5432/ong`, rollback par `@TestTransaction`) : conteneurs `postgres` (et `nats`) démarrés.
- Lancer : `export JAVA_HOME=~/.sdkman/candidates/java/25.0.3-tem PATH=$JAVA_HOME/bin:$PATH; mvn test`
  (seul échec connu : `GreetingResourceTest` ; suite complète en quelques minutes, harnais compris).

## 4. Brancher l'ACL dans le module

- Reprendre `ReferentialAcl` et `AclComparator` de conversation dans le package `util/` du module (le créer s'il
  n'existe pas ; Java 8 : le comparateur de referential est en Java 25 et ne s'y charge pas). Adapter :
  - les actions comparées, leur type d'entité et leurs champs (`FIELDS`, `ENTITY_TYPES`) ;
  - la forme de réponse : `AclComparator` attend un tableau de lignes à `id`. Pour une réponse objet `{id: valeur}`,
    `ReferentialAcl.compare` reçoit l'objet et convertit **les deux réponses** (Neo4j et referential) en lignes
    `{id, <champ>}` avant la comparaison (nom du champ à choisir, ex. `displayName`) ; les cas « lignes répétées » et
    « listes » du comparateur deviennent sans objet ;
  - copier la réponse Neo4j (`copy()`) avant de la passer à la comparaison asynchrone : l'appelant peut la modifier ;
  - le champ `pilot` des lignes de log (nom du module).
- `ReferentialAcl.init(vertx, config)` au démarrage du module ; désactivé sans le bloc
  `"referential-acl": {"url": "...", "sample-rate": 0..1}` de la configuration.
- Appeler `ReferentialAcl.compare(message, réponse)` **après** avoir rendu la main (`promise.complete`), uniquement
  sur une réponse réussie.
- Logger `referential.acl.diff` : une ligne JSON par écart `{pilot, endpoint, correlationId, entityType, entityId,
  kind (MISSING_IN_PG | EXTRA_IN_PG | FIELD_MISMATCH), fields}` — jamais de valeur, y compris dans les messages
  d'échec (action et statut HTTP seulement).
- Test unitaire du comparateur (JUnit 4) : réponses identiques, écarts avec ids et noms de champs seulement (vérifier
  qu'aucune valeur n'apparaît), lignes répétées, champ absent = nul.

## 5. Vérifier sur la recette locale  

Prérequis : conteneurs de la recette démarrés ; ADMC `tom.mate` / `password`. Chemin de la recette
1. Sauvegarder `ent-core.json` et **chaque jar remplacé** de `mods/`. Tout remplacement d'un jar de la recette, même
   de même version, **se demande** ; à la fin, remettre les sauvegardes (config et jars).
2. Construire le module (`mvn -o -pl <module> package -DskipTests`, JDK 8), copier le fat jar dans `mods/`, supprimer
   le dossier dézippé de la même version, ajouter `referential-acl` dans la config du module avec l'url vue du
   conteneur `vertx` : passerelle du réseau compose (`docker inspect recette-ode1-vertx-1`, ex. `http://172.18.0.1:8108`),
   puis `docker compose restart vertx`.
3. Lancer referential (après `mvn package`) avec l'API ouverte **en local seulement** :
   `QUARKUS_HTTP_AUTH_PERMISSION_API_POLICY=permit IO_NATS_CLIENT_URL=nats://localhost:4222 java -jar target/quarkus-app/quarkus-run.jar`.
   Base vide ou ancienne : `POST /api/resync` puis attendre `GET /api/locks` vide (≈ 12 min sur la recette complète).
4. Scénario k6 dans `tests/src/test/js/it/scenarios/<module>/` (modèle : `conversation/referential-acl.ts`) :
   - établir les données (`initStructure`…), puis les faire relire par referential
     (`POST /api/resync/changed`, les imports n'étant pas notifiés) ;
   - exercer les lectures du module par ses routes HTTP : identifier la route qui déclenche la lecture et les données
     qu'elle exige. Si elles appartiennent à une autre application (ex. audience lit les réactions sur une ressource
     d'une application qui répond à son contrôle d'accès), créer la ressource par l'API d'une application de la
     recette qui l'utilise ;
   - provoquer un écart par une écriture **directe** dans Neo4j (`http://<hôte>:7474/db/data/transaction/commit`,
     recette locale uniquement), relire, et remettre la valeur à la fin.
   - lancer : `k6 run -e ROOT_URL=http://localhost:8090 -e ADMC_LOGIN=tom.mate -e ADMC_PASSWORD=password -e DEFAULT_PASSWORD=password <scénario>`.
5. Contrôler les logs : `docker compose logs --since 10m vertx | grep referential.acl.diff` ; résumer par
   `endpoint/kind/fields` sans afficher de valeur. Attendu : aucun écart avant l'écriture directe, l'écart provoqué
   après, aucune ligne « not compared ».
6. Latence : même charge (ex. 400 itérations, 4 VU, p95 par route via des seuils taggés `http_req_duration{name:…}`)
   avec `sample-rate` 1.0 puis 0.0, **JVM chaude** (jeter le premier passage après redémarrage). Critère : ±5 %.
7. Remettre la recette dans son état (config sauvegardée, version d'origine, redémarrage), arrêter referential.

## 6. Clore

- Doc du POC : lectures, arbitrages, résultats chiffrés, limites.
- Critère de sortie (D7, hors local) : 10 jours ouvrés sans écart non expliqué en préprod (échantillon 5 %), couvrant
  un import AAF et un import CSV. Prérequis préprod : autorisation de service sur `/api/directory-bus` (pas de session
  ENT en serveur à serveur), referential déployé et resynchronisé.
- Commits sur demande seulement, code et commentaires en anglais, docs en français.

## Pièges rencontrés

- `localhost` dans un conteneur désigne le conteneur : utiliser la passerelle Docker (CLI `edifice` : `172.17.0.1`).
- `/home/wse/aaf` peut contenir un export de production : aucun test ne doit le lire.
- Un message du bus peut être modifié par le traitement (la validation retire des champs) : copier ce qu'on veut
  comparer ou notifier **à l'arrivée**.
- La resynchro de la recette complète est longue (Neo4j local à petit tas) : la lancer une fois, en arrière-plan.
- Valeurs autorisées d'un module (types de réaction, pagination…) : les lire dans sa configuration (`publicConf`) et
  son contrôleur, pas dans les énumérations du code (audience : `REACTION_1`, pages à partir de 1).
- Latence en local : l'ENT, referential et PostgreSQL partagent la machine ; à 100 % d'échantillonnage, un surcoût de
  quelques % (audience : +6 %) peut venir de cette contention. Le noter et prévoir la mesure en préprod au taux réel.
