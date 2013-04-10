Documentation
=============

Le dictionnaire des données fournit les fonctionnalités suivantes :

* API de d'interaction avec un dictionnaire de données (seul un embryon AAF est disponible)

		boolean validateField(String name, String value);
		Map\<String, Boolean\> validateFields(Map\<String,String\> fileds);

* un API de validation de données extensible

* un IHM de consultation


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


