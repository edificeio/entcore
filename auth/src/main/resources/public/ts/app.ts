import { routes, ng } from 'entcore/entcore';
import { activationController } from './controllers/activation';
import { forgotController } from './controllers/forgot';
import { resetController } from './controllers/reset';
import { loginController } from './controllers/login';

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

ng.controllers.push(activationController);
ng.controllers.push(forgotController);
ng.controllers.push(resetController);
ng.controllers.push(loginController);

console.log('app');