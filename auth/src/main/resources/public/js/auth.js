// Copyright. Tous droits réservés. WebServices pour l’Education.

function AuthController($scope, template){
	$scope.template = template;
	$scope.user = {};

	if(window.location.href.indexOf('?') !== -1){
		if(window.location.href.split('login=').length > 1){
			$scope.login = window.location.href.split('login=')[1].split('&')[0];
		}
		if(window.location.href.split('activationCode=').length > 1){
			$scope.activationCode = window.location.href.split('activationCode=')[1].split('&')[0];
		}
	}

	http().get('/auth/context').done(function(data){
		$scope.callBack = data.callBack;
		$scope.cgu = data.cgu;
		$scope.$apply('cgu');
	});

	$scope.connect = function(){
		http().post('/auth/login', http().serialize({
			email: $scope.user.email,
			password: $scope.user.password,
			callBack: $scope.callBack
		}))
		.done(function(data){
			if(typeof data !== 'object'){
				window.location.href = $scope.callBack;
			}
			$scope.error = data.error.message;
			$scope.$apply('error');
		});
	};

	$scope.activate = function(){
		http().post('/auth/activation', http().serialize({
			login: $scope.user.login,
			password: $scope.user.password,
			confirmPassword: $scope.user.confirmPassword,
			acceptCGU: $scope.user.acceptCGU,
			activationCode: $scope.activationCode,
			callBack: $scope.callBack
		}))
			.done(function(data){
				if(typeof data !== 'object'){
					window.location.href = '/';
				}
				$scope.error = data.error.message;
				$scope.$apply('error');
			});
	}
}