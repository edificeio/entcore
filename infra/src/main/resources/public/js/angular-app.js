var protoApp = {
	scope : '#main',
	init : function() {
		var that = this;
		that.i18n.load();
		$('body').delegate(that.scope, 'click',function(event) {
			if (!event.target.getAttribute('call')) return;
			event.preventDefault();
			if(event.target.getAttribute('disabled') !== null){
				return;
			}
			var call = event.target.getAttribute('call');
			that.action[call]({url : event.target.getAttribute('href'), target: event.target});
			event.stopPropagation();
		});
		// TODO : refactor web message dispatch policiy
		window.addEventListener('message', that.message.css, false);
	},
	action : {
	},
	template : {
		getAndRender : function (pathUrl, templateName, elem, dataExtractor){
			var that = this;
			if (_.isUndefined(dataExtractor)) {
				dataExtractor = function (d) { return {list : _.values(d.result)}; };
			}
			$.get(pathUrl)
				.done(function(data) {
					$(elem).html(that.render(templateName, dataExtractor(data)));
				})
				.error(function(data) {
					protoApp.notify.error(data);
				});
		},
		render : function (name, data) {
			_.extend(data, {
				'i18n' : protoApp.i18n.i18n,
				'formatDate' : function() {
					return function(str) {
						var dt = new Date(Mustache.render(str, this).replace('CEST', 'EST')).toShortString();
						return dt;
					};
				},
				'formatDateTime' : function() {
					return function(str) {
						var dt = new Date(Mustache.render(str, this).replace('CEST', 'EST')).toShortString();
						return dt;
					};
				},
				//today, yesterday...
				calendar: function(){
					return function(date) {
						return moment(Mustache.render(date, this).replace('CEST', 'EST')).calendar();
					};
				},
				//0 month 0000
				longDate: function(){
					return function(date) {
						var momentDate = moment(Mustache.render(date, this).replace('CEST', 'EST'));
						if(momentDate !== null){
							return momentDate.format('D MMMM YYYY');
						}
					};
				},
				//0 month
				longDay: function(){
					return function(date) {
						var momentDate = moment(Mustache.render(date, this).replace('CEST', 'EST'));
						if(momentDate !== null){
							return momentDate.format('D MMMM');
						}
					};
				}
			});
			return Mustache.render(this[name] === undefined ? name : this[name], data);
		}
	},
	notify : {
		done : function (msg) { this.instance('success')(msg);},
		error : function (msg) { this.instance('error')(msg); },
		warn : function (msg) {},
		info : function (msg) { this.instance('info')(msg); },
		instance : function(level) {
			return humane.spawn({ addnCls: 'humane-original-' + level });
		}
	},
	i18n : {
		load : function () {
			var that = this;
			$.ajax({url: 'i18n', async: false})
				.done(function(data){
					that.bundle = data;
				})
		},
		bundle : {},
		i18n : function() {
			return function(key) {
				key = Mustache.render(key, this);
				return protoApp.i18n.bundle[key] === undefined ? key : protoApp.i18n.bundle[key];
			};
		},
		translate: function(key){
			return this.i18n()(key);
		}
	},
	message : {
		// TODO : dispatch policiy and paraméter
		css : function(e) {
			if (event.origin == "http://localhost:8008") {
				$("head").append("<link rel='stylesheet' href='" + e.data + "' media='all' />");
			}
		}
	},
	define : function (o) {
		var props = { template : {}, action:{}};
		for (prop in props) {
			for (key in o[prop]) {
				props[[prop]][key] = {'value' : o[[prop]][key]};
			}
			Object.defineProperties(this[prop], props[[prop]]);
		}
	}
};

