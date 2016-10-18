// Copyright © WebServices pour l'Éducation, 2014
//
// This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation (version 3 of the License).
//
// For the sake of explanation, any module that communicate over native
// Web protocols, such as HTTP, with ENT Core is outside the scope of this
// license and could be license under its own terms. This is merely considered
// normal use of ENT Core, and does not fall under the heading of "covered work".
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

import { $ } from './libs/jquery/jquery';
import { idiom as lang, idiom as idiom } from './idiom';
import { http, Http } from './http';
import { Collection, Model, model } from './modelDefinitions';
import { bootstrap } from './lib';
import { ui } from './ui';
import { Behaviours } from './behaviours';
import { currentLanguage, routes, appPrefix, infraPrefix } from './globals';
import { template } from './template';
import { moment } from './libs/moment/moment';
import { _ } from './libs/underscore/underscore';
import { angular } from './libs/angular/angular';
import { workspace, notify, skin, RTE } from './entcore';
import { ng } from './ng-start';
import * as directives from './directives';

var module = angular.module('app', ['ngSanitize', 'ngRoute'], function($interpolateProvider) {
		$interpolateProvider.startSymbol('[[');
		$interpolateProvider.endSymbol(']]');
	})
	.factory('route', function($rootScope, $route, $routeParams){
		var routes = {};
		var currentAction = undefined;
		var currentParams = undefined;

		$rootScope.$on("$routeChangeSuccess", function($currentRoute, $previousRoute){
			if(typeof routes[$route.current.action] === 'function' &&
				(currentAction !== $route.current.action || (currentParams !== $route.current.params && !(Object.getOwnPropertyNames($route.current.params).length === 0)))){
				currentAction = $route.current.action;
				currentParams = $route.current.params;
				routes[$route.current.action]($routeParams);
				setTimeout(function () {
				    ui.scrollToId(window.location.hash.split('#')[1]);
				}, 100);
			}
		});

		return function(setRoutes){
			routes = setRoutes;
		}
	})
	.factory('model', function($timeout){
		var fa = Collection.prototype.trigger;
		Collection.prototype.trigger = function(event){
			$timeout(function(){
				fa.call(this, event);
			}.bind(this), 10);
		};

		var fn = Model.prototype.trigger;
		Model.prototype.trigger = function(event, eventData){
			$timeout(function(){
				fn.call(this, event, eventData);
			}.bind(this), 10);
		};

		return model;
	})
    .factory('xmlHelper', function(){
        return {
            xmlToJson: function(xml, accumulator, stripNamespaces, flatten) {
                var nodeName;
                var that = this;
                if (!accumulator)
                    accumulator = {};
                if (!stripNamespaces)
                    stripNamespaces = true;
                if(flatten == null)
                    flatten = true

                if (stripNamespaces && xml.nodeName.indexOf(":") > -1) {
                    nodeName = xml.nodeName.split(':')[1];
                } else {
                    nodeName = xml.nodeName;
                }
                if ($(xml).children().length > 0) {
                    if(!flatten)
                        accumulator[nodeName] = [];
                    else
                        accumulator[nodeName] = {};
                    _.each($(xml).children(), function(child) {
                        if(!flatten)
                            accumulator[nodeName].push(that.xmlToJson(child, {}, stripNamespaces, flatten));
                        else
                            return that.xmlToJson(child, accumulator[nodeName], stripNamespaces, flatten);
                    });
                } else {
                    accumulator[nodeName] = $(xml).text();
                }
                return accumulator;
            }
        }
    })
    .factory('httpWrapper', function(){
        return {
            wrap: function(name, fun, context){
                var scope = this
        		if(typeof fun !== "function")
        			return
        		if(typeof scope[name] !== "object")
        			scope[name] = {}
                if(scope[name].loading)
                    return
        		scope[name].loading = true

        		var args = []
        		for(var i = 3; i < arguments.length; i++)
        			args.push(arguments[i])

        		var completion = function(){
        			scope[name].loading = false
        			scope.$apply()
        		}

                var xhrReq
        		if(context){
        			xhrReq = fun.apply(context, args)
        		} else {
        			xhrReq =  fun.apply(scope, args)
        		}
                if(xhrReq && xhrReq.xhr)
                    xhrReq.xhr.complete(completion)
                else
                    completion()
        	}
        }
    });

//routing
if(routes.routing){
	module.config(routes.routing);
}

for (let directive in directives) {
    ng.directives.push(directives[directive]);
}

//directives
module.directive('completeChange', function() {
	return {
		restrict: 'A',
		scope:{
			exec: '&completeChange',
			field: '=ngModel'
		},
		link: function(scope, element, attributes) {
			scope.$watch('field', function(newVal) {
				element.val(newVal);
				if(element[0].type === 'textarea' && element.hasClass('inline-editing')){
					setTimeout(function(){
						element.height(1);
						element.height(element[0].scrollHeight - 1);
					}, 100);

				}
			});

			element.bind('change', function() {
				scope.field = element.val();
				if(!scope.$$phase){
					scope.$apply('field');
				}
				scope.$parent.$eval(scope.exec);
				if(!scope.$$phase){
					scope.$apply('field');
				}
			});
		}
	};
});

module.directive('mediaLibrary', function($compile){
	return {
		restrict: 'E',
		scope: {
			ngModel: '=',
			ngChange: '&',
			multiple: '=',
			fileFormat: '='
		},
		templateUrl: '/' + appPrefix + '/public/template/entcore/media-library.html',
		link: function(scope, element, attributes){
			scope.$watch(function(){
				return scope.$parent.$eval(attributes.visibility);
			}, function(newVal){
				scope.visibility = newVal;
				if(!scope.visibility){
					scope.visibility = 'protected';
				}
				scope.visibility = scope.visibility.toLowerCase();
			});

			scope.upload = {
				loading: []
			};

			scope.$watch('ngModel', function(newVal){
				if((newVal && newVal._id) || (newVal && scope.multiple && newVal.length)){
					scope.ngChange();
				}

				scope.upload = {
					loading: []
				};
			});

			$('body').on('click', '.lightbox-backdrop', function(){
				scope.upload = {
					loading: []
				};
			});
		}
	}
});

module.directive('container', function($compile){
	return {
		restrict: 'E',
		scope: true,
		template: '<div ng-include="templateContainer"></div>',
		link: function(scope, element, attributes){
			scope.tpl = template;

			template.watch(attributes.template, function(){
				scope.templateContainer = template.containers[attributes.template];
				if(scope.templateContainer === 'empty'){
					scope.templateContainer = undefined;
				}
			});

			if(attributes.template){
				scope.templateContainer = template.containers[attributes.template];
			}
		}
	}
});

module.directive('colorSelect', function($compile){
	return {
		restrict: 'E',
		scope: {
			ngModel: '='
		},
		replace: true,
		template: '' +
			'<div class="color-picker" ng-class="{ opened: pickColor }">' +
				'<button class="colors-opener" type="button"></button>' +
				'<div class="colors-list">' +
					'<button type="button" ng-repeat="color in colors" class="[[color]]" ng-click="setColor(color)"></button>' +
				'</div>' +
			'</div>',
		link: function(scope, element, attributes){
			scope.colors = ['orange', 'pink', 'purple', 'blue', 'green', 'black', 'white', 'transparent'];
			scope.setColor = function(color){
			    scope.ngModel = color;
			};

			element.find('.colors-opener').on('click', function(e){
				scope.pickColor = !scope.pickColor;
				scope.$apply('pickColor');
				e.stopPropagation();
				$('body, .main').one('click', function(){
					scope.pickColor = false;
					scope.$apply('pickColor');
				});
			});
		}
	}
});

module.directive('soundSelect', function($compile){
	return {
		restrict: 'E',
		transclude: true,
		scope: {
			ngModel: '=',
			ngChange: '&',
			visibility: '@'
		},
        template: '<div><audio ng-src="[[ngModel]]" controls ng-if="ngModel" style="cursor: pointer"></audio>' +
            '<button ng-click="display.userSelecting = true">Sélectionner un fichier audio</button>' +
			'<lightbox show="display.userSelecting" on-close="userSelecting = false;">' +
			'<media-library ' +
				'visibility="selectedFile.visibility"' +
				'ng-change="updateDocument()" ' +
				'ng-model="selectedFile.file" ' +
				'file-format="\'audio\'">' +
			'</media-library>' +
			'</lightbox>' +
			'</div>',
        link: function (scope, element, attributes) {
            scope.display = {};
			scope.selectedFile = { file: {}, visibility: 'protected'};

			scope.selectedFile.visibility = scope.$parent.$eval(attributes.visibility);
			if(!scope.selectedFile.visibility){
				scope.selectedFile.visibility = 'protected';
			}
			scope.selectedFile.visibility = scope.selectedFile.visibility.toLowerCase();

			scope.updateDocument = function(){
				scope.display.userSelecting = false;
				var path = '/workspace/document/';
				if(scope.selectedFile.visibility === 'public'){
					path = '/workspace/pub/document/'
				}
				scope.ngModel = path + scope.selectedFile.file._id;
				scope.$apply('ngModel');
				scope.ngChange();
			};
			element.on('click', 'audio', function(){
				scope.userSelecting = true;
				scope.$apply('userSelecting');
			});
		}
	}
});

module.directive('mediaSelect', function($compile){
	return {
		restrict: 'E',
		transclude: true,
		replace: true,
		scope: {
			ngModel: '=',
			multiple: '=',
			ngChange: '&',
			fileFormat: '=',
			label: "@",
			class: "@",
			value: '@',
			mytooltip: "@"
		},
		template: '<div><input type="button" class="pick-file [[class]]" tooltip="[[mytooltip]]" />' +
					'<lightbox show="userSelecting" on-close="userSelecting = false;">' +
						'<media-library ng-change="updateDocument()" ng-model="selectedFile.file" multiple="multiple" file-format="fileFormat" visibility="selectedFile.visibility"></media-library>' +
					'</lightbox>' +
				'</div>',
		compile: function(element, attributes){

			if(!attributes.mytooltip && attributes.tooltip){
				console.warn('tooltip attribute is deprecated on media-select tag, use mytooltip instead.');
				element.attr("mytooltip", attributes.tooltip);
				element.removeAttr('tooltip');
				attributes.mytooltip = attributes.tooltip;
				delete attributes.tooltip;
			}

			return function(scope, element, attributes){
			//link: function(scope, element, attributes){
				scope.selectedFile = { file: {}, visibility: 'protected' };
				scope.selectedFile.visibility = scope.$parent.$eval(attributes.visibility);
				if(!scope.selectedFile.visibility){
					scope.selectedFile.visibility = 'protected';
				}
				scope.selectedFile.visibility = scope.selectedFile.visibility.toLowerCase();

				if(!scope.mytooltip){
					element.find('input').removeAttr('tooltip');
				}

				attributes.$observe('label', function(newVal){
					element.find('[type=button]').attr('value', lang.translate(newVal));
				});

				scope.$watch('fileFormat', function(newVal){
					if(newVal === undefined){
						scope.fileFormat = 'img'
					}
				});
				scope.updateDocument = function(){
					scope.userSelecting = false;
					var path = '/workspace/document/';
					if(scope.selectedFile.visibility === 'public'){
						path = '/workspace/pub/document/'
					}
					scope.ngModel = path + scope.selectedFile.file._id;
					scope.$apply('ngModel');
					scope.ngChange();
				};
				element.find('.pick-file').on('click', function(){
					scope.userSelecting = true;
					scope.$apply('userSelecting');
				});
			}
		}
	}
});

