import { ng, _ } from 'entcore';

/**
 * @description Display pastilles and a particular search template according to the selected pastille.
 * @param search Function applying a search according to the selected pastille.
 * @param images A string representing an array of string containing the list of images paths.
 * @example
 *  <search-module 
        search="<function>()"
        on-close="<function>()"
        images='["path1", "path2", "path3"]'>
        <div>
            Page 1
        </div>
        <div>
            Page 2
        </div>
        <div>
            Page 3
        </div>
	</search-module>
 */

export const searchModule = ng.directive('searchModule', () => {
    return {
        restrict: 'E',
        transclude: true,
        priority: 100,
        template: `
            <pastilles 
                index="indexForm"
                images="images">
            </pastilles>
            <form name="searchForm" ng-submit="search()" novalidate>
                <article class="twelve cell search reduce-block-six">
                    <div class="spacer-large"></div>
                    <a ng-click="onClose()" class="zero-desktop close-lightbox" ng-show="showClose">
                        <i class="close" />
                    </a>
                    <ng-transclude></ng-transclude>
                </article>
            </form>
        `,

        scope: {
            search: '&',
            onClose: '&',
            showClose: '=',
            indexForm: '='
        },

        link: (scope, element, attributes) => {
            scope.images = JSON.parse(attributes.images).reverse();

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
