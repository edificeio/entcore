# Cadrage technique — Identification de l'établissement émetteur

> FS source : [FS-IMPULS-6108-structure-sender.md](./FS-IMPULS-6108-structure-sender.md)
> Contrat d'API US-1 : [CONTRAT-API-IMPULS-6113-US1.md](./CONTRAT-API-IMPULS-6113-US1.md)
> Repo : entcore
> Date : 27/07/2026 (mise à jour 28/07/2026 : nommage du champ, contrat US-1 figé)
> Participants : Squad Impulsion (dev front, dev back)
> Statut : Draft
> Tickets : EPIC [IMPULS-6108](https://edifice-community.atlassian.net/browse/IMPULS-6108) — US-1 [IMPULS-6113](https://edifice-community.atlassian.net/browse/IMPULS-6113), US-2 [IMPULS-6114](https://edifice-community.atlassian.net/browse/IMPULS-6114)

---

## Contexte / Objectif

Un destinataire de message ne peut pas savoir à quel établissement est rattaché l'émetteur sans ouvrir sa fiche annuaire — friction notable pour les agents institutionnels (collectivité, académie) qui reçoivent des messages d'usagers d'établissements variés, et réciproquement. Ce chantier ajoute l'affichage systématique et discret (libellé gris) de l'établissement de l'émetteur dans deux surfaces du module `conversation` : la liste des messages (US-1) et la vue détail (US-2). Lorsque l'émetteur est rattaché à plusieurs établissements, une règle de résolution (établissement commun avec le destinataire → départage par préféré → sinon préféré → sinon fallback alphabétique) détermine l'établissement unique affiché, avec cohérence stricte entre les deux surfaces. La résolution doit être effectuée côté back : le destinataire n'a pas accès, via l'API existante, aux préférences d'un autre utilisateur.

---

## Schéma d'ensemble

```text
Destinataire (session UserInfos)
        │
        ▼
GET api/folders/:folderId/messages   (US-1)         GET api/messages/:id   (US-2)
        │                                                     │
        ▼                                                     ▼
ConversationService.listAndFormat            ConversationService.getAndFormat
        │                                                     │
        └──────────────► MessageUtil (userIndex : émetteurs de la page + destinataire) ◄──────────────┘
                                        │
                                        ▼
                 Action eventbus "directory" : list-users-structures
                 - structures d'appartenance (batch, Cypher)
                 - établissement préféré (parse UserAppConf.widgets → school-widget.schoolId)
                                        │
                                        ▼
                 Résolution (dans conversation) : commun → préféré parmi communs → préféré → fallback alphabétique
                                        │
                                        ▼
                 from.displayStructure = { id, name }  (additif, injecté dans le payload existant)
                                        │
                                        ▼
                 Front : MessagePreview.tsx (liste, ellipsis+tooltip) / MessageHeader.tsx (détail, wrap)
```

---

## Risques / Points d'attention

- **Format du blob `UserAppConf.widgets` non typé formellement** : le parsing JSON de `school-widget.schoolId` doit être défensif (try/catch, absence si échec) pour ne pas faire échouer toute la résolution si la structure du JSON évolue ailleurs. *Mitigation : tests unitaires ciblés sur le parsing (préféré présent, absent, JSON malformé) ; dégradation silencieuse actée (cf. ci-dessous).*
- **Cohérence stricte liste/détail** (US-1 et US-2 exigent la même règle de résolution pour un même message) : la logique doit être centralisée dans une fonction/service unique, partagée entre `listAndFormat` et `getAndFormat`, jamais dupliquée. *Mitigation : test d'intégration Vert.x dédié dans `conversation/backend`.*
- **Dégradation en cas d'échec de résolution** (décidé) : si la résolution échoue pour un ou plusieurs émetteurs, le message concerné s'affiche quand même sans libellé d'établissement — pas d'échec de la requête globale.
- **Pas de feature flag** (décidé) : toute anomalie en production nécessite un revert + redeploy classique. Risque accepté, atténué par la dégradation silencieuse ci-dessus.
- **Fallback alphabétique** (décidé) : peut sembler arbitraire à l'utilisateur si ce n'est pas "l'établissement attendu" en l'absence de préféré défini — compromis pragmatique assumé et documenté dans la FS.
- **Perf** : `LIST_LIMIT = 25` messages/page (`ConversationService.java:41`) → au plus 25 émetteurs distincts + le destinataire par batch. Le design retenu (nouvelle action eventbus batchée, destinataire inclus dans le même appel que les émetteurs) élimine le risque de N+1 identifié par la FS.

## Limitations / Hors-scope

- Pas de reprise de l'information d'établissement dans les notifications.
- Pas de fonction de tri ou de filtre de la liste des messages par établissement.
- Le champ `displayStructure.id` (objet imbriqué retenu au contrat) n'est exploité pour aucune navigation/clic dans cette FS — disponible pour un usage futur uniquement, non spécifié ni testé ici.
- Mobile : hors-scope pour l'affichage (non demandé par la FS). L'ajout étant additif au payload existant, aucun risque de casse pour les clients mobiles actuels.

## Métriques produit

Aucune métrique chiffrée fournie dans la FS/PRD au moment du cadrage. `To be decided later` : suivi éventuel du volume de sollicitations support liées à l'identification de l'émetteur (mentionné en §2 de la FS comme symptôme), à cadrer avec le PO si un suivi quantitatif est souhaité.

---

## Spécifications techniques

### Back

- **API existantes réutilisées (inchangées dans leur contrat actuel)** :
  - `GET api/folders/:folderId/messages` — `ApiController.listFolderMessages` (`conversation/backend/src/main/java/org/entcore/conversation/controllers/ApiController.java:64`) → `ConversationService.listAndFormat()`.
  - `GET api/messages/:id` — `ApiController.getFullMessage` (même fichier, ligne 98) → `ConversationService.getAndFormat()`.
- **Enrichissement additif décidé** : ajout d'un champ `displayStructure?: { id: string; name: string }` sur l'objet `from` déjà présent dans les deux payloads. Pas de nouvel endpoint dédié.
- **Nommage du champ arrêté en équipe (28/07/2026)** : `displayStructure`, et non `structure`. Le pluriel `structures` / `structureNames` désigne déjà, ailleurs dans la plateforme, l'ensemble des rattachements d'un utilisateur (cf. `conversation/frontend/src/mocks/handlers.ts`) ; un `structure` singulier se lirait comme un attribut intrinsèque de l'émetteur, alors qu'il s'agit d'une valeur **calculée et relative à l'appelant** — deux destinataires peuvent légitimement voir deux établissements différents pour un même émetteur. Le préfixe `display` s'accorde avec `displayName` sur le même objet et signale une donnée d'affichage. Le vocabulaire côté `directory` (`structures`, `preferredStructureId`) n'est **pas** concerné par ce nommage : c'est `conversation` qui calcule la valeur d'affichage à partir de ces données brutes.
- **Pattern réutilisé** : `MessageUtil.computeUsersAndGroupsDisplayNames` + `MessageUtil.loadUsersAndGroupsDetails` (`conversation/backend/.../util/MessageUtil.java`) construisent déjà un `userIndex` de tous les `userId` distincts de la page — **y compris le destinataire** (ligne 80-83) — avant un unique appel eventbus batché. C'est le mécanisme étendu, pas réinventé.
- **Nouvelle action eventbus `directory`** — `list-users-structures` (nom tranché à l'implémentation) : distincte de l'action existante `list-users` (qui reste inchangée — confirmé comme n'ayant aucun autre consommateur que `conversation` dans ce repo, mais son nom actuel serait trompeur si on y greffait cette responsabilité). Prend en entrée la liste des `userId` du batch (émetteurs + destinataire, dédupliqués) et retourne pour chacun :
  - les structures d'appartenance (`(User)-[:IN]->(ProfileGroup)-[:DEPENDS]->(Structure)`, extension de requête directement possible — pattern déjà utilisé côté `groupIds` de `DefaultUserService.list(JsonArray, JsonArray, ...)`, `directory/.../services/impl/DefaultUserService.java:~1250`) ;
  - l'établissement préféré, résolu en lisant `UserAppConf.widgets` (préférence générique, `UserBookController.java:749`, relation `(User)-[:PREFERS]->(UserAppConf)`) et en parsant côté Java la clé `school-widget.schoolId` du JSON sérialisé. Présent dans ~50 % des cas seulement (confirmé par le dev back).
- **Résolution (côté `conversation`, pas `directory`)** : pour chaque message, comparer les structures de l'émetteur à celles du destinataire (déjà dans le même `userIndex`) : établissement commun → si plusieurs communs, départage par préféré de l'émetteur → si aucun commun, préféré de l'émetteur → si pas de préféré défini, premier établissement par ordre alphabétique du nom (fallback décidé).
- **Contrat d'API à générer au format Postman** (Swagger abandonné) : collections des deux endpoints (`api/folders/:folderId/messages`, `api/messages/:id`) mises à jour avec le champ `displayStructure` additif. Cible : collection **ENT** du workspace « Édifice Workspace ! 🔥 », dossier `conversation/api` — les deux requêtes y existent déjà, leurs exemples de réponse `200` étant des placeholders OpenAPI non résolus à remplacer par le payload réel. **Fait pour le endpoint liste (US-1)** : exemple `200` et documentation de la requête à jour, contrat détaillé dans [CONTRAT-API-IMPULS-6113-US1.md](./CONTRAT-API-IMPULS-6113-US1.md). Reste à faire pour le endpoint détail (US-2, IMPULS-6121).
- **Sécurité/permissions** : aucun changement identifié — le nouveau champ s'appuie sur les mêmes filtres d'accès existants (`SystemOrUserFolderFilter`, `MessageUserFilter`), pas de nouvelle donnée sensible exposée (tout utilisateur de la plateforme est nécessairement rattaché à au moins un établissement, cf. FS §3).
- **Procédure de rollback** : pas de nouvelle donnée persistée (résolution calculée à la volée) → rollback par revert de code + redeploy, pas de migration à annuler. Pas de feature flag (décidé).

### Front

- **Modèle** : extension de `User` (`conversation/frontend/src/models/user.ts`) avec `displayStructure?: { id: string; name: string }`. `MessageBase.from` (`models/message.ts`) reste de type `User`.
- **Liste des messages (US-1)** — `features/message-list/components/MessagePreview/MessagePreview.tsx:77` : ajout d'un `<span>` gris (`text-gray-700`, déjà utilisé ligne 98 du même fichier) entre le nom de l'émetteur et l'heure, dans le conteneur `text-truncate flex-fill` existant (ligne 71). Troncature avec ellipsis (`overflow-hidden text-ellipsis whitespace-nowrap`, conforme à la maquette Figma nœud `mail-card`) + tooltip au survol via le composant `Tooltip` de `@edifice.io/react`, **déjà utilisé dans ce module** (`components/MessageActionDropdown/MessageActionDropdown.tsx`, `components/SignatureEditor/.../SignatureEditorToolbar.DropdownMenu.tsx`) — pas de nouvelle dépendance.
- **Vue détail (US-2)** — `features/message/components/MessageHeader.tsx:28-46` : ajout d'un `<span>` gris dans le conteneur `d-flex flex-wrap column-gap-8` **déjà existant** — le retour à la ligne pour un nom d'établissement long (exigé par la FS) est nativement supporté par ce `flex-wrap`, sans changement de structure. Libellé non interactif (pas de lien, conforme à la maquette Figma nœud "Mail 3").
- **Design tokens** : gris `#909090` / `text-gray-700`, 12px Roboto Regular (liste) — conformes aux tokens déjà en usage dans ce module, confirmés par la maquette Figma (fichier `B8KkuSYSpB3SZYnM3MDRJB`, nœuds `7575:103962` liste et `7575:103918` détail).
- **Dégradation** : si `displayStructure` est absent du payload (échec de résolution back), le libellé ne s'affiche simplement pas — pas de state d'erreur dédié.
- **Mocks** : `mocks/handlers/message-handlers.ts` (MSW) à mettre à jour avec le champ `displayStructure`.

### Mobile

Hors-scope pour l'affichage (non demandé par la FS). L'ajout étant additif au payload existant des deux endpoints partagés, aucun risque de casse pour les clients mobiles actuels.

---

## Découpage détaillé

### Étapes & dépendances

1. Socle back partagé : nouvelle action eventbus `directory` (structures + préféré + fallback) — développée une fois, réutilisée par les deux endpoints.
2. Intégration back dans `listAndFormat` (US-1) puis réutilisation dans `getAndFormat` (US-2, dépend de l'étape 1).
3. Contrat d'API (`displayStructure?: {id, name}`) figé dès l'étape 1 — le front peut démarrer son intégration une fois le contrat connu, mais la validation fonctionnelle dépend de la livraison back.
4. Collections Postman mises à jour au fil de chaque tâche back (pas une étape séparée).

### Sous-tâches par US

#### US-1 : Repérer l'établissement de l'émetteur dans la liste des messages *(IMPULS-6113)*

- [x] `[back]` [IMPULS-6116](https://edifice-community.atlassian.net/browse/IMPULS-6116) — Créer la nouvelle action eventbus `directory` `list-users-structures` : requête Cypher batchée par liste de `userId` (structures d'appartenance) + lecture/parsing du blob `UserAppConf.widgets` pour extraire `school-widget.schoolId` — livrable : méthode + action testée unitairement (préféré présent/absent, JSON malformé) — CA : règle de résolution US-1 — **3 SP** — *fait le 28/07/2026 : `UserService.getUsersStructuresWithPreferred` + `DefaultUserService` (une seule requête, `MATCH` structures et `OPTIONAL MATCH` préférence), action câblée dans `DirectoryController.directoryHandler`. 22 tests : 11 unitaires sur le parsing défensif, 11 d'intégration sur la requête contre Neo4j 3.1. Le fallback alphabétique n'est pas ici mais côté `conversation` (6117), conformément au découpage.*
- [x] `[back]` [IMPULS-6117](https://edifice-community.atlassian.net/browse/IMPULS-6117) — Intégrer l'appel dans `MessageUtil`/`SqlConversationService.listAndFormat` (réutilise le `userIndex` incluant déjà le destinataire), appliquer la règle commun → préféré → fallback, peupler `from.displayStructure`, dégradation silencieuse si échec — livrable : payload `GET api/folders/:folderId/messages` enrichi — CA : tous les scénarios Gherkin US-1 — **3 SP** — *fait le 28/07/2026 (commit `278d61a90`) : `MessageUtil.loadUsersStructures` + `applySenderDisplayStructure` + `resolveDisplayStructure`, câblés dans `listAndFormat`. 22 tests unitaires. À noter : `formatRecipients` renvoie désormais une **copie** de l'émetteur — l'objet indexé étant partagé avec les listes `to`/`cc`/`cci`, l'enrichir en place décorait le même utilisateur vu comme destinataire ailleurs.*
- [x] `[back]` [IMPULS-6118](https://edifice-community.atlassian.net/browse/IMPULS-6118) — Générer/mettre à jour la collection Postman du endpoint liste avec le champ `displayStructure` — livrable : collection Postman à jour — **1 SP** — *fait le 28/07/2026 : exemple `200` (4 cas, dont deux sans le champ pour couvrir la dégradation) et documentation complète de la requête dans la collection ENT ; contrat figé dans [CONTRAT-API-IMPULS-6113-US1.md](./CONTRAT-API-IMPULS-6113-US1.md), ce qui débloque 6117 et 6119 en parallèle*
- [ ] `[front]` [IMPULS-6119](https://edifice-community.atlassian.net/browse/IMPULS-6119) — Étendre le modèle `User` (`displayStructure?: {id, name}`), afficher le libellé dans `MessagePreview.tsx` (ellipsis + `Tooltip`), mettre à jour les mocks MSW, tests unitaires Vitest — livrable : composant à jour + tests — CA : scénarios "nom trop long", "affichage systématique", "absence de tri/filtre" — **3 SP**

**Sous-total US-1 : 10 SP** (back 7 / front 3)

#### US-2 : Identifier l'établissement de l'émetteur dans la vue détail *(IMPULS-6114)*

- [x] `[back]` [IMPULS-6120](https://edifice-community.atlassian.net/browse/IMPULS-6120) — Réutiliser la fonction de résolution du socle dans `getAndFormat`/`getFullMessage`, peupler `from.displayStructure` du détail, test d'intégration Vert.x de cohérence croisée liste/détail (créé dans `conversation/backend`) — livrable : payload `GET api/messages/:id` enrichi + test d'intégration — CA : cohérence stricte liste/détail — **2 SP** — *fait le 28/07/2026 (commit `8b2a3aad9`) : `getAndFormat` reçoit les deux mêmes appels que `listAndFormat`, aucune logique dupliquée. `DisplayStructureConsistencyTest` (PostgreSQL + bus réels) : l'émetteur préfère l'établissement que le lecteur ne partage pas, donc une surface oubliant la comparaison au destinataire renverrait une autre valeur. Vérifié qu'il échoue si l'appel est retiré côté détail.*
- [x] `[back]` [IMPULS-6121](https://edifice-community.atlassian.net/browse/IMPULS-6121) — Mettre à jour la collection Postman du endpoint détail — livrable : collection Postman à jour — **1 SP** — *fait le 28/07/2026 : exemple `200` réel (19 champs) et documentation complète de la requête « Get the full content of a message. ». Écarts avec la liste documentés : `apiVersion=1` ajoute `content_version`/`folder_id`/`trashed`/`original_format_exists`, et `count`/`response`/`hasAttachment` n'existent pas sur le détail.*
- [ ] `[front]` [IMPULS-6122](https://edifice-community.atlassian.net/browse/IMPULS-6122) — Afficher le libellé dans `MessageHeader.tsx` (conteneur `flex-wrap` existant, pas d'ellipsis, non cliquable), tests unitaires Vitest — livrable : composant à jour + tests — CA : scénarios "retour à la ligne", "non interactif" — **1 SP**

**Sous-total US-2 : 4 SP** (back 3 / front 1)

**Total : 14 SP** (back 10 / front 4) — estimation ajustée par l'équipe (÷2 par rapport à l'estimation brute, pour tenir compte de la vélocité réelle de la squad).

---

## Feature flag

Décidé : **pas de feature flag**. Rollback par revert de code + redeploy en cas d'anomalie en production (pas de donnée persistée à faire évoluer).

## Charge macro (estimation)

| Compétence | Charge (SP) | Commentaire |
| --- | --- | --- |
| Back | 10 | Socle de résolution (nouvelle action eventbus + parsing préférences) + intégration dans les 2 endpoints + Postman |
| Front | 4 | Affichage dans les 2 surfaces (liste : ellipsis+tooltip ; détail : wrap), modèle étendu, mocks + tests |
| Mobile | — | Hors-scope |

## Procédures d'exploitation

Aucune procédure spécifique identifiée : pas de nouvelle donnée persistée, pas de script de migration, pas de paramétrage de mise en production au-delà du déploiement standard du module `conversation`. `To be decided later` : besoin éventuel d'une alerte de monitoring dédiée sur le taux d'échec de résolution (dégradation silencieuse) — à évaluer avec le SRE si jugé utile après mise en prod.

## Stratégie de déploiement

Déploiement séquentiel classique : back en premier (contrat d'API stabilisé), front ensuite. Pas de rattrapage de données nécessaire (calcul à la volée, rien à backfiller). Pas de pilote spécifique proposé — le risque étant borné (dégradation silencieuse, pas de feature flag jugé nécessaire par l'équipe).

## Questions ouvertes

*Aucune question ouverte restante.*

**Tranchées depuis la rédaction :**

- **Nom exact de la nouvelle action eventbus `directory`** : `list-users-structures`, tranché à l'implémentation d'IMPULS-6116 (28/07/2026). Nom retenu pour décrire la donnée retournée plutôt que l'usage métier, donc réutilisable hors `conversation` — la résolution restant côté `conversation`.

- **Nommage du champ exposé** : `from.displayStructure` (28/07/2026, cf. section Back). L'ancien nom `structure` n'est plus valide nulle part.
