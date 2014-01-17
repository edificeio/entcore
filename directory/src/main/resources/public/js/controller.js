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

function Directory($scope, route){
	$scope.users = Model.users;
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
	}

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
			Model.users.deselectAll();
			new User({ id: params.userId }).select();

			$scope.openView('profile', 'page');
			$scope.title = 'profile';
		},
		directory: function(){
			Model.users.all = [];
			$scope.openView('directory', 'page');
			$scope.viewsContainers.main = 'empty';
			$scope.title = 'directory';
		},
		myClass: function(){
			$scope.openView('class', 'page');
			Model.users.loadClass();
			$scope.title = 'class';
		}
	});

	$scope.searchDirectory = function(){
		Model.users.all = [];
		Model.users.searchDirectory($scope.search.field);
		$scope.openView('list-view', 'main');
	};

	$scope.selectFirstUser = function(){
		$scope.selectUser(Model.users.first());
	};

	$scope.deselectUser = function(){
		Model.users.current.deselect();
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


		Model.users.deselectAll();
		user.select();

		$scope.openView('user-selected', 'main');
	};

	var colorsMatch = { relative: 'cyan', teacher: 'green', student: 'orange' };
	$scope.colorFromType = function(type){
		return colorsMatch[type.toLowerCase()];
	};

	Model.on('users.change', function(e){
		$scope.$apply('users');
	});

	$scope.openView('list-view', 'main');
}