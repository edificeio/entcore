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
			action: 'viewSchool'
		})
});

function CommunicationAdmin($scope, route, containers){
	$scope.containers = containers;
	route({
		viewSchool: function(params){
			containers.main = ''
		},
		viewClasses: function(params){

		},
		viewClass: function(params){

		}
	})
}