import { IAttributes, IController, IDirective, IScope } from "angular";
import { IIdiom, IThemeDesc, IWidget, WidgetFrameworkFactory } from "ode-ts-client";
import { ThemeHelperService, session, conf, TrackingService } from "ode-ngjs-front";
import * as $ from "jquery";
import { TimelineController } from "./timeline.directive";
import { TRACK } from "../tracking/events";

/* Controller for the directive */
export class Controller implements IController {
	public skins:IThemeDesc[];
	public currentSkinName?:string;
	public widgets:IWidget[];
	public languages:string[];
	public themeRoot:string;
	public safeApply:()=>void;

    constructor(
		public themeSvc:ThemeHelperService,
		public tracker:TrackingService
		) {
		this.widgets = WidgetFrameworkFactory.instance().list;
    }
	showPanel:boolean = false;

	get languagePreference():string {
		return session().currentLanguage;
	}

	canTogglePanel():boolean {
		const ww = $(window).width();
		return (typeof ww!=="number" || ww >= 992);
	}

	togglePanel($event) {
		this.showPanel = !this.showPanel;

		// #50542: Track this event.
		const evt = TRACK.SETTINGS;
		if( this.showPanel && this.tracker.willTrackEvent(evt.OPEN) ) {
			this.tracker.trackEvent( TRACK.event, evt.action, evt.OPEN );
		}
	}

	async saveTheme(skin:IThemeDesc, $event) {
		this.currentSkinName = skin.displayName;
		await this.themeSvc.setTheme( skin );

		// #50542: Track this event.
		const evt = TRACK.SETTINGS;
		if( this.tracker.willTrackEvent(evt.SKIN_CHANGE) ) {
			const skinName = conf().Platform.idiom.translate(skin.displayName);
			this.tracker.trackEvent( TRACK.event, evt.action, TRACK.nameForSkin(evt.SKIN_CHANGE, skinName) );
		}
	}

	toggleWidget( widget:IWidget, $event) {
		if( ! widget.platformConf.mandatory ) {
			widget.userPref.show = !widget.userPref.show;
			WidgetFrameworkFactory.instance().saveUserPrefs();

			// #50542: Track this event.
			const evt = TRACK.SETTINGS;
			const evtName = widget.userPref.show ? evt.WIDGET_SHOW : evt.WIDGET_HIDE;
			if( this.tracker.willTrackEvent(evtName) ) {
				const widgetName = conf().Platform.idiom.translate('timeline.settings.'+widget.platformConf.name);
				this.tracker.trackEvent( TRACK.event, evt.action, TRACK.nameForWidget(evtName, widgetName) );
			}
    	}
	}

	getFlagUrlFor( language:string ):string {
		let lang = language.toLocaleLowerCase();
		// Map between language codes and their corresponding flags name.
		switch( lang ) {
			case "en": lang="gb"; break;
			default: break;
		}
		return `${this.themeRoot}/themes/neo/img/icons/flags/${lang}.svg`;
	}

	saveLang(language, $event) {
		conf().User.saveLanguage( language ).then( () => {
			location.reload();
		});

		// #50542: Track this event.
		const evt = TRACK.SETTINGS;
		if( this.tracker.willTrackEvent(evt.CHANGE_LANG) ) {
			const langName = conf().Platform.idiom.translate('language.' + language);
			this.tracker.trackEvent( TRACK.event, evt.action, TRACK.nameForLang(evt.CHANGE_LANG, langName) );
		}
	};
};

interface LocalScope extends IScope {
	lang?: IIdiom;
	me?:{
		hasWorkflow(right:string):boolean;
	};
}

/* Directive */
class Directive implements IDirective<LocalScope,JQLite,IAttributes,IController[]> {
    restrict = 'E';
	replace = true;
	template = require("./timeline-settings.directive.html");
    scope = {
    };
	bindToController = true;
	controller = ["odeThemeHelperService", "odeTracking", Controller];
	controllerAs = 'ctrl';
	require = ['timelineSettings', '^timeline'];

    async link(scope:LocalScope, elem:JQLite, attr:IAttributes, controllers:IController[]|undefined) {
        let ctrl:Controller|null = controllers ? controllers[0] as Controller : null;
        let timelineCtrl:TimelineController|null = controllers ? controllers[1] as TimelineController : null;
        if(!ctrl || !timelineCtrl) return;

		scope.lang = conf().Platform.idiom;
		scope.me = {
			hasWorkflow(right:string):boolean {
				return session().hasWorkflow(right);
			}
		};

		Promise.all([
			conf().Platform.listLanguages(),
			conf().Platform.theme.listThemes(),
			ctrl.themeSvc.getBootstrapThemePath()
		]).then( results => {
			ctrl.languages = results[0];
			ctrl.skins = results[1];
			ctrl.currentSkinName = conf().Platform.theme.skinName;
			ctrl.themeRoot = results[2];
			ctrl.safeApply = ( fn?: string | ((scope:IScope)=>any) ) => {
				const phase = scope.$root.$$phase;
				if (phase == '$apply' || phase == '$digest') {
					if (typeof (fn) === 'function') {
						fn(scope);
					}
				} else {
					scope.$apply(fn as string);
				}
			}
			scope.$apply();
		});

    }

}

/**
 * The timeline-settings directive.
 *
 * Usage:
 *   &lt;timeline-settings></timeline-settings&gt;
 */
export function DirectiveFactory() {
	return new Directive();
}
