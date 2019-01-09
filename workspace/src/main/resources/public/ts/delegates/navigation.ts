import { template, moment, idiom as lang } from "entcore";
import { models, workspaceService, WorkspacePreferenceView } from "../services";
import { Subject, Observable } from "rxjs";

export interface NavigationDelegateScope {
    //
    infotipVisible: boolean
    display: { viewFile?: models.Element, nbFiles: number, editedImage?: models.Element, editImage?: boolean, quickStart?: boolean }
    boxes: { selectAll: boolean }
    openedFolder: models.FolderContext
    selectedFolder: { folder: models.Element, name: string }
    onQuickstartFinished()
    //order
    order: { field: string, desc: boolean, order?: (item: models.Element) => any }
    orderByField(fieldName: string, desc?: boolean, save?: boolean)
    applySort();
    //list view
    onInfotipChange(visible: boolean)
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
    getImageUrl(doc: models.Element)
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
    closeViewFile()
    viewFile(document: models.Element)
    setFolder(key: string, folder: models.Element)
    setAll();
    selectedItems(): models.Element[];
    isSelectedFolder(folder: models.Element): boolean
    setCurrentFolder(folder: models.Element, reload?: boolean)
    onReloadContent:Subject<() => void>
    // from others 
    currentTree: models.Tree;
    trees: models.Tree[]
    safeApply(a?)
    $watch(a: any, f: Function)
    onInit(cab: () => void);
}
export function NavigationDelegate($scope: NavigationDelegateScope, $location, $anchorScroll, $timeout) {
    let highlighted: models.Element[] = [];
    let viewMode: WorkspacePreferenceView = null;
    $scope.onReloadContent = new Subject;
    $scope.onInit(function () {
        //INIT 
        $scope.openedFolder = new models.FolderContext;
        $scope.boxes = { selectAll: false }
        $scope.selectedFolder = { folder: models.emptyFolder(), name: '' };
        $scope.changeViewMode("icons");
        workspaceService.getPreference().then(pref => {
            $scope.changeViewMode(pref.view)
            $scope.display.quickStart = pref.quickstart == "notviewed";
            if (pref.sortField) {
                $scope.orderByField(pref.sortField, pref.sortDesc, false);
            }
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
    //
    $scope.onQuickstartFinished = function () {
        workspaceService.savePreference({ quickstart: "viewed" })
    }
    $scope.getImageUrl = function (document) {
        return `${document.icon}?thumbnail=120x120&v=${document.version}`
    }
    //order
    $scope.order = {
        field: 'name',
        desc: false,
        order(item) {
            if (item[$scope.order.field] && ["created", "date", "modified"].indexOf($scope.order.field) > -1) {
                return moment(item[$scope.order.field], "YYYY-MM-DD HH:mm:ss.SSS").toDate().getTime();
            }
            if ($scope.order.field === 'name') {
                return lang.removeAccents(item[$scope.order.field]);
            }
            if ($scope.order.field.indexOf('.') >= 0) {
                const splitted_field = $scope.order.field.split('.')
                let sortValue = item
                for (let i = 0; i < splitted_field.length; i++) {
                    sortValue = typeof sortValue === 'undefined' ? undefined : sortValue[splitted_field[i]]
                }
                return sortValue
            } else
                return item[$scope.order.field];
        }
    }
    $scope.orderByField = function (fieldName, desc = null, save = true) {
        if (fieldName === $scope.order.field) {
            $scope.order.desc = !$scope.order.desc;
        }
        else {
            $scope.order.desc = false;
            $scope.order.field = fieldName;
        }
        if (desc != null && desc != undefined) {
            $scope.order.desc = desc;
        }
        if (save) {
            workspaceService.savePreference({ sortDesc: $scope.order.desc, sortField: $scope.order.field })
        }

        $scope.applySort();

        $timeout(function () {
            if ($location.hash() !== 'start') {
                $location.hash('start');
            }
            else {
                $anchorScroll();
            }
        });
    };
    $scope.applySort = function () {
        $scope.openedFolder.applySort((item1,item2) => $scope.order.order(item1) < $scope.order.order(item2) != $scope.order.desc ? -1 : 1);
    }
    //list
    $scope.onInfotipChange = function (visible) {
        $scope.infotipVisible = visible;
    }
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
        if (mode != "carousel") {
            workspaceService.savePreference({ view: mode })
        }
    }
    let timeout = null;
    $scope.setHighlighted = function (els) {
        highlighted = els ? [...els] : [];
        highlighted = highlighted.filter(h => !!h);
        timeout && clearTimeout(timeout)
        timeout = setTimeout(() => {
            $scope.removeHighlight(highlighted)
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
            $scope.applySort();
            $scope.reloadFolderContent();
        }
    }
    //
    /**aply a debounce time to avoid reloading content every time (sync bugs + optimize perf)**/
    let reloadSubject = new Subject();
    (reloadSubject as Observable<any>).debounceTime(350).subscribe(async e => {
        //on refresh folder content => reset search
        $scope.onReloadContent.next();
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
    })
    $scope.reloadFolderContent = function () {
        reloadSubject.next();
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
        let one = false;
        $scope.openedFolder.documents.forEach(function (document) {
            all = all && document.selected;
            one = one || document.selected;
        });
        $scope.openedFolder.folders.forEach(function (folder) {
            all = all && folder.selected;
            one = one || folder.selected;
        });
        $scope.boxes.selectAll = all;
        one = one && !all;
        // Not the Angular way, but would be overcomplicated to access indeterminate property of parent checkbox.
        (<HTMLInputElement> document.getElementById("parent-checkbox")).indeterminate = one;
    };
    $scope.closeViewFile = function () {
        if (template.contains('documents', 'viewer')) {
            template.open("documents", viewMode);
        }
    }
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

