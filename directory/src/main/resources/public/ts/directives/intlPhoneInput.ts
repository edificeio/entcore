import { IAttributes, IController } from "angular";
import { $, httpPromisy, ng } from "entcore";

declare const window: any;

export interface IntlPhoneInputScope {
  $watch(a?, b?);
  $on(a?, b?);
  intlFormatNumber: () => string;
}

/**
 * Directive for initializing and managing an international phone input field using the intl-tel-input library.
 * See https://www.twilio.com/fr/blog/saisie-numeros-telephone-internationaux-html-javascript
 * Libs are imported from a CDN in the HTML file - not webpack.
 * 
 * @directive
 * @name intlPhoneInput
 * 
 * @scope
 * @property {string} intlFormatNumber - A function to get the formatted international phone number.
 * 
 * @requires ngModel
 * 
 * @description
 * This directive initializes an international phone input field with the intl-tel-input library.
 * It dynamically loads the necessary CSS and JS files if they are not already loaded.
 * The directive also handles configuration loading, setting the phone number, and managing events.
 * 
 * @example
 * <input type="tel" intl-phone-input ng-model="phoneNumber" />
 * 
 * @param {IntlPhoneInputScope} scope - The scope of the directive.
 * @param {JQLite} elem - The element to which the directive is applied.
 * @param {IAttributes} attrs - The attributes of the element.
 * @param {IController} [ngModelController] - The ngModel controller.
 */
export const intlPhoneInputDirective = ng.directive("intlPhoneInput", [
  () => {
    return {
      restrict: "A",
      scope: {
        intlFormatNumber: "=",
      },
      require: "ngModel",
      link: (
        scope: IntlPhoneInputScope,
        elem: JQLite,
        attrs: IAttributes, 
        ngModelController?: IController
      ) => {
        let intlPhoneInput: any;
        // Load intl-phone-input configuration
        if (!window.intlTelInputConfig) {
          /**
           * Configuration object for initializing the international phone input.
           * 
           * @property {string} initialCountry - The default country code to be used for the phone input.
           * @property {string[]} preferredCountries - An array of country codes that will be displayed at the top of the country dropdown list.
           */
          let defaultConf = {
            initialCountry: "fr",
            preferredCountries: ["fr", "mx", "es", "co", "gf", "pf", "gp", "yt", "nc", "pm", "wf", "gy", "mq", "mm", "ph"]
          };

          try {
            // intl-phone-input configuration undefined, keep using default values.
            
            httpPromisy<any>().get(`/auth/conf/public`).then(
              (publicConf) => {
                if (publicConf && typeof publicConf["intl-phone-input"] === "object") {
                  window.intlTelInputConfig =  publicConf["intl-phone-input"];
                } else {
                  window.intlTelInputConfig = defaultConf;
                }
              }
            );
          } catch (e) {
            window.intlTelInputConfig = defaultConf;
          }
        }
        
        scope.$watch(attrs.ngModel, function() { 
          intlPhoneInput.setNumber(ngModelController.$modelValue);
        });

        // Init intl-phone-input field after both CSS and JS are loaded
        let filesLoaded = 0;
        function importLoadedCallback() {
          filesLoaded++;

          if (filesLoaded === 2) {
            intlPhoneInput = window.intlTelInput(elem[0], {
              customContainer: "w-100",
              utilsScript:
                "https://cdnjs.cloudflare.com/ajax/libs/intl-tel-input/17.0.8/js/utils.js",
              ...window.intlTelInputConfig,
            });

            if (intlPhoneInput) {
              scope.intlFormatNumber = () => intlPhoneInput.getNumber();
              if (ngModelController.$modelValue && intlPhoneInput.getNumber() !== ngModelController.$modelValue) {
                intlPhoneInput.setNumber(ngModelController.$modelValue);
              }

              elem.on("open:countrydropdown", (e) => {
                if ($("body").hasClass("iti-mobile")) {
                  const topOffset = elem[0].getBoundingClientRect().bottom;
                  $("body > .iti.iti--container").css({
                    top: topOffset + "px",
                    bottom: 0,
                  });
                }
              0});

              elem.on("close:countrydropdown", (e) => {});

              scope.$on("$destroy", () => {
                intlPhoneInput.destroy();
                elem.off("open:countrydropdown close:countrydropdown");
              });
            }
          }
        }

        // Dynamically import intl-tel-input js and CSS
        const intlTelInputStyles =
          "https://cdnjs.cloudflare.com/ajax/libs/intl-tel-input/17.0.8/css/intlTelInput.css";
        const intlTelInputJS =
          "https://cdnjs.cloudflare.com/ajax/libs/intl-tel-input/17.0.8/js/intlTelInput.min.js";

        if (!document.querySelector(`link[href="${intlTelInputStyles}"]`)) {
          const link = document.createElement("link");
          link.rel = "stylesheet";
          link.href = intlTelInputStyles;
          link.onload = importLoadedCallback; // Fire callback when loaded
          document.head.appendChild(link);
        } else {
          importLoadedCallback();
        }

        if (!document.querySelector(`script[src="${intlTelInputJS}"]`)) {
          var script = document.createElement("script");
          script.type = "text/javascript";
          script.src = intlTelInputJS;
          script.async = true;
          script.onload = importLoadedCallback; // Fire callback when loaded
          document.body.appendChild(script);
        } else {
          importLoadedCallback();
        }
      },
    };
  },
]);
