import { ng, template, idiom as lang, http, $ } from 'entcore';

export let loginController = ng.controller('LoginController', ['$scope', ($scope) => {
	$scope.template = template;
	$scope.template.open('main', 'login-form');
	$scope.user = {};
	$scope.lang = lang;


	$scope.welcome = {

	};

	http().get('/auth/configure/welcome').done(function (d) {
		$scope.welcome.content = d.welcomeMessage;
		if (!d.enabled) {
			$scope.welcome.hideContent = true;
		}
		if (!$scope.$$phase) {
			$scope.$apply();
		}
	})
		.e404(function () {
			$scope.welcome.hideContent = true;
			if (!$scope.$$phase) {
				$scope.$apply();
			}
		});

	$scope.cookieEnabled = navigator.cookieEnabled;
	const safeSplit = (str: string = "", pattern: string = "") => {
		if (typeof str == "string") {
			return str.split(pattern);
		} else {
			return [];
		}
	}
	const checkBrowser = (browser: { browser: string, version: number }) => {
		if (typeof browser == "undefined") {
			console.warn("[Auth][Login.checkBrowser] chould not identify browser NAME: ", browser, navigator.userAgent)
		} else if (typeof browser.version == "undefined") {
			console.warn("[Auth][Login.checkBrowser] chould not identify browser VERSION: ", browser, navigator.userAgent)
		}
	}
	const browser = function (userAgent) {
		if (userAgent.indexOf('Chrome') !== -1) {
			const chromeVersion = safeSplit(userAgent, 'Chrome/')[1];
			const version = parseInt(safeSplit(chromeVersion, '.')[0]);
			return {
				browser: 'Chrome',
				version: version,
				outdated: version < 39
			}
		}
		else if (userAgent.indexOf('IEMobile') !== -1) {
			const ieVersion = safeSplit(userAgent, 'IEMobile/')[1];
			const version = parseInt(safeSplit(ieVersion, ';')[0]);
			return {
				browser: 'MSIE',
				version: version,
				outdated: version < 10
			}
		}
		else if (userAgent.indexOf('AppleWebKit') !== -1 && userAgent.indexOf('Chrome') === -1) {
			const safariVersion = safeSplit(userAgent, 'Version/')[1];
			const version = parseInt(safeSplit(safariVersion, '.')[0]);
			return {
				browser: 'Safari',
				version: version,
				outdated: version < 7
			}
		}
		else if (userAgent.indexOf('Firefox') !== -1) {
			const ffVersion = safeSplit(userAgent, 'Firefox/')[1];
			const version = parseInt(safeSplit(ffVersion, '.')[0]);
			return {
				browser: 'Firefox',
				version: version,
				outdated: version < 34
			}
		}
		else if (userAgent.indexOf('MSIE') !== -1) {
			const msVersion = safeSplit(userAgent, 'MSIE ')[1];
			const version = parseInt(safeSplit(msVersion, ';')[0]);
			return {
				browser: 'MSIE',
				version: version,
				outdated: version < 10
			}
		}
		else if (userAgent.indexOf('MSIE') === -1 && userAgent.indexOf('Trident') !== -1) {
			const msVersion = safeSplit(userAgent, 'rv:')[1];
			const version = parseInt(safeSplit(msVersion, '.')[0]);
			return {
				browser: 'MSIE',
				version: version,
				outdated: version < 10
			}
		}
	};

	$scope.browser = browser(navigator.userAgent);
	checkBrowser($scope.browser);
	if (window.location.href.indexOf('?') !== -1) {
		if (window.location.href.split('login=').length > 1) {
			$scope.login = window.location.href.split('login=')[1].split('&')[0];
		}
		if (window.location.href.split('activationCode=').length > 1) {
			$scope.activationCode = window.location.href.split('activationCode=')[1].split('&')[0];
		}
		if (window.location.href.split('callback=').length > 1) {
			let details = window.location.href.split('callback=')[1].split('&')[0].split('#');
			$scope.callBack = details[0];
			$scope.details = details.length > 0 ? details[1] : "";
		}
	}

	http().get('/auth/context').done(function (data) {
		//$scope.callBack = data.callBack;
		$scope.cgu = data.cgu;
		if (!$scope.$$phase) {
			$scope.$apply();
		}
	});

	$scope.connect = function () {
		console.log('connect');
		// picking up values manually because the browser autofill isn't registered by angular
		http().post('/auth/login', http().serialize({
			email: $('#email').val(),
			password: $('#password').val(),
			rememberMe: $scope.user.rememberMe,
			secureLocation: $scope.user.secureLocation,
			callBack: $scope.callBack,
			details: $scope.details
		}))
			.done(function (data) {
				if (typeof data !== 'object') {
					if (window.location.href.indexOf('callback=') !== -1) {
						window.location.href = (window as any).unescape(window.location.href.split('callback=')[1]);
					}
					else {
						window.location.href = $scope.callBack;
					}
				}
				if (data.error) {
					$scope.error = data.error.message;
				}
				if (!$scope.$$phase) {
					$scope.$apply();
				}
			});
	};

	for (var i = 0; i < 10; i++) {
		if (history.pushState) {
			history.pushState('', '');
		}
	}

	$scope.noSpace = function (event) {
		if (event.keyCode === 32) {
			event.preventDefault();
		}
	}

	$scope.noUpperCase = function () {
		$scope.user.email = $scope.user.email.toLowerCase();
	}

}]);
