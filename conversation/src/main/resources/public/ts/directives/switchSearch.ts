import { ng, _ } from 'entcore';

export const switchSearch = ng.directive('switchSearch', () => {
    return {
        restrict: 'E',
        transclude: true,
        template: `
            <div ng-class="{'hide-search': hide}" class="flex-row align-center justify-between search-pagination">
                <a class="zero mobile-fat-mobile" ng-click='cancelSearch()'><i class="close horizontal-spacing"></i></a>
                <div class="cell">
                    <input class="twelve mobile-fat-mobile" type="text" ng-model="ngModel"
                    ng-keyup="$event.keyCode == 13 ? ngChange({words: ngModel}) : null"
                    i18n-placeholder="search.condition"/>
                    <i class="search mobile-fat-mobile flex-row align-center justify-center" ng-click="hide ? extend() : ngChange({words: ngModel});"></i>
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

                // element.find('.cell').addClass("twelve-mobile");
                // element.find('a').removeClass("zero-mobile");

                element.find('a').removeClass("zero");
                element.find('.cell').addClass("twelve");
            }

            scope.cancelSearch = () => {
                scope.hide = true;
                scope.ngModel = "";

                element.find('.cell').removeClass("twelve");
                element.find('a').addClass("zero");

                //element.find('.cell').removeClass("twelve-mobile");
                //element.find('a').addClass("zero-mobile");
                scope.cancel();
            }
        }
    };
});
