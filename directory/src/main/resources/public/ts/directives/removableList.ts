import { ng, _, angular } from 'entcore';

/**
 * @description A list that can be filtered and items can be removed.
 * @example
 *  
 */

export const removableList = ng.directive('removableList', () => {
    return {
        restrict: 'E',
        template: `
            <div class="row info" ng-if="ngModel.length === 0"><i18n>[[ noitems ]]</i18n></div>
            <div ng-show="ngModel.length > 0">
                <input type="text"
                    ng-model="searchText"
                    placeholder="[[ placeholder ]]"
                    class="text-flow twelve"/>
                <nav class="removable-list wrapper left-text" ng-show="filteredItems.length > 0">
                    <div class="row big-block-container" ng-repeat="item in filteredItems = (ngModel | filter:filterByName)" ng-click="selectItem({item: item})">
                        <span class="block cell-ellipsis right-spacing-twice">[[ item.name ]]</span>
                        <i class="trash right-spacing-twice vertical-spacing-four absolute-magnet only-desktop" 
                            ng-click="deleteItem({item: item}); $event.stopPropagation();" 
                            ng-if="removable"/>
                    </div>
                </nav>
                <div ng-show="filteredItems.length === 0"><span><i18n>[[ noresult ]]</i18n></span></div>
            </div>
        `,

        scope: {
            ngModel: '=',
            selectItem: '&',
            deleteItem: '&',
        },

        link: (scope, element, attributes) => {
            scope.placeholder = attributes.placeholder;
            scope.noitems = attributes.noitems;
            scope.noresult = attributes.noresult;
            scope.removable = !angular.isUndefined(attributes.deleteItem);
            scope.searchText = '';

            // Filter names in search field
            scope.filterByName = (item) => {
                if (!item.name)
                    item.name = '';
                return item.name.toLowerCase().includes(scope.searchText.toLowerCase());
            };
        }
    };
});