module.directive('filesPicker', function($compile){
	return {
		restrict: 'E',
		transclude: true,
		replace: true,
		template: '<input type="button" ng-transclude />',
		scope: {
			ngChange: '&',
			ngModel: '='
		},
		link: function(scope, element, attributes){
			element.on('click', function(){
				var fileSelector = $('<input />', {
					type: 'file'
				})
					.hide()
					.appendTo('body');
				if(attributes.multiple !== undefined){
					fileSelector.attr('multiple', true);
				}

				fileSelector.on('change', function(){
					scope.ngModel = fileSelector[0].files;
					scope.$apply();
					scope.$eval(scope.ngChange);
					scope.$parent.$apply();
				});
				fileSelector.click();
				fileSelector.trigger('touchstart');
			});
		}
	}
})

module.directive('filesInputChange', function($compile){
	return {
		restrict: 'A',
		scope: {
			filesInputChange: '&',
			file: '=ngModel'
		},
		link: function($scope, $element){
			$element.bind('change', function(){
				$scope.file = $element[0].files;
				$scope.$apply();
				$scope.filesInputChange();
				$scope.$apply();
			})
		}
	}
})

module.directive('iconsSelect', function($compile) {
	return {
		restrict: 'E',
		scope:{
			options: '=',
			class: '@',
			current: '=',
			change: '&'
		},
		link: function(scope, element, attributes){
			element.bind('change', function(){
				scope.current.id = element.find('.current').data('selected');
				scope.$eval(scope.change);
				element.unbind('change');
			})
		},
		template: '' +
			'<div>' +
				'<div class="current fixed cell twelve" data-selected="[[current.id]]">' +
					'<i class="[[current.icon]]"></i>' +
					'<span translate content="[[current.text]]"></span>' +
				'</div>' +
				'<div class="options-list icons-view">' +
				'<div class="wrapper">' +
					'<div class="cell three option" data-value="[[option.id]]" data-ng-repeat="option in options">' +
						'<i class="[[option.icon]]"></i>' +
						'<span translate content="[[option.text]]"></span>' +
					'</div>' +
				'</div>' +
				'</div>' +
			'</div>'
	};
});

module.directive('preview', function($compile){
	return {
		restrict: 'E',
		template: '<div class="row content-line"><div class="row fixed-block height-four">' +
			'<div class="four cell fixed image clip text-container"></div>' +
			'<div class="eight cell fixed-block left-four paragraph text-container"></div>' +
			'</div></div>',
		replace: true,
		scope: {
			content: '='
		},
		link: function($scope, $element, $attributes){
				$scope.$watch('content', function(newValue){
					var fragment = $(newValue);
					$element.find('.image').html(fragment.find('img').first());

					var paragraph = _.find(fragment.find('p'), function(node){
						return $(node).text().length > 0;
					});
					$element.find('.paragraph').text($(paragraph).text());
				})
			}
		}
});

module.directive('portal', function($compile){
	return {
		restrict: 'E',
		transclude: true,
		templateUrl: skin.portalTemplate,
		compile: function(element, attributes, transclude){
			element.find('[logout]').attr('href', '/auth/logout?callback=' + skin.logoutCallback);
			ui.setStyle(skin.theme);
			Http.prototype.bind('disconnected', function(){
				window.location.href = '/';
			})
		}
	}
});

module.directive('adminPortal', function($compile){
	skin.skin = 'admin';
	skin.theme = '/public/admin/default/';
	return {
		restrict: 'E',
		transclude: true,
		templateUrl: '/public/admin/portal.html',
		compile: function(element, attributes, transclude){
			$('[logout]').attr('href', '/auth/logout?callback=' + skin.logoutCallback);
			http().get('/userbook/preference/admin').done(function(data){
				var theme = data.preference ? JSON.parse(data.preference) : null

				if(!theme || !theme.path)
					ui.setStyle(skin.theme)
				else{
					ui.setStyle('/public/admin/'+theme.path+'/')
				}
			}).error(function(error){
				ui.setStyle(skin.theme)
			})
		}
	}
});

module.directive('portalStyles', function($compile){
	return {
		restrict: 'E',
		compile: function(element, attributes){
			$('[logout]').attr('href', '/auth/logout?callback=' + skin.logoutCallback);
			ui.setStyle(skin.theme);
		}
	}
});

module.directive('defaultStyles', function($compile){
	return {
		restrict: 'E',
		link: function(scope, element, attributes){
			ui.setStyle(skin.theme);
		}
	}
});

module.directive('skinSrc', function($compile){
	return {
		restrict: 'A',
		scope: '&',
		link: function($scope, $element, $attributes){
			if(!$('#theme').attr('href')){
				return;
			}
			var path = skin.basePath;
			$attributes.$observe('skinSrc', function(){
				if($attributes.skinSrc.indexOf('http://') === -1 && $attributes.skinSrc.indexOf('https://') === -1 && $attributes.skinSrc.indexOf('/workspace/') === -1){
					$element.attr('src', path + $attributes.skinSrc);
				}
				else{
					$element.attr('src', $attributes.skinSrc);
				}
			});

		}
	}
});

module.directive('localizedClass', function($compile){
	return {
		restrict: 'A',
		link: function($scope, $attributes, $element){
			$element.$addClass(currentLanguage);
		}
	}
});

module.directive('pullDownMenu', function($compile, $timeout){
	return {
		restrict: 'E',
		transclude: true,
		template: '<div class="pull-down-menu hide" ng-transclude></div>',
		controller: function($scope){
		}
	}
});

module.directive('pullDownOpener', function($compile, $timeout){
	return {
		restrict: 'E',
		require: '^pullDownMenu',
		transclude: true,
		template: '<div class="pull-down-opener" ng-transclude></div>',
		link: function(scope, element, attributes){
			element.find('.pull-down-opener').on('click', function(){
				var container = element.parents('.pull-down-menu');
				if(container.hasClass('hide')){
					setTimeout(function(){
						$('body').on('click.pulldown', function(){
							container.addClass('hide');
							$('body').unbind('click.pulldown');
						});
					}, 0);
					container.removeClass('hide');

				}
				else{
					$('body').unbind('click.pulldown');
					container.addClass('hide');
				}
			});
		}
	}
});

module.directive('pullDownContent', function($compile, $timeout){
	return {
		restrict: 'E',
		require: '^pullDownMenu',
		transclude: true,
		template: '<div class="wrapper"><div class="arrow"></div><div class="pull-down-content" ng-transclude></div></div>',
		link: function(scope, element, attributes){
		}
	}
});

module.directive('topNotification', function(){
    return {
		restrict: 'E',
		template:
            '<div class="notify-top">'+
                '<div class="notify-top-content" ng-bind-html="content"></div>'+
                '<div class="notify-top-actions">'+
                    '<span ng-click="cancel()">[[doConfirm ? labels().cancel : labels().ok]]</span>'+
                    '<span ng-click="ok()" ng-show="doConfirm">[[labels().confirm]]</span> '+
                '</div>'+
            '</div>',
        scope: {
            trigger: '=',
            confirm: '=',
            content: '=',
            labels: '&'
        },
		link: function(scope, element, attributes){
            element.css('display', 'none')
            scope.doConfirm = false
            scope.cancel = function(){
                scope.trigger = false
            }
            scope.ok = function(){
                scope.trigger = false
                scope.confirm()
            }
            if(!scope.labels()){
                scope.labels = function(){
                    return {
                        confirm: lang.translate('confirm'),
                        cancel: lang.translate('cancel'),
                        ok: lang.translate('ok')
                    }
                }
            }
            scope.$watch('trigger', function(newVal){
                if(newVal)
                    element.slideDown()
                else
                    element.slideUp()
            });
            scope.$watch('confirm', function(newVal){
                scope.doConfirm = newVal ? true : false
            })
		}
	}
});

module.directive('dropDown', function($compile, $timeout){
	return {
		restrict: 'E',
		scope: {
			options: '=',
			ngChange: '&',
			onClose: '&',
			ngModel: '='
		},
		template: '<div data-drop-down class="drop-down">' +
						'<div>' +
							'<ul class="ten cell right-magnet">' +
								'<li ng-repeat="option in options | limitTo:limit" ng-model="option">[[option.toString()]]</li>' +
								'<li class="display-more" ng-show="limit < options.length" ng-click="increaseLimit()">' + lang.translate('seemore') + '</li>' +
							'</ul>' +
						'</div>' +
					'</div>',
		link: function(scope, element, attributes){
			scope.limit = 6;
			var dropDown = element.find('[data-drop-down]');
			scope.setDropDownHeight = function(){
				var liHeight = 0;
				var max = Math.min(scope.limit, scope.options.length);
				dropDown.find('li').each(function(index, el){
					liHeight += $(el).height();
					return index < max;
				});
				dropDown.height(liHeight)
			};
			scope.increaseLimit = function(){
				scope.limit += 5;
				$timeout(function(){
					scope.setDropDownHeight()
				});
			};
			scope.$watchCollection('options', function(newValue){
				if(!scope.options || scope.options.length === 0){
					dropDown.height();
					dropDown.addClass('hidden');
					scope.limit = 6;
					dropDown.attr('style', '');
					return;
				}
				dropDown.removeClass('hidden');
				var linkedInput = $('#' + attributes.for);
				var pos = linkedInput.offset();
				var width = linkedInput.width() +
					parseInt(linkedInput.css('padding-right')) +
					parseInt(linkedInput.css('padding-left')) +
					parseInt(linkedInput.css('border-width') || 1) * 2;
				var height = linkedInput.height() +
					parseInt(linkedInput.css('padding-top')) +
					parseInt(linkedInput.css('padding-bottom')) +
					parseInt(linkedInput.css('border-height') || 1) * 2;

				pos.top = pos.top + height;
				dropDown.offset(pos);
				dropDown.width(width);
				scope.setDropDownHeight();
				setTimeout(function(){
					scope.setDropDownHeight()
				}, 100);
			});

			dropDown.detach().appendTo('body');

			dropDown.on('click', 'li', function(e){
				if($(e.target).hasClass('display-more')){
					return;
				}
				scope.limit = 6;
				dropDown.attr('style', '');
				scope.current = $(this).scope().option;
				scope.ngModel = $(this).scope().option;
				scope.$apply('ngModel');
				scope.$eval(scope.ngChange);
				scope.$eval(scope.onClose);
				scope.$apply('ngModel');
			});

			var closeDropDown = function(e){
				if(dropDown.find(e.target).length > 0){
					return;
				}
				scope.$eval(scope.onClose);
				scope.$apply();
			};

			$('body').on('click', closeDropDown);
			dropDown.attr('data-opened-drop-down', true);
			element.on('$destroy', function(){
				$('body').unbind('click', closeDropDown);
				dropDown.remove();
			});
		}
	}
});

