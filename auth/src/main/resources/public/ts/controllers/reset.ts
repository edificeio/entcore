import { ng, http, template, idiom as lang } from 'entcore';

declare let resetCode: string;

export let resetController = ng.controller('ResetController', ['$scope', ($scope) => {
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
}]);