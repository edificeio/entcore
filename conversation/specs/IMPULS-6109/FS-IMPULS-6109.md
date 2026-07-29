# Feature Spec FS-01 : Message d'absence

> Projet : Messagerie
> Statut : Finalisé
> Version : v3 - 29/07/2026
> Langue : Français
> Périmètre : Autonome
> PRD parent : PRD - Message d'absence
> Glossaire : aucun

---

## 1. Introduction

*Owner : PO*

Le Message d'absence permet à un utilisateur de la Messagerie de programmer une réponse automatique informant de son indisponibilité pendant une période donnée (congés, arrêt, absence). Cette FS couvre l'ensemble du besoin décrit dans le PRD parent : elle n'est pas découpée, le périmètre restant resserré autour d'un seul acteur qui paramètre et d'un ensemble d'expéditeurs qui reçoivent la réponse. Ce document sert d'artefact central au développement assisté par IA : toute décision non tranchée est marquée explicitement (`To be decided later`, `Don't know yet`, `Hypothesis to be validated`) et doit être résolue avant développement.

---

## 2. Problématique

*Owner : PO*

Aujourd'hui, un utilisateur absent une ou plusieurs journées entières (justifié ou non) n'a aucun moyen de signaler son indisponibilité aux personnes qui lui écrivent dans la Messagerie. Les expéditeurs, qu'il s'agisse de personnel, d'enseignants, de parents ou d'élèves, n'ont aucune confirmation que leur message sera traité ni aucune idée du délai de réponse. Cette absence de signal se traduit concrètement par des messages sans réponse pendant plusieurs jours ou semaines, de l'incertitude côté expéditeur, et une charge de rattrapage importante pour l'absent à son retour. Le besoin est documenté depuis plus d'un an dans les retours utilisateurs (20 remontées, dont 4 critiques, sur des académies et comptes variés), et son absence contribue à l'image d'une messagerie qui manque de fonctionnalités standards, déjà citée comme facteur de remplacement par des messageries tierces sur certains comptes.

---

## 3. Aperçu de la solution

*Owner : PO*

L'utilisateur (personnel administratif ou enseignant) accède au paramétrage de son message d'absence depuis le menu principal de la Messagerie. Il y sélectionne une période d'absence (date de début et date de fin) et saisit le texte de son message. Une fois activé, chaque personne qui lui envoie un message pendant cette période reçoit automatiquement en retour ce message, à chaque message envoyé (dans la limite d'un envoi par jour et par expéditeur, voir principes ci-dessous). Tant que le message d'absence est actif, un bandeau de rappel reste visible sur tous les écrans de la Messagerie.

Sont dans le périmètre de cette FS : le paramétrage d'une période et d'un texte, l'activation et la désactivation manuelle, la modification d'un paramétrage déjà activé, le rappel permanent de l'état actif via un bandeau, et le déclenchement de la réponse automatique pour tout expéditeur (personnel, enseignant, parent ou élève), qu'il écrive à la personne absente nominativement ou via un groupe dont elle est membre.

Sont hors périmètre : la redirection des messages vers un autre destinataire, la gestion de plages horaires hors journées d'absence, le paramétrage par un tiers (ADML) pour le compte d'un autre utilisateur, et la gestion de plusieurs périodes d'absence en parallèle pour un même utilisateur.

### Principes de design

*Owner : PO (intentions), validé par Design*

1. **Non-bouclage** : une réponse automatique ne doit jamais déclencher elle-même une nouvelle réponse automatique, même si l'expéditeur a également un message d'absence actif. Le système doit se comporter comme si la réponse automatique n'était jamais un message qui « déclenche » une absence chez son destinataire. Comportement invisible pour l'utilisateur, sans pattern UI dédié.
2. **Une réponse par jour et par expéditeur** : pour éviter le spam, un même expéditeur ne reçoit qu'une seule réponse automatique par jour pendant la période d'absence, quel que soit le nombre de messages qu'il envoie ce jour-là à la personne absente. Comportement invisible pour l'utilisateur, sans pattern UI dédié.
3. **Identification claire** : la réponse automatique doit être immédiatement reconnaissable comme telle par l'expéditeur, et ne pas se confondre avec une réponse personnelle rédigée par la personne absente. Traduit en design par le préfixe d'objet (voir chapitre 5).
4. **Unicité du paramétrage** : un utilisateur ne peut avoir qu'un seul message d'absence actif ou programmé à la fois. Il n'existe pas de notion de périodes multiples empilées. Traduit en design par un écran unique de création/édition (voir chapitre 5).
5. **Persistance du paramétrage** : désactiver un message d'absence avant son terme ne doit pas faire perdre les informations saisies (dates, texte) ; l'utilisateur doit pouvoir réactiver rapidement sans tout ressaisir. Traduit en design par le toggle actif/inactif (voir chapitre 5).
6. **Visibilité permanente** : tant que le message d'absence est actif, l'utilisateur doit pouvoir s'en souvenir et le retrouver facilement depuis n'importe quel écran de la Messagerie, sans avoir à retourner spécifiquement au menu de paramétrage. Traduit en design par le bandeau de rappel (voir chapitre 5, US-4).

---

## 4. Cas d'usage

*Owner : PO (cas d'usage + critères fonctionnels), Owner : Design (scénarios de test Gherkin)*

### US-1 : Paramétrer, activer et modifier un message d'absence

**En tant que** personnel administratif ou enseignant
**je veux** définir, puis au besoin modifier, une période d'absence et un texte de message
**afin de** informer automatiquement les personnes qui m'écrivent de mon indisponibilité, et ajuster cette information si besoin sans devoir tout recréer

**Critères d'acceptation :**

- L'accès au paramétrage se fait depuis le menu principal de la Messagerie (et, une fois actif, depuis le bandeau de rappel, voir US-4).
- Une date de fin est obligatoire et doit être égale ou postérieure à la date de début.
- Le texte du message est obligatoire pour pouvoir activer le paramétrage.
- Le texte est saisi avec l'éditeur riche utilisé pour le corps des messages (même composant que la rédaction d'un message classique).
- Une fois activé, le paramétrage est actif automatiquement dès la date de début, sans action supplémentaire de l'utilisateur.
- Un utilisateur ne peut avoir qu'un seul message d'absence actif ou programmé à la fois.
- Le texte et les dates (début et fin) restent modifiables pendant que le message d'absence est actif, via le même écran que la création (pré-rempli avec les valeurs existantes).
- Si la date de fin est modifiée à une date antérieure ou égale à aujourd'hui, cela équivaut à désactiver le message d'absence (voir US-3).
- Il n'y a pas de contrainte empêchant de saisir une date de début dans le passé, aussi bien à la création qu'à la modification.

**Scénarios de test :**

```gherkin
Scénario: Activation d'un message d'absence avec des dates et un texte valides
  Étant donné je suis connecté en tant que personnel administratif ou enseignant
  Et je n'ai aucun message d'absence actif ou programmé
  Quand j'ouvre la modale "Paramétrer le message d'absence" depuis le menu contextuel du bandeau
  Et je saisis une date de début et une date de fin égale ou postérieure à la date de début
  Et je saisis un texte dans l'éditeur riche
  Et j'active le toggle et je clique sur "Enregistrer"
  Alors un toast de confirmation DOIT s'afficher
  Et la modale NE DOIT PAS se fermer automatiquement
  Et le message d'absence DOIT devenir actif à la date de début renseignée
