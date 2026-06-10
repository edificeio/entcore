# PERMISSIONS — module `directory`

> Sources : annotations `@SecuredAction` / `@ResourceFilter` / `@MfaProtected` des contrôleurs Java, filtres de `security/`, droits workflow déclarés dans `public/ts/behaviours.ts`.

Légende : **AUTH** = `ActionType.AUTHENTICATED` (workflow éventuel entre parenthèses), **RES** = `ActionType.RESOURCE` (filtre de ressource), **WF** = droit workflow nommé, **MFA** = `@MfaProtected`.

---

## Filtres de sécurité du module (`security/`)

| Filtre | Autorise |
|--------|----------|
| `SuperAdminFilter` (common) | SUPER_ADMIN uniquement |
| `AdminFilter` (common) | SUPER_ADMIN ou ADMIN_LOCAL |
| `AdminStructureFilter` | Admin dans le périmètre de la structure ciblée |
| `AdmlOfStructure` / `AdmlOfStructures` | ADML de la/des structure(s) du path |
| `AdmlOfStructuresByExternalId` / `AdmlOfStructuresByUAI` | ADML résolu par externalId / UAI |
| `AdmlOfStructureOrClass` | ADML de la structure ou de la classe |
| `AdmlOfStructureOrClassOrTeachOfUser` | ADML, ou enseignant de l'utilisateur ciblé |
| `AdmlOfStructureWithoutEDTInit` | ADML, structure sans EDT initialisé |
| `AdmlOfUser` / `AdmlOfTwoUsers` | ADML de l'utilisateur / des deux utilisateurs ciblés |
| `AnyAdminOfUser` | Tout admin ayant autorité sur l'utilisateur |
| `TeacherOfUser` / `TeacherOfClass` / `TeacherOfUserFromDifferentClass` | Enseignant de l'utilisateur / de la classe |
| `TeacherInAllStructure` | Enseignant dans toutes les structures concernées |
| `RelativeStudentFilter` | Lien parent-élève valide |
| `UserAccess` / `UserAccessOrVisible` | Soi-même/admin/enseignant ; variante « OrVisible » accepte aussi un utilisateur visible mais **filtre les champs sensibles** |
| `UserInStructure` | Utilisateur membre de la structure |
| `AddFunctionFilter` | Contrôle l'ajout de fonctions (ADML ne peut donner que certaines fonctions) |
| `UserbookCsrfFilter` | Protection CSRF des API userbook |
| `DirectoryResourcesProvider` | Provider central des règles ressources du module |

**Source** : `src/main/java/org/entcore/directory/security/`

---

## Ressource: User

| Action | Garde | Condition / note | Source |
|--------|-------|------------------|--------|
| GET /user/:userId | RES + `UserAccessOrVisible` | En accès « visible » : ~40 champs sensibles retirés (liste complète ci-dessous) | `UserController.java:333-358` |
| GET /user/:userId/groups | RES + `UserAccessOrVisible` | manual-groups en option | `UserController.java:360` |
| GET /user/:userId/children | RES + `UserAccessOrVisible` | | `UserController.java:747` |
| PUT /user/:userId | RES + **MFA** | Non-admin : `positionIds` retiré ; `lastName`/`firstName` retirés sauf CLASS_ADMIN ; mobile validé/normalisé E.164 | `UserController.java:134-165` |
| PUT /user/login/:userId | RES + `SuperAdminFilter` + **MFA** | Changement du login canonique (≠ alias) | `UserController.java:241` |
| DELETE /user | RES | | `UserController.java:518` |
| POST /user/delete | RES + `TeacherOfUser` | Suppression par l'enseignant (class-admin) | `UserController.java:526` |
| PUT /restore/user | RES + `AnyAdminOfUser` | | `UserController.java:543` |
| GET /export/users | RES | | `UserController.java:552` |
| POST /user/function/:userId | RES + `AddFunctionFilter` | | `UserController.java:637` |
| DELETE /user/function/:userId/:function | RES | | `UserController.java:730` |
| GET /user/:userId/functions | RES | | `UserController.java:739` |
| POST\|PUT /:structure/user/:userId/headteacher | RES + `AdmlOfStructures` | Professeur principal | `UserController.java:665, 683` |
| POST\|PUT /:structure/user/:userId/direction | RES + `AdmlOfStructures` | Fonction de direction | `UserController.java:699, 714` |
| PUT\|DELETE /user/:studentId/related/:relativeId | RES + `RelativeStudentFilter` | Lien parent-enfant | `UserController.java:857, 886` |
| POST\|DELETE /user/group/:userId/:groupId | RES | Ajout/retrait d'un groupe | `UserController.java:755, 779` |
| GET /user/group/:groupId | RES | | `UserController.java:788` |
| GET /user/adml/list/:structureId | WF `user.adml.list` | | `UserController.java:796` |
| GET /user/admin/list | RES | | `UserController.java:803` |
| GET /list/isolated | RES | Utilisateurs sans structure | `UserController.java:479` |
| GET /user/structures/list | RES + `AdmlOfStructuresByUAI` | | `UserController.java:933` |
| GET /user/list/by/structure | RES + `AdmlOfStructuresByUAI` | | `UserController.java:1278` |
| GET /user/level/list | RES + `SuperAdminFilter` | | `UserController.java:1051` |
| GET /user/:userId/attachment-school | AUTH | | `UserController.java:1077` |
| PUT /user/attachments/infos | RES + `SuperAdminFilter` | | `UserController.java:1287` |

