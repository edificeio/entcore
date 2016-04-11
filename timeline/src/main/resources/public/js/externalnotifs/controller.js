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

		if(!model.preference.preference.config)
			model.preference.preference.config = {}

		model.applis.each(function(appli){
			appli.appActions.each(function(appAction){
				var setKey = function(){
					if(!model.preference.preference.config[appAction.key]){
						model.preference.preference.config[appAction.key] = {}
					}
					return appAction.key
				}
				model.preference.preference.config[setKey()].defaultFrequency = appAction.defaultFrequency
			})
		})
		model.preference.putinfo();
	}

}
