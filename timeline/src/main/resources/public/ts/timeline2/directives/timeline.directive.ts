import { IAttributes, IController, IDirective, IScope } from "angular";
import { L10n, conf, http, session, TrackingService, TrackedAction, TrackedActionFromWidget } from "ode-ngjs-front";
import  gsap = require("gsap");
import { ITimelineFactory, ITimelineNotification } from "ode-ts-client";
import * as $ from "jquery";
import { TRACK } from "../tracking/events";

/* Controller for the directive */
export class TimelineController implements IController {
    private me = session().user;
	public lang =  conf().Platform.idiom;

	public savePrefsAndReload: () => Promise<void>;
	public handleLoadPageClick: (force: boolean) => Promise<void>;

	constructor( public tracker:TrackingService ) {
        if (this.userStructures && this.userStructures.length == 1) {
            this.userStructure = this.userStructures[0];
        }
    }

    app = ITimelineFactory.createInstance();

	selectedFilter = {};	// ng-model for filters chip.

	config = {
		hideAdminv1Link: false
	};
    userStructure = null;
	userStructures = this.me.structures;

	get isAdml() {
		return this.me.functions && this.me.functions.ADMIN_LOCAL && this.me.functions.ADMIN_LOCAL.scope;
	}

	get isAdmc() {
		return this.me.functions && this.me.functions.SUPER_ADMIN && this.me.functions.SUPER_ADMIN.scope;
	}

	showAdminv1Link() {
		return !this.config.hideAdminv1Link;
	}

	showAdminv2HomeLink() {
		return !this.showAdminv1Link() && this.userStructures && this.userStructures.length > 1;
	}

	showAdminv2AlertsLink() {
		return !this.showAdminv1Link() && this.userStructures && this.userStructures.length == 1;
	}

	public async initialize() {
		const admx:Promise<any> = (this.isAdml || this.isAdmc)
		// get platform config about admin version to create admin (v1 or v2) link for report notification
		? http().get('/admin/api/platform/config')
			.then(res => {
				this.config.hideAdminv1Link = res['hide-adminv1-link'];
			})
		: Promise.resolve();

		await Promise.all([
			this.app.initialize(),
			admx
		]);
	}

	public isAllSelected:boolean = false;	// ng-model for the "Select All / none" chip

	public get canDiscard():boolean {
		return session().hasWorkflow("org.entcore.timeline.controllers.TimelineController|discardNotification");
	}
	public doDiscard( notif:ITimelineNotification ) {
		if( this.canDiscard ) {
			const idx = this.app.notifications.findIndex( n => n._id===notif._id );
			if( idx >= 0 ) {
				this.app.notifications.splice( idx, 1 );
				notif.discard();

				// #50542: Track this event.
				const evt = TRACK.NOTIF_DELETE;
				if( this.tracker.willTrackEvent(evt.CLICK) ) {
					this.tracker.trackEvent( TRACK.event, evt.action, evt.CLICK );
				}
			}
		}
	}

	display:{
		confirmReport:boolean;
	} = {
		confirmReport: false
	};
	currentNotification:ITimelineNotification;

	public canReport( notif:ITimelineNotification ):boolean {
		return notif.model.sender && session().hasWorkflow("org.entcore.timeline.controllers.TimelineController|reportNotification");
	}
	public confirmReport( notif:ITimelineNotification ) {
		if( this.canReport(notif) ) {
			this.currentNotification = notif;
			this.display.confirmReport = true;
		}
	}
	public doReport() {
		// #50542: Track this event.
		const evt = TRACK.NOTIF_SIGNAL;
		if( this.tracker.willTrackEvent(evt.CLICK) ) {
			this.tracker.trackEvent( TRACK.event, evt.action, evt.CLICK );
		}

		this.currentNotification.report().then( () => {
			this.currentNotification.model.reported = true;
			this.currentNotification = null;
		});
	}

/*
	actions = {
		discard = {
			label: "timeline.action.discard",
			action: (notification) => {
				notification.opened = false
				notification.discard().done(function() {
					notifications.remove(notification)
					$scope.$apply()
				})
			},
			condition: () => {
				return this.me.workflow.timeline.discardNotification
			}
		} as Action,
		report: {
			label: "timeline.action.report",
			doneProperty: 'reported',
			doneLabel: 'timeline.action.reported',
			action: function(notification) {
				$scope.display.confirmReport = true;
				$scope.doReport(notif) {
					notification.report().done(function() {
						notification.reported = true
						$scope.$apply()
					})
				}
			},
			condition: function(notif) {
				return notif.sender && model.me.workflow.timeline.reportNotification
			}
		}  as Action
	}
	showActions(notif) {
		return Object.values( this.actions ).filter( (act:Action) => act.condition(notif) );
	}
	toggleNotificationById(id:string, force:boolean){
		const notif = this.app.notifications.all.find(n=>n._id==id);
		notif && this.toggleNotification(notif,null,force);
	}
	toggleNotification(notification, $event, force:boolean=null){
		$event && $event.stopPropagation();
        notification.opened = (force!=null) ? force : !notification.opened;
	}
*/

