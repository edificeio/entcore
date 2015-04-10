ENT Core : Notes de versions
=======================

# v1.13.0 (15/04/2015)

## Conversation

* Gestion des dossiers
* Gestion des pièces jointes
* Ajout du drag and drop

## Directory

* Onglet de recherche d'utilisateurs sur toutes les structures
* Vue pour afficher son réseau
* Sniplet trombinoscope
* Multithèmes dans l'admin
* Gestion des structures pour les ADML
* Ajout d'information dans la fiche utilisateur

## Feeder

* Ajout du profil invité
* Ajout de la source de l'import dans les informations de l'utilisateur
* Gestion des doublons
* Decodage des htmlentities dans les fichiers AAF
* Modification du validateur pour ne pas bloquer l'import si certains champs facultatifs sont invalides
* Import csv

## Infra

* Ajout du mode public dans la bibliothèque multimedia
* Visualisation du code source dans l'éditeur

## Portal

* Gestion du multi-thèmes en fonction du domaine
* Changement de appPrefix dans l'adapter

## Recorder

* Nouveau widget pour effectuer une capture sonore depuis le micro
* Possibilité d'enregistrer du son depuis la bibliothèque multimédia

## Session

* Changement du comportement des sessions pour avoir :
** Une session qui expire à la fermeture du navigateur
** La possibilité de garder une session persistante

## Timeline

* Rendre les uri relatives

## Workspace

* Visionneuse de documents pour les images, sons, vidéos et pdf
* Sélecteur pour indiquer le niveau de compression des images lors de l'ajout
* Possibilité d'avoir des documents publics (accesibles pour des utilisateurs non connectés)
* Correction des droits pour le versionning
* Possibilité de supprimer des commentaires

# v1.12.1 (07/04/2015)

## Anomalies corrigées

* Profil en double dans la réponse des infos d'un utilisateur
* Validation Pronote invalide pour les utilisateurs multi-profils

# v1.12.0 (05/03/2015)

## Archive

* Support du mode cluster

## Auth

* Validation de la complexité du mot de passe (configurable avec une regex)

## Cas

* Ajout de l'adapter pour le connecteur WebClasseur

## Conversation

* Amélioration de la vue snipplet

## Directory

* Un ADML peut ajouter ou supprimer une fonction (avant seul un admin central pouvait le faire)
* Blocage d'un utilisateur depuis la console d'admin
* Attribution de la fonction d'administrateur central depuis la console d'admin
* Affichage de la ou les fonction(s) d'un utilisateur dans sa page de détails
* Modification du nom d'une structure dans la console d'admin
* Création d'une structure pour un super admin dans la console d'admin
* Ajout de la gestion des groupes de classe dans la console d'admin
* Gestion des regroupements de structures dans la console d'admin
* Possibilité de restaurer un utilisateur en pré-suppression
* Amélioration de l'export

## Feeder

* Création d'un compte parent seulement si le compte enfant existe dans l'import
* Lors de l'import AAF le rattachement d'un personnel (ou enseignant) est effectué en prenant en compte les structures indiquées dans ses fonctions
* Suppression de tous les groupes au niveau classe lors de la transition

## Timeline

* Possibilité de spécifier la date de publication d'un évènement

## Worspace

* Ajout d'un caroussel
* Versionning des documents
* Drag and drop des fichiers depuis le bureau

## Framework

* Api pour connaître les droits de partage de chaque application
* Vérification des droits de partage à la compilation (read,comment,contrib,manager)
* Ajout d'une directive attachments
* Ajout d'une directive wizard
* Ajout d'un mode de stockage Swift
* Blocage du démarrage de l'application si le démarrage d'un module essentiel échoue
* Refactoring du theme
* Ajout d'une méthode générique pour le chargement des scripts js
* Refactoring de l'helper sql pour le support d'un driver jdbc
* Refactoring de la gestion des adresses du bus pour le mode cluster
* Afficher tous les partages sur une ressource même lorsqu'ils ne respectent pas les règles de communication
* Faire passer les appels aux unmanaged extension de neo4j par le persistor
* Implémentation abstraite de l'interface RepositoryEvents pour la transition des applications MongoDb

