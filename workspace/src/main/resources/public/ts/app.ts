import { ng, routes } from 'entcore';
import { workspaceController } from './controller';
import { importFiles } from './directives/import';
import { fileViewer } from './directives/fileViewer';
import { pdfViewer } from './directives/pdfViewer';
import { cssTransitionEnd } from './directives/cssTransitions';
import { dropzoneOverlay } from './directives/dropzoneOverlay';
import { helpBox, helpBoxStep } from './directives/helpBox'; 
import { lazyLoadImg } from './directives/lazyLoad';
import { csvViewer } from './directives/csvViewer';

routes.define(function ($routeProvider) {
	$routeProvider
		.when('/', {
			action: 'openOwn'
		})
		.when('/folder/:folderId', {
			action: 'viewFolder'
		})
		.when('/shared/folder/:folderId', {
			action: 'viewSharedFolder'
		})
		.when('/shared', {
			action: 'openShared'
		})
		.when('/trash', {
			action: 'openTrash'
		})
		.when('/apps', {
			action: 'openApps'
		})
		.when('/external', {
			action: 'openExternal'
		})
		.otherwise({
			redirectTo: '/'
		})
});

ng.controllers.push(workspaceController);
ng.directives.push(importFiles);
ng.directives.push(fileViewer);
ng.directives.push(pdfViewer);
ng.directives.push(cssTransitionEnd);
ng.directives.push(dropzoneOverlay)
ng.directives.push(helpBoxStep);
ng.directives.push(helpBox); 
ng.directives.push(lazyLoadImg)
ng.directives.push(csvViewer)
