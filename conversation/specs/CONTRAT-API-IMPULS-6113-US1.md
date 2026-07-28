# Contrat d'API — US-1 : établissement de l'émetteur dans la liste des messages

> Cadrage source : [CADRAGE-IMPULS-6108-structure-sender.md](./CADRAGE-IMPULS-6108-structure-sender.md)
> FS source : [FS-IMPULS-6108-structure-sender.md](./FS-IMPULS-6108-structure-sender.md)
> US : [IMPULS-6113](https://edifice-community.atlassian.net/browse/IMPULS-6113) — tâches [IMPULS-6116](https://edifice-community.atlassian.net/browse/IMPULS-6116) (socle `directory`), [IMPULS-6117](https://edifice-community.atlassian.net/browse/IMPULS-6117) (intégration `conversation`), [IMPULS-6118](https://edifice-community.atlassian.net/browse/IMPULS-6118) (Postman), [IMPULS-6119](https://edifice-community.atlassian.net/browse/IMPULS-6119) (front)
> Repo : entcore
> Date : 28/07/2026
> Statut : Proposé — à valider avant démarrage parallèle back/front

Ce document fige les deux contrats nécessaires pour que le front (IMPULS-6119), le socle
`directory` (IMPULS-6116) et l'intégration `conversation` (IMPULS-6117) puissent avancer en
parallèle :

1. le **contrat HTTP** du endpoint de liste, enrichi du champ `from.displayStructure` ;
2. le **contrat eventbus** entre `conversation` et `directory`, frontière entre 6116 et 6117.

---

## 1. Contrat HTTP

### Endpoint

`GET /conversation/api/folders/:folderId/messages`

Implémentation : `ApiController.listFolderMessages`
(`conversation/backend/src/main/java/org/entcore/conversation/controllers/ApiController.java:64`)
→ `ConversationService.listAndFormat`.

**Inchangé** : route, méthode, paramètres, filtres de sécurité
(`SystemOrUserFolderFilter`), codes de retour. Le seul changement est **additif** dans le corps
de la réponse.

### Paramètres

| Paramètre | Type | Défaut | Description |
| --- | --- | --- | --- |
| `folderId` (path) | `string` | — | ID de dossier système (`INBOX`, `OUTBOX`, `DRAFT`, `TRASH`) ou UUID de dossier utilisateur. |
| `page` (query) | `int` | `0` | Index de page, 0-based. |
| `page_size` (query) | `int` | `25` | Taille de page. Borné à `LIST_LIMIT = 25` (`ConversationService.java:41`) : toute valeur `> 25` ou `< 1` est ramenée à 25. |
| `unread` (query) | `boolean` | `false` | Ne renvoyer que les messages non lus. |
| `search` (query) | `string` | — | Recherche plein texte. **Minimum 3 caractères**, sinon `400`. |

### Réponse `200`

Un tableau JSON d'objets `MessageMetadata`. Champ nouveau signalé par **`NEW`**.

| Champ | Type | Nullable | Description |
| --- | --- | --- | --- |
| `id` | `string` | non | UUID du message. |
| `subject` | `string` | non | Objet du message. |
| `state` | `"SENT" \| "RECALL"` | non | État. La liste ne renvoie jamais `DRAFT` pour ce endpoint. |
| `date` | `number` | non | Timestamp epoch en millisecondes. |
| `unread` | `boolean` | non | Non lu pour l'utilisateur courant. |
| `response` | `boolean` | non | L'utilisateur courant a déjà répondu à ce message. |
| `hasAttachment` | `boolean` | non | Le message porte au moins une pièce jointe. |
| `noReply` | `boolean` | oui | Message sans réponse possible. |
| `count` | `number` | non | Nombre total de messages correspondant au filtre (`COUNT(*) OVER()`), répété sur chaque élément — sert à la pagination. |
| `from` | `User` | oui | Émetteur. Absent/vide si émetteur supprimé (voir ci-dessous). |
| `from.displayStructure` | `Structure` | **oui** | **`NEW`** Établissement retenu pour l'émetteur. **Absent** si non résolu. |
| `to` | `Recipients` | non | Destinataires directs. |
| `cc` | `Recipients` | non | Copie. |
| `cci` | `Recipients` | non | Copie cachée — filtrée : ne contient l'utilisateur courant que s'il en fait partie. |

Types référencés :

```ts
type User = {
  id: string;
  displayName: string;
  profile: string;            // "Teacher" | "Student" | "Relative" | "Personnel" | "Guest"
  displayStructure?: Structure;  // NEW — absent si non résolu
};

type Structure = {            // NEW
  id: string;                 // UUID de la Structure Neo4j
  name: string;               // Nom affichable, ex. "Collège Jean Moulin"
};

type Group = {
  id: string;
  displayName: string;
  size?: number;
  type?: string;
  subType?: string;
};

type Recipients = {
  users: User[];
  groups: Group[];
};
```

> **Nommage** : `displayStructure` et non `structure`. Le pluriel `structures` /
> `structureNames` désigne déjà, ailleurs dans la plateforme, l'ensemble des rattachements d'un
> utilisateur ; un `structure` singulier se lirait comme un attribut intrinsèque de l'émetteur,
> alors qu'il s'agit d'une valeur **calculée et relative à l'appelant**. Le préfixe `display`
> s'accorde avec `displayName` sur le même objet et signale une donnée d'affichage.
> Choix arrêté en équipe — ne pas renommer sans repasser par elle.

> **Portée du champ `displayStructure`** : il n'est peuplé que sur `from`. Les `User` présents dans
> `to`, `cc` et `cci` partagent le même type TypeScript mais **ne sont pas enrichis** par cette
> US — ne pas s'appuyer sur leur `displayStructure`.

> **`displayStructure.id`** est fourni pour un usage futur. Aucune navigation ni interaction ne
> l'exploite dans cette US (hors-scope explicite du cadrage) : le front n'affiche que
> `displayStructure.name`.

### Exemple `200`

```json
[
  {
    "id": "c3f6356a-1e0b-41ab-868c-5c191a09604b",
    "subject": "Réunion de rentrée — ordre du jour",
    "state": "SENT",
    "date": 1769598000000,
    "unread": true,
    "response": false,
    "hasAttachment": true,
    "noReply": false,
    "count": 42,
    "from": {
      "id": "f6d9a1b4-3c2e-4d5f-8a7b-1e2c3d4f5a6b",
      "displayName": "Dupont Marie",
      "profile": "Teacher",
      "displayStructure": {
        "id": "9b1c7d3e-5f4a-4b2c-9d8e-7a6b5c4d3e2f",
        "name": "Collège Jean Moulin"
      }
    },
    "to": {
      "users": [
        {
          "id": "2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d",
          "displayName": "Martin Julien",
          "profile": "Relative"
        }
      ],
      "groups": [
        {
          "id": "7c8d9e0f-1a2b-4c3d-8e9f-0a1b2c3d4e5f",
          "displayName": "Enseignants du collège Jean Moulin",
          "size": 48,
          "type": "ProfileGroup",
          "subType": "StructureGroup"
        }
      ]
    },
    "cc": { "users": [], "groups": [] },
    "cci": { "users": [], "groups": [] }
  },
  {
    "id": "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a",
    "subject": "Sortie scolaire — autorisation",
    "state": "SENT",
    "date": 1769511600000,
    "unread": false,
    "response": true,
    "hasAttachment": false,
    "noReply": false,
    "count": 42,
    "from": {
      "id": "8e7d6c5b-4a3f-4e2d-9c8b-7a6f5e4d3c2b",
      "displayName": "Bernard Sophie",
      "profile": "Personnel",
      "displayStructure": {
        "id": "3f2e1d0c-9b8a-4f7e-6d5c-4b3a2f1e0d9c",
        "name": "Académie de Lyon"
      }
    },
    "to": {
      "users": [
        {
          "id": "2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d",
          "displayName": "Martin Julien",
          "profile": "Relative"
        }
      ],
      "groups": []
    },
    "cc": { "users": [], "groups": [] },
    "cci": { "users": [], "groups": [] }
  },
  {
    "id": "5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d",
    "subject": "Message dont l'établissement n'a pas pu être résolu",
    "state": "SENT",
    "date": 1769425200000,
    "unread": false,
    "response": false,
    "hasAttachment": false,
    "noReply": false,
    "count": 42,
    "from": {
      "id": "4b5c6d7e-8f9a-4b0c-9d1e-2f3a4b5c6d7e",
      "displayName": "Petit Thomas",
      "profile": "Teacher"
    },
    "to": {
      "users": [
        {
          "id": "2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d",
          "displayName": "Martin Julien",
          "profile": "Relative"
        }
      ],
      "groups": []
    },
    "cc": { "users": [], "groups": [] },
    "cci": { "users": [], "groups": [] }
  },
  {
    "id": "9f0a1b2c-3d4e-4f5a-8b6c-7d8e9f0a1b2c",
    "subject": "Message d'un compte supprimé",
    "state": "SENT",
    "date": 1769338800000,
    "unread": false,
    "response": false,
    "hasAttachment": false,
    "noReply": false,
    "count": 42,
    "from": {
      "id": "",
      "displayName": "Ancien Utilisateur"
    },
    "to": {
      "users": [
        {
          "id": "2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d",
          "displayName": "Martin Julien",
          "profile": "Relative"
        }
      ],
      "groups": []
    },
    "cc": { "users": [], "groups": [] },
    "cci": { "users": [], "groups": [] }
  }
]
```

Les quatre éléments couvrent les cas que le front doit savoir rendre :
établissement résolu via un établissement commun, établissement résolu hors commun,
**`displayStructure` absente** (dégradation silencieuse), et émetteur supprimé (`id` vide, pas de
`profile`, pas de `displayStructure`).

### Autres codes de retour

| Code | Condition | Corps |
| --- | --- | --- |
| `302` | Session fermée | vide, en-tête `Location` vers la page de login |
| `400` | `folderId` vide, `search` de moins de 3 caractères, ou échec de `listAndFormat` | `{ "error": "<message>" }` |
| `401` | Utilisateur non autorisé sur ce dossier (`SystemOrUserFolderFilter`) | — |
| `500` | Erreur inattendue | `{ "error": "<message>" }` |

**Un échec de résolution d'établissement ne produit jamais d'erreur HTTP** : le champ
`displayStructure` est simplement omis (cf. §3).

---

## 2. Contrat eventbus `conversation` → `directory`

Frontière entre IMPULS-6116 (producteur, côté `directory`) et IMPULS-6117 (consommateur, côté
`conversation`). C'est le contrat à respecter de part et d'autre pour développer les deux
tâches en parallèle.

**Adresse** : `directory`
**Action** : `list-users-structures` — **implémenté** (IMPULS-6116) : `DirectoryController.directoryHandler`
→ `UserService.getUsersStructuresWithPreferred`.

Action **nouvelle** : l'action existante `list-users` reste strictement inchangée.

### Requête

```json
{
  "action": "list-users-structures",
  "userIds": [
    "f6d9a1b4-3c2e-4d5f-8a7b-1e2c3d4f5a6b",
    "8e7d6c5b-4a3f-4e2d-9c8b-7a6f5e4d3c2b",
    "2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d"
  ]
}
```

`userIds` : identifiants dédupliqués issus du `userIndex` déjà construit par
`MessageUtil.computeUsersAndGroupsDisplayNames` — **émetteurs de la page *et* destinataire
courant** (ce dernier y est ajouté par `MessageUtil.java:80-83`). Un seul appel batché par page,
soit au plus 26 identifiants (`LIST_LIMIT = 25` + le destinataire).

### Réponse

```json
{
  "status": "ok",
  "result": [
    {
      "id": "f6d9a1b4-3c2e-4d5f-8a7b-1e2c3d4f5a6b",
      "structures": [
        { "id": "9b1c7d3e-5f4a-4b2c-9d8e-7a6b5c4d3e2f", "name": "Collège Jean Moulin" },
        { "id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d", "name": "Lycée Ampère" }
      ],
      "preferredStructureId": "9b1c7d3e-5f4a-4b2c-9d8e-7a6b5c4d3e2f"
    },
    {
      "id": "8e7d6c5b-4a3f-4e2d-9c8b-7a6f5e4d3c2b",
      "structures": [
        { "id": "3f2e1d0c-9b8a-4f7e-6d5c-4b3a2f1e0d9c", "name": "Académie de Lyon" }
      ]
    }
  ]
}
```

| Champ | Type | Nullable | Description |
| --- | --- | --- | --- |
| `status` | `"ok" \| "error"` | non | Statut standard du bus. |
| `result[].id` | `string` | non | ID utilisateur demandé. |
| `result[].structures` | `Structure[]` | non | Structures d'appartenance via `(User)-[:IN]->(ProfileGroup)-[:DEPENDS]->(Structure)`. Jamais vide : un utilisateur sans structure est absent de `result` (voir ci-dessous). |
| `result[].preferredStructureId` | `string` | **oui** | Établissement préféré, extrait de `school-widget.schoolId` dans le blob `UserAppConf.widgets` (relation `(User)-[:PREFERS]->(UserAppConf)`). **Omis** si non défini ou si le parsing échoue — présent dans ~50 % des cas seulement. |

Contraintes à respecter par IMPULS-6116 :

- **Batch** : un seul appel pour tous les `userIds`, pas de N+1.
- **Parsing défensif** : `widgets` est un blob JSON non typé formellement. Un JSON malformé ou une
  clé absente doit produire l'**omission** de `preferredStructureId` pour cet utilisateur, jamais
  une erreur globale ni un `status: "error"`.
- **Utilisateurs absents de `result`** : un utilisateur inconnu **ou rattaché à aucune structure** n'est pas
  renvoyé. Ce n'est pas une erreur : le consommateur doit lire cette absence comme « rien à afficher » et
  omettre `displayStructure`, jamais supposer que chaque `userId` demandé revient.
- **Le blob de préférences brut n'est jamais renvoyé** : `widgets` contient d'autres préférences de
  l'utilisateur, seul l'identifiant résolu est exposé.
- `preferredStructureId` **n'est pas garanti** présent dans `structures` (préférence obsolète
  possible). Le consommateur doit gérer ce cas (cf. §3, étape 2).

