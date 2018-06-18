import { ng, _, angular } from 'entcore';

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

export const searchModule = ng.directive('searchModule', ['$window', ($window) => {
    return {
        restrict: 'E',
        transclude: true,
        template: `
            <pastilles 
                ng-model="ngModel"
                images="images">
            </pastilles>
            <form name="searchForm" ng-submit="search()" novalidate>
                <article class="twelve cell search reduce-block-six">
                    <div class="spacer-large"></div>
                    <a ng-click="onClose()" class="zero-large-desktop close-lightbox" ng-show="showClose">
                        <i class="close" />
                    </a>
                    <ng-transclude></ng-transclude>
                </article>
            </form>
        `,

        scope: {
            ngModel: '=',
            showClose: '=',
            ngChange: '&',
            search: '&',
            onClose: '&',
        },

        link: (scope, element, attributes) => {
            var imgs = JSON.parse(attributes.images);
            var pages = element.find("ng-transclude").children();
            var i, l = pages.length;
            scope.images = [];
            for (i = 0; i < l; i++) {
                scope.images[i] = {
                    img: imgs[i],
                    visible: true
                };
            }

            var hideAll = () => {
                for (i = 0; i < l; i++) {
                    pages.eq(i).hide();
                }
            }
            hideAll();

            var fillImages = () => {
                for (i = 0; i < l; i++) {
                    scope.images[i].visible = pages.eq(i).children().eq(0).css("display") !== "none";
                }
                scope.$apply();
            }
            
            // Pastilles changing index
            scope.$watch("ngModel", function(newValue) {
                hideAll();
                pages.eq(newValue).show();
                scope.ngChange({ index: newValue });
            });

            angular.element($window).bind('resize', function() {
                fillImages();
            });

            setTimeout(function() {
                fillImages();
            }, 0);
        }
    };
}]);
