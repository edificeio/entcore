// Copyright. Tous droits réservés. WebServices pour l’Education.

function LoginController($scope, template){
	$scope.template = template;
	$scope.template.open('main', 'login-form');
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
		console.log('connect');
		// picking up values manually because the browser autofill isn't registered by angular
		http().post('/auth/login', http().serialize({
			email: $('#email').val(),
			password: $('#password').val(),
			callBack: $scope.callBack
		}))
			.done(function(data){
				if(typeof data !== 'object'){
					if(window.location.href.indexOf('callback=') !== -1){
						window.location.href = unescape(window.location.href.split('callback=')[1]);
					}
					else{
						window.location.href = $scope.callBack;
					}
				}
				if(data.error){
					$scope.error = data.error.message;
				}
				$scope.$apply('error');
			});
	};
}

function ForgotController($scope, template){
	$scope.template = template;
	$scope.template.open('main', 'forgot-form');
	$scope.user = {};

	if(window.location.href.indexOf('?') !== -1){
		if(window.location.href.split('login=').length > 1){
			$scope.login = window.location.href.split('login=')[1].split('&')[0];
		}
		if(window.location.href.split('activationCode=').length > 1){
			$scope.activationCode = window.location.href.split('activationCode=')[1].split('&')[0];
		}
	}

	$scope.forgot = function(){
		http().post('/auth/forgot', http().serialize({
			login: $scope.user.login
		}))
			.done(function(data){
				if(data.message){
					template.open('main', 'forgot-message')
					$scope.message = data.message;
					$scope.$apply('message');
				}
				else{
					$scope.error = data.error.message;
					$scope.$apply('error');
				}
			});
	};
}

function ActivationController($scope, template){
	$scope.template = template;
	$scope.template.open('main', 'activation-form');
	$scope.user = {};

	if(window.location.href.indexOf('?') !== -1){
		if(window.location.href.split('login=').length > 1){
			$scope.user.login = window.location.href.split('login=')[1].split('&')[0];
		}
		if(window.location.href.split('activationCode=').length > 1){
			$scope.user.activationCode = window.location.href.split('activationCode=')[1].split('&')[0];
		}
	}

	http().get('/auth/context').done(function(data){
		$scope.callBack = data.callBack;
		$scope.cgu = data.cgu;
		$scope.$apply('cgu');
	});

	$scope.activate = function(){
		http().post('/auth/activation', http().serialize({
			login: $scope.user.login,
			password: $scope.user.password,
			confirmPassword: $scope.user.confirmPassword,
			acceptCGU: $scope.user.acceptCGU,
			activationCode: $scope.user.activationCode,
			callBack: $scope.callBack
		}))
			.done(function(data){
				if(typeof data !== 'object'){
					window.location.href = '/';
				}
				if(data.error){
					$scope.error = data.error.message;
				}

				$scope.$apply('error');
			});
	};
}

function ResetController($scope, template){
	$scope.template = template;
	$scope.template.open('main', 'reset-form');
	$scope.user = {};

	if(window.location.href.indexOf('?') !== -1){
		if(window.location.href.split('login=').length > 1){
			$scope.login = window.location.href.split('login=')[1].split('&')[0];
		}
		if(window.location.href.split('activationCode=').length > 1){
			$scope.activationCode = window.location.href.split('activationCode=')[1].split('&')[0];
		}
	}

	$scope.reset = function(){
		http().post('/auth/reset', http().serialize({
			login: $scope.user.login,
			password: $scope.user.password,
			confirmPassword: $scope.user.confirmPassword,
			resetCode: resetCode
		}))
		.done(function(data){
			if(typeof data !== 'object'){
				window.location.href = '/';
			}
			if(data.error){
				$scope.error = data.error.message;
			}

			$scope.$apply('error');
		});
	};
}

function CGUController($scope, template){
	$scope.template = template;
	$scope.template.open('main', 'cgu-content');
}