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

import { template, idiom as lang, ng, ui, model, moment, $ } from 'entcore';
import { directory } from '../model';

export const directoryController = ng.controller('DirectoryController',['$scope', 'route', ($scope, route) => {
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
			$scope.currentUser = new directory.User({ id: params.userId });
			$scope.currentUser.open();
			$scope.users = directory.directory.users;
			template.open('page', 'profile');
			template.open('details', 'user-infos');
			$scope.title = 'profile';
		},
		directory: function(){
			$scope.classrooms = [];
			$scope.currentSchool = undefined;
			directory.directory.users.all = [];
			directory.network.schools.all = [];
			$scope.users = directory.directory.users;
			$scope.schools = directory.network.schools;
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

	$scope.searchDirectory = function(){
		$scope.display.searchmobile = false;

		directory.directory.users.all = [];
		directory.directory.users.searchDirectory($scope.search.field, $scope.filters);
		directory.directory.users.one('change', function(){
			$scope.users = directory.directory.users;
			$scope.$apply('users');
		});

		template.open('main', 'mono-class');
		template.open('list', 'dominos');
		if (ui.breakpoints.checkMaxWidth("tablette") && $scope.currentUser) {
			$scope.display.searchmobile = true;
		}
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
}]);