ONE : Notes de versions
=======================

# v1.5.1

## Améliorations mineures

* \#910 : [Portail] Ajouter une alerte sur le nombre de messages reçus
* \#919 : [Blog] Espacer l'affichage du titre et de l'auteur d'un blog (dans la vignette)
* \#800 : [Messagerie] Suppression du tooltip sur l'éditeur de message
* \#1026 : [Directory] Adapter le message de confirmation de création de compte
* \#1017 : API de collecte / stockage des préférences utilisateur indépendantes des application

## Anomalies corrigées
* \#727 : [Espace Dcoumentaire] Tri sur le champ Titre ne fonctionne pas avec les fichiers comportant des accents
* \#924 [Ressources externes] Accès des élèves sans niveau à Maxicours
* \#1036 : [Quoi de neuf] - Problème avec les notifications du cahier multimédia (QuikFix)
* \#1019 : [Messagerie] création de l'utilisateur après la création des rôles de la classe
* \#1020 : [Administration] Correction du modèles des applications configurées par défaut

# v1.5.0

## Administration de la classe

* [nouveau] Application de gestion des utilisateurs de classe par l'enseignant

## Annuaire

* \#907 Attribuer les roles par défaut après la création d'une classe
* \#898 Masquer utilisateurs bloqués dans la classe, les recherches, les partagess et l'envoie de message
* \#864, \#863 Api de création de d'école / classe
* \#899 Pouvoir envoyer un email à un utilisateur lors de sa création

## La classe

* \#776 Relooking de la vue "Ma classe"
* \#619 Recherche par auto complétion dans la classe

## Registre d'application

* \#902 Pouvoir attribuer des roles par défaut
* \#901 Créer des roles par défaut

## Messagerie

* \#815 Transférer un message depuis la boite d'envoi

## Évolutions Techniques
* \#865 Api idempotente de création et de suppression des règles de communication par défaut
* Renommage des espaces de nom pour supprimer toute référence à _"one"_  du code (au profit de _"ent-core"_)

# Authentification / Session

* \#559	[oAuth2] Pouvoir se servir du code une seule fois
* \#558	[oAuth2] Expirer le code au bout de 10 min
* \#867	[oAuth2] Supporter le flux "client_credentials"
* \#895	Bloquer la connexion pour un utilisateur
* \#904	Authentifier directement les utilisateurs après l'activation

# v1.4.0

## Messagerie

* \#792, \#751, \#750, \#748 : API et Test (Activation / Désactivation, Envoie, Réponse, Transfert, Brouillon, Corbeille, Intégration des règle de communication)
* IHM de consultation et de conversation

## Espace Documentaire

* \#738 Créer un nouveau document : Ne plus  afficher les fichiers de la fenêtre précédente
* \#737 Copier dans mes documents : sélection précédente conservée
* \#735 Sélectionner le dossier "Mes documents" par défaut lors de la copie d'un document
* \#734 Copier dans mes documents : ne pas autoriser la sélection multiple de dossiers

## Portail / Fil de Nouveautés / Widget

* \#827 [portail] Api donner la locale courant de l'utilisateur connecté
* \#668 [fenêtre de partage] Indiquer par défaut les droits de l'utilisateur
* \#786 [fil de nouveauté] Renvoyer les clés i18n des événements de la timeline pour les internationaliser
* [widget] Ajout du widget changement d'humeur
* [widget] Anniversaire : Affichage de tous les élèves de la classe (indépendamment des règles de communication) et de toutes les dates d'anniversaires (indépendamment des choix de visibilité d'information personnelle)

## Blog

* \#742 [anomalie] Empêcher et informer l'utilisateur de la création d'un billet sans contenu
* Ajout du workflow de modération pour la publication de billet
* Ajout de Filtre d'affichage des billets

## Administration

* \#785 [annuaire] Réinitialiser un mot de passe via l'explorateur d'annuaire
* \#20 [annuaire][anomalie] Affichage des parents dans l'explorateur d'annuaire

## Évolutions techniques

* \#787 [infra] Exécuter les scripts du schema neo4j au démarrage pour le mode embarqué
* \#746 [infra] Helpers pour neo4j version 2 avec les nouveaux formats de retour
* \#749 [registre d'application] Broadcaster un message lors de la modification des habilitations associées à un groupe de profil
* \#754 [registre d'application] Collecter la liste des utilisateurs ou des groupes qui ont accès à mon application

# v1.3.0-neo4j2

* Migration de la base données neo4j de la version 1.9.2 à 2.0.0