    /* FIXME mobile swipe events
	ui.extendSelector.touchEvents('div.notification')
	const  onBodyClick = (event) => {
		event.stopPropagation();
		$('.notification-actions.opened').each((key,value)=>{
			const id = $(value).closest(".notification").attr('data-notificationid');
			this.toggleNotificationById(id,false);
		})
		$scope.$apply();
	}
	var applySwipeEvent() {
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
    */

	public lightmode: boolean = false;
	public isCache:boolean = false;

	showSeeMore() {
		if(this.app.isLoading){
			return false;
		}
		return this.app.hasMorePage;
	}

	showSeeMoreOnEmpty() {
		try{
			if(this.app.isLoading){
				return false;
			}
			return this.isCache && this.app.page===0 && this.app.notifications.length===0 && !this.app.hasMorePage;
		} catch(e){
			return false;
		}
	}

	noResultsWithFilters():boolean {
		return this.app.notifications
			&& this.app.notifications.length === 0 
			&& this.app.selectedNotificationTypes.length < this.app.notificationTypes.length
			&& this.app.selectedNotificationTypes.length > 0;
	}

	loadPage( force?:boolean ): Promise<void> {
		return this.app.loadNotifications( force );
	}

/*

	unactivesFilters(){
		var unactives = model.notificationTypes.length() - model.notificationTypes.selection().length;
		return unactives;
	}

*/
	private updateSelectAllChip() {
		this.isAllSelected = this.areAllFiltersOn();
	}

	initFilters() {
		// If the user has not selected any preference, then show all notifications by default.
		if( !this.app.preferences || typeof this.app.preferences.type==="undefined" ) {
			this.app.notificationTypes.forEach( type => {
				this.selectedFilter[type] = true;
				this.app.selectedNotificationTypes.push( type );
			});
		} else {
			// Deactivate all
			this.app.notificationTypes.forEach( type => {
				this.selectedFilter[type] = false;
			});
			// Then reactivate notifications whose type was explicitely selected by the user.
			this.app.selectedNotificationTypes.forEach( type => {
				this.selectedFilter[type] = true;
			});
		}
		this.updateSelectAllChip();
	}

	switchFilter( type:string ) {
		const isSelected = this.selectedFilter[type]; // has just been updated by ng-model
		const savedIndex = this.app.selectedNotificationTypes.findIndex( t=>t===type );
		let evtName = "";
		if( isSelected && savedIndex===-1 ) {
			this.app.selectedNotificationTypes.push( type );
			this.savePrefsAndReload();
			if( this.tracker.willTrackEvent(TRACK.FILTER.SHOW_TYPE) ) {
				evtName = TRACK.nameForModule(TRACK.FILTER.SHOW_TYPE, this.translateType(type));
			}
		} else if( !isSelected && savedIndex!==-1 ) {
			this.app.selectedNotificationTypes.splice(savedIndex,1);
			this.savePrefsAndReload();
			if( this.tracker.willTrackEvent(TRACK.FILTER.HIDE_TYPE) ) {
				evtName = TRACK.nameForModule(TRACK.FILTER.HIDE_TYPE, this.translateType(type));
			}
		}
		this.updateSelectAllChip();

		// #50542: Track this event.
		if( evtName.length > 0 ) {
			this.tracker.trackEvent( TRACK.event, TRACK.FILTER.action, evtName );
		}
	}

//	public switchingFilters = false;