```

```gherkin
Scénario: Tentative d'activation avec une date de fin antérieure à la date de début
  Étant donné je suis en train de paramétrer mon message d'absence
  Quand je saisis une date de fin antérieure à la date de début
  Et je clique sur "Enregistrer"
  Alors une erreur DOIT s'afficher sous le champ date de fin
  Et le message d'absence NE DOIT PAS être activé
  Et la modale NE DOIT PAS se fermer
```

```gherkin
Scénario: Tentative d'activation avec un texte vide
  Étant donné je suis en train de paramétrer mon message d'absence
  Et le champ texte est vide
  Quand je clique sur "Enregistrer"
  Alors une erreur DOIT s'afficher sous l'éditeur de texte
  Et le message d'absence NE DOIT PAS être activé
```

```gherkin
Scénario: Ouverture du paramétrage alors qu'un message d'absence existe déjà
  Étant donné j'ai déjà un message d'absence actif ou programmé
  Quand j'ouvre la modale "Paramétrer le message d'absence"
  Alors le formulaire DOIT être pré-rempli avec les dates et le texte déjà enregistrés
  Et le toggle DOIT refléter l'état actif ou inactif actuel
  Et aucun nouvel écran de création NE DOIT être proposé
```

```gherkin
Scénario: Modification du texte pendant que l'absence est en cours
  Étant donné un message d'absence est actif avec un texte "A"
  Quand j'ouvre la modale de paramétrage et je remplace le texte par "B"
  Et je clique sur "Enregistrer"
  Alors les nouveaux expéditeurs DOIVENT recevoir le texte "B" en réponse automatique
  Et le message d'absence DOIT rester actif sans interruption
