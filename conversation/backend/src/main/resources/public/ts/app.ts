import { routes, ng } from 'entcore';
import { conversationController } from './controllers/controller';
import { printController } from "./controllers/printController";
import { recipientList } from "./directives/recipientList";
import { switchSearch } from "./directives/switchSearch";
import { excludedList } from "./directives/excludedList";

routes.define(function ($routeProvider) {
    $routeProvider
        .when("/read-mail/:mailId", {
            action: "readMail"
        })
        .when("/write-mail/:id", {
            action: "writeMail"
        })
        .when("/write-mail/:id/:type", {
            action: "writeMail"
        })
        .when("/write-mail", {
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
ng.directives.push(excludedList);