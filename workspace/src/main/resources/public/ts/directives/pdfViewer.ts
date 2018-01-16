import { ng, http, $ } from 'entcore';

export const pdfViewer = ng.directive('pdfViewer', function(){
	return {
		restrict: 'E',
        template: `
            <div class="pagination">
                <input type="text" ng-model="pageIndex" ng-change="openPage()" /> / [[numPages]]
            </div>
            <div class="file-controls">
                <i class="back" ng-click="previousPage()"></i>
                <i class="forward" ng-click="nextPage()"></i>
            </div>`,
		link: function(scope, element, attributes){
			var pdf;
			scope.pageIndex = 1;
			scope.nextPage = function(){
				if(scope.pageIndex < scope.numPages){
					scope.pageIndex ++;
					scope.openPage();
				}
			};
			scope.previousPage = function(){
				if(scope.pageIndex > 0){
					scope.pageIndex --;
					scope.openPage();
				}
			};
			scope.openPage = function(){
				var pageNumber = parseInt(scope.pageIndex);
				if(!pageNumber){
					return;
				}
				if(pageNumber < 1){
					pageNumber = 1;
				}
				if(pageNumber > scope.numPages){
					pageNumber = scope.numPages;
				}
				pdf.getPage(pageNumber).then(function (page) {
					var viewport;
					if(!$(canvas).hasClass('fullscreen')){
						viewport = page.getViewport(1);
						var scale = element.width() / viewport.width;
						viewport = page.getViewport(scale);
					}
					else{
						viewport = page.getViewport(2);
					}

					var context = canvas.getContext('2d');
					canvas.height = viewport.height;
					canvas.width = viewport.width;

					var renderContext = {
						canvasContext: context,
						viewport: viewport
					};
					page.render(renderContext);
				});
			};
			scope.$parent.render = scope.openPage;

			(window as any).PDFJS = { workerSrc: '/infra/public/js/viewers/pdf.js/pdf.worker.js' };
			var canvas = document.createElement('canvas');
			$(canvas).addClass('render');
			element.append(canvas);
			http().loadScript('/infra/public/js/viewers/pdf.js/pdf.js').then(() => {
				(window as any).PDFJS
						.getDocument(attributes.ngSrc)
						.then(function(file){
							pdf = file;
							scope.numPages = pdf.pdfInfo.numPages;
							scope.$apply('numPages');
							scope.openPage();
						});
			});
		}
	}
});