```

```gherkin
Scénario: Modification de la date de fin à une date passée
  Étant donné un message d'absence est actif
  Quand je modifie la date de fin pour une date antérieure ou égale à aujourd'hui
  Et je clique sur "Enregistrer"
  Alors le message d'absence DOIT devenir inactif immédiatement
  Et aucune nouvelle réponse automatique NE DOIT être envoyée après l'enregistrement
```

```gherkin
Scénario: Modification de la date de début alors que la période est déjà en cours
  Étant donné un message d'absence est actif depuis 3 jours
  Quand je modifie la date de début, y compris pour une date passée
  Et je clique sur "Enregistrer"
  Alors le message d'absence DOIT respecter la nouvelle date de début saisie
  Et aucune erreur de validation liée à la date de début NE DOIT s'afficher
```

### US-2 : Recevoir la réponse automatique en tant qu'expéditeur

**En tant qu'** expéditeur (personnel, enseignant, parent ou élève)
**je veux** être informé automatiquement quand la personne à qui j'écris est absente
**afin de** ne pas rester dans l'incertitude sur le traitement de mon message et connaître la période de retour

**Critères d'acceptation :**

- La réponse automatique se déclenche que la personne absente soit destinataire nominatif du message, ou destinataire via son appartenance à un groupe.
- La réponse automatique reprend le texte paramétré par la personne absente.
- Un même expéditeur ne reçoit pas plus d'une réponse automatique par jour de la part d'une même personne absente, même s'il lui envoie plusieurs messages ce jour-là.
- La réponse automatique ne doit jamais elle-même déclencher une autre réponse automatique en retour (y compris si l'expéditeur a lui aussi un message d'absence actif).
- La réponse automatique doit être clairement identifiable comme telle par l'expéditeur (objet préfixé et/ou indicateur visuel), et ne pas se confondre avec une réponse personnelle.

**Scénarios de test :**

```gherkin
Scénario: Un expéditeur nominatif reçoit la réponse automatique
  Étant donné un utilisateur a un message d'absence actif avec le texte "Je suis en congés jusqu'au 30/08"
  Quand un expéditeur lui envoie un message avec pour objet "Question sur la réunion"
  Alors l'expéditeur DOIT recevoir en retour un message avec pour objet "Réponse automatique : Question sur la réunion"
  Et le corps du message reçu DOIT reprendre le texte "Je suis en congés jusqu'au 30/08"
```

```gherkin
Scénario: Déclenchement de la réponse automatique via un groupe
  Étant donné un utilisateur absent est membre du groupe "Enseignants du groupe CM2"
  Et cet utilisateur a un message d'absence actif
  Quand un expéditeur envoie un message au groupe "Enseignants du groupe CM2"
  Alors l'expéditeur DOIT recevoir une réponse automatique de la part de l'utilisateur absent
