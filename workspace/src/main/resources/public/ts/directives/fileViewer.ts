import { $, ng, Behaviours } from 'entcore';
import http from "axios";
import { workspaceService, models } from "../services";
import { CsvDelegate, CsvFile, CsvController } from './csvViewer';
import { TxtDelegate, TxtController, TxtFile } from './txtViewer';
declare var ENABLE_LOOL: boolean;
declare var ENABLE_SCRATCH: boolean;


interface FileViewerScope {
	contentType: string;
	ngModel: models.Element;
	isFullscreen: boolean;
	csvDelegate: CsvDelegate;
	txtDelegate: TxtDelegate
	isTxt(): boolean
	isStreamable(): boolean
	download(): void;
	isOfficePdf(): boolean;
	isOfficeExcelOrCsv(): boolean;
	getCsvContent(): CsvDelegate;
	editImage(): void;
	fullscreen(allo: boolean): void;
	render?: () => void;
	previewUrl(): string;
	closeViewFile(): void;
	canEditInLool(): boolean;
	canEditInScratch(): boolean;
	openOnLool(): void;
	openOnScratch(): void;
	$parent: {
		display: {
			editedImage: any;
			editImage: boolean;
		}
	}
	onBack: any
	htmlContent: string;
	download(): void;
	canDownload(): boolean
	editImage(): void
	fullscreen(allow: boolean): void
	$apply: any
}

class CsvProviderFromText implements CsvFile {
	private _cache: Promise<string>;
	constructor(private model: models.Element) { }
	get id() { return this.model._id; }
	get content() {
		if (this._cache) return this._cache;
		this._cache = new Promise<string>(async (resolve, reject) => {
			const a = await workspaceService.getDocumentBlob(this.model._id);
			const reader = new FileReader();
			reader.onload = () => {
				const res = (reader.result) as string;
				resolve(res);
			}
			reader.onerror = (e) => reject(e);
			reader.readAsText(a);
		})
		return this._cache;
	}
}
class CsvProviderFromExcel implements CsvFile {
	private _cache: Promise<string>;
	constructor(private model: models.Element) { }
	get id() { return this.model._id; }
	get content() {
		if (this._cache) return this._cache;
		this._cache = new Promise<string>(async (resolve, reject) => {
			const a = await workspaceService.getPreviewBlob(this.model._id);
			const reader = new FileReader();
			reader.onload = () => {
				const res = (reader.result) as string;
				resolve(res);
			}
			reader.onerror = (e) => reject(e);
			reader.readAsText(a);
		})
		return this._cache;
	}
}
export const fileViewer = ng.directive('fileViewer', ['$sce', ($sce) => {
	return {
		restrict: 'E',
		scope: {
			ngModel: '=',
			onBack: '&'
		},
		templateUrl: '/workspace/public/template/directives/file-viewer.html',
		link: function (scope: FileViewerScope, element, attributes) {
			const _csvCache: { [key: string]: CsvFile } = {}
			const _txtCache: { [key: string]: TxtFile } = {}
			let _csvController: CsvController = null;
			let _txtDelegate: TxtController = null;
			scope.csvDelegate = {
				onInit(ctrl) {
					_csvController = ctrl;
					_csvController.setContent(getCsvContent());
				}
			}
			scope.txtDelegate = {
				onInit(ctrl) {
					_txtDelegate = ctrl;
					_txtDelegate.setContent(getTxtContent())
				}
			}
			const getCsvContent = () => {
				if (_csvCache[scope.ngModel._id]) {
					return _csvCache[scope.ngModel._id];
				}
				if (scope.contentType == "csv") {
					_csvCache[scope.ngModel._id] = new CsvProviderFromText(scope.ngModel);
				} else {
					_csvCache[scope.ngModel._id] = new CsvProviderFromExcel(scope.ngModel);
				}
				return _csvCache[scope.ngModel._id];
			}
			const getTxtContent = () => {
				if (_txtCache[scope.ngModel._id]) {
					return _txtCache[scope.ngModel._id];
				}
				_txtCache[scope.ngModel._id] = new CsvProviderFromText(scope.ngModel);
				return _txtCache[scope.ngModel._id];
			}
			//
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
			scope.canDownload = () => {
				return workspaceService.isActionAvailable("download", [scope.ngModel])
			}

			scope.isOfficePdf = () => {
				const ext = ['doc', 'ppt'];
				return ext.includes(scope.contentType);
			}

			scope.isOfficeExcelOrCsv = () => {
				const ext = ['xls', 'csv'];
				return ext.includes(scope.contentType);
			}

			scope.isTxt = () => {
				const ext = ['txt'];
				return ext.includes(scope.contentType);
			}

			scope.isStreamable = () => {
				return scope.contentType==='video' 
					&& scope.ngModel 
					&& scope.ngModel.metadata 
					&& typeof scope.ngModel.metadata.captation === "boolean";
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

			scope.openOnScratch = () => {
				ENABLE_SCRATCH && window.open(`/scratch/open?ent_id=${scope.ngModel._id}`);
			}

			scope.canEditInLool = () => {
				const ext = ['doc', 'ppt', "xls"];
				const isoffice = ext.includes(scope.contentType);
				return isoffice && ENABLE_LOOL && Behaviours.applicationsBehaviours.lool.canBeOpenOnLool(scope.ngModel);
			}

			scope.canEditInScratch = () => {
				return ENABLE_SCRATCH &&
					["sb","sb2","sb3"].includes(scope.ngModel.metadata.extension) &&
					scope.ngModel.metadata["content-type"] === "application/octet-stream";
			}

			scope.fullscreen = (allow) => {
				//is an external renderer managing the fullscreen? if so return
				if (workspaceService.renderFullScreen(scope.ngModel) != false) {
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