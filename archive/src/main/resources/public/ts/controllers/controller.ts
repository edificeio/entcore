import { ng, template } from 'entcore'

export let archiveController = ng.controller('ArchiveController', ['$scope', ($scope) => {

    $scope.currentTab = 'export';
    template.open('main', 'export-tab');

    $scope.switchTab = function(tab) {
        $scope.currentTab = tab;
        switch (tab) {
            case 'export':
                template.open('main', 'export-tab');
                break;
            case 'import':
                template.open('main', 'import-tab');
                break;
        }
    }
    
	
}]);