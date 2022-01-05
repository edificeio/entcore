import angular = require("angular");
import { APP } from "ode-ts-client";
import { OdeModules, conf } from 'ode-ngjs-front';
import { AppController } from "./controller";
import * as TimelineApp from './directives/timeline.directive';
import * as FlashMsg from './directives/flash-messages.directive';
import * as FlashMsgContent from './directives/flash-message-content.directive';
import * as TimelineSettings from './directives/timeline-settings.directive';

angular.module("app", [OdeModules.getBase(), OdeModules.getI18n(), OdeModules.getUi(), OdeModules.getWidgets()])
.controller("appCtrl", ['$scope', AppController])
.directive("timeline", TimelineApp.DirectiveFactory)
.directive("flashMessages", FlashMsg.DirectiveFactory)
.directive("flashMessageContent", FlashMsgContent.DirectiveFactory)
.directive("timelineSettings", TimelineSettings.DirectiveFactory);

conf().Platform.apps.initialize(APP.TIMELINE);