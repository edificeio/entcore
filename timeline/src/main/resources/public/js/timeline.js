function Timeline($scope, date, notify){
	$scope.notifications = [];
	notify.info('test!');
	One.get('lastNotifications').done(function(response){
		$scope.notifications = response.results;
		$scope.$apply();
	});

	$scope.formatDate = function(dateString){
		return date.calendar(dateString);
	}
}