var module = angular.module('app', ['ngSanitize', 'ngRoute'], function($interpolateProvider) {
		$interpolateProvider.startSymbol('[[');
		$interpolateProvider.endSymbol(']]');
	})
	.factory('notify', function(){
		if(!window.humane){
			loader.syncLoad('humane');
		}

		return {
			message: function(type, message){
				message = lang.translate(message);
				if(parent !== window){
					messenger.notify(type, message);
				}
				else{
					humane.spawn({ addnCls: 'humane-original-' + type })(message);
				}
			},
			error: function(message){
				this.message('error', message);
			},
			info: function(message){
				this.message('info', message)
			}
		}
	})
	.factory('route', function($rootScope, $route, $routeParams){
		var routes = {};

		$rootScope.$on("$routeChangeSuccess", function($currentRoute, $previousRoute){
			if(typeof routes[$route.current.action] === 'function'){
				routes[$route.current.action]($routeParams);
			}
		});

		return function(setRoutes){
			routes = setRoutes;
		}
	})
	.factory('date', function() {
		if(window.moment === undefined){
			loader.syncLoad('moment');
		}

		if(currentLanguage === 'fr'){
			moment.lang(currentLanguage, {
				calendar : {
					lastDay : '[Hier à] HH[h]mm',
					sameDay : '[Aujourd\'hui à] HH[h]mm',
					nextDay : '[Demain à] HH[h]mm',
					lastWeek : 'dddd [dernier à] HH[h]mm',
					nextWeek : 'dddd [prochain à] HH[h]mm',
					sameElse : 'dddd LL'
				}
			});
		}
		else{
			moment.lang(currentLanguage);
		}

		return {
			format: function(date, format) {
				if(!moment){
					return '';
				}
				return moment(date).format(format);
			},
			calendar: function(date){
				if(!moment){
					return '';
				}
				return moment(date).calendar();
			}
		};
	})
	.factory('lang', function(){
		return lang
	})
	.factory('_', function(){
		if(window._ === undefined){
			loader.syncLoad('underscore');
		}
		return _;
	})
	.factory('model', function(){
		return Model;
	})
	.factory('ui', function(){
		return ui;
	});

//routing
if(routes.routing){
	module.config(routes.routing);
}

//directives

module.directive('completeChange', function() {
	return {
		restrict: 'A',
		scope:{
			exec: '&completeChange',
			field: '=ngModel'
		},
		link: function($scope, $linkElement, $attributes) {
			$scope.$watch('field', function(newVal) {
				$linkElement.val(newVal);
				if($linkElement[0].type === 'textarea' && $linkElement.hasClass('inline-editing')){
					setTimeout(function(){
						$linkElement.height(1);
						$linkElement.height($linkElement[0].scrollHeight - 1);
					}, 500);

				}
			});

			$linkElement.bind('change', function() {
				$scope.field = $linkElement.val();
				if(!$scope.$$phase){
					$scope.$apply('field');
				}
				$scope.$parent.$eval($scope.exec);

			});
		}
	};
});

module.directive('fileInputChange', function($compile){
	return {
		restrict: 'A',
		scope: {
			fileInputChange: '&',
			file: '=ngModel'
		},
		link: function($scope, $element){
			$element.bind('change', function(){
				$scope.file = $element[0].files[0];
				$scope.$apply();
				$scope.fileInputChange();
				$scope.$apply();
			})
		}
	}
});

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
		link: function($scope, $element, $attributes){
			$element.bind('change', function(){
				$scope.current.id = $element.find('.current').data('selected');
				$scope.$eval($scope.change);
				$element.unbind('change');
			})
		},
		template: '\
			<div>\
				<div class="current fixed cell twelve" data-selected="[[current.id]]">\
					<i role="[[current.icon]]"></i>\
					<span>[[current.text]]</span>\
				</div>\
				<div class="options-list icons-view">\
				<div class="cell three option" data-value="[[option.id]]" data-ng-repeat="option in options">\
					<i role="[[option.icon]]"></i>\
					<span>[[option.text]]</span>\
				</div>\
				</div>\
			</div>'
	};
});

module.directive('translate', function($compile) {
	return {
		restrict: 'A',
		link: function ($scope, $element, $attributes) {
			$element.html($compile('<span>' + lang.translate($attributes.key) + '</span>')($scope));
		}
	};
});

