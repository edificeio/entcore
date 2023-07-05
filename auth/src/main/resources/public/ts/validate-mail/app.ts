import angular = require("angular");
import { APP } from "ode-ts-client";
import { OdeModules, conf } from 'ode-ngjs-front';
import { AppController } from "./controller";
import * as App from './directives/validate-mail.directive';
import * as NavbarLegacy from './directives/navbar-legacy.directive';
import * as IntlPhoneInput from './directives/intl-phone-input.directive';
import { LoadIntlPhoneInputConfig } from "./services/intl-phone-input.config";

LoadIntlPhoneInputConfig()
.then( config => {
    angular.module("app", [OdeModules.getBase(), OdeModules.getI18n(), OdeModules.getUi()])
    .value("intlPhoneInputConf", config)
    .controller("appCtrl", ['$scope', AppController])
    .directive("validateMail", App.DirectiveFactory)
    .directive("navbarLegacy", NavbarLegacy.DirectiveFactory)
    .directive("intlPhoneInput", IntlPhoneInput.DirectiveFactory)
    ;
    conf().Platform.apps.initialize(APP.PORTAL, true);
});