module.directive('dropDownButton', function(){
	return {
		restrict: 'E',
		transclude: 'true',
		controller: function($scope){
		},
		template: '<div class="drop-down-button hidden"><div ng-transclude></div></div>',
		link: function(scope, element, attributes){
			element.on('click', '.opener', function(){
				element.find('.drop-down-button').removeClass('hidden');
				$(document).one('mousedown', function(e){
					setTimeout(function() {
						element.find('.drop-down-button').addClass('hidden');
					}, 200);
				});
			});
		}
	}
});

module.directive('opts', function(){
	return {
		restrict: 'E',
		require: '^dropDownButton',
		transclude: true,
		template: '<div class="options"><ul ng-transclude></ul></div>',
		link: function(scope, element, attributes){
			element.on('click', 'li', function(){
				element.parents('.drop-down-button').addClass('hidden');
			});
		}
	}
});

module.directive('loadingIcon', function($compile){
	return {
		restrict: 'E',
		link: function($scope, $element, $attributes){
			var addImage = function(){
                if($('#theme').length === 0)
                    return;
				var loadingIllustrationPath = skin.basePath + '/img/icons/anim_loading_small.gif';
				$('<img>')
					.attr('src', loadingIllustrationPath)
					.attr('class', $attributes.class)
					.addClass('loading-icon')
					.appendTo($element);
			};
			if($attributes.default=== 'loading'){
				addImage();
			}
			http().bind('request-started.' + $attributes.request, function(e){
				$element.find('img').remove();
				addImage();
			});

            if($attributes.onlyLoadingIcon === undefined){
    			http().bind('request-ended.' + $attributes.request, function(e){
    				var loadingDonePath = skin.basePath + '/img/icons/checkbox-checked.png';
    				$element.find('.loading-icon').remove();
    				$('<img>')
    					.attr('src', loadingDonePath)
    					.appendTo($element);
    			});
            } else {
                http().bind('request-ended.' + $attributes.request, function(e){
                    $element.find('img').remove();
                })
            }
		}
	}
})

module.directive('loadingPanel', function($compile){
	return {
		restrict: 'A',
		link: function($scope, $element, $attributes){
			$attributes.$observe('loadingPanel', function(val) {
				http().bind('request-started.' + $attributes.loadingPanel, function(e){
					var loadingIllustrationPath = skin.basePath + '/img/illustrations/loading.gif';
					if($element.children('.loading-panel').length === 0){
						$element.append('<div class="loading-panel">' +
							'<h1>' + lang.translate('loading') + '</h1>' +
							'<img src="' + loadingIllustrationPath + '" />' +
							'</div>');
					}

				})
				http().bind('request-ended.' + $attributes.loadingPanel, function(e){
					$element.find('.loading-panel').remove();
				})
			});
		}
	}
});

module.directive('userRole', function($compile){
	return {
		restrict: 'A',
		link: function($scope, $element, $attributes){
			var auth = $attributes.userRole;
			if(!model.me.functions[auth]){
				$element.hide();
			}
			else{
				$element.show();
			}
		}
	}
});

module.directive('behaviour', function($compile){
	return {
		restrict: 'E',
		template: '<div ng-transclude></div>',
		replace: false,
		transclude: true,
		scope: {
			resource: '='
		},
		link: function($scope, $element, $attributes){
			console.error('This directive is deprecated. Please use "authorize" instead.');
			if(!$attributes.name){
				throw "Behaviour name is required";
			}
			var content = $element.children('div');
			$scope.$watch('resource', function(newVal){
				var hide = ($scope.resource instanceof Array && _.find($scope.resource, function(resource){ return !resource.myRights || resource.myRights[$attributes.name] === undefined; }) !== undefined) ||
					($scope.resource instanceof Model && (!$scope.resource.myRights || $scope.resource.myRights[$attributes.name] === undefined));

				if(hide){
					content.hide();
				}
				else{
					content.show();
				}

			});
		}
	}
});

module.directive('authorize', function($compile){
	return {
		restrict: 'EA',
		link: function(scope, element, attributes){
			if(attributes.name === undefined && attributes.authorize === undefined){
				throw "Right name is required";
			}
			var content = element.children('div');

			var switchHide = function(){
				var resource = scope.$eval(attributes.resource);
				var name = attributes.name || attributes.authorize;
				var hide = name && (resource instanceof Array && _.find(resource, function(resource){ return !resource.myRights || resource.myRights[name] === undefined; }) !== undefined) ||
					(resource instanceof Model && (!resource.myRights || resource.myRights[name] === undefined));

				if(hide){
					content.remove();
					element.hide();
				}
				else{
					element.append(content);
					element.show();
				}
			};

			attributes.$observe('name', switchHide);
			attributes.$observe('authorize', switchHide);
			scope.$watch(function(){ return scope.$eval(attributes.resource); }, switchHide);
		}
	}
});

module.directive('drawingZone', function(){
	return function($scope, $element, $attributes){
		$element.addClass('drawing-zone');
	};
});

module.directive('resizable', function(){
	return {
		restrict: 'A',
		link: function(scope, element, attributes){

			ui.extendElement.resizable(element, {
				lock: {
					horizontal: element.attr('horizontal-resize-lock'),
					vertical: element.attr('vertical-resize-lock')
				}
			});
		}
	}
});

module.directive('placedBlock', function($compile){
	return {
		restrict: 'E',
		replace: true,
		transclude: true,
		scope: {
			x: '=',
			y: '=',
			z: '=',
			h: '=',
			w: '='
		},
		template: '<article ng-transclude ng-style="{\'z-index\': z }"></article>',
		link: function(scope, element){
			element.css({ 'position': 'absolute' });
			scope.$watch('x', function(newVal){
				element.offset({
					top: element.offset().top,
					left: parseInt(newVal) + element.parent().offset().left
				});
			});

			scope.$watch('y', function(newVal){
				element.offset({
					left: element.offset().left,
					top: parseInt(newVal) + element.parent().offset().top
				});
			});

			var toTop = function(){
				$(':focus').blur();
				if(scope.z === undefined){
					return;
				}
				element.parents('.drawing-zone').find('article[draggable]').each(function(index, item){
					var zIndex = $(item).css('z-index');
					if(!scope.z){
						scope.z = 1;
					}
					if(parseInt(zIndex) && parseInt(zIndex) >= scope.z){
						scope.z = parseInt(zIndex) + 1;
					}
				});
				if(scope.z){
					scope.$apply('z');
				}
			};

			element.on('startDrag', toTop);
			element.on('startResize', function(){
				scope.w = element.width();
				scope.$apply('w');
				scope.h = element.height();
				scope.$apply('h');
				toTop();
			});

			element.on('stopDrag', function(){
				scope.x = element.position().left;
				scope.$apply('x');
				scope.y = element.position().top;
				scope.$apply('y');
			});

			scope.$watch('z', function(newVal){
				element.css({ 'z-index': scope.z })
			});

			scope.$watch('w', function(newVal){
				element.width(newVal);
			});
			element.on('stopResize', function(){
				scope.w = element.width();
				scope.$apply('w');
				scope.h = element.height();
				scope.$apply('h');
			});

			scope.$watch('h', function(newVal){
				element.height(newVal);
			});
		}
	}
});

module.directive('draggable', function($compile){
	return {
		restrict: 'A',
		link: function(scope, element, attributes){
			if(attributes.draggable == 'false' || attributes.native !== undefined){
				return;
			}
			ui.extendElement.draggable(element, {
				mouseUp: function(){
					element.on('click', function(){
						scope.$parent.$eval(attributes.ngClick);
					});
				}
			});
		}
	}
});

module.directive('sharePanel', function($compile){
	return {
		scope: {
			resources: '=',
			appPrefix: '='
		},
		restrict: 'E',
		templateUrl: '/' + appPrefix + '/public/template/entcore/share-panel.html',
		link: function($scope, $element, $attributes){
			$scope.shareTable = '/' + appPrefix + '/public/template/entcore/share-panel-table.html';
		}
	}
});

module.directive('widgets', function($compile){
	return {
		scope: {
			list: '='
		},
		restrict: 'E',
		templateUrl: '/' + appPrefix + '/public/template/entcore/widgets.html',
		link: function(scope, element, attributes){
			element.on('index-changed', '.widget-container', function(e){
				var widgetObj = angular.element(e.target).scope().widget;
				element.find('.widget-container').each(function(index, widget){
					if(e.target === widget){
						return;
					}
					if($(e.target).offset().top + ($(e.target).height() / 2) > $(widget).offset().top &&
						$(e.target).offset().top + ($(e.target).height() / 2) < $(widget).offset().top + $(widget).height()){
						widgetObj.setIndex(index);
						scope.$apply('widgets');
					}
					//last widget case
					if($(e.target).offset().top > $(widget).offset().top + $(widget).height() && index === element.find('.widget-container').length - 1){
						widgetObj.setIndex(index);
						scope.$apply('widgets');
					}
					//first widget case
					if($(e.target).offset().top + $(e.target).height() > $(widget).offset().top && index === 0){
						widgetObj.setIndex(index);
						scope.$apply('widgets');
					}
				});
				element.find('.widget-container').css({ position: 'relative', top: '0px', left: '0px' });
			});
			model.widgets.on('change', function(){
				if(element.find('.widget-container').length === 0){
					element
						.parents('.widgets')
						.next('.widgets-friend')
						.addClass('widgets-enemy');
					element.parents('.widgets').addClass('hidden');
				}
				else{
					element
						.parents('.widgets')
						.next('.widgets-friend')
						.removeClass('widgets-enemy');
					element.parents('.widgets').removeClass('hidden');
				}
			});
		}
	}
});

module.directive('progressBar', function($compile){
	return {
		restrict: 'E',
		scope: {
			max: '=',
			filled: '=',
			unit: '@'
		},
		template: '<div class="progress-bar">' +
			'<div class="filled">[[filled]] <span translate content="[[unit]]"></span></div>[[max]] <span translate content="[[unit]]"></span>' +
			'</div>',
		link: function(scope, element, attributes){
			function updateBar(){
				var filledPercent = scope.filled * 100 / scope.max;
				element.find('.filled').width(filledPercent + '%');
				if(filledPercent < 10){
					element.find('.filled').addClass('small');
				}
				else{
					element.find('.filled').removeClass('small');
				}
			}

			scope.$watch('filled', function(newVal){
				updateBar();
			});

			scope.$watch('max', function(newVal){
				updateBar();
			});
		}
	}
});

