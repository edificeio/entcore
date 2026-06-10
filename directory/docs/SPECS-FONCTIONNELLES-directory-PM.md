# Annuaire & gestion des utilisateurs — Dossier de validation fonctionnelle

**Module** : Directory (Annuaire / Mon compte / Paramétrage de la classe)
**Date** : 10 juin 2026
**Objet** : relecture et validation des cas d'usage par le PM, en préparation de la migration React.

> Ce document est une synthèse fonctionnelle des spécifications extraites du code existant. Chaque comportement décrit ici est **le comportement actuel en production**, pas une proposition. La relecture vise à confirmer : « oui, c'est bien le comportement attendu, à conserver » ou « non, c'est un bug / à changer lors de la migration ».
>
> **Comment relire** : les cas d'usage sont numérotés **UC-xx**, les règles de gestion **RG-xx**. Les points nécessitant un arbitrage explicite sont signalés par ⚠️. Une checklist de validation se trouve en fin de document.

---

## 1. Vue d'ensemble

Le module couvre quatre espaces utilisateur :

| Espace | Qui y accède | À quoi ça sert |
|--------|--------------|----------------|
| **Annuaire** | Tous les utilisateurs | Rechercher des personnes et des groupes, consulter les fiches, gérer ses favoris de partage |
| **Mon compte** | Tous les utilisateurs | Consulter et modifier ses informations personnelles, sa photo, son humeur/devise, son mot de passe, ses préférences |
| **Paramétrage de la classe** | Enseignants (et directeurs d'école) | Gérer les comptes des élèves, parents et collègues de sa classe : création, activation, blocage, mots de passe, publipostage |
| **Widget anniversaires** | Tous (page d'accueil) | Voir les anniversaires du mois dans ses classes |

S'y ajoutent des fonctions transverses : la **fusion de comptes** (un utilisateur ayant deux comptes peut les réunir) et la **mise en relation** entre établissements (onglet « découvrir » de l'annuaire).

### Profils et rôles (vocabulaire)

- **Profils** : Élève, Parent, Enseignant, Personnel, Invité.
- **ADML** : administrateur local d'un ou plusieurs établissements.
- **Super-admin** : administrateur central de la plateforme.
- **Professeur principal**, **Direction** : fonctions attribuables à un utilisateur sur un établissement.
- Beaucoup d'écrans et de boutons sont conditionnés par des **droits configurables** par établissement (ex. : droit d'utiliser les favoris, droit d'importer un CSV en classe, droit de changer de thème). La liste figure en annexe A.

---

## 2. Annuaire — recherche

### UC-01 — Rechercher des personnes ou des groupes
**Acteur** : tout utilisateur connecté.
**Déroulé** :
1. L'utilisateur arrive sur l'annuaire (par défaut sur la vue « Ma classe », cf. UC-05) et ouvre la recherche.
2. Il choisit l'onglet **Utilisateurs**, **Groupes** ou **Favoris**, saisit éventuellement un texte et coche des filtres : établissement, classe, profil, fonction, poste.
3. Les résultats s'affichent en vignettes (« dominos »), triés par nom. Pour les groupes : nom + nombre de membres.

**Règles de gestion** :
- **RG-01** — La recherche ne montre que les personnes que l'utilisateur **a le droit de voir** (règles de communication de la plateforme). Un élève voit moins de monde qu'un enseignant.
- **RG-02** — Les résultats s'affichent par tranches de 50 ; un bouton « voir plus » charge la suite. La liste d'établissements s'affiche par tranches de 7.
- **RG-03** — Certains filtres se désactivent entre eux pour éviter les combinaisons sans résultat :
  - les profils Élève / Parent / Invité sont indisponibles si une fonction (ex. prof principal) est cochée ;
  - la fonction « professeur principal » n'est proposée que si le profil Enseignant est coché (ou aucun profil) ;
  - le filtre « poste » n'est proposé que pour Enseignant ou Personnel.
- **RG-04** — Si l'utilisateur appartient à **plusieurs établissements**, le filtre « classes » est verrouillé tant qu'aucun établissement n'est coché (message explicatif). Les classes proposées sont alors celles des établissements cochés.
- **RG-05** — Une recherche peut être pré-remplie par lien (URL avec filtres) : on peut donc partager un lien « tous les enseignants de l'établissement X ». Sur mobile, le lien ouvre directement les résultats.
- **RG-06** — Sur mobile uniquement, une recherche sans résultat affiche une notification « aucun résultat ».
- **RG-07** — Un champ de filtre local permet d'affiner les résultats affichés par nom, sans relancer la recherche.

### UC-02 — Consulter une fiche utilisateur
**Acteur** : tout utilisateur connecté.
**Déroulé** : depuis les résultats ou un lien direct, l'utilisateur ouvre la fiche d'une personne : photo, nom, profil, établissement(s), humeur/devise, centres d'intérêt, coordonnées (selon visibilité), enfants ou parents le cas échéant. Il peut naviguer de fiche en fiche (ex. parent → enfant) avec un bouton retour.

**Règles de gestion** :
- **RG-08** — Les informations personnelles (email, téléphone, date de naissance, santé, mobile) ne s'affichent que si leur propriétaire les a rendues **publiques** (cf. UC-10). ⚠️ Ce masquage est actuellement appliqué côté navigateur en correctif temporaire ; le serveur peut encore envoyer ces données. À traiter côté serveur lors de la migration.
- **RG-09** — La section « parents » d'une fiche élève n'est visible que par les enseignants et personnels. La section « enfants » d'une fiche parent est visible si la fiche en comporte.
- **RG-10** — Sur la vignette d'un enseignant : sa première matière enseignée ; pour un poste fonctionnel : le premier poste avec « … » si plusieurs (liste complète en infobulle).
- **RG-11** — Les boutons « écrire un message » n'apparaissent que si l'utilisateur dispose de la messagerie correspondante (messagerie interne, Zimbra ou fournisseur externe selon la configuration).
- **RG-12** — Les établissements affichés sur la fiche sont ordonnés : établissement principal d'abord, puis ceux où la personne est ADML, puis les autres.

### UC-03 — Consulter une fiche groupe
**Acteur** : tout utilisateur connecté.
**Déroulé** : ouverture d'un groupe → nom du groupe + liste des membres visibles, avec les mêmes vignettes que la recherche. Boutons d'action collective (écrire au groupe, ajouter aux favoris).

---

## 3. Annuaire — favoris de partage

Les « favoris » sont des listes personnelles mêlant personnes et groupes, réutilisables ensuite dans les écrans de partage des applications (ex. partager un document à « mes collègues de SVT »).

### UC-04 — Créer, modifier, supprimer un favori
**Acteur** : tout utilisateur disposant du droit « favoris ».
**Déroulé** :
1. Onglet Favoris → « créer » : l'utilisateur nomme la liste, recherche des personnes et des groupes (résultats mélangés, groupes en tête) et les ajoute.
2. Depuis une fiche personne ou groupe, un bouton « ajouter au favori » permet d'enrichir une liste existante ou d'en créer une à la volée.
3. La suppression demande confirmation.

**Règles de gestion** :
- **RG-13** — Les favoris sont strictement **personnels** : nul autre ne les voit (exception : un administrateur peut les consulter pour support).
- **RG-14** — Pas de doublon : ajouter une personne déjà présente dans la liste est sans effet. ⚠️ La notification de succès s'affiche quand même — à confirmer si c'est acceptable.
- **RG-15** — Un favori devenu **vide** (tous ses membres ont quitté la plateforme) est supprimé automatiquement à sa prochaine consultation.
- **RG-16** — Les favoris sont triés par ordre alphabétique ; sur grand écran, le premier favori est présélectionné à l'arrivée.

---

## 4. Annuaire — Ma classe

### UC-05 — Voir les membres de ma classe
**Acteur** : tout utilisateur rattaché à une ou plusieurs classes. C'est la **vue d'accueil** de l'annuaire.
**Déroulé** :
- aucune classe → message dédié « pas de classe » ;
- une seule classe → ses membres s'affichent directement ;
- plusieurs classes → liste des classes avec recherche, puis affichage des membres de la classe choisie.

**Règle de gestion** :
- **RG-17** — La recherche de personnes dans la classe tolère les accents et l'ordre des mots (« dupont marie » trouve « Marie DUPONT »).
- ⚠️ **Anomalie détectée** : si la liste des identifiants de classes et celle des noms de classes sont désynchronisées côté session, l'affichage peut associer des noms de classes erronés. Garde-fou présent mais inopérant dans le code actuel. À corriger à la migration.

---

## 5. Annuaire — mise en relation entre établissements (« découvrir »)

Fonctionnalité optionnelle permettant à des enseignants/personnels de **se rendre visibles et de se mettre en relation** avec des homologues d'autres établissements (au-delà des règles de communication standard).

### UC-06 — Rechercher et se mettre en relation
**Acteur** : enseignant ou personnel, si la fonctionnalité est activée pour son profil.
**Déroulé** :
1. Onglet « découvrir » de l'annuaire → recherche par établissement et/ou nom.
2. Sur chaque résultat, un bouton « se mettre en relation » / « rompre la relation ». La relation est **réciproque** : chacun peut ensuite écrire à l'autre.

**Règles de gestion** :
- **RG-18** — L'onglet n'apparaît que si la plateforme l'autorise pour le profil de l'utilisateur (Enseignant et/ou Personnel).
- **RG-19** — Une recherche exige au moins un critère (établissement coché ou texte saisi) ; sinon message d'information.

### UC-07 — Gérer un groupe de mise en relation
**Acteur** : même population que UC-06.
**Déroulé** : création d'un groupe nommé, ajout/retrait de membres parmi les personnes trouvées, renommage, consultation des membres, possibilité de **quitter** un groupe dont on est membre.
**Règle** :
- **RG-20** — Ces actions sont comptabilisées à des fins de statistiques d'usage (création de groupe, mise en relation, rupture, sortie de groupe).

---

## 6. Mon compte

### UC-08 — Consulter et modifier mes informations
**Acteur** : tout utilisateur connecté.
**Déroulé** : l'écran présente l'identité, les coordonnées, la photo, l'humeur, la devise, les centres d'intérêt, les établissements, et (selon le profil) ses enfants ou parents. Les champs modifiables s'éditent en place.

**Règles de gestion** :
- **RG-21** — Un **élève ne peut pas modifier** ses informations administratives (nom, adresse, email…) : il les consulte seulement. Les autres profils le peuvent.
- **RG-22** — La **devise** est limitée à 75 caractères, les informations « santé » à 1 000, chaque centre d'intérêt à 80. L'**humeur** se choisit dans une liste configurée par la plateforme.
- **RG-23** — Le numéro de mobile est contrôlé (format téléphone international) et enregistré dans un format normalisé.
- **RG-24** — En cas de changement d'email ou de mobile, un **message d'alerte est envoyé à l'ancienne adresse/numéro** (protection contre le détournement de compte).
- **RG-25** — L'email et le mobile peuvent être **vérifiés** (envoi d'un code) ; un picto indique s'ils sont validés.
- **RG-26** — L'**alias de connexion** (identifiant personnalisé) est modifiable si le droit est ouvert ; en cas d'alias déjà pris, message d'erreur dédié.
- **RG-27** — Supprimer sa photo l'enlève immédiatement (pas de bouton enregistrer séparé).

