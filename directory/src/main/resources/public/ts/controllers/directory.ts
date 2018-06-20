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

import { template, notify, idiom as lang, ng, ui, model, moment, $ } from 'entcore';
import { directory } from '../model';

export const directoryController = ng.controller('DirectoryController',['$scope', 'route', ($scope, route) => {
	$scope.template = template;
	template.open('userActions', 'user-actions');
	$scope.users = {};
	$scope.groups = {};
	$scope.favorites = {};
	$scope.currentFavorite = null;
	$scope.lang = lang;
	$scope.lightbox = {};
	$scope.currentDeletingFavorite = null;

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

	var colorsMatch = {
		relative: 'cyan',
		teacher: 'green',
		student: 'orange',
		personnel: 'purple',
		guest: 'pink'
	};

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
		viewUser: function(params){
			$scope.currentUser = new directory.User({ id: params.userId });
			$scope.currentUser.open();
			$scope.users = directory.directory.users;
			template.open('page', 'profile');
			template.open('details', 'user-infos');
			$scope.title = 'profile';
		},
		directory: async function(){
			$scope.display.searchmobile = false;
			$scope.display.loading = false;
			$scope.display.loadingmobile = false;
			$scope.display.showCloseMobile = false;
			$scope.classrooms = [];
			$scope.currentSchool = undefined;
			directory.directory.users.all = [];
			directory.directory.groups.all = [];
			directory.directory.favorites.all = [];
			directory.favoriteForm.users.all = [];
			directory.favoriteForm.groups.all = [];
			directory.network.schools.all = [];
			await directory.directory.favorites.getAll();
			$scope.users = directory.directory.users;
			$scope.groups = directory.directory.groups;
			$scope.favorites = directory.directory.favorites;
			$scope.favorites.all = $scope.favorites.all.sort($scope.sortByName);
			$scope.favoriteFormUsersGroups = [];
			if (!ui.breakpoints.checkMaxWidth("wideScreen")) {
				await $scope.selectFirstFavorite();
			}
			
			$scope.schools = directory.network.schools;
			await $scope.schools.sync();

			// Filters for search
			$scope.criteria = await directory.directory.users.getSearchCriteria();
			$scope.filters = {
				users: {
					structures: null,
					classes: null,
					profiles: null,
					functions: null
				},
				groups: {
					structures: null,
					classes: null,
					profiles: null,
					functions: null,
					types: null
				}
			};
			$scope.filtersOptions = {
				users: $scope.generateCriteriaOptions(),
				groups: $scope.generateCriteriaOptions()
			};

			$scope.create = {
				favorite: {
					userName: '',
					name: '',
					members: [],
					search: '',
					filters: {
						structures: null,
						classes: null,
						profiles: null,
						functions: null,
						types: null,
					},
					options: $scope.generateCriteriaOptions()
				}
			}

			template.open('page', 'directory');
			template.close('list');
			template.open('list', 'dominos');
			$scope.title = 'directory';
			$scope.$apply();
		},
		myClass: function(){
			if($scope.network !== undefined){
				return;
			}
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
					template.open('dominosUser', 'dominos-user');					
					$scope.title = 'class';
					$scope.$apply();
				});
			});
		}
	});

	directory.directory.on('users.change', function(){
		$scope.$apply('users');
	});

	$scope.generateCriteriaOptions = function() {
		return {
			structures: $scope.criteria.structures.map((element) => {
				return { label: element.name, type: element.id };
			}),
			classes: $scope.criteria.classes.map((element) => {
				return { label: element.name, type: element.id };
			}),
			profiles: $scope.criteria.profiles.map((element) => {
				return { label: lang.translate("directory." + element), type: element };
			}),
			functions: $scope.criteria.functions.map((element) => {
				return { label: lang.translate("directory." + element), type: element };
			}),
			types: $scope.criteria.groupTypes.map((element) => {
				return { label: lang.translate("directory." + element), type: element };
			})
		};
	}

	$scope.showSchool = function(school){

		$scope.currentSchool = school;
		school.sync();

		school.one('sync', function(){
			$scope.users = school.users;
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
		$scope.search.maxLength = 50;

		// Favorite
		if ($scope.search.index == 2) {
			$scope.createFavorite();
			return;
		}

		$scope.display.loading = true;
		if (ui.breakpoints.checkMaxWidth("wideScreen")) {
			$scope.display.loadingmobile = true;
		}
		template.open('main', 'mono-class');
		template.open('list', 'dominos');
		if ($scope.search.index === 0) {
			await directory.directory.users.searchDirectory($scope.search.users, $scope.filters.users);
			$scope.users = directory.directory.users;
			template.open('dominosUser', 'dominos-user');
		}
		else {
			await directory.directory.groups.searchDirectory($scope.search.groups, $scope.filters.groups);
			$scope.groups = directory.directory.groups;
			template.open('dominosGroup', 'dominos-group');
		}
		$scope.display.searchmobile = false;
		$scope.display.showCloseMobile = $scope.display.searchmobile;
		$scope.display.loading = false;
		$scope.display.loadingmobile = false;
		if (($scope.search.index === 0 && $scope.users.all.length === 0) || ($scope.search.index === 1 && $scope.groups.all.length === 0)) {
			if (ui.breakpoints.checkMaxWidth("wideScreen")) {
				notify.info("noresult");
			}
		}
		else
			$scope.display.searchmobile = true;
		$scope.$apply();
	};

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
	}

	$scope.preHideFavoriteForm = function() {
		$scope.display.creatingFavorite = false;
	}

	$scope.hideFavoriteForm = function() {
		$scope.favoriteFormUsersGroups = [];
		$scope.display.editingFavorite = false;
		template.close('list');
		template.open('list', 'dominos');
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
		$scope.lightbox.show = true;
		template.open('lightbox', 'add-user-favorite');
	}

	$scope.addToFavorite = async function(favorite) {
		$scope.display.loading = true;
		var members = favorite.groups.concat(favorite.users);
		var alreadyIn = false;
		members.forEach(member => {
			if (member.id === $scope.currentUser.id) {
				alreadyIn = true;
				return;
			}
		});
		if (!alreadyIn) {
			members.push($scope.currentUser);
			await favorite.save(favorite.name, members, true);
		}
		$scope.display.loading = false;
		$scope.lightbox.show = false;
		template.close('lightbox');
		if (!alreadyIn) {
			$scope.$apply();
		}
	}

	$scope.confirmAddToFavorite = async function() {
		$scope.display.loading = true;
		var favorite = new directory.Favorite();
		await favorite.save($scope.create.favorite.userName, [$scope.currentUser], false);
		$scope.favorites.push(favorite);
		$scope.favorites.all.sort($scope.sortByName);
		$scope.display.loading = false;
		$scope.lightbox.show = false;
		template.close('lightbox');
		$scope.$apply();
	}

	$scope.searchUsersAndGroups = async function(favorite) {
		$scope.display.loadingFavoriteForm = true;
		await directory.favoriteForm.users.searchDirectory($scope.create.favorite.search, $scope.create.favorite.filters);
		await directory.favoriteForm.groups.searchDirectory($scope.create.favorite.search, $scope.create.favorite.filters);
		$scope.favoriteFormUsersGroups = directory.favoriteForm.groups.all.concat(directory.favoriteForm.users.all);
		$scope.display.loadingFavoriteForm = false;
		$scope.$apply('favoriteFormUsersGroups');
	};

	$scope.deselectUser = function(tpl){
		$scope.currentUser = undefined;
		template.close('details');
	};

	$scope.selectUser = function(user){
		$scope.display.searchmobile = false;

		if(!$scope.$$phase){
			$scope.$apply('search');
		}

		if($scope.currentUser !== undefined){
			ui.scrollToTop();
		}

		user.open();
		user.one('sync', function(){
			$scope.currentUser = user;
			$scope.$apply('currentUser');
		});

		template.open('details', 'user-infos');
	};

	$scope.deleteFavorite = function(favorite) {
		if (!$scope.display.creatingFavorite) {
			$scope.tryRemoveFavorite(favorite);
		}
	}

	$scope.tryRemoveFavorite = function(favorite) {
		$scope.currentDeletingFavorite = favorite;
		$scope.lightbox.show = true;
		template.open('lightbox', 'confirm-favorite-remove');
	}

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
		if (form)
			$scope.hideFavoriteForm();
		$scope.$apply();
	}

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
	}

	$scope.back = function() {
		$scope.currentUser = null;
		template.close('details');
	}

	$scope.backToSearch = function() {
		$scope.search.maxLength = 50;
		$scope.display.searchmobile = false;
		$scope.display.loadingmobile = false;
	}
	$scope.backToGroups = function() {
		$scope.currentGroup = null;
		if(!$scope.$$phase){		
			$scope.$apply('currentGroup');
		}
	}

	$scope.showGroupUsers = async function(group) {
		$scope.display.loading = true;
		if (ui.breakpoints.checkMaxWidth("wideScreen")) {
			$scope.display.loadingmobile = true;
		}
		await group.getUsers();
		$scope.currentGroup = group;
		template.open('dominosUser', 'dominos-user')
		template.open('groupActions', 'group-actions');
		$scope.display.loading = false;
		$scope.display.loadingmobile = false;
		$scope.$apply();
	}

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
		if (type) {
			if(type instanceof Array)
				return colorsMatch[type[0].toLowerCase()];
			return colorsMatch[type.toLowerCase()];
		}
		return "grey";
	};

	$scope.lightenColorFromType = function(type){
		return "lighten-" + this.colorFromType(type);
	};

	$scope.filterTopStructures = function(structure){
		return !structure.parents
	};

	$scope.displayFamily = function(currentUser) {
		return currentUser && currentUser.relatives.length && (model.me.type === 'ENSEIGNANT' || model.me.type === 'PERSEDUCNAT');
	};

	$scope.onCloseSearchModule = function() {
		$scope.display.searchmobile = true;
	};

	$scope.getCurrentItems = function() {
		switch($scope.search.index) {
			case 0:
				return $scope.users;
			case 1:
				return $scope.groups;
			case 2:
				return $scope.users; // TODO
		}
	};

	$scope.indexFormChanged = function(index) {
		if ($scope.display.creatingFavorite) {
			template.close('list');
			if (index === 2)
				template.open('list', 'favorite-form');
			else
				template.open('list', 'dominos');
		}
		$scope.currentGroup = null;
		$scope.search.maxLength = 50;
		$scope.back();
	}

	$scope.canFavoriteFormInitSearch = function() {
		return $scope.create.favorite.search || $scope.create.favorite.filters.structures || $scope.create.favorite.filters.classes || 
				$scope.create.favorite.filters.profles || $scope.create.favorite.filters.functions || 
				$scope.create.favorite.filters.types || $scope.favoriteFormUsersGroups.length > 0;
	}

	$scope.favoriteFormInitSearch = function() {
		$scope.create.favorite.search = '';
		$scope.create.favorite.filters.structures = null;
		$scope.create.favorite.filters.classes = null;
		$scope.create.favorite.filters.profles = null;
		$scope.create.favorite.filters.functions = null;
		$scope.create.favorite.filters.types = null;
		$scope.checkOption($scope.create.favorite.options.structures, true);
		$scope.checkOption($scope.create.favorite.options.classes, true);
		$scope.checkOption($scope.create.favorite.options.profiles, true);
		$scope.checkOption($scope.create.favorite.options.functions, true);
		$scope.checkOption($scope.create.favorite.options.types, true);
	}

	$scope.checkOption = function(array, check) {
		array.forEach(element => {
			element.checked = check;
		});
		$scope.favoriteFormUsersGroups = [];
	}

	$scope.selectFirstFavorite = async function(noupdate) {
		if($scope.favorites.empty()) {
			$scope.currentFavorite = null;
		}
		else {
			await $scope.selectFavorite($scope.favorites.first(), noupdate);
		}
	}

	$scope.sortByName = function(a, b) {
		return a.name > b.name;
	}
}]);