module.directive('datePicker', function($compile){
	return {
		scope: {
			minDate: '=',
			ngModel: '=',
			ngChange: '&'
		},
		transclude: true,
		replace: true,
		restrict: 'E',
		template: '<input ng-transclude type="text" data-date-format="dd/mm/yyyy"  />',
		link: function(scope, element, attributes){
			scope.$watch('ngModel', function(newVal){
				element.val(moment(scope.ngModel).format('DD/MM/YYYY'));
                if(element.datepicker)
                    element.datepicker('setValue', moment(scope.ngModel).format('DD/MM/YYYY'));
			});

			if(scope.minDate){
				scope.$watch('minDate', function(newVal){
					setNewDate();
				});
			}

			function setNewDate(){
				var minDate = scope.minDate;
				var date = element.val().split('/');
				var temp = date[0];
				date[0] = date[1];
				date[1] = temp;
				date = date.join('/');
				scope.ngModel = new Date(date);

				if(scope.ngModel < minDate){
					scope.ngModel = minDate;
					element.val(moment(minDate).format('DD/MM/YYYY'));
				}

				scope.$apply('ngModel');
				scope.$parent.$eval(scope.ngChange);
				scope.$parent.$apply();
			}

			http().loadScript('/' + infraPrefix + '/public/js/bootstrap-datepicker.js').then(function(){
				element.datepicker({
						dates: {
							months: moment.months(),
							monthsShort: moment.monthsShort(),
							days: moment.weekdays(),
							daysShort: moment.weekdaysShort(),
							daysMin: moment.weekdaysMin()
						},
						weekStart: 1
					})
					.on('changeDate', function(){
						setTimeout(setNewDate, 10);

						$(this).datepicker('hide');
					});
				element.datepicker('hide');
			});

			var hideFunction = function(e){
				if(e.originalEvent && (element[0] === e.originalEvent.target || $('.datepicker').find(e.originalEvent.target).length !== 0)){
					return;
				}
				element.datepicker('hide');
			};
			$('body, lightbox').on('click', hideFunction);
			$('body, lightbox').on('focusin', hideFunction);

			element.on('focus', function(){
				var that = this;
				$(this).parents('form').on('submit', function(){
					$(that).datepicker('hide');
				});
				element.datepicker('show');
			});

			element.on('change', setNewDate);

			element.on('$destroy', function(){
				element.datepicker('hide');
			});
		}
	}
});

module.directive('datePickerIcon', function($compile){
	return {
		scope: {
			ngModel: '=',
			ngChange: '&'
		},
		replace: true,
		restrict: 'E',
		template: '<div class="date-picker-icon"> <input type="text" class="hiddendatepickerform" style="visibility: hidden; width: 0px; height: 0px; float: inherit" data-date-format="dd/mm/yyyy"/> <a ng-click="openDatepicker()"><i class="calendar"/></a> </div>',
		link: function($scope, $element, $attributes){
			http().loadScript('/' + infraPrefix + '/public/js/bootstrap-datepicker.js').then(() => {
				var input_element   = $element.find('.hiddendatepickerform')
				input_element.value = moment(new Date()).format('DD/MM/YYYY')

				input_element.datepicker({
					dates: {
						months: moment.months(),
						monthsShort: moment.monthsShort(),
						days: moment.weekdays(),
						daysShort: moment.weekdaysShort(),
						daysMin: moment.weekdaysMin()
					},
					weekStart: 1
				})
				.on('changeDate', function(event){
					$scope.ngModel = event.date
					$scope.$apply('ngModel')
					$(this).datepicker('hide');
					if(typeof $scope.ngChange === 'function'){
						$scope.ngChange();
					}
				});

				input_element.datepicker('hide')

				$scope.openDatepicker = function(){
					input_element.datepicker('show')
				}
			});
		}
	}
});

module.directive('filters', function(){
	return {
		restrict: 'E',
		template: '<div class="row line filters">' +
			'<div class="filters-icons">' +
					'<ul ng-transclude>' +
					'</ul></div>' +
				'</div><div class="row"></div> ',
		transclude: true,
		link: function(scope, element, attributes){
		}
	}
});

module.directive('alphabetical', function($compile, $parse){
	return {
		restrict: 'E',
		controller: function($scope){
			$scope.letters = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '#'];
			$scope.matchingElements = {};
			$scope.matching = function(letter){
				return function(element){
					return element[$scope.title][0].toUpperCase() === letter || (letter === '#' && element[$scope.title][0].toLowerCase() === element[$scope.title][0].toUpperCase());
				}
			};

			$scope.updateElements = function(){
				$scope.letters.forEach(function(letter){
					$scope.matchingElements[letter] = _.filter($scope.collection($scope), function(element){
						return element[$scope.title][0].toUpperCase() === letter || (letter === '#' && element[$scope.title][0].toLowerCase() === element[$scope.title][0].toUpperCase());
					});
				});
			};

			if(!$scope.display){
				$scope.display = {};
			}
			$scope.display.pickLetter;
		},
		compile: function(element, attributes){
			var iterator = attributes.list;
			var iteratorContent = element.html();
			element.html('<lightbox class="letter-picker" show="display.pickLetter" on-close="display.pickLetter = false;">' +
				'<div ng-repeat="letter in letters"><h2 ng-click="viewLetter(letter)" class="cell" ng-class="{disabled: matchingElements[letter].length <= 0 }">[[letter]]</h2></div>' +
				'</lightbox>' +
				'<div ng-repeat="letter in letters">' +
				'<div ng-if="matchingElements[letter].length > 0" class="row">' +
				'<h1 class="letter-picker" ng-click="display.pickLetter = true;" id="alphabetical-[[letter]]">[[letter]]</h1><hr class="line" />' +
				'<div class="row"><div ng-repeat="' + iterator + ' |filter: matching(letter)">' + iteratorContent + '</div></div>' +
				'</div><div ng-if="matchingElements[letter].length > 0" class="row"></div>' +
				'</div>');
			element.addClass('alphabetical');
			var match = iterator.match(/^\s*([\s\S]+?)\s+in\s+([\s\S]+?)(?:\s+as\s+([\s\S]+?))?(?:\s+track\s+by\s+([\s\S]+?))?\s*$/);
			var collection = match[2];
			return function(scope, element, attributes){
				scope.title = attributes.title || 'title';
				element.removeAttr('title');
				scope.collection = $parse(collection);
				scope.$watchCollection(collection, function(newVal){
					scope.updateElements();
				});
				scope.updateElements();
				scope.viewLetter = function(letter){
					document.getElementById('alphabetical-' + letter).scrollIntoView();
					scope.display.pickLetter = false;
				};
			}
		}
	}
});

module.directive('completeClick', function($parse){
	return {
		compile: function(selement, attributes){
			var fn = $parse(attributes.completeClick);
			return function(scope, element, attributes) {
				element.on('click', function(event) {
					scope.$apply(function() {
						fn(scope, {$event:event});
					});
				});
			};
		}
	}
});

module.directive('dragstart', function($parse){
    return {
        restrict: 'A',
        link: function(scope, element, attributes){
			var dragStartFn = $parse(attributes.dragstart);
			var ngModel = $parse(attributes.ngModel);
            if(attributes.dragcondition !== undefined && scope.$eval(attributes.dragcondition) === false){
                element.attr("draggable", "false");
                return
            }

            element.attr("draggable", "true");
            element.attr("native", "");

            element.on("dragstart", function(event){
				if(ngModel && ngModel(scope)){
					try{
						event.originalEvent.dataTransfer.setData('application/json', JSON.stringify(ngModel(scope)));
					} catch(e) {
						event.originalEvent.dataTransfer.setData('Text', JSON.stringify(ngModel(scope)));
					}
				}
				if(attributes.dragstart === ''){

					return;
				}
                dragStartFn(scope, { $originalEvent: event.originalEvent });
            });

            element.on('$destroy', function() {
                element.off()
            })
        }
    }
})

module.directive('dragdrop', function($parse){
    return {
        restrict: 'A',
        link: function(scope, element, attributes){
			var dropFn = $parse(attributes.dragdrop);
			var dropConditionFn = $parse(attributes.dropcondition);
            element.on("dragover", function(event){
                if(attributes.dropcondition === undefined || dropConditionFn(scope, { $originalEvent: event.originalEvent })){
                   event.preventDefault();
                   event.stopPropagation();
                   element.addClass("droptarget")
                }
            });

            element.on("dragleave", function(event){
                event.preventDefault();
                event.stopPropagation();
                element.removeClass("droptarget");
            });

            element.on("drop", function(event){
				event.originalEvent.preventDefault();
                element.removeClass("droptarget");
				var item;
				try{
					item = JSON.parse(event.originalEvent.dataTransfer.getData('application/json'));
				}
				catch(e){
					item = JSON.parse(event.originalEvent.dataTransfer.getData('Text'));
				}
				dropFn(scope, { $originalEvent: event.originalEvent, $item: item });
            });

            element.on('$destroy', function() {
                element.off()
            });
        }
    }
});

module.directive('dropFiles', function($parse){
	return {
		link: function(scope, element, attributes){
			var ngModel = $parse(attributes.dropFiles);
			element.on('dragover', function(e){
				e.preventDefault();
				scope.$eval(attributes.onDrag);
				element.addClass('droptarget');
			});

			element.on('dragleave', function(e){
				e.preventDefault();
				scope.$eval(attributes.onLeave);
				element.removeClass('droptarget');
			});

			element.on('drop', function(e){
				e.preventDefault();
				ngModel.assign(scope, e.originalEvent.dataTransfer.files);
				scope.$eval(attributes.onDrop);
				scope.$apply();
				element.removeClass('droptarget');
			});
		}
	}
});

module.directive('attachments', function($parse){
	return {
		scope: true,
		restrict: 'E',
		templateUrl: '/' + appPrefix + '/public/template/entcore/attachments.html',
		controller: function($scope){
			$scope.linker = {
				resource: {}
			}
			$scope.attachments = {
				me: model.me,
				display: {
					search: { text: '', application: {} },
					pickFile: false
				},
				addAttachment: function(resource){
					resource.provider = $scope.attachments.display.search.application;
					if($scope.ngModel($scope) instanceof Array){
						$scope.ngModel($scope).push(resource);
					}
					else{
						$scope.ngModel.assign($scope, [resource]);
					}
					$scope.attachments.display.pickFile = false;
				},
				removeAttachment: function(resource){
					$scope.ngModel.assign($scope, _.reject($scope.ngModel($scope), function(item){
						return item === resource;
					}));
				},
				attachmentsList: function(){
					$scope.list = $scope.ngModel($scope);
					return $scope.list;
				}
			};
		},
		link: function(scope, element, attributes){
			scope.ngModel = $parse(attributes.ngModel);
			scope.attachments.onChange = function(){
				scope.$eval(attributes.onChange);
			};
			scope.apps = scope.$eval(attributes.apps);
			scope.$watch(
				function(){
					return scope.attachments.display.pickFile
				},
				function(newVal){
					if(newVal){
						scope.attachments.loadApplicationResources(function(){
							scope.attachments.searchApplication();
							scope.attachments.display.search.text = ' ';
							scope.$apply('attachments');
						});
					}
					else{
						scope.attachments.display.search.text = '';
					}
				},
				true
			);

			http().get('/resources-applications').done(function(apps){
				scope.attachments.apps = _.filter(model.me.apps, function(app){
					return _.find(apps, function(match){
						return app.address.indexOf(match) !== -1 && app.icon
					}) && _.find(scope.apps, function(match){
						return app.address.indexOf(match) !== -1
					});
				});

				scope.attachments.display.search.application = scope.attachments.apps[0];
				scope.attachments.loadApplicationResources(function(){});

				scope.$apply('attachments');
			});

			scope.attachments.loadApplicationResources = function(cb){
				if(!cb){
					cb = function(){
						scope.attachments.display.searchApplication();
						scope.$apply('attachments');
					};
				}

				var split = scope.attachments.display.search.application.address.split('/');
				scope.prefix = split[split.length - 1];

				Behaviours.loadBehaviours(scope.prefix, function(appBehaviour){
					appBehaviour.loadResources(cb);
					scope.attachments.addResource = appBehaviour.create;
				});
			};

			scope.attachments.searchApplication = function(){
				var split = scope.attachments.display.search.application.address.split('/');
				scope.prefix = split[split.length - 1];

				Behaviours.loadBehaviours(scope.prefix, function(appBehaviour){
					scope.attachments.resources = _.filter(appBehaviour.resources, function(resource) {
						return scope.attachments.display.search.text !== '' && (lang.removeAccents(resource.title.toLowerCase()).indexOf(lang.removeAccents(scope.attachments.display.search.text).toLowerCase()) !== -1 ||
							resource._id === scope.attachments.display.search.text);
					});
				});
			};

			scope.linker.createResource = function(){
				var split = scope.attachments.display.search.application.address.split('/');
				var prefix = split[split.length - 1];

				Behaviours.loadBehaviours(prefix, function(appBehaviour){
					appBehaviour.create(scope.linker.resource, function(resources, newResource){
						if(!(scope.ngModel(scope) instanceof Array)){
							scope.ngModel.assign(scope, []);
						}
						scope.attachments.display.pickFile = false;
						var resource = _.find(resources, function(resource){
							return resource._id === newResource._id;
						})
						resource.provider = scope.attachments.display.search.application;
						scope.ngModel(scope).push(resource);
						scope.$apply();
					});
				});
			};
		}
	}
});