### UC-09 — Changer mon mot de passe / OTP
**Acteur** : tout utilisateur, pour son propre compte uniquement.
**Règles de gestion** :
- **RG-28** — Une jauge indique la solidité du mot de passe : « faible / moyen / fort » (longueur + mélange chiffres-lettres + caractères spéciaux).
- **RG-29** — Pour les comptes **fédérés** (connexion via un fournisseur d'identité externe), le bouton « mot de passe » n'apparaît que si pertinent ; un bouton **OTP** (code à usage unique à 8 chiffres) est proposé aux comptes fédérés équipés de l'application mobile.
- **RG-30** — La double authentification (MFA) peut être exigée ou non selon la configuration de la plateforme et la situation de l'utilisateur.

### UC-10 — Gérer la visibilité de mes informations
**Acteur** : tout utilisateur.
**Déroulé** : chaque information sensible (email, téléphone, date de naissance, santé, mobile) et chaque centre d'intérêt porte un picto œil : **public** (visible dans l'annuaire) ou **privé**. Le réglage est immédiat.

### UC-11 — Changer de thème graphique
**Acteur** : tout utilisateur disposant du droit « changer de thème ».
**Règle** :
- **RG-31** — Si la plateforme regroupe ses thèmes par famille, seuls les thèmes de la famille du thème courant sont proposés. Le choix est mémorisé.

