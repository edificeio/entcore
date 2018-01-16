import { ng, _ } from 'entcore';

export const switchSearch = ng.directive('switchSearch', () => {
    return {
        restrict: 'E',
        transclude: true,
        template: `
            <div ng-class="{'hide-search': hide}" class="search-pagination flex-row align-center horizontal-spacing-twicen">
                <a class="zero-mobile" ng-click='cancelSearch()'><i class="close-2x horizontal-spacing hidden-desktop"></i></a>
                <div class="cell">
                    <input class="twelve hidden-desktop" type="text" ng-model="ngModel"
                    ng-keyup="$event.keyCode == 13 ? ngChange({words: ngModel}) : null"
                    i18n-placeholder="search"/>
                    <i class="search flex-row align-center justify-center hidden-desktop" ng-click="hide ? extend() : ngChange({words: ngModel});"></i>
                </div>
                <ng-transclude></ng-transclude>
            </div>
        `,

        scope: { 
            ngModel: '=',
            ngChange: '&',
            cancel: '&'
        },

        link: (scope, element, attributes) => {
            scope.hide = true;

            scope.extend = () => {
                scope.hide = false;
                element.find('.cell').addClass("twelve-mobile");
                element.find('a').removeClass("zero-mobile");
            }

            scope.cancelSearch = () => {
                scope.hide = true;
                scope.ngModel = "";
                element.find('.cell').removeClass("twelve-mobile");
                element.find('a').addClass("zero-mobile");
                scope.cancel();
            }
        }
    };
});