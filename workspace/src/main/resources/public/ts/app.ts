import { ng, routes } from 'entcore';
import { workspaceController } from './controller';
import { importFiles } from './directives/import';
import { fileViewer } from './directives/fileViewer';
import { pdfViewer } from './directives/pdfViewer';

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
ng.directives.push(importFiles);
ng.directives.push(fileViewer);
ng.directives.push(pdfViewer);