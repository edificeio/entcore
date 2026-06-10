# Comparaison des extractions métier `directory` — docs/ (Fable 5) vs docs2/ (Sonnet 4.6)

Date : 2026-06-10
- `docs2/` : génération Sonnet 4.6 (première passe)
- `docs/` : génération Fable 5 (seconde passe, indépendante)

## Vue d'ensemble

| Document | docs2 (Sonnet) | docs (Fable) | Écart principal |
|----------|----------------|--------------|-----------------|
| BUSINESS_RULES.md | 209 l. — 6 features, 22 règles | 521 l. — 9 features, ~60 règles | + Class Admin (~20 règles), widget anniversaires, sniplet, règles backend transverses |
| DATA_MODEL.md | 167 l. — 7 entités | 227 l. — 12 entités | + modèle `User` du class-admin, Mood, Slot/SlotProfile, POJOs, table des 21 schémas |
| WORKFLOWS.md | 256 l. — 11 WF | 268 l. — 20 WF + routage | + table de routage `app.ts`, 6 WF class-admin, WF widget |
| PERMISSIONS.md | 162 l. — 7 ressources (~50 routes) | 269 l. — ~120 routes, 19 contrôleurs | + Structure, Import, Timetable, Profile, Positions, MassMessaging, Tenant, SlotProfile, Subject, Remote, Calendar, Task, doublons, mailstate |
| API_CONTRACTS.md | 493 l. — 28 endpoints | 323 l. — ~55 endpoints | Couverture plus large mais payloads d'exemple moins verbeux |

Les deux passes s'accordent sur tout ce qu'elles couvrent en commun : aucune contradiction factuelle détectée entre les deux jeux de documents. Les écarts sont des écarts de **couverture** et de **précision**, pas de désaccord.

## Différences majeures de couverture

### 1. Le module Class Admin (la plus grosse divergence)
Sonnet n'a pas exploré `public/ts/admin/` (3 326 lignes : controller, service, model, 9 delegates). C'est pourtant la moitié fonctionnelle du module côté enseignant. En conséquence, docs2 ignore :
- les règles de création d'utilisateur (champs obligatoires par profil, bornes de date de naissance, garde « parent sans enfant », détection de doublons avec rattacher/déplacer/créer) ;
- la restriction de suppression aux sources `MANUAL|CLASS_PARAM|BE1D|CSV` ;
- la redirection ADML-soi-même vers Mon Compte, `lockedEmail`, la gestion TOTP ;
- la propagation parent/enfant (`withRelative`) des opérations de classe et l'endpoint atomique `PUT /class/:id/change` ;
- le publipostage (`POST /class-admin/massmail`, types pdf/simplePdf/mail/csv) et les endpoints auth associés (`/auth/users/block`, `/auth/massGeneratePasswordRenewalCode`, `sendResetPassword` vers l'email de l'enseignant) ;
- la lightbox choose-class (`PUT /class/add-self` + reload complet).
Sonnet avait signalé cette lacune dans son CR (« 8 contrôleurs secondaires non analysés ») mais sans mentionner `admin/` côté front.

### 2. Le routage `app.ts`
docs2 marque les routes hash « ⚠️ À confirmer » (fichier non lu). docs documente la table complète, qui révèle deux différences avec les suppositions de docs2 : la route fiche utilisateur est `#/user-view/:userId` (et le raccourci `#/:userId`), pas `#/viewUser/:id` ; et la route par défaut est `/myClass`, pas `/search`.

### 3. Les contrôleurs backend secondaires
docs2 couvre 7 contrôleurs ; docs couvre les annotations des 19 (Structure ~30 routes, Import/wizard, Timetable, Profile, UserPosition, MassMessaging, Tenant, SlotProfile, Subject, RemoteUser, Calendar, Task internes, doublons, mailstate/mobilestate).

### 4. behaviours.ts, birthday.ts, sniplet
Absents de docs2 : les 14 droits workflow consommés côté front (dont les 6 `allowClassAdmin*` qui pilotent l'UI du class-admin), le widget anniversaires (préférence de classe, filtre mois courant) et le sniplet trombinoscope.

### 5. Anomalies de code relevées
docs (CR) signale des points absents de docs2 : le garde-fou inopérant du zip classes/realClassesNames (bug latent), le paramètre malformé `&=collectRelative=`, `GET /userbook/avatar/:id` et deux routes slotprofiles **sans annotation de sécurité**, les TODO « import owner » du wizard.

## Points où docs2 (Sonnet) est plus détaillé

- **API_CONTRACTS** : payloads d'exemple JSON plus longs et systématiques (docs privilégie la densité : description + corps + erreurs, exemples plus courts).
- Quelques endpoints décrits individuellement dans docs2 sont regroupés en tables dans docs (ex. les 10 endpoints discoverVisible).
- Le détail du filtrage de champs de `UserAccessOrVisible` : docs2 liste nominativement les ~40 champs retirés ; docs résume (« ~40 champs sensibles ») en renvoyant à la source.

## Erreurs ou imprécisions de docs2 corrigées dans docs

1. **Routes hash erronées** : docs2/WORKFLOWS cite `#/viewUser/:id` et `#/viewGroup/:groupId` (noms des *actions*, pas des routes). Les vraies routes sont `#/user-view/:userId`, `#/:userId`, `#/group-view/:groupId`.
2. **`view-src/` mal interprété** : docs2 le présente comme possible « pont AngularJS→React » (vocabulaire du template de la skill). Ce sont les shells HTML sources des vues legacy — il n'y a aucun pont React dans ce module.
3. **Comptage** : docs2 annonce « 28 endpoints » et « 7 entités » comme s'ils étaient exhaustifs ; le module en compte environ le double (endpoints propres) et le modèle User existe en deux variantes front non signalées.
4. **`directory.allow.sharebookmarks`** : décrit dans docs2/PERMISSIONS comme simple ligne de table ; docs précise que la route est volontairement no-op (déclarative) et que le droit conditionne toute la feature favoris côté front.

## Ce que les deux passes laissent ouvert (identique)

- Logique Cypher des services Neo4j (`Default*Service`) non explorée.
- Règles d'autorisation discoverVisible (module `communication`).
- Fix temporaire MOZO-77 (masquage client des données privées).
- Liste réelle des moods (dépend de la configuration de déploiement).

## Synthèse

docs2 est une bonne première passe sur le cœur annuaire/Mon Compte/favoris, fidèle sur ce qu'elle couvre, mais elle ne voit qu'environ la moitié du module (ni class-admin front, ni 12 contrôleurs backend) et contient deux imprécisions structurantes (routes hash, rôle de view-src). docs reprend tout le contenu factuel de docs2, corrige ces points et double la couverture. Pour la migration React, la recommandation est d'utiliser `docs/` comme référence et d'archiver `docs2/`.

## Fusion appliquée (2026-06-10)

Les points où docs2 était meilleur ont été réinjectés dans `docs/` :
- **API_CONTRACTS.md** : exemples de payload JSON multi-lignes repris pour `PUT /directory/user/:userId` (corps complet), `GET /directory/user/:userId` (réponse), `PUT /directory/userbook/:userId` (corps avec contraintes inline), `POST /communication/visible` (corps + réponse), `GET /sharebookmark/:id` (réponse), `GET /userbook/structures`, `GET /userbook/search/criteria`, `GET mailstate/mobilestate`, réponse OTP.
- **PERMISSIONS.md** : liste nominative des ~40 champs retirés par `UserAccessOrVisible` en accès « visible » (au lieu du résumé).

`docs/` constitue désormais le surensemble des deux passes ; `docs2/` peut être archivé ou supprimé.
