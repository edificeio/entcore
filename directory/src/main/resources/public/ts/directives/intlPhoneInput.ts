import { IAttributes, IController } from "angular";
import { $, httpPromisy, ng } from "entcore";

declare const window: any;

export interface IntlPhoneInputScope {
  $watch(a?, b?);
  $on(a?, b?);
  intlFormatNumber: () => string;
}

/**
 * This directives integrates an international phone input helper.
 * It only works in the context of the validate-mail directive.
 * See https://www.twilio.com/fr/blog/saisie-numeros-telephone-internationaux-html-javascript
 * Libs are imported from a CDN in the HTML file - not webpack.
 */
export const intlPhoneInputDirective = ng.directive("intlPhoneInput", [
  () => {
    return {
      restrict: "A",
      scope: {
        intlFormatNumber: "=",
      },
      require: "ngModel",
      link: async (
        scope: IntlPhoneInputScope,
        elem: JQLite,
        attrs: IAttributes, 
        ngModelController?: IController
      ) => {
        let intlPhoneInput: any;
        // Load intl-phone-input configuration
        if (!window.intlTelInputConfig) {
          let defaultConf = {
            onlyCountries: ["fr"],
          };

          try {
            // intl-phone-input configuration undefined, keep using default values.
            const publicConf = await httpPromisy<any>().get(`/auth/conf/public`);
            if (publicConf && typeof publicConf["intl-phone-input"] === "object") {
              window.intlTelInputConfig = publicConf["intl-phone-input"];
            } else {
              window.intlTelInputConfig = defaultConf;
            }
          } catch (e) {
            window.intlTelInputConfig = defaultConf;
          }
        }
        
        scope.$watch(attrs.ngModel, function() { 
          // if (intlPhoneInput && intlPhoneInput.getNumber() !== ngModelController.$modelValue) {
            intlPhoneInput.setNumber(ngModelController.$modelValue);
          // }
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
