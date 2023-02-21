import { IAttributes, IController, IDirective, IScope } from "angular";
import { ValidateMailController } from "./validate-mail.directive";

declare const window:any;

/** 
 * This directives integrates an international phone input helper.
 * It only works in the context of the validate-mail directive.
 * See https://www.twilio.com/fr/blog/saisie-numeros-telephone-internationaux-html-javascript
 * Libs are imported from a CDN in the HTML file - not webpack.
 */

/* Directive */
class Directive implements IDirective<IScope,JQLite,IAttributes,IController[]> {
    restrict = 'A';
	require = ['^validateMail'];

    link(scope:IScope, elem:JQLite, attrs:IAttributes, controllers?:IController[]): void {
		if( !controllers ) return;
        const validationCtrl:ValidateMailController|null = controllers[0] as ValidateMailController;
        if(!validationCtrl) return;

		// Check if intlTelInput.min.js is available, then apply to phone input
		if(elem && elem[0] && window && window.intlTelInput) {
			const intlPhoneInput = window.intlTelInput(elem[0], {
			  preferredCountries: ["fr", "de", "es", "it", "mx"],
			  utilsScript:"https://cdnjs.cloudflare.com/ajax/libs/intl-tel-input/17.0.8/js/utils.js",
			});
			validationCtrl.intlFormat = () => intlPhoneInput.getNumber();
		}
	}
}

/** The intl-phone-input directive. */
export function DirectiveFactory() {
	return new Directive();
}