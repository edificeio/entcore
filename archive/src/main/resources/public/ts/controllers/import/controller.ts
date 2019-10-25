import { ng, idiom as lang, notify, template } from 'entcore'
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
            $scope.firstPhase = true;
            $scope.importLaunched = false;

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
                    archiveService.analyzeArchive($scope.currentImportId).then(r => {
                        $scope.availableApps = Object.keys(r.data.apps);
                        $scope.quota = getSize(r.data.quota);
                        $scope.quotaExceeded = false;
                        $scope.appsSize = [];
                        $scope.availableApps.forEach(app => {
                            if(r.data.apps[app].size != 0) {
                                $scope.appsSize[app] =  getSize(r.data.apps[app].size);
                            }
                        });
                        $scope.sum = getSize(0);
                        if ($scope.availableApps.length == 0) {
                            notify.error('archive.import.none.available');
                            $scope.cancelImport();
                        } else {
                            currentImport = r.data;
                            $scope.isAnalized = true;
                            $scope.$apply();
                        }
                    }).catch(err => {
                        notify.error('archive.import.corrupted');
                        delete $scope.currentImportId;
                        $scope.cancelImport();
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

            $scope.updateQuota = function() {
                var newSum = 0;
                $scope.availableApps.forEach(app => {
                    if ($scope.selectedApps[app]) {
                        newSum += currentImport.apps[app].size
                    }
                });
                $scope.sum = getSize(newSum);
                $scope.quotaExceeded = newSum > currentImport.quota;
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
                    $scope.importLaunched = false;
                    $scope.selectedApps = [];
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
                $scope.updateQuota();
            }

            $scope.initiateImport = function () {
                $scope.availableApps.forEach(app => {
                    if (!$scope.selectedApps[app]) {
                        delete currentImport.apps[app];
                    }
                });
                $timeout(function() {
                    $scope.importLaunched = true;
                });
                archiveService.launchImport($scope.currentImportId,currentImport.path, currentImport.apps)
                .then(result => {
                    $scope.resultsApps = Object.keys(result.data);
                    $scope.resultsApps.forEach(app => {
                        if (app == "actualites" || app == "schoolbook" || app == "exercizer") {
                            result.data[app].duplicatesNumber = lang.translate("archive.import.na");
                        }
                    });
                    $scope.results = result.data;
                    $scope.loadingSpinner = false;
                    $scope.$apply();
                });
                $scope.loadingSpinner = true;
                $scope.firstPhase = false;

            }
}]);