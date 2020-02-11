import { ng, template, idiom as lang, ui, http, currentLanguage, $, _, moment, skin } from 'entcore';
import * as timelineControllers from './controller';

export let mainController = ng.controller('MainController', ['$rootScope', '$scope', 'model', async ($rootScope, $scope, model) => {
	$scope.closePanel = function(){
		$rootScope.$broadcast('close-panel');
	};

	$scope.widgets = model.widgets;

	template.open('main', 'main');
	template.open('widgets', 'widgets');
	template.open('settings', 'settings');
	template.open('notifications', 'notifications');
	template.open('notifspanel', 'notifspanel');
	$scope.me = model.me;
	$scope.template = template;
	$scope.lang = lang;
	$scope.lightmode = (window as any).LIGHT_MODE;
    $scope.hasWorkflowZimbraExpert = () => {
       return model.me.hasWorkflow('fr.openent.zimbra.controllers.ZimbraController|preauth');
    };
	await skin.listSkins();
	$scope.display = {}
	$scope.display.pickTheme = skin.pickSkin;
}]);

export let timelineController = ng.controller('Timeline', ['$scope', 'model', ($scope, model) => {
	$scope.notifications = [];
	$scope.notificationTypes = model.notificationTypes;
    $scope.registeredNotifications = model.registeredNotifications;
	$scope.translate = lang.translate;
	$scope.filtered = {}
	$scope.config = {
		hideAdminv1Link: false
	}
	$scope.userStructures = model.me.structures;
	if ($scope.userStructures && $scope.userStructures.length == 1) {
		$scope.userStructure = $scope.userStructures[0];
	}
	$scope.switchingFilters = false;

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
				$scope.display.confirmReport = true;
				$scope.doReport = function(notif) {
					notification.report().done(function() {
						notification.reported = true
						$scope.$apply()
					})
				}
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
	$scope.toggleNotificationById=function(id:string, force:boolean){
		const notif = $scope.notifications.all.find(n=>n._id==id);
		notif && $scope.toggleNotification(notif,null,force)
	}
	$scope.toggleNotification=function(notification,$event, force:boolean=null){
		$event && $event.stopPropagation();
		if(force!=null){
			notification.opened = force;
		}else{
			notification.opened = !notification.opened;
		}
	}
	ui.extendSelector.touchEvents('div.notification')
	const  onBodyClick = (event) => {
		event.stopPropagation();
		$('.notification-actions.opened').each((key,value)=>{
			const id = $(value).closest(".notification").attr('data-notificationid');
			$scope.toggleNotificationById(id,false);
		})
		$scope.$apply();
	}
	var applySwipeEvent = function() {
	    $('div.notification').off('swipe-left');
		$('div.notification').off('swipe-right');
		$("body").off("click",onBodyClick)
	    $('div.notification').on('swipe-left', function(event) {
			const id = $(event.delegateTarget).attr('data-notificationid');
			$scope.toggleNotificationById(id,true);
	    })
		$('div.notification').on('swipe-right', function(event) {
			const id = $(event.delegateTarget).attr('data-notificationid');
			$scope.toggleNotificationById(id,false);
		})
		$('body').on('click', onBodyClick);
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
		return moment(dateString).calendar();
	};

	$scope.removeFilter = function(){
		if(model.notificationTypes.noFilter){
			model.notificationTypes.deselectAll();
		}
		model.notifications.sync();
	};

	$scope.allFilters = function(){
		$scope.switchingFilters = true;
		if(model.notificationTypes.selection().length === model.notificationTypes.length()){
			model.notificationTypes.deselectAll();
		}else{
			model.notificationTypes.selectAll();
		}

		model.notifications.page = 0;
		model.notifications.lastPage = false;
		model.notifications.all = [];
		model.notifications.sync(false, () => $scope.switchingFilters = false);
	};

	$scope.isCache = () => (window as any).TIMELINE_CACHE;

	$scope.showSeeMore = () => {
		if($scope.notifications.loading){
			return false;
		}
		return (window as any).TIMELINE_CACHE && model.notifications.page==1 && !model.notifications.lastPage;
	}

	$scope.showSeeMoreOnEmpty = () => {
		try{
			if($scope.notifications.loading){
				return false;
			}
			return (window as any).TIMELINE_CACHE && model.notifications.page==0 && $scope.notifications.all.length === 0 && $scope.notifications.lastPage;
		} catch(e){
			return false;
		}
	}

	$scope.forceLoadPage = () =>{
		$scope.notifications.lastPage = false;
		model.notifications.page++
		$scope.loadPage();
	}
	
	$scope.switchFilter = (type) => {
		$scope.switchingFilters = true;
		type.apply(() => $scope.switchingFilters = false);
	}

	$scope.unactivesFilters = function(){
		var unactives = model.notificationTypes.length() - model.notificationTypes.selection().length;
		return unactives;
	}

	$scope.loadPage = function(){
		model.notifications.sync(true);
	}

	$scope.display = {};

	$scope.suffixTitle = function(type) {
		return lang.translate(type === 'timeline' ? type + '.notification' : type);
	}

	let isAdml = () => {
		return model.me.functions && model.me.functions.ADMIN_LOCAL && model.me.functions.ADMIN_LOCAL.scope;
	}

	let isAdmc = () => {
		return model.me.functions && model.me.functions.SUPER_ADMIN && model.me.functions.SUPER_ADMIN.scope;
	}

	// get platform config about admin version to create admin (v1 or v2) link for report notification
	if (isAdml() || isAdmc()) {
		http()
			.get('/admin/api/platform/config')
			.done(res => {
				$scope.config.hideAdminv1Link = res['hide-adminv1-link'];
			});
	}

	$scope.showAdminv1Link = function() {
		return !$scope.config.hideAdminv1Link;
	}

	$scope.showAdminv2HomeLink = function() {
		return !$scope.showAdminv1Link() && $scope.userStructures && $scope.userStructures.length > 1;
	}

	$scope.showAdminv2AlertsLink = function() {
		return !$scope.showAdminv1Link() && $scope.userStructures && $scope.userStructures.length == 1;
	}

	$scope.allFiltersOn = (): boolean => {
		return $scope.notificationTypes.selection() 
			&& $scope.notificationTypes.all.length > 0
			&& $scope.notificationTypes.selection().length === $scope.notificationTypes.all.length;
	}

	$scope.isEmpty = (): boolean => {
		return $scope.notifications.all 
			&& $scope.notifications.all.length === 0 
			&& $scope.allFiltersOn();
	}

	$scope.noFiltersSelected = (): boolean => {
		return $scope.notificationTypes.selection().length == 0;
	}

	$scope.noResultsWithFilters = (): boolean => {
		return $scope.notifications.all 
			&& $scope.notifications.all.length === 0 
			&& $scope.notificationTypes.selection().length < $scope.notificationTypes.all.length
			&& $scope.notificationTypes.selection().length > 0;
	}
}]);

export let personalizationController = ng.controller('Personalization', ['$rootScope', '$scope', 'model', ($rootScope, $scope, model) => {
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
}]);

export let notificationsController = ng.controller('Notifications', ['$scope', 'model', ($scope, model) => {

}]);

export let flashMessagesController = ng.controller('FlashMessages', ['$scope', 'model', ($scope, model) => {
	$scope.currentLanguage = currentLanguage
    $scope.messages = model.flashMessages

    $scope.markMessage = function(message){
        message.markAsRead().done(function(){
            $scope.messages.sync()
            $scope.messages.one('sync', $scope.$apply)
        })
    }
}]);
