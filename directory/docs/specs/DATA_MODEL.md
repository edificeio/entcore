# DATA_MODEL — module `directory`

> Sources : `src/main/resources/jsonschema/`, `src/main/resources/public/ts/model.ts`, `src/main/resources/public/ts/admin/model.ts`, `src/main/java/org/entcore/directory/pojo/`

Le module manipule deux modèles `User` distincts côté front : celui de l'annuaire (`public/ts/model.ts`) et celui du class-admin (`public/ts/admin/model.ts`, plus riche). Les deux sont documentés.

---

## Entité: User (annuaire / Mon Compte)

| Champ | Type | Obligatoire | Calculé | Description |
|-------|------|-------------|---------|-------------|
| id | String | oui | non | UUID |
| firstName / lastName | String | non | non | Prénom / nom |
| displayName | String | non | non | Nom affiché |
| loginAlias | String (null) | non | non | Alias de connexion |
| birthDate | String (null) | non | non | `YYYY-MM-DD` (formaté par moment à l'envoi) |
| address / zipCode / city | String (null) | non | non | Coordonnées postales |
| email | String (null) | non | non | Email |
| homePhone / mobile | String (null) | non | non | Téléphones ; mobile normalisé E.164 par le backend |
| picture | String | non | non | URL avatar ; `no-avatar.jpg`/`no-avatar.svg` sont remplacés par `''` au chargement |
| mood | Object `{id, icon, text}` | non | oui | Construit depuis l'id ; repli sur `default` si humeur inconnue |
| motto | String | non | non | Devise, max 75 caractères |
| health | String | non | non | Max 1 000 caractères |
| hobbies | Hobby[] | non | non | Les hobbies sans `values` sont filtrés au chargement |
| type / profile | String\|String[] | non | non | `Student`, `Teacher`, `Personnel`, `Relative`, `Guest` ; `getProfileType()` = `profile` ?? `type[0]` ?? `profiles[0]` |
| schools | School[] | non | oui | Établissements (depuis `/userbook/api/person`) |
| attachedStructures | School[] | non | oui | Structures triées : admin → adml → autres (cf. BUSINESS_RULES) |
| relatives | User[] | non | oui | Parents ou enfants, construits depuis les lignes `relatedId/relatedName/relatedType` du résultat person |
| childrenStructure | Array | non | oui | Chargé via `/directory/user/:id/children` |
| visibleInfos | String[] | non | non | `SHOW_EMAIL`, `SHOW_PHONE`, `SHOW_BIRTHDATE`, `SHOW_HEALTH`, `SHOW_MOBILE` (`SHOW_MAIL` existe mais marqué inutilisé) |
| visible | Object | non | oui | Vue dérivée par champ : `public`/`prive` selon `visibleInfos` |
| positions / userPositions | UserPosition[] | non | non | Postes fonctionnels ; `extractPositionNames()` = `positionNames` ?? `userPositions[].name` |
| subjects | String[] | non | non | Matières (Teacher) |
| functions | Array | non | non | `functions[0][1]` contient les ids de structures du périmètre ADML |
| mergeKey | String | non | oui | Clé de fusion générée |
| mergedLogins | String[] | non | oui | Logins fusionnés (résultat) |
| federatedAddress | String | non | oui | Adresse IdP (depuis `/directory/conf/public`, indexé par `federatedIDP`) |
| pictureVersion | Number | non | oui | Compteur de cache-busting avatar |
| edit | `{infos?, userbook?, visibility?}` | non | non | Drapeaux d'édition activés par la route |

**Relations** : User ∈ 0..N School ; Student ↔ 0..N Relative ; User ∈ 0..N Group.

**Source** : `public/ts/model.ts:27-97, 614-875`, `jsonschema/updateUser.json`

---

## Entité: User (class-admin)

Champs supplémentaires ou divergents par rapport au modèle annuaire :

| Champ | Type | Calculé | Description |
|-------|------|---------|-------------|
| login / originalLogin / tempLoginAlias | String | non | Login courant, login d'origine (retour si alias vidé), saisie en cours |
| lastLogin | String | non | Dernière connexion (affichée via `Intl.DateTimeFormat`) |
| blocked | Boolean | non | Compte bloqué |
| activationCode | String | non | Présent ⇒ compte non activé |
| resetCode / resetCodeDate | String | non | Code de renouvellement généré et sa date |
| source | `"MANUAL"\|"CLASS_PARAM"\|"BE1D"\|"CSV"` | non | Source du compte — conditionne le droit de suppression (AAF non supprimable) |
| totp / hasTotp | String / Boolean | non | Secret TOTP |
| lockedEmail | Boolean? | non | Email verrouillé (édition réservée au SUPER_ADMIN), présent seulement sur `/directory/class-admin/:id` |
| relativeIds / relativeList | String[] / `{relatedId, relatedName}[]` | non | Liens parent-enfant sous deux formes |
| classIds | String[] | non | Classes d'appartenance |
| selected | Boolean | non | Sélection dans la liste |
| version | Number | non | Incrémenté à chaque update (cache-busting avatar) |
| safeLastName | getter/setter | oui | Setter : MAJUSCULES sans caractères spéciaux |
| safeFirstName | getter/setter | oui | Setter : Capitalisation Complète sans caractères spéciaux |
| safeDisplayName | getter/setter | oui | `displayName` ?? `lastName firstName` |
| birthDate | getter/setter | oui | Stocké en `_birthDateISO` (`YYYY-MM-DD`) |
| shortBirthDate / inverseBirthDate | getter | oui | `DD/MM/YYYY` / `YYYYMMDD` (tri) |
| safePicture / avatar48Uri / avatarUri | getter | oui | URL avatar avec `?version=` ; thumbnail 48x48 |
| firstClassName / classNames | getter | oui | Classes de la 1ʳᵉ école ; `"-"` si indisponible |
| isMe | getter | oui | `id === model.me.userId` |
| safeHasEmail | getter | oui | `hasEmail \|\| !!email` |

**Source** : `public/ts/admin/model.ts:64-200+`

---

## Entité: Hobby

| Champ | Type | Obligatoire | Description |
|-------|------|-------------|-------------|
| category | String | oui | sport, cinema, music… |
| values | String | non | Texte libre ≤ 80 caractères ; `undefined` remplacé par `""` avant sauvegarde |
| visibility | String | non | `PUBLIC` / `PRIVE` |

**Source** : `public/ts/model.ts:614-618`, `admin/model.ts:5`, `UserController.java:284-296`

---

## Entité: Group

| Champ | Type | Obligatoire | Calculé | Description |
|-------|------|-------------|---------|-------------|
| id | String | oui | non | UUID |
| name | String | oui | non | Nom |
| sortName | String | non | non | Nom de tri prioritaire sur `name` |
| groupType | String | non | non | Type (Manual, Profile, Functional…) — clé de tri primaire |
| nbUsers | Number | non | oui | Renvoyé si `nbUsersInGroups: true` dans la recherche |
| users | User[] | non | oui | Via `GET /communication/visible/group/:id` |
| isGroup | Boolean | non | oui | Marqueur client dans les résultats mixtes users+groups |

**Source** : `public/ts/model.ts:98-112, 454-489`

---

## Entité: ManualGroup (corps de création/màj)

| Champ | Type | Obligatoire | Description |
|-------|------|-------------|-------------|
| name | String | **oui** | Nom |
| classId / structureId | String | non | Rattachement |
| subType | String | non | Sous-type |
| autolinkTargetAllStructs | Boolean | non | Auto-lien vers toutes les structures |
| autolinkTargetStructs | String[] | non | Structures cibles |
| autolinkUsersFromGroups | String[] | non | Groupes sources |
| autolinkUsersFromPositions | String[] | non | Positions sources |
| autolinkUsersFromLevels | String[] | non | Niveaux sources |

`additionalProperties: false`. **Source** : `jsonschema/createManualGroup.json`, `updateManualGroup.json`

---

## Entité: Favorite (ShareBookmark)

| Champ | Type | Obligatoire | Calculé | Description |
|-------|------|-------------|---------|-------------|
| id | String | oui | oui | Généré à la création (201) |
| name | String | **oui** | non | Nom du favori |
| members | String[] | oui (écriture) | non | Ids users + groups confondus |
| users / groups | User[] / Group[] | non | oui | Reconstitués à la lecture ; distinction client par présence de `member.name` |

Comportements : favori vide à la lecture ⇒ auto-suppression backend + 404 ; groupes triés type puis nom.

**Source** : `public/ts/model.ts:113-158`, `jsonschema/createShareBookmark.json`, `ShareBookmarkController.java`

---

## Entité: School (Structure)

| Champ | Type | Description |
|-------|------|-------------|
| id / name | String | Identité |
| externalId | String | Id externe (imports) |
| UAI | String | Code UAI (recherches ADML par UAI) |
| classrooms | Classroom[] | Classes |
| users | User[] | Utilisateurs |
| parents / children | School[] | Hiérarchie reconstruite côté client : un parent absent de la liste est retiré ; `parents` supprimé si vide |
| admin / adml | Boolean | Drapeaux posés sur `attachedStructures` (cf. BUSINESS_RULES) |
| levels / distributions / levelsOfEducation | divers | Paramétrage admin (cf. StructureController) |

**Source** : `public/ts/model.ts:251-377`, `admin/model.ts:6-7`, `StructureController.java`

---

## Entité: Classroom

| Champ | Type | Description |
|-------|------|-------------|
| id / name | String | Identité ; en « Ma Classe », id/nom zippés depuis `classes` + `realClassesNames` de la session OAuth |
| externalId | String | Utilisé pour l'import CSV (`classExternalId`) |
| level | String | Niveau (éditable avec le nom via `PUT /directory/class/:id`) |
| users | User[] | Membres |

**Source** : `public/ts/model.ts:307-352`, `admin/service.ts:169-186`

---

## Entité: UserPosition

| Champ | Type | Description |
|-------|------|-------------|
| id | String | UUID |
| name | String | Libellé |

CRUD admin complet via `/directory/positions` (cf. API_CONTRACTS). **Source** : `UserPositionController.java`, `public/ts/model.ts:867-873`

---

## Entité: Mood

Liste dynamique : `GET /directory/userbook/moods` retourne les ids configurés (`user-book-data.moods`, contient toujours `default`). Chaque id est projeté en `{id, icon, text: 'userBook.mood.<id>'}`.

**Source** : `public/ts/model.ts:875-901`, `UserController.java:114-132`, `admin/model.ts` (classe `Mood`)

---

## Entité: Slot / SlotProfile

Grilles horaires d'établissement : un `SlotProfile` appartient à une école (`schoolId`) et contient des `Slots` (`name`, `startHour`, `endHour`). CRUD soumis au droit `directory.slot.manage`.

**Source** : `jsonschema/createSlotProfile.json`, `createSlot.json`, `SlotProfileController.java`, `pojo/Slot.java`

---

## Schémas de validation (`src/main/resources/jsonschema/`)

| Schéma | Usage | Contraintes clés |
|--------|-------|------------------|
| `updateUser.json` | PUT /user/:id | `additionalProperties: false` ; champs identité + `totp`, `childrenIds`, `positionIds` |
| `createShareBookmark.json` | POST/PUT sharebookmark | `name` requis |
| `createManualGroup.json` / `updateManualGroup.json` | groupes manuels | `name` requis ; options autolink |
| `addFunction.json` / `createFunction.json` / `createFunctionGroup.json` | fonctions | — |
| `addHeadTeacher.json` / `updateHeadTeacher.json` | professeur principal | — |
| `addDirection.json` / `removeDirection.json` | direction | — |
| `createSlot.json` / `createSlotProfile.json` | grilles horaires | — |
| `createManualSubject.json` / `updateManualSubject.json` / `deleteManualSubject.json` | matières manuelles | — |
| `createTenant.json` | tenant | — |
| `initTimetable.json` | init EDT | — |
| `postMailState.json` / `putMailState.json` / `postMobileState.json` / `putMobileState.json` | validation email/SMS | — |
| `updateStructure.json` | PUT structure | — |
| `getAttachmentsInfos.json` | infos de rattachement | — |

---

## POJOs backend notables

| Classe | Rôle |
|--------|------|
| `pojo/ImportInfos.java` | Paramètres d'un import (wizard) |
| `pojo/MergeUsersMetadata.java` | Métadonnées de fusion de comptes |
| `pojo/TransversalSearchQuery.java` / `TransversalSearchType.java` | Recherche transversale admin (par nom/email…) |
| `pojo/Slot.java` | Créneau horaire |
| `pojo/Users.java` / `pojo/Ent.java` | Sérialisation JAXB pour exports XML |

**Source** : `src/main/java/org/entcore/directory/pojo/`
