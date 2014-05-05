//Copyright. Tous droits réservés. WebServices pour l’Education.
function SchoolsController($scope, views){
	$scope.views = views;
	views.open('list', 'table-list');
}