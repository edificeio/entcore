import { models, workspaceService } from "../services";
import { Observable, Subject } from "rxjs";



export interface SearchDelegateScope {
    search: { criteria: string, everywhere: boolean, state: "initial" | "searching" | "finished" }
    //search
    showSearchResultForFolder(): boolean
    showSearchResultForWorkspace(): boolean
    showSearchOptions(): boolean
    canResetSearch(): boolean
    resetSearch()
    searchInWorkspace();
    searchSubmit();
    searchKeyUp(event);
    isSearchResult(): boolean;
    isSearching();
    showOpenLocation(): boolean
    openLocation();
    //from others
    currentTree: models.Tree
    openedFolder: models.FolderContext
    onReloadContent: Subject<() => void>
    openFolderById(id: string):Promise<boolean>
    openFolderRouteById(id: string)
    setCurrentTreeRoute(tree: models.TREE_NAME, forceReload?: boolean);
    selectedDocuments(): models.Element[]
    safeApply()
    onInit(cab: () => void);

}

export function SearchDelegate($scope: SearchDelegateScope) {
    $scope.onInit(function () {
        //INIT
        $scope.search = { criteria: "", everywhere: false, state: "initial" }
    });
    $scope.isSearching = function () {
        return $scope.search.state == "searching";
    }
    $scope.isSearchResult = function () {
        return $scope.search.state == "finished";
    }
    $scope.onReloadContent.subscribe(() => {
        $scope.resetSearch()
    })
    $scope.searchSubmit = async function () {
        if ($scope.search.state == "searching" || !$scope.search.criteria || !$scope.search.criteria.length) {
            return;
        }
        try {
            $scope.search.state = "searching";
            if ($scope.search.everywhere) {
                let all = await workspaceService.fetchDocuments({ filter: "all", hierarchical: true, search: $scope.search.criteria, includeall: true })
                $scope.openedFolder.setFilter(all);
            } else {
                //use a backend search on current directory 
                if ($scope.openedFolder && $scope.openedFolder.folder && $scope.openedFolder.folder._id) {
                    let all = await workspaceService.fetchDocuments({ filter: "all", hierarchical: true, search: $scope.search.criteria, includeall: true, ancestorId: $scope.openedFolder.folder._id })
                    $scope.openedFolder.setFilter(all);
                } else {
                    let all = await workspaceService.fetchDocuments({ filter: $scope.currentTree.filter, hierarchical: true, search: $scope.search.criteria, includeall: true })
                    $scope.openedFolder.setFilter(all);
                }
                /**
                let criteria = $scope.search.criteria;
                //
                if (criteria) {
                    criteria = criteria.toLowerCase();
                    const filter = c => {
                        const name = c.name ? c.name.toLowerCase() : "";
                        const ownerName = c.ownerName ? c.ownerName.toLowerCase() : "";
                        return name.startsWith(criteria) || ownerName.startsWith(criteria);
                    };
                    $scope.openedFolder.applyFilter(filter);
                } else {
                    $scope.openedFolder.restore();
                }*/
            }
        } finally {
            $scope.search.state = "finished";
            $scope.safeApply();
        }
    }
    $scope.canResetSearch = function () {
        return $scope.search.state != "searching" && $scope.search.criteria && $scope.search.criteria.length > 0;
    }
    $scope.resetSearch = function () {
        $scope.search = { criteria: "", everywhere: false, state: "initial" }
        $scope.openedFolder.restore();
    }
    $scope.showSearchResultForFolder = function () {
        return $scope.search.state == "finished" && !$scope.search.everywhere;
    }
    $scope.showSearchResultForWorkspace = function () {
        return $scope.search.state == "finished" && $scope.search.everywhere;
    }
    $scope.showSearchOptions = function () {
        return $scope.search.state == "initial" && $scope.search.criteria && $scope.search.criteria.length > 0;
    }
    $scope.searchInWorkspace = function () {
        $scope.search.everywhere = true;
        $scope.searchSubmit();
    }
    $scope.showOpenLocation = function () {
        return $scope.search.state == "finished" && $scope.selectedDocuments().length == 1;
    }
    $scope.openLocation = function () {
        const first = $scope.selectedDocuments()[0];
        if (first.eParent) {
            $scope.openFolderRouteById(first.eParent);
        } else if (first.isShared) {
            $scope.setCurrentTreeRoute("shared", true)
        } else if (first.deleted) {
            $scope.setCurrentTreeRoute("trash", true)
        } else if (first.protected) {
            $scope.setCurrentTreeRoute("protected", true)
        } else {
            $scope.setCurrentTreeRoute("owner", true)
        }
    }
    $scope.searchKeyUp = function (keyEvent) {
        if (keyEvent.which === 13) {//enter
            $scope.searchSubmit();
        }
    }
}