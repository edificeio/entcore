function MainController($rootScope, $scope, template, lang, model){
	$scope.closePanel = function(){
		$rootScope.$broadcast('close-panel');
	};

	$scope.widgets = model.widgets;

	template.open('main', 'main');
	template.open('widgets', 'widgets');
	template.open('settings', 'settings');
	template.open('notifications', 'notifications');
	template.open('notifspanel', 'notifspanel');

	$scope.template = template;
	$scope.lang = lang;
}

function Timeline($scope, date, model, lang){
	$scope.notifications = [];
	$scope.notificationTypes = model.notificationTypes;
    $scope.registeredNotifications = model.registeredNotifications;
	$scope.translate = lang.translate;
    $scope.filtered = {}

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

	$scope.loadPage = function(){
		model.notifications.sync(true);
	}

    $scope.filterTypes = function(typeObj){
        var type = typeObj.data
        var matchingNotifs = $scope.registeredNotifications.filter(function(notif){ return notif.type === type })
        if(matchingNotifs.length < 1)
            return true
        var access = model.me.apps.some(function(app){
            return _.some(matchingNotifs, function(n){
                return app.name.toLowerCase() === n.type.toLowerCase() || (n["app-name"] && app.name.toLowerCase() === n["app-name"].toLowerCase())
            })
        })
        if(!access){
            typeObj.selected = false
        }
        return access
    }
}

function Personalization($rootScope, $scope, model, ui){
	$scope.skins = model.skins;
	$scope.widgets = model.widgets;

	$scope.saveTheme = function(skin, $event){
		$event.stopPropagation();
		skin.setForUser();
		ui.setStyle(skin.path);
	};

	$scope.togglePanel = function($event){
		$scope.showPanel = !$scope.showPanel;
		$event.stopPropagation();
	};

	$scope.display = {};

	$scope.showNotifs = function() {
		$scope.dispaly.showNotifsPanel = true;
	};

	$scope.hideNotifs = function() {
		$scope.dispaly.showNotifsPanel = false;
	};

	$('lightbox[show="display.showNotifsPanel"]').on('click', function(event){
		event.stopPropagation()
	});

	$rootScope.$on('close-panel', function(e){
		$scope.showPanel = false;
	})
}

function Notifications($scope, model, lang){

}
