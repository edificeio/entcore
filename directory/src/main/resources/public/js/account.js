routes.define(function($routeProvider){
	$routeProvider
		.when('/edit-user/:id', {
			action: 'editUser'
		})
		.when('/edit-user-infos/:id', {
			action: 'editUserInfos'
		})
		.otherwise({
			action: 'editMe'
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
	};

	$scope.closePassword = function(){
		ui.hideLightbox();
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
				if(response.indexOf('html') === -1){
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
	}

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
		//if we're editing someone else's profile, we're unlikely to be the owner
		//of the photo ; therefore we need to upload a new one
		if(!model.account.picture || model.me.userId !== model.account.id){
			model.account.uploadAvatar();
		}
		else{
			model.account.updateAvatar();
		}
	}
}