## Anomalies corrigées

* Redirection de l'utilisateur quand sa session a expiré
* Destruction correcte de ckeditor
* Autoriser la mise en cache des fichiers statiques
* Lorsqu'un dossier contient le caractère "#", l'import de doc se fait à la racine et pas dans le dossier

# v1.11.1 (04/02/2015)

## Workspace

* Le commentaire d'un document envoi maintenant une notification
* Suppression des partages lors de la mise à la corbeille d'un document ou d'un dossier. Attention, la restauration ne restaure pas les partages.

## Timeline

* Sauvegarde des préférences

## Framework

* Ajout d'un titre à la fenêtre du linker
* Mise à jour des traductions en anglais et portuguais

## Anomalies corrigées

* Correction de l'affichage de l'avatar par défaut
* Correction des règles de communications vers un groupe quand la longueur du chemin est égal à 1
* Suppression du double envoi de la requête lors de la procédure de mot de passe oublié
* Echappement de certains caractères dans les noms de dossiers
* Correction de l'initialisation d'un parent sans enfant après la création
* Correction du chemin vers l'admin de la classe

# v1.11.0 (14/01/2015)

## App-Registry

* Pouvoir utiliser tous les types de groupes pour affecter les roles
* Ajouter pour les adml le droit d'affecter les roles généraux

## Cas

* Ajout des services :
** Universalis
** Lesite.tv
** Kne
* Correction du service Pronote pour les personnels

## Directory

* Ajout de méthodes pour les utilisateurs dans l'api bus
* Ajout de droits pour les adml dans la console d'admin
* Lier manuellement les parents aux enfants
* Export de toutes les structures d'un adml

## Feeder

* Ajouter ou supprimer des utilisateurs dans les groupes manuels
* Pouvoir mettre à jour une structure

## Portal

* Message d'avertissement pour les navigateurs antiques

## Timeline

* Classement des widgets
* Activation ou désactivation des widgets

## Workspace

* Partage des dossiers
* Suppression des commentaires lors de la copie d'un document
* Suppression du casier
* Correction de la duplication de l'affichage des dossiers dans certains cas
* Affichage de la taille des documents
* Ajout du droit de partage pour les contributeurs
* Renommage des fichiers et dossiers
* Drag'n'drop des fichiers et des dossiers
* Message d'avertissement lors de l'upload multiple d'un même fichier
* Ajout d'un lien pour vider la corbeille

## Framework

* Ajout d'une directive pour le drag'n'drop

## Évolutions techniques

* Mise à jour de la version de gatling (2.0.3)

# v1.10.0 (24/11/2014)

## App-Registry

* Ajout d'une api bus

## Auth

* Bloquer le bouton retour sur la page de connexion

## Cas

* Ajout d'un module cas avec gestion des flux service, proxy et saml

## Directory

* Ajout d'une api bus

## Feeder

* Ajout d'un cron pour déclencher les imports

## Portal

* Les adml ont accès à la console d'administration
* Gestion des adml
* Gestion des groupes manuels dans l'admin
## Framework

* Possibilité de partager des ressources à un groupe manuel
* Ajout de traces applicatives
* Agrégateur de traces pour simplifier la construction de stats

## Évolutions techniques

* refactoring des fonctions pour gérer "l'héritage" de structures et le regroupement de fonctions

## Anomalies corrigées

* [Communication] appliquer les règles de com à un utilisateur qui est ajouté à un groupe manuel
* [Feeder] correction d'un problème de concurrence dans le chargement des données du graphe
* [Linker] le linker ne chargait pas les ressources dans certains cas
* [Timeline] correction de la taille des notifications
* [Workspace] les pdf étaient chargés avec un mauvais mime type

# v1.9.0 (05/11/2014)

## App-Registry

* Refactoring des api
* Rendre les api accessibles aux adml

## Auth

* Pouvoir configurer l'envoi du mot de passe de l'élève vers l'email de l'enseignant

## Communication

