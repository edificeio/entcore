import { $, ng } from 'entcore';
import { workspaceService, Document } from "../services";

interface FileViewerScope {
	contentType: string
	htmlContent: string;
	isFullscreen: boolean
	ngModel: Document
	download(): void;
	canDownload():boolean
	editImage(): void
	fullscreen(allow: boolean): void
	render?(): void
	$apply: any
	$parent: any
}
export const fileViewer = ng.directive('fileViewer', ['$sce',($sce) => {
	return {
		restrict: 'E',
		scope: {
			ngModel: '='
		},
		templateUrl: '/workspace/public/template/directives/file-viewer.html',
		link: function (scope: FileViewerScope, element, attributes) {
			scope.contentType = scope.ngModel.previewRole();
			if (scope.contentType == 'html') {
				const call = async () => {
					const a = await workspaceService.getDocumentBlob(scope.ngModel._id);
					const reader = new FileReader();
					reader.onload = function () {
						scope.htmlContent = $sce.trustAsHtml(reader.result) as string;
						scope.$apply();
					}
					reader.readAsText(a);
				}
				call();
			}
			scope.isFullscreen = false;

			scope.download = function () {
				workspaceService.downloadFiles([scope.ngModel]);
			};
			let renderElement;
			let renderParent;
			scope.canDownload = ()=>{
				return workspaceService.isActionAvailable("download",[scope.ngModel])
			}
			scope.editImage = () => {
				//scope.$parent.openedFolder.content.forEach(d => d.selected = false);
				scope.$parent.display.editedImage = scope.ngModel;
				scope.$parent.display.editImage = true;
			}

			scope.fullscreen = (allow) => {
				//is an external renderer managing the fullscreen? if so return
				if(workspaceService.renderFullScreen(scope.ngModel)!=false){
					return;
				}
				scope.isFullscreen = allow;
				if (allow) {
					let container = $('<div class="fullscreen-viewer"></div>');
					container.hide();
					container.on('click', function (e) {
						if (!$(e.target).hasClass('render')) {
							scope.fullscreen(false);
							scope.$apply('isFullscreen');
						}
					});
					element.children('.embedded-viewer').addClass('fullscreen');
					renderElement = element
						.find('.render');
					renderParent = renderElement.parent();

					renderElement
						.addClass('fullscreen')
						.appendTo(container);
					container.appendTo('body');
					container.fadeIn();
					if (typeof scope.render === 'function') {
						scope.render();
					}
				}
				else {
					renderElement.removeClass('fullscreen').appendTo(renderParent);
					element.children('.embedded-viewer').removeClass('fullscreen');
					var fullscreenViewer = $('body').find('.fullscreen-viewer');
					fullscreenViewer.fadeOut(400, function () {
						fullscreenViewer.remove();
					});

					if (typeof scope.render === 'function') {
						scope.render();
					}
				}
			}
		}
	}
}]);