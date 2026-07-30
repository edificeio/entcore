# Contrat d'API — US-1 : paramétrage du message d'absence

> Cadrage source : [CADRAGE-IMPULS-6109.md](./CADRAGE-IMPULS-6109.md)
> FS source : [FS-IMPULS-6109.md](./FS-IMPULS-6109.md)
> US : [IMPULS-6130](https://edifice-community.atlassian.net/browse/IMPULS-6130) — tâches [IMPULS-6135](https://edifice-community.atlassian.net/browse/IMPULS-6135) (contrat, ce document), [IMPULS-6136](https://edifice-community.atlassian.net/browse/IMPULS-6136) (migration `025`), [IMPULS-6137](https://edifice-community.atlassian.net/browse/IMPULS-6137) (service/routes), [IMPULS-6138](https://edifice-community.atlassian.net/browse/IMPULS-6138) (modale front), [IMPULS-6139](https://edifice-community.atlassian.net/browse/IMPULS-6139) (hooks front)
> Repo : entcore (module `conversation`)
> Date : 30/07/2026
> Statut : Proposé — fige le contrat avant développement parallèle back/front. Sert aussi de référence pour US-2 ([IMPULS-6146](https://edifice-community.atlassian.net/browse/IMPULS-6146), champ `timezone` sur l'envoi) et US-3 (désactivation, même `PUT`).

Ce document fige trois contrats :

1. `GET /conversation/absence` — lecture du paramétrage courant.
2. `PUT /conversation/absence` — upsert : création, modification, désactivation.
3. Évolution de `POST /conversation/send` (route existante) — ajout du champ `timezone` (US-2).

Collection Postman associée : **ENT** (workspace « Édifice Workspace ! 🔥 ») › dossier `conversation` › `absence` (nouveau) et `send` (existant, mis à jour).

---

## 1. `GET /conversation/absence`

### 1.1 Endpoint

`GET /conversation/absence`

Implémentation prévue : nouvelle route dans `ConversationController` → `ConversationService` / `SqlConversationService` (IMPULS-6137). Route nouvelle, purement additive — aucun impact sur l'existant.

### 1.2 Authentification & droits

- Session utilisateur requise.
- Restriction de profil : réservé aux profils **Teacher** et **Personnel**, testés sur le **premier élément** de `UserInfos.getType()` — convention `HEAD(profiles)` généralisée dans le back (`OAuthDataHandler.java:364`, `DefaultUserAuthAccount.java:158`, `SSOAten.java:61`).
- Aucun droit workflow créé : la restriction de profil suffit (cf. cadrage, section « Procédures d'exploitation »).
- Un utilisateur ne peut lire que **son propre** paramétrage — aucun identifiant dans l'URL, l'utilisateur courant est déduit de la session.

### 1.3 Paramètres

Aucun (ni path, ni query).

### 1.4 Réponse `200` — schéma

| Champ | Type | Nullable | Description |
| --- | --- | --- | --- |
| `enabled` | `boolean` | non | Paramétrage **armé** ou non. Ne signifie **pas** « absence en cours » — voir §1.4.1 |
| `startAt` | `string` (ISO 8601, UTC) | non | Début de la période d'absence |
| `endAt` | `string` (ISO 8601, UTC) | non | Fin de la période d'absence |
| `bodyJson` | `object` (JSON tiptap) | non | Contenu normalisé — **sortie** du transformer de contenu, jamais l'entrée brute (voir §2.3) |
| `bodyHtml` | `string` (HTML) | non | Rendu HTML, **dérivé** de `bodyJson` par le même transformer |
| `updatedAt` | `string` (ISO 8601, UTC) | non | Date de dernière modification |

Si aucun paramétrage n'a jamais été créé pour l'utilisateur : `200` avec `{}` (pas de `404`).

### 1.4.1 `enabled` n'est pas « absence en cours »

L'API n'expose **pas** de booléen « absence en cours » : elle expose un paramétrage armé (`enabled`) et ses bornes. Le consommateur doit dériver lui-même l'état courant :

```
actif = enabled && startAt ≤ maintenant ≤ endAt
```

`enabled: true` avec un `startAt` futur décrit une absence **programmée mais pas commencée** — cas explicitement autorisé par la FS (US-1 : « le paramétrage est actif automatiquement dès la date de début », sans action supplémentaire de l'utilisateur). Prendre `enabled` pour « en cours » afficherait donc un état d'absence avant le début de la période.

Ce calcul doit être **refait à chaque évaluation**, et non mémorisé au chargement : l'état bascule tout seul au franchissement de `startAt` et de `endAt`, sans qu'aucune modification du paramétrage ne vienne invalider un cache.

Le back applique la même règle, et c'est la seule qui gouverne l'envoi effectif des réponses automatiques (US-2, détection en lot sur la période active). Il n'y a donc pas deux définitions d'« actif » à maintenir : une seule, appliquée de part et d'autre.

### 1.5 Exemple `200` — paramétrage existant

```json
{
  "enabled": true,
  "startAt": "2026-08-01T22:00:00.000Z",
  "endAt": "2026-08-15T21:59:59.000Z",
  "bodyJson": {
    "type": "doc",
    "content": [
      {
        "type": "paragraph",
        "content": [{ "type": "text", "text": "Je suis absent, réponse à mon retour." }]
      }
    ]
  },
  "bodyHtml": "<p>Je suis absent, réponse à mon retour.</p>",
  "updatedAt": "2026-07-30T08:00:00.000Z"
}
```

### 1.6 Exemple `200` — aucun paramétrage

```json
{}
```

### 1.7 Autres codes de retour

| Code | Condition | Corps |
| --- | --- | --- |
| `401` | Session fermée | — |
| `403` | Profil autre que Teacher/Personnel | `{ "error": "<message>" }` |

---

## 2. `PUT /conversation/absence`

### 2.0 Endpoint

`PUT /conversation/absence`

Upsert (`ON CONFLICT DO UPDATE` sur la clé primaire `user_id`, cf. migration `025` dans le cadrage). Pas de `DELETE` : la désactivation renvoie le même payload avec `enabled: false`, les champs sont conservés.

Authentification & droits : identiques au `GET` (§1.2) — session requise, restriction Teacher/Personnel, un utilisateur ne modifie que son propre paramétrage.

### 2.1 Corps de la requête

| Champ | Type | Requis | Description |
| --- | --- | --- | --- |
| `enabled` | `boolean` | oui | Active ou désactive le message d'absence |
| `startAt` | `string` (ISO 8601, UTC) | oui | Début de période. Calculé côté front à partir du **début de journée locale** de l'utilisateur, converti en UTC. **Aucune contrainte** sur une date passée (autorisé explicitement par la FS). |
| `endAt` | `string` (ISO 8601, UTC) | oui | Fin de période. Doit être **≥ `startAt`**, sinon `400`. Pas de `minDate` appliqué côté front (cf. risque #8 du cadrage) : le scénario d'erreur doit rester atteignable. |
| `bodyJson` | `object` (JSON tiptap) | **oui** | Contenu du message. Non vide si `enabled: true` (validation : texte requis pour activer). |
| `bodyHtml` | `string` (HTML) | non — **ignoré** | Toléré dans le payload par tolérance de forme, mais **jamais persisté ni utilisé** (voir §2.3). Ne pas s'y fier côté consommateur. |

### 2.2 Validation

| Règle | Comportement en échec |
| --- | --- |
| `endAt` ≥ `startAt` | `400` |
| `bodyJson` non vide si `enabled: true` | `400` |
| `bodyJson` présent et bien formé | `400` si absent/malformé |
| `startAt` dans le passé | **autorisé**, aucune erreur |

### 2.3 Normalisation côté back — point clé du contrat

**`bodyJson` ne doit jamais être stocké tel quel.** Il passe par le même transformer de contenu que les messages classiques (`SqlConversationService.transformMessageContent`, `SqlConversationService.java:1075-1094`), qui demande déjà les deux formats de sortie via `expectedFormats = new HashSet<>(Arrays.asList(ContentTransformerFormat.HTML, ContentTransformerFormat.JSON))` (ligne 1078) :

- le **JSON normalisé** (schéma tiptap valide, marks/nodes autorisés) est stocké comme `body_json` ;
- le **HTML** est **dérivé** de ce JSON normalisé par le transformer, et stocké comme `body_html`.

L'appelant existant (`updateMessageWithTransformedContent`) ne lit aujourd'hui que la sortie HTML et jette la partie JSON ; l'implémentation du message d'absence (IMPULS-6137) doit exploiter **les deux** sorties.

Conséquence directe : un `bodyHtml` envoyé dans le `PUT` est **accepté mais ignoré** — aucune valeur fournie par l'appelant n'est jamais persistée comme `body_html`. C'est une décision assumée du cadrage (voir CADRAGE-IMPULS-6109.md, section Contrat d'API) : documenter ce point de façon très explicite pour qu'aucun consommateur (front, mobile) ne s'appuie sur un `bodyHtml` qu'il aurait lui-même envoyé.

### 2.4 Exemple de requête — activation

```json
{
  "enabled": true,
  "startAt": "2026-08-01T22:00:00.000Z",
  "endAt": "2026-08-15T21:59:59.000Z",
  "bodyJson": {
    "type": "doc",
    "content": [
      {
        "type": "paragraph",
        "content": [{ "type": "text", "text": "Je suis absent, réponse à mon retour." }]
      }
    ]
  }
}
```

`bodyHtml` volontairement omis ici — il est toléré s'il est envoyé, mais inutile.

### 2.5 Exemple de requête — désactivation

Même paramétrage, `enabled` à `false`. Les autres champs sont conservés (US-3, pas de purge des données à la désactivation) :

```json
{
  "enabled": false,
  "startAt": "2026-08-01T22:00:00.000Z",
  "endAt": "2026-08-15T21:59:59.000Z",
  "bodyJson": {
    "type": "doc",
    "content": [
      {
        "type": "paragraph",
        "content": [{ "type": "text", "text": "Je suis absent, réponse à mon retour." }]
      }
    ]
  }
}
```

### 2.6 Réponse `200`

Même schéma que le `GET` (§1), reflétant l'état persisté après upsert — `bodyJson` normalisé et `bodyHtml` dérivé, jamais les valeurs brutes envoyées.

### 2.7 Autres codes de retour

| Code | Condition | Corps |
| --- | --- | --- |
| `400` | `endAt` antérieur à `startAt` | `{ "error": "<message>" }` |
| `400` | `enabled: true` avec `bodyJson` vide ou sans texte | `{ "error": "<message>" }` |
| `400` | `bodyJson` absent ou malformé | `{ "error": "<message>" }` |
| `401` | Session fermée | — |
| `403` | Profil autre que Teacher/Personnel | `{ "error": "<message>" }` |

---

## 3. Évolution de `POST /conversation/send` (US-2, IMPULS-6146)

### 3.1 Endpoint

`POST /conversation/send` — route **existante**, inchangée sauf ajout d'un champ optionnel au payload.

Implémentation : `ConversationController.send` (`ConversationController.java:493`).

### 3.2 Évolution du corps de la requête

| Champ | Type | Requis | Description |
| --- | --- | --- | --- |
| `timezone` | `string` (identifiant IANA, ex. `"Europe/Paris"`) | **non** — `NEW` | Fuseau horaire de l'expéditeur. **Absent → repli sur le fuseau serveur.** Purement additif, aucune rupture de compatibilité mobile (les clients déployés ne l'enverront pas). |

Tous les autres champs (`to`, `cc`, `cci`, `subject`, `body`, …) sont inchangés.

### Usage

Sert exclusivement au garde-fou « une réponse automatique par jour et par expéditeur » du message d'absence (US-2) : le test d'unicité journalière sur `absence_replies.last_sent_at` s'évalue dans ce fuseau plutôt que dans celui du serveur. Aucun autre comportement de la route n'est affecté par ce champ.

### Exemple de requête (évolution)

```json
{
  "to": ["24827863-82d6-4812-944f-13608b07a811"],
  "cc": [],
  "cci": [],
  "subject": "Réunion de rentrée",
  "body": "<p>Bonjour, ...</p>",
  "timezone": "Europe/Paris"
}
```

### Codes de retour

Inchangés par rapport à la route existante — cette évolution n'introduit aucun nouveau code d'erreur.

---

## 4. Impact front

- **IMPULS-6138** (modale) : calcul de `startAt`/`endAt` en UTC à partir des bornes de journée locale ; envoi de `bodyJson` (obligatoire) — `bodyHtml` n'a pas besoin d'être envoyé par le front, il peut l'omettre du payload `PUT`.
- **IMPULS-6139** (hooks) : clés TanStack Query, invalidation/`setQueryData` optimiste au succès du `PUT`.
- **IMPULS-6147** (timezone à l'envoi) : injecter `Intl.DateTimeFormat().resolvedOptions().timeZone` dans le payload de `POST /conversation/send`.

---

## 5. Collection Postman

Collection **ENT**, workspace « Édifice Workspace ! 🔥 » :

- `conversation` › `absence` (nouveau dossier) : requêtes `GET` et `PUT` avec description complète (paramètres, schéma de réponse, codes d'erreur) et exemples de réponse pour les cas `200` (avec/sans paramétrage), `400` (×2), `401`, `403`.
- `conversation` › `send` (existant) : requête « or transfer an email » mise à jour — payload d'exemple avec `timezone`, description du champ (optionnel, repli serveur, usage anti-spam).