# v1.3.0

## Activation et connexion

* \#675 Ne pas afficher d'erreur lors de la tentative d'activation d'un compte déjà activé (e.g. double clic)

## Portail

* \#652 Police dans la liste des applications
* \#644 Taille des widgets
* \#601 [widget][Calendrier] Erreur quand on clique sur le nom d'une personne
* [widget] Ajout du widget Anniversaire

## Fil de nouveauté

* \#646 Style des notifications
* \#633 Ajouter un type par événement

## Mon Compte

* \#655 Changer ma photo : appliquer le clic à l'avatar
* \#625 Possibilité de supprimer la photo après un premier ajout
* \#57 Afficher d'infos-bulles sur sur les icônes de gestion des droits de visibilité

## Espace Documentaire

* \#624, \#673, \#672 :  api de gestion des dossiers (création , copie, copie depuis "documents partagés avec moi", déplacement, déplacement dans la corbeille)
* \#632 Copier un document partagé vers ses documents personnels
* \#654, \#637 Trier les documents par date (choix par défaut), par titre, par propriétaire
* \#647, \#631 Action en lot sur les documents : Chargement, partage copie et déplacement
* \#629 [ergonomie] Affichage des actions contextuelles à une ressource via une barre de boutons en bas de l'écran. (résout en même temps \#37)
* \#626 Ne pas faire apparaitre les boutons d'action en fonction des habilitations
* \#56 Faire apparaître le nom du document partagé lors de la publication sur le fil de nouveauté

# Blog

* \#645 Ajouter un bord gris à la liste des blogs
* \#627 [partage] le rôle "consulter" est conditionne l'activation du rôle "commenter"
* Gestion de la pagination

## Opération techniques

* \#676 [alimentation] Supprimer tous les envois à wordpress

# v1.2.1

## Widgets

* Calendrier : Correction de la boucle infinie qui se produisait au mois de décembre.

# v1.2.0

## Portail

