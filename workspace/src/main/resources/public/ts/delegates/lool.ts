import {Behaviours, idiom as lang, model, notify} from 'entcore'
import {models, workspaceService} from "../services";

declare let window: any;

export interface LoolDelegateScope {
    trees: models.ElementTree[]
    onInit(cab: () => void);
    safeApply();
    canBeOpenOnLool(): boolean;
    openOnLool(): void;
    onTreeInit(cb: () => void);
    openFolderById(id: string):Promise<boolean>
    setCurrentTree(tree: models.TREE_NAME);
    display: { loolModal?: boolean, loolModalLoaded?: boolean, loolModalFolderId?: string, loolModalUrl?: string };
    lastRoute: string;
    closeLoolModal($event: any): void;
    onLoolModalError(): void;
    onLoolModalLoad(): void;
    openLoolModal(): void;
    refresh(): void;
}

export function LoolDelegate($scope: LoolDelegateScope, $route, $location) {
    let loolModalTimeout = null;

    $scope.openLoolModal = function () {
        $scope.display.loolModal = true;
        $scope.display.loolModalLoaded = false;
        const folderId = $route.current?.params?.folderId || '';
        $scope.display.loolModalFolderId = folderId;
        $scope.display.loolModalUrl = folderId ? `/lool/modal/create?folderId=${folderId}` : '/lool/modal/create';
    }

    $scope.closeLoolModal = function ($event) {
        if ($event && $event.target && $event.target.classList.contains('lool-modal-overlay')) {
            $scope.display.loolModal = false;
            $location.path($scope.lastRoute ? $scope.lastRoute.split('#')[1] : '/');
        }
    }

    $scope.onLoolModalError = function () {
        if (loolModalTimeout) {
            clearTimeout(loolModalTimeout);
            loolModalTimeout = null;
        }
        //$scope.display.loolModal = false;
        //notify.error(lang.translate("workspace.lool.modal.error"));
        //$location.path($scope.lastRoute ? $scope.lastRoute.split('#')[1] : '/');
        console.error("Could not load lool modal")
    }

    $scope.onLoolModalLoad = function () {
        if (loolModalTimeout) {
            clearTimeout(loolModalTimeout);
        }
        loolModalTimeout = setTimeout(function() {
            if ($scope.display.loolModal) {
                $scope.safeApply();
                $scope.onLoolModalError();
            }
        }, 5000);
    }

    $scope.onInit(() => {
        lang.addBundle('/lool/i18n');
        model.me.workflow.load(['lool']);
        Behaviours.load('lool').then(() => {
            Behaviours.applicationsBehaviours.lool.init(() => $scope.safeApply());
            Behaviours.applicationsBehaviours.lool.initPostMessage((event) => {
               let data;
               try {
                   data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
               } catch (e) {
                   console.error("failed to parse window message", e)
                   return;
               }
               switch (data.id) {
                   case 'lool@getFolder': {
                       const response: any = {id: 'lool@getFolderResponse'};
                       if ('folderId' in $route.current.params) {
                           response.folderId = $route.current.params.folderId;
                       }
                       window.postMessage(JSON.stringify(response), window.location.origin);
                   }
                   break;
                   case 'lool@resync': {
                       $scope.onTreeInit(() => {
                           workspaceService.onChange.next({action:"tree-change"});
                           if ('folderId' in $route.current.params) {
                               $scope.openFolderById($route.current.params.folderId)
                           } else {
                               $scope.setCurrentTree("owner")
                           }
                       })
                   }
                   break;
                    case 'lool@close-modal': {
                        ($scope as any).display.loolModal = false;
                        $scope.refresh();
                        $scope.safeApply();
                    }
                    break;
                   case 'lool@modal-ready': {
                       if (loolModalTimeout) {
                           clearTimeout(loolModalTimeout);
                           loolModalTimeout = null;
                       }
                       $scope.display.loolModalLoaded = true;
                       $scope.safeApply();
                   }
               }
            });

            $scope.canBeOpenOnLool = Behaviours.applicationsBehaviours.lool.canBeOpenOnLool;
            $scope.openOnLool = Behaviours.applicationsBehaviours.lool.openOnLool;
        });
    });
}
