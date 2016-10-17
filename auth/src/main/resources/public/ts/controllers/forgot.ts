import { http, ng, template, notify } from 'entcore';

export let forgotController = ng.controller('ForgotController', ['$scope', 'route', ($scope, route) => {
	$scope.template = template;
	$scope.template.open('main', 'forgot-form');
	$scope.user = {};

	$scope.welcome = {};

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
}]);