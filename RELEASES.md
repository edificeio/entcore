ONE : Notes de versions
=======================

# v1.2.0

## Portail

* [thème] intégration des thèmes Panda déclinés
* [thème] Personnalisation du thème par utilisateur (avec un volet de configuration)
* [widget] Infrastructure de widget statique côté client
* [widget] Date courante
* [widget] Anniversaires (+ #527 Api permettant de renvoyer tous les utilisateurs avec une date de naissance comprise dans les trois mois encadrant la date courante)

## Fil de nouveautés

* #555 : Pour mettre en évidence les nouveautés jamais consultées
* #554 : Pour filter les nouveautés par applications sources (Blog, Espace Documentare, Mon Compte)
* #524 : Pour afficher de l'avatar de l'utilisateur à l'origine de la nouveauté

## Droit et habilitations

* #540, #529, #530, #531, #532, #533, #534, #535, #536 : API REST de gestion des gestions des partages indépendantes des IHM 
 - Matrice d'autorisation
 - Utilisateur visible (via règle de communication)
 - Ajout/Suppression d'un droit ...
* IHM de présentation synthétique de la matrice d'autorisations et recherche d'utilisateur et de groupe visible par auto-complétion

## Espace Documentaire

* #562, #563 Gestion du format des miniatures (création et affichage)
* #62 Mise en évidance des documents partagés
* [fix] #16 Copier un document dans le dossier "racine" de l'arborescence

## Mon compte et Annuaire

* [fix] #567 : Changement de mot de passe cassé par le portage Angular.js 
* [fix] #556 : Lors de la saisie de la devise  des requêtes sont envoyées avant la fin de la saisie.
* #522 : Api pour récupérer l'avatar de l'usager à partir de son id
* [fix] #355 Mettre à jour l'avatar plutôt que de créer un nouveau document "workspace"
* #436 : Permettre à l'utilisateur de gérer la visibilité de son adresse postale, email et numéro de téléphone dans l'annuaire
* [fix] #56 : La saisie des centres d'intérêts envoie une requête par lettre saisie

# Opérations Techniques

* [infra] #184 : Regroupement des fonctionnalités communes au application ent-core dans un module
* [infra] #179 : Ecriture d'module vertx de redimensionnement d'image basé sur _org.imgscalr_
* [test] Test d'intégration et performance. Ecriture de simulations Gatlin pour  : AppRegistry, Auth, Blog, Directory, Import

# v1.1.0

## Blog

* portage en application ent-core

## Droits et habilitation

* structuration de l'écran de partage en groupes et utilisateurs

## Opérations techniques

* [UX] passage à Angular.js
* [Test] intégration de Gatlin pour les tests intégration et de performance
* [infra] #357: Fusionner les application Directory et UserBook
* [infra] #161: Intégration du module vertx proxy HTTP pour simplifier les environements de développement
* [sécurité] #180: Création du type Authentification pour l'annotation SecuredAction pour filter l'accès simplement pour les utilisateurs authentifiés
* [infra] #211: Sortir les librairies utilitaires pour vertx dans un dépôt séparé
* [dictionnaire des données] #344:  pourvoir charger des fichiers ressource depuis le classpath

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
