import { IAttributes, IController, IDirective, IScope, ICompileService } from "angular";
import { NgHelperService, RichContentService } from "ode-ngjs-front";
import { IFlashMessageModel } from "ode-ts-client";
import { FlashMsgController } from "./flash-messages.directive";

interface Scope extends IScope {
	message: IFlashMessageModel;
	/** Helper function for toggling long messages. */
	toggleContent?: () => void;
	/** Check if the message is fully readable. */
	isFullyExpandable?: () => Boolean;
	isExpandableClass?: string;
	isExpandable: boolean;
	maxHeightText: number;
}

/* Directive */
class Directive implements IDirective<Scope,JQLite,IAttributes,IController[]> {
	constructor( 
		private $compile:ICompileService,
		private richContentSvc:RichContentService, 
		private helperSvc:NgHelperService ) {
	}

	restrict= 'A';
	scope= {
		message: '=flashMessageContent'
	};
	require= ["^flashMessages"];

    link(scope:Scope, elem:JQLite, attr:IAttributes, controllers?:IController[]): void {
		const isExpandableClass = "flash-content-is-expandable";
		const maxHeightText = 88;
		const parentCtrl = controllers[0] as FlashMsgController;
		if( !parentCtrl || !scope.message ) return;

		this.richContentSvc.apply(scope.message.contents[parentCtrl.currentLanguage] ?? '', elem, scope);

		// If needed, limit the height of displayed text, and add a button "See more" which toggles the full message display back and forth.
		if( this.helperSvc ) {
			scope.isExpandable = true;
			scope.isFullyExpandable = () => scope.isExpandable;

			const moreContainer = $(`
				<div class="btn-expand" ng-click="toggleContent()">
					<span class="btn-expand-inner">
						<i18n ng-if="!isFullyExpandable()">timeline.flash.message.seemore</i18n>
						<i18n ng-if="isFullyExpandable()">timeline.flash.message.seeless</i18n>
					</span>
				</div>
			`);
			
			const initialHTML = elem.html();
			const dummyHTML = () => elem.find("p").each(function () {
				$(this).replaceWith($(this).html()+'<br>');
			});

			if (elem.height() > maxHeightText) {

				dummyHTML();

				elem.parent().addClass("can-be-truncated");
				scope.toggleContent = function () {
					if (scope.isFullyExpandable()) {
						elem.parent().removeClass(isExpandableClass);
						scope.isExpandable = false;
						dummyHTML();
					} else {
						elem.parent().addClass(isExpandableClass);
						scope.isExpandable = true;
						elem.html(initialHTML);
					}
				};
				elem.parent().append(this.$compile(moreContainer)(scope));
				scope.toggleContent();
			}
		}
    }
}

/**
 * The flash-message-content directive.
 *
 * Usage:
 *   &lt;flash-message-content>The content to display which can be very very long</flash-message-content&gt;
 */
export function DirectiveFactory($compile:ICompileService, richContentSvc:RichContentService, helperSvc:NgHelperService) {
	return new Directive($compile, richContentSvc, helperSvc);
}
DirectiveFactory.$inject = ["$compile", "odeRichContentService", "odeNgHelperService"];
