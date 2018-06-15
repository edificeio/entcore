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
            <div class="row info" ng-if="ngModel.length === 0"><i18n>directory.nofavorite</i18n></div>
            <div ng-if="ngModel.length > 0">
                <input type="search"
                    ng-model="search"
                    placeholder="[[ placeholder ]]"
                    class="text-flow twelve"
                    required ng-minlength="1"/>
                <nav class="removable-list wrapper left-text">
                    <div class="row big-block-container" ng-repeat="item in ngModel" ng-click="selectItem({item: item})">
                        <span class="block cell-ellipsis right-spacing-twice">[[ item.name ]]</span>
                        <i class="trash right-spacing-twice vertical-spacing-four absolute-magnet only-desktop" 
                            ng-click="deleteItem({item: item}); $event.stopPropagation();" 
                            ng-if="removable"/>
                    </div>
                </nav>
            </div>
        `,

        scope: {
            ngModel: '=',
            selectItem: '&',
            deleteItem: '&',
        },

        link: (scope, element, attributes) => {
            scope.placeholder = attributes.placeholder;
            scope.search = '';
            scope.removable = !angular.isUndefined(attributes.deleteItem);
        }
    };
});
