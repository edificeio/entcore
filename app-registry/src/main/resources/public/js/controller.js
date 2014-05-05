//Copyright. Tous droits réservés. WebServices pour l’Education.
function AppRegistry($scope, $sce, model){
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
		$scope.application = application;
		$scope.updatePath();
		$scope.application.on('change', function(){
			$scope.updatePath();
		});
	};

	$scope.updatePath = function(){
		previewPath = $sce.trustAsResourceUrl('/appregistry/app-preview?displayName=' + lang.translate($scope.application.displayName) + '&icon=' + $scope.application.icon
			+ '&target=' + $scope.application.target + '&path=' + $scope.application.url);
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

	$scope.selectRole = function(role){
		role.selected = !role.selected;
	}
}