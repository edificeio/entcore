var oneApp = {
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
					oneApp.notify.error(data);
				});
		},
		render : function (name, data) {
			_.extend(data, {
				'i18n' : oneApp.i18n.i18n,
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
				return oneApp.i18n.bundle[key] === undefined ? key : oneApp.i18n.bundle[key];
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

var oneModule = angular.module('one', ['ngSanitize'], function($interpolateProvider) {
		$interpolateProvider.startSymbol('[[');
		$interpolateProvider.endSymbol(']]');
	})
	.factory('notify', function(){
		if(!window.humane){
			One.load('humane');
		}

		return {
			message: function(type, message){
				message = One.translate(message);
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
	.factory('date', function() {
		if(window.moment === undefined){
			loader.syncLoad('moment');
		}
		var currentLanguage = ( navigator.language || navigator.browserLanguage ).slice( 0, 2 );
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
	.factory('http', function(){
		return {
			get: One.get,
			post: One.post,
			put: One.put,
			delete: One.delete,
			postFile: One.postFile,
			putFile: One.putFile,
			bind: One.bind,
			serialize: One.serialize
		}
	})
	.factory('lang', function(){
		return {
			translate: One.translate
		}
	})
	.factory('_', function(){
		if(window._ === undefined){
			loader.syncLoad('underscore');
		}
		return _;
	})
	.factory('navigation', function(){
		return navigation;
	})
	.factory('ui', function(){
		return ui;
	});

//directives

oneModule.directive('completeChange', function() {
	return {
		restrict: 'A',
		scope:{
			exec: '&completeChange',
			field: '=ngModel'
		},
		link: function($scope, $linkElement, $attributes) {
			$scope.$watch('field', function(newVal) {
				$linkElement.val(newVal);
			});

			$linkElement.bind('change', function() {
				$scope.field = $linkElement.val();
				$scope.$eval($scope.exec);
			});
		}
	};
});

oneModule.directive('fileInputChange', function($compile){
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
})

oneModule.directive('enhancedSelect', function($compile) {
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

oneModule.directive('translate', function($compile) {
	return {
		restrict: 'A',
		compile: function compile($element, $attributes, transclude) {
			$element.text(lang.translate($attributes.key));
			return {
				pre: function preLink(scope, $element, $attributes, controller) {},
				post: function postLink(scope, $element, $attributes, controller) {}
			};
		}
	};
});

oneModule.directive('translateAttr', function($compile) {
	return {
		restrict: 'A',
		compile: function compile($element, $attributes, transclude) {
			$element.attr($attributes.translateAttr, lang.translate($attributes[$attributes.translateAttr]));
			return {
				pre: function preLink(scope, $element, $attributes, controller) {},
				post: function postLink(scope, $element, $attributes, controller) {}
			};
		}
	};
});

oneModule.directive('preview', function($compile){
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

oneModule.directive('bindHtmlUnsafe', function($compile){
	return {
		restrict: 'A',
		scope: {
			bindHtmlUnsafe: '='
		},
		link: function($scope, $element){
			$scope.$watch('bindHtmlUnsafe', function(newVal){
				$element.html(newVal)
			});
		}
	}
});

oneModule.directive('portal', function($compile){
	return {
		restrict: 'E',
		transclude: true,
		templateUrl: '/public/template/portal.html',
		compile: function($element, $attribute){
			var rand = Math.random();
			$.getJSON('/theme?token=' + rand, function(data){
				var css = data.skin;
				ui.setStyle(css);
				$('body').show();
			})
		}
	}
});

oneModule.directive('htmlEditor', function($compile){
	return {
		restrict: 'E',
		transclude: true,
		replace: true,
		scope: {
			ngModel: '='
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

				CKEDITOR.on('instanceReady', function(){
					editor.html($scope.ngModel);
					var positionning = function(){
						$('.cke_chrome').width(editor.width());
						$('.cke_chrome').offset({
							top: editor.offset().top - $('.cke_chrome').height(),
							left: editor.offset().left
						});
					};
					if($scope.ngModel && $scope.ngModel.indexOf('<img') !== -1){
						$('img').on('load', positionning);
					}
					else{
						positionning();
					}
				})
				$scope.$watch('ngModel', function(newValue){
					if(editor.html() !== newValue){
						editor.html(newValue);
					}
				});

				editor.on('blur', function(e) {
					$scope.ngModel = editor.html();
					$scope.$apply();
				});

				$element.on('removed', function(){
					$('.cke').remove();
				})
			}
		}
	}
});

oneModule.directive('loadingPanel', function($compile){
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

oneModule.directive('navigationContext', function($compile){
	return {
		scope: {
			ngModel: '=',
			parentContext: '@',
			path: '@'
		},
		restrict: 'E',
		transclude: true,
		template: '<div ng-transclude></div>',
		link: function($scope, $element, $attribute){
			var newContext = navigation.addContext($scope.parentContext);
			$scope.ngModel = newContext;
			navigation.navigate(newContext, $scope.path);

			navigation.listen(function(){
				$element.trigger('navigation-changed');
			}, newContext);
			navigation.listen(function(){
				console.log('test');
			});
		}
	}
})

oneModule.directive('view', function($compile){
	return {
		template: '<div ng-include="view"></div>',
		compile: function($element, $attributes){
			return function($scope, $element, $attributes){
				var viewName = $attributes.view;

				var updateView = function(){
					var currentViews = navigation.views();
					$scope.view = currentViews[viewName];

					if(!$scope.$$phase) {
						$scope.$apply('view');
					}
				};

				var parentContext = $element.parents('navigation-context');
				if(parentContext.length === 0){
					navigation.listen(updateView);
				}
				else{
					parentContext.on('navigation-changed', function(){
						updateView();
					});
				}
			}
		}
	}
});

oneModule.directive('sharePanel', function($compile){
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
	angular.bootstrap($('html'), ['one'])
})


function Account($scope, http){
	"use strict";

	$scope.refreshAvatar = function(){
		http.get('/userbook/api/person').done(function(result){
			$scope.avatar = result.result['0'].photo;
			$scope.username = result.result['0'].displayName;
			$scope.$apply();
		});
	};

	$scope.refreshAvatar();
}

function Share($scope, http, ui, _, lang){
	$scope.sharing = {};
	$scope.edited = [];
	$scope.found = [];

	$scope.translate = lang.translate;

	function actionsToRights(item, findMissing){
		var actions = [];
		for(var action in item.actions){
			if(!findMissing){
				_.where($scope.sharing.actions, { displayName: action }).forEach(function(item){
					item.name.forEach(function(i){
						actions.push(i);
					})
				})
			}
		}

		return actions;
	}

	function findMissingActions(item){
		var missingActions = [];
		$scope.sharing.actions.forEach(function(action){
			if(!item.actions[action.displayName]){
				action.name.forEach(function(action){
					missingActions.push(action);
				})
			}
		});

		return missingActions;
	}

	function rightsToActions(rights){
		var actions = {};
		rights.forEach(function(right){
			var action = _.find($scope.sharing.actions, function(action){
				return action.name.indexOf(right) !== -1
			})

			if(!actions[action.displayName]){
				actions[action.displayName] = true;
			}
		});

		return actions;
	}

	var feedData = function(){
		http.get('/blog/share/json/' + $scope.resources._id).done(function(data){
			$scope.sharing = data;

			function addToEdit(type){
				for(var element in $scope.sharing[type].checked){
					var rights = $scope.sharing[type].checked[element];
					var groupActions = rightsToActions(rights);
					var elementObj = _.findWhere($scope.sharing[type].visibles, {
						id: element
					});
					elementObj.actions = groupActions;
					$scope.edited.push(elementObj);
				}
			}

			addToEdit('groups');
			addToEdit('users');
			$scope.$apply('edited');
		});
	}

	$scope.$watch('resources', function(){
		$scope.sharing = {};
		$scope.edited = [];
		$scope.search = '';
		$scope.found = [];
		feedData();
	})

	$scope.addEdit = function(item){
		item.actions = {};
		$scope.edited.push(item);
		$scope.found = [];
		$scope.search = '';
	};

	$scope.findUserOrGroup = function(){
		$scope.search = $scope.search.toLowerCase();
		$scope.found = _.union(
			_.filter($scope.sharing.groups.visibles, function(group){
				return group.name.toLowerCase().indexOf($scope.search) !== -1;
			}),
			_.filter($scope.sharing.users.visibles, function(user){
				return (user.firstName.toLowerCase() + ' ' + user.lastName.toLowerCase())
					.indexOf($scope.search) !== -1;
			})
		);
		$scope.found = _.filter($scope.found, function(element){
			return $scope.edited.indexOf(element) === -1;
		})
	};

	$scope.remove = function(element){
		var path = '/blog/share/remove/' + $scope.resources._id;
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

		http.put(path, http.serialize(data)).done(function(data){
			$scope.edited = _.reject($scope.edited, function(item){
				return item.id === element.id;
			})
		})
	}

	function setRights(data, actions, addOrRemove){
		var path = '/blog/share/' + addOrRemove + '/' + $scope.resources._id;
		data.actions = actions;

		http.put(path, http.serialize(data));
	}

	$scope.saveRights = function(){
		ui.hideLightbox();
		$scope.edited.forEach(function(element){
			var data;
			if(element.login !== undefined){
				data = { userId: element.id }
			}
			else{
				data = { groupId: element.id }
			}
			var actions = actionsToRights(element);

			if($scope.sharing.users.checked[element.id] || $scope.sharing.groups.checked[element.id]){
				//drop existing rights
				http.put('/blog/share/remove/' + $scope.resources._id, http.serialize(data)).done(function(){
					//add new rights
					setRights(data, actions, 'json');
				});
			}
			else{
				setRights(data, actions, 'json');
			}
		});
	}
}