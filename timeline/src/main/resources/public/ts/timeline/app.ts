import { model, ng } from 'entcore';
import { build } from '../model/timeline';
import * as timelineControllers from './controller';
import angular = require("angular");

for (let controller in timelineControllers) {
	ng.controllers.push(timelineControllers[controller]);
}
const flashMsgCollapsable = ng.directive('flashMsg', ['$window', ($window) => ({
	restrict: 'E',
	replace: true,
	template: '\
		<div class="flashmsg" ng-class="message.customColor ? \'blue\' : message.color" flash-msg-collapse>\
			<svg class="icon-svg flashmsg-icon" width="20" height="20" viewBox="0 0 24 24">\
				<use href="{{icon}}"></use>\
			</svg>\
			<svg class="icon-svg flashmsg-close" tooltip="timeline.mark.flashmsg" ng-click="markMessage(message)" width="20" height="20" viewBox="0 0 24 24">\
				<use href="/timeline/public/icons/icons.svg#close"></use>\
			</svg>\
			<div class="flash-msg-collapsable flash-msg-collapsable--collapsable flash-msg-collapsable--collapsed">\
				<div bind-html="contents"></div>\
			</div>\
			<span ng-if="message.signature" class="flash-content-signature">{{message.signature}}</span>\
			<span class="flash-msg-collapsable-button" ng-if="collapsable && collapsed">\
				<b ng-click="toggleCollapse()">\
					<i18n>timeline.flash.message.seemore</i18n>\
				</b>\
			</span>\
			<span class="flash-msg-collapsable-button" ng-if="collapsable && !collapsed">\
				<b ng-click="toggleCollapse()">\
					<i18n>timeline.flash.message.seeless</i18n>\
				</b>\
			</span>\
		</div>',
	scope: {
		message: '=',
		markMessage: '&',
		currentLanguage: '='
	},
	link: function ($scope, $element, $attrs) {
		$scope.icon = $scope.message.color === 'red' ? '/timeline/public/icons/icons.svg#alert-triangle' : '/timeline/public/icons/icons.svg#info-circle';
		$scope.collapsed = true;
		$scope.collapsable = undefined;
		var $collapsableElement = $element.find('.flash-msg-collapsable');
		var $collapsableElementInner = $collapsableElement.find('div');
		$scope.contents = '<p>' + $scope.message.contents[$scope.currentLanguage] + '</p>';

		$scope.onResize = function () {
			if ($scope.collapsable !== undefined) return;
			$scope.collapsable = $collapsableElementInner[0].scrollHeight > $collapsableElementInner[0].clientHeight;
			$collapsableElementInner[$scope.collapsable ? 'addClass' : 'removeClass']('flash-msg-collapsable--collapsable');
			$scope.$apply();
		};
		$scope.toggleCollapse = function () {
			$scope.collapsed = !$scope.collapsed;
			$collapsableElement[$scope.collapsed ? 'addClass' : 'removeClass']('flash-msg-collapsable--collapsed');
		};

		window.setTimeout(function () {
			$scope.onResize();
		}, 100); // let time to render everything

		angular.element($window).on('resize', $scope.onResize);
		$scope.$on('$destroy', function () {
			angular.element($window).off('resize', $scope.onResize);
		});

	}
})]);

ng.directives.push(flashMsgCollapsable);

model.build = build;