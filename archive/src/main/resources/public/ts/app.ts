import { ng, idiom as lang, notify } from 'entcore'
import http from 'axios'

const archiveController = ng.controller('ArchiveController', ['$scope', ($scope) => {
	$scope.filePath = 'about:blank'
	var expected;
	http.get('/archive/conf/public').then(function(exportApps){
		http.get('/applications-list').then(function(myApps){
			$scope.selectedApps = [];
			$scope.availableApps = myApps.data.apps.map(app => app.prefix ? app.prefix.slice(1) : "undefined")
			.filter(app => exportApps.data.apps.includes(app))
			.sort(function(a, b) {
				let a2 = lang.translate(a), b2 = lang.translate(b);
				return a2 < b2 ? -1 : a2 > b2 ? 1 : 0;
			});
			if ($scope.availableApps.length === 0) {
				$scope.isPreDeleted = true;
				expected = exportApps.data.apps;
			}
			$scope.availableApps.forEach(app => { $scope.selectedApps[app] = false });
			$scope.selectedApps["workspace"] = true;
			$scope.selectedApps["rack"] = true;
			$scope.areAllSelected = function() {
				return $scope.availableApps.find(app => !$scope.selectedApps[app]) === undefined
			}
			$scope.areNoneSelected = function() {
				return $scope.availableApps.find(app => $scope.selectedApps[app]) === undefined
			}
			$scope.selectAll = function(event){
				let oneFalse = !$scope.areAllSelected()
				$scope.availableApps.forEach(app => { $scope.selectedApps[app] = oneFalse })
			}
			$scope.$apply();
		});
	});
	$scope.initiateExport = function(mode: string){
		var appList;
		if (mode === "docsOnly") {
			appList = ["workspace","rack"];
		} else if (mode === "all") {
			appList = expected;
		} else if (mode === "apps") {
			appList = $scope.availableApps.filter(app => $scope.selectedApps[app]);
		}
		http.post('/archive/export', {'apps':appList}).then(function(res){
			$scope.loadingSpinner = true;
			setTimeout(function() {
				http.get('/archive/export/verify/' + res.data.exportId).then(function(status){
					window.location.href = '/archive/export/' + res.data.exportId;
					$scope.loadingSpinner = false;
					$scope.$apply();
				}).catch(function(){
					notify.error('archive.error');
					setTimeout(function() {
						window.location.reload();
					},3000);
				});
			},5000);
			$scope.loading = true;
			$scope.$apply();
		}).catch(function(){
			notify.error('export.already');
		})
	};
}]);

ng.controllers.push(archiveController);