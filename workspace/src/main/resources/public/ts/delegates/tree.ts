import { template, quota, Quota, $ } from "entcore";
import { models, workspaceService } from "../services";



export interface TreeDelegateScope {
    //from others
    onInit(cab: () => void);
    setCurrentFolder(folder: models.Element, reload?: boolean)
    selectedFolders(): models.Element[];
    openedFolder: models.FolderContext
    safeApply(a?)
    closeViewFile()
    //
    wrapperTrees: models.Node[]
    trees: models.Tree[]
    quota: Quota
    currentTree: models.Tree
    onTreeInit(cb: () => void);
    isHighlightTree(folder: models.Element): boolean
    getHighlightCount(folder: models.Element): number
    openFolderRoute(el: models.Node)
    openFolder(el: models.Node);
    openFolderById(id: string)
    isInSelectedFolder(folder: models.Element)
    isOpenedFolder(folder: models.Node): boolean
    canExpendTree(folder: models.Node): boolean
    setCurrentTree(tree: models.TREE_NAME);
    setCurrentTreeRoute(tree: models.TREE_NAME);
    getTreeByFilter(filter: models.TREE_NAME): models.Tree;
    removeHighlightTree(els: { folder: models.Element | models.TREE_NAME, count: number }[])
    setHighlightTree(els: { folder: models.Element | models.TREE_NAME, count: number }[]);

}
export function TreeDelegate($scope: TreeDelegateScope, $location) {
    let refreshPromise: Promise<models.Tree[]> = null;
    const refreshAll = function () {
        refreshPromise = workspaceService.fetchTrees({
            filter: "all",
            hierarchical: true
        });
        refreshPromise.then(trees => {
            trees.forEach(tree => {
                const current = $scope.trees.find(t => t.filter == tree.filter);
                current.children = tree.children;
            })
            if ($scope.openedFolder.folder && $scope.openedFolder.folder._id) {
                $scope.openFolderById($scope.openedFolder.folder._id)
            } else {
                $scope.setCurrentTree($scope.currentTree.filter)
            }
        })
        return refreshPromise;
    }
    const callbackInits: (() => void)[] = [];
    $scope.onTreeInit = function (cb) {
        if (refreshPromise) {
            refreshPromise.then(() => cb())
        } else {
            callbackInits.push(cb);
        }
    }
    $scope.onInit(function () {
        //INIT
        $scope.wrapperTrees = [{
            children: $scope.trees
        }]
        $scope.quota = quota;
        quota.refresh();
        //if already init (from routes)
        if (!$scope.currentTree) {
            $scope.setCurrentTree("owner")
        }
        //load trees
        refreshAll().then(e => {
            callbackInits.forEach(cb => cb())
        });
        //
        workspaceService.onChange.subscribe(event => {
            const folders = event.elements ? //
                event.elements.filter(el => workspaceService.isFolder(el)) //
                : event.ids ? event.ids.map(id => workspaceService.findFolderInTrees($scope.trees, id)) //
                    : null;
            switch (event.action) {
                case "add":
                    if (folders) {
                        folders.forEach(el => {
                            if (event.dest && event.dest._id) {
                                //add to dest folder
                                const founded = workspaceService.findFolderInTrees($scope.trees, event.dest._id);
                                if (founded) {
                                    founded.children.push(el);
                                }
                            } else if (event.treeDest) {
                                const tree = $scope.trees.find(tree => tree.filter == event.treeDest);
                                tree.children.push(el);
                            } else {
                                //add to current tree
                                $scope.currentTree.children.push(el);
                            }
                        })
                    }
                    break;
                case "update":
                    if (folders) {
                        folders.forEach(el => {
                            const founded = workspaceService.findFolderInTrees($scope.trees, el._id);
                            if (founded)
                                Object.assign(founded, el)
                        })
                    }
                    break;
                case "delete":
                    if (folders) {
                        $scope.trees.forEach(tree => {
                            workspaceService.removeFromTree(tree, el => {
                                const found = folders.findIndex(el2 => workspaceService.elementEqualsByRefOrId(el, el2));
                                return found > -1;
                            })
                        })
                    }
                    break;
                case "tree-change":
                    if (folders && folders.length) {
                        //if any folder => refresh all trees
                        refreshAll();
                    }
                    break;
                case "empty":
                    $scope.getTreeByFilter(event.treeSource).children = []
                    break;
            }
            quota.refresh();
            //Highlight new shared
            if ((event.action == "add" || event.action == "tree-change") && event.treeDest == "shared" && event.treeSource != "shared" && event.elements) {
                $scope.setHighlightTree([{ folder: "shared", count: event.elements.length }])
            }
            //
            $scope.safeApply()
        })
    });
    $scope.getTreeByFilter = function (filter) {
        const tree = $scope.trees.find(tree => tree.filter == filter);
        if (!tree) {
            throw "could not found tree with name: " + filter;
        }
        return tree;
    }
    $scope.setCurrentTreeRoute = function (name) {
        const tree = $scope.getTreeByFilter(name);
        $scope.openFolderRoute(tree);
    }
    $scope.setCurrentTree = function (name) {
        $scope.currentTree = $scope.getTreeByFilter(name);
        $scope.setCurrentFolder($scope.currentTree as models.Element, true);
    }
    $scope.canExpendTree = function (folder) {
        return folder.children.length > 0;
    }
    $scope.isOpenedFolder = function (folder) {
        if ($scope.openedFolder.folder === folder) {
            return true;
        }
        return workspaceService.findFolderInTreeByRefOrId(folder, $scope.openedFolder.folder);
    }

    $scope.isInSelectedFolder = function (folder) {
        return workspaceService.isInFoldersRecursively(folder, $scope.selectedFolders());
    }
    $scope.openFolderRoute = function (folder) {
        if (folder._id) {
            $location.path("/folder/" + folder._id);
        } else if ((folder as models.Tree).filter) {
            switch ((folder as models.Tree).filter) {
                case "protected":
                    $location.path("/apps");
                    break;
                case "owner":
                    $location.path("/");
                    break;
                case "shared":
                    $location.path("/shared");
                    break;
                case "trash":
                    $location.path("/trash");
                    break;
                default:
                    $scope.openFolder(folder);
                    break;
            }
        } else {
            $scope.openFolder(folder);
        }
    }
    $scope.openFolder = async function (folder) {
        //if any refresh wait it finished
        await refreshPromise;
        //if needed
        $scope.closeViewFile();
        setTimeout(function () {
            $('body').trigger('whereami.update');
        }, 100)

        const founded = $scope.trees.find(tree => {
            return workspaceService.findFolderInTreeByRefOrId(tree, folder, (founded) => {
                folder = founded;
            });
        })
        //change current tree if needed
        if (founded && founded !== $scope.currentTree) {
            $scope.currentTree = founded;
        }
        $scope.setCurrentFolder(folder as models.Element, true);
    };

    $scope.openFolderById = async function (folderId) {
        //if any refresh wait it finished
        await refreshPromise;
        const founded = workspaceService.findFolderInTrees($scope.trees, folderId);
        if (founded) {
            $scope.openFolder(founded)
        }
    }
    /**
     * Highlight
     */
    let highlighted: { folder: models.Node | models.TREE_NAME, count: number }[] = []
    $scope.setHighlightTree = function (els: { folder: models.Element | models.TREE_NAME, count: number }[]) {
        highlighted = els ? [...els] : [];
        highlighted = highlighted.filter(h => !!h);
        $scope.safeApply();

        setTimeout(() => {
            $scope.removeHighlightTree(els)
        }, 3100)
    }
    $scope.removeHighlightTree = function (els) {
        els.forEach(el => {
            highlighted = highlighted.filter(e => e !== el);
        })
        $scope.safeApply();
    }
    const findHighlight = function (folder: models.Node) {
        return highlighted.filter(f => {
            if (typeof f.folder == "string") {
                return (folder as models.Tree).filter === f.folder;
            } else {
                return workspaceService.elementEqualsByRefOrId(folder, f.folder as models.Node)
            }
        })
    }
    $scope.isHighlightTree = function (folder: models.Element) {
        return findHighlight(folder).length > 0;
    }
    $scope.getHighlightCount = function (folder: models.Element) {
        let t = findHighlight(folder)
        return t && t.length ? t[0].count : null;
    }


}

