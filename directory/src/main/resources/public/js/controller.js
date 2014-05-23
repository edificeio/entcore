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

function DirectoryController($scope, model, route, date, template){
	$scope.template = template;
	$scope.users = [];
	$scope.lang = lang;
	$scope.search = {
		text: '',
		field: '',
		schoolField: '',
		maxLength: 15,
		clear: function(){
			this.text = '';
			this.field = '';
		}
	};

	var colorsMatch = { relative: 'cyan', teacher: 'green', student: 'orange', personnel: 'purple' };

	$scope.increaseSearchSize = function(){
		$scope.search.maxLength += 15;
		if(!$scope.$$phase){
			$scope.$apply('search');
		}
	};

	$scope.longDate = function(dateString){
		return moment(dateString).format('DD MMMM YYYY')
	};

	route({
		viewUser: function(params){
			new User({ id: params.userId }).select();
			$scope.users = model.directory.users;
			template.open('page', 'profile');
			$scope.title = 'profile';
		},
		directory: function(){
			$scope.users = model.directory.users;
			template.open('page', 'directory');
			template.close('main');
			$scope.title = 'directory';
		},
		myClass: function(){
			model.network.schools.sync();
			model.network.schools.on('sync', function(){
				$scope.schools = model.network.schools;
				$scope.currentSchool = $scope.schools.first();
				$scope.currentSchool.sync();
				$scope.currentSchool.one('sync', function(){
					$scope.users = $scope.currentSchool.users;
					$scope.classrooms = $scope.currentSchool.classrooms;

					template.open('page', 'class');

					if($scope.classrooms.length() === 1){
						template.open('main', 'mono-class');
					}
					else{
						template.open('main', 'multi-class');

					}

					template.open('list', 'dominos');
					$scope.title = 'class';
					$scope.$apply();
				});

			});
		}
	});

	model.directory.on('users.change', function(){
		$scope.$apply('users');
	});

	$scope.showSchool = function(school){
		$scope.currentSchool = school;
		school.sync();
		school.one('sync', function(){
			$scope.users = school.users;
			$scope.classrooms = school.classrooms;
			$scope.$apply('users');
			$scope.$apply('classrooms');
		});
	};

	$scope.searchDirectory = function(){
		model.directory.users.all = [];
		model.directory.users.searchDirectory($scope.search.field);

		template.open('main', 'mono-class');
		template.open('list', 'dominos');
	};

	$scope.deselectUser = function(){
		$scope.currentUser = undefined;
		template.open('list', 'dominos');
	};

	$scope.selectUser = function(user){
		if(!$scope.$$phase){
			$scope.$apply('search');
		}

		if(template.contains('list', 'user-selected')){
			ui.scrollToTop();
		}
		else{
			window.scrollTo(0, 200);
		}

		user.open();
		user.one('sync', function(){
			$scope.currentUser = user;
			$scope.$apply('currentUser');
		});

		template.open('list', 'user-selected');
		template.open('details', 'user-infos');
	};

	$scope.selectClassroom = function(classroom){
		classroom.sync();
		$scope.classrooms = undefined;
		classroom.one('users.sync', function(){
			$scope.users = classroom.users;
			$scope.$apply('users');
		});
	};


	$scope.colorFromType = function(type){
		return colorsMatch[type.toLowerCase()];
	};
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
		model.classAdmin.importFile($scope.import.csv[0], $scope.display.show.toLowerCase());
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