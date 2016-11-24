function MainController($rootScope, $scope, template, lang, model){
	$scope.closePanel = function(){
		$rootScope.$broadcast('close-panel');
	};

	$scope.widgets = model.widgets;

	template.open('main', 'main');
	template.open('widgets', 'widgets');
	template.open('settings', 'settings');
	template.open('notifications', 'notifications');
	template.open('notifspanel', 'notifspanel');

	$scope.template = template;
	$scope.lang = lang;
}

function Timeline($scope, date, model, lang){
	$scope.notifications = [];
	$scope.notificationTypes = model.notificationTypes;
    $scope.registeredNotifications = model.registeredNotifications;
	$scope.translate = lang.translate;
    $scope.filtered = {}

	$scope.actions = {
		discard: {
			label: "timeline.action.discard",
			action: function(notification) {
				notification.opened = false
				notification.discard().done(function() {
					$scope.notifications.remove(notification)
					$scope.$apply()
				})
			},
			condition: function() {
				return model.me.workflow.timeline.discardNotification
			}
		},
		report: {
			label: "timeline.action.report",
			doneProperty: 'reported',
			doneLabel: 'timeline.action.reported',
			action: function(notification) {
				notification.report().done(function() {
					notification.reported = true
					$scope.$apply()
				})
			},
			condition: function(notif) {
				return notif.sender && model.me.workflow.timeline.reportNotification
			}
		}
	}
	$scope.showActions = function(notif) {
		return _.any($scope.actions, function(act){
			return act.condition(notif)
		})
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

function Personalization($rootScope, $scope, model, ui){
	$scope.skins = model.skins;
	$scope.widgets = model.widgets;

	$scope.saveTheme = function(skin, $event){
		$event.stopPropagation();
		skin.setForUser();
		ui.setStyle(skin.path);
	};

	http().get('/languages').done(function(data){
		$scope.languages = data;
    }.bind(this))

	http().get('/userbook/preference/language').done(function(data){
		try{
			if(data.preference){
				$scope.languagePreference = JSON.parse(data.preference)['default-domain']
			}
		} catch(e) {
			$scope.languagePreference = currentLanguage;
		}
		if(!$scope.languagePreference)
			$scope.languagePreference = currentLanguage;

    }.bind(this))

	$scope.saveLang = function(language, $event){
		$event.stopPropagation();
		http().putJson('/userbook/preference/language', { 'default-domain': language}).done(function(){
			location.reload();
		})
	};

	$scope.togglePanel = function($event){
		$scope.showPanel = !$scope.showPanel;
		$event.stopPropagation();
	};

	$scope.display = {};

	$scope.showNotifs = function() {
		$scope.dispaly.showNotifsPanel = true;
	};

	$scope.hideNotifs = function() {
		$scope.dispaly.showNotifsPanel = false;
	};

	$('lightbox[show="display.showNotifsPanel"]').on('click', function(event){
		event.stopPropagation()
	});

	$rootScope.$on('close-panel', function(e){
		$scope.showPanel = false;
	})
}

function Notifications($scope, model, lang){

}

function FlashMessages($scope, model, lang) {
	$scope.currentLanguage = currentLanguage
    $scope.messages = model.flashMessages

    $scope.markMessage = function(message){
        message.markAsRead().done(function(){
            $scope.messages.sync()
            $scope.messages.one('sync', $scope.$apply)
        })
    }
}
