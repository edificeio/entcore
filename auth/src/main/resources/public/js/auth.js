// Copyright. Tous droits réservés. WebServices pour l’Education.

function LoginController($scope, template){
	$scope.template = template;
	$scope.template.open('main', 'login-form');
	$scope.user = {};

	var browser = function(userAgent){
		var version;
		if(userAgent.indexOf('Chrome') !== -1){
			version = parseInt(userAgent.split('Chrome/')[1].split('.')[0]);
			return {
				browser: 'Chrome',
				version: version,
				outdated: version < 39
			}
		}
		else if(userAgent.indexOf('AppleWebKit') !== -1 && userAgent.indexOf('Chrome') === -1){
			version = parseInt(userAgent.split('Version/')[1].split('.')[0]);
			return {
				browser: 'Safari',
				version: version,
				outdated: version < 7
			}
		}
		else if(userAgent.indexOf('Firefox') !== -1){
			version = parseInt(userAgent.split('Firefox/')[1].split('.')[0]);
			return {
				browser: 'Firefox',
				version: version,
				outdated: version < 34
			}
		}
		else if(userAgent.indexOf('MSIE') !== -1){
			version = parseInt(userAgent.split('MSIE ')[1].split(';')[0]);
			return {
				browser: 'MSIE',
				version: version,
				outdated: version < 10
			}
		}
		else if(userAgent.indexOf('MSIE') === -1 && userAgent.indexOf('Trident') !== -1){
			version = parseInt(userAgent.split('rv:')[1].split('.')[0]);
			return {
				browser: 'MSIE',
				version: version,
				outdated: version < 10
			}
		}
	};

	$scope.browser = browser(navigator.userAgent);

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

	for(var i = 0; i < 10; i++){
		if(history.pushState){
			history.pushState('', {});
		}
	}

	$scope.skins = [];
		http().get('/skins').done(function(data){
		for(var i=0; i<data.skins.length; i++) {
			$scope.skins.push({'id':data.skins[i], 'label':data.skins[i]});
		}
	});

	$scope.switchSkin = function(){
		http().putJson('/skin', {'skin': this.selectedSkin})
			.done(function(data){
			window.location = window.location;
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