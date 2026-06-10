# WORKFLOWS — module `directory`

> Sources : `public/ts/app.ts` (routing), `public/ts/controllers/`, `public/ts/admin/`, `view/` (shells HTML)

---

## Routage AngularJS (`app.ts`)

Le routeur sert deux applications selon l'URL hôte :

**Sur `/userbook/mon-compte`** (contrôleur `MyAccount`) :

| Route hash | Action | Périmètre d'édition |
|------------|--------|---------------------|
| `#/edit-user/:id` | `editUser` | userbook + infos d'un tiers |
| `#/edit-user-infos/:id` | `editUserInfos` | infos seulement |
| `#/edit-me` | `editMe` | userbook + visibility (+ infos si non-élève) |
| `#/themes` | `themes` | userbook + visibility (vue thèmes) |
| autre | redirection → `edit-me` | |

**Sur `/userbook/annuaire`** (contrôleur `DirectoryController`) :

| Route hash | Action |
|------------|--------|
| `#/search` | `directory` (recherche) |
| `#/myClass` | `myClass` |
| `#/user-view/:userId` | `viewUser` |
| `#/:userId` | `viewUser` (raccourci, utilisé par le sniplet trombinoscope) |
| `#/group-view/:groupId` | `viewGroup` |
| autre | redirection → `/myClass` |

**Source** : `public/ts/app.ts:8-47`

⚠️ Il n'existe **pas** de pont AngularJS→React dans ce module : `view-src/` contient les shells HTML sources des vues (`annuaire.html`, `mon-compte.html`, `class-admin.html`, `admin-console.html`, `birthday.html`, `wizard.html`, `timetable.html`), compilés vers `view/`. Aucune redirection de rétrocompatibilité à documenter.

---

## WF-01 — Chargement de l'annuaire (`#/search`)

