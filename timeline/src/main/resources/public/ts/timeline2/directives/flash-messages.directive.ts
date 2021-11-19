import { IAttributes, IController, IDirective, IScope } from "angular";
import { IFlashMessageModel, ITimelineApp, ITimelineFactory, NotifyFrameworkFactory } from "ode-ts-client";
import { session } from "ode-ngjs-front";

/* Controller for the directive */
export class FlashMsgController implements IController {
	public app:ITimelineApp;
	public currentLanguage:string;

	constructor( private $scope:IScope ) {}

	list() {
		return this.app.loadFlashMessages();
	}

	public get messages() {
		return this.app.flashMessages;
	}

    dismiss( message:IFlashMessageModel ) {
		this.app.markAsRead( message ).then( () => {
			this.app.flashMessages.splice(this.app.flashMessages.findIndex(m => m.id === message.id), 1);
			this.$scope.$apply();
        });
    }
};

/* Directive */
class Directive implements IDirective<IScope,JQLite,IAttributes,IController[]> {
    restrict = 'E';
	template = require("./flash-messages.directive.html");
    scope = {
    };
	bindToController = true;
	controller = ["$scope", FlashMsgController];
	controllerAs = 'ctrl';
	require = ['flashMessages'];

    link(scope:IScope, elem:JQLite, attr:IAttributes, controllers:IController[]|undefined) {
        let ctrl:FlashMsgController|null = controllers ? controllers[0] as FlashMsgController : null;
        if(!ctrl) return;

		ctrl.app = ITimelineFactory.createInstance();
		NotifyFrameworkFactory.instance().onSessionReady().promise
		.then( userinfo => {
			ctrl.currentLanguage = session().currentLanguage;
			ctrl.list();
			scope.$apply();
		});
    }
}

/**
 * The flash-messages directive.
 *
 * Usage:
 *   &lt;flash-messages></flash-messages&gt;
 */
export function DirectiveFactory() {
	return new Directive();
}
