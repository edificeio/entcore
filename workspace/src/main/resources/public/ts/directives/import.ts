import { ng, Document, quota, template, DocumentStatus, $ } from 'entcore';
import { folderToString } from '../model';

export const importFiles = ng.directive('importFiles', () => {
    return {
        restrict: 'E',
        template: `
            <lightbox show="display.importFiles" on-close="cancelUpload()">
                <div ng-if="display.editDocument">
                    <image-editor document="display.editedDocument" show="display.editDocument" inline></image-editor>
                </div>
                <div class="row media-library" ng-if="!display.editDocument">
                    <container template="import"></container>
                </div>
            <lightbox>
        `,
        link: (scope, element, attributes) => {
            template.open('import', 'directives/import/upload');

            const previousImage = () => {
				const start = scope.upload.documents.indexOf(scope.display.editedDocument) - 1;
				for(let i = start; i >= 0; i --){
					if(scope.upload.documents[i].isEditableImage){
						return scope.upload.documents[i];
					}
				}
			};

			const nextImage = () => {
				const start = scope.upload.documents.indexOf(scope.display.editedDocument) + 1;
				for(let i = start; i < scope.upload.documents.length; i ++){
					if(scope.upload.documents[i].isEditableImage){
						return scope.upload.documents[i];
					}
				}
			};

			scope.isEditedFirst = () => !previousImage();
			scope.isEditedLast = () => !nextImage();
			scope.nextImage = () => scope.display.editedDocument = nextImage();
			scope.previousImage = () => scope.display.editedDocument = previousImage();

            scope.importFiles = function(files){
                if(!files){
                    files = scope.upload.files;
                }
                template.open('import', 'directives/import/loading');
                const path = folderToString(scope.currentFolderTree, scope.openedFolder.folder);
                for(var i = 0; i < files.length; i++){
                    let doc = new Document();
                    doc.path = path;
                    scope.upload.documents.push(doc);
                    doc.upload(files[i], 'owner').then(() => {
                        quota.refresh();
                        scope.openFolder(scope.openedFolder.folder);
                    });
                }
                scope.upload.files = undefined;
            }
        
            scope.openCompression = (doc: Document) => {
                if(!doc.isEditableImage){
                    return;
                }
                scope.display.editedDocument = doc;
                setTimeout(() => {
                    scope.display.compressionReady = true;
                    scope.$apply();
                }, 350);
            };

            scope.closeCompression = () => {
				scope.display.editedDocument = undefined;
				scope.display.compressionReady = false;
			};

			scope.upload = {
				documents: []
			};
			
			$('body').on('dragenter', '.icons-view', (e) => e.preventDefault());
			$('body').on('dragover', '.icons-view', (e) => e.preventDefault());
            element.on('dragenter', (e) => e.preventDefault());

			element.on('dragover', (e) => {
				element.find('.drop-zone').addClass('dragover');
				e.preventDefault();
			});

			element.on('dragleave', () => {
				element.find('.drop-zone').removeClass('dragover');
			});

			const dropFiles = async (e) => {
				element.find('.drop-zone').removeClass('dragover');
				e.preventDefault();
				const files = e.originalEvent.dataTransfer.files;
				scope.importFiles(e.originalEvent.dataTransfer.files);
				scope.display.importFiles = true;
				scope.$apply();
            }

			$('body').on('drop', '.icons-view', dropFiles);
			element.on('drop', dropFiles);

            scope.abortOrDelete = (doc: Document) => {
				if(doc.status === DocumentStatus.loaded){
					doc.delete();
				}
				if(doc.status === DocumentStatus.loading){
					doc.abort();
				}
				const index = scope.upload.documents.indexOf(doc);
				scope.upload.documents.splice(index, 1);
				if(!scope.upload.documents.length){
					template.open('import', 'directives/import/upload');
				}
			};
            
            const cancelAll = async () => {
                template.open('import', 'directives/import/upload');
				scope.display.editedDocument = undefined;
				for(let i = 0; i < scope.upload.documents.length; i++){
                    let doc = scope.upload.documents[i];
                    if(doc.status === DocumentStatus.loaded){
						await doc.delete();
					}
					if(doc.status === DocumentStatus.loading){
						doc.abort();
					}
                }
                scope.openFolder(scope.openedFolder.folder);
				scope.upload.documents = [];
            }
            
            scope.confirmImport = async () => {
                template.open('import', 'directives/import/upload');
				scope.upload.documents.forEach(doc => doc.applyBlob());
				scope.upload.documents = [];
                scope.display.importFiles = false;
                scope.closeCompression();
				scope.$apply();
			}

			scope.cancelUpload = () => {
                scope.display.importFiles = false;
				cancelAll();
            };
            
            scope.editImage = () => scope.display.editDocument = true;
        }
    }
})