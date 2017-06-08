// Copyright. Tous droits réservés. WebServices pour l’Education.

routes.define(function($routeProvider) {
	$routeProvider
		.when('/id', {
			action: 'actionId'
		})
		.when('/password', {
	  		action: 'actionPassword'
		})
		.otherwise({
		  	redirectTo: '/'
		})
});

function LoginController($scope, template){
	$scope.template = template;
	$scope.template.open('main', 'login-form');
	$scope.user = {};
	$scope.lang = lang;


	$scope.welcome = {

	};

	http().get('/auth/configure/welcome').done(function (d) {
	    $scope.welcome.content = d.welcomeMessage;
		if(!d.enabled){
			$scope.welcome.hideContent = true;
		}
	    $scope.$apply();
	})
    .e404(function () {
        $scope.welcome.hideContent = true;
        $scope.$apply();
    });

	$scope.cookieEnabled = navigator.cookieEnabled;

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
		if(window.location.href.split('callback=').length > 1){
			$scope.callBack = window.location.href.split('callback=')[1].split('&')[0];
		}
	}

	http().get('/auth/context').done(function(data){
		//$scope.callBack = data.callBack;
		$scope.cgu = data.cgu;
		$scope.$apply('cgu');
	});

	$scope.connect = function(){
		console.log('connect');
		// picking up values manually because the browser autofill isn't registered by angular
		http().post('/auth/login', http().serialize({
			email: $('#email').val(),
			password: $('#password').val(),
			rememberMe: $scope.user.rememberMe,
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

}

function ForgotController($scope, route, template){
	$scope.template = template;
	$scope.template.open('main', 'forgot-form');
	$scope.user = {};

	$scope.welcome = {

	};

	http().get('/auth/configure/welcome').done(function (d) {
	    $scope.welcome.content = d.welcomeMessage;
	    if (!d.enabled) {
	        $scope.welcome.hideContent = true;
	    }
	    $scope.$apply();
	})
    .e404(function () {
        $scope.welcome.hideContent = true;
        $scope.$apply();
    });

	if(window.location.href.indexOf('?') !== -1){
		if(window.location.href.split('login=').length > 1){
			$scope.login = window.location.href.split('login=')[1].split('&')[0];
		}
		if(window.location.href.split('activationCode=').length > 1){
			$scope.activationCode = window.location.href.split('activationCode=')[1].split('&')[0];
		}
	}

	route({
		actionId: function(params){
			$scope.user.mode = "id"
		},
		actionPassword: function(params){
			$scope.user.mode = "password"
		}
	})

	$scope.initUser = function(){
		$scope.user = {}
	}

	$scope.forgot = function(){
		if($scope.user.mode === 'password'){
			$scope.forgotPassword($scope.user.login, 'mail')
		} else {
			$scope.forgotId($scope.user.mail, 'mail')
		}
	};

	$scope.passwordChannels = function(login){
		http().get('/auth/password-channels', {login: login})
			.done(function(data){
				$scope.user.channels = {
					mail: data.mail,
					mobile: data.mobile
				}
				$scope.$apply()
			})
			.e400(function(data){
				$scope.error = 'auth.notify.' + JSON.parse(data.responseText).error + '.login'
				$scope.$apply()
			})
	}

	$scope.forgotPassword = function(login, service){
		http().postJson('/auth/forgot-password', {login: login, service: service})
			.done(function(data){
				notify.info("auth.notify."+service+".sent")
				$scope.user.channels = {}
				$scope.$apply()
			})
			.e400(function(data){
				$scope.error = 'auth.notify.' + JSON.parse(data.responseText).error + '.login'
				$scope.$apply()
			})
	}

	$scope.forgotId = function(mail, service){
		http().postJson('/auth/forgot-id', {mail: mail, service: service})
			.done(function(data){
				notify.info("auth.notify."+service+".sent")
				if(data.mobile){
					$scope.user.channels = {
						mobile: data.mobile
					}
				} else {
					$scope.user.channels = {}
				}
				$scope.$apply()
			})
			.e400(function(data){
				$scope.error = 'auth.notify.' + JSON.parse(data.responseText).error + '.mail'
				$scope.$apply()
			})
	}
}

function ActivationController($scope, template){
	$scope.template = template;
	$scope.lang = lang;
	$scope.template.open('main', 'activation-form');
	$scope.user = {};
	$scope.phonePattern = new RegExp("^(00|\\+)?(?:[0-9] ?-?\\.?){6,15}$");

	$scope.welcome = {

	};

	http().get('/auth/configure/welcome').done(function (d) {
	    $scope.welcome.content = d.welcomeMessage;
	    if (!d.enabled) {
	        $scope.welcome.hideContent = true;
	    }
	    $scope.$apply();
	})
    .e404(function () {
        $scope.welcome.hideContent = true;
        $scope.$apply();
    });

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
		$scope.passwordRegex = data.passwordRegex;
		$scope.mandatory = data.mandatory;
		$scope.$apply('cgu');
	});

	$scope.identicalRegex = function(str){
		if(!str)
			return new RegExp("^$")
		return new RegExp("^"+str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")+"$")
	}

	$scope.refreshInput = function(form, inputName){
		form[inputName].$setViewValue(form[inputName].$viewValue)
	}

	$scope.passwordComplexity = function(password){
		if(!password)
			return 0

		if(password.length < 6)
			return password.length

		var score = password.length
		if(/[0-9]+/.test(password) && /[a-zA-Z]+/.test(password)){
			score += 5
		}
		if(!/^[a-zA-Z0-9- ]+$/.test(password)) {
			score += 5
		}

		return score
	}

	$scope.translateComplexity = function(password){
		var score = $scope.passwordComplexity(password)
		if(score < 12){
			return lang.translate("weak")
		}
		if(score < 20)
			return lang.translate("moderate")
		return lang.translate("strong")
	}

	$scope.activate = function(){
		var emptyIfUndefined = function(item){
			return item ? item : ""
		}

		http().post('/auth/activation', http().serialize({
			login: $scope.user.login,
			password: $scope.user.password,
			confirmPassword: $scope.user.confirmPassword,
			acceptCGU: $scope.user.acceptCGU,
			activationCode: $scope.user.activationCode,
			callBack: $scope.callBack,
			mail: emptyIfUndefined($scope.user.email),
			phone: emptyIfUndefined($scope.user.phone)
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
	$scope.lang = lang;
	$scope.template.open('main', 'reset-form');
	$scope.user = {};

	$scope.welcome = {

	};

	http().get('/auth/configure/welcome').done(function (d) {
	    $scope.welcome.content = d.welcomeMessage;
	    if (!d.enabled) {
	        $scope.welcome.hideContent = true;
	    }
	    $scope.$apply();
	})
    .e404(function () {
        $scope.welcome.hideContent = true;
        $scope.$apply();
    });

	if(window.location.href.indexOf('?') !== -1){
		if(window.location.href.split('login=').length > 1){
			$scope.login = window.location.href.split('login=')[1].split('&')[0];
		}
		if(window.location.href.split('activationCode=').length > 1){
			$scope.activationCode = window.location.href.split('activationCode=')[1].split('&')[0];
		}
	}

	http().get('/auth/context').done(function(data){
		$scope.passwordRegex = data.passwordRegex;
	});

	$scope.identicalRegex = function(str){
		if(!str)
			return new RegExp("^$")
		return new RegExp("^"+str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")+"$")
	}

	$scope.refreshInput = function(form, inputName){
		form[inputName].$setViewValue(form[inputName].$viewValue)
	}

	$scope.passwordComplexity = function(password){
		if(!password)
			return 0

		if(password.length < 6)
			return password.length

		var score = password.length
		if(/[0-9]+/.test(password) && /[a-zA-Z]+/.test(password)){
			score += 5
		}
		if(!/^[a-zA-Z0-9- ]+$/.test(password)) {
			score += 5
		}

		return score
	}

	$scope.translateComplexity = function(password){
		var score = $scope.passwordComplexity(password)
		if(score < 12){
			return lang.translate("weak")
		}
		if(score < 20)
			return lang.translate("moderate")
		return lang.translate("strong")
	}

	$scope.reset = function(){
		http().post('/auth/reset', http().serialize({
			login: $scope.user.login.trim(),
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
