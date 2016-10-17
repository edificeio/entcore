import { ng, template, idiom as lang, http } from 'entcore';
import { $ } from 'entcore/libs/jquery/jquery';

export let loginController = ng.controller('LoginController', ['$scope', ($scope) => {
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
						window.location.href = (window as any).unescape(window.location.href.split('callback=')[1]);
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
			history.pushState('', '');
		}
	}

}]);