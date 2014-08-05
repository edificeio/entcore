function ArchiveController($scope){
	$scope.filePath = 'about:blank'
	$scope.initiateExport = function(){
		http().post('/archive/export', {}, { requestName: 'archive' }).done(function(data){
			window.location.href = '/archive/export/' + data.exportId;
			$scope.loading = true;
			$scope.$apply();
		})
	};
}