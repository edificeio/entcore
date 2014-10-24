function MainController($rootScope, $scope, template, lang){
	$scope.closePanel = function(){
		$rootScope.$broadcast('close-panel');
	};

	template.open('main', 'main');
	template.open('widgets', 'widgets');
	template.open('settings', 'settings');
	template.open('notifications', 'notifications');

	$scope.template = template;
	$scope.lang = lang;
}

function Timeline($scope, date, model, lang){
	$scope.notifications = [];
	$scope.notificationTypes = model.notificationTypes;
	$scope.translate = lang.translate;

	model.on('notifications.change, notificationTypes.change', function(e){
		if(!$scope.$$phase){
			$scope.$apply('notifications');
			$scope.$apply('notificationTypes');
		}
	});

	lang.addBundle('/timeline/i18nNotifications', function(){
		$scope.notifications = model.notifications;
		$scope.$apply('notifications');
	});

	$scope.formatDate = function(dateString){
		return date.calendar(dateString);
	};

	$scope.removeFilter = function(){
		if(model.notificationTypes.noFilter){
			model.notificationTypes.deselectAll();
		}
		model.notifications.sync();
	};
}

function Personalization($rootScope, $scope, model, ui){
	$scope.skins = model.skins;

	$scope.saveTheme = function(skin, $event){
		$event.stopPropagation();
		skin.setForUser();
		ui.setStyle(skin.path);
	};

	$scope.togglePanel = function($event){
		$scope.showPanel = !$scope.showPanel;
		$event.stopPropagation();
	};

	$rootScope.$on('close-panel', function(e){
		$scope.showPanel = false;
	})
}