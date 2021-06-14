import { ng, model, notify, idiom as lang } from 'entcore';
import http from 'axios';

export let termsRevalidationController = ng.controller('TermsRevalidationController', ['$scope', ($scope) => {

    $scope.chartUrl = lang.translate('auth.charter');
    $scope.cguUrl = lang.translate('cgu.file');

    $scope.validate = async () => {
        await http.put(`/auth/cgu/revalidate`, {}, {headers: {'X-Requested-With': 'XMLHttpRequest'}});
        document.location.href = '/timeline/timeline';
    }
}]);