---

## 3. Règle de résolution (côté `conversation`, IMPULS-6117)

Appliquée par message, pour l'émetteur `from`, en comparant ses structures à celles du
destinataire (l'utilisateur de session, présent dans le même `userIndex`).

Soit `S(e)` les structures de l'émetteur, `S(d)` celles du destinataire,
`C = S(e) ∩ S(d)` (intersection sur `displayStructure.id`), et `p` le `preferredStructureId` de
l'émetteur.

1. **`C` non vide** :
   - un seul élément → c'est lui ;
   - plusieurs éléments → celui dont l'`id` vaut `p` si `p ∈ C` ; sinon **fallback alphabétique
     sur `name` appliqué à `C`**.
2. **`C` vide** : l'élément de `S(e)` dont l'`id` vaut `p`, si `p` est défini **et** présent dans
   `S(e)`.
3. **Sinon** : premier élément de `S(e)` par ordre alphabétique croissant de `name`.
4. **`S(e)` vide, émetteur absent de `result`, ou échec de l'appel eventbus** : `displayStructure`
   **omise** du payload.

Comparaison alphabétique : sur `name`, insensible à la casse et aux accents, pour rester
cohérente avec un affichage francophone.

### Dégradation silencieuse

Un échec — appel eventbus en erreur, timeout, émetteur non résolu — n'interrompt **jamais** la
requête HTTP. Le champ `displayStructure` est omis pour le ou les émetteurs concernés, les autres
messages restent enrichis, et la réponse reste `200`. Le front n'affiche alors simplement pas
le libellé (pas d'état d'erreur dédié).

### Cohérence liste / détail

La même fonction de résolution doit servir `listAndFormat` (US-1) et `getAndFormat`
(US-2, IMPULS-6120) : pour un même message et un même destinataire, l'établissement affiché
dans la liste et dans le détail est **identique**. La logique est donc à écrire une seule fois,
dans un point partagé (`MessageUtil`), jamais dupliquée entre les deux services.

---

## 4. Impact front (IMPULS-6119)

- `conversation/frontend/src/models/user.ts` : ajouter `displayStructure?: { id: string; name: string }`
  au type `User`. `MessageBase.from` reste de type `User`, aucun autre modèle à toucher.
- `MessagePreview.tsx` : afficher `from.displayStructure.name` uniquement s'il est défini.
- `mocks/handlers/message-handlers.ts` (MSW) : refléter les quatre cas de l'exemple §1 —
  dont **au moins un message sans `displayStructure`**, pour couvrir la dégradation.

---

## 5. Collection Postman

Collection **ENT** › dossier `conversation/api/folders/{folderId}/messages`, requête
« List messages from a folder. » (workspace « Édifice Workspace ! 🔥 »).

L'exemple de réponse `200` de cette requête portait un placeholder non résolu
(`reference jsonschema/ListFolderMessagesResponse.json not found in the OpenAPI spec`), hérité
d'une génération OpenAPI incomplète. Il est remplacé par le payload réel de l'exemple §1.

> **Anomalie préexistante relevée, non corrigée** : la requête Postman déclare la variable de
> chemin `:folderName`, alors que la route back est `api/folders/:folderId/messages`. Simple
> incohérence de nommage dans la collection, sans effet sur l'appel — à corriger hors de cette US.