module.directive('pdfViewer', function(){
	return {
		restrict: 'E',
		template: '' +
		'<div class="file-controls">' +
			'<i class="previous" ng-click="previousPage()"></i>' +
			'<i class="next" ng-click="nextPage()"></i>' +
		'</div>' +
		'<div class="pagination">' +
			'<input type="text" ng-model="pageIndex" ng-change="openPage()" /> / [[numPages]]' +
		'</div>',
		link: function(scope, element, attributes){
			var pdf;
			scope.pageIndex = 1;
			scope.nextPage = function(){
				if(scope.pageIndex < scope.numPages){
					scope.pageIndex ++;
					scope.openPage();
				}
			};
			scope.previousPage = function(){
				if(scope.pageIndex > 0){
					scope.pageIndex --;
					scope.openPage();
				}
			};
			scope.openPage = function(){
				var pageNumber = parseInt(scope.pageIndex);
				if(!pageNumber){
					return;
				}
				if(pageNumber < 1){
					pageNumber = 1;
				}
				if(pageNumber > scope.numPages){
					pageNumber = scope.numPages;
				}
				pdf.getPage(pageNumber).then(function (page) {
					var viewport;
					if(!$(canvas).hasClass('fullscreen')){
						viewport = page.getViewport(1);
						var scale = element.width() / viewport.width;
						viewport = page.getViewport(scale);
					}
					else{
						viewport = page.getViewport(2);
					}

					var context = canvas.getContext('2d');
					canvas.height = viewport.height;
					canvas.width = viewport.width;

					var renderContext = {
						canvasContext: context,
						viewport: viewport
					};
					page.render(renderContext);
				});
			};
			scope.$parent.render = scope.openPage;

			(window as any).PDFJS = { workerSrc: '/infra/public/js/viewers/pdf.js/pdf.worker.js' };
			var canvas = document.createElement('canvas');
			$(canvas).addClass('render');
			element.append(canvas);
			http().loadScript('/infra/public/js/viewers/pdf.js/pdf.js').then(() => {
				(window as any).PDFJS
						.getDocument(attributes.ngSrc)
						.then(function(file){
							pdf = file;
							scope.numPages = pdf.pdfInfo.numPages;
							scope.$apply('numPages');
							scope.openPage();
						});
			});
		}
	}
});

module.directive('fileViewer', function(){
	return {
		restrict: 'E',
		scope: {
			ngModel: '='
		},
		templateUrl: '/' + appPrefix + '/public/template/entcore/file-viewer.html',
		link: function(scope, element, attributes){
			scope.contentType = scope.ngModel.metadata.contentType;
			scope.isFullscreen = false;

			scope.download = function(){
				window.location.href = scope.ngModel.link;
			};
			var renderElement;
			var renderParent;
			scope.fullscreen = function(allow){
				scope.isFullscreen = allow;
				if(allow){
					var container = $('<div class="fullscreen-viewer"></div>');
					container.hide();
					container.on('click', function(e){
						if(!$(e.target).hasClass('render')){
							scope.fullscreen(false);
							scope.$apply('isFullscreen');
						}
					});
					element.children('.embedded-viewer').addClass('fullscreen');
					renderElement = element
						.find('.render');
					renderParent = renderElement.parent();

					renderElement
						.addClass('fullscreen')
						.appendTo(container);
					container.appendTo('body');
					container.fadeIn();
					if(typeof scope.render === 'function'){
						scope.render();
					}
				}
				else{
					renderElement.removeClass('fullscreen').appendTo(renderParent);
					element.children('.embedded-viewer').removeClass('fullscreen');
					var fullscreenViewer = $('body').find('.fullscreen-viewer');
					fullscreenViewer.fadeOut(400, function(){
						fullscreenViewer.remove();
					});

					if(typeof scope.render === 'function'){
						scope.render();
					}
				}
			}
		}
	}
});

module.directive('ngTouchstart', function(){
	return {
		restrict: 'A',
		scope: true,
		link: function(scope, element, attributes){
			element.on('touchstart', function(){
				scope.$eval(attributes['ngTouchstart']);
			});

			if(attributes['ngTouchend']){
				$('body').on('touchend', function(){
					scope.$eval(attributes['ngTouchend']);
				});
			}
		}
	}
})

module.directive('inputPassword', function(){
	return {
		restrict: 'E',
		replace: false,
		template:
			'<input type="password"/>' +
			'<button type="button" \
				ng-mousedown="show(true)" \
				ng-touchstart="show(true)" \
				ng-touchend="show(false)" \
				ng-mouseup="show(false)" \
				ng-mouseleave="show(false)"></button>',
		scope: true,
		compile: function(element, attributes){
			element.addClass('toggleable-password');
			var passwordInput = element.children('input[type=password]');
			for(var prop in attributes.$attr){
				passwordInput.attr(attributes.$attr[prop], attributes[prop]);
				element.removeAttr(attributes.$attr[prop]);
			}
			return function(scope){
				scope.show = function(bool){
					passwordInput[0].type = bool ? "text" : "password"
				}
			};
		}
	}
});

module.directive('sidePanel', function(){
	return{
		restrict: 'E',
		transclude: true,
		template: '<div class="opener"></div>' +
		'<div class="toggle">' +
			'<div class="content" ng-transclude></div>' +
		'</div>',
		link: function(scope, element, attributes){
			element.addClass('hidden');
			element.children('.opener').on('click', function(e){
				if(!element.hasClass('hidden')){
					return;
				}
				element.removeClass('hidden');
				setTimeout(function(){
					$('body').on('click.switch-side-panel', function(e){
						if(!(element.children('.toggle').find(e.originalEvent.target).length)){
							element.addClass('hidden');
							$('body').off('click.switch-side-panel');
						}
					});
				}, 0);
			});
		}
	};
});

module.directive('plus', function($compile){
	return {
		restrict: 'E',
		transclude: true,
		template: '' +
		'<div class="opener">' +
			'<div class="plus"></div>' +
			'<div class="minus"></div>' +
		'</div>' +
		'<section class="toggle-buttons">' +
			'<div class="toggle" ng-transclude></div>' +
		'</div>',
		link: function(scope, element, attributes){
			element.children('.toggle-buttons').addClass('hide');
			element.children('.opener').addClass('plus');
			element.children('.opener').on('click', function(e) {
				if(!element.children('.toggle-buttons').hasClass('hide')){
					return;
				}
				element.children('.toggle-buttons').removeClass('hide');
				element.children('.opener').removeClass('plus').addClass('minus');
				setTimeout(function(){
					$('body').on('click.switch-plus-buttons', function(e){
						//if(!(element.children('.toggle-buttons').find(e.originalEvent.target).length)){
							element.children('.toggle-buttons').addClass('hide');
							element.children('.opener').removeClass('minus').addClass('plus');
							$('body').off('click.switch-plus-buttons');
						//}
					});
				}, 0);
			});
		}
	}
});

module.directive('help', function(){
	var helpText;
	return {
		restrict: 'E',
		scope: {},
		template: '<i class="help"></i>' +
		'<lightbox show="display.read" on-close="display.read = false"><div></div></lightbox>',
		link: function(scope, element){
			scope.display = {};
			scope.helpPath = '/help/application/' + appPrefix + '/';
			if(appPrefix === '.' && window.location.pathname !== '/adapter') {
				scope.helpPath = '/help/application/portal/';
			}
			else if(window.location.pathname === '/adapter'){
				scope.helpPath = '/help/application/' + window.location.search.split('eliot=')[1].split('&')[0] + '/'
			}

			var helpContent;

			var setHtml = function(content){
				helpContent = $('<div>' + content + '</div>');
				helpContent.find('img').each(function(index, item){
					$(item).attr('src', scope.helpPath + $(item).attr('src'));
				});
				helpContent.find('script').remove();
				element.find('div.content').html(helpContent.html());
				element.find('a').on('click', function(e){
					element.find('.app-content-section').slideUp();
					$('#' + $(e.target).attr('href').split('#')[1]).slideDown();
				});
				element.find('a').first().click();
				scope.display.read = true;
				scope.$apply('display');
			};

			element.children('i.help').on('click', function () {
			    if (helpText) {
			        setHtml(helpText);
			    }
			    else {
			        http().get(scope.helpPath)
                        .done(function (content) {
                            helpText = content;
                            setHtml(helpText);
                        })
                        .e404(function () {
                            helpText = '<h2>' + lang.translate('help.notfound.title') + '</h2><p>' + lang.translate('help.notfound.text') + '</p>';
                            setHtml(helpText);
                        });
			    }
			});
		}
	}
});

module.directive('stickToTop', function() {
    return {
        restrict: 'EA',
        link: function(scope, element, attributes) {
            var initialPosition;
            setTimeout(function() {
                initialPosition = element.offset().top;
            }, 200);

            var scrollTop = $(window).scrollTop()
            var actualScrollTop = $(window).scrollTop()

            var animation = function() {
				element.addClass('scrolling')
                   element.offset({
                       top: element.offset().top + (
                           actualScrollTop + $('.height-marker').height() - (
                               element.offset().top
                           )
                       ) / 20
                   });
                requestAnimationFrame(animation)
            }

            var scrolls = false;
				$(window).scroll(function() {
	                actualScrollTop = $(window).scrollTop()
					if(actualScrollTop <= initialPosition - $('.height-marker').height()){
						actualScrollTop = initialPosition - $('.height-marker').height();
					}
	                if (!scrolls) {
	                    animation();
	                }
	                scrolls = true;
	            })

        }
    }
});

