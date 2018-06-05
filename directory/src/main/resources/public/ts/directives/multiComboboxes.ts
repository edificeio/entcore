import { ng, _, idiom as lang, $ } from 'entcore';

/**
 * @description Filter with multiple checkboxes.
 * @param ngModel An array of strings containing all the types selected (null if all selected).
 * @param options An array of all the possible option to check. Each option must contain a label and type field.
 * @param titleAll A string representing what should be displayed when all the options are selected.
 * @param title A string reprensenting the begining of what to display when several options are selected.
 * @example
 *  <multi-comboboxes 
        ng-model="<ngModel>"
        options="<options>"
        title-all="<titleAll>"
        title="<title>">
    </multi-comboboxes>
 */

export const multiComboboxes = ng.directive('multiComboboxes', () => {
    return {
        restrict: 'E',
        template: `
            <div class="fluid row">
                <button type="button" class="select-button left-text low-text row" ng-class="{ selected : showOptions }" ng-click="showOptions = !showOptions">
                    <span ng-if="!ngModel" class="block cell-ellipsis right-spacing">[[ titleAll ]]</span>
                    <span ng-if="ngModel" class="block cell-ellipsis right-spacing active">[[ title ]]: [[ ngModel.length ]] <i18n>portal.selected</i18n></span> <i class="sort horizontal-margin top-spacing absolute-magnet"/>
                </button>
                <article ng-show="showOptions" class="absolute-w high-index">
                    <div class="search-pagination flex-row align-center">
                        <div class="cell twelve">
                            <input class="twelve" name="searchField" type="text" ng-model="searchField"
                                i18n-placeholder="search" ng-click="doSearch()"/>
                            <i class="search"></i>
                        </div>
                    </div>
                    <div class="top-spacing-twice left-text smaller-text">
                        <a class="text-underline-hover" ng-click="selectNone()"><i18n>portal.none</i18n></a> <span class="horizontal-margin-small">|</span> <a class="text-underline-hover" ng-click="selectAll()"><i18n>portal.select.all</i18n></a>
                    </div>
                    <div class="top-spacing-twice left-text">
                        <div class="row top-spacing" ng-repeat="option in options | filter:filterByLabel">
                            <label class="wrapping-checkbox relative">
                                <input type="checkbox" ng-model="option.checked" />
                                <i18n class="low-importance">[[ option.label ]]</i18n>
                            </label>
                        </div>
                    </div>
                </article>
            </div>
        `,

        scope: {
            ngModel: '=',
            options: '='
        },

        link: (scope, element, attributes) => {
            scope.titleAll = attributes.titleAll;
            scope.title = attributes.title;

            // When options are available, run !
            scope.$watchCollection("options", function(newValue) {
                scope.options.forEach(option => {
                    option.checked = scope.ngModel ? scope.ngModel.indexOf(option.type) !== -1 : true;

                    scope.$watch(function() { return option; }, function(newValue, oldValue) {
                        if (newValue != oldValue) {
                            if (newValue.checked)
                                scope.ngModel.push(newValue.type);
                            else {
                                if (!scope.ngModel) {
                                    scope.ngModel = [];
                                    scope.options.forEach(o => {
                                        scope.ngModel.push(o.type);
                                    });
                                }
                                scope.ngModel.splice(scope.ngModel.indexOf(newValue.type), 1);
                            }
                            if (scope.ngModel.length === scope.options.length)
                                scope.ngModel = null;
                        }
                    }, true);
                });
            });

            scope.showOptions = false;
            scope.searchField = "";

            // Show / Hide the options on click
            var close = function(e){
				if(element.find(e.target).length > 0)
                    return;
                scope.showOptions = false;
				scope.$apply();
			};
            $('body').on('click', close);

            // Filter labels in search field
            scope.filterByLabel = (option) => {
                return option.label.toLowerCase().includes(scope.searchField.toLowerCase());
            };

            // Select none / all
            scope.select = (checked) => {
                scope.options.forEach(option => {
                    option.checked = checked;
                });
            };
            scope.selectNone = () => {
                scope.select(false);
            };
            scope.selectAll = () => {
                scope.select(true);
            };

            // Placing article correctly
            scope.$watch(function() { return element.find('button').css("height"); }, function(newValue) {
                var article = element.find('article').eq(0);
                article.css('top', (element.find('button')[0].offsetHeight + 1) + 'px');
                article.css('left', '2px');
            });
        }
    };
});