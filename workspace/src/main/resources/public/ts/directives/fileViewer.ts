import { $, ng, Behaviours } from 'entcore';
import { workspaceService, models } from "../services";
declare var ENABLE_LOOL: boolean;

interface FileViewerScope {
	contentType: string;
	ngModel: models.Element;
	isFullscreen: boolean;
	download(): void;
	isOfficeType(): boolean;
	editImage(): void;
	fullscreen(allo: boolean): void;
	render?: () => void;
	previewUrl(): string;
	closeViewFile(): void;
	canEditInLool(): boolean;
	openOnLool(): void;
	$apply: any;
	$parent: {
		display: {
			editedImage: any;
			editImage: boolean;
		}
	}
	onBack: any
}

interface FileViewerScope {
	contentType: string
	htmlContent: string;
	isFullscreen: boolean
	download(): void;
	canDownload():boolean
	editImage(): void
	fullscreen(allow: boolean): void
	$apply: any
}
export const fileViewer = ng.directive('fileViewer', ['$sce',($sce) => {
	return {
		restrict: 'E',
		scope: {
			ngModel: '=',
			onBack: '&'
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

			scope.isOfficeType = () => {
				const ext = ['doc', 'xls', 'ppt'];
				return ext.includes(scope.contentType);
			}
			scope.closeViewFile = () => {
				scope.onBack();
			}
			scope.previewUrl = () => {
				return scope.ngModel.previewUrl;
			}

			scope.editImage = () => {
				//scope.$parent.openedFolder.content.forEach(d => d.selected = false);
				scope.$parent.display.editedImage = scope.ngModel;
				scope.$parent.display.editImage = true;
			}

			scope.openOnLool = () => {
				ENABLE_LOOL && Behaviours.applicationsBehaviours.lool.openOnLool(scope.ngModel);
			}

			scope.canEditInLool = () => {
				return scope.isOfficeType() && ENABLE_LOOL && Behaviours.applicationsBehaviours.lool.canBeOpenOnLool(scope.ngModel);
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