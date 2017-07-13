import { ng, template, idiom as lang } from 'entcore';

export let mainController = ng.controller('MainController', ['$rootScope', '$scope', 'model', ($rootScope, $scope, model) => {

	template.open('notifspanel', 'notifspanel');

	$scope.template = template;

	model.applis.list();
    $scope.applis = model.applis;
	$scope.preference = model.preference;
	$scope.userinfos = model.userinfos;

	$scope.lang = lang;

	/*lang.addBundle('/timeline/i18nNotifications', function(){
		$scope.$apply();
	});*/

	$scope.saveChanges = function(userinfos){
		userinfos.putinfo()
	}

	$scope.appliFreq = function(appli){
		appli.appActions.each(function(appAction){
			appAction.defaultFrequency = appli.freq;
		})
	}

	$scope.updateAppliFreq = function(appli){
		var val = appli.appActions.all[0].defaultFrequency;
		var result = appli.appActions.every(function(appAction){
			return val === appAction.defaultFrequency
		})
		if(result){
			appli.freq = val
		} else {
			delete appli.freq
		}
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

}]);