	switchAll() {
		if( this.areAllFiltersOn() ){
			//Deselect all
			this.app.selectedNotificationTypes.splice(0);
			this.app.notificationTypes.forEach( type => {
				this.selectedFilter[type] = false;
			});
			this.isAllSelected = false;
		} else {
			//Select all
			this.app.selectedNotificationTypes.splice(0);
			this.app.notificationTypes.forEach( type => {
				this.app.selectedNotificationTypes.push( type );
				this.selectedFilter[type] = true;
			});
			this.isAllSelected = true;
		}
		this.savePrefsAndReload();

		// #50542: Track this event.
		const evt = TRACK.FILTER;
		if( this.tracker.willTrackEvent(this.isAllSelected ? evt.SHOW_TYPE : evt.HIDE_TYPE) ) {
			this.tracker.trackEvent( TRACK.event, evt.action, TRACK.nameForModule(this.isAllSelected ? evt.SHOW_TYPE : evt.HIDE_TYPE, "Tout") );
		}
	}

	areAllFiltersOn(): boolean {
		return (this.app.selectedNotificationTypes.length >= this.app.notificationTypes.length);
	}

	formatDate(dateString){
		return L10n.moment(dateString).fromNow();
	}

	isEmpty(): boolean  {
		return this.app.notifications.length === 0 
			&& this.areAllFiltersOn();
	}

	noFiltersSelected = (): boolean => {
		return this.app.selectedNotificationTypes.length === 0;
	}

	getCssType( notifType:string ):string {
		notifType = notifType.toLowerCase();
		// This mapping follows the CSS classes defined at https://support.web-education.net/issues/47239
		switch( notifType ) {
			case "news":						return "actualites";
			case "collaborativewall":			return "collaborative-wall";
			case "messagerie":					return "conversation";
			case "homeworks":					return "cahier-de-texte";
			case "userbook_motto":				return "userbook"; //#45822, motto notifications share the same color as userbook
			case "userbook_mood":				return "userbook"; //#45822, mood  notifications share the same color as userbook
			case "userbook_discovervisiblegroups":				return "userbook"; //#45822, userbook_discover_workGroup notifications share the same color as userbook
			default:							return notifType;
		}
	}

	getFilterClass(notifType:string) {
		return "filter color-app-"+this.getCssType(notifType) + (this.selectedFilter[notifType]?" active":"");
	}

	toggleTools(event:UIEvent) {
		$((event.currentTarget as HTMLElement).parentNode).toggleClass('open');
	}

	translateType(notifType:string) {
		notifType=notifType.toLowerCase();
		return this.lang.translate(notifType === 'timeline' ? notifType + '.notification' : notifType);
	}

