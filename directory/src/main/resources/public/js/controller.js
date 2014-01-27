routes.define(function($routeProvider){
	$routeProvider
		.when('/search', {
			action: 'directory'
		})
		.when('/myClass', {
			action: 'myClass'
		})
		.when("/user-view/:userId", {
			action: "viewUser"
		})
		.when('/:userId', {
			action: 'viewUser'
		})
		.otherwise({
			redirectTo: '/myClass'
		})
});

function DirectoryController($scope, model, route){
	$scope.users = [];
	$scope.lang = lang;
	$scope.search = {
		text: '',
		field: '',
		maxLength: 15,
		clear: function(){
			this.text = '';
			this.field = '';
		}
	};

	$scope.increaseSearchSize = function(){
		$scope.search.maxLength += 15;
		if(!$scope.$$phase){
			$scope.$apply('search');
		}
	};

	$scope.viewsContainers = {};
	$scope.openView = function(view, name){
		$scope.search.maxLength = 15;
		var viewsPath = '/directory/public/template/';
		$scope.viewsContainers[name] = viewsPath + view + '.html';
	};

	$scope.containsView = function(name, view){
		var viewsPath = '/directory/public/template/';
		return $scope.viewsContainers[name] === viewsPath + view + '.html';
	};

	$scope.openView('class', 'page');
	$scope.openView('user-infos', 'details');

	route({
		viewUser: function(params){
			new User({ id: params.userId }).select();

			$scope.openView('profile', 'page');
			$scope.title = 'profile';
		},
		directory: function(){
			$scope.users = model.directory.users;
			$scope.openView('directory', 'page');
			$scope.viewsContainers.main = 'empty';
			$scope.title = 'directory';
		},
		myClass: function(){
			$scope.users = model.myClass.users;
			model.myClass.sync();
			$scope.openView('class', 'page');
			$scope.title = 'class';
		}
	});

	$scope.searchDirectory = function(){
		model.directory.users.all = [];
		model.directory.users.searchDirectory($scope.search.field);
		$scope.openView('list-view', 'main');
	};

	$scope.selectFirstUser = function(){
		if(model.directory.users.length){
			$scope.selectUser(model.directory.users.first());
		}
		else{
			$scope.selectUser(model.myClass.users.first());
		}
	};

	$scope.deselectUser = function(){
		model.directory.users.current.deselect();
		model.myClass.users.current.deselect();
		$scope.openView('list-view', 'main');
	};

	$scope.selectUser = function(user){
		if(!$scope.$$phase){
			$scope.$apply('search');
		}

		if($scope.containsView('main', 'user-selected')){
			ui.scrollToTop();
		}
		else{
			window.scrollTo(0, 200);
		}

		model.directory.users.deselectAll();
		model.myClass.users.deselectAll();
		user.select();

		$scope.openView('user-selected', 'main');
	};

	var colorsMatch = { relative: 'cyan', teacher: 'green', student: 'orange' };
	$scope.colorFromType = function(type){
		return colorsMatch[type.toLowerCase()];
	};

	model.on('directory.users.change, myClass.users.change', function(e){
		if(!$scope.$$phase){
			$scope.$apply('users');
		}
	});

	$scope.openView('list-view', 'main');
}

function ClassAdmin($scope, model, date){
	model.myClass.sync();
	model.on('myClass.users.change', function(){
		$scope.users = model.myClass.users;
		$scope.$apply('users');
	});

	$scope.shortDate = function(dateString){
		return moment(dateString).format('D/MM/YYYY');
	}

	$scope.display = {
		show: 'Student',
		selectAll: false
	};

	$scope.show = function(tab){
		$scope.display.show = tab;
	};

	$scope.switchAll = function(){
		if($scope.display.selectAll){
			model.myClass.users.selectAll();
		}
		else{
			model.myClass.users.deselectAll();
		}
	}
}