### UC-12 — Fusionner deux comptes
**Acteur** : utilisateur possédant deux comptes (cas typique : un compte dans deux établissements).
**Déroulé** :
1. Depuis le compte A : « générer une clé de fusion » (suite de caractères à recopier).
2. Depuis le compte B : saisie de la clé → les comptes sont fusionnés, les identifiants regroupés.

**Règles de gestion** :
- **RG-32** — La clé doit respecter le format attendu, sinon message d'erreur immédiat (sans appel au serveur).
- **RG-33** — La génération et l'usage de la clé sont des **droits configurables** ; un administrateur peut aussi générer une clé pour un utilisateur, et seul le super-admin peut « défusionner ».

---

## 7. Paramétrage de la classe (enseignants)

Écran de gestion quotidienne des comptes d'une classe, accessible aux enseignants habilités. Quatre onglets par profil : Élèves, Parents, Enseignants, Personnels.

### UC-13 — Choisir sa classe
**Acteur** : enseignant.
**Règles de gestion** :
- **RG-34** — Si l'enseignant n'est rattaché à **aucune classe** alors qu'il appartient à un établissement, une fenêtre lui propose de choisir son école puis de **s'auto-rattacher** à une ou plusieurs classes. L'écran se recharge ensuite intégralement (ses droits de communication ont changé).
- **RG-35** — La dernière classe consultée est mémorisée et présélectionnée à la prochaine visite.
- **RG-36** — Le nom de la classe est modifiable directement dans l'en-tête.

