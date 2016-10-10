// Copyright © WebServices pour l'Éducation, 2014
//
// This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation (version 3 of the License).
//
// For the sake of explanation, any module that communicate over native
// Web protocols, such as HTTP, with ENT Core is outside the scope of this
// license and could be license under its own terms. This is merely considered
// normal use of ENT Core, and does not fall under the heading of "covered work".
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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

function DirectoryController($scope, route, date, template){
	$scope.template = template;
	template.open('userActions', 'user-actions');
	$scope.users = [];
	$scope.lang = lang;
	$scope.search = {
		text: '',
		field: '',
		schoolField: '',
		maxLength: 20,
		maxSchoolsLength: 7,
		clear: function(){
			this.text = '';
			this.field = '';
		}
	};

	var colorsMatch = {
		relative: 'cyan',
		teacher: 'green',
		student: 'orange',
		personnel: 'purple',
		guest: 'pink'
	};

	$scope.increaseSearchSize = function(){
		$scope.search.maxLength += 15;
		if(!$scope.$$phase){
			$scope.$apply('search');
		}
	};

	$scope.increaseSchoolsSize = function(){
		$scope.search.maxSchoolsLength += 7;
		if(!$scope.$$phase){
			$scope.$apply('search');
		}
	};

	$scope.longDate = function(dateString){
		return moment(dateString).format('DD MMMM YYYY')
	};

	route({
		viewUser: function(params){
			$scope.currentUser = new User({ id: params.userId });
			$scope.currentUser.open();
			$scope.users = model.directory.users;
			template.open('page', 'profile');
			template.open('details', 'user-infos');
			$scope.title = 'profile';
		},
		directory: function(){
			$scope.classrooms = [];
			$scope.currentSchool = undefined;
			model.directory.users.all = [];
			model.network.schools.all = [];
			$scope.users = model.directory.users;
			$scope.schools = model.network.schools;
			$scope.schools.sync();
			$scope.filters = {};
			template.open('page', 'directory');
			template.close('list');
			$scope.title = 'directory';
		},
		myClass: function(){
			if($scope.network !== undefined){
				return;
			}
			$scope.network = model.network;
			model.network.schools.sync();
			model.network.schools.one('sync', function(){
				$scope.schools = model.network.schools;
				$scope.currentSchool = $scope.schools.first();
				if($scope.currentSchool === undefined){
					template.open('page', 'noschool');
					return;
				}
				$scope.currentSchool.sync();

				$scope.currentSchool.one('sync', function(){
					$scope.users = $scope.currentSchool.users;

					template.open('page', 'class');

					$scope.classrooms = $scope.currentSchool.classrooms;
					if($scope.schools.all.length === 1 && model.me.classes.length === 1){
						template.open('main', 'mono-class');
						$scope.myClass = $scope.classrooms.where({id: model.me.classes[0]});
						if($scope.myClass.length > 0)
							$scope.selectClassroom($scope.myClass[0]);
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
			$scope.deselectUser('dominos');
			$scope.$apply('users');
			$scope.$apply('classrooms');
		});
	};

	$scope.searchDirectory = function(){
		model.directory.users.all = [];
		model.directory.users.searchDirectory($scope.search.field, $scope.filters);
		model.directory.users.one('change', function(){
			$scope.users = model.directory.users;
			$scope.$apply('users');
		});

		template.open('main', 'mono-class');
		template.open('list', 'dominos');
	};

	$scope.deselectUser = function(tpl){
		$scope.currentUser = undefined;
		template.open('list', tpl);
		template.close('details');
		template.close('classNav');
	};

	$scope.selectUser = function(user){
		if(!$scope.$$phase){
			$scope.$apply('search');
		}

		if($scope.currentUser !== undefined){
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

		template.open('classNav', 'class-vertical-content');
		template.open('details', 'user-infos');
		template.close('list');
	};

	$scope.selectClassroom = function(classroom){
		classroom.sync();
		$scope.classrooms = undefined;
		$scope.users = { loading: true };
		classroom.one('users.sync', function(){
			$scope.users = classroom.users;
			$scope.$apply('users');
		});
	};

	$scope.getType = function(type){
		if(type instanceof Array)
		 	return type[0]
		return type
	}

	$scope.colorFromType = function(type){
		if(type instanceof Array)
			return colorsMatch[type[0].toLowerCase()];
		return colorsMatch[type.toLowerCase()];
	};

	$scope.filterTopStructures = function(structure){
		return !structure.parents
	};

	$scope.displayFamily = function(currentUser) {
		return currentUser.relatives.length && (model.me.type === 'ENSEIGNANT' || model.me.type === 'PERSEDUCNAT');
	};
}

function ClassAdminController($scope, date, notify){
	model.network.sync();
	model.network.one('schools.sync', function(){
		model.network.schools.forEach(function(school){
			school.sync();
		});
	});

	$scope.classAdmin = model.classAdmin;
	$scope.users = model.classAdmin.users;
	$scope.newUser = new User();
	$scope.import = {};
	$scope.me = model.me;
	$scope.display = {};

	model.network.on('classrooms-sync', function(){
		$scope.classrooms = _.filter(model.network.schools.allClassrooms(), function(classroom){
			return model.me.classes.indexOf(classroom.id) !== -1;
		});
		model.classAdmin.sync();
		$scope.$apply();
	});

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
			model.classAdmin.users.forEach(function(user){
			   user.selected = user.type === $scope.display.show;
		   });
		}
		else {
			model.classAdmin.users.forEach(function(user){
				if(user.type === $scope.display.show){
					user.selected = false;
				}
		   });
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
		model.directory.users.searchDirectory($scope.newUser.lastName, '', function(){
			$scope.existingMatchs = usersMatch.call(model.directory.users, $scope.newUser.firstName + ' ' + $scope.newUser.lastName);
			if($scope.existingMatchs.length > 0 && $scope.display.show === 'Student'){
				$scope.openView('link-user', 'lightbox');
                $scope.$apply();
				return;
			}
			$scope.$apply(function(){
				$scope.createUser();
			});
		});
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

	$scope.removeUsers = function(){
		model.classAdmin.users.removeSelection();
		$scope.display.confirmRemove = false;
	};

	$scope.uploadPhoto = function(){
		$scope.newUser.uploadAvatar()
	};

	$scope.checkUsersSource = function(selectedUsers) {
		return _.filter(selectedUsers, function(user) {
			return user.source != 'MANUAL' && user.source != 'CLASS_PARAM' && user.source != 'BE1D' && user.source != 'CSV'
		}).length < 1;
	}

}
