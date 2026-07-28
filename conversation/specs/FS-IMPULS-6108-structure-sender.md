# Feature Spec FS-01 : Identification de l'établissement émetteur

> Projet : Messagerie
> Statut : Finalisé
> Version : v5 - 27/07/2026
> Langue : Français
> Périmètre : Autonome
> PRD parent : PRD - Identification établissement émetteur
> Maquettes : https://www.figma.com/design/B8KkuSYSpB3SZYnM3MDRJB/W---Messagerie--Portage-03-2024-?node-id=7575-103885

---

## 1. Introduction

Cette FS découle du PRD "Identification de l'établissement émetteur" et couvre l'intégralité de son périmètre : l'affichage de l'établissement de rattachement de l'émetteur d'un message, dans la liste des messages et dans la vue détail. Ce document sert d'artefact central pour le développement assisté par IA : les agents qui l'implémentent doivent s'y référer au pied de la lettre. Les décisions non tranchées sont marquées explicitement (`To be decided later`, `Don't know yet`, `Hypothesis to be validated`) et doivent être résolues avant développement.

---

## 2. Problématique

Un destinataire qui reçoit un message ne peut pas savoir à quel établissement est rattaché l'émetteur sans action supplémentaire, alors que ce repère est nécessaire pour comprendre le contexte de la demande. Le cas le plus fréquent est celui d'un agent institutionnel (collectivité, académie) sollicité par des interlocuteurs issus d'établissements très différents : sans ce repère, il ne peut pas prioriser ni traiter la demande efficacement. Le cas inverse existe aussi : un destinataire qui reçoit un message de cet agent institutionnel ne sait pas non plus de quel établissement il parle. Aujourd'hui, la seule façon de lever le doute est de cliquer sur le nom de l'émetteur puis d'ouvrir sa fiche annuaire, un détour jugé trop coûteux par les utilisateurs concernés. Cette friction se traduit par des sollicitations récurrentes auprès du support, en plus d'un volume de contournement non mesuré mais significatif.

---

## 3. Aperçu de la solution

