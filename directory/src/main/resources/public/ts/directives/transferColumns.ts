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
            <div class="flex-row">
                <div class="six">
                    TODO
                </div>
                <div class="horizontal-margin-twice divider-border"></div>
                <div class="six">
                    TODO
                </div>
            </div>
        `,

        scope: {

        },

        link: (scope, element, attributes) => {
            
        }
    };
});