**Champs retirés en accès « visible »** (`GET /user/:userId` quand l'attribut `visibleCheck` vaut `"true"`) : `activationCode`, `firstName`, `lastName`, `mobile`, `mobilePhone`, `lastLogin`, `created`, `modified`, `ine`, `email`, `emailAcademy`, `workPhone`, `homePhone`, `country`, `zipCode`, `address`, `postbox`, `city`, `otherNames`, `title`, `surname`, `functions`, `headTeacher`, `relativeAddress`, `classCategories`, `subjectTaught`, `needRevalidateTerms`, `joinKey`, `isTeacher`, `structures`, `type`, `children`, `parents`, `functionalGroups`, `startDateStruct`, `endDateStruct`, `administrativeStructures`, `subjectCodes`, `fieldOfStudyLabels`, `startDateClasses`, `scholarshipHolder`, `attachmentId`, `fieldOfStudy`, `module`, `transport`, … (`UserController.java:339-349`).

## Ressource: Doublons & fusion

| Action | Garde | Source |
|--------|-------|--------|
| GET /duplicates | RES + `AdmlOfStructures` | `UserController.java:922` |
| PUT /duplicate/merge/:userId1/:userId2 | RES + `AdmlOfTwoUsers` | `UserController.java:910` |
| DELETE /duplicate/ignore/:userId/:userId2 | RES + `AdmlOfUser` | `UserController.java:895` |
| POST /duplicate/generate/mergeKey/:userId | RES + `AdmlOfUser` | `UserController.java:987` |
| GET /duplicate/user/mergeKey | WF `user.generate.merge.key` | `UserController.java:995` |
| POST /duplicate/user/mergeByKey | WF `user.merge.by.key` | `UserController.java:1010` |
| POST /duplicate/user/unmergeByLogins | RES + `SuperAdminFilter` | `UserController.java:1030` |
| POST /duplicates/mark | WF `directory.duplicates.mark` | `DirectoryController.java:183` |

## Ressource: UserBook & préférences

| Action | Garde | Condition / note | Source |
|--------|-------|------------------|--------|
| GET /userbook/:userId | RES | | `UserController.java:413` |
| PUT /userbook/:userId | RES | Validations motto/mood/health/hobbies | `UserController.java:262` |
| PUT /avatar/:userId | RES | | `UserController.java:420` |
| GET /userbook/moods | AUTH (`userbook.authent`) | | `UserController.java:128` |
| GET /avatar/:id | **public** (aucune annotation de sécurité) | Avatars accessibles sans session | `UserBookController.java:631-646` |
| GET /userbook/mon-compte, /birthday, /mood, /annuaire, /api/search, /api/person, /api/class, /api/edit-userbook-info, /api/set-visibility, /api/edit-user-info-visibility | AUTH (`userbook.authent`) | | `UserBookController.java:165-422, 690` |
| GET /userbook/structures | AUTH (`userbook.my.structures`) | | `UserBookController.java:252` |
| GET /userbook/structure/:structId | AUTH (`userbook.structure.classes.personnel`) | | `UserBookController.java:268` |
| GET /userbook/visible/users/:groupId | AUTH (`userbook.visible.users.group`) | | `UserBookController.java:315` |
| GET /userbook/user-preferences | AUTH (`userbook.preferences`) | | `UserBookController.java:709` |
| GET\|PUT /userbook/preference/:application, GET\|PUT /userbook/api/preferences | AUTH (`user.preference`) | | `UserBookController.java:730-837` |
| GET /userbook/search/criteria, /search/criteria/:structureId/classes | AUTH | | `UserBookController.java:900, 939` |
| GET /userbook/person/birthday | AUTH (`userbook.authent`) | | `UserBookController.java:648` |
| — | WF `userbook.show.motto.mood` / `userbook.switch.theme` | Droits d'affichage humeur-devise / changement de thème | `UserBookController.java:454, 457` |

## Ressource: Validation email / mobile

| Action | Garde | Source |
|--------|-------|--------|
| GET\|PUT\|POST /user/mailstate | AUTH | `UserController.java:1159-1202` |
| GET\|PUT\|POST /user/mobilestate | AUTH | `UserController.java:1085-1127` |

## Ressource: ShareBookmark (favoris)

| Action | Garde | Condition | Source |
|--------|-------|-----------|--------|
| POST /sharebookmark | AUTH | Propriétaire implicite (userId de session) | `ShareBookmarkController.java:46` |
| PUT\|DELETE /sharebookmark/:id, GET /sharebookmark/:id\|all | AUTH | Portée limitée à ses propres favoris ; favori vide auto-supprimé (404) | `ShareBookmarkController.java:58-158` |
| GET /sharebookmark/:userId/:id | RES + `AdminFilter` | Consultation des favoris d'autrui | `ShareBookmarkController.java:108` |
| GET /allowSharebookmarks | WF `directory.allow.sharebookmarks` | Route déclarative (no-op) | `ShareBookmarkController.java:160` |

## Ressource: Class

| Action | Garde | Source |
|--------|-------|--------|
| GET\|PUT /class/:classId | RES | `ClassController.java:76, 83` |
| DELETE /class/:classId | RES + `SuperAdminFilter` | `ClassController.java:95` |
| POST /class/:classId/user | RES | `ClassController.java:111` |
| GET /class/:classId/users | RES | `ClassController.java:150` |
| PUT /class/add-self | AUTH | `ClassController.java:198` |
| PUT /class/:classId/add/:userId | RES | `ClassController.java:263` |
| PUT /class/:classId/apply | RES | `ClassController.java:282` |
| PUT /class/:classId/link/:userId, /link, /unlink, /change ; DELETE /unlink/:userId | RES + `AdmlOfStructureOrClassOrTeachOfUser` | `ClassController.java:342-427` |
| GET /class/admin/list | RES | `ClassController.java:435` |
| GET /class/users/detached, /class/users/visibles | AUTH | `ClassController.java:452, 467` |

## Ressource: Group

| Action | Garde | Source |
|--------|-------|--------|
| GET /group/admin/list, /group/admin/funcAndDisciplines | RES | `GroupController.java:53, 107` |
| POST /group ; PUT\|DELETE /group/:groupId | RES | `GroupController.java:127-205` |
| PUT /group/:groupId/users/add\|delete | RES + `AdminFilter` | `GroupController.java:206, 241` |
| GET /group/:groupId, /group/communityGroup | AUTH | `GroupController.java:258, 296` |
| POST /group/:groupId/setManualGroupAutolinkUsersPositions, /group/updateManualGroupsByUserPositions | RES + `AdminFilter` | `GroupController.java:309, 336` |

## Ressource: Structure

| Action | Garde | Source |
|--------|-------|--------|
| PUT /structure/:structureId | RES + **MFA** | `StructureController.java:102` |
| PUT /structure/:id/link/:userId ; DELETE /unlink/:userId | RES + **MFA** | `StructureController.java:124, 156` |
| GET /structure/admin/list | RES | `StructureController.java:165` |
| PUT\|DELETE /structure/:id/parent/:parentId | WF `structure.define.parent` / `structure.remove.parent` | `StructureController.java:180, 207` |
| GET /structure/:id/children | RES + **MFA** | `StructureController.java:215` |
| GET /structures | WF `structure.list.all` | `StructureController.java:223` |
| GET /structure/:id/levels ; PUT levels-of-education, distributions | RES (+MFA) | `StructureController.java:277-309` |
| GET /structure/:id/massMail/users, /allUsers ; GET massmessaging/template | RES + **MFA** | `StructureController.java:325-375` |
| GET /structure/massMail/:userId/:type | RES + `AnyAdminOfUser` + **MFA** | `StructureController.java:411` |
| GET /structure/:id/massMail/process/:type | RES + **MFA** | `StructureController.java:475` |
| POST /class-admin/massmail | RES | `StructureController.java:562` |
| GET /structure/:id/metrics | RES + `AdmlOfStructure` + **MFA** | `StructureController.java:636` |
| GET /structure/:id/sources, /aaffunctions | RES + `AdminFilter` + **MFA** | `StructureController.java:644, 653` |
| GET /structure/:id/quicksearch/users, /users, /removedUsers | RES + `AdminStructureFilter` + **MFA** | `StructureController.java:662-697` |
| PUT structure/:id/profile/block | RES + `AdminStructureFilter` + **MFA** | `StructureController.java:710` |
| PUT /structure/:id/resetName | RES + `AdmlOfStructure` + **MFA** | `StructureController.java:787` |
| PUT /structure/check/uai, /structure/:id/duplicate, /structure/check/gar, /structure/gar/activate ; GET /structure/:id/contacts | RES + `SuperAdminFilter` + **MFA** | `StructureController.java:828-944` |

## Ressource: Import (wizard) & imports de classe

| Action | Garde | Source |
|--------|-------|--------|
| GET /wizard ; POST /wizard/column/mapping, /wizard/classes/mapping, /wizard/validate, /wizard/import ; PUT /wizard/validate/:id, /wizard/import/:id ; GET /wizard/import/:id ; POST\|PUT /wizard/update/:id/:profile ; DELETE /wizard/update/:id/:profile/:line | RES + `AdminFilter` + **MFA** (TODO du code : vérifier le propriétaire de l'import) | `ImportController.java:72-265` |
| POST /import/:userType/class/:classId | RES + `TeacherOfClass` | `ImportController.java:180` |
| POST /import | RES + `AdmlOfStructuresByExternalId` | `DirectoryController.java:138` |

## Ressource: Timetable (EDT)

| Action | Garde | Source |
|--------|-------|--------|
| GET /timetable | RES + `AdminFilter` + **MFA** | `TimetableController.java:105` |
| GET /timetable/courses/:structureId | RES + `SuperAdminFilter` + **MFA** | `TimetableController.java:113` |
| GET /timetable/courses/:id/:begin/:end, /subjects/:id, /subjects/:id/group | RES + `UserInStructure` | `TimetableController.java:133-168` |
| PUT /timetable/init/:structureId | RES + `AdmlOfStructureWithoutEDTInit` + **MFA** | `TimetableController.java:176` |
| GET\|PUT /timetable/classes/:id, /groups/:id | RES + `AdmlOfStructure` + **MFA** | `TimetableController.java:189-221` |
| DELETE /timetable/import/progress | RES + `SuperAdminFilter` + **MFA** | `TimetableController.java:231` |
| POST /timetable/import/:structureId, /import/:type/:structureId, /import/groups/:structureId | RES + `AdminFilter` + **MFA** | `TimetableController.java:240-266` |

## Ressource: Profils & fonctions

| Action | Garde | Source |
|--------|-------|--------|
| GET /profiles | RES + `AdminFilter` | `ProfileController.java:47` |
| PUT /profiles | RES + `SuperAdminFilter` | `ProfileController.java:55` |
| GET /functions | RES | `ProfileController.java:72` |
| POST /function/:profile | WF `profile.create.function` | `ProfileController.java:78` |
| DELETE /function/:function | WF `profile.delete.function` | `ProfileController.java:90` |
| DELETE /functiongroup/:groupId | WF `profile.delete.function.group` | `ProfileController.java:97` |

## Ressource: Positions (postes fonctionnels)

| Action | Garde | Source |
|--------|-------|--------|
| GET /positions, /positions/:id ; POST /positions ; PUT\|DELETE /positions/:id | RES + `AdminFilter` | `UserPositionController.java:28-128` |

## Ressource: Mass messaging

| Action | Garde | Source |
|--------|-------|--------|
| GET "" (vue), /massmessaging/senderName ; POST /massmessaging/column/mapping, /massmessaging/validation/populate | RES + `AdminFilter` | `MassMessagingController.java:70-120` |
| POST /massmessaging | RES | `MassMessagingController.java:137` |

## Ressource: Subjects (matières manuelles)

| Action | Garde | Source |
|--------|-------|--------|
| GET /subject/admin/list | AUTH + `AdminFilter` | `SubjectController.java:32` |
| POST /subject ; PUT\|DELETE /subject/:subjectId | RES + `AdminFilter` | `SubjectController.java:50-82` |

## Ressource: SlotProfiles (grilles horaires)

| Action | Garde | Source |
|--------|-------|--------|
| POST /slotprofiles ; PUT\|DELETE /slotprofiles/:id ; POST slots ; PUT\|DELETE slots/:idSlot | WF `directory.slot.manage` | `SlotProfileController.java:42-294` |
| GET /slotprofiles/schools/:schoolId, /slotprofiles/:id/slots | **aucune annotation** (⚠️ à confirmer : accès authentifié implicite ?) | `SlotProfileController.java:376, 393` |

## Ressource: Tenant / Remote / Calendar / Tâches internes

| Action | Garde | Source |
|--------|-------|--------|
| POST /tenant | WF `tenant.create` | `TenantController.java:41` |
| GET /tenant/:id | WF `tenant.get` + `SuperAdminFilter` | `TenantController.java:52` |
| PUT /remote/user/old-platforms-sync, /search-old-platform | RES + `SuperAdminFilter` | `RemoteUserController.java:43-54` |
| GET /calendar | RES + `AdminFilter` | `CalendarController.java:13` |
| POST /api/internal/* (delete-users, pre-delete-users, trigger-import, upload-aaf, trigger-csv-import, reinit-login, erase-timetable-reports) | RES (appels internes/cron) | `TaskController.java:18-157` |

## Ressource: Console & vues (DirectoryController)

| Action | Garde | Source |
|--------|-------|--------|
| GET /admin-console | RES + `AdminFilter` + **MFA** | `DirectoryController.java:90` |
| GET /class-admin | WF `classadmin.address` + **MFA** | `DirectoryController.java:98` |
| GET /class-admin/:userId | RES | `DirectoryController.java:106` |
| GET /gar/config | RES + `SuperAdminFilter` | `DirectoryController.java:130` |
| POST /transition | WF `directory.transition` | `DirectoryController.java:154` |
| POST /removeclassgroupshare/:structureExternalId | WF `directory.removeclassgroupshare` | `DirectoryController.java:176` |
| POST /autogroups/link | WF `directory.autogroups.link` | `DirectoryController.java:195` |
| POST /export | WF `directory.export` | `DirectoryController.java:206` |
| POST /reinitLogins | WF `directory.reinit.login` | `DirectoryController.java:213` |
| GET /annuaire | AUTH (`directory.search.view`) | `DirectoryController.java:220` |
| GET /schools | AUTH (`directory.schools`) | `DirectoryController.java:226` |
| GET /api/ecole, /api/personnes, /api/details | WF `directory.authent` | `DirectoryController.java:232-367` |
| POST /school | WF `directory.school.create` | `DirectoryController.java:238` |
| GET /school/:id | RES | `DirectoryController.java:271` |
| POST /class/:schoolId | WF `directory.class.create` | `DirectoryController.java:278` |
| GET /api/classes | WF `directory.classes` | `DirectoryController.java:326` |
| GET /users | WF `directory.list.users` | `DirectoryController.java:357` |
| POST /api/user, GET /api/export | RES | `DirectoryController.java:377, 476` |
| — | WF `classadmin.add.users` | `DirectoryController.java:687` |

---

## Droits workflow consommés côté front (`behaviours.ts`)

| Clé behaviours | Droit (classe\|méthode) | Effet UI |
|----------------|------------------------|----------|
| showMoodMotto | `UserBookController\|userBookMottoMood` | Affiche humeur/devise |
| switchTheme | `UserBookController\|userBookSwitchTheme` | Onglet thèmes de Mon Compte |
| generateMergeKey / mergeByKey | `UserController\|generateMergeKey` / `mergeByKey` | Section fusion de comptes |
| allowSharebookmarks | `ShareBookmarkController\|allowSharebookmarks` | Feature favoris |
| allowLoginUpdate | `UserController\|allowLoginUpdate` | Édition du login alias |
| allowClassAdminAddUsers / ResetPassword / BlockUsers / DeleteUsers / UnlinkUsers / CSVImport | `DirectoryController\|allowClassAdmin*` | Actions du class-admin |
| externalNotifications / historyView | `TimelineController\|…` | Réglages notifications (Mon Compte) |

Le contrôleur annuaire consulte aussi directement : `ZimbraController|view`, `ConversationController|view`, `PortalController|optionalFeatureWriteToEmailProviderWordline`, `ZimbraController|preauth`.

**Source** : `public/ts/behaviours.ts:5-23`, `public/ts/controllers/directory.ts:1012-1018, 1128-1131`, `public/ts/controllers/account.ts:192-195`
