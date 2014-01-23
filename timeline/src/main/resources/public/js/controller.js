function MainController($rootScope, $scope){
	$scope.closePanel = function(){
		$rootScope.$broadcast('close-panel');
	}
}

function Timeline($scope, date, model, lang){
	$scope.notifications = []
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

	$scope.order = function(item){
		return moment(item.date.$date);
	};

	$scope.removeFilter = function(){
		if(Model.notificationTypes.noFilter){
			Model.notificationTypes.deselectAll();
		}
		Model.notifications.sync();
	};
}

function Personalization($rootScope, $scope, model, ui){
	$scope.skins = model.skins;

	$scope.saveTheme = function(skin, $event){
		$event.stopPropagation();
		skin.setForUser();
		ui.setStyle(skin.skinPath);
	};

	$scope.togglePanel = function($event){
		$scope.showPanel = !$scope.showPanel;
		$event.stopPropagation();
	};

	$rootScope.$on('close-panel', function(e){
		$scope.showPanel = false;
	})
}

function Widgets($scope, model, lang, date){
	$scope.widgets = model.widgets;

	model.on('widgets.change', function(){
		if(!$scope.$$phase){
			$scope.$apply('widgets');
		}
	})

	$scope.translate = lang.translate;
}