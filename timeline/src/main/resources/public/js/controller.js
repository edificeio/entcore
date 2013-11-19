function MainController($rootScope, $scope){
	$scope.closePanel = function(){
		$rootScope.$broadcast('close-panel');
	}
}

function Timeline($scope, date, http, model, lang){
	$scope.model = model;
	$scope.translate = lang.translate;

	model.on('change', function(e){
		if(!$scope.$$phase){
			$scope.$apply('model');
			console.log($scope.model);
		}

	})

	$scope.formatDate = function(dateString){
		return date.calendar(dateString);
	};

	$scope.setFilter = function(filter){
		$scope.model.notificationsTypes.current = filter;
	};

	$scope.removeFilter = function(){
		$scope.model.notificationsTypes.current = null;
	}
}

function Personalization($rootScope, $scope, http, ui){
	http.get('/timeline/public/json/themes.json').done(function(data){
		$scope.skins = data;
		$scope.$apply();
	})

	$scope.saveTheme = function(skin, $event){
		$event.stopPropagation();
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