import { ng, _ } from 'entcore';

/**
 * @description A list that can be filtered and items can be removed.
 * @example
 *  
 */

export const transferColumns = ng.directive('transferColumns', () => {
    return {
        restrict: 'E',
        template: `
            <div class="flex-row bottom-spacing">
                <div class="six">
                    <div class="circle square-normal green right-magnet right-spacing-three"
                        ng-click="addAllItems()" 
                        ng-if="searchedItems.length > 0"
                        tooltip="portal.all.add">
                        <i class="right-arrow white-text centered-text block"></i>
                    </div>
                </div>
                <div class="six">
                    <span class="medium-importance left-spacing-twice">[[ textTitle ]]</span>
                    <div class="circle square-normal red right-magnet right-spacing-twice" 
                        ng-click="removeAllItems()" 
                        ng-if="ngModel.length > 0"
                        tooltip="portal.all.remove">
                        <i class="close white-text centered-text block"></i>
                    </div>
                </div>
            </div>
            <div class="scroll-nine-chips" bottom-scroll="updatingMaxItems()">
                <div class="flex-row">
                    <div class="six">
                        <div class="row info" ng-if="searchedItems.length === 0 && !loading"><i18n>portal.enter.criterias.search</i18n></div>
                        <div class="row centered-text reduce-block-six" ng-if="loading">
                            <img skin-src="/img/illustrations/loading.gif" width="30px" heigh="30px"/>
                        </div>
                        <label class="block row twelve chip movable low-importance" ng-repeat="item in searchedItems | limitTo:maxItems" 
                            ng-if="!loading" ng-class="{ 'divide-opacity': item.selected, 'chip-hover': !item.selected }" ng-click="addItem(item)">
                            <span class="cell round square-small" ng-class="{ group: item.name }">
                                <img ng-if="item.name" skin-src="/img/illustrations/group-avatar.svg"/>
                                <img ng-if="!item.name" ng-src="/userbook/avatar/[[item.id]]?thumbnail=100x100"/>
                            </span>
                            <span ng-if="!item.name" class="cell circle square-mini" ng-class="getColor({profile: item.profile})"></span>
                            <span ng-if="item.name" class="cell-ellipsis block left-text">[[ item.name ]]</span>
                            <span ng-if="!item.name" class="cell-ellipsis block left-text">[[ item.displayName ]]</span>
                            <i class="right-arrow absolute-magnet" ng-if="!item.selected"></i>
                        </label>
                    </div>
                    <div class="horizontal-margin-twice divider-border"></div>
                    <div class="six">
                        <div class="flex-row warning" ng-if="textRightWarningTitle && ngModel.length === 0">
                            <div><i class="warning"></i></div>
                            <div>
                                <div>[[ textRightWarningTitle ]]</div>
                                <div>[[ textRightWarningDescription ]]</div>
                            </div>
                        </div>
                        <div class="row info" ng-if="ngModel.length === 0 && searchedItems.length > 0"><i18n>portal.select.users.criterias</i18n></div>
                        <label class="block row twelve chip chip-hover removable low-importance" ng-repeat="item in ngModel | limitTo:maxItems" 
                            ng-click="removeItem(item)">
                            <span class="cell round square-small" ng-class="{ group: item.name }">
                                <img ng-if="item.name" skin-src="/img/illustrations/group-avatar.svg"/>
                                <img ng-if="!item.name" ng-src="/userbook/avatar/[[item.id]]?thumbnail=100x100"/>
                            </span>
                            <span ng-if="!item.name" class="cell circle square-mini" ng-class="getColor({profile: item.profile})"></span>
                            <span ng-if="item.name" class="cell-ellipsis block left-text">[[ item.name ]]</span>
                            <span ng-if="!item.name" class="cell-ellipsis block left-text">[[ item.displayName ]]</span>
                            <i class="close absolute-magnet"></i>
                        </label>
                    </div>
                </div>
            </div>
        `,

        scope: {
            ngModel: '=',
            searchedItems: '=',
            loading: '=',
            getColor: '&'
        },

        link: (scope, element, attributes) => {
            scope.textTitle = attributes.textTitle;
            scope.textRightWarningTitle = attributes.textRightWarningTitle;
            scope.textRightWarningDescription = attributes.textRightWarningDescription;

            scope.initMaxItems = function() {
                scope.maxItems = 50;
            };

            scope.updatingMaxItems = function() {
                scope.maxItems += 50;
            };

            scope.addItem = function(item) {
                if (!item.selected) {
                    item.selected = true;
                    scope.ngModel.push(item);
                }
            };

            scope.addAllItems = function() {
                scope.searchedItems.forEach(item => {
                    scope.addItem(item);
                });
            };

            scope.removeItem = function(item) {
                item.selected = false;
                scope.ngModel.splice(scope.ngModel.indexOf(item), 1);
                scope.searchedItems.forEach(searched => {
                    if (searched.id === item.id)
                        searched.selected = false;
                });
            };

            scope.removeAllItems = function() {
                for (var i = scope.ngModel.length - 1; i >= 0; i--) {
                    scope.removeItem(scope.ngModel[i]);
                }
            };

            scope.$watchCollection("searchedItems", function() {
                scope.initMaxItems();
                scope.searchedItems.forEach(searched => {
                    scope.ngModel.forEach(item => {
                        if (searched.id === item.id)
                            searched.selected = true;
                    });
                });
            });

            scope.initMaxItems();
        }
    };
});