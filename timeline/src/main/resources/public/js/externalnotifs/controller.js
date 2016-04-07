function MainController($rootScope, $scope, template, lang, model){

	template.open('notifspanel', 'notifspanel');

	$scope.template = template;

    $scope.appli = model.applis;
}

//(ng-repeat > applis.all)
