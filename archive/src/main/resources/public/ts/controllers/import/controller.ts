import { ng, idiom as lang, notify } from 'entcore'
import { archiveService } from '../../service'

export let importController = ng.controller('ImportController', ['$scope', '$timeout', ($scope, $timeout) => {
	/*element.on('dragenter', (e) => e.preventDefault());

			element.on('dragover', (e) => {
				element.find('.drop-zone').addClass('dragover');
				e.preventDefault();
			});

			element.on('dragleave', () => {
				element.find('.drop-zone').removeClass('dragover');
            });*/

            const types = ['bytes','kilobytes','megabytes','gigabytes'];
            var currentImport;

            $scope.selectedApps = [];

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
                    $scope.currentImportId = res.data.importId;
                    archiveService.analyseArchive($scope.currentImportId).then(r => {
                        $scope.availableApps = Object.keys(r.data.apps);
                        currentImport = r.data;
                        $scope.isAnalized = true;
                        $scope.$apply();
                    })
                }).catch(err => {
                    $scope.uploadStatus = 'failed';
                    $scope.$apply();
                });
            }

            $scope.abortOrDelete = function () {
                if ($scope.uploadStatus == 'loading') {
                    archiveService.cancelUpload();
                }
                $scope.cancelImport();
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

            $scope.cancelImport = function () {
                if (!!$scope.currentImportId) {
                    archiveService.cancelImport($scope.currentImportId);
                }
                $timeout(function() {
                    delete $scope.currentImportId;
                    $scope.isAnalized = false;
                    $scope.isImporting = false;
                    delete $scope.upload;
                });
            }

            $scope.areAllSelected = function () {
                return $scope.availableApps.find(app => !$scope.selectedApps[app]) === undefined
            }
            $scope.areNoneSelected = function () {
                return $scope.availableApps.find(app => $scope.selectedApps[app]) === undefined
            }
            $scope.selectAll = function (){
                let oneFalse = !$scope.areAllSelected()
                $scope.availableApps.forEach(app => { $scope.selectedApps[app] = oneFalse })
            }

            $scope.initiateImport = function () {
                $scope.availableApps.forEach(app => {
                    if (!$scope.selectedApps[app]) {
                        delete currentImport.apps[app];
                    }
                });
                archiveService.launchImport($scope.currentImportId,currentImport.path, currentImport.apps);

            }
}]);