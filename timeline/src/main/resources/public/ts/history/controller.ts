import { ui, http, idiom as lang, ng, skin, $, moment } from 'entcore';

export let historyController = ng.controller('HistoryController', ['$scope', 'model', function HistoryController($scope, model){
	$scope.notifications = [];
	$scope.notificationTypes = model.notificationTypes;
    $scope.registeredNotifications = model.registeredNotifications;
	$scope.translate = lang.translate;
	$scope.me = model.me;
	$scope.filtered = {};
	$scope.display = {};

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

	const loadThemeConf = async function(){
		await skin.listSkins();
		$scope.display.pickTheme = skin.pickSkin;
		$scope.$apply();
	}
	loadThemeConf();

	$scope.formatDate = function(dateString){
		return moment(dateString).calendar();
	};

	$scope.removeFilter = function(){
		if(model.notificationTypes.noFilter){
			model.notificationTypes.deselectAll();
		}
		model.notifications.sync();
	};

	$scope.allFilters = function(){
		if(model.notificationTypes.selection().length === model.notificationTypes.length()){
			model.notificationTypes.deselectAll();
		}else{
			model.notificationTypes.selectAll();
		}

		model.notifications.page = 0;
		model.notifications.lastPage = false;
		model.notifications.all= [];
		model.notifications.sync();
	};

	$scope.unactivesFilters = function(){
		var unactives = model.notificationTypes.length() - model.notificationTypes.selection().length;
		return unactives;
	}

	$scope.loadPage = function(){
		model.notifications.sync(true);
	}

	http().get('/userbook/api/person').done(function(data){
		model.me.email = data.result[0].email;
		$scope.$apply();
	})

	$scope.display = {};

}]);
