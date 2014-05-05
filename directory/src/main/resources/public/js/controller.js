//Copyright. Tous droits réservés. WebServices pour l’Education.
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

function DirectoryController($scope, model, route, date){
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

	$scope.longDate = function(dateString){
		return moment(dateString).format('DD MMMM YYYY')
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
			$scope.users = model.directory.users;
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
			$scope.myClass = model.myClass;
			model.myClass.sync();
			$scope.openView('class', 'page');
			$scope.title = 'class';
		}
	});

	$scope.searchDirectory = function(){
		model.directory.users.all = [];
		model.directory.users.searchDirectory($scope.search.field);

		$scope.openView('dominos', 'main');
	};

	$scope.selectFirstUser = function(){
		if(model.myClass.users.length()){
			$scope.selectUser(model.myClass.users.first());
		}
		else{
			$scope.selectUser(model.directory.users.first());
		}
	};

	$scope.deselectUser = function(){
		if(model.directory.users.current){
			model.directory.users.current.deselect();
		}
		if(model.myClass.users.current){
			model.myClass.users.current.deselect();
		}
		$scope.openView('dominos', 'main');
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

	$scope.openView('dominos', 'main');
}

function ClassAdminController($scope, model, date, notify){
	model.classAdmin.sync();
	$scope.classAdmin = model.classAdmin;
	$scope.users = model.classAdmin.users;
	$scope.newUser = new User();
	$scope.import = {};

	$scope.viewsContainers = {};
	$scope.openView = function(view, name){
		if(name === 'lightbox'){
			ui.showLightbox();
		}
		var viewsPath = '/directory/public/template/';
		$scope.viewsContainers[name] = viewsPath + view + '.html';
	};

	$scope.containsView = function(name, view){
		var viewsPath = '/directory/public/template/';
		return $scope.viewsContainers[name] === viewsPath + view + '.html';
	};

	model.on('classAdmin.change, classAdmin.users.change', function(){
		$scope.display.importing = false;
		if(!$scope.$$phase){
			$scope.$apply('users');
			$scope.$apply('display');
			$scope.$apply('classAdmin');
		}
	});

	$scope.shortDate = function(dateString){
		return moment(dateString).format('D/MM/YYYY');
	};

	$scope.display = {
		show: 'Student',
		relative: 'Relative',
		selectAll: false,
		limit: 20,
		relativeSearch: ''
	};

	$scope.show = function(tab){
		model.classAdmin.users.deselectAll();
		$scope.display.show = tab;
		$scope.display.limit = 20;
		if(tab === 'Relative'){
			$scope.display.relative = 'Student';
		}
		else{
		   $scope.display.relative = 'Relative';
		}
	};

	$scope.showMore = function(){
		$scope.display.limit += 20;
	};

	$scope.clearSearch = function(){
		$scope.display.relativeSearch = '';
		$scope.updateFoundRelatives();
	};

	$scope.updateFoundRelatives = function(){
		if(!$scope.display.relativeSearch){
			$scope.foundRelatives = '';
			return;
		}
		$scope.foundRelatives = _.filter(
			model.classAdmin.users.match($scope.display.relativeSearch), function(user){
				return user.type === $scope.display.relative && $scope.newUser.relatives.indexOf(user) === -1;
			}
		);
	};

	$scope.addRelative = function(){
		$scope.newUser.relatives.push($scope.newUser.newRelative);
		$scope.clearSearch();
	};

	$scope.removeRelative = function(relative){
		$scope.newUser.removeRelative(relative);
	};

	$scope.switchAll = function(){
		if($scope.display.selectAll){
			model.classAdmin.users.selectAll();
		}
		else{
			model.classAdmin.users.deselectAll();
		}
	};

	$scope.saveClassInfos = function(){
		model.classAdmin.saveClassInfos();
	};

	$scope.importCSV = function(){
		$scope.display.importing = true;
		model.classAdmin.importFile($scope.import.csv, $scope.display.show.toLowerCase());
		ui.hideLightbox();
	};

	$scope.grabUser = function(user){
		model.classAdmin.grabUser(user);
		notify.info('user.added');
		ui.hideLightbox();
		$scope.newUser = new User();
	};

	$scope.createUser = function(){
		ui.hideLightbox();
		var user = $scope.newUser;
		user.type = $scope.display.show;
		model.classAdmin.addUser(user);
		$scope.newUser = new User();
		$('#lastName').focus();
		notify.info('user.added');
	};

	$scope.addUser = function(){
		$scope.clearSearch();
		$scope.existingMatchs = usersMatch.call(model.directory.users, $scope.newUser.firstName + ' ' + $scope.newUser.lastName);
		if($scope.existingMatchs.length > 0 && $scope.display.show === 'Student'){
			$scope.openView('link-user', 'lightbox');
			return;
		}

		$scope.createUser();
	};

	$scope.blockUsers = function(){
		model.classAdmin.blockUsers(true);
	};

	$scope.unblockUsers = function(){
		model.classAdmin.blockUsers(false);
	};

	$scope.resetPasswords = function(){
		if(!model.me.email){
			notify.error('error.missing.mail');
		}
		else{
			notify.info('info.passwords.sent');
			model.classAdmin.resetPasswords();
		}
	};

	$scope.uploadPhoto = function(){
		$scope.newUser.uploadAvatar()
	};
}

function SchoolController($scope, template){
	$scope.template = template;
	$scope.template.open('list', 'table');

	$scope.search = {
		text: '',
		maxLength: 20
	};

	model.network.schools.sync();
	$scope.schools = model.network.schools;
	model.network.schools.on('sync', function(){
		$scope.$apply('schools');
	});

	$scope.openSchool = function(school){
		$scope.currentSchool = school;
		school.sync();
		school.on('sync', function(){
			$scope.users = school.users;
			$scope.classrooms = school.classrooms;

			$scope.$apply('users');
			$scope.$apply('classrooms');
		})
	};

	var colorsMatch = { relative: 'cyan', teacher: 'green', student: 'orange' };
	$scope.colorFromType = function(type){
		return colorsMatch[type.toLowerCase()];
	};

	$scope.increaseSearchSize = function(){
		$scope.search.maxLength += 20;
	};

	$scope.updateSearch = function(){
		if($scope.template.contains('list', 'user-infos')){
			$scope.template.open('list', 'table');
		}
	};

	$scope.selectUser = function(user){
		window.scrollTo(0, 200);
		model.myClass.users.deselectAll();
		user.select();

		user.on('change', function(){
			$scope.$apply('users')
		});

		$scope.template.open('list', 'user-infos');
	}
}