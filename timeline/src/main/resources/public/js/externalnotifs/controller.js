function MainController($rootScope, $scope, template, lang, model){

	template.open('notifspanel', 'notifspanel');

	$scope.template = template;

	model.applis.list();
    $scope.applis = model.applis;
	$scope.preference = model.preference

	$scope.lang = lang;

	lang.addBundle('/timeline/i18nNotifications', function(){
		$scope.$apply();
	});

	$scope.appliFreq = function(appli){
		appli.appActions.each(function(appAction){
			appAction.defaultFrequency = appli.freq;
		})
	}

	$scope.removeAppliFreq = function(appli){
		delete appli.freq
	}

	$scope.savePreferences = function(){

		model.applis // liste des applis
		appli.appActions // list les appActions qd t'es dns une appli

		function(appAction){
			var freq = appAction.defaultFrequency


		}
		// 1 - Pour chaque appli
			// 2 - Pour chaque appAction
				// 3 - On récupère la variable defaultFrequency
				// 4 - On cherche dans les préférences (model.preference.preference.config.clef-de-la-notif)
				// 5 - On affecte la defaultFrequency de l'appAction dans la preference
		// 6 - On appelle preference.putinfo
		// 7 - youpi
	}
}

//(ng-repeat > applis.all)
