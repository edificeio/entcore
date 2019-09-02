import { ng, idiom as lang, notify } from 'entcore'
import { archiveService } from '../../service'

export let importController = ng.controller('ImportController', ['$scope', ($scope) => {
	/*element.on('dragenter', (e) => e.preventDefault());

			element.on('dragover', (e) => {
				element.find('.drop-zone').addClass('dragover');
				e.preventDefault();
			});

			element.on('dragleave', () => {
				element.find('.drop-zone').removeClass('dragover');
            });*/

            const types = ['bytes','kilobytes','megabytes','gigabytes'];

            $scope.importFile = function () {
                let file: File = $scope.upload.files.item(0);
                let formData: FormData = new FormData();
                formData.append('file', file);
                $scope.isImporting = true;
                $scope.filename = file.name;
                $scope.filesize = getSize(file.size);
                $scope.uploadStatus = 'loading';
                archiveService.uploadArchive(formData).then(res => {
                    $scope.uploadStatus = 'loaded';
                    archiveService.analyseArchive(res.data.importId).then(r => {
                        console.log(r);
                    })
                    $scope.$apply();
                }).catch(err => {
                    $scope.uploadStatus = 'failed';
                    $scope.$apply();
                });
            }

            $scope.abortOrDelete = function () {
                if ($scope.uploadStatus == 'loading') {
                    archiveService.cancelUpload();
                } else if ($scope.uploadStatus == 'loaded') {

                }
                $scope.isImporting = false;
            }

            const getSize = function(sizeAsBytes: number): { quantity: number, unity: string } {
                for (var i = 0; i < 4; i++) {
                    var v = Math.floor(sizeAsBytes/Math.pow(1000,i));
                    if (v < 1024  || i == 3) {
                        let quantity = sizeAsBytes/Math.pow(1000,i);
                        return { quantity: Math.round(quantity*100)/100 , unity: `import.${types[i]}` }
                    }
                }
            }
}]);