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
                    <div class="circle square-normal green right-magnet right-spacing-three" ng-click="search()" ng-if="searchedItems.length > 0">
                        <i class="right-arrow white-text centered-text block"></i>
                    </div>
                </div>
                <div class="six">
                    <span class="medium-importance left-spacing-twice">[[ textTitle ]]</span>
                    <div class="circle square-normal red right-magnet right-spacing" ng-click="search()" ng-if="ngModel.length > 0">
                        <i class="close white-text centered-text block"></i>
                    </div>
                </div>
            </div>
            <div class="scroll-nine-chips">
                <div class="flex-row">
                    <div class="six">
                        <div class="row info" ng-if="searchedItems.length === 0">[[ textLeftInfo ]]</div>
                        <label class="block row twelve chip movable low-importance" ng-repeat="item in searchedItems">
                            <span class="cell round square-small group">
                                <img skin-src="/img/illustrations/group-avatar.svg"/>
                            </span>
                            <span class="cell circle square-mini purple"></span>
                            <span class="cell-ellipsis block left-text">Tous les élèves du groupe 4TRIS</span>
                            <i class="right-arrow absolute-magnet"></i>
                        </label>
                    </div>
                    <div class="horizontal-margin-twice divider-border"></div>
                    <div class="six">
                        <div class="flex-row warning" ng-if="ngModel.length === 0">
                            <div><i class="warning"></i></div>
                            <div>
                                <div>[[ textRightWarningTitle ]]</div>
                                <div>[[ textRightWarningDescription ]]</div>
                            </div>
                        </div>
                        <label class="block row twelve chip removable low-importance" ng-repeat="item in ngModel">
                            <span class="cell round square-small group">
                                <img skin-src="/img/illustrations/group-avatar.svg"/>
                            </span>
                            <span class="cell circle square-mini purple"></span>
                            <span class="cell-ellipsis block left-text">Tous les élèves du groupe 4TRIS</span>
                            <i class="close absolute-magnet"></i>
                        </label>
                    </div>
                </div>
            </div>
        `,

        scope: {
            ngModel: '=',
            searchedItems: '='
        },

        link: (scope, element, attributes) => {
            scope.textTitle = attributes.textTitle;
            scope.textLeftInfo = attributes.textLeftInfo;
            scope.textRightWarningTitle = attributes.textRightWarningTitle;
            scope.textRightWarningDescription = attributes.textRightWarningDescription;
        }
    };
});