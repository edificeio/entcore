function AppRegistry($scope, model){
	$scope.display = {
		advanced: false
	}

	$scope.showAdvanced = function(){
		$scope.display.advanced = true;
	};

	$scope.hideAdvanced = function(){
		$scope.display.advanced = false;
	};

	$scope.applications = model.applications;
	model.on('applications.change', function(){
		if(!$scope.application){
			$scope.application = model.applications.first();
		}
		$scope.$apply('applications');
	});

	$scope.viewApplication = function(application){
		$scope.application = application;
	};
}