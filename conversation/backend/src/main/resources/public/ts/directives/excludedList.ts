import { ng, _ } from 'entcore';

export const excludedList = ng.directive('excludedList', () => {
    return {
        restrict: 'E',
        template: `
            <lightbox show="excluded && excluded.length > 0" on-close="handleClose()">
                <h2><i18n>warning.title</i18n></h2>
                <span class="bottom-spacing-twice">
                    <i18n>warning.excluded</i18n>
                </span>
                <table class="twelve">
                    <thead>
                    <tr>
                        <th class="" ng-click=""><i18n>name</i18n></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr ng-repeat="e in excluded">
                        <td class="user">[[e.displayName || e.name]]</td>
                    </tr>
                    </tbody>
                </table>
                <button type="button" class="cancel right-magnet" ng-click="handleClose()"><i18n>warning.close</i18n></button>
            </lightbox>
        `,
        scope: {
            excluded: '='
        },
        link: (scope) => {
            scope.handleClose = () => {
                scope.excluded = [];
                scope.$apply('excluded');
            }
        }
    }
});
