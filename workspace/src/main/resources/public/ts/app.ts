import { ng, routes } from 'entcore';
import { workspaceController } from './controller';

routes.define(function($routeProvider) {
	$routeProvider
		.when('/folder/:folderId', {
			action: 'viewFolder'
		})
		.when('/shared/folder/:folderId', {
	  		action: 'viewSharedFolder'
		})
		.when('/shared', {
		  	action: 'openShared'
		})
		.otherwise({
		  	redirectTo: '/'
		})
});

ng.controllers.push(workspaceController);