* [thème] intégration des thèmes Panda déclinés
* [thème] Personnalisation du thème par utilisateur (avec un volet de configuration)
* [widget] Infrastructure de widget statique côté client
* [widget] Date courante
* [widget] Anniversaires (+ \#527 Api permettant de renvoyer tous les utilisateurs avec une date de naissance comprise dans les trois mois encadrant la date courante)

## Fil de nouveautés

* \#555 : Pour mettre en évidence les nouveautés jamais consultées
* \#554 : Pour filter les nouveautés par applications sources (Blog, Espace Documentare, Mon Compte)
* \#524 : Pour afficher de l'avatar de l'utilisateur à l'origine de la nouveauté

## Droit et habilitations

* \#540, \#529, \#530, \#531, \#532, \#533, \#534, \#535, \#536 : API REST de gestion des gestions des partages indépendantes des IHM 
 - Matrice d'autorisation
 - Utilisateur visible (via règle de communication)
 - Ajout/Suppression d'un droit ...
* IHM de présentation synthétique de la matrice d'autorisations et recherche d'utilisateur et de groupe visible par auto-complétion

## Espace Documentaire

* \#562, \#563 Gestion du format des miniatures (création et affichage)
* \#62 Mise en évidance des documents partagés
* [fix] \#16 Copier un document dans le dossier "racine" de l'arborescence

## Mon compte et Annuaire

* [fix] \#567 : Changement de mot de passe cassé par le portage Angular.js 
* [fix] \#556 : Lors de la saisie de la devise  des requêtes sont envoyées avant la fin de la saisie.
* \#522 : Api pour récupérer l'avatar de l'usager à partir de son id
* [fix] \#355 Mettre à jour l'avatar plutôt que de créer un nouveau document "workspace"
* \#436 : Permettre à l'utilisateur de gérer la visibilité de son adresse postale, email et numéro de téléphone dans l'annuaire
* [fix] \#56 : La saisie des centres d'intérêts envoie une requête par lettre saisie

# Opérations Techniques

* [infra] \#184 : Regroupement des fonctionnalités communes au application ent-core dans un module
* [infra] \#179 : Ecriture d'module vertx de redimensionnement d'image basé sur _org.imgscalr_
* [test] Test d'intégration et performance. Ecriture de simulations Gatlin pour  : AppRegistry, Auth, Blog, Directory, Import

# v1.1.0

## Blog

* portage en application ent-core

## Droits et habilitation

* structuration de l'écran de partage en groupes et utilisateurs

## Opérations techniques

* [UX] passage à Angular.js
* [Test] intégration de Gatlin pour les tests intégration et de performance
* [infra] \#357: Fusionner les application Directory et UserBook
* [infra] \#161: Intégration du module vertx proxy HTTP pour simplifier les environements de développement
* [sécurité] \#180: Création du type Authentification pour l'annotation SecuredAction pour filter l'accès simplement pour les utilisateurs authentifiés
* [infra] \#211: Sortir les librairies utilitaires pour vertx dans un dépôt séparé
* [dictionnaire des données] \#344:  pourvoir charger des fichiers ressource depuis le classpath

# v1.0.0

## Console d’administration

### Annuaire

* alimentation BE1D (élèves, parents, enseignants et directeurs)
 - par fichier CSV / BE1D
 - création des groupes par défaut 
 - configuration des règles de communication par défaut
* consultation (parcours école / classe / personnes)
* consultation des informations d'activation d'un usager
* extraction des données d’activation de compte (par école et par classe)

### Registres des applications

* enregistrement des applications internes au démarrage (nom et habilitations)
* création de rôles (= paquet d'habilitations inter-applications)
* affection / suppression de rôles à un groupe d'usager
* administration des informations des applications internes ("secret", "adresse", "icône", "target")
* administration des informations des applications externes ("nom", "secret", "adresse", "icône", "target")

### Règles de communication

* gestion des groupes au niveau école et au niveau classe
 - ENSEIGNANT, ELEVE, PERSRELELEVE, DIRECTEUR (seulement école)
* modification des règles de communication pour les groupes
* modification des règles de communication spécifiques des parents de l
* API (BUSMODE) de collecte pour l'usager connecté
 - des usagers qu'il peut voir
 - des usagers qui le voit
* [non résolu] consultation de la matrice de règles de communication configurée par défaut

## L’ENT

### Activation & Connexion

* activation du compte (information de connexion) usager
* authentification par login / mot de passe
* la durée d’une session d’utilisation est de 30 minutes (elle est paramétrable)
* procédure de mot de passe oublié (saisi du login et envoie d'un email avec un lien de reset à l'usager ou à son enseignant)
* procédure de changement de mot de passe (pour un utilisateur connecté)
* gestion automatique des adresses de redirection après connexion (callback)
* déconnexion avec redirection vers la page d'accueil de l'ENT
* gestion des sessions utilisateurs (et la distribution des cartes d'identité utilisateur aux applications clientes)
* serveur OAuth2 minimaliste (authentification et "autorisation code")

### Portail

* affichage du  prénom de l’usager connecté dans l’en-tête
* affichage de l’avatar de l’usager connecté dans l’en-tête
* page par défaut : "Quoi de neuf", "La classe", "Mes applis"
* gestion d'un thème graphique complet, indépendant et remplaçable
* communication (par post message) portail / iframe (application)
 - injection style
 - unification de l'historique de navigation
 - unification de la gestion des notifications générales d'utilisation (confirmation, message, alerte, erreur)
 - redimensionnement de l'iframe
 - positionnement de la lightbox

### Quoi de neuf

* affichage d'un fil de nouveautés issues de l'activité des personnes visibles par l'usager connecté
* nouveautés collectées
 - "Mon compte" : changement d'humeur, changement de devise
 - "Espace documentaire" : partage de document, réception de document dans le casier

### Mon compte

* Consultation des informations administratives (nom, prénom, identifiant, téléphone, adresse, date de naissance, école, courriel)
* Modification courriel
* Changement de mot de passe
* Administration des informations de socialisations (photo, humeur, devise, centre d'intérêts, santé et alimentation)
* gestion de la visibilité des centres d'intérêt

### Ma classe

* Affichage des personnes (enseignant, élèves et parents) de la classe sous forme de vignette (prénom, nom, avatar, humeur)
* Consultation de la fiche d'une personne
* Vue liste et vue vignette

### Annuaire

* recherche de toutes les personnes à partir du nom d’affichage dans la limite des règles de communications de l'usager connecté
* affichage des personnes trouvées sous forme de vignettes en conservant le contexte de recherche

### Espace documentaire

* stockage de documents (de tout type) en ligne
* explorateur de documents (vue liste / vue miniature )
* dossiers présents par défaut : "Mes documents", "Documents partagés", "Mon casier", "Corbeille"
* chargement de documents dans "Mes documents"
* déplacement, copie, suppression
* partage de document à une personne visible (règles de communication)
* dépôt de document dans le casier d'une personne visible
* commentaire sur un document
