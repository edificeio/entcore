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
			$scope.currentFavoriteCreation = [];
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
			$scope.favoriteFormUsersGroups = [];
			
			if(!$scope.favorites.empty()) {
				$scope.selectFavorite($scope.favorites.first());
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
					title: '',
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
		
		// Favorite
		if ($scope.search.index == 2) {
			$scope.createFavorite();
			return;
		}

		// Loading activation
		$scope.display.loading = true;
		if (ui.breakpoints.checkMaxWidth("tablette")) { 
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
		if (ui.breakpoints.checkMaxWidth("tablette")) {
			if (directory.directory.users.all.length === 0)
				notify.info("noresult");
			else
				$scope.display.searchmobile = true;
		} 
		$scope.$apply();
	};

	$scope.createFavorite = async function() {
		$scope.display.creatingFavorite = true;
		$('removable-list *').off();
		template.close('list');
		template.open('list', 'favorite-form');
	};

	$scope.selectFavorite = async function(favorite) {
		$scope.display.loading = true;
		await favorite.getUsersAndGroups();
		$scope.currentFavorite = favorite;
		template.open('dominosUser', 'dominos-user')
		template.open('dominosGroup', 'dominos-group')		
		$scope.loading = false;
		$scope.$apply();
	};

	$scope.deleteFavorite = async function(favorite) {
		console.log("delete");
	};

	$scope.cancelFavorite = async function(favorite) {
		$scope.display.creatingFavorite = false;
		template.close('list');
		template.open('list', 'dominos');
	};

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

	$scope.back = function() {
		$scope.currentUser = undefined;
		template.close('details');
	}

	$scope.backToSearch = function() {
		$scope.display.searchmobile = false;$scope.display.loadingmobile = false;
	}
	$scope.backToGroups = function() {
		$scope.currentGroup = null;
		if(!$scope.$$phase){		
			$scope.$apply('currentGroup');
		}
	}

	$scope.showGroupUsers = async function(group) {
		$scope.loading = true;
		await group.getUsers();
		$scope.currentGroup = group;
		template.open('dominosUser', 'dominos-user')
		template.open('groupActions', 'group-actions');
		$scope.loading = false;
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
		return currentUser.relatives.length && (model.me.type === 'ENSEIGNANT' || model.me.type === 'PERSEDUCNAT');
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
		$scope.back();
	}
}]);