# API_CONTRACTS — module `directory`

> Sources : contrôleurs Java (`src/main/java/org/entcore/directory/controllers/`), appels front (`public/ts/model.ts`, `public/ts/admin/service.ts`, `public/ts/controllers/`).
> Les gardes de sécurité détaillées sont dans PERMISSIONS.md ; seul « Auth requise » est rappelé ici.

## Sommaire par domaine
- [Personne & annuaire](#personne--annuaire) · [Utilisateur (CRUD)](#utilisateur) · [UserBook](#userbook) · [Validation email/mobile](#validation-emailmobile) · [Favoris](#favoris-sharebookmark) · [Classes](#classes) · [Class-admin & publipostage](#class-admin--publipostage) · [Doublons & fusion](#doublons--fusion) · [DiscoverVisible (module communication)](#discovervisible-module-communication) · [Divers consommés](#endpoints-externes-consommés)

---

## Personne & annuaire

### GET /userbook/api/person
**Description** : Profil « personne » (userbook) de soi-même ou d'un tiers. Une ligne de résultat **par proche** (relative) — le front agrège.
**Auth** : oui (workflow `userbook.authent`)
**Paramètres** : `id` (query, optionnel — défaut : soi), `type` (query, optionnel)
**Réponse 200** :
```json
{ "result": [{
    "id": "uuid", "displayName": "…", "email": "…", "mobile": "…",
    "birthdate": "…", "health": "…", "motto": "…", "mood": "happy",
    "schools": [{"id": "uuid", "name": "…"}],
    "hobbies": [{"category": "sport", "values": "…", "visibility": "PUBLIC"}],
    "visibleInfos": ["SHOW_EMAIL"],
    "relatedId": "uuid|''", "relatedName": "…", "relatedType": "Relative"
}]}
```
**Source** : `UserBookController.java:229` ; front `model.ts:35-62`, `account.ts`, `admin/service.ts:22-26`

### GET /userbook/search/criteria
**Description** : Critères de recherche disponibles (structures, classes, profils, fonctions, positions, types de groupes).
**Auth** : oui
**Paramètres** : `getClassesMonoStructureOnly` (query, bool) — classes retournées seulement si mono-structure
**Réponse 200** :
```json
{
  "structures": [{ "id": "uuid", "name": "string" }],
  "classes": [{ "id": "uuid", "name": "string" }],
  "profiles": ["Student", "Teacher", "Personnel", "Relative", "Guest"],
  "functions": ["HeadTeacher", "AdminLocal"],
  "positions": [{ "id": "uuid", "name": "string" }],
  "groupTypes": ["ManualGroup", "FunctionalGroup", "ProfileGroup"]
}
```
**Source** : `UserBookController.java:900` ; front `model.ts:443-445`

### GET /userbook/search/criteria/:structureId/classes
**Description** : Classes d'une structure (alimentation dynamique du filtre classes).
**Auth** : oui — **Réponse 200** : `{ "classes": [{id,name}] }`
**Source** : `UserBookController.java:939` ; front `model.ts:446-452`

### GET /userbook/structures
**Description** : Établissements de l'utilisateur avec hiérarchie (`parents`) ; le front reconstruit `children`.
**Auth** : oui (workflow `userbook.my.structures`)
**Réponse 200** :
```json
[{
  "id": "uuid",
  "name": "Lycée Example",
  "parents": [{ "id": "uuid" }]
}]
```
**Source** : `UserBookController.java:252` ; front `model.ts:252-272`, `admin/service.ts:138-161`

### GET /userbook/structure/:structId
**Description** : Détail d'une structure : classes, utilisateurs, profileGroups, manualGroups.
**Auth** : oui (workflow `userbook.structure.classes.personnel`)
**Source** : `UserBookController.java:268` ; front `model.ts:369-377`, `behaviours.ts:59`, `admin/service.ts:162-168`

### GET /userbook/api/class
**Description** : Membres d'une classe (vue « Ma Classe »).
**Auth** : oui — **Paramètres** : `id` (query, id de classe)
**Source** : `UserBookController.java:338` ; front `model.ts:342-349`

### GET /userbook/visible/users/:groupId
**Description** : Utilisateurs visibles d'un groupe (sniplet trombinoscope).
**Auth** : oui (workflow `userbook.visible.users.group`)
**Source** : `UserBookController.java:315` ; front `behaviours.ts:67`

### GET /userbook/person/birthday
**Description** : Anniversaires visibles (widget). Le front filtre par mois courant.
**Auth** : oui — **Réponse 200** : `[{ "birthDate", "classes": [[id, name],…], … }]`
**Source** : `UserBookController.java:648` ; front `birthday.ts:43-77`

### GET /userbook/user-preferences
**Description** : Préférences userbook (dont `userPreferencesBirthdayClass`).
**Auth** : oui (workflow `userbook.preferences`)
**Source** : `UserBookController.java:709` ; front `birthday.ts:43`

### GET|PUT /userbook/preference/:application
**Description** : Préférence par application (`theme`, `classadmin`, …). PUT accepte une valeur brute (string) ou un objet JSON selon l'app.
**Auth** : oui (workflow `user.preference`)
**Réponse GET 200** : `{ "preference": "<valeur>" }`
**Source** : `UserBookController.java:730, 836` ; front `account.ts:155-165`, `admin/service.ts:74-93`

### GET /userbook/api/edit-userbook-info
**Description** : Mise à jour d'une propriété userbook par query string (legacy, utilisée par le widget anniversaires).
**Auth** : oui — **Paramètres** : `prop`, `value`
**Source** : `UserBookController.java:384` ; front `birthday.ts:39-41`

### GET /api/set-visibility · GET /api/edit-user-info-visibility
**Description** : Bascule de visibilité d'un hobby (`value=PUBLIC|PRIVE`, `category=`) / d'une info (`info=email|phone|birthdate|health|mobile`, `state=public|prive`).
**Auth** : oui (workflow `userbook.authent`)
**Source** : `UserBookController.java:422, 690` ; front `account.ts:380-399`

---

## Utilisateur

### GET /directory/user/:userId
**Description** : Données administratives complètes. En accès « visible » (sans lien hiérarchique), ~40 champs sensibles sont retirés de la réponse.
**Auth** : oui (filtre `UserAccessOrVisible`)
**Paramètres** : `manual-groups` (query, bool)
**Réponse 200** :
```json
{
  "id": "uuid",
  "firstName": "string",
  "lastName": "string",
  "displayName": "string",
  "email": "string",
  "mobile": "string",
  "schools": [],
  "classes": [],
  "functions": [],
  "administrativeStructures": [],
  "userPositions": [{ "id": "uuid", "name": "Poste" }]
}
```
**Source** : `UserController.java:333-358` ; front `model.ts:749-788`

### PUT /directory/user/:userId
**Description** : Mise à jour des infos administratives. Corps validé par `jsonschema/updateUser.json` (`additionalProperties:false`).
**Auth** : oui (+ MFA)
**Corps** :
```json
{
  "firstName": "string",
  "lastName": "string",
  "displayName": "string",
  "birthDate": "YYYY-MM-DD",
  "address": "string",
  "zipCode": "string",
  "city": "string",
  "loginAlias": "string",
  "email": "string",
  "homePhone": "string",
  "mobile": "+33600000000",
  "totp": "string",
  "childrenIds": "string",
  "positionIds": ["uuid"]
}
```
**Comportements** :
- mobile non vide → validation + normalisation E.164, sinon `400` avec code d'erreur de validation
- non-admin : `positionIds` (et `lastName`/`firstName` hors CLASS_ADMIN) silencieusement retirés
- changement d'email/mobile → notification d'avertissement à l'ancienne valeur
- session recréée ; `loginAlias` en doublon → `400 "…already exists"`
**Source** : `UserController.java:134-239` ; front `model.ts:640-662`, `admin/service.ts:27-73` (variantes mono-champ : displayName, email, homePhone, mobile, totp, loginAlias)

### PUT /directory/user/login/:userId
**Description** : Changement du **login canonique** (réservé super-admin). Corps `{ "login": "…" }` ; `400` si absent.
**Source** : `UserController.java:241-259`

### GET /directory/user/:userId/children
**Description** : Structures des enfants d'un parent. **Réponse 200** : array `childrenStructure`.
**Source** : `UserController.java:747` ; front `model.ts:63-68`

### PUT|DELETE /directory/user/:studentId/related/:relativeId
**Description** : Lie / délie un parent et un élève.
**Source** : `UserController.java:857, 886` ; front `admin/service.ts:270-287`

### POST /directory/user/delete
**Description** : Suppression (pré-suppression) d'utilisateurs par un enseignant. Corps `{ "users": ["id"] }`.
**Source** : `UserController.java:526` ; front `admin/service.ts:187-190`

### POST /directory/class/:classId/user
**Description** : Création d'un utilisateur directement dans une classe (class-admin).
**Corps** : `{ lastName, firstName, type, birthDate?, email?, mobile?, childrenIds? }`
**Réponse 200/201** : utilisateur créé (id, login, activationCode…)
**Source** : `ClassController.java:111` ; front `admin/service.ts:95-99`

### GET /directory/class-admin/:userId
**Description** : Vue « person V2 » pour le class-admin : écoles avec classes `{id,name}`, source, et indicateurs comme `lockedEmail`.
**Source** : `DirectoryController.java:106-128` ; front `admin/service.ts:17-21`

---

## UserBook

### GET /directory/userbook/:userId
**Description** : Carnet (mood, motto, health, hobbies, picture). Le front remplace `no-avatar.jpg|svg` par `''` et résout l'objet mood.
**Source** : `UserController.java:413` ; front `model.ts:695-720`

### PUT /directory/userbook/:userId
**Description** : Mise à jour du carnet.
**Corps** :
```json
{
  "mood": "happy",
  "motto": "Ma devise (max 75 chars)",
  "health": "Infos santé (max 1000 chars)",
  "picture": "url",
  "hobbies": [{ "category": "sport", "values": "foot (max 80 chars)" }]
}
```
**Erreurs 400** : motto > 75 ; mood hors liste ; health > 1000 ; hobby.values > 80
**Effets** : cookie `userbookVersion` mis à jour, session recréée, notification timeline si motto/mood de soi-même.
**Source** : `UserController.java:262-331` ; front `model.ts:614-638`, `admin/service.ts:28-39`

### PUT /directory/avatar/:userId
**Description** : Upload d'avatar (multipart, champ `image`). Le front ajoute des paramètres `thumbs` et incrémente `pictureVersion`.
**Source** : `UserController.java:420` ; front `model.ts:810-822`

### GET /directory/userbook/moods
**Description** : Liste des humeurs configurées (toujours ≥ `["default"]`).
**Source** : `UserController.java:128-132` ; front `model.ts:899-901`

### GET /userbook/avatar/:id
**Description** : Avatar public d'un utilisateur. **Sans authentification.** Paramètre `thumbnail` (ex. `48x48`).
**Source** : `UserBookController.java:631-646` ; front `admin/model.ts:188-193`, `directory.ts:214-220`

---

## Validation email/mobile

### GET /directory/user/mailstate · GET /directory/user/mobilestate
**Description** : État de validation de l'email / du mobile de l'utilisateur courant.
**Réponse 200** :
```json
{ "email": "email@example.com", "emailState": { "valid": "email@example.com" } }
```
```json
{ "mobile": "+33600000000", "mobileState": { "valid": "+33600000000" } }
```
Le front considère la valeur validée si `state.valid === valeur courante`.
**Source** : `UserController.java:1085, 1159` ; front `account.ts:114-136`

### PUT|POST /directory/user/mailstate · /user/mobilestate
**Description** : Démarrage (PUT, corps validé par `putMailState.json`/`putMobileState.json`) et vérification du code (POST, `postMailState.json`/`postMobileState.json`) du parcours de validation.
**Source** : `UserController.java:1104-1127, 1178-1202`, `jsonschema/p*State.json`

---

## Favoris (ShareBookmark)

### GET /directory/sharebookmark/all
**Réponse 200** : `[{ "id", "name" }]` (favoris de l'utilisateur courant).
**Source** : `ShareBookmarkController.java:76-78` ; front `model.ts:490-498`

### GET /directory/sharebookmark/:id
**Description** : Détail avec membres traduits et regroupés (`users` + `groups`).
**Réponse 200** :
```json
{
  "id": "uuid",
  "name": "Mon favori",
  "users": [{ "id": "uuid", "displayName": "string" }],
  "groups": [{ "id": "uuid", "name": "string", "groupType": "ManualGroup" }]
}
```
**Erreur** : `404 empty.sharebookmark` si plus aucun membre (favori auto-supprimé).
**Source** : `ShareBookmarkController.java:70-106` ; front `model.ts:129-139`

### POST /directory/sharebookmark · PUT /directory/sharebookmark/:id · DELETE /directory/sharebookmark/:id
**Corps (POST/PUT, schéma `createShareBookmark.json`)** : `{ "name": "…", "members": ["id", …] }`
**Réponse POST 201** : `{ "id": "uuid" }`
**Source** : `ShareBookmarkController.java:46-158` ; front `model.ts:140-158`

### GET /directory/sharebookmark/:userId/:id
**Description** : Variante admin — favoris d'un autre utilisateur (`id` = `all` pour lister).
**Source** : `ShareBookmarkController.java:108-146`

---

## Classes

### GET /directory/class/:classId
**Réponse 200** : `{ name, level, externalId, … }`
**Source** : `ClassController.java:76` ; front `admin/service.ts:169-176`

### PUT /directory/class/:classId
**Corps** : `{ "name", "level" }` — **Source** : `ClassController.java:83` ; front `admin/service.ts:183-186`

### GET /directory/class/:classId/users
**Paramètres** : `collectRelative` (bool), `type` + `format=csv` (export CSV des codes d'activation par profil)
**Source** : `ClassController.java:150` ; front `admin/service.ts:177-182`, `admin/delegates/userList.ts:266-269`

### PUT /directory/class/add-self
**Description** : Auto-rattachement de l'utilisateur courant à des classes. Corps `{ "classIds": ["id"] }`.
**Source** : `ClassController.java:198` ; front `admin/service.ts:257-260`

### PUT /directory/class/:classId/add/:userId
**Description** : Rattache un utilisateur existant à la classe (sans le détacher d'ailleurs).
**Source** : `ClassController.java:263` ; front `admin/service.ts:261-269`

### PUT /directory/class/:classId/link · /unlink · /change
**Description** : Liaison/déliaison/changement **en masse**. Corps `{ "ids": [...] }` ; pour `change` : `{ "ids": [...], "classIds": [classes d'origine] }` (opération atomique remplaçant link+unlink).
**Source** : `ClassController.java:366-424` ; front `admin/service.ts:288-359`

### GET /directory/class/users/detached
**Description** : Utilisateurs détachés (sans classe) des structures données. Paramètres `structureId` répétés.
**Source** : `ClassController.java:452` ; front `admin/service.ts:100-106`

### GET /directory/class/users/visibles
**Description** : Utilisateurs visibles rattachables à une classe. Paramètres `classId`, `collectRelative` (⚠️ le front envoie `&=collectRelative=` — paramètre mal formé, probablement ignoré par le backend).
**Source** : `ClassController.java:467` ; front `admin/service.ts:107-110`

---

## Class-admin & publipostage

### POST /directory/class-admin/massmail
**Description** : Publipostage / exports du class-admin.
**Corps** : `{ "type": "pdf"|"newPdf"|"simplePdf"|"mail"|"csv", "structureId", "theme", "ids": [...] }`
**Réponse** : JSON (mail) ou binaire (PDF/CSV téléchargé en blob).
**Source** : `StructureController.java:562` ; front `admin/service.ts:380-410`

### POST /directory/import/:userType/class/:classId
**Description** : Import CSV de classe (multipart : fichier nommé selon le profil + `classExternalId`).
**Erreurs** : message `"<clé i18n> <numLigne>"` ou contenant `already exists`.
**Source** : `ImportController.java:180` ; front `admin/service.ts:218-256`

### Endpoints auth consommés par le class-admin
| Endpoint | Usage |
|----------|-------|
| `PUT /auth/users/block` `{users, block}` | Blocage/déblocage en masse |
| `POST /auth/sendResetPassword` (form : `login`, `email`) | Mail de réinitialisation — envoyé **à l'email de l'enseignant** |
| `POST /auth/massGeneratePasswordRenewalCode` `{users}` | Codes de renouvellement ; réponse `{ "<userId>": {code, date} }` |

**Source** : `admin/service.ts:360-375`, `admin/delegates/actions.ts:118-131`

---

## Doublons & fusion

### GET /directory/duplicates · PUT /duplicate/merge/:id1/:id2 · DELETE /duplicate/ignore/:id1/:id2
**Description** : Gestion des doublons détectés (console admin).
**Source** : `UserController.java:895-932`

### GET /directory/duplicate/user/mergeKey
**Réponse 200** : `{ "mergeKey": "uuid" }` — **Source** : `UserController.java:995` ; front `model.ts:839-844`

### POST /directory/duplicate/user/mergeByKey
**Corps** : `{ "mergeKeys": ["uuid"] }` — **Réponse 200** : `{ "mergedLogins": [...] }`
**Erreur** : 400 avec `{error}` affiché tel quel, sinon repli `invalid.merge.keys`.
**Source** : `UserController.java:1010` ; front `model.ts:846-865`

### POST /directory/duplicate/generate/mergeKey/:userId · POST /duplicate/user/unmergeByLogins
**Description** : Variantes admin (génération pour un tiers / défusion super-admin).
**Source** : `UserController.java:987, 1030`

---

## DiscoverVisible (module `communication`)

Endpoints consommés par l'annuaire mais **implémentés dans le module communication** :

| Méthode/chemin | Usage | Réponse |
|----------------|-------|---------|
| `GET /communication/discover/visible/profiles` | Profils autorisés (active l'onglet) | `["Teacher","Personnel"]` ; 403 → `[]` côté front |
| `GET /communication/discover/visible/structures` | Structures de découverte | array |
| `POST /communication/discover/visible/users` `{search, structures?, profiles?}` | Recherche d'utilisateurs | array users avec `hasCommunication` |
| `GET /communication/discover/visible/groups` | Mes groupes de découverte | array |
| `GET /communication/discover/visible/group/:id/users` | Membres d'un groupe | array |
| `POST /communication/discover/visible/group` `{name}` | Création de groupe | `{id}` |
| `PUT /communication/discover/visible/group/:id` `{name}` | Renommage | — |
| `PUT /communication/discover/visible/group/:id/users` `{oldUsers, newUsers}` | Remplacement des membres | — |
| `POST /communication/discover/visible/add/commuting/:receiverId` | Création du lien bidirectionnel | `{number}` (>0 = OK) |
| `DELETE /communication/discover/visible/remove/commuting/:receiverId` | Suppression du lien | `{number}` |

**Source** : front `model.ts:523-594`

### POST /communication/visible
**Description** : Recherche principale de l'annuaire (users et/ou groupes visibles). Également utilisée par le class-admin (recherche d'enfants, détection de doublons).
**Corps** :
```json
{
  "search": "texte de recherche (lowercase)",
  "types": ["User"],
  "structures": ["uuid"],
  "classes": ["uuid"],
  "profiles": ["Teacher", "Student", "Personnel", "Relative", "Guest"],
  "functions": ["HeadTeacher"],
  "positions": ["uuid"],
  "mood": true,
  "nbUsersInGroups": true,
  "groupType": true
}
```
**Réponse 200** :
```json
{
  "users": [{ "id": "uuid", "displayName": "string", "mood": "default", "profile": "Teacher" }],
  "groups": [{ "id": "uuid", "name": "string", "groupType": "ManualGroup", "nbUsers": 12 }]
}
```
**Source** : front `model.ts:397-489`, `admin/service.ts:111-137`

### GET /communication/visible/group/:id
**Description** : Membres visibles d'un groupe (fiche groupe annuaire).
**Source** : front `model.ts:101-106`

---

## Endpoints externes consommés

| Endpoint | Usage | Source front |
|----------|-------|--------------|
| `GET /auth/oauth2/userinfo?version=v2.0` | Classes de « Ma Classe » (`classes` + `realClassesNames`) | `model.ts:307-326` |
| `GET /auth/context` | `passwordRegex`, `mfaConfig` | `account.ts:104-112` |
| `GET /auth/user/requirements` | `needMfa` | `account.ts:104-112` |
| `POST /auth/generate/otp` | OTP pour comptes fédérés avec app — réponse `{"otp": "12345678"}` (8 caractères exigés par le front) | `model.ts:85-96` |
| `POST /auth/reset/password` | Changement de mot de passe (form) | `account.ts:238, 343-361` |
| `GET /directory/conf/public` | `federatedAddress` par IdP, `disabledFederatedAdress` | `model.ts:800-808` |
| `POST /infra/event/web/store` `{event-type}` | Tracking discoverVisible (échec silencieux) | `model.ts:595-611` |
| `GET /optionalFeature/writeToEmailProvider/:endPoint` | Redirection vers le fournisseur mail (Wordline) — `{url}` ouvert en `_self` | `directory.ts:1137-1151` |
| `GET /assets/theme-conf.js` | Groupes de thèmes (eval du JS) | `account.ts:174-187` |