	/** #50542: Track widgets events. 
	 * Widgets emit custom events (see https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent/CustomEvent)
	 * that are listened to and treated for tracking statistics here. 
	 */
	public trackWidgetActions( wrapper:Element ) {
		const open:any = TRACK.OPEN_APP;
		// Last-infos widget tracking
		if( this.tracker.willTrackEvent(open.FROM_NEWS_LINK) ) {
			wrapper.addEventListener( TrackedActionFromWidget.lastInfos, (e:CustomEventInit<TrackedAction>) => {
				if( e.detail?.open==="app" ) {
					this.tracker.trackEvent( TRACK.event, open.action, open.FROM_NEWS_MORE );
				} else if( e.detail?.open==="info" ) {
					this.tracker.trackEvent( TRACK.event, open.action, open.FROM_NEWS_LINK );
				}
			});
		}
		// Agenda widget
		if( this.tracker.willTrackEvent(open.FROM_AGENDA_MORE) 
			|| this.tracker.willTrackEvent(open.FROM_AGENDA_EVENT) ) {
			wrapper.addEventListener( TrackedActionFromWidget.agenda, (e:CustomEventInit<TrackedAction>) => {
				if( e.detail?.open==="app" ) {
					this.tracker.trackEvent( TRACK.event, open.action, open.FROM_AGENDA_MORE );
				} else if( e.detail?.open==="event" ) {
					this.tracker.trackEvent( TRACK.event, open.action, open.FROM_AGENDA_EVENT );
				}
			});
		}
		// Myapps widget
		if( this.tracker.willTrackEvent(open.FROM_MYAPPS_WIDGET) 
			|| this.tracker.willTrackEvent(open.FROM_MYAPPS_WIDGET_MORE) ) {
			wrapper.addEventListener( TrackedActionFromWidget.myApps, (e:CustomEventInit<TrackedAction>) => {
				if( e.detail?.open==="app" && e.detail?.app?.name ) {
					const appName = this.lang.translate(e.detail?.app?.name);
					this.tracker.trackEvent( TRACK.event, open.action, TRACK.nameForModule(open.FROM_MYAPPS_WIDGET, appName) );
				} else if( e.detail?.open==="more" ) {
					this.tracker.trackEvent( TRACK.event, open.action, open.FROM_MYAPPS_WIDGET_MORE );
				}
			});
		}
		// School widget
		if( this.tracker.willTrackEvent(open.FROM_SCHOOL_MY_CLASSES)
			|| this.tracker.willTrackEvent(open.FROM_SCHOOL_TEAM)
			|| this.tracker.willTrackEvent(open.FROM_SCHOOL_DIRECTION) ) {
			wrapper.addEventListener( TrackedActionFromWidget.school, (e:CustomEventInit<TrackedAction>) => {
				if( e.detail?.open==="student.class" || e.detail?.open==="teacher.students" ) {
					this.tracker.trackEvent( TRACK.event, open.action, open.FROM_SCHOOL_MY_CLASSES );
				} else if( e.detail?.open==="relative.direction" ) {
					this.tracker.trackEvent( TRACK.event, open.action, open.FROM_SCHOOL_DIRECTION );
				} else if( e.detail?.open==="student.teachers"
						|| e.detail?.open==="teacher.teachers"
						|| e.detail?.open==="relative.teachers" ) {
					this.tracker.trackEvent( TRACK.event, open.action, open.FROM_SCHOOL_TEAM );
				}
			});
		}

		// School-widget and navbar menu
		const profile = TRACK.PROFILE;
		if( this.tracker.willTrackEvent(profile.FROM_SCHOOL_WIDGET) ) {
			wrapper.addEventListener( TrackedActionFromWidget.school, (e:CustomEventInit<TrackedAction>) => {
				if( e.detail?.open==="profile" ) {
					this.tracker.trackEvent( TRACK.event, profile.action, profile.FROM_SCHOOL_WIDGET );
				}
			});
		}

		// Record-me widget (audio-recorder)
		const record = TRACK.RECORD_SOUND;
		if( this.tracker.willTrackEvent(record.START) ) {
			wrapper.addEventListener( 'ode-recorder', (e:CustomEventInit<TrackedAction>) => {
				if( e.detail?.open==="audio" ) {
					this.tracker.trackEvent( TRACK.event, record.action, record.START );
				}
			});
		}

		// carnet-de-bord widget
		const carnet = TRACK.CARNET_DE_BORD;
		if( this.tracker.willTrackEvent(carnet.NAVIGATE) || this.tracker.willTrackEvent(carnet.REDIRECT) ) {
			wrapper.addEventListener( TrackedActionFromWidget.carnetDeBord, (e:CustomEventInit<TrackedAction>) => {
				if( typeof e.detail?.open==="string" ) {
					this.tracker.trackEvent( TRACK.event, carnet.action, carnet.REDIRECT );
				} else if( typeof e.detail?.properties==="string" ) {
					this.tracker.trackEvent( TRACK.event, carnet.action, carnet.NAVIGATE );
				}
			});
		}

		// Navigation events from Bookmarks widget and more
		const navigate = TRACK.NAVIGATE;
		if( this.tracker.willTrackEvent(navigate.FROM_BOOKMARK) ) {
			wrapper.addEventListener( TrackedActionFromWidget.bookmark, (e:CustomEventInit<TrackedAction>) => {
				if( typeof e.detail?.open==="string" ) {
					this.tracker.trackEvent( TRACK.event, navigate.action, navigate.FROM_BOOKMARK );
				}
			});
		}
		if( this.tracker.willTrackEvent(navigate.FROM_RSS) ) {
			wrapper.addEventListener( TrackedActionFromWidget.rss, (e:CustomEventInit<TrackedAction>) => {
				if( typeof e.detail?.open==="string" ) {
					this.tracker.trackEvent( TRACK.event, navigate.action, navigate.FROM_RSS );
				}
			});
		}
		if( this.tracker.willTrackEvent(navigate.FROM_QWANT) ) {
			wrapper.addEventListener( TrackedActionFromWidget.qwant, (e:CustomEventInit<TrackedAction>) => {
				if( e.detail?.open==="qwant" || typeof e.detail?.search==='string' ) {
					this.tracker.trackEvent( TRACK.event, navigate.action, navigate.FROM_QWANT );
				}
			});
		}

		// Drag'n'drop events
		const settings = TRACK.SETTINGS;
		if( this.tracker.willTrackEvent(settings.MOVE_WIDGET) ) {
			wrapper.addEventListener( "ode-widget-container", (e:CustomEventInit<TrackedAction>) => {
				if( typeof e.detail?.move==="string" && typeof e.detail?.to==="number" ) {
					this.tracker.trackEvent( TRACK.event, settings.action, settings.MOVE_WIDGET, e.detail.to );
				}
			});
		}
	}
	/** #50542: Track navbar events. 
	 * Navbar emit custom events (see https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent/CustomEvent)
	 * that are listened to and treated for tracking statistics here.
	 */
	public trackNavbarActions( wrapper:Element ) {
		if( this.tracker.willTrackEvent(TRACK.OPEN_APP.FROM_MENU_MYAPPS) 
		 || this.tracker.willTrackEvent(TRACK.OPEN_APP.FROM_MENU_MYAPPS_MORE) 
		 || this.tracker.willTrackEvent(TRACK.OPEN_APP.FROM_MENU_MYAPPS_APP) 
		 || this.tracker.willTrackEvent(TRACK.OPEN_APP.FROM_MENU_MAIL) 
		 || this.tracker.willTrackEvent(TRACK.OPEN_APP.FROM_MENU_COMMUNITY) 
		 || this.tracker.willTrackEvent(TRACK.HOME.FROM_MENU_HOME) 
		 || this.tracker.willTrackEvent(TRACK.HOME.FROM_LOGO) 
		 || this.tracker.willTrackEvent(TRACK.PROFILE.FROM_MENU_PROFILE) 
		 || this.tracker.willTrackEvent(TRACK.SEARCH.GO) 
		) {
			wrapper.addEventListener( "ode-navbar", (e:CustomEventInit<TrackedAction>) => {
				let track:any = TRACK.OPEN_APP;	// "Accéder à une appli"
				if( e.detail?.open==="welcome" && e.detail?.from==="more" ) {
					this.tracker.trackEvent( TRACK.event, track.action, track.FROM_MENU_MYAPPS_MORE );
				} else if( e.detail?.open==="welcome" && e.detail?.from==="menu" ) {
					this.tracker.trackEvent( TRACK.event, track.action, track.FROM_MENU_MYAPPS );
				} else if( e.detail?.open==="app" && e.detail?.app?.name ) {
					const appName = this.lang.translate(e.detail?.app?.name);
					this.tracker.trackEvent( TRACK.event, track.action, TRACK.nameForModule(track.FROM_MENU_MYAPPS_APP, appName) );
				} else if( e.detail?.open==="conversation" ) {
					this.tracker.trackEvent( TRACK.event, track.action, track.FROM_MENU_MAIL );
				} else if( e.detail?.open==="community" ) {
					this.tracker.trackEvent( TRACK.event, track.action, track.FROM_MENU_COMMUNITY );
				}

				track = TRACK.HOME;	// "Revenir à la page d'accueil"
				if( e.detail?.open==="timeline" && e.detail?.from==='menu' ) {
					this.tracker.trackEvent( TRACK.event, track.action, track.FROM_MENU_HOME );
				} else if( e.detail?.open==="timeline" && e.detail?.from==='logo' ) {
					const appName = this.lang.translate(e.detail?.app?.name);
					this.tracker.trackEvent( TRACK.event, track.action, track.FROM_LOGO );
				}

				track = TRACK.PROFILE;	// "Accéder à mon compte"
				if( e.detail?.open==="myaccount" ) {
					this.tracker.trackEvent( TRACK.event, track.action, track.FROM_MENU_PROFILE );
				}

				track = TRACK.SEARCH;	// "Lancer une recherche"
				if( e.detail?.open==="searchengine" ) {
					this.tracker.trackEvent( TRACK.event, track.action, track.GO );
				}
			});
		}
	}
	/** #50542: Track pulsar events. */
	public trackPulsarActions() {
		if( this.tracker.willTrackEvent(TRACK.DISCOVER.QUICKSTART_START) 
		 || this.tracker.willTrackEvent(TRACK.DISCOVER.QUICKSTART_END)
		) {
			document.addEventListener( "pulsar", (e:CustomEventInit<TrackedAction>) => {
				const discover:any = TRACK.DISCOVER;	// "Home - Quickstart - Clique"
				if( e.detail?.open=="true") {
					this.tracker.trackEvent( TRACK.event, discover.action, discover.QUICKSTART_START );
				} else if( e.detail?.open=="false") {
					this.tracker.trackEvent( TRACK.event, discover.action, discover.QUICKSTART_END );
				}
			});
		}
	}
};

