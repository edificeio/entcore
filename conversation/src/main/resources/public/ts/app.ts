import { routes, ng } from 'entcore';
import { conversationController } from './controller';
import {printController} from "./printController";

routes.define(function ($routeProvider) {
    $routeProvider
        .when("/read-mail/:mailId", {
            action: "readMail"
        })
        .when("/write-mail/:userId", {
            action: "writeMail"
        })
        .when('/inbox', {
            action: 'inbox'
        })
        .when('/printMail/:mailId', {
            action: 'viewPrint'
        })
        .otherwise({
            redirectTo: "/inbox"
        })
});

ng.controllers.push(conversationController)
ng.controllers.push(printController);