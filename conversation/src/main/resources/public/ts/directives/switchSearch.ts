import { ng, _ } from 'entcore';

export const switchSearch = ng.directive('switchSearch', () => {
    return {
        restrict: 'E',
        transclude: true,
        template: `
            <div ng-class="{'hide-search': hide}" class="search-pagination flex-row justify-start mobile-justify-center horizontal-spacing-twicen">
                <div class="cell">
                    <input class="twelve" type="text" ng-model="ngModel"
                    ng-keyup="$event.keyCode == 13 ? ngChange({words: ngModel}) : null"
                    i18n-placeholder="search"/>
                    <i class="search flex-row align-center justify-center" ng-click="hide ? extend() : ngChange({words: ngModel});"></i>
                </div>
                <a><i class="close-2x horizontal-spacing" ng-click=''></i></a>
                <ng-transclude></ng-transclude>
            </div>
        `,

        scope: { 
            ngModel: '=',
            ngChange: '&'
        },

        link: (scope, element, attributes) => {
            scope.hide = true;
            //element.find('input').
            //twelve twelve-mobile 
            // <div class="cell vertical-spacing">

            scope.extend = () => {
                scope.hide = false;
                element.find('.cell').addClass("twelve twelve-mobile");
            }
        }
    };
});