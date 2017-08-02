import { ng, template, idiom as lang, http } from 'entcore';

export let activationController = ng.controller('ActivationController', ['$scope', ($scope) =>{
	$scope.template = template;
	$scope.lang = lang;
	$scope.user = { themes: {} };
	$scope.phonePattern = new RegExp("^(00|\\+)?(?:[0-9] ?-?\\.?){6,14}[0-9]$");

	$scope.welcome = {};
	template.open('main', 'activation-form');

	let conf = { overriding: [] };
	const xhr = new XMLHttpRequest();
	xhr.open('get', '/assets/theme-conf.js');
	xhr.onload = () => {
		eval(xhr.responseText.split('exports.')[1]);
		$scope.themes = conf.overriding;
	};
	xhr.send();

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
	};

	$scope.noThemePicked = () => !Object.keys($scope.user.themes).length;

	$scope.activate = function(){
		if($scope.themes.length > 1 && $scope.noThemePicked()){
			template.open('main', 'activation-themes');
			return;
		}

		if(Object.keys($scope.user.themes).length > 1){
			conf.overriding.forEach(o => {
				if(o.parent === 'theme-open-ent' && $scope.user.themes[o.child]){
					$scope.user.theme = o.child;
				}
			});
		}
		else if(Object.keys($scope.user.themes).length > 0){
			$scope.user.theme = Object.keys($scope.user.themes)[0];
		}

		var emptyIfUndefined = function(item){
			return item ? item : ""
		}

		http().post('/auth/activation', http().serialize({
			theme: $scope.user.theme || '',
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
}]);

function CGUController($scope, template){
	$scope.template = template;
	$scope.template.open('main', 'cgu-content');
}