### UC-14 — Créer un utilisateur dans la classe
**Acteur** : enseignant habilité (droit « ajouter des utilisateurs »).
**Déroulé** : formulaire avec profil, nom, prénom, et selon le profil : date de naissance, email, mobile, enfants à rattacher.

**Règles de gestion** :
- **RG-37** — Champs obligatoires : nom + prénom pour tous ; **date de naissance en plus pour un élève**. La date doit être comprise entre il y a 100 ans et aujourd'hui.
- **RG-38** — Le champ mobile n'est proposé que pour un **parent**. Changer de profil en cours de saisie **réinitialise le formulaire**.
- **RG-39** — Pour un **parent**, il faut rattacher au moins un enfant ; sinon une alerte s'affiche (la création reste possible en confirmant une seconde fois).
- **RG-40** — **Détection de doublons** (élèves et parents) : avant création, le système cherche un homonyme dans l'établissement. S'il en trouve, il les liste avec leurs classes et propose trois issues :
  1. **rattacher** le compte existant à la classe (il garde ses autres classes) ;
  2. **déplacer** le compte existant dans la classe (il quitte les anciennes, ses parents suivent) ;
  3. **créer quand même** un nouveau compte.
- **RG-41** — La recherche d'enfants à rattacher ne propose que des élèves de l'établissement.
- **RG-42** — Après création : notification de succès, et au choix « créer un autre » (formulaire vierge) ou retour à la liste.

