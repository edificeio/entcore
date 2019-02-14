import { ng, idiom as lang, notify } from 'entcore'
import http from 'axios'

const archiveController = ng.controller('ArchiveController', ['$scope', ($scope) => {
	$scope.filePath = 'about:blank'
	http.get('/archive/conf/public').then(function(exportApps){
		http.get('/applications-list').then(function(myApps){
			$scope.selectedApps = [];
			$scope.availableApps = myApps.data.apps.map(app => app.prefix ? app.prefix.slice(1) : "undefined")
			.filter(app => exportApps.data.apps.includes(app))
			.sort(function(a, b) {
				let a2 = lang.translate(a), b2 = lang.translate(b);
				return a2 < b2 ? -1 : a2 > b2 ? 1 : 0;
			});
			$scope.availableApps.forEach(app => { $scope.selectedApps[app] = true });
			$scope.areAllSelected = function() {
				return $scope.availableApps.find(app => !$scope.selectedApps[app]) === undefined
			}
			$scope.selectAll = function(){
				let oneFalse = !$scope.areAllSelected()
				$scope.availableApps.forEach(app => { $scope.selectedApps[app] = oneFalse })
			}
			$scope.$apply();
		});
	});
	$scope.initiateExport = function(){
		var appList = $scope.availableApps.filter(app => $scope.selectedApps[app]);
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
		})
	};
}]);

ng.controllers.push(archiveController);