module.directive('floatingNavigation', function(){
	return {
		restrict: 'E',
		replace: true,
		transclude: true,
		scope: {},
		template: '<nav class="vertical hash-magnet floating" stick-to-top>' +
		'<div class="previous arrow" ng-class="{ visible: step > 0 }"></div>' +
		'<div class="content" ng-transclude></div>' +
		'<div class="next arrow" ng-class="{ visible: step < stepsLength && stepsLength > 0 }"></div>' +
		'</nav>',
		link: function(scope, element, attributes){

			var initialPosition;
			scope.step = 0;
			setTimeout(function(){
				initialPosition = element.offset();
				element.height($(window).height() - parseInt(element.css('margin-bottom'))-100);
				scope.stepsLength = parseInt(element.find('.content')[0].scrollHeight / element.height());
			}, 800);
			element.find('.arrow.next').on('click', function(){
				scope.step ++;
				scope.$apply();
				element.find('.content').animate({
					scrollTop: element.height() * scope.step
				}, 450);
			});
			element.find('.arrow.previous').on('click', function(){
				scope.step --;
				scope.$apply();
				element.find('.content').animate({
					scrollTop: element.height() * scope.step
				}, 450);
			});
		}
	}
});

module.directive('multiCombo', function() {
    return {
        restrict: 'E',
        replace: false,
        scope: {
            title: '@',
            comboModel: '=',
            filteredModel: '=',
            searchOn: '@',
            orderBy: '@',
            filterModel: '&',
            searchPlaceholder: '@',
            maxSelected: '@',
            labels: '&',
            disable: '&',
            selectionEvent: '&',
            deselectionEvent: '&'
        },
		templateUrl: '/' + appPrefix + '/public/template/entcore/multi-combo.html',
        controller: function($scope, $filter, $timeout) {
            /* Search input */
            $scope.search = {
                input: '',
				reset: function(){ this.input = "" }
            }

            /* Combo box visibility */
            $scope.show = false
            $scope.toggleVisibility = function() {
                $scope.show = !$scope.show
                if ($scope.show) {
                    $scope.addClickEvent()
                    $scope.search.reset()
                    $timeout(function() {
                        $scope.setComboPosition()
                    }, 1)
                }
            }

            /* Item list selection & filtering */
            if (!$scope.filteredModel || !($scope.filteredModel instanceof Array))
                $scope.filteredModel = []

            $scope.isSelected = function(item) {
                return $scope.filteredModel.indexOf(item) >= 0
            }

            $scope.toggleItem = function(item) {
                var idx = $scope.filteredModel.indexOf(item)
                if (idx >= 0) {
                    $scope.filteredModel.splice(idx, 1);
                    if ($scope.deselectionEvent() instanceof Function)
                        $scope.deselectionEvent()()
                } else if (!$scope.maxSelected || $scope.filteredModel.length < $scope.maxSelected) {
                    $scope.filteredModel.push(item);
                    if ($scope.selectionEvent() instanceof Function)
                        $scope.selectionEvent()()
                }
            }

            $scope.selectAll = function() {
                $scope.filteredModel.length = 0
                for (var i = 0; i < $scope.filteredComboModel.length; i++) {
                    $scope.filteredModel.push($scope.filteredComboModel[i])
                }
                if ($scope.selectionEvent() instanceof Function)
                    $scope.selectionEvent()()
            }

            $scope.deselectAll = function() {
                $scope.filteredModel.length = 0
                if ($scope.deselectionEvent() instanceof Function)
                    $scope.deselectionEvent()()
            }

            $scope.fairInclusion = function(anyString, challenger) {
                return lang.removeAccents(anyString.toLowerCase()).indexOf(lang.removeAccents(challenger.toLowerCase())) >= 0
            }

            $scope.filteringFun = function(item) {
                var precondition = $scope.filterModel() ? $scope.filterModel()(item) : true
                if ($scope.searchOn && item instanceof Object)
                    return precondition && $scope.fairInclusion(item[$scope.searchOn], $scope.search.input)
                return precondition && $scope.fairInclusion(item, $scope.search.input)
            }

            /* Item display */
            $scope.display = function(item) {
                return item instanceof Object ? item.toString() : item
            }

            /* Ensure that filtered elements are not obsolete */
            $scope.$watchCollection('comboModel', function() {
                if (!$scope.comboModel) {
                    $scope.filteredModel = []
                    return
                }

                for (var i = 0; i < $scope.filteredModel.length; i++) {
                    var idx = $scope.comboModel.indexOf($scope.filteredModel[i])
                    if (idx < 0) {
                        $scope.filteredModel.splice(idx, 1)
                        i--
                    }
                }
            })
        },
        link: function(scope, element, attributes) {
            if (!attributes.comboModel || !attributes.filteredModel) {
                throw '[<multi-combo> directive] Error: combo-model & filtered-model attributes are required.'
            }

            /* Max n° of elements selected limit */
            scope.maxSelected = parseInt(scope.maxSelected)
            if (!isNaN(scope.maxSelected) && scope.maxSelected < 1) {
                throw '[<multi-combo> directive] Error: max-selected must be an integer greater than 0.'
            }

            /* Visibility mouse click event */
            scope.addClickEvent = function() {
                if (!scope.show)
                    return

                var timeId = new Date().getTime()
                $('body').on('click.multi-combo' + timeId, function(e) {
                    if (!(element.find(e.originalEvent.target).length)) {
                        scope.show = false
                        $('body').off('click.multi-combo' + timeId)
                        scope.$apply()
                    }
                })
            }

            /* Drop down position */
            scope.setComboPosition = function() {
                element.css('position', 'relative')
                element.find('.multi-combo-root-panel').css('top',
                    element.find('.multi-combo-root-button').outerHeight()
                )
            }
            scope.setComboPosition()
        }
    }
});

module.directive('slide', function () {
	return {
		restrict: 'A',
		scope: false,
		link: function (scope, element, attributes) {
			scope.$watch(
				function(){
					return scope.$eval(attributes.slide);
				},
				function(newVal) {
					if (newVal) {
						element.slideDown();
					} else {
						element.slideUp();
					}
				}
			)

			if(!scope.$eval(attributes.slide)){
				element.hide();
			}
		}
	}
});

module.directive('sideNav', function(){
	return {
		restrict: 'AE',
		link: function(scope, element, attributes){
			var body = $('body');;
			$('.mobile-nav-opener').addClass('visible');
			var maxWidth = ui.breakpoints.tablette;
			var target = attributes.targetElement || '.navbar';

			element.addClass('side-nav');
			$('body').addClass('transition');

			var opener = $('.mobile-nav-opener');
			opener.on('click', function(){
				if(!element.hasClass('slide')){
					element.addClass('slide');
					$('body').addClass('point-out');
				}
				else{
					element.removeClass('slide');
					$('body').removeClass('point-out');
				}

			});

			$('body').on('click', function(e){
				if(element[0] === e.target || element.find(e.target).length || $('.mobile-nav-opener')[0] === e.target){
					return;
				}
				element.removeClass('slide');
				$('body').removeClass('point-out');

			})

			if(attributes.maxWidth){
				maxWidth = parseInt(attributes.maxWidth);
			}
			function addRemoveEvents(){
				if(ui.breakpoints.checkMaxWidth(maxWidth)){
					element.height($(window).height());

					if($('.mobile-nav-opener').hasClass('visible')){
						body.find('.application-title').addClass('move-right');
					}

					ui.extendElement.touchEvents(body, {
                        exclude: ['longclick'],
                        allowDefault: true
                    });

					ui.extendElement.touchEvents(element, {
                        exclude: ['longclick']
                    });

					body.on('swipe-right', function(){
						element.addClass('slide');
					});
					element.on('swipe-left', function(){
						element.removeClass('slide');
						$('body').removeClass('point-out');
					});
				}else {
					element.height('auto');
				}
			}
			addRemoveEvents();
			$(window).on('resize', addRemoveEvents);

			scope.$on("$destroy", function() {
				$('.mobile-nav-opener').removeClass('visible');
				body.find('.application-title').removeClass('move-right');
			});

		}
	}
});

module.directive('appTitle', function($compile){
	return {
		restrict: 'E',
		link: function(scope, element, attributes){
			element.addClass('zero-mobile');
			element.find('h1').addClass('application-title');

			function setHeader(){
				var header = $('app-title').html();
				var mobileheader = $('header.main .application-title');

				if(ui.breakpoints.checkMaxWidth("tablette")){
					if(!mobileheader.length)
						$('header.main').append($compile(header)(scope));
				} else {
					mobileheader.remove();
				}
			}
			setHeader();
			$(window).on('resize', setHeader);

			scope.$on("$destroy", function() {
				$('body').find('header.main .application-title').remove();
			});
		}
	}
});

module.directive('microbox', function($compile){
	return {
		restrict: 'E',
		compile: function(element, attributes, transclude){
			var content = element.html();

			return function(scope, element, attributes){
				var microtitle = lang.translate(attributes.microtitle);
				var closeBox = lang.translate(attributes.close);
				element.addClass('zero-mobile');

				function setBox(apply?){
					if(ui.breakpoints.checkMaxWidth("tablette")){

						if (!$('.microbox-wrapper').length){
							//creer la box
							$('body').append('<div class="microbox-wrapper zero">'+
								'<div class="microbox-content">'+
								'<i class="close-2x"></i>'+
								'<div class="microbox-material"></div>'+
								'<button class="microbox-close">'+ closeBox +'</button>'+
								'</div></div>');

							$('.microbox-material').html($compile(content)(scope));
							element.after('<button class="microbox">'+ microtitle +'</button>');

							$('button.microbox').on('click', function(){
								if($('.microbox-wrapper').hasClass('zero')){
									$('.microbox-wrapper').removeClass('zero');
								}
							});

							$('button.microbox-close, .microbox-content i.close-2x').on('click', function(){
								if(!$('.microbox-wrapper').hasClass('zero')){
									$('.microbox-wrapper').addClass('zero');
								}
							});

							if(apply){
								scope.$apply();
							}
						}
					}else{
						$('.microbox-wrapper').remove();
						$('button.microbox').remove();
					}
				}

				setBox();
				$(window).on('resize', function(){ setBox(true) });

				scope.$on("$destroy", function() {
					$('body').find('button.microbox').remove();
					$('body').find('.microbox-content').remove();
				});
			}
		}
	}
});

module.directive('subtitle', function () {
	return {
		restrict: 'A',
		scope: false,
		link: function (scope, element, attributes) {
			$('section.main').addClass('subtitle-push');

			scope.$on("$destroy", function() {
				$('section.main').removeClass('subtitle-push');
			});
		}
	}
});

module.directive('whereami', function () {
	return {
		restrict: 'A',
		scope: false,
		link: function (scope, element, attributes) {
			element.addClass('whereami');
			var current = $('nav.side-nav a.selected').text();
			$('body').on('whereami.update', function(){
				element.text($('nav.side-nav a.selected').text());
			})
			element.text(current);
		}
	}
});




var checkToolDelay = (function(){
    var applyAllowed = true;

	return function checkApplication(scope){
    	if(applyAllowed){
			applyAllowed = false;

			setTimeout(function(){
				scope.$apply();
				applyAllowed = true;
			}, 200);
		}

	}
}());

module.directive('checkTool', function () {
	return {
		restrict: 'E',
		scope: {
				ngModel: '=',
				ngClick: '&',
				ngChange: '&',
		},
		template: '<div class="check-tool"><i class="check-status"></i></div>',
		link: function (scope, element, attributes) {
			element.on('click', function(){
				scope.ngModel = !scope.ngModel;
				checkToolDelay(scope)

				if (scope.ngModel) {
					element.addClass('selected')
				}else {
					element.removeClass('selected')
				}
				if (scope.ngClick) {
					scope.ngClick();
				}
				if (scope.ngChange) {
					scope.ngChange();
				}
				checkToolDelay(scope)
			});

			$('body').on('click', function(e){
				if($(e.target).parents('.check-tool, .toggle, .lightbox').length ===0 && e.target.nodeName!=="CHECK-TOOL" && $('body').find(e.target).length !== 0){
					scope.ngModel = false;
					element.removeClass('selected');
					checkToolDelay(scope)
					if(scope.ngChange){
                        scope.ngChange();
						checkToolDelay(scope)
                    }
				}
			})
		}
	}
});

