# BUSINESS_RULES — module `directory`

> Pattern structurel : **A** (mono legacy AngularJS — `src/` sans `frontend/`)
> Sources : `src/main/resources/public/ts/` (controllers, admin/, model.ts, behaviours.ts, birthday.ts), `src/main/java/org/entcore/directory/`

Features identifiées : **Annuaire** (recherche users/groupes), **Favoris** (ShareBookmarks), **Mon Compte** (UserBook), **DiscoverVisible**, **Ma Classe**, **Class Admin** (gestion de classe par l'enseignant), **Widget Anniversaires**, **Fusion de comptes**, **Sniplet Trombinoscope**.

---

## Feature: Annuaire — recherche

### Règle: Trois index de recherche par onglet
- **Contexte** : formulaire de recherche de l'annuaire (`/userbook/annuaire#/search`)
- **Condition** : `search.index === 0` (utilisateurs), `1` (groupes), `2` (favoris)
- **Action** : la soumission route vers la recherche correspondante ; l'index 2 sur l'onglet `myNetwork` ouvre le formulaire de création de favori au lieu de lancer une recherche.
- **Source** : `public/ts/controllers/directory.ts:336-380`

### Règle: Pagination locale par tranche de 50
- **Contexte** : listes de résultats (dominos) et liste d'établissements
- **Condition** : `search.maxLength` initialisé à 50 ; `search.maxSchoolsLength` à 7
- **Action** : `increaseSearchSize()` ajoute +50 résultats affichés ; `increaseSchoolsSize()` ajoute +7 établissements. `maxLength` est systématiquement remis à 50 à chaque navigation (sélection favori, retour, changement d'onglet).
- **Source** : `public/ts/controllers/directory.ts:72-84`

### Règle: Disponibilité croisée des filtres profil/fonction/position
- **Contexte** : panneau de filtres de recherche
- **Condition / Action** :
  - `Student`, `Relative`, `Guest` : disponibles seulement si **aucune fonction** n'est cochée (`testStudentRelativeGuestFilterAvailable`)
  - `Teacher` : toujours disponible
  - `Personnel` : indisponible si la fonction `HeadTeacher` est cochée
  - Fonction `HeadTeacher` : disponible seulement si aucun profil coché ou profil `Teacher` coché
  - Le filtre ADML (`testADMLFilterAvailable`) existe mais est **commenté** (désactivé)
  - Filtres fonction et position : disponibles si profils vides ou contenant `Teacher`/`Personnel` (`checkProfileTeacherPersonnel`)
  - Filtre type de groupe : disponible seulement si classes, profils et fonctions sont tous vides
- **Source** : `public/ts/controllers/directory.ts:954-1006`, `directory.ts:229-276`

### Règle: Filtre classes verrouillé en multi-établissement
- **Contexte** : utilisateur rattaché à plusieurs structures (`criteria.structures.length > 1`)
- **Condition** : `isMultiStructure() && noStructureSelected(searchIndex)`
- **Action** : le filtre classes est désactivé avec l'infobulle `directory.classes.disabled.pick.structure` ; il faut d'abord cocher un établissement. Les classes sont alors chargées dynamiquement par établissement coché (`onCheck` → `getSearchClasses(structureId)`), et retirées quand l'établissement est décoché (y compris les classes déjà sélectionnées dans les filtres actifs).
- **Source** : `public/ts/controllers/directory.ts:278-311`, `directory.ts:914-952`

### Règle: Critères de recherche mono-structure
- **Contexte** : chargement initial de l'annuaire
- **Condition** : appel `GET /userbook/search/criteria?getClassesMonoStructureOnly=true`
- **Action** : les classes ne sont incluses dans les critères que pour les utilisateurs mono-structure ; les options structures sont ensuite remplacées par la liste de `GET /userbook/structures` (commentaire « fix structure filter (add manual group structures) »).
- **Source** : `public/ts/controllers/directory.ts:137-161`, `public/ts/model.ts:443-445`

### Règle: Pré-application des filtres par paramètres d'URL
- **Contexte** : arrivée sur l'annuaire avec une URL du type `/userbook/annuaire#/search?filters=groups&structure=id&profile=Teacher&class=TP1&class=TP2&position=id`
- **Condition** : présence des paramètres `filters` (`users`|`groups`, défaut `users`), `profile`, `structure`, `class`, `position` (simples ou multiples)
- **Action** : les options correspondantes sont cochées (seulement si elles existent et sont `available`), la recherche est lancée automatiquement ; en responsive le panneau résultats ne s'ouvre que s'il y a des résultats.
- **Source** : `public/ts/controllers/directory.ts:1024-1126`

### Règle: Recherche locale par nom dans les résultats
- **Contexte** : champ de filtre local après une recherche
- **Condition** : saisie dans `search.text`
- **Action** : filtre client sur `displayName` (insensible à la casse) sans nouvel appel réseau (`updateSearch`).
- **Source** : `public/ts/controllers/directory.ts:382-386`

### Règle: Notification « aucun résultat » sur mobile uniquement
- **Contexte** : recherche users/groupes onglet `myNetwork` retournant 0 résultat
- **Condition** : viewport ≤ `wideScreen`
- **Action** : `notify.info("noresult")` ; sur desktop, simple liste vide.
- **Source** : `public/ts/controllers/directory.ts:372-378`

---

## Feature: Annuaire — fiche profil

### Règle: Masquage des données privées côté client (fix temporaire MOZO-77)
- **Contexte** : ouverture d'une fiche utilisateur
- **Condition** : pour chaque clé `SHOW_EMAIL`, `SHOW_PHONE`, `SHOW_BIRTHDATE`, `SHOW_HEALTH`, `SHOW_MOBILE` absente de `currentUser.visibleInfos`
- **Action** : le champ correspondant (`email`, `homePhone`, `birthdate`, `health`, `mobile`) est forcé à `undefined` côté client. Le commentaire indique un correctif temporaire en attendant que le backend filtre lui-même.
- **Source** : `public/ts/controllers/directory.ts:539-556, 583`

### Règle: Chargements conditionnels selon le profil du consultant
- **Contexte** : sélection d'un utilisateur (`selectUser`)
- **Condition / Action** :
  - `model.me.type !== 'ELEVE'` → chargement des infos administratives (`loadInfos` → `GET /directory/user/:id`)
  - consultant `ENSEIGNANT` ou `PERSEDUCNAT` ET utilisateur consulté de type `Relative` → chargement des enfants (`loadChildren`)
  - la visibilité est testée par `visibleUser()` : un `GET /directory/user/:id` qui réussit → `true`, toute erreur → `false`
- **Source** : `public/ts/controllers/directory.ts:558-598`, `public/ts/model.ts:740-747`

### Règle: Affichage des sections enfants / parents
- **Contexte** : fiche profil annuaire
- **Condition** :
  - enfants : `currentUser.childrenStructure.length > 0`
  - parents : `currentUser.relatives.length > 0` ET consulté de type `Student` ET consultant `ENSEIGNANT`/`PERSEDUCNAT`
- **Action** : sections affichées dans la fiche.
- **Source** : `public/ts/controllers/directory.ts:781-787`

### Règle: Structures rattachées ordonnées par responsabilité
- **Contexte** : `loadInfos()` construit `attachedStructures`
- **Condition / Action** : d'abord la structure administrative (`administrativeStructures[0]`, flag `admin: true`), puis les structures où l'utilisateur est ADML (`functions[0][1]`, flag `adml: true`, sans doublonner la structure admin), puis les autres écoles.
- **Source** : `public/ts/model.ts:749-788`

### Règle: Historique de navigation entre profils
- **Contexte** : navigation de fiche en fiche (ex. parent → enfant)
- **Condition** : `pastUsers` empile chaque utilisateur consulté (sans doublon consécutif)
- **Action** : `back()` dépile et réaffiche le profil précédent ; si la pile ne correspond pas, désélectionne (`deselectUser`).
- **Source** : `public/ts/controllers/directory.ts:569-570, 685-694`

### Règle: Affichage matière et position sur les dominos
- **Contexte** : carte utilisateur (domino)
- **Condition / Action** :
  - matière affichée si profil `Teacher` avec `subjects` non vide → première matière seulement
  - position : première position + « … » si plusieurs ; tooltip avec la liste complète jointe par « , »
- **Source** : `public/ts/controllers/directory.ts:600-622`

### Règle: Boutons messagerie conditionnés par les droits workflow
- **Contexte** : actions de la fiche profil/groupe
- **Condition** : `hasWorkflow('fr.openent.zimbra...|view')`, `hasWorkflow('org.entcore.conversation...|view')`, ou `hasWorkflow('org.entcore.portal...|optionalFeatureWriteToEmailProviderWordline')`
- **Action** : affichage du bouton d'écriture correspondant ; pour Wordline, redirection via `GET /optionalFeature/writeToEmailProvider/:endPoint?id=...&type=user|group|shareBookmark`.
- **Source** : `public/ts/controllers/directory.ts:1012-1018, 1128-1151`

---

## Feature: Favoris (ShareBookmarks)

### Règle: Détection de doublon avant ajout à un favori
- **Contexte** : ajout du profil courant (user ou groupe) à un favori existant
- **Condition** : un membre du favori a déjà le même `id`
- **Action** : pas de double ajout, pas de sauvegarde ; la notification de succès `directory.notify.confirmAddUser` est néanmoins affichée dans tous les cas.
- **Source** : `public/ts/controllers/directory.ts:476-499`

### Règle: Distinction users/groupes par la propriété `name`
- **Contexte** : sauvegarde d'un favori (`registerChanges`)
- **Condition** : `member.name` défini → groupe ; sinon → user
- **Action** : répartition dans `this.groups` / `this.users` avant l'envoi.
- **Source** : `public/ts/model.ts:117-127`

### Règle: Suppression automatique d'un favori vide (backend)
- **Contexte** : `GET /directory/sharebookmark/:id`
- **Condition** : la liste `members` est nulle ou vide
- **Action** : le backend supprime le favori et répond `404 empty.sharebookmark`.
- **Source** : `src/main/java/org/entcore/directory/controllers/ShareBookmarkController.java:79-101`

### Règle: Tri des favoris et de leurs groupes
- **Contexte** : liste des favoris et contenu d'un favori
- **Action** : favoris triés par `name` ; les groupes d'un favori sont triés par `groupType` puis par `sortName||name` (`sortGroups`).
- **Source** : `public/ts/controllers/directory.ts:222-227, 902-904`, `public/ts/model.ts:383-392`

### Règle: Auto-sélection du premier favori sur desktop
- **Contexte** : chargement de l'annuaire, annulation de création, suppression d'un favori
- **Condition** : viewport > `wideScreen` (`!ui.breakpoints.checkMaxWidth("wideScreen")`)
- **Action** : `selectFirstFavorite()` ; si aucun favori, `currentFavorite = null`.
- **Source** : `public/ts/controllers/directory.ts:128-130, 893-900`

### Règle: Droit workflow d'activation des favoris
- **Contexte** : visibilité de la fonctionnalité favoris
- **Condition** : droit `directory.allow.sharebookmarks` (route `GET /allowSharebookmarks` qui ne sert qu'à déclarer le droit)
- **Action** : la feature est masquée sans ce droit (`allowSharebookmarks` dans behaviours).
- **Source** : `ShareBookmarkController.java:160-165`, `public/ts/behaviours.ts:14`

---

## Feature: Mon Compte (UserBook)

### Règle: Routes et périmètre d'édition
- **Contexte** : `/userbook/mon-compte`
- **Condition / Action** :
  - `#/edit-me` (défaut) → édition userbook + visibility ; l'édition des infos (`edit.infos`) n'est activée que si `model.me.type !== 'ELEVE'` (un élève voit ses infos en lecture seule)
  - `#/edit-user/:id` → édition userbook + infos d'un tiers (usage admin)
  - `#/edit-user-infos/:id` → édition infos seulement
  - `#/themes` → préférences de thème uniquement
- **Source** : `public/ts/app.ts:9-25`, `public/ts/controllers/account.ts:24-69`

### Règle: Validations du carnet (backend)
- **Contexte** : `PUT /directory/userbook/:userId`
- **Condition / Action** : `400 Bad Request` si :
  - `motto` > 75 caractères
  - `mood` absent de la liste configurée (`user-book-data.moods` de la conf, qui contient toujours au moins `default`)
  - `health` > 1 000 caractères
  - un `hobby.values` > 80 caractères
- **Source** : `src/main/java/org/entcore/directory/controllers/UserController.java:96-98, 262-296`

### Règle: Liste des humeurs dynamique
- **Contexte** : sélecteur d'humeur
- **Condition** : `GET /directory/userbook/moods` retourne la liste configurée
- **Action** : remplace la liste par défaut `["default"]` ; le mood `default` est remonté en tête du sélecteur avec icône `none`. Sauvegarde uniquement si l'humeur a changé (`previousMood`).
- **Source** : `public/ts/model.ts:875-901`, `public/ts/controllers/account.ts:232-237, 363-371`

### Règle: Score de complexité du mot de passe
- **Contexte** : formulaire de changement de mot de passe
- **Condition / Action** :
  - longueur < 6 → score = longueur
  - sinon score = longueur, +5 si mélange chiffres/lettres, +5 si caractère spécial (hors `[a-zA-Z0-9- ]`)
  - affichage : < 12 → « weak », < 20 → « moderate », sinon « strong »
- **Source** : `public/ts/controllers/account.ts:415-441`

### Règle: Visibilité des boutons Mot de passe / OTP
- **Contexte** : section sécurité de Mon Compte
- **Condition** :
  - mot de passe : `account.id === me.userId` ET (non fédéré OU fédéré avec `federatedAddress` OU fédéré avec `hasPw`)
  - OTP : `account.id === me.userId` ET fédéré ET `hasApp`
- **Action** : affichage conditionnel ; l'OTP généré (`POST /auth/generate/otp`) doit faire exactement 8 caractères pour être affiché.
- **Source** : `public/ts/controllers/account.ts:273-283, 453-459`

### Règle: MFA ignorée
- **Contexte** : init de Mon Compte
- **Condition** : `ignoreMfa = (me.ignoreMFA OU mfaConfig vide) ET !needMfa` (croisement de `GET /auth/context` et `GET /auth/user/requirements`)
- **Action** : la section MFA est adaptée en conséquence.
- **Source** : `public/ts/controllers/account.ts:104-112`

### Règle: Validation email / mobile à l'initialisation
- **Contexte** : init de Mon Compte
- **Condition** : `emailState.valid === email` → email validé ; `mobileState.valid === mobile` → SMS validé
- **Action** : flags `validateMail` / `validateSms` pilotant les pictos de validation. La fermeture de la lightbox email recharge la page entière.
- **Source** : `public/ts/controllers/account.ts:114-136, 513-515`

### Règle: Format du numéro de téléphone
- **Contexte** : champs téléphone de Mon Compte
- **Condition** : regex `^((00|\+)?(?:[0-9] ?-?\.?){6,15})?$` (vide accepté)
- **Action** : validation client ; le backend valide en plus le mobile via `PhoneValidation` et le normalise en E.164.
- **Source** : `public/ts/controllers/account.ts:97-102`, `UserController.java:154-165`

### Règle: Gestion d'erreur du login alias
- **Contexte** : sauvegarde du login alias
- **Condition** : erreur 400 dont le message contient « already exists » ou « existe déjà »
- **Action** : notification `directory.notify.loginUpdate.error.alreadyExists` ; sinon le message d'erreur brut est notifié.
- **Source** : `public/ts/controllers/account.ts:294-309`

### Règle: Anti double-soumission de la lightbox d'édition
- **Contexte** : lightbox d'édition d'un champ (`saveEditLightbox`)
- **Condition** : `isEditLightboxSaving` à `true`
- **Action** : la soumission est ignorée tant que la requête précédente n'est pas terminée.
- **Source** : `public/ts/controllers/account.ts:311-337`

### Règle: Visibilité des informations personnelles (toggle public/privé)
- **Contexte** : pictos œil de Mon Compte
- **Condition / Action** : bascule `public` ↔ `prive` par info via `GET /api/edit-user-info-visibility?info=&state=` ; pour les hobbies via `GET /api/set-visibility?value=&category=` (valeurs `PUBLIC`/`PRIVE` en majuscules).
- **Source** : `public/ts/controllers/account.ts:380-400`

### Règle: Suppression de la photo = sauvegarde immédiate
- **Contexte** : avatar de Mon Compte
- **Action** : `removePicture`/`resetAvatar` vide `picture` puis appelle directement `saveChanges()` ; l'upload (`PUT /directory/avatar/:id`) incrémente `pictureVersion` et rafraîchit l'avatar global (`ui.updateAvatar()`).
- **Source** : `public/ts/controllers/account.ts:289-292, 402-409`, `public/ts/model.ts:810-822`

### Règle: Détection Admin local / central
- **Contexte** : affichages réservés dans Mon Compte
- **Condition** : `isAdmx = me.functions.ADMIN_LOCAL.scope` existe OU `me.functions.SUPER_ADMIN` existe
- **Source** : `public/ts/controllers/account.ts:138-145`

### Règle: Affichage enfants / parents dans Mon Compte (différent de l'annuaire)
- **Condition** :
  - enfants : `childrenStructure.length > 0` ET `me.type ∈ {PERSRELELEVE, ENSEIGNANT, PERSEDUCNAT}`
  - parents : `relatives.length > 0` ET `me.type === 'ELEVE'`
- **Source** : `public/ts/controllers/account.ts:461-467`

### Règle: Thèmes groupés
- **Contexte** : onglet thèmes
- **Condition** : `theme-conf.js` chargé ; si le thème courant appartient à un `group`, seuls les thèmes du même groupe sont proposés
- **Action** : sélection exclusive (un seul thème actif), persistée via `PUT /userbook/preference/theme`. Au premier chargement sans préférence, le skin courant est enregistré comme préférence.
- **Source** : `public/ts/controllers/account.ts:147-187, 247-258`

---

## Feature: Fusion de comptes

### Règle: Format de la clé de fusion
- **Contexte** : champ « fusionner mon compte » de Mon Compte
- **Condition** : la clé doit matcher le format UUID `[0-9a-fA-F]{8}-(4 groupes)-[0-9a-fA-F]{12}`
- **Action** : sinon notification `invalid.merge.keys` sans appel réseau ; en cas de succès le champ est vidé et `mergedLogins` est affiché.
- **Source** : `public/ts/controllers/account.ts:469-497`, `public/ts/model.ts:839-865`

### Règle: Droits workflow de fusion
- **Condition** : `user.generate.merge.key` pour générer (`GET /directory/duplicate/user/mergeKey`), `user.merge.by.key` pour fusionner (`POST /directory/duplicate/user/mergeByKey`)
- **Source** : `UserController.java:995-1011`, `public/ts/behaviours.ts:12-13`

---

## Feature: DiscoverVisible (mise en relation inter-établissements)

### Règle: Activation de l'onglet
- **Contexte** : init de l'annuaire
- **Condition** : `GET /communication/discover/visible/profiles` retourne une liste non vide ET le profil courant correspond (`Teacher` ↔ me.type `Teacher`/`ENSEIGNANT`, `Personnel` ↔ `Personnel`/`PERSEDUCNAT`)
- **Action** : onglet affiché, groupes chargés, structures de découverte chargées. Si un seul profil accepté, il est pré-coché (`defaultProfiles`). Un 403 sur l'API profils est avalé (retourne `[]`).
- **Source** : `public/ts/controllers/directory.ts:1174-1201, 1443-1454`, `public/ts/model.ts:537-545`

### Règle: Garde-fou de recherche
- **Contexte** : recherche d'utilisateurs visibles
- **Condition** : aucune structure cochée ET champ texte vide
- **Action** : `notify.info("userbook.discover.visible.users.search.filter.empty")`, recherche annulée.
- **Source** : `public/ts/controllers/directory.ts:1204-1212`

### Règle: Mise à jour locale du lien de communication
- **Contexte** : liaison (`add/commuting`) ou déliaison (`remove/commuting`)
- **Condition** : la réponse contient `number > 0`
- **Action** : `hasCommunication` est mis à jour dans les deux listes (résultats de recherche ET membres du groupe affiché) ; l'utilisateur modifié est déplacé en fin de liste (filter + push).
- **Source** : `public/ts/controllers/directory.ts:1398-1441`

### Règle: Sortie d'un groupe = mise à jour des membres sans soi
- **Contexte** : bouton « quitter le groupe »
- **Action** : `PUT .../group/:id/users` avec `oldUsers` = tous les membres et `newUsers` = tous sauf `model.me.userId`.
- **Source** : `public/ts/controllers/directory.ts:1456-1464`

### Règle: Édition de groupe — renommage seulement si modifié
- **Contexte** : sauvegarde d'un groupe discoverVisible en mode édition
- **Condition** : `groupName` saisi ≠ nom actuel
- **Action** : sinon le PUT de renommage est sauté ; la mise à jour des membres est toujours envoyée.
- **Source** : `public/ts/controllers/directory.ts:1382-1391`

### Règle: Tracking des événements
- **Action** : `POST /infra/event/web/store` avec `event-type` ∈ {`DISCOVER_VISIBLE_CREATE_GROUP`, `DISCOVER_VISIBLE_LINK_USER`, `DISCOVER_VISIBLE_UNLINK_USER`, `DISCOVER_VISIBLE_EXIT_GROUP`} ; échec silencieux.
- **Source** : `public/ts/model.ts:595-611`

---

## Feature: Ma Classe

### Règle: Affichage selon le nombre de classes
- **Contexte** : route `#/myClass` (route par défaut de l'annuaire)
- **Condition / Action** :
  - 0 classe → template `no-classroom`
  - 1 classe → sélection automatique, template `mono-class`
  - plusieurs → template `class-list` avec recherche (placeholder `class.search`)
- **Source** : `public/ts/controllers/directory.ts:175-207`

### Règle: Liste des classes issue de la session OAuth
- **Contexte** : sync des classes
- **Condition** : `GET /auth/oauth2/userinfo?version=v2.0` fournit `classes` (ids) et `realClassesNames` (noms) en parallèle
- **Action** : zip par index ; ⚠️ si les longueurs diffèrent, `results` est d'abord assigné à `[]` mais **est ensuite écrasé par le map** (le garde-fou est inopérant — bug latent).
- **Source** : `public/ts/model.ts:307-326`

### Règle: Recherche multi-mots tolérante aux accents
- **Contexte** : recherche de classes/écoles/utilisateurs dans Ma Classe
- **Action** : chaque mot de la recherche doit matcher (accents supprimés, insensible à la casse) ; pour les utilisateurs, testé sur `displayName`, nom inversé, `firstName lastName` et `lastName firstName`.
- **Source** : `public/ts/model.ts:273-282, 500-522`

---

## Feature: Class Admin (`/directory/class-admin`)

### Règle: Accès et lightbox de choix de classe
- **Contexte** : ouverture de la console class-admin
- **Condition** : droit workflow `classadmin.address` ; si le réseau de l'utilisateur n'a aucune classe (ou 1 école sans classes) alors qu'il a des structures
- **Action** : ouverture de la lightbox `choose-class` : choix d'une école → liste des classes → sélection multiple → `PUT /directory/class/add-self` puis **rechargement complet de la page** (les règles de communication ont changé).
- **Source** : `public/ts/admin/controller.ts:49-60`, `admin/delegates/choose-class.ts:51-58`, `DirectoryController.java:98-104`

### Règle: Champs obligatoires à la création d'un utilisateur selon le profil
- **Contexte** : formulaire de création (lightbox `admin/create-user/form`)
- **Condition / Action** :
  - `Student` : nom, prénom **et date de naissance** obligatoires
  - `Teacher`, `Personnel`, `Relative` : nom et prénom obligatoires
  - date de naissance bornée : entre il y a 100 ans et aujourd'hui
  - champ mobile visible uniquement pour le type `Relative`
  - changer le type **réinitialise tout le formulaire**
- **Source** : `public/ts/admin/delegates/userCreate.ts:88-89, 189-237, 427-430`

### Règle: Avertissement « parent sans enfant »
- **Contexte** : soumission d'une création de `Relative`
- **Condition** : `checkRelations` actif ET aucun enfant rattaché
- **Action** : lightbox d'avertissement `no-relatives` ; l'utilisateur peut forcer la création (le check n'est désactivé qu'une seule fois puis réarmé).
- **Source** : `public/ts/admin/delegates/userCreate.ts:268-271, 298-309`

### Règle: Détection de doublons à la création
- **Contexte** : soumission d'une création de `Student` ou `Relative`
- **Condition** : recherche du nom dans `/communication/visible` (périmètre = structure de la classe), filtrée par profil identique — le filtre backend ne suffit pas car il n'inclut pas les élèves sans classe (#24057)
- **Action** : si des homonymes existent → lightbox « doublon » listant les candidats avec leurs classes (`aucune` / `une` / `plusieurs` libellés dédiés). Trois issues :
  - **rattacher** l'existant à la classe (`addExistingUserToClass`, + liaisons parent/enfant éventuelles)
  - **déplacer** l'existant (changement de classe `withRelative: true`)
  - **créer quand même** un nouveau compte
- **Source** : `public/ts/admin/delegates/userCreate.ts:310-399`

### Règle: Recherche d'enfants limitée au profil Student
- **Contexte** : rattachement d'enfants dans le formulaire de création (type `Relative`)
- **Condition** : la recherche (debounce 450 ms, déclenchée dès 1 caractère, ou au blur du champ nom) filtre côté client sur `profile == "Student"` (#24229)
- **Source** : `public/ts/admin/delegates/userCreate.ts:118-142, 173-178, 215-227`

### Règle: Suppression réservée aux comptes de sources manuelles
- **Contexte** : action « supprimer » du toaster de sélection
- **Condition** : tous les utilisateurs sélectionnés ont une `source` ∈ {`MANUAL`, `CLASS_PARAM`, `BE1D`, `CSV`}
- **Action** : sinon le bouton supprimer est masqué (les comptes issus de l'annuaire académique AAF ne sont pas supprimables ici).
- **Source** : `public/ts/admin/delegates/actions.ts:54-58`

### Règle: Actions bloquer/débloquer/supprimer conditionnées par l'état de la sélection
- **Contexte** : toaster d'actions
- **Condition / Action** : les actions ne s'affichent que si la sélection est homogène — tous activés (`selectedUsersAreActivated` : aucun `activationCode`), tous non activés, tous bloqués, tous non bloqués. Après action, la classe est rechargée et la sélection vidée.
- **Source** : `public/ts/admin/delegates/actions.ts:42-89`

### Règle: Renvoi de mot de passe nécessite l'email du déclencheur
- **Contexte** : « réinitialiser le mot de passe » (envoi de mail)
- **Condition** : `model.me.email` renseigné
- **Action** : sinon `notify.error("classAdmin.reset.error")` ; sinon `POST /auth/sendResetPassword` **avec l'email de l'enseignant** comme destinataire (une requête par utilisateur).
- **Source** : `public/ts/admin/controller.ts:102-121`, `admin/service.ts:363-372`

### Règle: Génération de codes temporaires seulement pour les comptes activés
- **Contexte** : « générer des codes de renouvellement »
- **Condition** : seuls les utilisateurs **sans** `activationCode` (déjà activés) reçoivent un code (`POST /auth/massGeneratePasswordRenewalCode`)
- **Action** : les codes et dates sont affichés puis imprimables ; les non-activés gardent leur code d'activation.
- **Source** : `public/ts/admin/delegates/actions.ts:118-131`

### Règle: Statut d'activation affiché par priorité
- **Contexte** : colonne « code » de la liste
- **Condition / Action** (dans cet ordre) : bloqué → libellé « bloqué » ; `activationCode` présent → le code ; `resetCode` présent → libellé avec le code de réinitialisation ; sinon → « activé ». Tri spécifique sur cette colonne : resetCode < activationCode < bloqué < activé.
- **Source** : `public/ts/admin/delegates/userList.ts:81-110, 234-255`

### Règle: Normalisation des noms à la saisie
- **Contexte** : modèle `User` du class-admin
- **Action** : `safeLastName` = majuscules sans caractères spéciaux ; `safeFirstName` = Capitalisation De Chaque Mot sans caractères spéciaux ; `safeDisplayName` = sans caractères spéciaux.
- **Source** : `public/ts/admin/model.ts:144-167`

### Règle: Format du login alias (class-admin)
- **Contexte** : édition du login dans la fiche utilisateur
- **Condition** : regex `^[a-z\d\.-]*$` (minuscules, chiffres, points, tirets)
- **Action** : si l'alias saisi est vide, retour au `originalLogin`. Erreur réseau → notification « alias déjà existant ».
- **Source** : `public/ts/admin/delegates/userInfos.ts:244-269`

### Règle: ADML modifiant ses propres coordonnées → redirection Mon Compte
- **Contexte** : édition de l'email ou du mobile dans la fiche class-admin
- **Condition** : `Me.session.functions.ADMIN_LOCAL` ET utilisateur édité = soi-même
- **Action** : redirection forcée vers `/userbook/mon-compte#/edit-me` (l'édition passe par le parcours sécurisé de validation).
- **Source** : `public/ts/admin/delegates/userInfos.ts:294-302, 347-355`

### Règle: Email verrouillé (`lockedEmail`)
- **Contexte** : fiche utilisateur class-admin
- **Condition** : `selectedUser.lockedEmail` ET consultant non SUPER_ADMIN ET utilisateur ≠ soi-même
- **Action** : `isForbidden()` → édition interdite.
- **Source** : `public/ts/admin/delegates/userInfos.ts:412-419`

### Règle: Gestion du secret TOTP
- **Contexte** : fiche utilisateur class-admin
- **Action** : saisie d'un secret TOTP (`PUT /directory/user/:id` avec `totp`) → `hasTotp = true` ; suppression → `totp: null`, `hasTotp = false`. Notifications dédiées succès/erreur.
- **Source** : `public/ts/admin/delegates/userInfos.ts:379-411`

### Règle: Userbook visible seulement pour les comptes activés
- **Contexte** : fiche utilisateur class-admin
- **Condition** : `!selectedUser.activationCode`
- **Action** : la section userbook (humeur, devise, photo) n'apparaît que pour un compte activé. Enfants affichés si type `Relative`, parents si type `Student`.
- **Source** : `public/ts/admin/delegates/userInfos.ts:420-428`

### Règle: Liaison/déliaison parent-enfant avec mise à jour locale
- **Contexte** : fiche d'un `Relative` — rattacher/détacher des enfants
- **Action** : `PUT|DELETE /directory/user/:studentId/related/:relativeId` ; les résultats de recherche excluent les enfants déjà liés ; verrou `linking` anti double-clic.
- **Source** : `public/ts/admin/delegates/userInfos.ts:436-490`, `admin/service.ts:270-287`

### Règle: Opérations de classe propagées aux parents (`withRelative`)
- **Contexte** : changement/liaison/déliaison de classe d'élèves
- **Condition** : `withRelative: true`
- **Action** : les ids des parents (`safeRelativeIds`) sont ajoutés à l'opération, avec la même classe d'origine. Le changement de classe utilise un endpoint atomique `PUT /directory/class/:toClass/change` (l'ancien enchaînement link puis unlink est commenté).
- **Source** : `public/ts/admin/service.ts:288-359`

### Règle: Import CSV de classe
- **Contexte** : lightbox d'import class-admin
- **Condition** : droit `allowClassAdminCSVImport` ; fichier envoyé en multipart avec `classExternalId`
- **Action** : verrou `importing` anti double-envoi ; gestion d'erreur : « code + index de ligne » traduit, « already exists » → `directory.import.already.exists`, sinon message brut. Côté backend la route exige `TeacherOfClass`.
- **Source** : `public/ts/admin/controller.ts:125-141`, `admin/service.ts:218-256`, `ImportController.java:180-182`

### Règle: Publipostage / exports
- **Contexte** : exports class-admin (PDF détaillé, PDF simple, mail, CSV)
- **Condition / Action** :
  - liste d'ids vide → `notify.info("classAdmin.email.nousers"|"classAdmin.report.nousers")`, pas d'appel
  - le thème de l'utilisateur est joint à la requête `POST /directory/class-admin/massmail`
  - export PDF individuel (`type: "pdf"`) ou famille (`type: "simplePdf"` avec l'élève + ses parents)
  - export CSV des codes d'activation par onglet : `GET /directory/class/:id/users?type=<profil>&format=csv`
- **Source** : `public/ts/admin/service.ts:380-410`, `admin/delegates/userInfos.ts:227-243`, `admin/delegates/userList.ts:266-269`

### Règle: Bouton ONDE limité au français
- **Contexte** : import ONDE (lightbox d'import)
- **Condition** : `currentLanguage == "fr"`
- **Source** : `public/ts/admin/controller.ts:145-148`

### Règle: Édition du nom de classe
- **Contexte** : en-tête class-admin
- **Action** : `PUT /directory/class/:id` avec `{name, level}` ; la collection locale `classrooms` est mise à jour par id (l'objet `$scope.selectedClass` peut avoir changé entre temps — commentaire « Big mess »).
- **Source** : `public/ts/admin/controller.ts:155-176`, `admin/service.ts:183-186`

### Règle: Droits workflow granulaire du class-admin
- **Contexte** : visibilité des actions
- **Condition** : droits déclarés dans behaviours : `allowClassAdminAddUsers`, `allowClassAdminResetPassword`, `allowClassAdminBlockUsers`, `allowClassAdminDeleteUsers`, `allowClassAdminUnlinkUsers`, `allowClassAdminCSVImport`.
- **Source** : `public/ts/behaviours.ts:16-21`

### Règle: Tracking Matomo (#47174)
- **Contexte** : toutes les actions sensibles du class-admin
- **Action** : événements trackés — création (CREATE/ADD/ATTACH/MOVE/DOUBLE par profil), blocage/déblocage, suppression, retrait de classe, modification login/mot de passe/code, publipostage (par type et par profil), import CSV (succès/erreur).
- **Source** : `public/ts/admin/delegates/events.ts`, usages dans `controller.ts`, `userCreate.ts`, `actions.ts`, `userInfos.ts`, `userExport.ts`

---

## Feature: Widget Anniversaires

### Règle: Anniversaires du mois courant, triés par jour
- **Contexte** : widget `birthday` du portail
- **Condition** : `moment(birthDate).month() === moment().month()`
- **Action** : filtre puis tri croissant par jour du mois.
- **Source** : `public/ts/birthday.ts:43-54`

### Règle: Classe par défaut persistée en préférence
- **Contexte** : sélecteur de classe du widget
- **Condition** : préférence `userPreferencesBirthdayClass` lue depuis `/userbook/user-preferences` ; si la classe enregistrée n'existe plus, repli sur la première classe
- **Action** : sauvegarde via `GET /userbook/api/edit-userbook-info?prop=userPreferencesBirthdayClass&value=`.
- **Source** : `public/ts/birthday.ts:39-72`

---

## Feature: Sniplet « facebook » (trombinoscope de groupes)

### Règle: Composition de groupes par établissement
- **Contexte** : sniplet configurable dans les pages (nom interne `facebook`)
- **Action** : choix d'une structure → chargement de `profileGroups + manualGroups` ; les groupes ajoutés à la source affichent leurs membres via `GET /userbook/visible/users/:groupId`. Les noms de groupes suffixés sont traduits par segments.
- **Source** : `public/ts/behaviours.ts:24-92`

---

## Règles transverses backend notables

### Règle: Restriction de mise à jour des noms et positions (PUT /user/:id)
- **Condition** : appelant sans fonction `SUPER_ADMIN` ni `ADMIN_LOCAL`
- **Action** : `positionIds` est retiré du corps ; `lastName`/`firstName` sont aussi retirés sauf si l'appelant a la fonction `CLASS_ADMIN`.
- **Source** : `UserController.java:144-152`

### Règle: Notification de sécurité sur changement d'email/mobile
- **Contexte** : `PUT /user/:userId` modifiant `email` ou `mobile`
- **Condition** : ancienne et nouvelle valeurs non vides et différentes
- **Action** : un message d'avertissement est envoyé à l'**ancienne** adresse (EmailValidation/MobileValidation `sendWarning`).
- **Source** : `UserController.java:215-239`

### Règle: Session recréée après modification
- **Contexte** : `PUT /user/:id`, `PUT /userbook/:id`, `PUT /user/login/:id`
- **Action** : l'attribut de session `PERSON_ATTRIBUTE` est invalidé et la session est recréée ; le cookie `userbookVersion` est rafraîchi après mise à jour du carnet (cache-busting avatar/infos).
- **Source** : `UserController.java:184-209, 297-331`
