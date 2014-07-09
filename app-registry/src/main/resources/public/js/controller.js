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
	$scope.lang = lang;

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
		previewPath = $sce.trustAsResourceUrl('/appregistry/app-preview?displayName=' + lang.translate($scope.application.displayName) + '&icon=' + $scope.application.icon
			+ '&target=' + $scope.application.target + '&path=' + path);
		if(!$scope.$$phase){
			$scope.$apply('application');
		}
	};
	$scope.updatePath();

	$scope.previewPath = function(){
		return previewPath;
	};

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

	$scope.newRole = function(){
		$scope.role = new Role();
	};

	$scope.createRole = function(){
		$scope.role.save();
		$scope.role = undefined;
	};

	$scope.selectRole = function(role){
		role.selected = !role.selected;
	};

	$scope.setUserinfoScope = function(){
		if((!$scope.application.scope || $scope.application.scope.indexOf('userinfo') === -1)  && $scope.application.transferSession){
			$scope.application.scope = 'userinfo' + ($scope.application.scope || '');
		}
		if($scope.application.scope && $scope.application.scope.indexOf('userinfo') !== -1 && !$scope.application.transferSession){
			$scope.application.scope = $scope.application.scope.replace('userinfo', '');
		}
	};
}