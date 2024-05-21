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

import { $, angular, idiom as lang, model, moment, ng, notify, template, ui } from 'entcore';
import { directory } from '../model';
import { filter } from 'core-js/core/array';

export const directoryController = ng.controller('DirectoryController',['$scope', '$window', 'route', '$location', ($scope, $window, route, $location) => {
	$scope.template = template;
	template.open('userActions', 'user-actions');
	$scope.users = {};
	$scope.groups = {};
	$scope.favorites = {};
	$scope.currentFavorite = null;
	$scope.lang = lang;
	$scope.lightbox = {};
	$scope.lightboxAddOneFavorite = {};
	$scope.currentDeletingFavorite = null;
	$scope.visibleUser = false;

	$scope.showDefaultValue = false;
	$scope.defaultValueTitle = '';

	$scope.currentUser = null;
	$scope.pastUsers = [];

	$scope.search = {
		users: '',
		groups: '',
		text: '',
		schoolField: '',
		maxLength: 50,
		maxSchoolsLength: 7,
		index: 0,
		clear: function(){
			this.text = '';
			this.field = '';
		}
	};

	$scope.create = {
		favorite: {
			userName: '',
			name: '',
			members: [],
			search: '',
			filters: {
				structures: [],
				classes: [],
				profiles: [],
				functions: [],
				types: [],
			}
		}
	}

	$scope.increaseSearchSize = function(){
		$scope.search.maxLength += 50;
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
		viewUser: async function(params){
			$scope.currentUser = new directory.User({ id: params.userId });
			await $scope.selectUser($scope.currentUser);
			$scope.users = directory.directory.users;
			$scope.allUsers = Object.assign([], $scope.users);
			template.open('page', 'profile');
			$scope.title = 'profile';
			await $scope.createAllFavorites();
		},
		viewGroup: async function(params){
			$scope.search.index = 1;
			$scope.selectTabAnnuaire('myNetwork')
			$scope.groups.searched = true;
			$scope.currentGroup = new directory.Group({ id: params.groupId });
			await $scope.currentGroup.getName();
			await $scope.showGroupUsers($scope.currentGroup);
			await $scope.createAllFavorites();
			$scope.title = 'profileGroup';
			template.open('page', 'profile');
			$scope.$apply();
		},
		directory: async function(){
			$scope.classView = false;
			$scope.display.loading = false;
			$scope.display.loadingmobile = false;
			$scope.display.showCloseMobile = false;
			$scope.classrooms = [];
			$scope.currentSchool = undefined;
			directory.directory.users.all = [];
			directory.directory.groups.all = [];
			directory.favoriteForm.users.all = [];
			directory.network.schools.all = [];
			$scope.users = directory.directory.users;
			$scope.allUsers = Object.assign([], $scope.users);
			$scope.groups = directory.directory.groups;
			await $scope.createAllFavorites();
			$scope.favoriteFormUsersGroups = [];
			if (!ui.breakpoints.checkMaxWidth("wideScreen")) {
				await $scope.selectFirstFavorite();
			}
			$scope.display.searchmobile = false;
			
			$scope.schools = directory.network.schools;
			await $scope.schools.sync();

			// Filters for search
			$scope.criteria = await directory.directory.users.getSearchCriteria();
			$scope.filters = {
				users: {
					structures: [],
					classes: [],
					profiles: [],
					functions: []
				},
				groups: {
					structures: [],
					classes: [],
					profiles: [],
					functions: [],
					types: []
				}
			};
			$scope.filtersOptions = {
				users: $scope.generateCriteriaOptions($scope.filters.users),
				groups: $scope.generateCriteriaOptions($scope.filters.groups)
			};
			$scope.create.favorite.options = $scope.generateCriteriaOptions($scope.create.favorite.filters);
			//fix structure filter (add manual group structures)
			$scope.filtersOptions.users.structures = $scope.schools.all.map(function(e){return {label:e.name, type: e.id}; });
			$scope.create.favorite.options.structures = $scope.schools.all.map(function(e){return {label:e.name, type: e.id}; });

			$scope.classesOrder = ['structId', 'label'];

			// Pre-apply filters that are specified in the url parameters :
			$scope.preApplyFilters();

			template.open('page', 'directory');
			template.close('list');
			template.open('list', 'dominos');
			$scope.title = 'directory';
			$scope.dominoInfosClass = 'dominos-infos';
			$scope.$apply();
		},
		myClass: async function(){
			$scope.classView = true;
			if($scope.network !== undefined){
				return;
			}
			await $scope.createAllFavorites();
			$scope.network = directory.network;
			directory.network.schools.sync();
			directory.network.schools.one('sync', function(){
				$scope.schools = directory.network.schools;
				$scope.currentSchool = $scope.schools.first();
				if($scope.currentSchool === undefined){
					template.open('page', 'noschool');
					return;
				}
				$scope.currentSchool.sync();
				$scope.showSchool($scope.currentSchool);

				$scope.currentSchool.one('sync', function(){
					$scope.users = $scope.currentSchool.users;
					$scope.allUsers = Object.assign([], $scope.users);

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
					template.open('dominosUser', 'dominos-user');	
					$scope.dominoClass ='my-class';
					$scope.title = 'class';
					$scope.$apply();
				});
			});
		}
	});

	directory.directory.on('users.change', function(){
		$scope.$apply('users');
	});

	$scope.avatarFor = function( user, thumbnail ) {
		return {
			 "background-image": "url(/userbook/avatar/"+user.id+ (typeof thumbnail==="string" ? "?thumbnail="+thumbnail : "" ) +")"
			,"background-size" : "cover"
			,"background-position": "center"
		};
	}

	$scope.createAllFavorites = async function(dateString){
		directory.directory.favorites.all = [];
		await directory.directory.favorites.getAll();
		$scope.favorites = directory.directory.favorites;
		$scope.favorites.all = $scope.favorites.all.sort($scope.sortByName);
	};

	$scope.generateCriteriaOptions = function(filters) {
		var test;
		return {
			structures: $scope.criteria.structures.map((element) => {
				return { label: element.name, type: element.id };
			}),
			classes: $scope.criteria.classes && $scope.criteria.classes.map((element) => {
				return { label: element.name, type: element.id };
			}),
			profiles: $scope.criteria.profiles.map((element) => {
				test = null;
				switch (element) {
				case "Student":
				case "Relative":
				case "Guest":
					test = $scope.testStudentRelativeGuestFilterAvailable(filters.functions);
					break;
				case "Teacher":
					test = $scope.testTeacherFilterAvailable();
					break;
				case "Personnel":
					test = $scope.testPersonnelFilterAvailable(filters.functions);
					break;
				}
				return { label: lang.translate("directory." + element), type: element, available: test };
			}),
			functions: $scope.criteria.functions.map((element) => {
				test = null;
				switch (element) {
				case "HeadTeacher":
					test = $scope.testHeadTeacherFilterAvailable(filters.profiles);
					break;
				/*
				case "AdminLocal":
					test = $scope.testADMLFilterAvailable(filters.profiles);
					break;
				*/
				}
				return { label: lang.translate(element), type: element, available: test };
			}),
			types: $scope.criteria.groupTypes.map((element) => {
				return { label: lang.translate("directory." + element), type: element };
			})
		};
	}

	$scope.onCheck = async function(option) {
		if (option.checked === true) {
			let filterClasses = await directory.directory.users.getSearchClasses(option.type);
			filterClasses.map(classe => {
				classe.structId = option.type;
				classe.type = classe.id;
			});
	
			if ($scope.search.index === 0 && $scope.isSelectedTabAnnuaire('myNetwork')) {
				$scope.filtersOptions.users.classes = $scope.filtersOptions.users.classes || [];
				$scope.filtersOptions.users.classes.push(...filterClasses);
			} else if ($scope.search.index === 1 && $scope.isSelectedTabAnnuaire('myNetwork')) {
				$scope.filtersOptions.groups.classes = $scope.filtersOptions.groups.classes || [];
				$scope.filtersOptions.groups.classes.push(...filterClasses);
			} else if ($scope.search.index === 2 && $scope.isSelectedTabAnnuaire('myNetwork')) {
				$scope.create.favorite.options.classes = $scope.create.favorite.options.classes || [];
				$scope.create.favorite.options.classes.push(...filterClasses);
			}
		} else {
			if ($scope.search.index === 0 && $scope.isSelectedTabAnnuaire('myNetwork')) {
				$scope.filtersOptions.users.classes = $scope.filtersOptions.users.classes.filter(classe => classe.structId != option.type);
				$scope.filters.users.classes = $scope.filters.users.classes.filter(
					filterModel => $scope.filtersOptions.users.classes.find(filterOption => filterModel === filterOption.type));
			} else if ($scope.search.index === 1 && $scope.isSelectedTabAnnuaire('myNetwork')) {
				$scope.filtersOptions.groups.classes = $scope.filtersOptions.groups.classes.filter(classe => classe.structId != option.type);
				$scope.filters.groups.classes = $scope.filters.groups.classes.filter(
					filterModel => $scope.filtersOptions.groups.classes.find(filterOption => filterModel === filterOption.type));
			} else if ($scope.search.index === 2 && $scope.isSelectedTabAnnuaire('myNetwork')) {
				$scope.create.favorite.options.classes = $scope.create.favorite.options.classes.filter(classe => classe.structId != option.type);
				$scope.create.favorite.filters.classes = $scope.create.favorite.filters.classes.filter(
					filterModel => $scope.create.favorite.options.classes.find(filterOption => filterModel === filterOption.type));
			}			
		}
	}

	$scope.showSchool = function(school){

		$scope.currentSchool = school;
		school.sync();

		school.one('sync', function(){
			$scope.users = school.users;
			$scope.allUsers = Object.assign([], $scope.users);
			$scope.classrooms = school.classrooms;
			$scope.deselectUser('dominos');
			$scope.$apply('users');
			$scope.$apply('classrooms');

			setTimeout(function(){
				$('body').trigger('whereami.update');
			}, 100);
		});


	};
	
	$scope.display = {};

	$scope.searchDirectory = async function(){
		$scope.showDefaultValue = false;
		$scope.defaultValueTitle = '';

		$scope.indexFormChanged($scope.search.index);

		// Favorite
		if ($scope.search.index == 2 && $scope.isSelectedTabAnnuaire('myNetwork')) {
			$scope.createFavorite();
			return;
		}

		$scope.display.loading = true;
		if (ui.breakpoints.checkMaxWidth("wideScreen")) {
			$scope.display.loadingmobile = true;
		}
		template.open('main', 'mono-class');
		template.open('list', 'dominos');
		if ($scope.search.index === 0 && $scope.isSelectedTabAnnuaire('myNetwork')) {
			await directory.directory.users.searchDirectory($scope.search.users, $scope.filters.users);
			$scope.users = directory.directory.users;
			$scope.allUsers = Object.assign([], $scope.users);
			template.open('dominosUser', 'dominos-user');
		} else if($scope.search.index === 0 && $scope.isSelectedTabAnnuaire('discoverVisible')) {
			template.open('dominosDiscoverVisibleUser', 'dominos-discover-visible-user');
		} else if ($scope.search.index === 1 && $scope.isSelectedTabAnnuaire('discoverVisible')) {
			template.open('dominosDiscoverVisibleGroup', 'dominos-discover-visible-group');
		} else {
			await directory.directory.groups.searchDirectory($scope.search.groups, $scope.filters.groups);
			$scope.groups = directory.directory.groups;
			template.open('dominosGroup', 'dominos-group');
		}
		$scope.display.searchmobile = false;
		$scope.display.showCloseMobile = $scope.display.searchmobile;
		$scope.display.loading = false;
		$scope.display.loadingmobile = false;
		if ((($scope.search.index === 0 && $scope.users.all.length === 0) || ($scope.search.index === 1 && $scope.groups.all.length === 0)) && $scope.isSelectedTabAnnuaire('myNetwork')) {
			if (ui.breakpoints.checkMaxWidth("wideScreen")) {
				notify.info("noresult");
			}
		}
		else
			$scope.display.searchmobile = true;
		$scope.$apply();
	};

	$scope.updateSearch = function() {
		$scope.users.all = $scope.allUsers.all.filter(user => user.displayName.toLowerCase().indexOf($scope.search.text.toLowerCase()) !== -1)
	}

	$scope.createFavorite = async function() {
		$scope.showFavoriteForm();
		$scope.currentFavorite = new directory.Favorite();
		$scope.create.favorite.name = '';
		$scope.create.favorite.members = [];
	};

	$scope.editFavorite = function() {
		$scope.display.editingFavorite = true;
		$scope.showFavoriteForm();
		$scope.create.favorite.name = $scope.currentFavorite.name;
		$scope.create.favorite.members = $scope.currentFavorite.groups.concat($scope.currentFavorite.users);
	}

	$scope.showFavoriteForm = function() {
		$scope.display.searchmobile = true;
		$scope.display.creatingFavorite = true;
		template.close('list');
		template.close('dominosUser');
		template.close('dominosGroup');
		template.open('list', 'favorite-form');
		$scope.scroolTop();
	}

	$scope.preHideFavoriteForm = function() {
		$scope.display.creatingFavorite = false;
	}

	$scope.hideFavoriteForm = function() {
		$scope.favoriteFormInitSearch();
		$scope.display.editingFavorite = false;
		template.close('list');
		template.open('list', 'dominos');
		$scope.scroolTop();
	}

	$scope.selectFavorite = async function(favorite, noupdate) {
		if (!$scope.display.creatingFavorite) {
			$scope.search.maxLength = 50;
			
			$scope.display.loading = true;
			if (ui.breakpoints.checkMaxWidth("wideScreen")) {
				$scope.display.loadingmobile = true;
			}
			await favorite.getUsersAndGroups();
			$scope.display.searchmobile = true;
			$scope.currentFavorite = favorite;
			$scope.currentGroup = null;
			$scope.deselectUser();
			template.open('dominosUser', 'dominos-user')
			template.open('dominosGroup', 'dominos-group')  
			$scope.display.loading = false;
			$scope.display.loadingmobile = false;
			if (!noupdate)
				$scope.$apply();
		}
	};

	$scope.cancelFavorite = async function() {
		$scope.search.maxLength = 50;
		
		var editing = $scope.display.editingFavorite;
		$scope.preHideFavoriteForm();

		if (editing) {
			await $scope.selectFavorite($scope.currentFavorite, true);
			$scope.display.searchmobile = true;
		}

		if (!editing) {
			$scope.currentFavorite = null;
			
			if (!ui.breakpoints.checkMaxWidth("wideScreen"))
				await $scope.selectFirstFavorite(true);
			$scope.display.searchmobile = false;
		}
		$scope.hideFavoriteForm();
		if (editing || !ui.breakpoints.checkMaxWidth("wideScreen"))
			$scope.$apply();
	};

	$scope.tryAddFavorite = function(favorite) {
		$scope.display.showUserCreationFavorite = false;
		$scope.create.favorite.userName = '';
		$scope.lightboxAddOneFavorite.show = true;
		template.open('lightbox', 'add-user-favorite');
	};

	$scope.addToFavorite = async function(favorite) {
		$scope.display.loading = true;
		await favorite.getUsersAndGroups();
		var newMember = $scope.currentUser ? $scope.currentUser : $scope.currentGroup;
		var members = favorite.groups.concat(favorite.users);
		var alreadyIn = false;
		members.forEach(member => {
			if (member.id === newMember.id) {
				alreadyIn = true;
				return;
			}
		});
		if (!alreadyIn) {
			members.push(newMember);
			await favorite.save(favorite.name, members, true);
		}
		$scope.display.loading = false;
		$scope.lightboxAddOneFavorite.show = false;
		template.close('lightbox');
		if (!alreadyIn) {
			$scope.$apply();
		}
		$scope.notifyAddUser(favorite.name);
	};

	$scope.confirmAddToFavorite = async function() {
		$scope.display.loading = true;
		var favorite = new directory.Favorite();
		await favorite.save($scope.create.favorite.userName, [$scope.currentUser ? $scope.currentUser : $scope.currentGroup], false);
		$scope.favorites.push(favorite);
		$scope.favorites.all.sort($scope.sortByName);
		$scope.display.loading = false;
		$scope.lightboxAddOneFavorite.show = false;
		template.close('lightbox');
		$scope.$apply();
		$scope.notifyAddUser(favorite.name);
	};

	$scope.notifyAddUser = function(name) {
		notify.success(lang.translate('directory.notify.confirmAddUser') + '</br>"' + name + '"');
	};

	$scope.searchUsersAndGroups = async function(event) {
		// Avoid submit form when pressing enter
		if (event)
			event.preventDefault();

		$scope.display.loadingFavoriteForm = true;
		await directory.favoriteForm.users.searchDirectory($scope.create.favorite.search, $scope.create.favorite.filters, null, true);
		$scope.favoriteFormUsersGroups = directory.favoriteForm.users.all;
		$scope.display.loadingFavoriteForm = false;
		$scope.$apply('favoriteFormUsersGroups');
	};

	$scope.deselectUser = function(tpl){
		$scope.currentUser = undefined;
		$scope.pastUsers = [];
		template.close('details');
	};

	const privateInfosMapping = {
		'SHOW_EMAIL': 'email',
		// 'SHOW_MAIL': 'email', unused field at this time
		'SHOW_PHONE': 'homePhone',
		'SHOW_BIRTHDATE': 'birthdate',
		'SHOW_HEALTH': 'health',
		'SHOW_MOBILE': 'mobile',
	}

	$scope.removePrivateInfos = function() {
		const infoToBeRemoved = {};
		Object.entries(privateInfosMapping).forEach(([k, v]) => {
			if (!$scope.currentUser.visibleInfos?.includes(k)) {
				infoToBeRemoved[v] = undefined;
			}
		});
		$scope.currentUser.updateData(infoToBeRemoved);
	}

	$scope.selectUser = async function(user){
		if(!$scope.$$phase){
			$scope.$apply('search');
		}

		if(typeof user == "string")
			user = new directory.User({ id: user });

		user.open();
		user.one('sync', async function(){
			$scope.currentUser = user;

			if($scope.pastUsers[$scope.pastUsers.length - 1] != user)
				$scope.pastUsers.push(user);
			//check visible
			$scope.visibleUser = await $scope.currentUser.visibleUser();

			if (model.me.type !== 'ELEVE') {
				await $scope.currentUser.loadInfos();
			}
			if ((model.me.type === 'ENSEIGNANT' || model.me.type === 'PERSEDUCNAT') && $scope.currentUser.type[0] === 'Relative') {
				await $scope.currentUser.loadChildren();
			}
			if($scope.currentUser !== undefined){
				$scope.scroolTop();
			}
			$scope.removePrivateInfos(); // Tmp fix: MOZO-77 prevent display of private data on this screen until backend removes them from the response.
			$scope.$apply('currentUser');
		});

		template.open('details', 'user-infos');
	};

	$scope.hasSubject = function(user): boolean {
		return user && user.profile == 'Teacher' && user.subjects && user.subjects.length > 0;
	}

	$scope.getSubject = function(user): string {
		if ($scope.hasSubject) {
			return user.subjects[0];
		} else return "";
	}

	$scope.deleteFavorite = function(favorite) {
		if (!$scope.display.creatingFavorite) {
			$scope.tryRemoveFavorite(favorite);
		}
	};

	$scope.tryRemoveFavorite = function(favorite) {
		$scope.currentDeletingFavorite = favorite;
		$scope.lightbox.show = true;
		template.open('lightbox', 'confirm-favorite-remove');
	};

	$scope.confirmRemoveFavorite = async function() {
		$scope.lightbox.show = false;
		template.close('lightbox');
		$scope.display.loading = true;
		$scope.search.maxLength = 50;
		var form = $scope.display.creatingFavorite;
		if (form)
			$scope.preHideFavoriteForm();
		if (ui.breakpoints.checkMaxWidth("wideScreen")) {
			$scope.display.loadingmobile = true;
		}
		await $scope.currentDeletingFavorite.delete();
		$scope.favorites.splice($scope.favorites.indexOf($scope.currentDeletingFavorite), 1);
		$scope.currentDeletingFavorite = null;
		if (!ui.breakpoints.checkMaxWidth("wideScreen")) {
			await $scope.selectFirstFavorite();
		}
		$scope.display.loading = false;
		$scope.display.loadingmobile = false;
		if (form) {
			if (ui.breakpoints.checkMaxWidth("wideScreen")) {
				$scope.currentFavorite = null;
			}
			$scope.display.searchmobile = false;
			$scope.hideFavoriteForm();
		}
		$scope.$apply();
	};

	$scope.saveFavorite = async function() {
		var isEditing = $scope.display.editingFavorite;
		$scope.display.loading = true;
		$scope.search.maxLength = 50;
		$scope.preHideFavoriteForm();
		if (ui.breakpoints.checkMaxWidth("wideScreen")) {
			$scope.display.loadingmobile = true;
		}
		await $scope.currentFavorite.save($scope.create.favorite.name, $scope.create.favorite.members, isEditing);
		if (!isEditing) {
			$scope.favorites.push($scope.currentFavorite);
		}
		$scope.favorites.all.sort($scope.sortByName);
		await $scope.selectFavorite($scope.currentFavorite, true);
		$scope.display.loading = false;
		$scope.display.loadingmobile = false;
		$scope.hideFavoriteForm();
		$scope.$apply();
	};

	$scope.back = function() {
		$scope.search.maxLength = 50;
		if($scope.pastUsers[$scope.pastUsers.length - 1] == $scope.currentUser)
		{
			$scope.pastUsers.pop();
			$scope.currentUser = $scope.pastUsers[$scope.pastUsers.length - 1];
		}
		else
			$scope.deselectUser();
	};

	$scope.backToSearch = function() {
		$scope.search.maxLength = 50;
		$scope.display.searchmobile = false;
		$scope.display.loadingmobile = false;
	};
	$scope.backToGroups = function() {
		$scope.search.maxLength = 50;
		$scope.currentGroup = null;
		if(!$scope.$$phase){		
			$scope.$apply('currentGroup');
		}
	};

	$scope.showGroupUsers = async function(group) {
		$scope.scroolTop();
		$scope.display.loading = true;
		if (ui.breakpoints.checkMaxWidth("wideScreen")) {
			$scope.display.loadingmobile = true;
		}
		await group.getUsers();
		$scope.currentGroup = group;
		template.open('dominosUser', 'dominos-user')
		template.open('groupActions', 'group-actions');
		template.open('groupInfos', 'group-infos');
		$scope.display.loading = false;
		$scope.display.loadingmobile = false;
		$scope.$apply();
	};

	$scope.selectClassroom = function(classroom){
		classroom.sync();
		$scope.classrooms = undefined;
		$scope.users = { loading: true };
		classroom.one('users.sync', function(){
			$scope.users = classroom.users;
			$scope.allUsers = Object.assign([], $scope.users);
			$scope.$apply('users');
		});
	};

	$scope.getType = function(type){
		if(type instanceof Array)
		 	return type[0]
		return type
	};

	$scope.colorFromType = function(type){
		return ui.profileColors.match(type) +" "+ $scope.dominoClass;
	};

	$scope.lightenColorFromType = function(type){
		return "lighten-" + this.colorFromType(type);
	};

	$scope.filterTopStructures = function(structure){
		return !structure.parents
	};

	$scope.displayChildren = function(currentUser) {
		return currentUser && currentUser.childrenStructure && currentUser.childrenStructure.length;
	};

	$scope.displayRelatives = function(currentUser) {
		return currentUser && currentUser.relatives.length && (currentUser.type[0] === 'Student') && (model.me.type === 'ENSEIGNANT' || model.me.type === 'PERSEDUCNAT');
	};


	$scope.onCloseSearchModule = function() {
		$scope.display.searchmobile = true;
	};

	$scope.getCurrentItemsLength = function() {
		if($scope.isSelectedTabAnnuaire('discoverVisible')) {
			switch($scope.search.index) {
				case 0:
					return $scope.discoverVisible.users.length;
				case 1:
					return $scope.discoverVisible.groups.length;
			}
		}

		switch($scope.search.index) {
			case 0:
				return $scope.users.all.length;
			case 1:
				return $scope.groups.all.length;
			case 2:
				return $scope.currentFavorite === null ? 0 : $scope.currentFavorite.users.length + $scope.currentFavorite.groups.length;
		}
	};

	$scope.indexFormChanged = function(index) {
		if ($scope.display.creatingFavorite) {
			template.close('list');
			if (index === 2 && $scope.isSelectedTabAnnuaire('myNetwork'))
				template.open('list', 'favorite-form');
			else if (index === 1 && $scope.isSelectedTabAnnuaire('discoverVisible'))
				template.open('list', 'discover-visible-group-add-edit');
			else
				template.open('list', 'dominos');
		}
		$scope.currentGroup = null;
		$scope.search.maxLength = 50;
		$scope.scroolTop();
		$scope.back();
	};

	$scope.switchForm = async function(index: number) {
		$scope.indexFormChanged(index);
		$scope.showDefaultValue = false;
		$scope.defaultValueTitle = '';
		template.open('main', 'mono-class');
		template.open('list', 'dominos');

		if (index === 0 && $scope.isSelectedTabAnnuaire('myNetwork')) {
			if (!$scope.users.all.length) $scope.display.loading = true;
			await directory.directory.users.searchDirectory($scope.search.users, $scope.filters.users);
			$scope.users = directory.directory.users;
			$scope.allUsers = Object.assign([], $scope.users);
			template.open('dominosUser', 'dominos-user');
		}
		else if (index === 1 && $scope.isSelectedTabAnnuaire('myNetwork')) {
			if (!$scope.groups.all.length) $scope.display.loading = true;
			await directory.directory.groups.searchDirectory($scope.search.groups, $scope.filters.groups);
			$scope.groups = directory.directory.groups;
			template.open('dominosGroup', 'dominos-group');
		} else if(index === 0 && $scope.isSelectedTabAnnuaire('discoverVisible')) {
			template.open('dominosDiscoverVisibleUser', 'dominos-discover-visible-user');
		} else if(index === 1 && $scope.isSelectedTabAnnuaire('discoverVisible')) {
			template.open('dominosDiscoverVisibleGroup', 'dominos-discover-visible-group');
		}
		
		$scope.display.searchmobile = false;
		$scope.display.showCloseMobile = $scope.display.searchmobile;
		$scope.display.loading = false;
		$scope.display.loadingmobile = false;
		$scope.$apply();
	}

	$scope.canFavoriteFormInitSearch = function() {
		return $scope.create.favorite.search || $scope.create.favorite.filters.structures || $scope.create.favorite.filters.classes || 
				$scope.create.favorite.filters.profles || $scope.create.favorite.filters.functions || 
				$scope.create.favorite.filters.types || $scope.favoriteFormUsersGroups.length > 0;
	};

	$scope.favoriteFormInitSearch = function() {
		$scope.create.favorite.search = '';
		$scope.create.favorite.filters.structures = [];
		$scope.create.favorite.filters.classes = [];
		$scope.create.favorite.filters.profiles = [];
		$scope.create.favorite.filters.functions = [];
		$scope.create.favorite.filters.types = [];
		$scope.checkOption($scope.create.favorite.options.structures, false);
		$scope.checkOption($scope.create.favorite.options.classes, false);
		$scope.checkOption($scope.create.favorite.options.profiles, false);
		$scope.checkOption($scope.create.favorite.options.functions, false);
		$scope.checkOption($scope.create.favorite.options.types, false);
		$scope.favoriteFormUsersGroups = [];
	};

	$scope.checkOption = function(array, check) {
		if(array) {
			array.forEach(element => {
				element.checked = check;
			});
		}
	};

	$scope.selectFirstFavorite = async function(noupdate) {
		if($scope.favorites.empty()) {
			$scope.currentFavorite = null;
		}
		else {
			await $scope.selectFavorite($scope.favorites.first(), noupdate);
		}
	};

	$scope.sortByName = function(a, b) {
		return a.name > b.name;
	};

	$scope.scroolTop = function () {
		var stick = $("[stick-to-top]")[0];
		if (stick) {
			stick.style.top = "0";
		}
		ui.scrollToTop();
	};

	$scope.isMultiStructure = function() {
		return $scope.criteria.structures.length > 1;
	};

	$scope.noStructureSelected = function(searchIndex) {
		switch (searchIndex) {
			case 0:
				return !$scope.filters.users.structures || !$scope.filters.users.structures.length;
			case 1:
				return !$scope.filters.groups.structures || !$scope.filters.groups.structures.length;
			case 2:
				return !$scope.create.favorite.filters.structures || !$scope.create.favorite.filters.structures.length;
			default:
				return false;
		}
	};

	$scope.getDisabledClassTitle = function(searchIndex) {
		let res = lang.translate('directory.classes');
		switch (searchIndex) {
			case 0:
				if ($scope.isMultiStructure() && $scope.noStructureSelected(searchIndex)) {
					res = lang.translate('directory.classes.disabled.pick.structure');
				}
			case 1:
				if ($scope.isMultiStructure() && $scope.noStructureSelected(searchIndex) 
					&& $scope.testClassFilterAvailable($scope.filters.groups.types, $scope.filters.groups.functions)) {
					res = lang.translate('directory.classes.disabled.pick.structure');
				}
			case 2:
				if ($scope.isMultiStructure() && $scope.noStructureSelected(searchIndex) 
					&& $scope.testClassFilterAvailable($scope.create.favorite.filters.types)) {
					res = lang.translate('directory.classes.disabled.pick.structure');
				}
			default:
				break;
		}
		return res;
	}

	$scope.testClassFilterAvailable = function(groups, functions) {
		return !groups.length && (!functions || (functions && !functions.length));
	};

	$scope.testProfileFilterAvailable = function(groups, functions) {
		return !groups.length && (!functions || (functions && !functions.length));
	};

	$scope.testFunctionFilterAvailable = function(profiles, groups, classes) {
		return (!groups && $scope.checkProfileTeacherPersonnel(profiles)) || (groups && ((classes && !groups.length && !profiles.length && !classes.length) || (!classes && !groups.length && $scope.checkProfileTeacherPersonnel(profiles))));
	};

	$scope.checkProfileTeacherPersonnel = function(profiles) {
		return profiles.length === 0 || profiles.indexOf("Teacher") !== -1 || profiles.indexOf("Personnel") !== -1;
	}

	$scope.testGroupTypeFilterAvailable = function(classes, profiles, functions) {
		return !classes.length && !profiles.length && !functions.length;
	};

	$scope.testHeadTeacherFilterAvailable = function(profiles) {
		return function() {
			return !profiles.length || profiles.indexOf("Teacher") !== -1;
		}
	};

	$scope.testADMLFilterAvailable = function(profiles) {
		return function() {
			return !profiles.length || profiles.indexOf("Teacher") !== -1 || profiles.indexOf("Personnel") !== -1;
		}
	};

	$scope.testStudentRelativeGuestFilterAvailable = function(functions) {
		return function() {
			return !functions.length;
		}
	};

	$scope.testTeacherFilterAvailable = function() {
		return function() {
			return true;
		}
	};

	$scope.testPersonnelFilterAvailable = function(functions) {
		return function() {
			return functions.indexOf("HeadTeacher") === -1;
		}
	};

	$scope.isMoodDefault = function(mood) {
		return mood === "default";
	};

	$scope.hasWorkflowZimbra = function() {
		return model.me.hasWorkflow('fr.openent.zimbra.controllers.ZimbraController|view');
	}

	$scope.hasWorkflowMessagerie = function() {
		return model.me.hasWorkflow('org.entcore.conversation.controllers.ConversationController|view');
	}

	$scope.getTarget = function(target, type) {
		return {...target, type}
	}

	/* Check and apply the URL parameters.
	 * @param filters		"users" | "groups"
	 * @param class			string | string[]
	 * @param profile		an ID
	 * @param structure		an ID
	 * 
	 * Example URL : /userbook/annuaire#/search?filters=groups&structure=an_id&profile=Teacher&class=TP1&class=TP2
	 */
	$scope.preApplyFilters = async function() {
		const params:{
			filters?:"users"|"groups",
			class?:string|Array<string>,
			profile?:string|Array<string>,
			structure?:string
		} = $location.search();

		let filters;

		switch( params.filters ) {
			case 'users': $scope.search.index = 0; break;
			case 'groups': $scope.search.index = 1; break;
			default: params.filters = 'users'; $scope.search.index = 0; break;
		}

		if( typeof params.profile === "string" ) {
			filters = filters || {};
			filters.profiles = [params.profile];
		} else if (angular.isArray(params.profile)) {
			filters = filters || {};
			filters.profiles = params.profile;
		}

		if( typeof params.structure === "string" ) {
			filters = filters || {};
			filters.structures = [params.structure];
		}

		if( typeof params.class === "string" ) {
			filters = filters || {};
			filters.classes = [params.class];
		} else if( angular.isArray(params.class) ) {
			filters = filters || {};
			filters.classes = params.class;
		}

		if( filters ) {
			// In responsive, display the results panel directly (not the search form)
			let showResultsPanel = ui.breakpoints.checkMaxWidth("wideScreen");
			$scope.showDefaultValue = false;
			$scope.defaultValueTitle = "";

			// Check the corresponding options in the search panel.
			// Let's do some typings to make this clear as water.
			type FilterOption = { label:string, type:/*id*/string, available?:boolean, checked?:boolean };
			const options:{
				structures:FilterOption[],
				classes:FilterOption[],
				profiles:FilterOption[],
//				functions:FilterOption[], 	// unused
//				types:FilterOption[]		// unused
			} = $scope.search.index === 0 ? $scope.filtersOptions.users : $scope.filtersOptions.groups;
			const checks = $scope.search.index === 0 ? $scope.filters.users : $scope.filters.groups;
			// Check the option if it exists in the array of ids.
			const checkIfExists = ( e:FilterOption, ids:string[], checkedFilters:string[] ) => {
				if( (typeof e.available==="undefined" || e.available) && angular.isArray(ids) && ids.indexOf(e.type)!==-1 ) {
					checkedFilters.push( e.type );
				}
			}
			options.structures?.forEach( e => checkIfExists(e, filters.structures, checks.structures) );
			options.classes?.forEach( e => checkIfExists(e, filters.classes, checks.classes) );
			options.profiles?.forEach( e => checkIfExists(e, filters.profiles, checks.profiles) );

			// Search directory for filtered results and display them.
			await directory.directory[params.filters].searchDirectory("", filters, null, false);
			if ($scope.search.index === 0 && $scope.isSelectedTabAnnuaire('myNetwork')) {
				$scope.users = directory.directory.users;
				$scope.allUsers = Object.assign([], $scope.users);
				template.open('dominosUser', 'dominos-user');
				showResultsPanel = showResultsPanel && $scope.users.all.length > 0;
			} else if ($scope.search.index === 0 && $scope.isSelectedTabAnnuaire('discoverVisible')) {
				template.open('dominosDiscoverVisibleUser', 'dominos-discover-visible-user');
				showResultsPanel = showResultsPanel && $scope.discoverVisible.users.length > 0; 
			} else if ($scope.search.index === 1 && $scope.isSelectedTabAnnuaire('discoverVisible')) {
				template.open('dominosDiscoverVisibleGroup', 'dominos-discover-visible-group');
				showResultsPanel = showResultsPanel && $scope.discoverVisible.groups.length > 0; 
			} 
			else {
				$scope.groups = directory.directory.groups;
				template.open('dominosGroup', 'dominos-group');
				showResultsPanel = showResultsPanel && $scope.groups.all.length > 0; 
			}
			if( showResultsPanel ) {
				$scope.display.searchmobile = true;
			}
		} 
	}

	$scope.discoverVisible = {
		users: [],
		displayCreateGroup: false,
		groups: [],
		selectedGroup: null,
		displaySelectedGroupUsers: {group:null, users:[]},
		displayEditGroup: false,
		editOrAddGroup: {searchedUsers: [], selectedUsers: [], groupName: null, baseUsersId: []},
		filters: {
			structures: [],
			profiles: [],
			search: ""
		},
		options: {
			structures: [],
			profiles: []
		}
	};

	$scope.annuaireTab = "myNetwork";

	$scope.getDiscoverVisibleOptions = function() {


		Promise.resolve(directory.discoverVisibleAcceptedProfiles()).then(function(result){

			var defaultProfiles = result.length === 1;

			for(var i = 0; i < result.length; i++) {
				switch(result[i]) {
					case 'Teacher':
						$scope.discoverVisible.options.profiles.push({label: 'Enseignant', type: 'Teacher', checked: defaultProfiles});
						break;
					case 'Personnel':
						$scope.discoverVisible.options.profiles.push({label: 'Personnel', type: 'Personnel', checked: defaultProfiles});
						break;
				}
			}

			if(result.length > 0 && $scope.discoverVisibleAutorize()) {
				$scope.getDiscoverVisibleGetGroups();
				Promise.resolve(directory.discoverVisibleStructure()).then(function(result){
					$scope.discoverVisible.options.structures = result;
				});
			}
		});
	}

	$scope.getDiscoverVisibleOptions();

	
	$scope.getDiscoverVisibleSearchUsers = async function() {

		$scope.display.loading = true;

		if($scope.discoverVisible.filters.structures.length === 0 && $scope.discoverVisible.filters.search === "") { 
			notify.info("userbook.discover.visible.users.search.filter.empty");
			$scope.display.loading = false;
			return;
		}

		var result = await directory.discoverVisibleUsers($scope.discoverVisible.filters);
		if(result.length === 0){
			$scope.discoverVisible.users = [];
		} else {
			$scope.discoverVisible.users = result;
			$scope.discoverVisible.editOrAddGroup.searchedUsers = result;

		}

		$scope.display.loading = false;

		$scope.$apply();

	}

	$scope.getDiscoverVisibleGetGroups = async function() {
		$scope.display.loading = true;

		var result = await directory.discoverVisibleGetGroups();
		$scope.discoverVisible.groups = result;
		
		$scope.display.loading = false;
		$scope.$apply('discoverVisible.groups');
	}

	$scope.isSelectedTabAnnuaire = function (tab) {
        if (!tab) {
            console.warn("[Directory][Annuaire.isSelectedTab] kind should not be null: ", tab)
        }
        return tab == $scope.annuaireTab;
    }
    $scope.selectTabAnnuaire = function (tab) {
		if($scope.discoverVisible.displayEditGroup || $scope.discoverVisible.displayCreateGroup) {
			$scope.backToDiscoverVisibleSelectedGroup();
		}
		$scope.search.index = 0;
        $scope.annuaireTab = tab;
    }

    $scope.selectedTabCssAnnuaire = function (tab) {
        return $scope.isSelectedTabAnnuaire(tab) ? "selected" : "";
    }


	$scope.discoverVisibleDisplayUsersInGroup = async function(selectedGroupId) {
		$scope.scroolTop();
		$scope.display.loading = true;
		if (ui.breakpoints.checkMaxWidth("wideScreen")) {
			$scope.display.loadingmobile = true;
		}
		$scope.discoverVisible.selectedGroup = selectedGroupId;
		var response;
		try{
			response = await directory.discoverVisibleGetUsersInGroup(selectedGroupId);
		} catch(e) {
			await $scope.getDiscoverVisibleGetGroups();
			await $scope.backToDiscoverVisibleGroups();
			return;
		}
		
		$scope.discoverVisible.displaySelectedGroupUsers.group = $scope.discoverVisible.groups.find(group => group.id === selectedGroupId);
		$scope.discoverVisible.displaySelectedGroupUsers.users = response;

		template.open('discoverVisibleGroupInfo', 'discover-visible-group-info');
		$scope.display.loading = false;
		$scope.display.loadingmobile = false;

		$scope.$apply();
	};

	$scope.discoverVisibleDisplayCreateOrEditGroup = async function(action) {
		$scope.scroolTop();
		$scope.display.loading = true;
		if (ui.breakpoints.checkMaxWidth("wideScreen")) {
			$scope.display.loadingmobile = true;
		}

		if(action === 'edit'){
			$scope.discoverVisible.displayEditGroup = true;
			$scope.discoverVisible.displayCreateGroup = false;
			$scope.discoverVisible.editOrAddGroup.selectedUsers = $scope.discoverVisible.displaySelectedGroupUsers.users;
			$scope.discoverVisible.displaySelectedGroupUsers.users.map(user => $scope.discoverVisible.editOrAddGroup.baseUsersId.push(user.id));
			$scope.discoverVisible.editOrAddGroup.groupName = $scope.discoverVisible.displaySelectedGroupUsers.group.name;
		} else {
			$scope.discoverVisible.selectedGroup = null;
			$scope.discoverVisible.displaySelectedGroupUsers.group = null;
			$scope.discoverVisible.displaySelectedGroupUsers.users = [];
			$scope.discoverVisible.displayEditGroup = false;
			$scope.discoverVisible.displayCreateGroup = true;
		}

	

		$scope.discoverVisible.editOrAddGroup.searchedUsers = $scope.discoverVisible.users;

		$scope.discoverVisible.editOrAddGroup.searchedUsers.forEach(user => {
			if(user.selected){
				user.selected = false;
			}
		});

		template.close('list');
		template.open('list', 'discover-visible-group-add-edit');
		$scope.display.loading = false;
		$scope.display.loadingmobile = false;

		$scope.$apply();
	};

	$scope.backToDiscoverVisibleGroups = function() {
		$scope.discoverVisible.selectedGroup = null;
		$scope.discoverVisible.displayEditGroup = false;
		$scope.discoverVisible.displayCreateGroup = false;
		$scope.discoverVisible.displaySelectedGroupUsers.group = null;
		$scope.discoverVisible.displaySelectedGroupUsers.users = [];
		$scope.discoverVisible.editOrAddGroup.searchedUsers = [];
		$scope.discoverVisible.editOrAddGroup.selectedUsers = [];
		$scope.discoverVisible.editOrAddGroup.baseUsersId = [];
		$scope.discoverVisible.editOrAddGroup.groupName = null;

		$scope.display.loading = false;

		template.open('dominosDiscoverVisibleGroup', 'dominos-discover-visible-group');
		
		$scope.$apply();
	};

	$scope.backToDiscoverVisibleSelectedGroup = async function() {

		$scope.discoverVisible.displayEditGroup = false;
		$scope.discoverVisible.displayCreateGroup = false;
		$scope.discoverVisible.displaySelectedGroupUsers.group = null;
		$scope.discoverVisible.displaySelectedGroupUsers.users = [];
		$scope.discoverVisible.editOrAddGroup.searchedUsers = [];
		$scope.discoverVisible.editOrAddGroup.selectedUsers = [];
		$scope.discoverVisible.editOrAddGroup.baseUsersId = [];
		$scope.discoverVisible.editOrAddGroup.groupName = null;

		if($scope.discoverVisible.selectedGroup !== null){
			await $scope.discoverVisibleDisplayUsersInGroup($scope.discoverVisible.selectedGroup);

		} else {
			await $scope.getDiscoverVisibleGetGroups();
	
		}

		template.close('list');
		template.open('list', 'dominos');
		$scope.display.loading = false;
		$scope.$apply();
	}

	$scope.discoverVisibleSaveGroup = async function() {
		$scope.display.loading = true;

		if($scope.discoverVisible.displayCreateGroup){
			var result = await directory.discoverVisibleCreateGroup($scope.discoverVisible.editOrAddGroup.groupName);
			if(result && result.id !== null){
				await directory.discoverVisibleAddUserToGroup(result.id, [], $scope.discoverVisible.editOrAddGroup.selectedUsers);
				await $scope.getDiscoverVisibleGetGroups();
			}

			$scope.discoverVisible.selectedGroup = result.id;

		} else {

			if($scope.discoverVisible.editOrAddGroup.groupName !== $scope.discoverVisible.displaySelectedGroupUsers.group.name) {
				await directory.discoverVisibleEditGroup($scope.discoverVisible.selectedGroup, $scope.discoverVisible.editOrAddGroup.groupName);
				await $scope.getDiscoverVisibleGetGroups();
			}

			await directory.discoverVisibleAddUserToGroup($scope.discoverVisible.selectedGroup, $scope.discoverVisible.editOrAddGroup.baseUsersId, $scope.discoverVisible.editOrAddGroup.selectedUsers);

		}

		await $scope.backToDiscoverVisibleSelectedGroup();

		$scope.$apply();
	}

	$scope.discoverVisibleCommuteUsers = async function(receiverId) {

		const result = await directory.discoverVisibleAddCommuteUsers(receiverId);
		if(result && result.number > 0) {
			const user = $scope.discoverVisible.users.find(item => item.id === receiverId);
			if(user) {
				user.hasCommunication = true;
				$scope.discoverVisible.users = [...$scope.discoverVisible.users.filter(item => item.id !== receiverId), user];
				$scope.$apply();
			}

			const userGroup = $scope.discoverVisible.displaySelectedGroupUsers.users.find(item => item.id === receiverId);
			if(userGroup) {
				userGroup.hasCommunication = true;
				$scope.discoverVisible.displaySelectedGroupUsers.users = [...$scope.discoverVisible.displaySelectedGroupUsers.users.filter(item => item.id !== receiverId), userGroup];
				$scope.$apply();
			}
		}
	} 

	$scope.discoverVisibleRemoveCommuteUsers = async function(receiverId) {

		const result = await directory.discoverVisibleRemoveCommuteUsers(receiverId);
		if(result && result.number > 0) {
			const user = $scope.discoverVisible.users.find(item => item.id === receiverId);
			if(user) {
				user.hasCommunication = false;
				$scope.discoverVisible.users = [...$scope.discoverVisible.users.filter(item => item.id !== receiverId), user];
				$scope.$apply();
			}
			const userGroup = $scope.discoverVisible.displaySelectedGroupUsers.users.find(item => item.id === receiverId);
			if(userGroup) {
				userGroup.hasCommunication = false;
				$scope.discoverVisible.displaySelectedGroupUsers.users = [...$scope.discoverVisible.displaySelectedGroupUsers.users.filter(item => item.id !== receiverId), userGroup];
				$scope.$apply();
			}
		}
	} 
	
	$scope.discoverVisibleAutorize = function() {
		if( $scope.discoverVisible.options.profiles !== null && $scope.discoverVisible.options.profiles.length > 0){
			for(var i = 0; i < $scope.discoverVisible.options.profiles.length; i++){
				if($scope.discoverVisible.options.profiles[i].type !== null && (model.me.type === 'Teacher' || model.me.type === 'ENSEIGNANT') && $scope.discoverVisible.options.profiles[i].type === 'Teacher'){			
					return true;
				} else if($scope.discoverVisible.options.profiles[i].type !== null && (model.me.type === 'Personnel' || model.me.type === 'PERSEDUCNAT') && $scope.discoverVisible.options.profiles[i].type === 'Personnel'){
					return true;
				}
			}
		}
		return false;
	}

	$scope.discoverVisibleExistGroup = async function() {
		await directory.discoverVisibleAddUserToGroup($scope.discoverVisible.selectedGroup, $scope.discoverVisible.displaySelectedGroupUsers.users.map(user => user.id), $scope.discoverVisible.displaySelectedGroupUsers.users.filter(user => user.id !== model.me.id).map(user => user.id));
		await $scope.getDiscoverVisibleGetGroups();
		await $scope.backToDiscoverVisibleGroups();
	}
}]);