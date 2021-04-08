import { ng, template, idiom as lang } from 'entcore';
import { appController } from './myapps.controller';

const mainController = ng.controller('MainController', ['$scope', ($scope) => {
    template.open('main', 'applications');
    $scope.template = template;
    $scope.translatedDisplayName = function(app){
        return lang.translate(app.displayName);
    }
}]);

ng.controllers.push(mainController);
ng.controllers.push(appController);
