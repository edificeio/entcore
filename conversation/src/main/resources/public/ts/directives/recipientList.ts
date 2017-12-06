import { ng, _ } from 'entcore';

/**
 * @description Displays chips of items list with a search input and dropDown options. If more than
 * 4 items are in the list, only 2 of them will be shown.
 * @param ngModel The list of items to display in chips.
 * @param ngChange Called when the items list changed.
 * @param updateFoundItems (search, model, founds) Called to update the dropDown content according to
 * the search input.
 * @example
 * <recipient-list
        ng-model="<model>"
        ng-change="<function>()"
        update-found-items="<function>(search, model, founds)">
    </recipient-list>
 */
export const recipientList = ng.directive('recipientList', () => {
    return {
        restrict: 'E',
        template: `
            <label ng-model="ngModel" ng-change="ngChange" class="chip removable" ng-repeat="item in ngModel | limitTo : (needChipDisplay() ? 2 : ngModel.length)" ng-click="deleteItem(item)">
                <span class="cell">[[item.toString()]]</span>
                <i class="close right-magnet"></i>
            </label>
            <label class="chip selected" ng-if="needChipDisplay()" ng-click="giveFocus()">
                <span class="cell">... <i18n>chip.more1</i18n> [[ngModel.length - 2]] <i18n>chip.more2</i18n></span>
            </label>
            <form class="input-help" ng-submit="update(true)">
                <label class="right-magnet" ng-class="{ hide: searchText.length >= 3 }">
                    <i18n>share.search.help1</i18n>[[3 - searchText.length]]<i18n>share.search.help2</i18n>
                </label>
                <input class="chip-input right-magnet" type="text" ng-model="searchText" ng-change="update()" autocomplete="off" ng-class="{ move: searchText.length > 0 }" />
            </form>
            <drop-down
                options="itemsFound"
                ng-change="addItem()"
                on-close="clearSearch()"
                ng-model="currentReceiver">
            </drop-down>
        `,

        scope: { 
            ngModel: '=',
            ngChange: '&',
            updateFoundItems: '&'
        },

        link: (scope, element, attributes) => {
            var firstFocus = true;
            scope.focused = false;
            scope.searchText = '';
            scope.itemsFound = [];
            scope.currentReceiver = 'undefined';

            element.find('input').on('focus', () => {
                if (firstFocus)
                    firstFocus = false;
                scope.focused = true;
                element.addClass('focus');
                element.find('form label').addClass('move');
            });

            element.find('input').on('blur', () => {
                scope.focused = false;
                element.removeClass('focus');
                if(!scope.searchText) {
                    element.find('form label').removeClass('move');
                }
            });

            scope.needChipDisplay = () => {
                return !scope.focused && (typeof scope.ngModel !== 'undefined') && scope.ngModel.length > 3;
            };

            scope.update = (force?: boolean) => {
                if (force) {
                    scope.doSearch();
                }
                else {
                    if(scope.searchText.length < 3) {
                        scope.itemsFound.splice(0, scope.itemsFound.length);
                    }
                    else {
                        scope.doSearch();
                    }
                }
            };

            scope.giveFocus = () => {
                element.find('input').focus();
            };

            scope.addItem = (item) => {
                if (!scope.ngModel) {
                    scope.ngModel = [];
                }
                if (item) {
                    scope.currentReceiver = item;
                }
                scope.ngModel.push(scope.currentReceiver);
                scope.$apply('ngModel');
				scope.$eval(scope.ngChange);
            };

            scope.deleteItem = (item) => {
                scope.ngModel = _.reject(scope.ngModel, function (i) { return i === item; });
                scope.$apply('ngModel');
				scope.$eval(scope.ngChange);
            };

            scope.clearSearch = () => {
                scope.itemsFound = [];
                scope.searchText = '';
            };
            
            scope.doSearch = () => {
                scope.updateFoundItems({search:scope.searchText, model:scope.ngModel, founds:scope.itemsFound});
            };

            // Focus when items list changes
            scope.$watchCollection('ngModel', function(newValue){
                if (!firstFocus) {
                    scope.giveFocus();
                }
            });
            
            // Make the input width be the label help infos width
            setTimeout(function(){
                element.find('input').width(element.find('form label').width());
            }, 0);
        }
    };
});