```

```gherkin
Scénario: Un même expéditeur envoie plusieurs messages le même jour
  Étant donné un expéditeur a déjà reçu une réponse automatique aujourd'hui de la part d'un utilisateur absent
  Quand ce même expéditeur envoie un second message le même jour à cet utilisateur
  Alors l'expéditeur NE DOIT PAS recevoir une seconde réponse automatique le même jour
```

```gherkin
Scénario: Deux utilisateurs absents s'écrivent mutuellement
  Étant donné l'utilisateur A et l'utilisateur B ont chacun un message d'absence actif
  Quand l'utilisateur B envoie un message classique à l'utilisateur A
  Alors l'utilisateur B DOIT recevoir la réponse automatique du message d'absence de l'utilisateur A
  Et cette réponse automatique reçue par l'utilisateur B NE DOIT PAS déclencher à son tour l'envoi de la réponse automatique de l'utilisateur B
```

```gherkin
Scénario: La réponse automatique envoyée est visible dans les messages envoyés
  Étant donné un utilisateur absent a un message d'absence actif
  Quand un expéditeur lui envoie un message et reçoit la réponse automatique
  Alors la réponse automatique DOIT apparaître dans le dossier "Messages envoyés" de l'utilisateur absent
  Et elle DOIT afficher le même objet préfixé "Réponse automatique : ..."
```

### US-3 : Désactiver un message d'absence avant la fin prévue

**En tant que** personnel administratif ou enseignant
**je veux** pouvoir arrêter manuellement mon message d'absence avant la date de fin initialement prévue
**afin de** ne plus déclencher de réponses automatiques si je reprends mon activité plus tôt que prévu

**Critères d'acceptation :**

- La désactivation est accessible depuis le même accès de paramétrage que l'activation.
- Après désactivation, aucune nouvelle réponse automatique n'est envoyée, même aux expéditeurs déjà relancés ce jour-là.
- Les dates et le texte précédemment saisis sont conservés après désactivation, pour permettre une réactivation rapide sans tout ressaisir.

**Scénarios de test :**

```gherkin
Scénario: Désactivation immédiate via le toggle
  Étant donné un message d'absence est actif
  Quand je bascule le toggle sur "inactif" dans la modale de paramétrage
  Et je clique sur "Enregistrer"
  Alors un toast de confirmation DOIT s'afficher
  Et aucune nouvelle réponse automatique NE DOIT être envoyée après la désactivation
  Et les dates et le texte précédemment saisis DOIVENT rester visibles dans le formulaire
```

```gherkin
Scénario: Un expéditeur déjà relancé le jour de la désactivation
  Étant donné un expéditeur a déjà reçu une réponse automatique aujourd'hui
  Quand la personne absente désactive son message d'absence plus tard dans la même journée
  Et ce même expéditeur envoie un nouveau message
  Alors l'expéditeur NE DOIT PAS recevoir de nouvelle réponse automatique
