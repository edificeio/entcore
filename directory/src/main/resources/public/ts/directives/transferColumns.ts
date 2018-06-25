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
                    <div class="circle square-normal red right-magnet right-spacing" 
                        ng-click="removeAllItems()" 
                        ng-if="ngModel.length > 0"
                        tooltip="portal.all.remove">
                        <i class="close white-text centered-text block"></i>
                    </div>
                </div>
            </div>
            <div class="flex-row">
                <div class="six scroll-nine-chips" bottom-scroll="updatingMaxLeftItems()">
                    <div class="row info" ng-if="searchedItems.length === 0 && !loading"><i18n>portal.enter.criterias.search</i18n></div>
                    <div class="row centered-text reduce-block-six" ng-if="loading">
                        <img skin-src="/img/illustrations/loading.gif" width="30px" heigh="30px"/>
                    </div>
                    <contact-chip class="block relative movable" 
                        ng-model="item"
                        action="addItem(item)"
                        ng-class="{ 'divide-opacity': item.selected, 'chip-hover': !item.selected }"
                        ng-repeat="item in searchedItems | limitTo:maxLeftItems">
                    </contact-chip>
                </div>
                <div class="horizontal-margin-twice divider-border"></div>
                <div class="six scroll-nine-chips" bottom-scroll="updatingMaxRightItems()">
                    <div class="flex-row warning" ng-if="textRightWarningTitle && ngModel.length === 0">
                        <div><i class="warning"></i></div>
                        <div>
                            <div>[[ textRightWarningTitle ]]</div>
                            <div>[[ textRightWarningDescription ]]</div>
                        </div>
                    </div>
                    <div class="row info" ng-if="ngModel.length === 0 && searchedItems.length > 0"><i18n>portal.select.users.criterias</i18n></div>
                    <contact-chip class="block relative removable chip-hover" stickerNotSelected
                        ng-model="item"
                        action="removeItem(item)"
                        ng-repeat="item in ngModel | limitTo:maxRightItems">
                    </contact-chip>
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

            scope.initMaxLeftItems = function() {
                scope.maxLeftItems = 50;
            };

            scope.initMaxRightItems = function() {
                scope.maxRightItems = 50;
            };

            scope.updatingMaxLeftItems = function() {
                scope.maxLeftItems += 50;
            };

            scope.updatingMaxRightItems = function() {
                scope.maxRightItems += 50;
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
                scope.initMaxLeftItems();
                scope.searchedItems.forEach(searched => {
                    scope.ngModel.forEach(item => {
                        if (searched.id === item.id)
                            searched.selected = true;
                    });
                });
            });

            scope.$watchCollection("ngModel", function() {
                scope.initMaxRightItems();
            });

            scope.initMaxLeftItems();
            scope.initMaxRightItems();
        }
    };
});