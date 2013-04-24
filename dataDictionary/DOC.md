Documentation
=============

Le dictionnaire des données est responsables des aspects suivants :

* Chargement de la structure des données d'annuaire (personnes, groupes, structure)
* Validation de données
* Génération de données (attributs calculé)
 - les règles de calcul sont implémentés selon le contrat de l'interface Generator
 - elles sont chargés par réflexion à l'instanciation du dictionnaires
 - elles sont seulement utilisées lors de la synchronisation de donnée. Proposer un modèle d'appel qui permette d'exécuter une règle à chaque fois qu'un des "champs arguments" est modifié.
* IHM de consultation
* API
		boolean validateField(String name, List<String> values);
		boolean validateField(String name, String value);
		Map<String, Boolean> validateFieldsList(Map<String,List<String>> fields);
		Map<String, Boolean> validateFields(Map<String,String> fields);
		Map<String, List<String>> generateField(Map<String, List<String>> values);

# Structure

* Un dictionnaire est composé de catégories. Elles les entités de première classe manipuler par le système
* Une catégorie est typée avec un profil de spécialisation et dispose d'un catalogue d'attribut
* Un attribut peut être multi-valué, modifiable, générable, restreint à certain profil ...

## Dictionnaire

		{
			"personnes" : {},
			"groupes" : {},
			"structures" : {}
		}

## Catégorie

		"personnes" : {
			"types" : ["ELEVE","PERS_REL_ELEVE","ENSEIGNANT","NON_ENSEIGNANT","EXTERIEUR"], // Liste des profil de spécialisation (possibilité d''extenstion au runtime')
			"attributs" : [] // catalogue d'attributs
		}

## Attribut

		{
			"label":"Login", // [obligatoire] libéllé affiché sur les IHM (Est-il judicieux de le trabuire sachant qu'un dictionnaire dépend fortement d'un région)
			"id":"ENTPersonLogin", // [obligatoire]
			"note":"Identifiant de connexion à l'ENT", // [optionnel]
			"obligatoire":true, // [optionnel, defaut=false] Si des restrictions sont définies alors l'attributs n'est obligatoire que pour les objets avec un des types de restriction
			"multiple":false, // [optionnel, defaut=false]
			"restrictions" : ["ELEVE","PERS_REL_ELEVE","ENSEIGNANT","NON_ENSEIGNANT"] // [optionnel
			"validator":"birthDate" // le nom du validator
			"auto": {"generator":"login", "args":["ENTPersonPrenom", "ENTPersonNom"]} // règle de calcul : utilisé de generotor avec les valeurs des attributs listés dans args.
		}

# TODO

* modéliser un dictionnaire de données ONE indépendant des standards AAF, BE1D
* modéliser complètement AAF
* Associer les champs d'un dictionnaire spécifique (ex. AAF) aux champs du dictionnaire ONE
* Implémenter une API "DSL" standard de parcours d'un dictionnaire
** gestion des Index (c'est peut être pas ici)
** production de requêtes Cypher
** L'implémentation par défaut utilise les champs du dictionnaire ONE comme pivot
* Implémenter un IHM de modification
** traduction des champs (Est-ce vraiment utile ? Sachant qu'un dictionnaire d'ENT anglais serait indépendant et décrit sa langue)
** politique d'édition de champs (Ajout / Modification)
** synchronisation des dictionnaires chargés précédemment par d'autre application
* tester l'intégration avec Sync et notamment les performances (On peut envisager classer les champs par fréquences d'utilisation au runtime de manière à limiter les temps de parcours).