* Refactoring des api
* Rendre les api accessibles aux adml
* Refactoring de la gestion des règles de communication

## Directory

* Autoriser les enseignants à changer la date de naissance des élèves
* Ajout de deux onglets pour les enseignants et les personnels dans le paramétrage de la classe
* Un enseignant peut supprimer un autre enseignant dans l'admin de la classe

## Portal

* Style "sea"
* Nouvelle console d'administration
* Mise à jour générale de l'ux

## Timeline

* Suppression des droits workflow pour l'accès à la timeline (restreint aux utilisateurs authentifiés)
* Responsive

## Workspace

* Ajout d'un sniplet documents
* Documents affichés par défaut en vue icône

## Framework

* Ajout de patchwork et flexible cells
* Ajout d'une directive sniplets
* Nouveau composant calendrier
* Réintégration du module timeline
* Portage des modules sur le nouveau système de route (basé sur des annotations)
* Suppression de la dépendance processor (réintégré dans web-utils et common)
* Possibilité de surcharger toutes les clés i18n dans les assets
* Tâche gradle pour générer les clés manquantes dans les autres langues
* Ajout d'un validateur JsonSchema pour valider le corps des requêtes

## Évolutions techniques

* [Infra] Passage à web-utils version 1.8.0

## Anomalies corrigées

* [Auth] correction de la procédure de mot de passe oublié
* [Communication] manque d'un label sur les groupes ou les utilisateurs dans certains cas
* [Conversation] correction d'un problème de concurrence dans l'affichage
* [Conversation] duplication (dans la corbeille) des messages envoyés à soi même
* [Directory] prémunir neo4j d'un possible deadlock lors de la suppression multiple d'utilisateurs
* [Directory] invalider le cache quand les visibilitées changent
* [Portal] correction de l'adapter pour les applications externes

# v1.8.0 (03/09/2014)

## Archive

Archive est un nouveau module qui permet aux utilisateurs d'exporter leurs données. Ces dernières sont contenues dans un zip regroupant les ressources pour lesquelles l'utilisateur est propriétaire ainsi que ses ressources partagées.

## Conversation

* Pouvoir envoyer des messages à soi-même
* Optimisation des requêtes
* Ajout des images et des videos dans un message
* Modification des messages envoyés aux groupes lors de la transition
* Suppression de la boite lors de la suppression du propriétaire

## Directory

* Export des comptes


## Feeder

* Exporteur pour Eliot
* Ajout d'une notion de porteur
* Gestion des admins locaux
* Suppression manuelle d'utilisateurs
* Transition d'année scolaire
* Gestion des utilisateurs en attente de suppression

## Workspace

* Comptage des commentaires dans la vue icônes
* Gestion des quotas
* Modification des partages lors de la transition
* Suppression des documents lors de la suppression du propriétaire
* Aggregation des documents lors de l'export

## Framework

* Nouveaux composants CSS : menu en fleur et accordéons
* Directive : barre de progression
* Ajout d'un validateur jsonschema
* [Infra] Pourvoir charger des modules externes avant le chargement des modules ent-core
* [Infra] Ajout d'une directive grid cells
* Support de formules latex dans ckeditor
* Application automatique du scope dans AngularJs
* Ajout d'un composant Linker pour lier créer des liens vers des ressource de l'ENT.
* [Common] Chargement de filtres resource via un service loader avec fichier META-INF généré par annotation
* [Common] Framework sql pour simplier la création d'application avec postgresql
** Crud
** Gestion des partage
** Filtres de sécurité par défaut pour les ressources
* [Common] Framework mongodb pour simplier la création d'application avec mongodb
** Crud
** Gestion des partage
** Filtres de sécurité par défaut pour les ressources

## Évolutions techniques

* Nouveau plugin video pour ckeditor
* Ajout de la licence AGPL v3
* [Infra] Passage à neo4j version 2.1.2
* [Infra] Passage à vert.x version 2.1.2
* [Infra] Passage à web-utils version 1.7.0

## Anomalies corrigées

