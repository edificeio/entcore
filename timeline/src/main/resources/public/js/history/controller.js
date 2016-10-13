model.notifications = { mine: true }
function HistoryController($scope, date, model, lang){
	$scope.notifications = [];
	$scope.notificationTypes = model.notificationTypes;
    $scope.registeredNotifications = model.registeredNotifications;
	$scope.translate = lang.translate;
    $scope.filtered = {}

	$scope.actions = {
		delete: {
			label: "timeline.action.delete.own",
			action: function(notification){
				notification.opened = false
				notification.delete().done(function(){
					$scope.notifications.remove(notification)
					$scope.$apply()
				})
			}
		}
	}

	ui.extendSelector.touchEvents('div.notification')
	var applySwipeEvent = function() {
	    $('div.notification').off('swipe-left')
		$('div.notification').off('swipe-right')
	    $('div.notification').on('swipe-left', function(event) {
	        $(event.delegateTarget).find('.notification-actions').addClass('opened')
	    })
		$('div.notification').on('swipe-right', function(event) {
	        $(event.delegateTarget).find('.notification-actions').removeClass('opened')
	    })
	}

	model.on('notifications.change, notificationTypes.change', function(e){
		applySwipeEvent()
		if(!$scope.$$phase){
			$scope.$apply('notifications');
			$scope.$apply('notificationTypes');
		}
	});

	lang.addBundle('/timeline/i18nNotifications', function(){
		$scope.notifications = model.notifications;
		$scope.$apply('notifications');
	});

	$scope.formatDate = function(dateString){
		return date.calendar(dateString);
	};

	$scope.removeFilter = function(){
		if(model.notificationTypes.noFilter){
			model.notificationTypes.deselectAll();
		}
		model.notifications.sync();
	};

	$scope.loadPage = function(){
		model.notifications.sync(true);
	}
}