interface TimelineScope extends IScope {
	canRenderUi: boolean;

	/* Needed for retro-compatibility with existing notifications text : they need these functions in the scope, directly. */
	userStructure: string;
	showAdminv1Link: () => boolean;
	showAdminv2HomeLink: () => boolean;
	showAdminv2AlertsLink: () => boolean;
}

/* Directive */
class Directive implements IDirective<TimelineScope,JQLite,IAttributes,IController[]> {
    restrict = 'E';
	template = require("./timeline.directive.html");
    scope = {
		pickTheme: "="
    };
	bindToController = true;
	controller = ["odeTracking", TimelineController];
	controllerAs = 'ctrl';
	require = ['timeline'];

    link(scope:TimelineScope, elem:JQLite, attr:IAttributes, controllers:IController[]|undefined) {
        const ctrl:TimelineController|null = controllers ? controllers[0] as TimelineController : null;
        if(!ctrl) return;

		ctrl.lightmode = attr["lightmode"] == "true" || false;
		ctrl.isCache = attr["cache"] == "true" || false;

		ctrl.savePrefsAndReload = () => {
			return ctrl.app.savePreferences()
			.then( () => {
				ctrl.app.notifications.splice(0);
				ctrl.app.resetPagination();
				return ctrl.app.loadNotifications();
			})
			.then( () => {
				scope.$apply();
			});
		}

		scope.canRenderUi = false;
		scope.userStructure = ctrl.userStructure;
		scope.showAdminv1Link = ctrl.showAdminv1Link.bind(ctrl);
		scope.showAdminv2HomeLink = ctrl.showAdminv2HomeLink.bind(ctrl);
		scope.showAdminv2AlertsLink = ctrl.showAdminv2AlertsLink.bind(ctrl);

		// In lightmode, don't load nor show the notifications.
		if( ctrl.lightmode ) {
			scope.canRenderUi = true;
		} else {
			Promise.all([
				ctrl.lang.addBundlePromise('/timeline/i18nNotifications?mergeall=true'),
				ctrl.initialize()
			])
			.then( () => ctrl.initFilters() )
			.then( () => ctrl.loadPage() )
			.then( () => {
				scope.canRenderUi = true;
				scope.$apply();
				
				ctrl.handleLoadPageClick = (force: boolean): Promise<void> => {
					return ctrl.loadPage(force).then(() => scope.$apply());
				}

				// #50542: Track widgets events :
				const wrapper = document.querySelector("div.container-advanced-wrapper");
				wrapper && ctrl.trackWidgetActions( wrapper );
				// #50542: Track navbar events :
				const navbar = document.querySelector("ode-navbar");
				navbar && ctrl.trackNavbarActions( navbar );
				// #50542: Track pulsar events :
				ctrl.trackPulsarActions();

				// Only once the UI is up-to-date can we use the gsap animations.
				// Advanced transitions for filters
				$('.filter-button').each(function (i) {
					var target = '#' + $(this).data('target');
					var filterTween = gsap.gsap.timeline().reversed(true).pause();
					filterTween.from(target, { duration:0.8, height:1, autoAlpha:0, ease:"sin.inOut", display:'none' });
					filterTween.from(target + " .filter", {
						duration: 0.4, 
						autoAlpha: 0, 
						translateY: '10px',
						ease: "power1.inOut",
						stagger: {
							amount: 0.6,
							ease: "sin.in",
						}
					}, "-=0.8");
					$(target).data('tween', filterTween);
				});

				$('.filter-button').on('click', function (e) {
					const evt = TRACK.FILTER;
					var target = '#' + $(this).data('target');
					if ($(target).data("tween").reversed()) {
						$(target).data("tween").play();

						// #50542: Track this event.
						if( ctrl.tracker.willTrackEvent(evt.SHOW) ) {
							ctrl.tracker.trackEvent( TRACK.event, evt.action, evt.SHOW );
						}
					} else {
						$(target).data("tween").reverse();

						// #50542: Track this event.
						if( ctrl.tracker.willTrackEvent(evt.HIDE) ) {
							ctrl.tracker.trackEvent( TRACK.event, evt.action, evt.HIDE );
						}
					}
				});
			});
		} // end if !ctrl.lightmode
    }
}

/**
 * The timeline directive.
 *
 * Usage:
 *   &lt;timeline lightmode="true|false" cache="true|false"></timeline&gt;
 */
export function DirectiveFactory() {
	return new Directive();
}