### UC-15 — Gérer l'activation et les mots de passe
**Acteur** : enseignant habilité.
**Contexte** : chaque compte affiche son état dans la liste : **bloqué**, **non activé** (avec son code d'activation), **code de renouvellement généré** (avec le code), ou **activé**. La colonne est triable par état.

**Règles de gestion** :
- **RG-43** — « Renvoyer un mot de passe » envoie le mail de réinitialisation… **à l'adresse email de l'enseignant** (et non de l'utilisateur), pour qu'il le transmette. L'enseignant doit donc avoir un email renseigné, sinon erreur. ⚠️ Comportement volontaire (écoles primaires) mais contre-intuitif : à confirmer.
- **RG-44** — « Générer des codes temporaires » ne concerne que les comptes **déjà activés** (les non-activés ont déjà leur code d'activation). Les codes générés s'affichent et sont imprimables.
- **RG-45** — Un export CSV des codes d'activation est disponible par onglet de profil.

### UC-16 — Actions de masse sur une sélection
**Acteur** : enseignant habilité (droits bloquer / supprimer / retirer selon configuration).
**Déroulé** : cases à cocher (et « tout sélectionner » par onglet) → barre d'actions.

**Règles de gestion** :
- **RG-46** — **Bloquer / débloquer** : proposé seulement si la sélection est homogène (tous bloqués ou tous non bloqués).
- **RG-47** — **Supprimer** : proposé uniquement si **tous** les comptes sélectionnés ont été créés manuellement (à la main, par import CSV ou équivalent). Les comptes issus de l'annuaire académique **ne sont pas supprimables** ici.
- **RG-48** — **Retirer de la classe** : détache les comptes de la classe sans les supprimer ; les **parents suivent leurs enfants** automatiquement.
- **RG-49** — Chaque action de masse demande confirmation et affiche le nombre de comptes concernés et le nom de la classe.

### UC-17 — Modifier la fiche d'un utilisateur de la classe
**Acteur** : enseignant habilité.
**Déroulé** : clic sur une ligne → fiche détaillée avec navigation précédent/suivant. Chaque champ (nom d'affichage, email, téléphones, identifiant de connexion, code TOTP) s'édite et s'enregistre individuellement.

**Règles de gestion** :
- **RG-50** — L'identifiant personnalisé n'accepte que minuscules, chiffres, points et tirets ; le vider restaure l'identifiant d'origine.
- **RG-51** — Un **ADML qui modifie son propre email ou mobile** depuis cet écran est redirigé vers « Mon compte » (parcours sécurisé avec vérification).
- **RG-52** — Certains comptes ont un **email verrouillé** : seuls le super-admin (ou l'intéressé) peuvent le modifier.
- **RG-53** — La partie « humeur / devise / photo » d'une fiche n'apparaît que si le compte est **activé**.
- **RG-54** — Depuis la fiche d'un parent, on peut rattacher/détacher des enfants ; depuis la fiche d'un élève, créer directement « un parent pour cet élève ».
- **RG-55** — Un changement de classe d'un élève emmène ses parents (option activée par défaut).

### UC-18 — Importer des comptes par fichier (CSV)
**Acteur** : enseignant habilité (droit « import CSV »).
**Règles de gestion** :
- **RG-56** — L'import se fait par profil (un fichier d'élèves, de parents…). En cas d'erreur, le message indique la **ligne en cause** ou signale les comptes déjà existants.
- **RG-57** — L'option d'import ONDE (export de l'outil de gestion des écoles) n'est proposée que sur plateforme en français.

### UC-19 — Publipostage et exports
**Acteur** : enseignant habilité.
**Déroulé** : depuis la sélection ou pour des profils entiers, l'enseignant génère : **PDF détaillé** (une fiche de connexion par personne), **PDF simplifié**, **envoi par email**, ou **CSV**. Une fiche famille (élève + parents) est disponible depuis la fiche d'un élève.

**Règles de gestion** :
- **RG-58** — L'envoi par email liste à part les utilisateurs **sans adresse email** et propose de leur imprimer un PDF ; confirmation avant envoi.
- **RG-59** — Les documents reprennent le thème graphique de la plateforme ; les fichiers sont nommés avec la date.
- **RG-60** — Une sélection vide affiche un message et ne génère rien.

---

## 8. Widget anniversaires

### UC-20 — Voir les anniversaires du mois
**Acteur** : tout utilisateur (page d'accueil).
**Règles de gestion** :
- **RG-61** — Le widget liste les anniversaires **du mois en cours** dans la classe choisie, triés par jour.
- **RG-62** — La classe choisie est mémorisée ; si elle n'existe plus, repli sur la première classe disponible.
- **RG-63** — Seuls les utilisateurs ayant rendu leur date de naissance **publique** apparaissent.

---

## 9. Qui peut faire quoi (synthèse)

| Action | Élève | Parent | Enseignant | Personnel | ADML | Super-admin |
|--------|:-----:|:------:|:----------:|:---------:|:----:|:-----------:|
| Rechercher dans l'annuaire (périmètre selon règles de communication) | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| Gérer ses favoris (si droit ouvert) | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| Modifier ses infos administratives (Mon compte) | ✘ (lecture) | ✔ | ✔ | ✔ | ✔ | ✔ |
| Modifier humeur / devise / photo / visibilité | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| Mise en relation inter-établissements (si activée) | ✘ | ✘ | ✔ | ✔ | ✔ | ✔ |
| Paramétrage de la classe (si droits ouverts) | ✘ | ✘ | ✔ | ✘* | ✔ | ✔ |
| Modifier nom/prénom d'un utilisateur | ✘ | ✘ | ✔ (si admin de classe) | ✘ | ✔ | ✔ |
| Attribuer prof principal / direction | ✘ | ✘ | ✘ | ✘ | ✔ | ✔ |
| Gérer les doublons, postes, groupes manuels, structures | ✘ | ✘ | ✘ | ✘ | ✔ (son périmètre) | ✔ |
| Changer l'identifiant **canonique** d'un compte, défusionner | ✘ | ✘ | ✘ | ✘ | ✘ | ✔ |

\* sauf droits spécifiques ouverts par la plateforme.

Les opérations sensibles d'administration (modification d'un compte, console d'administration, imports, structures) exigent en plus une **double authentification (MFA)** lorsque celle-ci est activée sur la plateforme.

---

## 10. Points à arbitrer (⚠️ récapitulatif)

| # | Sujet | Question posée au PM |
|---|-------|----------------------|
| A1 | RG-08 | Le masquage des données privées sur les fiches est fait côté navigateur (correctif temporaire). Confirmer qu'il doit être porté côté serveur à la migration. |
| A2 | RG-14 | Ajout d'un doublon dans un favori : rien ne se passe mais une notification de succès s'affiche. Conserver ou corriger ? |
| A3 | UC-05 | Anomalie potentielle d'association id/nom de classes dans « Ma classe ». À corriger à la migration (pas de décision produit, simple validation). |
| A4 | RG-43 | Le mail de réinitialisation part vers l'email de **l'enseignant**. Confirmer que ce comportement (pensé pour le 1er degré) doit être conservé. |
| A5 | Photos de profil | Les photos de profil sont aujourd'hui accessibles **sans être connecté** (URL publique). Confirmer que c'est voulu. |
| A6 | Grilles horaires | Deux écrans de consultation des grilles horaires ne semblent soumis à aucun contrôle d'accès explicite. À vérifier côté sécurité. |
| A7 | Assistant d'import (admin) | L'assistant d'import ne vérifie pas que celui qui reprend un import en cours en est l'auteur (noté « à faire » dans le code). À prioriser ou non. |
| A8 | Périmètre non couvert | Les règles fines de « qui voit qui » (annuaire) et de la mise en relation vivent dans un autre module (communication) ; la console d'administration complète et l'import académique sont hors de ce dossier. |

---

## Annexe A — Droits configurables consommés par le module

Côté plateforme, les fonctionnalités suivantes s'activent/désactivent par profil ou par établissement :

- Affichage humeur & devise ; changement de thème.
- Favoris de partage.
- Modification de l'alias de connexion.
- Fusion de comptes : générer une clé / utiliser une clé.
- Paramétrage de classe : ajouter des utilisateurs, réinitialiser des mots de passe, bloquer, supprimer, retirer de la classe, importer un CSV.
- Mise en relation inter-établissements : profils autorisés.
- Création d'établissements, de classes, de groupes ; exports ; transition d'année ; gestion des doublons (réservés aux administrateurs).

## Annexe B — Documents techniques de référence

Pour les équipes de développement, le détail technique (endpoints, schémas de données, filtres de sécurité, références au code source) est disponible dans `directory/docs/specs/` :
`BUSINESS_RULES.md`, `DATA_MODEL.md`, `WORKFLOWS.md`, `PERMISSIONS.md`, `API_CONTRACTS.md`.

---

## Checklist de validation PM

- [ ] §2 Annuaire — recherche (UC-01 à UC-03, RG-01 à RG-12)
- [ ] §3 Favoris (UC-04, RG-13 à RG-16)
- [ ] §4 Ma classe (UC-05, RG-17)
- [ ] §5 Mise en relation (UC-06, UC-07, RG-18 à RG-20)
- [ ] §6 Mon compte (UC-08 à UC-12, RG-21 à RG-33)
- [ ] §7 Paramétrage de la classe (UC-13 à UC-19, RG-34 à RG-60)
- [ ] §8 Widget anniversaires (UC-20, RG-61 à RG-63)
- [ ] §9 Tableau des rôles
- [ ] §10 Arbitrages A1 à A8
