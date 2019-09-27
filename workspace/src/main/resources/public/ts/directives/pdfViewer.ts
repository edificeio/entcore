import { ng, http, $ } from 'entcore';

interface ScopePdfViewer {
	pageIndex: number | any;
	numPages: number;
	$parent: {
		render: any
	}
	loading: boolean;
	nextPage(): void;
	openPage(): void;
	previousPage(): void;
	$apply: any;
}
let _loadedPdfJs = false;
function loadPdfJs():Promise<void>{
	if(_loadedPdfJs) return Promise.resolve();
	(window as any).PDFJS = { workerSrc: '/infra/public/js/viewers/pdf.js/pdf.worker.js' };
	return http().loadScript('/infra/public/js/viewers/pdf.js/pdf.js').then(e=>{
		_loadedPdfJs = true;
		return e;
	})
}
export const pdfViewer = ng.directive('pdfViewer', function () {
	return {
		restrict: 'E',
		template: `
			<div class="flex-row align-center justify-center">
				<div class="file-controls">
					<i class="back" ng-click="previousPage()"></i>
				</div>
				<div class="pagination">
					<input type="text" ng-model="pageIndex" ng-change="openPage()" /> / [[numPages]]
				</div>
				<div class="file-controls right">
					<i class="forward" ng-click="nextPage()"></i>
				</div>
			</div>
			<p ng-if="loading" class="flex-row align-start justify-center centered-text"><i18n>workspace.preview.loading</i18n>&nbsp;<i class="loading"></i></p>
			`,
		link: function (scope: ScopePdfViewer, element, attributes) {
			let pdf:any;
			scope.loading = true;
			scope.pageIndex = 1;
			scope.nextPage = function () {
				if (scope.pageIndex < scope.numPages) {
					scope.pageIndex++;
					scope.openPage();
				}
			};
			scope.previousPage = function () {
				if (scope.pageIndex > 0) {
					scope.pageIndex--;
					scope.openPage();
				}
			};
			scope.openPage = function () {
				var pageNumber = parseInt(scope.pageIndex);
				if (!pageNumber) {
					return;
				}
				if (pageNumber < 1) {
					pageNumber = 1;
				}
				if (pageNumber > scope.numPages) {
					pageNumber = scope.numPages;
				}
				pdf.getPage(pageNumber).then(function (page) {
					var viewport;
					if (!$(canvas).hasClass('fullscreen')) {
						viewport = page.getViewport(1);
						var scale = element.width() / viewport.width;
						viewport = page.getViewport(scale);
					}
					else {
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

			var canvas = document.createElement('canvas');
			$(canvas).addClass('render');
			element.append(canvas);
			loadPdfJs().then(() => {
				(window as any).PDFJS
					.getDocument(attributes.ngSrc)
					.then(function (file) {
						pdf = file;
						scope.numPages = pdf.pdfInfo.numPages;
						scope.$apply('numPages');
						scope.openPage();
						scope.loading = false;
						scope.$apply('loading');
					}).catch(function(e){
						scope.loading = false;
						scope.$apply('loading');
					})
			});
		}
	}
});