module.directive('subtitle', function () {
	return {
		restrict: 'A',
		scope: false,
		link: function (scope, element, attributes) {
			$('section.main').addClass('subtitle-push');

			scope.$on("$destroy", function() {
				$('section.main').removeClass('subtitle-push');
			});
		}
	}
});

module.directive('whereami', function () {
	//only on mailboxes
	return {
		restrict: 'A',
		scope: false,
		link: function (scope, element, attributes) {
			var current = $('nav.side-nav a.selected').text();
			$('body').on('whereami.update', function(){
				element.text($('nav.side-nav a.selected').text());
			})
			element.text(current);
		}
	}
});


module.controller('Account', ['$scope', function($scope) {
	$scope.nbNewMessages = 0;
	$scope.me = model.me;
	$scope.rand = Math.random();
	$scope.skin = skin;
	$scope.lang = lang;
	$scope.refreshAvatar = function(){
		http().get('/userbook/api/person', {}, {requestName: "refreshAvatar"}).done(function(result){
			$scope.avatar = result.result['0'].photo;
			if (!$scope.avatar || $scope.avatar === 'no-avatar.jpg' || $scope.avatar === 'no-avatar.svg') {
                $scope.avatar = skin.basePath + '/img/illustrations/no-avatar.svg';
            }
			$scope.username = result.result['0'].displayName;
			model.me.profiles = result.result['0'].type;
			$scope.$apply();
		});
	};

	$scope.refreshMails = function(){
		http().get('/conversation/count/INBOX', { unread: true }).done(function(nbMessages){
			$scope.nbNewMessages = nbMessages.count;
			$scope.$apply('nbNewMessages');
		});
	};

	$scope.openApps = function(event){
		if($(window).width() <= 700){
			event.preventDefault();
		}
	}

	http().get('/directory/userbook/' + model.me.userId).done(function(data){
		model.me.userbook = data;
		$scope.$apply('me');
	});

	skin.listThemes(function(themes){
		$scope.themes = themes;
		$scope.$apply('themes');
	});

	$scope.$root.$on('refreshMails', $scope.refreshMails);

	$scope.refreshMails();
	$scope.refreshAvatar();
	$scope.currentURL = window.location.href;
}]);


module.controller('Share', ['$rootScope','$scope', function($rootScope, $scope) {
	if(!$scope.appPrefix){
		$scope.appPrefix = appPrefix;
	}
	if($scope.resources instanceof Model){
		$scope.resources = [$scope.resources];
	}

	if(!($scope.resources instanceof Array)){
		throw new TypeError('Resources in share panel must be instance of Model or Array');
	}

	$scope.sharing = {};
	$scope.found = [];
	$scope.maxResults = 5;

	$scope.editResources = [];
	$scope.sharingModel = {
		edited: []
	};

	$scope.addResults = function(){
		$scope.maxResults += 5;
	};

	var actionsConfiguration = {};

	http().get('/' + infraPrefix + '/public/json/sharing-rights.json').done(function(config){
		actionsConfiguration = config;
	});

	$scope.translate = lang.translate;

	function actionToRights(item, action){
		var actions = [];
		_.where($scope.actions, { displayName: action.displayName }).forEach(function(item){
			item.name.forEach(function(i){
				actions.push(i);
			});
		});

		return actions;
	}

	function rightsToActions(rights, http?){
		var actions = {};

		rights.forEach(function(right){
			var action = _.find($scope.actions, function(action){
				return action.name.indexOf(right) !== -1
			});

			if(!action){
				return;
			}

			if(!actions[action.displayName]){
				actions[action.displayName] = true;
			}
		});

		return actions;
	}

	function setActions(actions){
		$scope.actions = actions;
		$scope.actions.forEach(function(action){
			var actionId = action.displayName.split('.')[1];
			if(actionsConfiguration[actionId]){
				action.priority = actionsConfiguration[actionId].priority;
				action.requires = actionsConfiguration[actionId].requires;
			}
		});
	}

	function dropRights(callback){
		function drop(resource, type){
			var done = 0;
			for(var element in resource[type].checked){
				var path = '/' + $scope.appPrefix + '/share/remove/' + resource._id;
				var data:any = {};
				if(type === 'users'){
					data.userId = element;
				}
				else{
					data.groupId = element;
				}
				http().put(path, http().serialize(data));
			}
		}
		$scope.editResources.forEach(function(resource){
			drop(resource, 'users');
			drop(resource, 'groups');
		});
		callback();
		$scope.varyingRights = false;
	}

	function differentRights(model1, model2){
		var result = false;
		function different(type){
			for(var element in model1[type].checked){
				if(!model2[type].checked[element]){
					return true;
				}

				model1[type].checked[element].forEach(function(right){
					result = result || model2[type].checked[element].indexOf(right) === -1
				});
			}

			return result;
		}

		return different('users') || different('groups');
	}

	var feedData = function(){
		var initModel = true;
		$scope.resources.forEach(function(resource){
			var id = resource._id;
			http().get('/' + $scope.appPrefix + '/share/json/' + id).done(function(data){
				if(initModel){
					$scope.sharingModel = data;
					$scope.sharingModel.edited = [];
				}

				data._id = resource._id;
				$scope.editResources.push(data);
				var editResource = $scope.editResources[$scope.editResources.length -1];
				if(!$scope.sharing.actions){
					setActions(data.actions);
				}

				function addToEdit(type){
					for(var element in editResource[type].checked){
						var rights = editResource[type].checked[element];

						var groupActions = rightsToActions(rights);
						var elementObj = _.findWhere(editResource[type].visibles, {
							id: element
						});
                        if(elementObj){
                            elementObj.actions = groupActions;
                            if(initModel){
                                $scope.sharingModel.edited.push(elementObj);
                            }

                            elementObj.index = $scope.sharingModel.edited.length;
                        }
					}
				}

				addToEdit('groups');
				addToEdit('users');

				if(!initModel){
					if(differentRights(editResource, $scope.sharingModel) || differentRights($scope.sharingModel, editResource)){
						$scope.varyingRights = true;
						$scope.sharingModel.edited = [];
					}
				}
				initModel = false;

				$scope.$apply('sharingModel.edited');
			});
		})
	};

	$scope.$watch('resources', function(){
		$scope.actions = [];
		$scope.sharingModel.edited = [];
		$scope.search = '';
		$scope.found = [];
		$scope.varyingRights = false;
		feedData();
	});

	$scope.$watchCollection('resources', function(){
		$scope.actions = [];
		$scope.sharingModel.edited = [];
		$scope.search = '';
		$scope.found = [];
		$scope.varyingRights = false;
		feedData();
	});

	$scope.addEdit = function(item){
		item.actions = {};
		$scope.sharingModel.edited.push(item);
		item.index = $scope.sharingModel.edited.length;
		var addedIndex = $scope.found.indexOf(item);
		$scope.found.splice(addedIndex, 1);

        var defaultActions = []
		$scope.actions.forEach(function(action){
			var actionId = action.displayName.split('.')[1];
			if(actionsConfiguration[actionId].default){
				item.actions[action.displayName] = true;
                defaultActions.push(action);
			}
		});

        var index = -1;
        var loopAction = function(){
            if(++index < defaultActions.length){
                $scope.saveRights(item, defaultActions[index], loopAction);
            }
        }
        loopAction()

	};

	$scope.findUserOrGroup = function(){
		var searchTerm = lang.removeAccents($scope.search).toLowerCase();
		$scope.found = _.union(
			_.filter($scope.sharingModel.groups.visibles, function(group){
				var testName = lang.removeAccents(group.name).toLowerCase();
				return testName.indexOf(searchTerm) !== -1;
			}),
			_.filter($scope.sharingModel.users.visibles, function(user){
				var testName = lang.removeAccents(user.lastName + ' ' + user.firstName).toLowerCase();
				var testNameReversed = lang.removeAccents(user.firstName + ' ' + user.lastName).toLowerCase();
				return testName.indexOf(searchTerm) !== -1 || testNameReversed.indexOf(searchTerm) !== -1;
			})
		);
		$scope.found = _.filter($scope.found, function(element){
			return $scope.sharingModel.edited.indexOf(element) === -1;
		})
	};

	$scope.remove = function(element){
		var data;
		if(element.login !== undefined){
			data = {
				userId: element.id
			}
		}
		else{
			data = {
				groupId: element.id
			}
		}

		$scope.sharingModel.edited = _.reject($scope.sharingModel.edited, function(item){
			return item.id === element.id;
		});

		$scope.resources.forEach(function(resource){
			var path = '/' + $scope.appPrefix + '/share/remove/' + resource._id;
			http().put(path, http().serialize(data)).done(function(){
				$rootScope.$broadcast('share-updated', data);
			});
		})
	}

	$scope.maxEdit = 3;

	$scope.displayMore = function(){
		var displayMoreInc = 5;
		$scope.maxEdit += displayMoreInc;
	}

	function applyRights(element, action, cb){
		var data;
		if(element.login !== undefined){
			data = { userId: element.id, actions: [] }
		}
		else{
            data = { groupId: element.id, actions: [] }
		}
		data.actions = actionToRights(element, action);

		var setPath = 'json';
		if(!element.actions[action.displayName]){
			setPath = 'remove';
			_.filter($scope.actions, function(item){
				return _.find(item.requires, function(dependency){
					return action.displayName.split('.')[1].indexOf(dependency) !== -1;
				}) !== undefined
			})
			.forEach(function(item){
				if(item){
					element.actions[item.displayName] = false;
					data.actions = data.actions.concat(actionToRights(element, item));
				}
			})
		}
		else{
			action.requires.forEach(function(required){
				var action = _.find($scope.actions, function(action){
					return action.displayName.split('.')[1].indexOf(required) !== -1;
				});
				if(action){
					element.actions[action.displayName] = true;
					data.actions = data.actions.concat(actionToRights(element, action));
				}
			});
		}

        var times = $scope.resources.length
        var countdownAction = function(){
            if(--times <= 0 && typeof cb === 'function'){
                cb()
            }
        }

		$scope.resources.forEach(function(resource){
			http().put('/' + $scope.appPrefix + '/share/' + setPath + '/' + resource._id, http().serialize(data)).done(function(){
				if(setPath === 'remove'){
					$rootScope.$broadcast('share-updated', { removed: { groupId: data.groupId, userId: data.userId, actions: rightsToActions(data.actions) } });
				}
				else{
					$rootScope.$broadcast('share-updated', { added: { groupId: data.groupId, userId: data.userId, actions: rightsToActions(data.actions) } });
				}
                countdownAction()
			});
		});
	}

	$scope.saveRights = function(element, action, cb){
		if($scope.varyingRights){
			dropRights(function(){
				applyRights(element, action, cb)
			});
		}
		else{
			applyRights(element, action, cb)
		}
	};
}]);

