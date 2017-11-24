import { ng, template } from 'entcore';
import { Mail } from './model';

export let printController = ng.controller('PrintController', [
    '$scope', 'route', 'model',
    function ($scope, route, model, ) {
        route({
            viewPrint: async function(params){
                $scope.mail = new Mail(params.mailId);

                try{
                    await $scope.mail.open(true);
                    $scope.$apply();
                    setTimeout(function(){
                        window.print();
                    }, 1000)
                }
                catch(e){
                    console.log(e);
                    template.open('page', 'errors/e404');
                }
            }
        })
    }
]);