import { routes, ng } from 'entcore';
import { conversationController } from './controllers/controller';
import { printController } from "./controllers/printController";
import { recipientList } from "./directives/recipientList";
import { switchSearch } from "./directives/switchSearch";

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

ng.controllers.push(conversationController);
ng.controllers.push(printController);
ng.directives.push(recipientList);
ng.directives.push(switchSearch);