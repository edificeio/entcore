import { ng, model, notify, idiom as lang } from 'entcore';
import http from 'axios';

export let termsRevalidationController = ng.controller('TermsRevalidationController', ['$scope', '$window', ($scope, $window) => {

    $scope.chartUrl = lang.translate('auth.charter');
    $scope.cguUrl = lang.translate('cgu.file');

    $scope.validate = async () => {
        await http.put(`/auth/cgu/revalidate`, {}, {headers: {'X-Requested-With': 'XMLHttpRequest'}});
        $window.location.href = '/timeline/timeline' ;
    }
}]);