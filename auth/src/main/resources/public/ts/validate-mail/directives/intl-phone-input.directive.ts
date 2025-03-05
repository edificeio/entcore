import { IAttributes, IController, IDirective, IScope } from "angular";
import * as $ from "jquery";
import { ValidateMailController } from "./validate-mail.directive";

declare const window: any;

/**
 * This directives integrates an international phone input helper.
 * It only works in the context of the validate-mail directive.
 * See https://www.twilio.com/fr/blog/saisie-numeros-telephone-internationaux-html-javascript
 * Libs are imported from a CDN in the HTML file - not webpack.
 */

/* Directive */
class Directive
  implements IDirective<IScope, JQLite, IAttributes, IController[]>
{
  restrict = "A";
  require = ["^validateMail", "ngModel"];

  constructor(private conf: any) {}

  link(
    scope: IScope,
    elem: JQLite,
    attrs: IAttributes,
    controllers?: IController[]
  ): void {
    if (!controllers) return;
    const validationCtrl: ValidateMailController | null =
      controllers[0] as ValidateMailController;
    if (!validationCtrl) return;

    // Check if intlTelInput.min.js is available, then apply to phone input
    // Available options are documented here : https://github.com/jackocnr/intl-tel-input#initialisation-options
    if (elem && elem[0] && window && window.intlTelInput) {
      const intlPhoneInput = window.intlTelInput(elem[0], {
        customContainer: "w-100",
        utilsScript:
          "https://cdnjs.cloudflare.com/ajax/libs/intl-tel-input/17.0.8/js/utils.js",
        ...this.conf,
      });
      if (intlPhoneInput) {
        validationCtrl.intlFormat = () => intlPhoneInput.getNumber();

        elem.on("open:countrydropdown", (e) => {
          if ($("body").hasClass("iti-mobile")) {
            const topOffset = elem[0].getBoundingClientRect().bottom;
            $("body > .iti.iti--container").css({
              top: topOffset + "px",
              bottom: 0,
            });
          }
        });
        elem.on("close:countrydropdown", (e) => {});

        scope.$on("$destroy", () => {
          intlPhoneInput.destroy();
          elem.off("open:countrydropdown close:countrydropdown");
        });

        scope.$watch(attrs.ngModel, function () {
          if (
            intlPhoneInput
          ) {
            intlPhoneInput.setNumber(controllers[1].$modelValue);
          }
        });
      }
    }
  }
}

/** The intl-phone-input directive. */
export function DirectiveFactory(conf) {
  return new Directive(conf);
}
DirectiveFactory.$inject = ["intlPhoneInputConf"];
