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
function AppRegistry($scope, $sce, model){
	$scope.lang = lang
	$scope.SCROLL_INCREMENT = 100
	$scope.groupsLimit = $scope.SCROLL_INCREMENT
	$scope.incrementGroupsLimit = function(){
		$scope.groupsLimit += $scope.SCROLL_INCREMENT
	}

	var previewPath = '';
	$scope.display = {
		advanced: false
	};
	$scope.application = new Application({ name: 'Application', displayName: 'application', external: true });

	$scope.showAdvanced = function(){
		$scope.display.advanced = true;
	};

	$scope.hideAdvanced = function(){
		$scope.display.advanced = false;
	};

	$scope.applications = model.applications;
	model.on('applications.change', function(){
		if(!$scope.$$phase){
			$scope.$apply('applications');
		}
	});

	$scope.roles = model.roles;
	model.on('roles.change', function(){
		if(!$scope.$$phase){
			$scope.$apply('roles');
		}
	});

	$scope.schools = model.schools;

	$scope.viewApplication = function(application){
		$scope.role = undefined;
		$scope.application = application;
		$scope.updatePath();
		$scope.application.open();
		$scope.application.on('change', function(){
			$scope.updatePath();
			$scope.$apply('application');
		});
	};

	$scope.updatePath = function(){
		var path = $scope.application.address;
		if($scope.application.target === 'adapter'){
			path = '/adapter#' + path;
		}
		previewPath = $sce.trustAsResourceUrl('/appregistry/app-preview?displayName=' + lang.translate($scope.application.displayName) + '&icon=' + $scope.application.icon + '&target=' + $scope.application.target + '&path=' + path);
		if(!$scope.$$phase){
			$scope.$apply('application');
		}
	};
	$scope.updatePath();

	$scope.previewPath = function(){
		return previewPath;
	};

	$scope.refreshPreview = function(){
		$('#previewFrame').attr('src', $('#previewFrame').attr('src')+'')
	}

	$scope.newApplication = function(){
		$scope.application = new Application({ name: 'Application', displayName: 'application', external: true });
		$scope.updatePath();
		$scope.application.on('change', function(){
			$scope.updatePath();
			if(!$scope.$$phase){
				$scope.$apply('application');
			}
		});
	};

	$scope.deleteApplication = function(){
		$scope.application.delete()
		delete $scope.application
	}

	$scope.setUserinfoScope = function(){
		if((!$scope.application.scope || $scope.application.scope.indexOf('userinfo') === -1)  && $scope.application.transferSession){
			$scope.application.scope = 'userinfo' + ($scope.application.scope || '');
		}
		if($scope.application.scope && $scope.application.scope.indexOf('userinfo') !== -1 && !$scope.application.transferSession){
			$scope.application.scope = $scope.application.scope.replace('userinfo', '');
		}
	};

	$scope.newRole = function(placeholder_name){
		$scope.role = new Role()
		$scope.role.appRoles = []
		$scope.role.name = placeholder_name ? placeholder_name : ""
	};

	$scope.createRole = function(prefix){
		$scope.role.name = prefix ? prefix + " - " + $scope.role.name : $scope.role.name
		$scope.role.save(function(){
			$scope.roles.sync(function(){
				$scope.crossRoles = $scope.flagCrossRoles()
			})
		})
		$scope.role = undefined
	}

	$scope.deleteRole = function(role){
		//Deletion process
		var deletion = function(){
			role.delete(function(){
				$scope.roles.sync(function(){
					$scope.crossRoles = $scope.flagCrossRoles()
				})
			})
			$scope.role = undefined
		}

		//If we are trying to delete an application role
		if(!_.contains($scope.crossRoles, role)){
			//1st step, deleting the application role from every cross role containing it and storing the cross role in a stack.
			var crossRoleStack = []
			$scope.crossRoles.forEach(function(crossRole){
				var index = crossRole.appRoles.indexOf(role)
				if(index >= 0){
					crossRole.appRoles.splice(index, 1)
					crossRoleStack.push(crossRole)
				}
			})
			//2nd step, saving every cross role we modified before (i.e. contained in the stack).
			//As the calls are asynchronous, we need to be sure that the deletion of the app role takes place only after the final save.
			var launcher = {
				count: crossRoleStack.length + 1,
				decrement: function(){ if(--this.count === 0){ this.launch() } },
				launch: deletion
			}
			launcher.decrement()
			_.forEach(crossRoleStack, function(crossRole){
				crossRole.saveCross(function(){ launcher.decrement() })
			})
		} else {
			deletion()
		}
	}

	$scope.saveCrossRole = function(role){
		role.saveCross(function(){
			$scope.roles.sync(function(){
				$scope.crossRoles = $scope.flagCrossRoles()
				$scope.role = $scope.roles.findWhere({name: role.name})
			})
		})
	}

	$scope.hideRoleCreationPanel = function(){
		$scope.role = undefined
	}

	$scope.selectRole = function(role){
		role.selected = !role.selected;
	};

	$scope.viewRole = function(role){
		$scope.role = role;
	}

	$scope.selectAllActions = function(role){
		role = role ? role : $scope.role
		role.actions.all = []
		$scope.application.actions.forEach(function(action){ role.actions.push(action) })
	}

	$scope.deselectAllActions = function(role){
		role = role ? role : $scope.role
		role.actions.all = []
	}

	$scope.crossRoleContains = function(approle){
		return _.contains($scope.role.appRoles, approle)
	}

	$scope.groupContains = function(group, role){
		return _.find(group.roles, function(role_id){
			return role_id === role.id
		})
	}

	$scope.switchGroupRole = function(group, role){
		if($scope.groupContains(group, role)){
			group.roles = _.reject(group.roles, function(r){ return r === role.id })
		} else {
			group.roles.push(role.id)
		}
		group.link()
	}

	$scope.selectAllGroupRoles = function(group){
		var roles = []
		_.forEach($scope.applications.all, function(application){
			roles = _.union(roles, $scope.roles.applicationRolesExclusive(application))
		})
		_.forEach(roles, function(role){
			if(!$scope.groupContains(group, role)){
				group.roles.push(role.id)
			}
		})
		group.link()
	}

	$scope.deselectAllGroupRoles = function(group){
		var roles = []
		_.forEach($scope.applications.all, function(application){
			roles = _.union(roles, $scope.roles.applicationRolesExclusive(application))
		})
		_.forEach(roles, function(role){
			group.roles = _.reject(group.roles, function(r){ return r === role.id })
		})
		group.link()
	}

	$scope.flagCrossRoles = function(){
		return $scope.roles.crossAppRoles($scope.applications).map(function(role){
			role.appRoles = []
			$scope.applications.forEach(function(app){
				var approles = $scope.roles.applicationRolesExclusive(app)
				_.forEach(approles, function(approle){
					if(role.crossRoleContains(approle)){
						role.appRoles.push(approle)
					}
				})
			})
			return role
		})
	}

	$scope.roleMode = 0

}
