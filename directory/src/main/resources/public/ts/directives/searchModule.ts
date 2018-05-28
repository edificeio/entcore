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
        template: `
            <pastilles images='["/img/illustrations/group-avatar.svg", "/img/illustrations/group-avatar.svg", "/img/illustrations/group-avatar.svg"]'>
            </pastilles>
            <form name="searchForm" ng-submit="ngChange()" novalidate>
                <div class="twelve cell search reduce-block-six border-shadow" style="padding-top: 50px;">
        
                    <div class="seven centered row centered-text twelve-mobile">
                        <input type="search"
                                ng-model="ngModel"
                                translate attr="placeholder"
                                placeholder="userBook.search"
                                class="nine text-flow"
                                required ng-minlength="1"/>
                        <input type="submit" value="OK" ng-disabled="searchForm.$invalid" class="text-flow two"/>
        
                        <select class="five styled-combo-box" ng-model="structure" ng-options="school.id as school.name for school in schools">
                            <option value="" selected="selected" translate content="directory.allStructures"></option>
                        </select>
        
                        <select class="five styled-combo-box" ng-model="profile">
                            <option value="" selected="selected" translate content="directory.allProfiles"></option>
                            <option value="Teacher" translate content="directory.Teacher"></option>
                            <option value="Personnel" translate content="directory.Personnel"></option>
                            <option value="Relative" translate content="directory.Relative"></option>
                            <option value="Student" translate content="directory.Student"></option>
                            <option value="Guest" translate content="directory.Guest"></option>
                        </select>
        
                    </div>
        
                    <div class="one cell">&nbsp</div>
        
                </div>
            </div>
        `,

        scope: {
            ngModel: '=',
            ngChange: '&',
            structure: '=',
            profile: '=',
            schools: '='
        },

        link: (scope, element, attributes) => {

        }
    };
});
