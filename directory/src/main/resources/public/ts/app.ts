import { ng, routes } from 'entcore';
import { classAdminController } from './admin/controller';
import { accountController } from './controllers/account';
import { directoryController } from './controllers/directory';
import { adaptiveHeight } from './directives/adaptiveHeight';
import { intlPhoneInputDirective } from './directives/intlPhoneInput';

routes.define(function ($routeProvider) {
    if (window.location.href.indexOf('mon-compte') !== -1) {
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
    else {
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
            .when('/group-view/:groupId', {
                action: 'viewGroup'
            })
            .otherwise({
                redirectTo: '/myClass'
            });
    }
});

ng.controllers.push(accountController);
ng.controllers.push(classAdminController);
ng.controllers.push(directoryController);
ng.directives.push(adaptiveHeight);
ng.directives.push(intlPhoneInputDirective);