L'établissement de l'émetteur est affiché sans action de la part du lecteur, à deux endroits : dans la liste des messages (pour permettre un repérage visuel rapide avant ouverture) et dans la vue détail du message (à côté du nom et prénom de l'émetteur). Est dans le périmètre de cette FS : l'affichage de cette information dans ces deux surfaces, ainsi que la règle de résolution de l'établissement à afficher lorsque l'émetteur est rattaché à plusieurs établissements. Est hors périmètre : toute reprise de cette information dans les notifications, ainsi que toute fonction de tri ou de filtre de la liste des messages par établissement (l'utilisateur trie visuellement lui-même, il n'y a pas de fonction dédiée dans cette FS).

### Principes de design

1. L'information doit être visible sans action de la part du lecteur, dans les deux surfaces du périmètre.
2. Le besoin ne concerne qu'une minorité d'utilisateurs (environ 5 %) : l'affichage ne doit pas alourdir l'interface pour les 95 % non concernés par un doute sur l'établissement de l'émetteur. Décidé en séance Design après itération sur les maquettes : affichage systématique (pas conditionnel), pour tous les messages, dans les deux surfaces, sous une forme visuellement discrète (libellé en gris, à côté du nom de l'émetteur).
3. Tout utilisateur de la plateforme est nécessairement rattaché à au moins un établissement : il n'existe pas de cas d'émetteur sans établissement à gérer dans cette FS.

---

## 4. Cas d'usage

*Owner : PO (cas d'usage + critères fonctionnels) - Owner : Design (scénarios de test Gherkin)*

### US-1 : Repérer l'établissement de l'émetteur dans la liste des messages

**En tant que** destinataire d'un message
**je veux** voir l'établissement de rattachement de l'émetteur directement sur chaque ligne de la liste des messages
**afin de** pouvoir repérer visuellement et prioriser les messages à ouvrir en premier, sans avoir à ouvrir la fiche annuaire de l'émetteur

**Critères d'acceptation :**

- L'établissement de l'émetteur est affiché sur chaque ligne de la liste des messages, pour tous les messages de la boîte de réception, sans action de la part du lecteur.
- Cet affichage permet un repérage visuel par l'utilisateur ; il n'y a pas de fonction de tri ou de filtre de la liste par établissement dans cette FS.
- Lorsque l'émetteur est rattaché à plusieurs établissements : si au moins un de ses établissements est commun avec un établissement du destinataire, c'est cet établissement commun qui est affiché ; si plusieurs établissements sont communs, celui qui est départagé est l'établissement préféré de l'émetteur parmi les communs ; si aucun établissement n'est commun, c'est l'établissement préféré de l'émetteur qui est affiché, c'est-à-dire celui qu'il a sélectionné dans la liste déroulante de sa page d'accueil. **Confirmé par la Tech (27/07/2026)** : règle validée au regard des données disponibles, complétée du fallback (premier établissement par ordre alphabétique du nom) si l'émetteur n'a pas de préféré défini.
- Le libellé de l'établissement est affiché de façon discrète (en gris), à côté du nom de l'émetteur, pour ne pas alourdir la lecture de la liste pour les utilisateurs non concernés par un doute sur l'établissement de l'émetteur.
- Le nom de l'établissement affiché dans la liste est cohérent avec celui affiché dans la vue détail pour un même message (même règle de résolution appliquée dans les deux surfaces).

**Scénarios de test :**

```gherkin
Scénario: Émetteur rattaché à un seul établissement
  Étant donné je consulte la liste des messages
  Et l'émetteur d'un message est rattaché à un seul établissement
  Quand la ligne du message s'affiche
  Alors le nom de cet établissement DOIT être affiché sur la ligne, sans action de ma part
```

```gherkin
Scénario: Émetteur multi-établissement avec un établissement commun avec le destinataire
  Étant donné je consulte la liste des messages
  Et l'émetteur d'un message est rattaché à plusieurs établissements
  Et un seul de ces établissements est commun avec l'un des miens
  Quand la ligne du message s'affiche
  Alors l'établissement commun DOIT être affiché sur la ligne
  Et l'établissement préféré de l'émetteur NE DOIT PAS être affiché s'il diffère de l'établissement commun
```

```gherkin
Scénario: Émetteur multi-établissement avec plusieurs établissements communs avec le destinataire
  Étant donné je consulte la liste des messages
  Et l'émetteur d'un message est rattaché à plusieurs établissements
  Et plusieurs de ces établissements sont communs avec les miens
  Quand la ligne du message s'affiche
  Alors l'établissement affiché DOIT être celui préféré de l'émetteur parmi les établissements communs
  Et le système NE DOIT PAS afficher indifféremment n'importe lequel des établissements communs
```

```gherkin
Scénario: Émetteur multi-établissement sans établissement commun avec le destinataire
  Étant donné je consulte la liste des messages
  Et l'émetteur d'un message est rattaché à plusieurs établissements
  Et aucun de ces établissements n'est commun avec les miens
  Quand la ligne du message s'affiche
  Alors l'établissement préféré de l'émetteur DOIT être affiché sur la ligne
```

```gherkin
Scénario: Absence de fonction de tri ou de filtre par établissement
  Étant donné je consulte la liste des messages
  Et plusieurs messages proviennent d'émetteurs rattachés à des établissements différents
  Quand je parcours la liste
  Alors je NE DOIS PAS disposer d'un contrôle de tri ou de filtre dédié à l'établissement
  Et le repérage DOIT rester un repérage visuel ligne par ligne
```

```gherkin
Scénario: Nom d'établissement trop long pour la largeur de la ligne
  Étant donné je consulte la liste des messages
  Et le nom de l'établissement de l'émetteur d'un message dépasse l'espace disponible sur la ligne
  Quand la ligne du message s'affiche
  Alors le nom DOIT être tronqué avec une ellipsis
  Et le nom complet DOIT être visible dans un tooltip au survol
  Et la ligne NE DOIT PAS s'agrandir en hauteur pour afficher le nom complet
```

### US-2 : Identifier l'établissement de l'émetteur dans la vue détail du message

**En tant que** destinataire d'un message
**je veux** voir le nom de l'établissement de rattachement de l'émetteur affiché à côté de son nom et prénom dans la vue détail du message
**afin de** comprendre immédiatement le contexte institutionnel du message, sans avoir à ouvrir la fiche annuaire de l'émetteur

**Critères d'acceptation :**

- Le nom de l'établissement de l'émetteur est affiché à côté de son nom et prénom dans la vue détail du message, pour tous les messages, sans action de la part du lecteur, sous la même forme discrète (en gris) que dans la liste.
- Lorsque l'émetteur est rattaché à plusieurs établissements, la même règle de résolution que dans US-1 s'applique, y compris le départage par établissement préféré en cas de plusieurs établissements communs. **Confirmé par la Tech (27/07/2026)** : même fonction de résolution partagée entre les deux surfaces, garantissant la cohérence exigée.
- Le traitement est identique quel que soit le profil du destinataire (agent institutionnel recevant un message d'un usager d'un autre établissement, ou usager recevant un message d'un agent institutionnel) : pas de logique d'affichage différenciée par profil.

**Scénarios de test :**

```gherkin
Scénario: Émetteur rattaché à un seul établissement
  Étant donné j'ouvre la vue détail d'un message
  Et l'émetteur est rattaché à un seul établissement
  Quand la vue détail s'affiche
  Alors le nom de cet établissement DOIT être affiché à côté du nom et prénom de l'émetteur
```

```gherkin
Scénario: Émetteur multi-établissement avec un établissement commun avec le destinataire
  Étant donné j'ouvre la vue détail d'un message
  Et l'émetteur est rattaché à plusieurs établissements dont un seul est commun avec les miens
  Quand la vue détail s'affiche
  Alors l'établissement commun DOIT être affiché à côté du nom et prénom de l'émetteur
```

```gherkin
Scénario: Émetteur multi-établissement avec plusieurs établissements communs avec le destinataire
  Étant donné j'ouvre la vue détail d'un message
  Et l'émetteur est rattaché à plusieurs établissements communs avec les miens
  Quand la vue détail s'affiche
  Alors l'établissement affiché DOIT être celui préféré de l'émetteur parmi les établissements communs
```

```gherkin
Scénario: Émetteur multi-établissement sans établissement commun avec le destinataire
  Étant donné j'ouvre la vue détail d'un message
  Et l'émetteur est rattaché à plusieurs établissements dont aucun n'est commun avec les miens
  Quand la vue détail s'affiche
  Alors l'établissement préféré de l'émetteur DOIT être affiché à côté de son nom et prénom
```

```gherkin
Scénario: Cohérence entre la liste et la vue détail pour un même message
  Étant donné un message dont l'émetteur est rattaché à plusieurs établissements
  Et l'établissement affiché sur la ligne de ce message dans la liste est déterminé
  Quand j'ouvre la vue détail de ce même message
  Alors l'établissement affiché dans la vue détail DOIT être identique à celui affiché dans la liste
  Et le système NE DOIT PAS appliquer une résolution différente entre les deux surfaces
```

```gherkin
Scénario: Nom d'établissement trop long dans la vue détail
  Étant donné j'ouvre la vue détail d'un message
  Et le nom de l'établissement de l'émetteur dépasse l'espace disponible à côté de son nom et prénom
  Quand la vue détail s'affiche
  Alors le nom DOIT passer à la ligne suivante
  Et le nom NE DOIT PAS être tronqué avec une ellipsis
```

```gherkin
Scénario: Le nom de l'établissement n'est pas interactif en vue détail
  Étant donné j'ouvre la vue détail d'un message
  Et le nom de l'établissement de l'émetteur est affiché à côté de son nom et prénom
  Quand je clique sur le nom de l'établissement
  Alors aucune action NE DOIT se déclencher
  Et le système NE DOIT PAS afficher un lien ou un curseur de type lien sur ce libellé
```

---

## 5. Brief design produit UX/UI

*Owner : Design*

L'établissement de l'émetteur est une métadonnée secondaire par rapport au contenu du message : elle ne doit pas concurrencer visuellement l'objet, l'expéditeur ou la date dans la liste, ni le nom et prénom de l'émetteur dans la vue détail. Les deux surfaces (liste et détail) doivent afficher l'établissement de façon visuellement cohérente entre elles, pour ne pas donner l'impression de deux traitements différents d'une même information. L'affichage n'est pas optionnel : il est systématique pour tous les messages, dans les deux surfaces.

Après itération sur les maquettes, la forme de l'affichage est tranchée : systématique (pas conditionnel), pour tous les messages, dans la liste comme dans la vue détail, sous un libellé discret en gris positionné à côté du nom de l'émetteur. Le libellé est un texte simple, non cliquable, dans les deux surfaces : aucune action n'est associée au nom de l'établissement affiché.

Gestion des noms d'établissement trop longs pour l'espace disponible :

- Dans la liste des messages : troncature avec ellipsis, le nom complet apparaît dans un tooltip au survol.
- Dans la vue détail : retour à la ligne, sans troncature.

### Décisions UX/UI ouvertes

Aucune décision UX/UI ouverte à ce stade : la forme de l'affichage, la gestion des noms longs et le caractère non cliquable ont été tranchés en séance Design.

---

## 6. Exigences techniques

*Owner : Tech lead*

- Le rattachement multi-établissement d'un utilisateur et la notion d'établissement préféré (sélectionné via le widget de la page d'accueil) sont déjà disponibles en base Neo4j, stockés dans les préférences utilisateur. Confirmé par la Tech, la donnée n'est donc pas à créer.
- L'établissement préféré n'est pas systématiquement défini : le widget de sélection côté front n'est pas toujours présent ou n'a pas toujours été renseigné par l'utilisateur (présent dans ~50 % des cas, confirmé par la Tech). La règle de résolution (US-1, US-2) doit donc prévoir un comportement de repli explicite pour ce cas. **Tranché en cadrage technique (27/07/2026)** : fallback = premier établissement par ordre alphabétique du nom. Compromis pragmatique assumé — il peut ne pas correspondre à « l'établissement attendu » par l'utilisateur en l'absence de préféré défini.
- Le destinataire d'un message n'a pas les droits d'accéder, via l'API existante, aux préférences d'un autre utilisateur (l'émetteur). Conséquence directe : la résolution de l'établissement à afficher (établissement commun, départage par préféré, fallback) ne peut pas être effectuée côté front et doit être réalisée côté back, qui a la légitimité d'accès aux préférences de l'émetteur. Le front ne fait que consommer une valeur déjà résolue.
- Repo principal concerné : `entcore/conversation` (frontend et une partie des API de messagerie). La logique de résolution s'appuie également sur d'autres modules `entcore/` côté back, notamment celui portant les préférences utilisateur et l'annuaire. **Tranché en cadrage technique (27/07/2026)** : le module `directory` expose une **nouvelle** action eventbus (l'existante `list-users` reste inchangée) qui retourne, par lot de `userId`, les structures d'appartenance et l'établissement préféré brut ; la **règle de résolution est portée par `conversation`**, qui est le seul à connaître le destinataire auquel comparer. Découpage documenté dans [CADRAGE-IMPULS-6108-structure-sender.md](./CADRAGE-IMPULS-6108-structure-sender.md) et contrat eventbus figé en section 2 de [CONTRAT-API-IMPULS-6113-US1.md](./CONTRAT-API-IMPULS-6113-US1.md).
- Point de vigilance performance (pas d'exigence chiffrée à ce stade) : la résolution doit éviter un appel N+1 vers le module préférences pour chaque émetteur affiché dans la liste des messages, en particulier en cas de pagination ou de scroll infini.
- Aucune dépendance connue avec un autre chantier ou FS en cours sur le même modèle de données (préférences / annuaire) au moment de la rédaction.

---

## 7. Contraintes

### Contraintes fonctionnelles

*Owner : PO*

- Hors périmètre : reprise de l'information d'établissement dans les notifications.
- Hors périmètre : toute fonction de tri ou de filtre de la liste des messages par établissement ; l'affichage sert uniquement au repérage visuel.
- Règle métier actée : un émetteur est nécessairement rattaché à au moins un établissement (aucun droit sur la plateforme sans établissement) ; le cas "émetteur sans établissement" n'existe pas et n'a pas à être géré.

### Contraintes techniques

*Owner : Tech lead*

- Aucune modification des API existantes de la messagerie ne doit casser leur contrat actuel (pas de suppression, renommage, ou changement de type d'une propriété existante), ces API étant consommées par d'autres clients (mobile, autres applications).
- L'établissement résolu doit être exposé soit par ajout de propriété(s) additive(s) sur les payloads existants (liste des messages, détail du message), soit via un nouvel endpoint dédié si l'ajout de propriété n'est pas adapté. **Tranché en cadrage technique (27/07/2026)** : enrichissement additif des payloads existants, pas de nouvel endpoint. Le champ ajouté est `from.displayStructure` (`{ id, name }`, optionnel) sur les deux endpoints. L'ajout étant purement additif, aucun risque de casse pour les clients existants, mobile compris.

### Questions ouvertes

Toutes les questions ouvertes à la rédaction ont été tranchées en cadrage technique (27/07/2026) et lors du figeage du contrat d'API (28/07/2026). Détail dans [CADRAGE-IMPULS-6108-structure-sender.md](./CADRAGE-IMPULS-6108-structure-sender.md) et [CONTRAT-API-IMPULS-6113-US1.md](./CONTRAT-API-IMPULS-6113-US1.md).

- **Règle de fallback en l'absence d'établissement préféré défini par l'émetteur** : **tranché** — premier établissement par ordre alphabétique du nom.
- **Module(s) back exact(s) portant la logique de résolution** : **tranché** — nouvelle action eventbus dans `directory` pour la donnée brute (structures d'appartenance + établissement préféré), règle de résolution portée par `conversation`.
- **Choix entre enrichissement des payloads existants et nouvel endpoint dédié** : **tranché** — enrichissement additif, champ `from.displayStructure` sur les deux endpoints existants.
- **Disponibilité des données de rattachement multi-établissement et d'établissement préféré** : confirmée par la Tech (stockage Neo4j, préférences utilisateur).
- **Formulation exacte de la règle de résolution multi-établissement** : **tranché** — commun avec le destinataire → si plusieurs communs, départage par préféré de l'émetteur, à défaut ordre alphabétique → si aucun commun, préféré de l'émetteur → sinon premier par ordre alphabétique.

Deux points nouveaux, apparus au cadrage et absents de la rédaction initiale de cette FS :

- **Dégradation silencieuse** : si la résolution échoue pour un émetteur (aucune structure retournée, préférence illisible, annuaire indisponible), le message s'affiche **sans** libellé d'établissement plutôt que de faire échouer la requête. Ce cas contredit l'hypothèse §3 « tout utilisateur est nécessairement rattaché à au moins un établissement » : celle-ci reste vraie fonctionnellement, mais le contrat d'API rend le champ optionnel par sûreté technique, et le front doit gérer son absence.
- **Résolution relative à l'appelant** : l'établissement affiché dépendant du destinataire (règle de l'établissement commun), deux destinataires d'un même message peuvent légitimement voir deux établissements différents pour le même émetteur. La cohérence exigée par les CA porte sur liste vs détail **pour un même lecteur**, pas entre lecteurs.

- **Nom exact de la nouvelle action eventbus `directory`** : **tranché** — `list-users-structures`, à l'implémentation d'IMPULS-6116.
