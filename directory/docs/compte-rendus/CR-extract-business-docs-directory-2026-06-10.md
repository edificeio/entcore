# CR — Extraction documents métier : directory
Date : 2026-06-10
Pattern structurel : **A** (mono legacy AngularJS — `src/` sans `frontend/` ; `view-src/` = shells HTML, pas de pont React)

## Documents générés

| Fichier | Contenu |
|---------|---------|
| `directory/docs/specs/BUSINESS_RULES.md` | 9 features, ~60 règles (Annuaire recherche + fiche, Favoris, Mon Compte, Fusion, DiscoverVisible, Ma Classe, **Class Admin**, Widget anniversaires, Sniplet trombinoscope, règles backend transverses) |
| `directory/docs/specs/DATA_MODEL.md` | 12 entités (dont les **deux** modèles `User` front distincts annuaire vs class-admin), 21 schémas JSON, POJOs backend |
| `directory/docs/specs/WORKFLOWS.md` | Table de routage `app.ts` complète + 20 workflows (WF-01 à WF-20) + table des vues servies |
| `directory/docs/specs/PERMISSIONS.md` | 19 filtres de sécurité documentés, ~120 routes couvrant les **19 contrôleurs**, droits workflow front (behaviours.ts) |
| `directory/docs/specs/API_CONTRACTS.md` | ~55 endpoints (dont class-admin, doublons, mailstate/mobilestate, endpoints communication/auth consommés) |

## Sources analysées

**Frontend** (`src/main/resources/public/ts/`) :
- `app.ts` (routage), `behaviours.ts` (droits workflow + sniplet), `birthday.ts` (widget), `model.ts` (903 l.)
- `controllers/directory.ts` (1465 l.), `controllers/account.ts` (517 l.)
- **`admin/`** (class-admin, 3326 l.) : `controller.ts`, `service.ts`, `model.ts`, delegates `userCreate`, `userInfos`, `userList`, `actions`, `userExport`, `userFind`, `choose-class`, `events`, `menu`
- `directives/` (adaptiveHeight, intlPhoneInput — survol)

**Backend** (`src/main/java/org/entcore/directory/`) :
- Annotations exhaustives des 19 contrôleurs : User, UserBook, Directory, ShareBookmark, Class, Group, Structure, Import, Timetable, Profile, UserPosition, MassMessaging, Tenant, SlotProfile, Subject, RemoteUser, Calendar, Task
- Lecture détaillée : `UserController` (validations PUT user/userbook, filtrage « visible », notifications email/mobile), `ShareBookmarkController` (complet)
- `security/` : 19 filtres listés et résumés

**Ressources** : `jsonschema/` (21 schémas), `i18n/fr.json` (indices de features), `view/` et `view-src/` (shells)

Pas de `README.md` ni `CHANGELOG.md` dans le module — étape 4 sans apport.

## Points ⚠️ À confirmer

1. **Bug latent « Ma Classe »** : dans `model.ts:313-323`, le garde-fou `classes.length !== realClassesNames.length` assigne `results = []` mais le map suivant l'écrase — le zip se fait quand même sur des tableaux désalignés. À vérifier/corriger avant migration.
2. **`GET /userbook/avatar/:id` sans annotation de sécurité** : avatar public assumé ? À confirmer pour la migration.
3. **`GET /slotprofiles/schools/:schoolId` et `/slotprofiles/:id/slots` sans `@SecuredAction`** : accès implicite à vérifier.
4. **Paramètre malformé** dans `admin/service.ts:108` : `?classId=…&=collectRelative=…` (signe `=` en trop) — `collectRelative` probablement jamais transmis.
5. **Fix temporaire MOZO-77** (`removePrivateInfos`) : le masquage des données privées est fait côté client en attendant le backend — statut à vérifier.
6. **TODO du code** dans `ImportController` : les routes wizard ne vérifient pas le propriétaire de l'import (commentaire « TODO add import owner and check »).
7. **Logique Neo4j non explorée** : les services `Default*Service` (requêtes Cypher) peuvent porter des règles métier invisibles depuis les contrôleurs (ex. règles exactes de visibilité de `UserAccessOrVisible`, détection de doublons).
8. **DiscoverVisible** : les règles d'autorisation serveur (qui peut voir qui) vivent dans le module `communication`, hors périmètre.
9. **Délégates non lus en détail** : `userFind.ts`, `menu.ts`, `events.ts` (survolés), fin de `userExport.ts` — le flux publipostage « mail » (confirmation, users sans email) est documenté d'après la première moitié du fichier.
10. **Console admin (`admin-console`) et wizard d'import** : vues legacy probablement remplacées par le module `admin` V2 du monorepo — la logique front de ces écrans n'est pas dans `public/ts/` et n'a pas été tracée.
