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
		.when('/edit-user/:id', {
			action: 'editUser'
		})
		.when('/edit-user-infos/:id', {
			action: 'editUserInfos'
		})
		.when('/edit-me', {
			action: 'editMe'
		})
		.otherwise({
			redirectTo: 'edit-me'
		});
});

function MyAccount($scope, lang, date, notify, route){
	route({
		editUserInfos: function(params){
			model.account = new User({ id: params.id, edit: { infos: true } });
			init();
			$scope.openView('user-edit', 'user');
		},
		editUser: function(params){
			model.account = new User({ id: params.id, edit: { userbook: true, infos: true } });
			init();
			$scope.openView('user-edit', 'user');
			$scope.openView('userbook-edit', 'userbook');
		},
		editMe: function(params){
			model.account = new User({ id: model.me.userId, edit: { userbook: true, visibility: true } });
			if(model.me.type === 'ENSEIGNANT'){
				model.account.edit.infos = true;
				$scope.openView('user-edit', 'user');
			}
			else{
				$scope.openView('user-view', 'user');
			}
			init();

			$scope.openView('userbook-edit', 'userbook');
		}
	});

	function init(){
		$scope.me = model.me;
		model.account.on('change', function(){
			$scope.$apply();
		});

		model.account.load();
		$scope.account = model.account;
	}

	$scope.display = {};

	$scope.viewsContainers = {};
	$scope.openView = function(view, name){
		var viewsPath = '/directory/public/template/';
		$scope.viewsContainers[name] = viewsPath + view + '.html';
	};

	$scope.containsView = function(name, view){
		var viewsPath = '/directory/public/template/';
		return $scope.viewsContainers[name] === viewsPath + view + '.html';
	};

	$scope.moods = User.prototype.moods;

	$scope.availableMoods = _.reject($scope.moods, function(mood){
		return mood.id === 'default';
	});

	$scope.resetPasswordPath = '/auth/reset/password';

	$scope.birthDate = function(birthDate){
		if(birthDate){
			return date.format(birthDate, 'D MMMM YYYY');
		}
		return '';
	};

	$scope.translate = function(label){
		return lang.translate(label);
	};

	$scope.saveChanges = function(){
		model.account.saveChanges();
	};

	$scope.openPasswordDialog = function(){
		ui.showLightbox();
		$scope.account.password = '';
	};

	$scope.closePassword = function(){
		ui.hideLightbox();
	};

	$scope.saveInfos = function(){
		model.account.saveInfos();
	};

	$scope.resetPassword = function(url){
		http().post(url, {
				oldPassword: $scope.account.oldPassword,
				password: $scope.account.password,
				confirmPassword: $scope.account.password,
				login: $scope.account.login,
				callback: '/userbook/mon-compte'
			})
			.done(function(response){
				if(response.error){
					notify.error('Le formulaire contient des erreurs');
				}
				else{
					$scope.resetErrors = false;
					ui.hideLightbox();
				}
				$scope.$apply();
			})
	};

	$scope.saveUserbookProperty = function(prop){
		model.account.saveUserbookProperty(prop);
	};

	$scope.changeVisibility = function(hobby){
		if(hobby.visibility.toLowerCase() === 'public'){
			hobby.visibility = 'PRIVE';
		}
		else{
			hobby.visibility = 'PUBLIC'
		}

		http().get('api/set-visibility', { value: hobby.visibility, category: hobby.category });
	};

	$scope.changeInfosVisibility = function(info, state){
        if(state.toLowerCase() === 'public'){
            $scope.account.visible[info] = 'prive';
        }
        else{
            $scope.account.visible[info] = 'public';
        }
		http().get('api/edit-user-info-visibility', { info: info, state: $scope.account.visible[info] });
	};

	$scope.resetAvatar = function(){
		model.account.picture = '';
		model.account.saveChanges();
	};

	$scope.updateAvatar = function(){
		model.account.uploadAvatar();
	}

	$scope.longDate = function(dateString){
		return moment(dateString).format('DD MMMM YYYY')
	};
}