module.controller('Admin', ['$scope', function($scope){
	$scope.urls = [];
	http().get('/admin-urls').done(function(urls){
		$scope.urls = urls;
		$scope.$apply('urls');
	});
    $scope.getHighlight = function(url){
        return window.location.href.indexOf(url.url) >= 0
    }
    $scope.orderUrls = function(url){
        return !$scope.getHighlight(url) ? 1 : 0
    }
    $scope.filterUrls = function(url){
        return !url.allowed || !(url.allowed instanceof Array) ? true : _.find(model.me.functions, function(f){ return _.contains(url.allowed, f.code) })
    }

	$scope.scrollUp = ui.scrollToTop;
}]);

$(document).ready(function(){
	setTimeout(function(){
		//routing
		if(routes.routing){
			module.config(routes.routing);
		}
		bootstrap(function(){
		    RTE.addDirectives(module);
            if (window.entcore.ng.init) {
                window.entcore.ng.init(module);
		    }
			model.build();

			lang.addDirectives(module);


			function start(){
				lang.addBundle('/i18n', function(){
					lang.addBundle('/' + appPrefix + '/i18n', function(){
                        angular.bootstrap($('html'), ['app']);
                        model.trigger('bootstrap');
						model.bootstrapped = true;
						model.sync();
					});
				});
			}

            http().get(skin.basePath + 'js/directives.js').done((d) => {
                eval(d);

                if(typeof skin.addDirectives === 'function'){
                    skin.addDirectives(module, start);
                }
                else{
                    start();
                }
            })
            .error(() => {
                start();
            });
		});
	}, 10);
});

module.controller('SearchPortal', ['$scope', function($scope) {
	$scope.launchSearch = function(event) {
		var words = $scope.mysearch;
		if (event != "link") event.stopPropagation();
		if ((event == "link" ||  event.keyCode == 13)) {
			words = (!words || words === '') ? ' ' : words;
			$scope.mysearch = "";
			window.location.href = '/searchengine#/' + words;
		}
	};
}]);

function Widget(){}

Widget.prototype.switchHide = function(){
    if(this.mandatory)
        return
	if(!this.hide){
		this.hide = false;
	}
	this.hide = !this.hide;
	this.trigger('change');
	model.widgets.trigger('change');
	model.widgets.savePreferences();
}

function WidgetModel(){
	model.makeModels([Widget]);
	model.collection(Widget, {
		preferences: {},
		savePreferences: function(){
			var that = this;
			this.forEach(function(widget){
				that.preferences[widget.name].hide = widget.hide;
				that.preferences[widget.name].index = widget.index;
			});
			http().putJson('/userbook/preference/widgets', this.preferences);
		},
		sync: function(){
			var that = this;
            var data = model.me.widgets

			http().get('/userbook/preference/widgets').done(function(pref){
				if(!pref.preference){
					this.preferences = {};
				}
				else{
					this.preferences = JSON.parse(pref.preference);
				}

				data = data.map(function(widget, i){
					if(!that.preferences[widget.name]){
						that.preferences[widget.name] = { index: i, show: true };
					}
					widget.index = that.preferences[widget.name].index;
					widget.hide = widget.mandatory ? false : that.preferences[widget.name].hide;
					return widget;
				});

                for(var i = 0; i < data.length; i++){
                    var widget = data[i];
                    (function(widget){
                        if (widget.i18n) {
                            lang.addTranslations(widget.i18n, function(){
                                that.push(widget)
                                http().loadScript(widget.js)
                            })
                        } else {
                            that.push(widget)
                            http().loadScript(widget.js)
                        }
                    })(widget)
                }
			}.bind(this))
		},
		findWidget: function(name){
			return this.findWhere({name: name});
		},
		apply: function(){
			model.trigger('widgets.change');
		}
	});
}

module.controller('Widgets', ['$scope', 'model', 'lang', 'date', function($scope, model, lang, date){
	if(!model.widgets){
		WidgetModel();
		model.widgets.sync();
	}

	$scope.widgets = model.widgets;

	$scope.allowedWidget = function(widget){
		return (!$scope.list || $scope.list.indexOf(widget.name) !== -1) && !widget.hide;
	}

	model.on('widgets.change', function(){
		if(!$scope.$$phase){
			$scope.$apply('widgets');
		}
	});

	$scope.translate = lang.translate;
	$scope.switchHide = function(widget, $event){
		widget.switchHide();
		$event.stopPropagation();
	}
}]);

module.controller('MediaLibrary', ['$scope', function($scope){
	if(!model.me){
		return;
	}
	
	if(!model.mediaLibrary){
		model.makeModels(workspace);
		model.mediaLibrary = new Model();
		model.mediaLibrary.myDocuments = new workspace.MyDocuments();
		model.mediaLibrary.sharedDocuments = new workspace.SharedDocuments();
		model.mediaLibrary.appDocuments = new workspace.AppDocuments();
		model.mediaLibrary.publicDocuments = new workspace.PublicDocuments();
	}

	$scope.myDocuments = model.mediaLibrary.myDocuments;

	$scope.display = {
		show: 'browse',
		search: '',
        limit: 12
	};

	$scope.show = function(tab){
		$scope.display.show = tab;
		$scope.upload.loading = [];
	};

    $scope.showPath = () => {
        return '/' + appPrefix + '/public/template/entcore/media-library-' + $scope.display.show + '.html'
    }

	$scope.listFrom = function(listName){
	    $scope.display.listFrom = listName;
	    model.mediaLibrary[$scope.display.listFrom].sync();
	};

	$scope.openFolder = function(folder){
		if($scope.openedFolder.closeFolder && folder.folder.indexOf($scope.openedFolder.folder + '_') === -1){
			$scope.openedFolder.closeFolder();
		}

		$scope.openedFolder = folder;
		folder.sync();
		folder.on('sync', function(){
			$scope.documents = filteredDocuments(folder);
			$scope.folders = folder.folders.all;
			$scope.$apply('documents');
			$scope.$apply('folders');
		});
	};

	$scope.$watch('visibility', function(newVal){
		if(model.me && model.me.workflow.workspace.create){
			if($scope.visibility === 'public'){
				$scope.display.listFrom = 'publicDocuments';
			}
			else{
				$scope.display.listFrom = 'appDocuments';
			}
		}
		else if(model.me && model.me.workflow.workspace.list){
			$scope.display.listFrom = 'sharedDocuments';
		}

		model.mediaLibrary.on('myDocuments.sync, sharedDocuments.sync, appDocuments.sync, publicDocuments.sync', function(){
			$scope.documents = filteredDocuments(model.mediaLibrary[$scope.display.listFrom]);
			if(model.mediaLibrary[$scope.display.listFrom].folders){
				$scope.folders = model.mediaLibrary[$scope.display.listFrom].folders.filter(function(folder){
					return lang.removeAccents(folder.name.toLowerCase()).indexOf(lang.removeAccents($scope.display.search.toLowerCase())) !== -1;
				});
				$scope.$apply('folders');
			} else {
				delete($scope.folders);
			}

			$scope.folder = model.mediaLibrary[$scope.display.listFrom];
			$scope.openedFolder = $scope.folder;
			$scope.$apply('documents');
		});

		$scope.$watch('fileFormat', function(newVal){
			if(!newVal){
				return;
			}

			if(newVal === 'audio'){
				$scope.display.show = 'record';
			}
			else{
				$scope.display.show = 'browse';
			}

			if (model.mediaLibrary[$scope.display.listFrom].documents.length() === 0) {
			    model.mediaLibrary[$scope.display.listFrom].sync();
			}
			else {
			    model.mediaLibrary[$scope.display.listFrom].trigger('sync');
			}
		});
	});

	function filteredDocuments(source){
		return source.documents.filter(function(doc){
			return (doc.role() === $scope.fileFormat || $scope.fileFormat === 'any') &&
				lang.removeAccents(doc.metadata.filename.toLowerCase()).indexOf(lang.removeAccents($scope.display.search.toLowerCase())) !== -1;
		});
	}

	$scope.insertRecord = function(){
		model.mediaLibrary.appDocuments.documents.sync();
		$scope.display.show = 'browse';
		$scope.listFrom('appDocuments');
	};

	$scope.selectDocument = function(document){
		if(($scope.folder === model.mediaLibrary.appDocuments && $scope.visibility === 'protected') ||
			($scope.folder === model.mediaLibrary.publicDocuments && $scope.visibility === 'public')){
			if($scope.multiple){
				$scope.$parent.ngModel = [document];
			}
			else{
				$scope.$parent.ngModel = document;
			}
		}
		else{
			var copyFn = document.protectedDuplicate;
			if($scope.visibility === 'public'){
				copyFn = document.publicDuplicate;
			}
			$scope.display.loading = [document];
				copyFn.call(document, function(newFile){
				$scope.display.loading = [];
				if($scope.multiple){
					$scope.$parent.ngModel = [newFile];
					$scope.$parent.$apply('ngModel');
				}
				else{
					$scope.$parent.ngModel = newFile;
					$scope.$parent.$apply('ngModel');
				}
			});
		}
	};

	$scope.selectDocuments = function(){
		var selectedDocuments = _.where($scope.documents, { selected: true });
		if(($scope.folder === model.mediaLibrary.appDocuments && $scope.visibility === 'protected') ||
			($scope.folder === model.mediaLibrary.publicDocuments && $scope.visibility === 'public')){
			$scope.$parent.ngModel = selectedDocuments;
		}
		else{
			var duplicateDocuments = [];
			var documentsCount = 0;
			$scope.display.loading = selectedDocuments;
			selectedDocuments.forEach(function(doc){
				var copyFn = doc.protectedDuplicate.bind(doc);
				if($scope.visibility === 'public'){
					copyFn = doc.publicDuplicate.bind(doc);
				}

				copyFn(function(newFile){
					$scope.display.loading = [];
					duplicateDocuments.push(newFile);
					documentsCount++;
					if(documentsCount === selectedDocuments.length){
						$scope.$parent.ngModel = duplicateDocuments;
						$scope.$parent.$apply('ngModel');
					}
				});
			})
		}
	};

	$scope.setFilesName = function(){
		$scope.upload.names = '';
		for(var i = 0; i < $scope.upload.files.length; i++){
			if(i > 0){
				$scope.upload.names += ', '
			}
			$scope.upload.names += $scope.upload.files[i].name;
		}
	};

	$scope.importFiles = function(){
		var waitNumber = $scope.upload.files.length;
		for(var i = 0; i < $scope.upload.files.length; i++){
			$scope.upload.loading.push($scope.upload.files[i]);
			workspace.Document.prototype.upload($scope.upload.files[i], 'file-upload-' + $scope.upload.files[i].name + '-' + i, function(){
				waitNumber--;
				model.mediaLibrary.appDocuments.documents.sync();
				if(!waitNumber){
					$scope.display.show = 'browse';
					if($scope.visibility === 'public'){
						$scope.listFrom('publicDocuments');
					}
					else{
						$scope.listFrom('appDocuments');
					}
				}
				$scope.$apply('display');
			}, $scope.visibility);
		}
		$scope.upload.files = undefined;
		$scope.upload.names = '';
	};

	$scope.updateSearch = function(){
		$scope.documents = filteredDocuments($scope.openedFolder);
	}
}]);
