import { routes, ng } from 'entcore';
import { directory } from './model';
import { accountController } from './controllers/account';
import { classAdminController } from './controllers/classAdmin';
import { directoryController } from './controllers/directory';
import { searchModule } from './directives/searchModule';
import { pastilles } from './directives/pastilles';
import { multiComboboxes } from "./directives/multiComboboxes";
import { removableList } from "./directives/removableList";
import { transferColumns } from "./directives/transferColumns";

routes.define(function($routeProvider){
    if(window.location.href.indexOf('mon-compte') !== -1){
        $routeProvider
            .when('/edit-user/:id', {
                action: 'editUser'
            })
            .when('/edit-user-infos/:id', {
                action: 'editUserInfos'
            })
            .when('/edit-me', {
                action: 'editMe'
            })
            .when('/themes', {
                action: 'themes'
            })
            .otherwise({
                redirectTo: 'edit-me'
            });
    }
    else{
        $routeProvider
            .when('/search', {
                action: 'directory'
            })
            .when('/myClass', {
                action: 'myClass'
            })
            .when("/user-view/:userId", {
                action: "viewUser"
            })
            .when('/:userId', {
                action: 'viewUser'
            })
            .otherwise({
                redirectTo: '/myClass'
            });
    }
});

ng.controllers.push(accountController);
ng.controllers.push(classAdminController);
ng.controllers.push(directoryController);
ng.directives.push(pastilles);
ng.directives.push(searchModule);
ng.directives.push(multiComboboxes);
ng.directives.push(removableList);
ng.directives.push(transferColumns);