import { template, quota, Quota, $ } from "entcore";
import { models, workspaceService } from "../services";



export interface TreeDelegateScope {
    //from others
    onInit(cab: () => void);
    setCurrentFolder(folder: models.Element, reload?: boolean)
    selectedFolders(): models.Element[];
    openedFolder: models.FolderContext
    rolledFolders: models.Node[];
    safeApply(a?)
    closeViewFile()
    //
    wrapperTrees: models.Node[]
    trees: models.Tree[]
    quota: Quota
    currentTree: models.Tree
    currentFolderName(): string;
    onTreeInit(cb: () => void);
    isHighlightTree(folder: models.Element): boolean
    getHighlightCount(folder: models.Element): number
    openFolderRoute(el: models.Node, forceReload?: boolean)
    openFolder(el: models.Node);
    openFolderById(id: string): Promise<boolean>
    openFolderRouteById(id: string)
    rollFoldersRecursively();
    isInSelectedFolder(folder: models.Element)
    isOpenedFolder(folder: models.Node): boolean
    openOrCloseFolder(event: Event, folder: models.Node): void
    isRolledFolder(folder: models.Node): boolean
    canExpendTree(folder: models.Node): boolean
    setCurrentTree(tree: models.TREE_NAME);
    setCurrentTreeRoute(tree: models.TREE_NAME, forceReload?: boolean);
    getTreeByFilter(filter: models.TREE_NAME): models.Tree;
    removeHighlightTree(els: { folder: models.Node, count: number }[])
    setHighlightTree(els: { folder: models.Node, count: number }[]);
    firstVisibleAscendant(folder: models.Node): models.Node;

}
export function TreeDelegate($scope: TreeDelegateScope, $location) {
    let refreshPromise: Promise<models.Tree[]> = null;
    const currentTreeVoid = {};
    $scope.currentTree = currentTreeVoid as any;
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
        $scope.rolledFolders = []
        quota.refresh();
        //load trees
        refreshAll().then(e => {
            //
            callbackInits.forEach(cb => cb())
            //if already init (from routes) => wait tree is loaded
            if (!$scope.currentTree || $scope.currentTree === currentTreeVoid) {
                $scope.setCurrentTree("owner")
            }

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
            if (event.dest || event.treeDest) {
                const count = event.elements ? event.elements.length : event.ids ? event.ids.length : 0;
                $scope.setHighlightTree([{ folder: event.dest ? event.dest : $scope.getTreeByFilter(event.treeDest), count }]);
            }
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
    $scope.setCurrentTreeRoute = function (name, forceReload = false) {
        const tree = $scope.getTreeByFilter(name);
        $scope.openFolderRoute(tree, forceReload);
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
    $scope.openOrCloseFolder = function (event, folder) {
        event.stopPropagation();
        const index = $scope.rolledFolders.indexOf(folder, 0);
        if (index > -1) {
            $scope.rolledFolders.splice(index, 1);
        }
        else {
            $scope.rolledFolders.push(folder);
        }
    }
    $scope.isRolledFolder = function (folder) {
        return $scope.rolledFolders.indexOf(folder) > -1;
    }
    $scope.isInSelectedFolder = function (folder) {
        return workspaceService.isInFoldersRecursively(folder, $scope.selectedFolders());
    }
    $scope.openFolderRoute = function (folder, forceReload = false) {
        const doLocation=function(path){
            //close view file if needed
            $scope.closeViewFile();
            if(forceReload){
                if($location.path()==path){
                    window.location.reload()
                } else {
                    $location.path(path)
                }
            } else {
                $location.path(path)
            }
        }
        if (!$scope.isRolledFolder(folder)) {
            $scope.rolledFolders.push(folder);
        }
        if (folder._id) {
            doLocation("/folder/" + folder._id);
        } else if ((folder as models.Tree).filter) {
            switch ((folder as models.Tree).filter) {
                case "protected":
                    doLocation("/apps");
                    break;
                case "owner":
                    doLocation("/");
                    break;
                case "shared":
                    doLocation("/shared");
                    break;
                case "trash":
                    doLocation("/trash");
                    break;
                default:
                    $scope.openFolder(folder);
                    break;
            }
        } else {
            $scope.openFolder(folder);
        }
    }

    $scope.currentFolderName = function () {
        const folder = $scope.openedFolder.folder;
        return folder ? folder.name : "";
    }
    $scope.openFolder = async function (folder) {
        //if any refresh wait it finished
        await refreshPromise;
        //if needed
        $scope.closeViewFile();

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
        $scope.rollFoldersRecursively();
    };

    $scope.openFolderById = async function (folderId) {
        //if any refresh wait it finished
        await refreshPromise;
        const founded = workspaceService.findFolderInTrees($scope.trees, folderId);
        if (founded) {
            $scope.openFolder(founded)
            return true;
        }
        return false;
    }
    $scope.openFolderRouteById = async function (folderId) {
        //if any refresh wait it finished
        await refreshPromise;
        const founded = workspaceService.findFolderInTrees($scope.trees, folderId);
        if (founded) {
            $scope.openFolderRoute(founded)
        }
        $scope.safeApply();
    }
    $scope.rollFoldersRecursively = function () {
        let rec = function (tree: models.Tree) {
            if ($scope.openedFolder.folder === tree) {
                return true;
            }
            for (let child of tree.children) {
                if (rec(child)) {
                    if (!$scope.isRolledFolder(tree)) {
                        $scope.rolledFolders.push(tree);
                    }
                    return true;
                }
            }
            return false;
        }
        for (let tree of $scope.trees) {
            if (rec(tree)) {
                if (!$scope.isRolledFolder(tree)) {
                    $scope.rolledFolders.push(tree);
                }
                break;
            }
        }
    }
    /**
     * Highlight
     */
    let highlighted: { folder: models.Node, count: number }[] = []
    $scope.setHighlightTree = function (els: { folder: models.Node, count: number }[]) {
        if (els) {
            els.map(el => el.folder = $scope.firstVisibleAscendant(el.folder))
        }
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
    $scope.firstVisibleAscendant = function (folder: models.Node): models.Node {
        let ret;
        let rec = function (tree: models.Tree) {
            if (folder === tree) {
                return true;
            }
            for (let child of tree.children) {
                if ($scope.isRolledFolder(tree)) {
                    ret = child;
                }
                if (rec(child)) {
                    return true;
                }
            }
            return false;
        }
        for (let tree of $scope.trees) {
            ret = tree;
            if (rec(tree)) {
                return ret;
            }
        }
    }

}

