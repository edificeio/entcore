import angular = require("angular");
import { APP } from "ode-ts-client";
import { OdeModules, conf } from 'ode-ngjs-front';
import { AppController } from "./controller";
import * as App from './directives/validate-mfa.directive';
import * as NavbarLegacy from '../validate-mail/directives/navbar-legacy.directive';

angular.module("app", [OdeModules.getBase(), OdeModules.getI18n(), OdeModules.getUi()])
.controller("appCtrl", ['$scope', AppController])
.directive("navbarLegacy", NavbarLegacy.DirectiveFactory)
.directive("validateMfa", App.DirectiveFactory)
;
conf().Platform.apps.initialize(APP.PORTAL, true);