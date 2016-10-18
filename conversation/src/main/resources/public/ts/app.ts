import { routes, ng } from 'entcore/entcore';
import { conversationController } from './controller';

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
        .otherwise({
            redirectTo: "/inbox"
        })
});

ng.controllers.push(conversationController)