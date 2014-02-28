routes.define(function($routeProvider){
	$routeProvider
		.when('/view-school', {
			action: 'viewSchool'
		})
		.when('/view-classes', {
			action: 'viewClasses'
		})
		.when('/view-class/:classId', {
			action: 'viewClass'
		})
		.otherwise({
			redirectTo: '/view-school'
		})
});

function CommunicationAdmin($scope, route, views){
	$scope.containers = views.containers;
	route({
		viewSchool: function(params){
			views.openView('view-school', 'main');
		},
		viewClasses: function(params){
			views.openView('view-classes', 'main');
		},
		viewClass: function(params){

		}
	})
}