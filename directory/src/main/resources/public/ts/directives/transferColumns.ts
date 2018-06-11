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
            <div class="scroll-nine-chips">
                <div class="flex-row">
                    <div class="six">
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
                        <label class="block row twelve chip removable low-importance">
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
            
        }
    };
});