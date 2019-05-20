import {Behaviours, idiom as lang, model} from 'entcore'
import {models} from "../services";

declare let window: any;

export interface LoolDelegateScope {
    onInit(cab: () => void);
    safeApply();
    canBeOpenOnLool(): boolean;
    openOnLool(): void;
    onTreeInit(cb: () => void);
    openFolderById(id: string):Promise<boolean>
    setCurrentTree(tree: models.TREE_NAME);
}

export function LoolDelegate($scope: LoolDelegateScope, $route) {
    $scope.onInit(() => {
        lang.addBundle('/lool/i18n');
        model.me.workflow.load(['lool']);
        Behaviours.load('lool').then(() => {
            Behaviours.applicationsBehaviours.lool.init(() => $scope.safeApply());
            Behaviours.applicationsBehaviours.lool.initPostMessage((event) => {
               const data = JSON.parse(event.data);
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
                           if ('folderId' in $route.current.params) {
                               $scope.openFolderById($route.current.params.folderId)
                           } else {
                               $scope.setCurrentTree("owner")
                           }
                       })
                   }
               }
            });

            $scope.canBeOpenOnLool = Behaviours.applicationsBehaviours.lool.canBeOpenOnLool;
            $scope.openOnLool = Behaviours.applicationsBehaviours.lool.openOnLool;
        });
    });
}