* [Feeder] éviter la duplication des relations lors de l'import dans certains cas
* [Infra] correction du chargement automatique des scripts neo4j au démarrage avec le mode embarqué
* [App-registry] mettre à jour les actions au démarrage
* [Workspace] déplacement d'un dossier à la racine

# v1.7.0 (04/06/2014)

## Annuaire
* \#603 : API de personnalisation des noms d'affichage des groupes
* Le widget "Anniversaire de la classe" se conforme désormais aux règles de communication
* Le service "Ma Classe" se conforme désormais aux règles de communication
* Ajout d'un écran qui propose une vue du réseau direct de l'utilisateur connecté. Cet écran liste les structures et les classes accessibles à l'utilisateur

## Anomalies corrigées

* [Espace documentaire] fixed files duplication when sharing
* [Userbook] get myclass without id
* [Portail] Branchement de la déconnexion dans la console d'admin
* [Annuaire] Chaînage des requêtes sources potentielles de Dead Lock pour Neo4j
* [Annuaire] \#1240 : Collecte des utilisateurs sans classe à l'intérieur d'une structure
* [infra] NPE si l'en-tête "accept-language" est absente
* [Annuaire] procédure de récupération de mot passe pour les élèves sans emails

## Évolutions technique

* [auth] Migration sur Angular
* [auth] Utilisation des template dynamique pour gérer le message d'information
* [infra] Passage à web-utils version 1.6.2

# v1.6.0 (21/05/2014)

__Importants__ : _Cette version conclue la séparation physique complète entre ENT Core et One. A partir ce celle-ci les évolutions et corrections de la base de code ne concernent qu'ENT Core_

## Modules

* Fusion _d'Admin_ dans _Portal_
* Fusion des modules _dataDictionnary_ et _Sync_ dans un module _Feeder_ (responsable de l'alimentation de l'annuaire)
* Extraction blog
* Extraction timeline

## Annuaire et Alimentation

* Modification du modèle d'annuaire pour gérer les personnels aux niveaux école et classe (+ migration des requêtes impliquant l'annuaire)
* Injecteur AAF 2D
* Injecteur Manuel
* Injecteur BE1D (portage nouveau modèle)
* Application des règle de communication par défaut après l'import
* \#1196 : affectation d'utilisateur à une ou plusieurs classes et ou structures
* \#1075 Création de personnel dans une structure

## Portail

* extraction du thème _Panda_
* paramétrisation du dossier contenant les Assets
* finalisation du thème _Raw_

## Framework

* Ajout d'une bibliothèque multi-média pour laisser les utilisateurs mutualiser leurs ressources dans toutes les applications ENT Core

## Évolution techique

* [HA] Stockage des sesseion dans des Map Hazelcast pour le déploiement en cluster
* [HA] Partage de la clé de signature de cookie pour des instances d'un cluster
* [Test] Test de montée en charge

# v1.5.2 (27/03/2014)

## Améliorations mineures

* \#1082: Déclinaison du thème sélectionné par défaut
* \#1083: Notification d'erreur après import d'un fichier non conforme
* \#1084: [Oss] ajouter un message de bienvenue dans la boite de l'enseignant
* \#1087: Gestion de l'ajout de fragment html d'intégration Video dans CK editor
* \#1092: [Auth] Validation d'une charte d'utilisation à l'activation des comptes
* \#1094: [Espace documentaire] Fichiers en .xls non reconnus comme des éléments de type tableur

## Anomalies corrigées
* \#1110: La liste des messages est parfois longue à charger

# v1.5.1 (13/03/2014)

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

# v1.5.0 (19/02/2014)

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

# v1.4.0 (15/01/2014)

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

# v1.3.0-neo4j2 (18/12/2013)

* Migration de la base données neo4j de la version 1.9.2 à 2.0.0

# v1.3.0 (10/12/2014)

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

# v1.2.1 (04/12/2013)

## Widgets

* Calendrier : Correction de la boucle infinie qui se produisait au mois de décembre.

# v1.2.0 (19/11/2013)

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

# v1.1.0 (30/10/2013)

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

# v1.0.0 (28/09/2013)

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
