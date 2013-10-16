ONE : Notes de versions
=======================
# v1.1.0

## Blog

* portage en application ent-core

## Droits et habilitation

* structuration de l'écran de partage en groupe

## Admin

* [Annuaire] #20: Affichage des parents dans l'annuaire admin
* [Annuaire] #22: Ajouter les infos de classe et école dans les exports utilisateurs

## Opérations techniques

* [UX] passage à Angular.js
* [Test] intégration de Gatlin pour les tests intégration et de performance
* [infra] #357: Fusionner les application Directory et UserBook
* [infra] #161: Intégration du module vertx proxy HTTP pour simplifier les environements de développement
* [sécurité] #180: Création du type Authentification pour l'annotation SecuredAction pour filter l'accès simplement pour les utilisateurs authentifiés
* [infra] #211: Sortir les librairies utilitaires pour vertx dans un dépôt séparé
* [dictionnaire des données] #344:  pourvoir charger des fichiers ressource depuis le classpath

## Anomalies corrigées

* #355: Mettre à jour l'avatar plutôt que de créer un nouveau document "workspace"

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