module.directive('translateAttr', function($compile) {
	return {
		restrict: 'A',
		link: function compile($scope, $element, $attributes) {
			var compiled = $compile('<span>' + lang.translate($attributes[$attributes.translateAttr]) + '</span>')($scope);
			setTimeout(function(){
				$element.attr($attributes.translateAttr, compiled.text());
			}, 10);
		}
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
})

module.directive('bindHtmlUnsafe', function($compile){
	return {
		restrict: 'A',
		scope: {
			bindHtmlUnsafe: '='
		},
		link: function($scope, $element){
			$scope.$watch('bindHtmlUnsafe', function(newVal){
				$element.html($compile('<div>' + newVal + '</div>')($scope.$parent));
				//weird browser bug with audio tags
				$element.find('audio').each(function(index, item){
					var parent = $(item).parent();
					$(item)
						.attr("src", item.src)
						.detach()
						.appendTo(parent);
				});
			});
		}
	}
});

module.directive('portal', function($compile){
	return {
		restrict: 'E',
		transclude: true,
		templateUrl: '/public/template/portal.html',
		compile: function($element, $attribute){
			var rand = Math.random();
			$.get('/theme?token=' + rand, function(data){
				var css = data.skin;
				$('[logout]').attr('href', '/auth/logout?callback=' + data.logoutCallback)
				ui.setStyle(css);
			})
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

module.directive('dropDown', function($compile, $timeout){
	return {
		restrict: 'E',
		transclude: true,
		replace: true,
		scope: {
			options: '=',
			change: '&',
			current: '='
		},
		template: '<div data-drop-down class="drop-down">\
						<div>\
							<ul class="ten cell right-magnet">\
								<li ng-repeat="option in options | limitTo:5" ng-model="option">[[option.toString()]]</li>\
							</ul>\
						</div>\
			</div>',
		link: function($scope, $element, $attributes){
			$scope.$watchCollection('options', function(newValue){
				if($scope.options.length === 0){
					$element.addClass('hidden');
					return;
				}
				$element.removeClass('hidden');
				var linkedInput = $('#' + $attributes.for);
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
				$element.offset(pos);
				$element.width(width);
			})
			$element.parent().on('remove', function(){
				$element.remove();
			})
			$element.detach().appendTo('body');


			$element.on('click', 'li', function(e){
				$scope.current = $(this).scope().option;
				$scope.$apply('current');
				$scope.$eval($scope.change);
				$scope.$apply('current');
			});
			$element.attr('data-opened-drop-down', true);

		}
	}
})

function createCKEditorInstance(editor, $scope, $compile){
	var positionning = function(){
		$('.cke_chrome').width(editor.width() + 2 + parseInt(editor.css('padding') || 4) * 2);
		$('.cke_chrome').offset({
			top: editor.offset().top - $('.cke_chrome').height(),
			left: editor.offset().left
		});
		$('<style></style>').text('.cke_chrome{' +
			'top:' + (editor.offset().top - $('.cke_chrome').height()) + 'px !important;' +
			'left:' + editor.offset().left + 'px !important;' +
			'position: absolute !important' +
			'}').appendTo('head');

	};

	CKEDITOR.on('instanceReady', function(ck){
		editor.focus();
		editor.html($compile($scope.ngModel)($scope.$parent));
		$scope.$parent.$apply();
		setTimeout(function(){
			$('input').first().focus();
		}, 500);

		if($scope.ngModel && $scope.ngModel.indexOf('<img') !== -1){
			$('img').on('load', positionning);
		}
		else{
			positionning();
		}
		editor.on('focus', function(){
			positionning();
		});
	});

	$scope.$watch('ngModel', function(newValue){
		if(editor.html() !== newValue){
			editor.html($compile(newValue)($scope.$parent));
			//weird browser bug with audio tags
			editor.find('audio').each(function(index, item){
				var parent = $(item).parent();
				$(item)
					.attr("src", item.src)
					.detach()
					.appendTo(parent);
			});
		}
	});

	editor.on('blur', function(e) {
		var content = editor.html();
		if(content.indexOf(';base64,') !== -1){
			$scope.notify.error('Une image est corrompue')
		}
		editor.find('img').each(function(index, item){
			if($(item).attr('src').indexOf(';base64,') !== -1){
				$(item).remove();
			}
		})
		$scope.ngModel = editor.html();
		$scope.$apply();
	});

	return positionning;
};

module.directive('richTextEditor', function($compile){
	return {
		restrict: 'E',
		scope: {
			ngModel: '=',
			watchCollection: '@',
			notify: '='
		},
		template: '<div class="twelve cell"><div contenteditable="true" class="editor-container twelve cell">' +
			'</div><div class="clear"></div></div>',
		compile: function($element, $attributes, $transclude){
			CKEDITOR_BASEPATH = '/infra/public/ckeditor/';
			if(window.CKEDITOR === undefined){
				loader.syncLoad('ckeditor');
				CKEDITOR.plugins.basePath = '/infra/public/ckeditor/plugins/';
			}
			return function($scope, $element, $attributes){
				var editor = $('[contenteditable=true]');
				CKEDITOR.inline(editor[0],
					{ customConfig: '/infra/public/ckeditor/rich-text-config.js' }
				);

				var positionning = createCKEditorInstance(editor, $scope, $compile);
				$scope.$eval($scope.watchCollection).forEach(function(col){
					$scope.$parent.$watchCollection(col, function(){
						positionning();
					});
				})

				$element.on('removed', function(){
					for(var instance in CKEDITOR.instances){
						CKEDITOR.instances[instance].destroy()
					}
					$('.cke').remove();
				})
			}
		}
	}
})

module.directive('htmlEditor', function($compile){
	return {
		restrict: 'E',
		transclude: true,
		replace: true,
		scope: {
			ngModel: '=',
			notify: '='
		},
		template: '<div class="twelve cell"><div contenteditable="true" class="editor-container twelve cell" loading-panel="ckeditor-image">' +
			'</div><div class="clear"></div></div>',
		compile: function($element, $attributes, $transclude){
			CKEDITOR_BASEPATH = '/infra/public/ckeditor/';
			if(window.CKEDITOR === undefined){
				loader.syncLoad('ckeditor');
				CKEDITOR.plugins.basePath = '/infra/public/ckeditor/plugins/';

			}
			return function($scope, $element, $attributes){
				CKEDITOR.fileUploadPath = $scope.$eval($attributes.fileUploadPath);
				CKEDITOR.inlineAll();
				var editor = $('[contenteditable=true]');

				createCKEditorInstance(editor, $scope, $compile);

				$element.on('removed', function(){
					for(var instance in CKEDITOR.instances){
						CKEDITOR.instances[instance].destroy()
					}
					$('.cke').remove();
				})
			}
		}
	}
});

module.directive('loadingIcon', function($compile){
	return {
		restrict: 'E',
		link: function($scope, $element, $attributes){
			var addImage = function(){
				var loadingIllustrationPath = $('link').attr('href').split('/theme.css')[0] + '/../img/icons/anim_loading_small.gif';
				$('<img>')
					.attr('src', loadingIllustrationPath)
					.attr('class', $attributes.class)
					.addClass('loading-icon')
					.appendTo($element);
			}
			if($attributes.default=== 'loading'){
				addImage();
			}
			http().bind('request-started.' + $attributes.request, function(e){
				addImage();
			});

			http().bind('request-ended.' + $attributes.request, function(e){
				var loadingDonePath = $('link').attr('href').split('/theme.css')[0] + '/../img/icons/checkbox-checked.png';
				$element.find('.loading-icon').remove();
				$('<img>')
					.attr('src', loadingDonePath)
					.appendTo($element);
			});
		}
	}
})

module.directive('loadingPanel', function($compile){
	return {
		restrict: 'A',
		link: function($scope, $element, $attributes){
			http().bind('request-started.' + $attributes.loadingPanel, function(e){
				var loadingIllustrationPath = $('link').attr('href').split('/theme.css')[0] + '/../img/illustrations/loading.gif';
				$element.append('<div class="loading-panel">' +
					'<h1>' + lang.translate('loading') + '</h1>' +
					'<img src="' + loadingIllustrationPath + '" />' +
					'</div>');

			})
			http().bind('request-ended.' + $attributes.loadingPanel, function(e){
				$element.find('.loading-panel').remove();
			})
		}
	}
});

module.directive('behaviour', function($compile){
	return {
		restrict: 'A',
		scope: {
			behaviour: '=',
			resource: '='
		},
		link: function($scope, $element, $attributes){
			console.log($scope);
		}
	}
});

module.directive('bottomScroll', function($compile){
	return {
		restrict: 'A',
		link: function($scope, $element, $attributes){
			$(window).scroll(function(){
				var scrollHeight = window.scrollY || document.getElementsByTagName('html')[0].scrollTop;
				//adding ten pixels to account for system specific behaviours
				scrollHeight += 10;

				if($(document).height() - $(window).height() < scrollHeight){
					$scope.$eval($attributes.bottomScroll);
				}
			})
		}
	}
})

module.directive('sharePanel', function($compile){
	return {
		scope: {
			resources: '='
		},
		restrict: 'E',
		templateUrl: '/infra/public/template/share-panel.html',
		link: function($scope, $element, $attributes){

		}
	}
})

$(document).ready(function(){
	if(!window.Model){
		Model = { build: function(){ } };
	}
	Model.build();
	Model.sync();
	angular.bootstrap($('html'), ['app']);
});


function Account($scope){
	"use strict";

	$scope.refreshAvatar = function(){
		http().get('/userbook/api/person').done(function(result){
			$scope.avatar = result.result['0'].photo;
			$scope.username = result.result['0'].displayName;
			$scope.$apply();
		});
	};

	$scope.refreshAvatar();
}

function Share($rootScope, $scope, ui, _, lang){
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

	http().get('/infra/public/json/sharing-rights.json').done(function(config){
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

	function rightsToActions(rights, http){
		var actions = {};

		rights.forEach(function(right){
			var action = _.find($scope.actions, function(action){
				return action.name.indexOf(right) !== -1
			});

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
				var path = '/' + appPrefix + '/share/remove/' + resource._id;
				var data = {};
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
			http().get('/' + appPrefix + '/share/json/' + id).done(function(data){
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
						elementObj.actions = groupActions;
						if(initModel){
							$scope.sharingModel.edited.push(elementObj);
						}

						elementObj.index = $scope.sharingModel.edited.length;
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
	})

	$scope.addEdit = function(item){
		item.actions = {};
		$scope.sharingModel.edited.push(item);
		item.index = $scope.sharingModel.edited.length;
		$scope.found = [];
		$scope.search = '';

		$scope.actions.forEach(function(action){
			var actionId = action.displayName.split('.')[1];
			if(actionsConfiguration[actionId].default){
				item.actions[action.displayName] = true;
				$scope.saveRights(item, action);
			}
		});
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
			var path = '/' + appPrefix + '/share/remove/' + resource._id;
			http().put(path, http().serialize(data)).done(function(){
				$rootScope.$broadcast('share-updated');
			});
		})
	}

	$scope.maxEdit = 5;

	$scope.displayMore = function(){
		var displayMoreInc = 5;
		$scope.maxEdit += displayMoreInc;
	}

	function applyRights(element, action){
		var data;
		if(element.login !== undefined){
			data = { userId: element.id }
		}
		else{
			data = { groupId: element.id }
		}
		data.actions = actionToRights(element, action);

		var setPath = 'json';
		if(!element.actions[action.displayName]){
			setPath = 'remove';
			_.filter($scope.actions, function(item){
				return _.find(item.requires, function(dependency){
					return action.displayName.indexOf(dependency) !== -1;
				}) !== undefined
			})
				.forEach(function(item){
					element.actions[item.displayName] = false;
					data.actions = data.actions.concat(actionToRights(element, item));
				})
		}
		else{
			action.requires.forEach(function(required){
				var action = _.find($scope.actions, function(action){
					return action.displayName.indexOf(required) !== -1;
				});
				element.actions[action.displayName] = true;
				data.actions = data.actions.concat(actionToRights(element, action));
			});
		}

		$scope.resources.forEach(function(resource){
			http().put('/' + appPrefix + '/share/' + setPath + '/' + resource._id, http().serialize(data)).done(function(){
				$rootScope.$broadcast('share-updated');
			});
		});
	}

	$scope.saveRights = function(element, action){
		if($scope.varyingRights){
			dropRights(function(){
				applyRights(element, action)
			});
		}
		else{
			applyRights(element, action);
		}
	};
}