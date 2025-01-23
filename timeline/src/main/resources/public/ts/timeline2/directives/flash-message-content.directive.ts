import { IAttributes, ICompileService, IController, IDirective, IScope } from "angular";
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
class Directive implements IDirective<Scope, JQLite, IAttributes, IController[]> {
	constructor(
		private $compile: ICompileService,
		private $sanitize: any,
		private richContentSvc: RichContentService,
		private helperSvc: NgHelperService) {
	}

	restrict = 'A';
	scope = {
		message: '=flashMessageContent'
	};
	require = ["^flashMessages"];

	link(scope: Scope, elem: JQLite, attr: IAttributes, controllers?: IController[]): void {
		const isExpandableClass = "flash-content-is-expandable";
		const maxHeightText = 44; // Magic value here ? From 88 -> 44 to have only two lines of text befor ellipsis
		const parentCtrl = controllers[0] as FlashMsgController;
		if (!parentCtrl || !scope.message) return;

		// get the content in the user's language or in french or in the first language available
		let messageContent = scope.message.contents[parentCtrl.currentLanguage] 
			?? scope.message.contents["fr"] 
			?? Object.keys(scope.message.contents)
				.map(key => scope.message.contents[key])
				.filter(content => content !== null)[0] ?? '';
		// replace empty lines from adminV1 editor
		messageContent = messageContent.replaceAll(/(<div>[\s\u200B]*<\/div>){2,}/g, '<div>\u200B</div>'); // This code merges consecutive empty lines from adminV1 editor
		messageContent = messageContent.replaceAll(/(<div>([\s\u200B]|<br\/?>)*<\/div>)$/g, ''); // This code remove last empty line from adminV1 editor
		messageContent = messageContent.replaceAll(/(<p><br><\/p>)+/g, ''); // This code merges consecutive empty lines from adminV2 editor
		this.richContentSvc.apply(this.$sanitize(messageContent), elem, scope);

		// If needed, limit the height of displayed text, and add a button "See more" which toggles the full message display back and forth.
		if (this.helperSvc) {
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

			console.log(scope.message.color, elem.height(), maxHeightText);

			if (elem.height() > maxHeightText) {

				elem.parent().addClass("can-be-truncated");
				scope.toggleContent = function () {
					if (scope.isFullyExpandable()) {
						elem.parent().removeClass(isExpandableClass);
						scope.isExpandable = false;
					} else {
						elem.parent().addClass(isExpandableClass);
						scope.isExpandable = true;
					}
				};
				elem.parent().parent().append(this.$compile(moreContainer)(scope));
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
export function DirectiveFactory($compile: ICompileService, $sanitize: any, richContentSvc: RichContentService, helperSvc: NgHelperService) {
	return new Directive($compile, $sanitize, richContentSvc, helperSvc);
}
DirectiveFactory.$inject = ["$compile", "$sanitize", "odeRichContentService", "odeNgHelperService"];