```

### US-4 : Voir en permanence l'état de mon message d'absence actif

**En tant que** personnel administratif ou enseignant
**je veux** voir un rappel visible en permanence tant que mon message d'absence est actif
**afin de** ne pas oublier qu'il est actif et pouvoir le modifier rapidement depuis n'importe quel écran de la Messagerie

**Critères d'acceptation :**

- Le bandeau de rappel est visible sur tous les écrans de la Messagerie tant qu'un message d'absence est actif.
- Le bandeau affiche un bouton "Modifier".
- Cliquer sur "Modifier" ouvre la modale de paramétrage du message d'absence, pré-remplie avec les valeurs actuelles (voir US-1).
- Le bandeau disparaît automatiquement dès que le message d'absence n'est plus actif (désactivation manuelle ou date de fin dépassée).

**Scénarios de test :** *(à compléter par le Designer)*

---

## 5. Brief design produit UX/UI

*Owner : Design*

La modale « Paramétrer le message d'absence » réutilise directement le pattern existant de la modale « Paramétrer la signature » : même point d'entrée (menu contextuel à droite du bouton « Nouveau message »), même composant d'édition (éditeur riche transverse), même bouton « Enregistrer », auquel s'ajoutent deux champs de date (début et fin). Un toggle en haut de la modale porte l'état actif/inactif : quand il est sur inactif, les champs restent visibles et modifiables mais atténués visuellement, conformément au principe de persistance.

Le même écran sert à la fois pour la création et la modification : s'il existe déjà un paramétrage (actif ou désactivé), la modale s'ouvre systématiquement pré-remplie, sans écran de création distinct. Cela matérialise directement le principe d'unicité posé par le PO.

Les erreurs de validation (date de fin antérieure au début, texte vide) s'affichent en ligne, sous le champ concerné, sans fermer la modale. Après un enregistrement réussi (activation, modification ou désactivation), un toast de confirmation s'affiche, mais la modale reste ouverte tant que l'utilisateur n'a pas cliqué sur la croix de fermeture.

Côté identification, la réponse automatique reçue par l'expéditeur porte un objet préfixé « Réponse automatique : [objet original] », sans badge ni bandeau supplémentaire dans le corps du message. Cette même réponse est visible dans le dossier Messages envoyés de la personne absente, avec le même préfixe, pour traçabilité.

Un bandeau de rappel est affiché dans le bandeau du module Messagerie (à côté du titre « Messagerie »), visible sur tous les écrans de l'application tant que le message d'absence est actif (US-4). Il porte un bouton « Modifier » qui ouvre la modale de paramétrage existante, pré-remplie : le bandeau ne duplique donc pas le formulaire, il agit comme raccourci d'accès.

Aucun nouveau composant n'est créé : l'éditeur riche, le pattern modale/toggle et le mécanisme de toast sont repris tels quels des écrans existants (signature, planification d'envoi différé).

### Décisions UX/UI ouvertes

Toutes les décisions UX/UI identifiées en phase PO ont été tranchées durant cette session (emplacement du paramétrage, forme de l'indicateur d'identification, affichage de l'état actif, comportement du bandeau de rappel). Aucune décision UX/UI ouverte à ce stade.

---

## 6. Exigences techniques

*Owner : Tech lead*

- **Stack** : développement front dans le repo `entcore/conversation` (React, composants du design system `edifice-frontend-framework`) ; développement back dans le repo `entcore` (Java 8). Aucun service tiers requis, la Messagerie est un service interne synchrone.
- **Traitement asynchrone** : la détection d'un message d'absence actif et l'envoi de la réponse automatique sont traités de manière asynchrone via l'event bus Vert.x déjà utilisé sur la plateforme, sans bloquer l'envoi du message initial (le coût d'un traitement synchrone est jugé non acceptable).
- **Anti-bouclage** : à réception d'un message, le système doit déterminer si le message entrant est lui-même une réponse automatique (pour ne jamais y répondre par une nouvelle réponse automatique), et déterminer si le destinataire a un message d'absence actif avant de déclencher l'envoi.
- **Limitation à une réponse par jour et par expéditeur** : nécessite un état persistant associant (personne absente, expéditeur, jour) pour éviter les envois multiples, consulté avant chaque déclenchement.
- **Persistance du paramétrage** : dates de début/fin, texte et statut actif/inactif sont conservés en base, y compris après désactivation (pas de suppression des données à la désactivation).
- **Stockage du texte riche** : le texte du message d'absence est stocké à la fois en JSON et en HTML en base ; le frontend consomme uniquement le JSON (cohérent avec le fonctionnement actuel de l'éditeur riche).
- **Volumétrie et SLA** : le mécanisme doit rester fonctionnel et garanti dans les cas d'envois de masse (jusqu'à 200 000 destinataires simultanés observés aujourd'hui sur la Messagerie), avec un traitement complet dans une marge de quelques minutes, sans dégrader le service pour les autres utilisateurs de la plateforme.
- **Droits d'accès** : seuls les profils enseignant et personnel administratif peuvent créer/paramétrer un message d'absence pour leur propre compte (pas de paramétrage pour le compte d'un tiers, cf. contraintes fonctionnelles).
- **Compatibilité API mobile** : le contrat d'API exposé pour le paramétrage et le déclenchement du message d'absence ne doit pas introduire de rupture de compatibilité avec les versions antérieures de l'API consommées par les applications mobiles.
- **Bandeau de rappel (US-4)** : l'état actif du message d'absence est mis en cache côté front pour permettre l'affichage du bandeau sur tous les écrans sans appel réseau répété par écran. Ce cache doit être invalidé côté front à chaque modification du paramétrage (activation, modification, désactivation). Pas de cache nécessaire côté back.
- **Absence d'exigence d'audit** : aucun besoin de traçabilité ou d'audit dédié au-delà de la visibilité déjà native de la réponse automatique dans le dossier « Messages envoyés » de la personne absente.
- **Exclusion des statistiques** : les réponses automatiques ne doivent pas être comptabilisées dans les statistiques de messages envoyés existantes.
- **Dépendances** : aucune dépendance technique identifiée avec les autres évolutions en cours de la Messagerie (envoi différé, refonte back, accusé de lecture).

---

## 7. Contraintes

### Contraintes fonctionnelles

*Owner : PO*

- Hors périmètre : redirection des messages vers un autre destinataire.
- Hors périmètre : gestion de plages horaires hors journées d'absence.
- Hors périmètre : paramétrage du message d'absence par un tiers (ADML) pour le compte d'un autre utilisateur.
- Hors périmètre : plusieurs périodes d'absence programmées en parallèle pour un même utilisateur.
- Parents et élèves restent hors périmètre en tant qu'utilisateurs pouvant paramétrer un message d'absence ; ils sont en revanche dans le périmètre en tant qu'expéditeurs pouvant recevoir la réponse automatique.
- Les réponses automatiques ne sont pas comptabilisées dans les statistiques de messages envoyés.

### Contraintes techniques

*Owner : Tech lead*

- Pas de système de queue dédié disponible : le développement doit s'appuyer sur l'event bus asynchrone Vert.x déjà en place.
- Réutiliser les crons existants si un traitement périodique s'avère nécessaire (ex. activation/désactivation automatique à date), plutôt que d'introduire un nouveau mécanisme de scheduling.
- Toute évolution du schéma de données doit être rollbackable : réversible directement en base, ou conçue pour rester compatible avec une version antérieure du module en cas de retour arrière.
- Vu la volumétrie de la base Messagerie (3 tables principales, 500M+ messages, environ 5 ans d'historique), toute migration de données doit être traitée avec une extrême précaution ; privilégier des évolutions qui n'impliquent pas de modification massive des lignes existantes.
- Le frontend doit s'appuyer exclusivement sur les composants existants du design system partagé `edifice-frontend-framework` (Button, Input, DatePicker, éditeur riche, Switch...), sans recréer de composants équivalents.
- Le contrat d'API ne doit pas casser la compatibilité avec les applications mobiles, qui consomment la même API que le frontend web.
- Le cache front de l'état actif du message d'absence (utilisé pour le bandeau US-4) doit être invalidé à chaque modification du paramétrage.

### Questions ouvertes

Aucune question ouverte à ce stade. Les 4 points précédemment ouverts ont été tranchés :

- Comptage des réponses automatiques dans les stats : tranché par le PM, exclues des statistiques.
- Bandeau de rappel sans US : tranché par le PO/PM, US-4 créée.
- Redondance US-4/US-1 : tranché par le PO, fusion dans US-1.
- Faisabilité du bandeau permanent : tranché par le Tech lead, faisable via cache front à invalider à chaque modification.
