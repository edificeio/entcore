import { ng, idiom as lang, model, http, template } from 'entcore';
import { _ } from 'entcore';

const appController = ng.controller('ApplicationController', ['$scope', ($scope) => {
    template.open('main', 'applications');
    $scope.template = template;
    $scope.lang = lang;
    $scope.bookmarkedApps = model.me.bookmarkedApps;
    $scope.display = {};
    http().get('/applications-list').done(function(app){
        $scope.applications = _.filter(app.apps, function(app){
            return app.display !== false;
        });
        $scope.$apply();
    });

    $scope.addBookmark = function($item){
        if(_.findWhere(model.me.bookmarkedApps, { name: $item.name }) !== undefined){
            return;
        }
        model.me.bookmarkedApps.push($item);
        $scope.$apply();
        http().putJson('/userbook/preference/apps', model.me.bookmarkedApps);
    };

    $scope.removeBookmark = function($item){
        var item = _.findWhere(model.me.bookmarkedApps, { name: $item.name });
        if(item === undefined){
            return;
        }
        var itemIndex = model.me.bookmarkedApps.indexOf(item);
        model.me.bookmarkedApps.splice(itemIndex, 1);
        $scope.$apply();
        http().putJson('/userbook/preference/apps', model.me.bookmarkedApps);
    };

    $scope.filterBookmark = function(item){
        return _.findWhere($scope.bookmarkedApps, {name : item.name})
    }

    $scope.drag = function(item, event){
        event.dataTransfer.setData('application/json', JSON.stringify(item));
    };

    $scope.searchDisplayName = function(item){
        return !$scope.display.searchText ||
                lang.removeAccents(lang.translate(item.displayName)).toLowerCase().indexOf(
                    lang.removeAccents($scope.display.searchText).toLowerCase()
            ) !== -1;
    };

    $scope.order = function(app){
        return lang.translate(app.displayName);
    }
}]);

ng.controllers.push(appController);