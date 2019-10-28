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
    
    $scope.countMainResources = function(rapport)
    {
        let mainResourceName = rapport["mainResourceName"];
        let idsMap = rapport["resourcesIdsMap"];

        if(mainResourceName != null && idsMap != null)
        {
            let mainResources = idsMap[mainResourceName];
            if(mainResources != null)
                return Object.keys(mainResources).length;
        }

        return rapport["resourcesNumber"];
    }

    $scope.countDuplicateResources = function(rapport)
    {
        let mainResourceName = rapport["mainResourceName"];
        let dupsMap = rapport["duplicatesNumberMap"];

        if(mainResourceName != null && dupsMap != null)
        {
            let dups = dupsMap[mainResourceName];
            if(dups != null)
                return dups;
        }

        return rapport["duplicatesNumber"];
    }
	
}]);