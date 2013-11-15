function MainController($rootScope, $scope){
	$scope.closePanel = function(){
		$rootScope.$broadcast('close-panel');
	}
}

function Timeline($scope, date, http, navigation, lang){
	$scope.notifications = [];
	$scope.me = {};
	$scope.types = [];
	$scope.translate = lang.translate;

	http.get('/timeline/types').done(function(types){
		$scope.types = types;
		$scope.$apply('types');
	});

	http.get('/auth/oauth2/userinfo', function(info){
		$scope.me = info;
		$scope.$apply('me');
	});

	http.get('/timeline/lastNotifications').done(function(response){
		$scope.notifications = response.results;
		$scope.$apply('notifications');
	});

	$scope.formatDate = function(dateString){
		return date.calendar(dateString);
	};

	$scope.isUnRead = function(notification){
		return _.find(notification.recipients, function(recipient){
			return recipient.userId === $scope.me.userId;
		}) !== undefined;
	};

	$scope.setFilter = function(filter){
		http.get('/timeline/lastNotifications?type=' + filter).done(function(response){
			$scope.notifications = response.results;
			$scope.$apply('notifications');
		});
	}

	$scope.navigate = navigation.navigate;
}

function Personalization($rootScope, $scope, http, ui){
	http.get('/timeline/public/json/themes.json').done(function(data){
		$scope.skins = data;
		$scope.$apply();
	})

	$scope.saveTheme = function(skin){
		ui.setStyle(skin.skinPath);
		http.get('/userbook/api/edit-userbook-info?prop=theme&value=' + skin._id);
	};

	$scope.togglePanel = function($event){
		$scope.showPanel = !$scope.showPanel;
		$event.stopPropagation();
	};

	$scope.create = {
		comment: {
			comment: ''
		}
	}

	$rootScope.$on('close-panel', function(e){
		$scope.showPanel = false;
	})
}


var LoadedWidgets = [];
LoadedWidgets.findWidget = function(name){
	return _.findWhere(LoadedWidgets, {name: name});
}

function Widgets($scope, http, _, lang){
	$scope.widgets = LoadedWidgets;
	LoadedWidgets.apply = function(){
		if(!$scope.$$phase) {
			$scope.$apply('widgets');
		}
	}

	http.get('/timeline/public/json/widgets.json').done(function(data){
		data.forEach(function(widget){
			LoadedWidgets.push(widget);
			loader.loadFile(widget.js);
		});
	});

	$scope.translate = lang.translate;
}