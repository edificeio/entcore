# Cadrage technique — Message d'absence

> FS source : [FS-IMPULS-6109.md](./FS-IMPULS-6109.md)
> Repo : entcore (module `conversation`)
> Date : 29/07/2026 — révisé le 30/07/2026 : retrait de l'index partiel sur `absence_settings` et du groupement par fuseau dans IMPULS-6144, charge back 35 → 32 SP
> Participants : Squad Impulsion (dev front, dev back)
> Statut : Draft
> Maquettes : [Figma — Messagerie / Message d'absence](https://www.figma.com/design/B8KkuSYSpB3SZYnM3MDRJB/W---Messagerie--Portage-03-2024-?node-id=7616-3) — US-4 (bandeau) révisée le 30/07/2026 : [Figma — bandeau-rappel-absence, v2](https://www.figma.com/design/B8KkuSYSpB3SZYnM3MDRJB/W---Messagerie--Portage-03-2024-?node-id=7773-2968)
> Tickets : EPIC [IMPULS-6109](https://edifice-community.atlassian.net/browse/IMPULS-6109) — US-1 [IMPULS-6130](https://edifice-community.atlassian.net/browse/IMPULS-6130), US-2 [IMPULS-6131](https://edifice-community.atlassian.net/browse/IMPULS-6131), US-3 [IMPULS-6132](https://edifice-community.atlassian.net/browse/IMPULS-6132), US-4 [IMPULS-6133](https://edifice-community.atlassian.net/browse/IMPULS-6133)

---

## Contexte / Objectif

Un utilisateur de la Messagerie absent plusieurs jours n'a aujourd'hui aucun moyen de signaler son indisponibilité : les expéditeurs restent sans confirmation ni délai de réponse, et l'absent subit une charge de rattrapage à son retour. Ce chantier ajoute au module `conversation` un paramétrage de message d'absence (période + texte riche, activable et modifiable) et un déclenchement automatique de réponse pour tout expéditeur écrivant à la personne absente, nominativement ou via un groupe.

Le chantier se décompose en un socle de persistance et d'API (US-1), un mécanisme de déclenchement et d'émission asynchrone (US-2), une désactivation manuelle portée par le même socle (US-3), et un bandeau de rappel permanent (US-4). L'essentiel de l'effort et du risque est concentré sur US-2, non pas dans la détection des absents — le code existant la rend simple — mais dans la maîtrise du volume d'émission sur les envois de masse.

---

## Schéma d'ensemble

```text
                    ┌─────────────────── US-1 / US-3 ────────────────────┐
  Modale front      │  GET  /conversation/absence                        │   absence_settings
  (portal, DS)  ────┤  PUT  /conversation/absence  (upsert)              ├──►  user_id (PK)
  bornes → UTC      │  profil : HEAD(profiles) ∈ {Teacher, Personnel}    │     start_at / end_at (TIMESTAMPTZ)
                    └───────────────────────────────────────────────────-┘     enabled, body_json, body_html

                    ┌─────────────────────── US-2 ───────────────────────┐
  @Post("send")     │  1. saveAndSend → allUsers (groupes déjà aplatis)  │
  + timezone        │  2. détection en lot des absents actifs            │──►  absence_settings (PK user_id)
  (optionnel)       │  3. préférences de langue, loties en masse         │
        │           │  4. garde-fou 1/jour/expéditeur                    │──►  absence_replies
        ▼           │  5. émission étalée via saveAndSend                │     (absent, sender) → last_sent_at
   eventHelper      └───────────────────────────────────────────────────-┘
   (stats)                          │
        ▲                           ▼
        └── non appelé ────►  réponse automatique : « Réponse automatique : <objet> »
            hors de la route            (anti-bouclage et exclusion des stats
            → exclusion par              acquis par construction : l'émission
              construction               ne repasse pas par le handler de route)

                    ┌─────────────────────── US-4 (révisé 30/07) ────────┐
  Folder.tsx   ─────┤  Alert dans la colonne de contenu, au-dessus de la │
  (écrans liste)    │  barre de recherche, + « Modifier »                │
  2 déclencheurs    │  réutilise le hook query de US-1 ; pas affiché sur │
  répartis          │  le détail d'un message ni sur la rédaction        │
                    └────────────────────────────────────────────────────┘
```

---

## Risques / Points d'attention

| # | Risque | Impact | Mitigation |
|---|---|---|---|
| 1 | **Fan-out d'émission sur envoi de masse.** Un envoi à 200 000 destinataires en période de congés peut déclencher des milliers de réponses automatiques, chacune étant un `saveAndSend` complet (draft + lignes filles). Le coût est à l'écriture, pas à la détection. | Fort — dégradation plateforme, ce que la FS interdit explicitement | Émission asynchrone découplée + étalement/throttling. Le garde-fou 1/jour borne les répétitions mais **pas** ce pic initial. Sous-tâche dédiée, estimée à 5. |
| 2 | **Requête de détection à 200 000 identifiants.** `allUsers` entier dans un `WHERE user_id = ANY($1)` n'est pas tenable d'un bloc. | Moyen | Découpage en lots — c'est la seule mitigation réelle. Pas d'index dédié : la PK sur `user_id` sert déjà le `= ANY($1)` (voir migration `025`). |
| 3 | **Collision avec l'envoi différé, en cours de développement.** Même table `messages`, même chemin d'envoi, fonctionnalité inachevée. | Moyen | Coordination avant merge. La FS annonçait « aucune dépendance » : vrai fonctionnellement, faux au niveau du code. |
| 4 | **Compatibilité mobile du champ fuseau.** Ajouter le fuseau à la charge utile d'envoi casserait les clients mobiles déployés si le champ était requis — or la FS interdit toute rupture. | Fort si le champ est rendu obligatoire | Champ **optionnel**, repli sur le fuseau serveur quand absent. Le garde-fou 1/jour se dégrade légèrement pour ces clients, sans jamais échouer. *Mitigation retenue au cadrage.* |
| 5 | **Purge RGPD.** Les deux nouvelles tables, clés sur `user_id`, doivent être ajoutées à `ConversationRepositoryEvents.deleteUsers()` (ligne 414), qui purge aujourd'hui `folders` et `usermessages`. | Moyen — lignes orphelines, écart RGPD | Sous-tâche dédiée, pas un rattrapage de fin de chantier. |
| 6 | **Modale qui ne se ferme pas après enregistrement.** La FS l'exige ; `SignatureModal` fait l'inverse (`handleCloseModal()` après succès). Un copier-coller reproduira le mauvais comportement. | Faible mais silencieux | À implémenter explicitement et à couvrir en test. |
| 7 | **Divergence front/back sur les comptes multi-profils.** Le back réduit le profil à `HEAD(profiles)` (convention généralisée : `OAuthDataHandler.java:364`, `DefaultUserAuthAccount.java:158`, `SSOAten.java:61`), le front reçoit le tableau complet. Un Personnel dont le premier profil est Relative verrait l'entrée de menu et se ferait refuser en 403. | Moyen — invisible en test mono-profil | Tester le **premier élément** des deux côtés. Couvrir un compte multi-profil en recette. |
| 8 | **Tension `minDate` / scénario d'erreur.** Le précédent `actualites` contraint les dates par `minDate`/`maxDate`, ce qui rendrait le scénario Gherkin « erreur sous le champ date de fin » inatteignable. | Faible | Pas de `minDate` sur la date de fin : on valide et on affiche l'erreur, comme l'imposent la FS et le Figma. |

**Risque écarté par conception.** L'ambiguïté de fuseau horaire sur les bornes de période, initialement identifiée comme risque moyen (erreur d'un jour en début ou fin de période), est supprimée : les bornes sont calculées côté front dans le fuseau local de l'utilisateur puis stockées en UTC. Le test d'activité back redevient une comparaison d'instants.

---

## Limitations / Hors-scope

- **Seuls les envois via la route HTTP `@Post("send")` déclenchent une réponse automatique.** Les messages émis via le `@BusAddress("org.entcore.conversation")` et via `@Post("externe")` n'en déclenchent pas. Décision assumée : le bus est utilisé par des applications, pas par des utilisateurs, et les messages `externe` posent déjà `noReply = true`.
- Hors périmètre fonctionnel (repris de la FS) : redirection des messages vers un autre destinataire, plages horaires hors journées d'absence, paramétrage par un ADML pour le compte d'un tiers, périodes d'absence multiples en parallèle.
- Aucune exigence d'audit au-delà de la visibilité native de la réponse automatique dans le dossier « Messages envoyés ».
- **US-4 (bandeau) révisée le 30/07/2026** : changement de dernière minute du PM sur la maquette, après un premier arbitrage déjà pris au cadrage (Figma pleine largeur sous l'`AppHeader` plutôt que « à côté du titre » comme l'écrivait la FS v4). La maquette v2 (node `7773-2968`) place le bandeau **dans la colonne de contenu** de l'écran de liste, au-dessus de la barre de recherche — plus au niveau du layout racine. Conséquence fonctionnelle actée avec le PM : le bandeau n'est **plus** visible sur tous les écrans de la Messagerie, seulement sur les écrans de liste (boîte de réception, dossiers, envoyés, corbeille) ; il disparaît de l'écran de lecture d'un message et de celui de rédaction. FS mise à jour en conséquence (v5). Voir §Front pour l'impact sur le point d'insertion et le partage d'état de la modale.
- **Écart FS assumé sur les composants de bouton** : `ButtonBeta` (edifice2d) et non le `Button` utilisé par `SignatureModal`. La modale d'absence n'aura donc pas exactement le même rendu que celle de la signature.
- **Écart FS corrigé en séance sur le stockage du texte** : la FS justifiait le double format JSON + HTML par « cohérence avec le fonctionnement actuel de l'éditeur riche », ce qui est faux dans ce module (HTML seul partout : `messages."body"` en `TEXT`, `models/message.ts:28`, `MessageBody.tsx:43`). Le double format est néanmoins retenu, mais pour la vraie raison : une décision d'architecture plateforme fait du JSON le format tiptap par défaut, et le HTML reste nécessaire à l'affichage mobile.
- **i18n** : français seul en développement.

---

## Métriques produit

La FS ne définit pas de métriques produit. Celles-ci sont proposées au titre de la couverture du risque #1 et restent à valider par le PO/PM :

- Nombre de réponses automatiques émises par envoi, et distribution de cette valeur — métrique de surveillance directe du fan-out.
- Durée de traitement complet du lot d'émission, à comparer à la marge de « quelques minutes » annoncée dans la FS.
- Nombre d'utilisateurs disposant d'un message d'absence actif, par profil — métrique d'adoption.

`To be decided later` sur les métriques d'adoption attendues, non fournies par le PRD.

---

## Spécifications techniques

### Back

**Réutilisé, avec chemins.** L'exploration a écarté plusieurs briques à écrire :

- `allUsers` (`Neo4jConversationService.java:132`, issu de `userTargets`) fournit les destinataires **déjà résolus, groupes aplatis**, après l'envoi. Le déclenchement « via groupe » exigé par la FS ne demande donc aucune re-résolution de groupes.
- Un précédent d'itération sur tous les destinataires existe déjà en production : la boucle `updateUserQuota` (`ConversationController.java:470-477`). L'ordre de grandeur d'un balayage de `allUsers` est donc connu et accepté.
- Le pattern cron est disponible si un traitement périodique s'avère nécessaire : `fr.wseduc.cron.CronTrigger`, expression pilotée par la config, handler `implements Handler<Long>` (`Conversation.java:98-118`, `cron/PurgeMessages.java`).
- Le pattern de migration est établi : fichiers incrémentaux `NNN-conversation-*.sql`, additifs, `IF NOT EXISTS`, index partiels. Dernier livré : `024`. Le pattern complet « flag + trigger + index partiel + balayage par cron » est documenté dans `024-conversation-orphan-flag.sql`.
- La restriction de profil n'exige **aucun droit workflow** : `UserInfos.getType()` suffit côté back, `user.type` côté front. Valeurs canoniques `Teacher` / `Personnel` (`SSOAzure.java:75`).

**À créer.**

Migration `025` — paramétrage :

```sql
CREATE TABLE conversation.absence_settings (
    user_id    VARCHAR PRIMARY KEY,
    start_at   TIMESTAMPTZ NOT NULL,
    end_at     TIMESTAMPTZ NOT NULL,
    enabled    BOOLEAN NOT NULL DEFAULT FALSE,
    body_json  JSONB NOT NULL,
    body_html  TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

La PK sur `user_id` matérialise le principe d'unicité de la FS : aucune règle applicative à écrire. La désactivation passe `enabled` à `false` **sans supprimer la ligne**, ce qui satisfait la persistance du paramétrage exigée par la FS et par US-3.

**Aucun index supplémentaire.** Un index partiel `(user_id) WHERE enabled` figurait dans une version antérieure de ce cadrage : il est retiré, la colonne indexée étant déjà la clé primaire, qui sert donc à elle seule le `= ANY($1)` de la détection en lot (US-2). À revoir si la table atteint une volumétrie où le filtre de période justifie un index sur les bornes — ce n'est pas le cas au démarrage, la table partant vide.

Migration `026` — garde-fou anti-spam :

```sql
CREATE TABLE conversation.absence_replies (
    absent_user_id VARCHAR NOT NULL,
    sender_id      VARCHAR NOT NULL,
    last_sent_at   TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (absent_user_id, sender_id)
);
```

Alimentée en upsert (`ON CONFLICT DO UPDATE`) : une ligne par paire, pas une par envoi, donc pas de croissance non bornée. Le test « une réponse par jour » s'évalue dans le fuseau de l'expéditeur, via `date_trunc('day', last_sent_at AT TIME ZONE $tz) = date_trunc('day', now() AT TIME ZONE $tz)`.

**Un seul fuseau par envoi.** Un groupement des requêtes par fuseau figurait dans une version antérieure de ce cadrage : il est retiré, car il n'y a rien à grouper. Le fuseau est celui de l'expéditeur du message initial, et un envoi n'a qu'un seul expéditeur — toutes les réponses automatiques déclenchées par un même envoi partagent donc `$tz`. C'est la même dissymétrie que celle relevée plus bas pour la langue du préfixe d'objet, prise dans l'autre sens : la langue est celle de chaque personne absente, donc à lotir ; le fuseau est celui de l'unique expéditeur, donc constant sur tout le lot.

**Contrat d'API** — deux routes, format Postman (Swagger abandonné) :

| Verbe | Route | Rôle |
|---|---|---|
| `GET` | `/conversation/absence` | Paramétrage de l'utilisateur courant, ou vide |
| `PUT` | `/conversation/absence` | Upsert : couvre création, modification et désactivation |

Charge utile en entrée du `PUT` : `{ enabled, startAt, endAt, bodyJson, bodyHtml }` — `bodyJson` **requis** ; `bodyHtml`, s'il est envoyé, est **ignoré côté back** (accepté pour tolérance de forme, jamais persisté tel quel, jamais utilisé pour dériver quoi que ce soit). Pas de `DELETE`, la désactivation devant conserver les données. Purement additif, donc **aucune rupture de compatibilité mobile** ; le mobile consomme `bodyHtml`, dérivé côté back et renvoyé par le `GET`.

**Oubli corrigé au cadrage — normalisation côté back.** Le `bodyJson` reçu ne doit pas être stocké tel quel : il passe par le transformer déjà utilisé pour les messages classiques (`SqlConversationService.updateMessageWithTransformedContent`, client `IContentTransformerClient` initialisé dans `Conversation.java:73-76`), qui normalise le contenu et permet d'en dériver le `body_html` stocké. `body_json` et `body_html` stockés sont tous deux des **sorties** du transformer, jamais des entrées brutes — le `bodyHtml` éventuellement reçu en entrée du `PUT` est **explicitement ignoré**, à documenter clairement dans le contrat d'API pour éviter toute confusion côté consommateurs.

Confirmé : le transformer accepte bien le JSON tiptap en entrée. `transformMessageContent` (`SqlConversationService.java:1075-1094`) demande déjà les **deux** formats en sortie via `expectedFormats` — `new HashSet<>(Arrays.asList(ContentTransformerFormat.HTML, ContentTransformerFormat.JSON))` (`SqlConversationService.java:1078`) — mais l'appelant actuel (`updateMessageWithTransformedContent`) ne lit que `getCleanHtml()` et ignore la partie JSON de la réponse. Pour le message d'absence, il faut réutiliser ce même appel (`expectedFormats = {HTML, JSON}`) et, cette fois, exploiter **les deux** sorties de `ContentTransformerResponse` : le HTML nettoyé pour `body_html`, le JSON normalisé pour `body_json`. Le getter exact côté JSON (`ContentTransformerResponse` vient de la lib externe `fr.wseduc:content-transformer`, non vendée dans ce repo) est à vérifier au moment de coder IMPULS-6137.

**Anti-bouclage et exclusion des statistiques, par construction.** `eventHelper.onCreateResource(request, RESOURCE_NAME)` n'est appelé que dans le handler de la route `@Post("send")` (`ConversationController.java:517`) ; ni `saveAndSend` (436-491), ni le handler de bus (1785-1826), ni `sendFromExterne` (1939) ne l'appellent. Les statistiques sont donc alimentées par un événement émis explicitement à cet endroit, pas par une requête sur `messages`.

Le déclencheur se place dans ce même handler, une fois `allUsers` résolu. L'émission de la réponse automatique appelle `saveAndSend` en interne, donc ne repasse **jamais** par le handler de route : elle ne redéclenche pas le test d'absence, et n'est pas comptée dans les statistiques. Les deux exigences de la FS sont satisfaites sans colonne, sans paramètre à propager et sans filtre. Aucun marqueur persistant n'est nécessaire.

À noter : `noReply` (`021-conversation-noreply.sql`) signifie « réponse interdite » (`ConversationController.java:223, 506`) et **ne doit pas** être posé sur la réponse automatique — le Figma montre des actions disponibles sur le message reçu.

**Langue du préfixe d'objet.** Récupérée depuis les préférences de la **personne absente**, auteure de sa réponse, et lotie en masse pour limiter la pression : plusieurs milliers de personnes absentes peuvent être concernées par un même envoi. (Si la langue était celle du lecteur, il n'y aurait qu'un seul appel par envoi, toutes les réponses d'un envoi partant vers le même expéditeur initial — le besoin de lotissement confirme la lecture retenue.)

**Rollback.** Les trois évolutions sont additives et ignorées par la version antérieure du module : retour arrière = redéployer l'ancien module sans toucher au schéma, puis `DROP` à froid si besoin. Aucune modification des lignes existantes, donc aucune migration de masse sur une base à 500M+ messages — contrainte FS respectée.

### Front

**Réutilisé, avec chemins.**

- `SignatureModal.tsx` sert de gabarit de structure : `Modal size="lg"` → `Modal.Header` → `Modal.Body` (`Switch` + éditeur) → `Modal.Footer`.
- **Ouverture par portal, pas par store** : pattern `createPortal(<Modal/>, document.getElementById('portal'))` avec `isOpen` / `onModalClose` en props, comme `AddMessageAttachmentToWorkspaceModal.tsx`. Le store Zustand `openedModal` n'est **pas** utilisé.
- `AppActionHeader.tsx:21-29` : le menu contextuel est un simple tableau `dropdownOptions` avec un champ `visibility` déjà prévu — ajouter l'entrée absence est un item plus une icône.
- `DatePicker` du DS, avec le précédent d'implémentation `actualites/frontend/src/features/info-form/components/InfoDetailsFormDatesModal.tsx` : composition `FormControl` + `Label` + `DatePicker`, deux dates en `Flex` responsive, `dateFormat` via i18n.
- `FormControl` porte `status: 'valid' | 'invalid'` et un sous-composant `FormControl.Text` : l'affichage d'erreur en ligne sous le champ est **nativement fourni par le DS**, aucun enrobage à écrire.
- `Editor` du DS (`@edifice.io/react/editor`), et non le wrapper `SignatureEditor` qui n'expose que `getHtmlContent()`. Sa ref expose `getContent(as: 'html' | 'json' | 'plain')` et sa prop `content` accepte directement du `JSONContent` tiptap : les deux formats exigés sont obtenus du même composant, sans pattern nouveau. C'est bien « le même composant que la rédaction d'un message classique » que demandait la FS.
- `ButtonBeta` (edifice2d) pour les actions de pied de modale. Couleurs `default | destructive | secondary | tertiary`, variantes `filled | outline | ghost` — noter qu'il n'y a pas de `primary`.
- `Alert` du DS pour le bandeau, déjà utilisé dans le module (`components/MessageBody.tsx:75`) et acceptant une prop `button`, ce qui donne le bouton « Modifier » sans rien créer. Emplacement révisé le 30/07/2026 (voir ci-dessous).
- Cache et invalidation : le pattern exigé par la FS est déjà en place — clés `configQueryKeys`, `staleTime`, `setQueryData` optimiste au succès (`services/queries/config.ts`). Aucune sous-tâche de cache dédiée n'est nécessaire.

**À créer.**

Modale de paramétrage servant indifféremment création, modification et désactivation, pré-remplie s'il existe un paramétrage. Validation en ligne sans fermeture de modale, toast de confirmation, et **la modale reste ouverte** après enregistrement (voir risque #6). Les bornes de période sont calculées dans le fuseau local de l'utilisateur — début et fin de journée locale — puis converties en UTC avant envoi.

**Point d'insertion du bandeau — révisé le 30/07/2026.** La maquette v2 (Figma, node `7773-2968`) place le bandeau dans la colonne de contenu de l'écran de liste, au-dessus de la barre de recherche — dans `routes/pages/Folder.tsx` (ou un composant qu'il englobe, ex. à côté de `MessageListHeader`), pas au niveau du layout racine `routes/root/index.tsx`. `Folder.tsx` sert les quatre dossiers système (`inbox`, `outbox`, `draft`, `trash`) et les dossiers utilisateur (`routes/index.tsx:24-94`) : un seul point d'insertion suffit toujours pour **ces** écrans. En revanche `Message.tsx` (lecture, rédaction) est une route sœur, rendue à la place de `Folder.tsx` (jamais en même temps) — le bandeau n'y apparaît donc plus, ce qui est désormais l'exigence assumée (voir Limitations/Hors-scope et FS v5, US-4).

**Point technique à trancher (IMPULS-6151/6138)** : la modale garde **deux déclencheurs** — l'entrée de menu (`AppActionHeader`, restée au niveau racine) et le bouton « Modifier » du bandeau (désormais dans `Folder.tsx`, un descendant de la racine rendu via `ScrollableOutlet`). Le cadrage initial écartait le store Zustand `openedModal` au profit d'un `useState` unique remonté dans `routes/root/index.tsx`, parce que les deux déclencheurs cohabitaient dans le même fichier. Ce n'est plus le cas : deux options, à trancher en implémentation, pas figées ici :
  1. **`useOutletContext`** (React Router v6, déjà en dépendance) : `routes/root/index.tsx` passe l'ouvreur de modale via `<Outlet context={...} />` sur `ScrollableOutlet`, `Folder.tsx` le lit avec `useOutletContext()`. Garde le `useState` unique et évite le store, dans l'esprit de la décision initiale.
  2. **Réutiliser `openedModal`** (`useActionsStore`, déjà store de tous les autres modales de ce fichier — `CreateFolderModal`, `RenameFolderModal`, `TrashFolderModal`, `MoveMessageToFolderModal`, `SignatureModal`) : plus cohérent avec le reste de `routes/root/index.tsx` maintenant que le déclencheur n'est plus localisé au même endroit que la modale.

Restriction de profil : test sur le **premier élément** de `user.type` (et non `includes`), pour rester aligné sur la convention `HEAD(profiles)` du back (voir risque #7).

### Mobile

Pas de développement mobile dans ce chantier. Deux points d'attention néanmoins :

- Le contrat d'API est **purement additif** (deux nouvelles routes), donc sans rupture pour les applications mobiles qui consomment la même API.
- Le champ fuseau ajouté à la charge utile d'envoi doit être **optionnel**, avec repli serveur, pour ne pas casser les clients déployés (risque #4).
- Le mobile consomme `bodyHtml`, ce qui motive le maintien du double format de stockage.

---

## Découpage détaillé

### Étapes & dépendances

1. **Contrat d'API d'abord** — il débloque le développement back et front en parallèle, chacun s'appuyant ensuite sur le contrat arrêté.
2. **US-1** constitue le socle : table `absence_settings`, routes, modale. US-3 en découle presque gratuitement, la PK et le `PUT` en upsert faisant le travail.
3. **US-2** est indépendante de US-1 côté front, mais dépend de sa table côté back. En son sein, l'ordre est : migration `026` → détection → émission et garde-fou → étalement → branchement dans la route.
4. **US-4** réutilise le hook query de US-1, donc arrive après lui.

### Sous-tâches par US

Les 17 sous-tâches ont été créées dans Jira. Le contrat d'API, préalable commun à tout le chantier, est rattaché à US-1 faute de porteur dédié — c'est bien la **première** à traiter.

#### US-1 : Paramétrer, activer et modifier un message d'absence *(IMPULS-6130)*

- [ ] `[back]` [IMPULS-6135](https://edifice-community.atlassian.net/browse/IMPULS-6135) — Créer le contrat d'API au format Postman (`GET` / `PUT /conversation/absence`). **Préalable commun, à traiter en premier** — livrable : collection Postman — **2**
- [ ] `[back]` [IMPULS-6136](https://edifice-community.atlassian.net/browse/IMPULS-6136) — Migration `025` : table `absence_settings` (`start_at` / `end_at` en `TIMESTAMPTZ`), PK sur `user_id`, sans index supplémentaire — **2**
- [ ] `[back]` [IMPULS-6137](https://edifice-community.atlassian.net/browse/IMPULS-6137) — Service et routes `GET` / `PUT` : validation des dates et du texte, restriction de profil sur `HEAD(profiles)`, stockage `body_json` + `body_html` — **5**
- [ ] `[front]` [IMPULS-6138](https://edifice-community.atlassian.net/browse/IMPULS-6138) — Modale de paramétrage : `Modal` + `Switch` + 2 `DatePicker` en `FormControl` / `Label` / `FormControl.Text` + `Editor` du DS, erreurs en ligne, toast, modale qui reste ouverte, calcul des bornes locales converties en UTC, clés i18n `fr` — **5**
- [ ] `[front]` [IMPULS-6139](https://edifice-community.atlassian.net/browse/IMPULS-6139) — Service API et hooks TanStack Query (query + mutation avec invalidation) — **3**
- [ ] `[front]` [IMPULS-6140](https://edifice-community.atlassian.net/browse/IMPULS-6140) — Entrée de menu contextuel, `useState` unique remonté dans `routes/root/index.tsx`, ouverture par `createPortal`, visibilité selon le premier profil — **2**

#### US-2 : Recevoir la réponse automatique en tant qu'expéditeur *(IMPULS-6131)*

- [ ] `[back]` [IMPULS-6141](https://edifice-community.atlassian.net/browse/IMPULS-6141) — Migration `026` : table `absence_replies` (`last_sent_at` en `TIMESTAMPTZ`) — **1**
- [ ] `[back]` [IMPULS-6142](https://edifice-community.atlassian.net/browse/IMPULS-6142) — Service de détection : requête en lot sur `allUsers`, filtre sur la période active, et récupération en masse des préférences de langue des personnes absentes — **5**
- [ ] `[back]` [IMPULS-6143](https://edifice-community.atlassian.net/browse/IMPULS-6143) — Émission de la réponse automatique via `saveAndSend`, objet préfixé « Réponse automatique : », garde-fou une réponse par jour et par expéditeur en upsert — **5**
- [ ] `[back]` [IMPULS-6144](https://edifice-community.atlassian.net/browse/IMPULS-6144) — Étalement asynchrone de l'émission et throttling, pour tenir les envois de masse sans dégrader la plateforme — **5** *(ramené de 8 : le groupement des requêtes par fuseau, initialement inclus, est retiré — un envoi n'a qu'un seul fuseau, voir §Back, migration `026`)*
- [ ] `[back]` [IMPULS-6145](https://edifice-community.atlassian.net/browse/IMPULS-6145) — Branchement du déclencheur dans le handler `@Post("send")`, après résolution de `allUsers` — **2**
- [ ] `[back]` [IMPULS-6146](https://edifice-community.atlassian.net/browse/IMPULS-6146) — Accepter le fuseau de l'expéditeur en champ **optionnel** de la charge utile d'envoi, avec repli sur le fuseau serveur — **2**
- [ ] `[back]` [IMPULS-6148](https://edifice-community.atlassian.net/browse/IMPULS-6148) — Purge RGPD : ajouter `absence_settings` et `absence_replies` à `ConversationRepositoryEvents.deleteUsers()` — **1**
- [ ] `[front]` [IMPULS-6147](https://edifice-community.atlassian.net/browse/IMPULS-6147) — Transmettre le fuseau de l'expéditeur dans la charge utile d'envoi — **1**

#### US-3 : Désactiver un message d'absence avant la fin prévue *(IMPULS-6132)*

- [ ] `[front]` [IMPULS-6149](https://edifice-community.atlassian.net/browse/IMPULS-6149) — Toggle inactif : enregistrement en `enabled = false`, champs conservés et atténués visuellement — **2**
- [ ] `[back]` [IMPULS-6150](https://edifice-community.atlassian.net/browse/IMPULS-6150) — Tests d'intégration de la désactivation, dont le cas d'un expéditeur déjà relancé le jour même — **2**

#### US-4 : Voir en permanence l'état de mon message d'absence actif *(IMPULS-6133)*

- [ ] `[front]` [IMPULS-6151](https://edifice-community.atlassian.net/browse/IMPULS-6151) — Bandeau `Alert` avec bouton « Modifier », inséré dans `Folder.tsx` (écrans de liste uniquement, révisé 30/07/2026 — voir §Front), conditionné à l'état actif — **3**

---

## Feature flag

`To be decided later`. Le module n'a aucun mécanisme de feature flag existant, et rien dans la FS ne réclame de pouvoir couper le déclencheur à chaud — la restriction de profil suffit à cadrer l'exposition.

Si le besoin apparaît, le point d'ancrage est le bloc `config` du module dans `ent-core.json.template` (templaté depuis `default.properties`), lu par `Conversation.java` — **et non `mod.json`, qui relève d'une évolution en cours pour k8s**. Pour un flag visible du front, la clé se place dans le sous-bloc `publicConf`, déjà consommé par `usePublicConfig()` / `models/publicConf.ts`.

La création d'un droit workflow dédié a été écartée : fastidieuse à gérer, et redondante avec la restriction de profil issue de la session.

---

## Charge macro (estimation)

| Compétence | Charge (SP) | Commentaire |
|---|---|---|
| Back | 32 | Dont 5 sur le seul étalement de l'émission (risque #1) et 2 sur le contrat d'API préalable |
| Front | 16 | Essentiellement de l'assemblage de composants du DS ; aucun composant à créer |
| Mobile | 0 | Hors-scope. Contrat additif, champ fuseau optionnel, consommation de `bodyHtml` |
| **Total** | **48** | Aucune sous-tâche au-delà de 5 |

---

## Procédures d'exploitation

- **Aucune affectation de droit workflow n'est nécessaire** — c'est le bénéfice direct du choix de s'appuyer sur le profil de session. Ce point figurait initialement comme risque fort de MEP ; il est supprimé.
- Deux migrations SQL à appliquer dans l'ordre (`025` puis `026`), additives, sans réécriture de lignes existantes.
- Monitoring à prévoir sur le volume de réponses automatiques émises par envoi et sur la durée de traitement du lot (voir Métriques produit) — ce sont les deux signaux d'alerte du risque #1.
- Purge éventuelle de `absence_replies` : réutiliser le pattern `CronTrigger` déjà en place plutôt qu'introduire un nouveau mécanisme de scheduling, conformément aux contraintes techniques de la FS.

---

## Stratégie de déploiement

- Ordre : migrations `025` et `026`, puis back, puis front. Le front sans le back n'expose rien d'utilisable ; le back sans le front est inerte, aucun paramétrage ne pouvant exister.
- Aucun rattrapage de données : la fonctionnalité démarre avec des tables vides.
- Coordination requise avec la fonctionnalité d'envoi différé, en cours de développement sur la même table et le même chemin d'envoi (risque #3), avant merge.
- Recette : prévoir explicitement un compte **multi-profil** (risque #7) et un client mobile ne transmettant pas le fuseau (risque #4).

---

## Questions ouvertes

- **Métriques produit attendues** : non fournies par le PRD ni la FS. `To be decided later`, à trancher par le PO/PM.
- **Feature flag** : `To be decided later`, voir la section dédiée.
- **Troisième entrée du menu contextuel** : le Figma en montre trois, le code n'en a qu'une (signature). Seule l'entrée absence est ajoutée ; la troisième est considérée hors sujet. `Hypothesis to be validated`.
- **Partage d'état de la modale entre `AppActionHeader` (racine) et le bandeau (`Folder.tsx`)** : `useOutletContext` ou réutilisation du store `openedModal` — voir §Front, révision du 30/07/2026. `To be decided later`, à trancher en implémentation (IMPULS-6138/6151), sans impact sur l'estimation.
