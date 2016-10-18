import { ng, http } from 'entcore'

const archiveController = ng.controller('ArchiveController', ['$scope', ($scope) => {
	$scope.filePath = 'about:blank'
	$scope.initiateExport = function(){
		http().post('/archive/export', {}, { requestName: 'archive' }).done(function(data){
			setTimeout(function() {
					window.location.href = '/archive/export/' + data.exportId;
				},
				5000
			);
			$scope.loading = true;
			$scope.$apply();
		})
	};
}]);

ng.controllers.push(archiveController);