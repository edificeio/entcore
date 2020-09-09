	import { ng, quota, template } from 'entcore';
import http from 'axios';
import { models, workspaceService, Document } from "../services";


interface ImportScope {
	upload: {
		files?: FileList
		documents: models.Element[]
	};
	display: {
		tempHide?:boolean
		dropFolderError: boolean
		editDocument: boolean
		importFiles: boolean
		compressionReady: boolean
		editedDocument: models.Element
	}
    //zip
    onZipUncompress(): void;
    onZipUpload(): void;
    onZipCancel(): void;
	hideDropFolderError(e?)
	abortOrDelete(el: models.Element)
	closeCompression()
	importFilesZip(files:FileList)
	importFiles(files: FileList)
	isEditedFirst(): boolean
	isEditedLast(): boolean
	nextImage()
	previousImage()
	openCompression(el: models.Element)
	cancelUpload()
	canConfirmImport(): boolean
	confirmImport()
	editImage()
	onImportFilesManually(files:FileList)
	//from others
	openedFolder: models.FolderContext
	safeApply(a?)
}
export const importFiles = ng.directive('importFiles', () => {
	return {
		restrict: 'E',
		template: `
            <lightbox show="display.importFiles && !display.tempHide" on-close="cancelUpload()">
                <div ng-if="display.editDocument">
                    <image-editor document="display.editedDocument" show="display.editDocument" inline></image-editor>
                </div>
				<div class="row media-library" ng-if="!display.editDocument">
					<h2><i18n>medialibrary.title</i18n></h2>
                    <container template="import"></container>
                    <container template="import-cache" style="display:none"></container>
                </div>
            <lightbox>
        `,
		link: (scope: ImportScope, element, attributes) => {
			//preload template to avoid lock screen
			template.open('import-cache', 'directives/import/loading');
			template.open('import', 'directives/import/upload');

			if (!(window as any).toBlobPolyfillLoaded) {
				http.get('/infra/public/js/toBlob-polyfill.js').then((response) => {
					eval(response.data);
					(window as any).toBlobPolyfillLoaded = true;
				});
			}

			const previousImage = () => {
				const start = scope.upload.documents.indexOf(scope.display.editedDocument) - 1;
				for (let i = start; i >= 0; i--) {
					if (scope.upload.documents[i].isEditableImage) {
						return scope.upload.documents[i];
					}
				}
			};

			const nextImage = () => {
				const start = scope.upload.documents.indexOf(scope.display.editedDocument) + 1;
				for (let i = start; i < scope.upload.documents.length; i++) {
					if (scope.upload.documents[i].isEditableImage) {
						return scope.upload.documents[i];
					}
				}
			};

			scope.isEditedFirst = () => !previousImage();
			scope.isEditedLast = () => !nextImage();
			scope.nextImage = () => scope.display.editedDocument = nextImage();
			scope.previousImage = () => scope.display.editedDocument = previousImage();

			let tempZipFiles : FileList;
			const openZipDialog = function (files: FileList) {
				tempZipFiles = files;
				scope.display.tempHide = true;
				scope.display.importFiles = true;
				template.open('lightbox', 'import-file/zip-dialog');
			}
			const cancelZipDialog = ()=>{
				tempZipFiles = null;
				template.close('lightbox');
				scope.display.tempHide = false;
			}
			scope.onImportFilesManually = function (files:FileList) {
				workspaceService.onImportFiles.next(files);
			}
			//===ZIP
			scope.onZipCancel = ()=>{
				cancelZipDialog()
				scope.display.importFiles = false;
			}
			scope.onZipUpload = ()=>{
				scope.importFiles(tempZipFiles);
				cancelZipDialog()
			}
			scope.onZipUncompress = ()=>{
				scope.importFilesZip(tempZipFiles);
				cancelZipDialog()
			}
			scope.importFiles = function (files) {
				if (!files) {
					files = scope.upload.files;
				}
				template.open('import', 'directives/import/loading');
				for (let i = 0; i < files.length; i++) {
					let file = files[i];
					let doc = new Document();
					//set parent
					workspaceService.createDocument(file, doc, scope.openedFolder.folder).then(() => {
						quota.refresh();
						//refresh content automatically
					}).catch(e=>{
						doc.uploadStatus = "failed"
						scope.safeApply();
					});
					scope.upload.documents.push(doc);
				}
				scope.upload.files = undefined;
			}
			scope.importFilesZip = function (files) {
				if (!files) {
					files = scope.upload.files;
				}
				template.open('import', 'directives/import/loading');
				for (let i = 0; i < files.length; i++) {
					let file = files[i];
					let doc = new Document();
					//set parent
					workspaceService.importZip(file, doc, scope.openedFolder.folder).then((e) => {
						quota.refresh();
						//refresh content automatically
						scope.upload.documents = scope.upload.documents.filter(d=> d!==doc);
						scope.upload.documents = [... scope.upload.documents, ...e]
						scope.safeApply();
					}).catch(e=>{
						doc.uploadStatus = "failed"
						scope.safeApply();
					});
					scope.upload.documents.push(doc);
				}
				scope.upload.files = undefined;
			}
			scope.openCompression = (doc: models.Element) => {
				if (!doc.isEditableImage) {
					return;
				}
				scope.display.editedDocument = doc;
				setTimeout(() => {
					scope.display.compressionReady = true;
					scope.safeApply();
				}, 350);
			};

			scope.closeCompression = () => {
				scope.display.editedDocument = undefined;
				scope.display.compressionReady = false;
			};

			scope.upload = {
				documents: []
			};
			element.on('dragenter', (e) => e.preventDefault());

			element.on('dragover', (e) => {
				element.find('.drop-zone').addClass('dragover');
				e.preventDefault();
			});

			element.on('dragleave', () => {
				element.find('.drop-zone').removeClass('dragover');
			});
			scope.hideDropFolderError = function (e) {
				e && e.preventDefault();
				e && e.stopPropagation()
				scope.display.dropFolderError = false;
				scope.safeApply()
			}
			const dropFiles = async (e) => {
				e.preventDefault();
				const files: FileList = e.originalEvent.dataTransfer.files;
				if (!files || !files.length) {
					return;
				}
				//
				let valid = true;
				for (let i = 0, f; f = files[i]; i++) { // iterate in the files dropped
					if (!f.type && f.size % 4096 == 0) {
						//it looks like folder
						valid = false;
					}
				}
				if (!valid) {
					scope.display.dropFolderError = true;
					scope.safeApply()
					return;
				}
				//
				element.find('.drop-zone').removeClass('dragover');
				scope.importFiles(files);
				scope.display.importFiles = true;
				scope.safeApply();
			}
			element.on('drop', dropFiles);
			workspaceService.onImportFiles.subscribe(files => {
				if (!files) {
					files = scope.upload.files;
				}
				for (let i = 0; i < files.length; i++) {
					const file = files.item(i);
					if (file.name.endsWith(".zip") || file.type == 'application/zip') {
						return openZipDialog(files);
					}
				}
				scope.importFiles(files);
				scope.display.importFiles = true;
				scope.safeApply();
			})
			scope.abortOrDelete = (doc: models.Element) => {
				if (doc.uploadStatus == "loaded") {
					workspaceService.deleteAll([doc])
				}
				if (doc.uploadStatus == "loading") {
					doc.abortUpload();
				}
				if (doc === scope.display.editedDocument) {
					scope.display.editedDocument = undefined;
				}
				const index = scope.upload.documents.indexOf(doc);
				scope.upload.documents.splice(index, 1);
				if (!scope.upload.documents.length) {
					template.open('import', 'directives/import/upload');
				}
			};
			const cancelAll = async () => {
				template.open('import', 'directives/import/upload');
				scope.display.editedDocument = undefined;
				//
				const toABort = scope.upload.documents.filter(d => d.uploadStatus == "loading");
				toABort.forEach(t => t.abortUpload())
				//
				const toDel = scope.upload.documents.filter(d => d.uploadStatus == "loaded");
				await workspaceService.deleteAll(toDel);
				//on deleteAll content auto refresh 
				scope.upload.documents = [];
			}
			scope.canConfirmImport = function () {
				return scope.upload.documents.map(d => d.uploadStatus == "loaded").reduce((a1, a2) => a1 && a2, true);
			}
			scope.confirmImport = async () => {
				template.open('import', 'directives/import/upload');
				scope.upload.documents.map(async doc => {
					if (doc.hiddenBlob) {
						await workspaceService.updateDocument(doc.hiddenBlob, doc);
						this.hiddenBlob = undefined;
					}
				});
				const copies = [...scope.upload.documents]
				const onlyRoots = scope.upload.documents.filter(current=>{
					for(const other of copies){
						if(current.eParent==other._id){
							return false;
						}
					}
					return true;
				});
				workspaceService.onConfirmImport.next(onlyRoots);
				scope.upload.documents = [];
				scope.display.importFiles = false;
				scope.closeCompression();
			}

			scope.cancelUpload = () => {
				scope.display.editDocument = false;
				scope.display.editedDocument = undefined;
				scope.display.importFiles = false;
				cancelAll();
			};

			scope.editImage = () => scope.display.editDocument = true;
		}
	}
})
