import { ng, _ } from 'entcore';

/**
 * @description Display pastilles and a particular search template according to the selected pastille.
 * @param ngModel ...
 * @example
 * example
 */
export const searchModule = ng.directive('searchModule', () => {
    return {
        restrict: 'E',
        transclude: true,
        template: `
            <pastilles 
                index="indexForm"
                images='["/img/illustrations/group-avatar.svg", "/img/illustrations/group-avatar.svg", "/img/illustrations/group-avatar.svg"]'>
            </pastilles>
            <form name="searchForm" ng-submit="search()" novalidate>
                <article class="twelve cell search reduce-block-six" style="padding-top: 80px;">
                    <ng-transclude></ng-transclude>
                </article>
            </form>
        `,

        scope: {
            search: '&'
        },

        link: (scope, element, attributes) => {
            scope.indexForm = 0;

            var pages = element.find("ng-transclude").children();
            var i, l = pages.length;

            var hideAll = () => {
                for (i = 0; i < l; i++) {
                    pages.eq(i).hide();
                }
            }

            hideAll();
            
            // Pastilles changing index
            scope.$watch("indexForm", function(newValue) {
                hideAll();
                pages.eq(newValue).show();
            });
        }
    };
});