**Déclencheur** : navigation vers `/userbook/annuaire#/search`
**États** : `vide` → `critères chargés` → `prêt` (→ `résultats pré-filtrés` si paramètres URL)
**Étapes** :
1. Réinitialisation des collections (users, groups, favoriteForm, schools)
2. `GET /directory/sharebookmark/all` (favoris) ; sur desktop, sélection auto du premier favori
3. `GET /userbook/structures` (hiérarchie d'établissements)
4. `GET /userbook/search/criteria?getClassesMonoStructureOnly=true` puis construction des options de filtres (users, groups, formulaire favori) ; remplacement des structures par celles de l'étape 3
5. `preApplyFilters()` : lecture des paramètres URL et recherche immédiate le cas échéant (cf. BUSINESS_RULES)
6. En parallèle : `GET /communication/discover/visible/profiles` (activation onglet discoverVisible)
7. Templates : `page:directory`, `list:dominos`
**Cas d'erreur** : 403 sur l'API discover → onglet masqué silencieusement.
**Source** : `public/ts/controllers/directory.ts:112-174, 1174-1201`

## WF-02 — Recherche annuaire (users / groupes / favoris)

**Déclencheur** : soumission du formulaire (`searchDirectory()`) ou changement d'onglet (`switchForm`)
**États** : `formulaire` → `loading` → `résultats` | `formulaire favori`
**Étapes** :
1. `index 2 + myNetwork` → bascule en création de favori (WF-06) et fin
2. `display.loading` (+ variante mobile)
3. `POST /communication/visible` — corps users : `{search (lowercase), types:["User"], structures?, classes?, profiles?, functions?, positions?, mood:true}` ; corps groupes : `{…, types:[types cochés]||["Group"], nbUsersInGroups:true, groupType:true}`
4. Tri : users par `displayName` ; groupes par `groupType` puis `sortName||name`
5. Mobile : 0 résultat → `notify.info("noresult")`, sinon ouverture du panneau résultats
**Source** : `public/ts/controllers/directory.ts:336-380, 830-860`, `public/ts/model.ts:397-489`

## WF-03 — Consultation d'une fiche utilisateur

**Déclencheur** : clic sur un domino, route `#/user-view/:id` ou `#/:id`
**États** : `liste` → `chargement` → `fiche` (empilable via `pastUsers`)
**Étapes** :
1. `GET /userbook/api/person?id=&type=` : hobbies sans valeurs filtrés, relatives construits, schools → attachedStructures
2. Test de visibilité : `GET /directory/user/:id` (succès = visible)
3. Si consultant non-élève : `loadInfos()` (mêmes données administratives + tri des structures)
4. Si consultant enseignant/personnel ET fiche d'un parent : `loadChildren()`
5. `removePrivateInfos()` (masquage selon `visibleInfos`)
6. Template `details:user-infos`, scroll en haut, push dans l'historique
**Cas d'erreur** : person vide → `user.id = undefined` (fiche non affichée).
**Source** : `public/ts/controllers/directory.ts:90-99, 558-598`, `public/ts/model.ts:35-96, 740-788`

## WF-04 — Consultation d'un groupe

**Déclencheur** : clic sur un domino groupe ou route `#/group-view/:groupId`
**Étapes** :
1. Si entrée par URL : `GET /directory/group/:id` pour le nom ; onglet groupes (`search.index=1`), `myNetwork`
2. `GET /communication/visible/group/:id` → membres mappés en `User`
3. Templates `dominosUser`, `groupActions`, `groupInfos`
**Source** : `public/ts/controllers/directory.ts:100-111, 709-723`, `public/ts/model.ts:98-112`

## WF-05 — Ma Classe (`#/myClass`, route par défaut)

**Déclencheur** : navigation vers l'annuaire sans hash explicite
**États** : `init` → `no-classroom` | `mono-class` | `class-list`
**Étapes** :
1. Favoris chargés (comme WF-01)
2. `GET /auth/oauth2/userinfo?version=v2.0` → zip `classes`/`realClassesNames`
3. 0 classe → `no-classroom` ; 1 classe → sélection auto ; plusieurs → liste avec recherche
4. Sélection d'une classe : `GET /userbook/api/class?id=` → membres
**Source** : `public/ts/controllers/directory.ts:175-207, 725-745`, `public/ts/model.ts:307-352`

## WF-06 — Création / édition d'un favori

**Déclencheur** : onglet « favoris » du formulaire (index 2) ou bouton éditer
**États** : `dominos` → `favorite-form` → `recherche membres` → `sauvegarde` → `favori sélectionné`
**Étapes** :
1. Création : reset (`name`, `members`) ; édition : pré-remplissage avec `groups.concat(users)`
2. Recherche de membres : `POST /communication/visible` avec `all=true` (users **et** groupes mélangés, groupes en premier)
3. `saveFavorite()` : POST `/directory/sharebookmark` `{name, members:[ids]}` (création) ou PUT `/directory/sharebookmark/:id` (édition)
4. Tri de la liste, re-sélection du favori (`GET /sharebookmark/:id`), fermeture du formulaire
**Cas d'erreur / annulation** : `cancelFavorite()` — en édition re-sélectionne le favori, en création désélectionne (et re-sélectionne le premier sur desktop).
**Source** : `public/ts/controllers/directory.ts:388-683`

## WF-07 — Ajout rapide aux favoris depuis une fiche

**Déclencheur** : bouton « ajouter au favori » d'une fiche user/groupe
**Étapes** :
1. Lightbox `add-user-favorite`
2. Favori existant : chargement des membres, contrôle de doublon par id, ajout + `save(…, editing=true)` si absent
3. Nouveau favori : `save(name, [fiche courante], editing=false)` puis insertion triée
4. Notification `directory.notify.confirmAddUser` + nom du favori
**Source** : `public/ts/controllers/directory.ts:469-512`

## WF-08 — Suppression d'un favori

**Déclencheur** : icône poubelle (inactive pendant la création)
**États** : `confirmation` (lightbox `confirm-favorite-remove`) → `suppression` → `liste mise à jour`
**Étapes** : `DELETE /directory/sharebookmark/:id` ; retrait local ; sur desktop sélection du premier favori restant ; si le formulaire était ouvert, fermeture propre.
**Source** : `public/ts/controllers/directory.ts:624-663`

## WF-09 — Mon Compte (`#/edit-me`)

**Déclencheur** : navigation `/userbook/mon-compte`
**États** : `init` → `compte chargé` → éditions ponctuelles
**Étapes** :
1. `open()` (person), `loadChildren()`, `load()` = `loadInfos()` + `loadUserbook()` (avatar par défaut nettoyé, mood résolu) + `loadVisibility()`
2. Bundle i18n auth ajouté (URLs charte/CGU/accessibilité/protection des données)
3. `loadThemeConf()`, `checkEmailValidation()`, `checkSmsValidation()`, `isAdmx()`, `checkIgnoreMFA()`
4. Vues : `user-edit` si non-élève sinon `user-view` ; `userbook-edit` toujours
5. `applyShowParam()` : ancre `?show=<id>` → scroll différé (1 s)
6. Si compte fédéré : chargement de `federatedAddress` depuis `/directory/conf/public`
**Source** : `public/ts/controllers/account.ts:24-90, 196-220`, `public/ts/model.ts:790-808`

## WF-10 — Changement de mot de passe (Mon Compte)

**Déclencheur** : bouton « mot de passe » (si autorisé, cf. BUSINESS_RULES)
**Étapes** :
1. Lightbox avec ancien / nouveau / confirmation ; jauge de complexité ; regex d'identité pour la confirmation
2. `POST` sur `resetPasswordPath` (`/auth/reset/password`) avec `{oldPassword, password, confirmPassword, login, callback:'/userbook/mon-compte'}`
3. Réponse avec `error` → `notify.error('userbook.renewpassword.error')` ; sinon fermeture
**Source** : `public/ts/controllers/account.ts:264-271, 343-361, 415-447`

## WF-11 — Fusion de comptes par clé

**Déclencheur** : section fusion de Mon Compte (droits workflow requis)
**Étapes** :
1. `generateMergeKey()` → `GET /directory/duplicate/user/mergeKey`
2. L'autre compte saisit la clé ; validation regex UUID côté client
3. `POST /directory/duplicate/user/mergeByKey` `{mergeKeys:[clé]}` ; spinner `mergeLoading`
4. Succès → champ vidé, `mergedLogins` affichés ; échec → message du backend ou `invalid.merge.keys`
**Source** : `public/ts/controllers/account.ts:469-497`, `public/ts/model.ts:839-865`

## WF-12 — DiscoverVisible : recherche et liaison

**Déclencheur** : onglet « découvrir » de l'annuaire
**Étapes** :
1. Garde-fou : structure cochée OU texte non vide
2. `POST /communication/discover/visible/users` `{search, structures?, profiles?}`
3. Liaison : `POST .../add/commuting/:receiverId` ; déliaison : `DELETE .../remove/commuting/:receiverId` ; mise à jour locale de `hasCommunication` dans les deux listes
4. Chaque action est trackée (`/infra/event/web/store`)
**Source** : `public/ts/controllers/directory.ts:1204-1441`

## WF-13 — DiscoverVisible : cycle de vie d'un groupe

**Déclencheur** : boutons créer / éditer / quitter dans l'onglet découverte
**Étapes** :
1. **Créer** : `POST .../group {name}` → `PUT .../group/:id/users {oldUsers:[], newUsers:[ids]}` → rechargement des groupes → sélection du nouveau groupe
2. **Éditer** : renommage seulement si le nom a changé ; puis mise à jour des membres `{oldUsers: baseUsersId, newUsers: selectedUsers}`
3. **Consulter** : `GET .../group/:id/users` ; si erreur (groupe disparu) → rechargement des groupes et retour liste
4. **Quitter** : mise à jour des membres sans soi-même
**Source** : `public/ts/controllers/directory.ts:1258-1464`

## WF-14 — Class Admin : initialisation et choix de classe

**Déclencheur** : ouverture de `/directory/class-admin`
**États** : `init` → `classe chargée` | `lightbox choose-class`
**Étapes** :
1. `GET /directory/class-admin/:userId` → écoles + classes (modèle V2)
2. Si aucune classe disponible alors que l'utilisateur a des structures → lightbox de choix : école → `GET /userbook/structure/:id` (classes) → sélection multiple → `PUT /directory/class/add-self {classIds}` → **window.location.reload()**
3. Sinon : classe restaurée depuis la préférence `classadmin` (`GET/PUT /userbook/preference/classadmin`), chargement `GET /directory/class/:id` + `GET /directory/class/:id/users?collectRelative=true` (tri par nom)
**Source** : `public/ts/admin/controller.ts:49-63`, `admin/delegates/choose-class.ts`, `admin/service.ts:17-21, 74-98, 169-182`

## WF-15 — Class Admin : création d'un utilisateur

**Déclencheur** : bouton « ajouter un utilisateur » (droit `allowClassAdminAddUsers`)
**États** : `form` → (`no-relatives`?) → (`duplicate`?) → `créé` (→ `form` si « créer et ajouter »)
**Étapes** :
1. Formulaire typé (champs requis selon profil, cf. BUSINESS_RULES) ; type `Relative` → recherche d'enfants (debounce 450 ms, filtre Student)
2. Garde « parent sans enfant » (une seule dérogation possible)
3. Garde doublons (Student/Relative) : recherche par nom dans la structure ; si homonymes → lightbox de choix : **rattacher** / **déplacer** (changement de classe avec parents) / **créer quand même**
4. `POST /directory/class/:classId/user {lastName, firstName, type, birthDate, email, mobile?, childrenIds?}` ; mobile éventuellement reformaté en international
5. `notify.success("user.added")` ; selon le bouton : fermeture ou nouveau formulaire ; événement `onUserCreated` → rafraîchissement de la liste
**Source** : `public/ts/admin/delegates/userCreate.ts`, `admin/service.ts:95-99`

## WF-16 — Class Admin : actions de masse sur la sélection

**Déclencheur** : cases cochées (par onglet de profil) → toaster d'actions
**Étapes** :
1. Sélection suivie par `onSelectionChanged` (tout cocher par onglet possible)
2. **Bloquer/débloquer** : `PUT /auth/users/block {users, block}` → reload classe + notification
3. **Supprimer** (si toutes les sources sont manuelles) : confirmation → `POST /directory/user/delete {users}` → reload
4. **Retirer de la classe** : confirmation → `PUT /directory/class/:id/unlink {ids}` avec parents inclus (`withRelative`)
5. **Mots de passe** : envoi mail (`POST /auth/sendResetPassword`, vers l'email de l'enseignant) ou génération de codes (`POST /auth/massGeneratePasswordRenewalCode`) puis impression
**Source** : `public/ts/admin/delegates/actions.ts`, `admin/delegates/userList.ts:215-233`, `admin/service.ts:360-375`

## WF-17 — Class Admin : fiche utilisateur

**Déclencheur** : clic sur une ligne (sauf si du texte est sélectionné)
**Étapes** :
1. `user.open({withChildren:true})` ; navigation précédent/suivant dans la liste triée du même onglet
2. Éditions champ par champ avec verrous anti double-envoi : displayName, email, téléphone, mobile (format international), login alias, TOTP — chaque PUT `/directory/user/:id` ne porte que le champ modifié
3. Garde ADML-soi-même (email/mobile → redirection Mon Compte) et `lockedEmail`
4. Userbook (mood/motto/photo) seulement si compte activé ; `saveUserBookChanges` via PUT `/directory/userbook/:id`
5. Liens parent-enfant : ajout/retrait avec exclusion des déjà-liés dans la recherche
6. Exports individuels : PDF détaillé (`pdf`) ou fiche famille (`simplePdf`, élève + parents)
**Source** : `public/ts/admin/delegates/userInfos.ts`, `admin/service.ts:27-73`

## WF-18 — Class Admin : import CSV

**Déclencheur** : lightbox import (droit `allowClassAdminCSVImport`)
**Étapes** :
1. Choix du fichier ; envoi multipart `POST /directory/import/:type/class/:classId` (champ nommé selon le profil, + `classExternalId`)
2. Verrou `importing` ; succès → rafraîchissement de la classe + tracking ; échec → message d'erreur contextualisé (cf. BUSINESS_RULES) + tracking erreur
**Source** : `public/ts/admin/controller.ts:122-141`, `admin/service.ts:218-256`

## WF-19 — Class Admin : publipostage et exports

**Déclencheur** : bouton export (toaster = sélection, ou bouton global = par profils)
**États** : pile de lightboxes naviguable (`_stack` avec retour arrière)
**Étapes** :
1. Choix du type : PDF simple / PDF détaillé / mail / CSV ; choix des profils si export global
2. `POST /directory/class-admin/massmail {type, structureId, theme, ids}` — réponse JSON (mail) ou blob téléchargé (PDF/CSV, nom de fichier horodaté traduit)
3. Cas mail : les utilisateurs **sans email** sont listés à part et imprimables en PDF ; confirmation avant envoi
4. Export CSV des codes d'activation : lien direct `GET /directory/class/:id/users?type=&format=csv`
**Source** : `public/ts/admin/delegates/userExport.ts`, `admin/service.ts:380-410`, `admin/delegates/userList.ts:266-269`

## WF-20 — Widget anniversaires

**Déclencheur** : affichage du widget portail
**Étapes** :
1. `GET /userbook/user-preferences` (classe préférée) puis `GET /userbook/person/birthday`
2. Filtre mois courant, tri par jour ; classes dérivées des résultats ; repli sur la première classe si la préférence n'existe plus
3. Changement de classe → sauvegarde de la préférence
**Source** : `public/ts/birthday.ts`

---

## Vues servies (shells HTML, dossier `view/`)

| Vue | Route backend | Garde |
|-----|---------------|-------|
| `annuaire.html` | `GET /userbook/annuaire` | workflow `userbook.authent` |
| `mon-compte.html` | `GET /userbook/mon-compte` | workflow `userbook.authent` |
| `class-admin.html` | `GET /directory/class-admin` | workflow `classadmin.address` + MFA |
| `admin-console.html` | `GET /directory/admin-console` | AdminFilter + MFA |
| `birthday.html` | `GET /userbook/birthday` | workflow `userbook.authent` |
| `wizard.html` | `GET /directory/wizard` | AdminFilter + MFA |
| `timetable.html` | `GET /directory/timetable` | AdminFilter + MFA |
| `calendar.html` | `GET /directory/calendar` | AdminFilter |

**Source** : `UserBookController.java:165-190`, `DirectoryController.java:90-104`, `ImportController.java:72-78`, `TimetableController.java:105-111`, `CalendarController.java:13-15`
