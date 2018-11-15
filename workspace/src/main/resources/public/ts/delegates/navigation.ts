import { template } from "entcore";
import { models, workspaceService, WorkspacePreferenceView } from "../services";

export interface NavigationDelegateScope {
    //
    display: { viewFile?: models.Element, nbFiles: number, editedImage?: models.Element, editImage?: boolean, quickStart?: boolean }
    boxes: { selectAll: boolean }
    openedFolder: models.FolderContext
    selectedFolder: { folder: models.Element, name: string }
    onQuickstartFinished()
    //list view
    isTrashTree(): boolean
    isOrderedAsc(field: string): boolean
    isOrderedDesc(field: string): boolean
    //
    isViewMode(mode: WorkspacePreferenceView): boolean
    changeViewMode(mode: WorkspacePreferenceView)
    //
    setHighlighted(els: models.Element[])
    isHighlighted(el: models.Element)
    removeHighlight(el: models.Element[]);
    //
    //
    canOpenFolder(): boolean
    canOpenFile(): boolean
    //
    currentFolderName(): string;
    incrementVisibleFiles();
    switchSelectAll();
    selectedDocuments(): models.Element[];
    selectedFolders(): models.Element[];
    reloadFolderContent();
    viewFile(document: models.Element)
    setFolder(key: string, folder: models.Element)
    setAll();
    selectedItems(): models.Element[];
    isSelectedFolder(folder: models.Element): boolean
    setCurrentFolder(folder: models.Element, reload?: boolean)
    // from others 
    currentTree: models.Tree;
    trees: models.Tree[]
    resetSearch();
    safeApply(a?)
    $watch(a: any, f: Function)
    onInit(cab: () => void);
    order: { field: string, desc: boolean, order?: (item: models.Element) => any }
}
export function NavigationDelegate($scope: NavigationDelegateScope) {
    let highlighted: models.Element[] = [];
    let viewMode: WorkspacePreferenceView = null;
    $scope.onInit(function () {
        //INIT 
        $scope.openedFolder = new models.FolderContext;
        $scope.boxes = { selectAll: false }
        $scope.selectedFolder = { folder: models.emptyFolder(), name: '' };
        $scope.changeViewMode("icons");
        workspaceService.getPreference().then(pref => {
            $scope.changeViewMode(pref.view)
            $scope.display.quickStart = pref.quickstart == "notviewed";
            $scope.safeApply()
        })
        workspaceService.onChange.subscribe(event => {
            const files = event.elements ? event.elements.filter(el => workspaceService.isFile(el)) : []
            const folders = event.elements ? event.elements.filter(el => workspaceService.isFolder(el)) : []
            switch (event.action) {
                case "add":
                    $scope.setHighlighted(folders);
                    if (event.dest === $scope.openedFolder.folder) {
                        files.forEach(a => {
                            $scope.openedFolder.pushDoc(a)
                        })
                    }
                    break;
                case "update":
                    $scope.setHighlighted(event.elements);
                    files.forEach(el => {
                        let founded = $scope.openedFolder.findDocuments(c => c._id == el._id);
                        founded.forEach(f => Object.assign(f, el))
                    })
                    break;
                case "tree-change":
                    $scope.reloadFolderContent();
                    break;
                case "delete":
                    if (files.length) {
                        $scope.openedFolder.deleteDocuments(el => {
                            const found = files.findIndex(el2 => workspaceService.elementEqualsByRefOrId(el, el2));
                            return found > -1;
                        });
                    }

                    break;
                case "empty":
                case "document-change":
                    $scope.reloadFolderContent();
                    break;
            }
            $scope.safeApply()
        })
    });
    $scope.onQuickstartFinished = function () {
        workspaceService.savePreference({ quickstart: "viewed" })
    }
    //list
    $scope.isTrashTree = function () {
        return $scope.currentTree.filter == 'trash';
    }
    $scope.isOrderedAsc = function (field) {
        return $scope.order.field === field && !$scope.order.desc;
    }
    $scope.isOrderedDesc = function (field) {
        return $scope.order.field === field && $scope.order.desc;
    }
    //
    $scope.canOpenFile = function () {
        return $scope.selectedDocuments().length == 1 && $scope.selectedFolders().length == 0;
    }
    $scope.canOpenFolder = function () {
        return $scope.selectedDocuments().length == 0 && $scope.selectedFolders().length == 1;
    }
    $scope.isViewMode = function (mode: WorkspacePreferenceView) {
        return template.contains('documents', mode);
    }
    $scope.changeViewMode = function (mode: WorkspacePreferenceView) {
        if (!mode || viewMode == mode) {
            return;
        }
        template.open('documents', mode);
        viewMode = mode;
        workspaceService.savePreference({ view: mode })
    }

    $scope.setHighlighted = function (els) {
        highlighted = els ? [...els] : [];
        highlighted = highlighted.filter(h => !!h);
        setTimeout(() => {
            $scope.removeHighlight(els)
        }, 3100)
        $scope.safeApply();
    }
    $scope.removeHighlight = function (els: models.Element[]) {
        els.forEach(el => {
            highlighted = highlighted.filter(e => !workspaceService.elementEqualsByRefOrId(el, e));
        })
        $scope.safeApply();
    }
    $scope.isHighlighted = function (el) {
        return highlighted.findIndex(e => workspaceService.elementEqualsByRefOrId(e, el)) > -1;
    }

    $scope.currentFolderName = function () {
        return $scope.openedFolder.folder.name;
    }
    $scope.setCurrentFolder = function (folder, reload = false) {
        if (folder !== $scope.openedFolder.folder || reload) {
            $scope.openedFolder = new models.FolderContext(folder);
            $scope.reloadFolderContent();
        }
    }
    $scope.reloadFolderContent = async function () {
        //on refresh folder content => reset search
        $scope.resetSearch();
        //fetch only documents in contents
        let content: models.Element[] = null;
        if ($scope.openedFolder.folder && $scope.openedFolder.folder._id) {
            const parentId = $scope.openedFolder.folder._id;
            content = await workspaceService.fetchDocuments({ filter: $scope.currentTree.filter, parentId, hierarchical: false });
        } else {
            content = await workspaceService.fetchDocuments({ filter: $scope.currentTree.filter, hierarchical: false });
        }
        $scope.openedFolder.setDocuments(content);
        $scope.safeApply();
    }

    $scope.isSelectedFolder = function (folder) {
        return $scope.openedFolder.folder === folder;
    }

    $scope.incrementVisibleFiles = function () {
        $scope.display.nbFiles += 30;
    }

    $scope.selectedDocuments = function () {
        return $scope.openedFolder.documents.filter(f => f.selected);
    };

    $scope.selectedFolders = function () {
        return $scope.openedFolder.folders.filter(f => f.selected);
    };

    $scope.switchSelectAll = function () {
        $scope.openedFolder.documents.forEach(function (document) {
            document.selected = $scope.boxes.selectAll;
        });

        $scope.openedFolder.folders.forEach(function (folder) {
            folder.selected = $scope.boxes.selectAll;
        });
    };

    $scope.setAll = function () {
        let all = true;
        $scope.openedFolder.documents.forEach(function (document) {
            all = all && document.selected;
        });
        $scope.openedFolder.folders.forEach(function (folder) {
            all = all && folder.selected;
        });
        $scope.boxes.selectAll = all;
    };

    $scope.viewFile = function (document) {
        if ($scope.openedFolder) {
            if ($scope.openedFolder.documents)
                $scope.openedFolder.documents.forEach(f => f.selected = false);
            if ($scope.openedFolder.folders)
                $scope.openedFolder.folders.forEach(f => f.selected = false);
        }

        $scope.display.viewFile = document;
        template.open('documents', 'viewer');
    };

    $scope.setFolder = function (key, value) {
        $scope.selectedFolder.name = key;
        $scope.selectedFolder.folder = value;
    };

    let selection = [];
    $scope.selectedItems = function () {
        let sel = $scope.selectedDocuments().concat($scope.selectedFolders());
        if (sel.length != selection.length) {
            selection = sel;
        }
        return selection;
    };
}

