/// <reference path="./ui.js"  />

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

var lang = window.idiom;

var template = {
    viewPath: '/' + appPrefix + '/public/template/',
    containers: {},
    open: function(name, view) {
        var path = this.viewPath + view + '.html';
        var folder = appPrefix;
        if (appPrefix === '.') {
            folder = 'portal';
        }
        if (skin.templateMapping[folder] && skin.templateMapping[folder].indexOf(view) !== -1) {
            path = '/assets/themes/' + skin.skin + '/template/' + folder + '/' + view + '.html';
        }

        this.containers[name] = path;

        if (this.callbacks && this.callbacks[name]) {
            this.callbacks[name].forEach(function(cb) {
                cb();
            });
        }
    },
    contains: function(name, view) {
        return this.containers[name] === this.viewPath + view + '.html';
    },
    isEmpty: function(name) {
        return this.containers[name] === 'empty' || !this.containers[name];
    },
    close: function(name) {
        this.containers[name] = 'empty';
        if (this.callbacks && this.callbacks[name]) {
            this.callbacks[name].forEach(function(cb) {
                cb();
            });
        }
    },
    watch: function(container, fn) {
        if (typeof fn !== 'function') {
            throw TypeError('template.watch(string, function) called with wrong parameters');
        }
        if (!this.callbacks) {
            this.callbacks = {};
        }
        if (!this.callbacks[container]) {
            this.callbacks[container] = [];
        }
        this.callbacks[container].push(fn);
    }
};

var notify = {
    message: function(type, message) {
        message = lang.translate(message);
        humane.spawn({
            addnCls: 'humane-original-' + type
        })(message);
    },
    error: function(message) {
        this.message('error', message);
    },
    info: function(message) {
        this.message('info', message)
    },
    success: function(message) {
        this.message('success', message);
    }
};

var module = angular.module('app', ['ngSanitize', 'ngRoute'], function($interpolateProvider) {
        $interpolateProvider.startSymbol('[[');
        $interpolateProvider.endSymbol(']]');
    })
    .factory('notify', function() {
        return notify;
    })
    .factory('route', function($rootScope, $route, $routeParams) {
        var routes = {};
        var currentAction = undefined;
        var currentParams = undefined;

        $rootScope.$on("$routeChangeSuccess", function($currentRoute, $previousRoute) {
            if (typeof routes[$route.current.action] === 'function' &&
                (currentAction !== $route.current.action || (currentParams !== $route.current.params && !(Object.getOwnPropertyNames($route.current.params).length === 0)))) {
                currentAction = $route.current.action;
                currentParams = $route.current.params;
                routes[$route.current.action]($routeParams);
                setTimeout(function() {
                    ui.scrollToId(window.location.hash.split('#')[1]);
                }, 100);
            }
        });

        return function(setRoutes) {
            routes = setRoutes;
        }
    })
    .factory('template', function() {
        return template;
    })
    .factory('date', function() {
        return {
            create: function(date) {
                return (moment ? moment(date) : date)
            },
            format: function(date, format) {
                if (!moment) {
                    return '';
                }
                return moment(date).format(format);
            },
            calendar: function(date) {
                if (!moment) {
                    return '';
                }
                return moment(date).calendar();
            }
        };
    })
    .factory('lang', function() {
        return lang
    })
    .factory('_', function() {
        if (window._ === undefined) {
            loader.syncLoad('underscore');
        }
        return _;
    })
    .factory('model', function($timeout) {
        var fa = Collection.prototype.trigger;
        Collection.prototype.trigger = function(event) {
            $timeout(function() {
                fa.call(this, event);
            }.bind(this), 10);
        };

        var fn = Model.prototype.trigger;
        Model.prototype.trigger = function(event, eventData) {
            $timeout(function() {
                fn.call(this, event, eventData);
            }.bind(this), 10);
        };

        return model;
    })
    .factory('ui', function() {
        return ui;
    })
    .factory('xmlHelper', function() {
        return {
            xmlToJson: function(xml, accumulator, stripNamespaces, flatten) {
                var nodeName;
                var that = this;
                if (!accumulator)
                    accumulator = {};
                if (!stripNamespaces)
                    stripNamespaces = true;
                if (flatten == null)
                    flatten = true

                if (stripNamespaces && xml.nodeName.indexOf(":") > -1) {
                    nodeName = xml.nodeName.split(':')[1];
                } else {
                    nodeName = xml.nodeName;
                }
                if ($(xml).children().length > 0) {
                    if (!flatten)
                        accumulator[nodeName] = [];
                    else
                        accumulator[nodeName] = {};
                    _.each($(xml).children(), function(child) {
                        if (!flatten)
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
    .factory('httpWrapper', function() {
        return {
            wrap: function(name, fun, context) {
                var scope = this
                if (typeof fun !== "function")
                    return
                if (typeof scope[name] !== "object")
                    scope[name] = {}
                if (scope[name].loading)
                    return
                scope[name].loading = true

                var args = []
                for (var i = 3; i < arguments.length; i++)
                    args.push(arguments[i])

                var completion = function() {
                    scope[name].loading = false
                    scope.$apply()
                }

                var xhrReq
                if (context) {
                    xhrReq = fun.apply(context, args)
                } else {
                    xhrReq = fun.apply(scope, args)
                }
                if (xhrReq && xhrReq.xhr)
                    xhrReq.xhr.complete(completion)
                else
                    completion()
            }
        }
    });

//routing
if (routes.routing) {
    module.config(routes.routing);
}

//directives
module.directive('completeChange', function() {
    return {
        restrict: 'A',
        scope: {
            exec: '&completeChange',
            field: '=ngModel'
        },
        link: function(scope, element, attributes) {
            scope.$watch('field', function(newVal) {
                element.val(newVal);
                if (element[0].type === 'textarea' && element.hasClass('inline-editing')) {
                    setTimeout(function() {
                        element.height(1);
                        element.height(element[0].scrollHeight - 1);
                    }, 100);

                }
            });

            element.bind('change', function() {
                scope.field = element.val();
                if (!scope.$$phase) {
                    scope.$apply('field');
                }
                scope.$parent.$eval(scope.exec);
                if (!scope.$$phase) {
                    scope.$apply('field');
                }
            });
        }
    };
});

module.directive('lightbox', function($compile) {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            show: '=',
            onClose: '&'
        },
        template: '<section class="lightbox">' +
            '<div class="background"></div>' +
            '<div class="content">' +
            '<div class="twelve cell" ng-transclude></div>' +
            '<div class="close-lightbox">' +
            '<i class="close-2x"></i>' +
            '</div>' +
            '</div>' +
            '</section>' +
            '</div>',
        link: function(scope, element, attributes) {
            var content = element.find('.content');
            element.children('.lightbox').find('> .background, > .content > .close-lightbox > i.close-2x').on('click', function(e) {
                $('body').removeClass('lightbox-opened');
                element.children('.lightbox').first().fadeOut();
                $('body').css({
                    overflow: 'auto'
                });

                scope.$eval(scope.onClose);
                if (!scope.$$phase) {
                    scope.$parent.$apply();
                }
            });
            element.children('.lightbox').on('mousedown', function(e) {
                e.stopPropagation();
            });

            scope.$watch('show', function(newVal) {
                if (newVal) {
                    $('body').addClass('lightbox-opened');
                    var lightboxWindow = element.children('.lightbox');

                    //Backup overflow hidden elements + z-index of parents
                    var parentElements = element.parents();

                    scope.backup = {
                        overflow: _.filter(parentElements, function(parent) {
                            return $(parent).css('overflow-x') !== 'visible' || $(parent).css('overflow-y') !== 'visible'
                        }),
                        zIndex: _.map(parentElements, function(parent) {
                            var index = '';
                            if ($(parent).attr('style') && $(parent).attr('style').indexOf('z-index') !== -1) {
                                index = $(parent).css('z-index')
                            }
                            return {
                                element: $(parent),
                                index: index
                            }
                        })
                    };

                    //Removing overflow properties
                    _.forEach(scope.backup.overflow, function(element) {
                        $(element).css({
                            'overflow': 'visible'
                        })
                    });

                    //Ensuring proper z-index
                    _.forEach(scope.backup.zIndex, function(elementObj) {
                        elementObj.element.css('z-index', 99999)
                    })

                    setTimeout(function() {
                        lightboxWindow.fadeIn();
                    }, 100);

                    $('body').css({
                        overflow: 'hidden'
                    });
                } else {
                    $('body').removeClass('lightbox-opened');
                    if (scope.backup) {
                        //Restoring stored elements properties
                        _.forEach(scope.backup.overflow, function(element) {
                            $(element).css('overflow', '')
                        })
                        _.forEach(scope.backup.zIndex, function(elementObj) {
                            elementObj.element.css('z-index', elementObj.index)
                        })
                    }

                    element.children('.lightbox').fadeOut();
                    $('body').css({
                        overflow: 'auto'
                    });
                }
            });

            scope.$on("$destroy", function() {
                $('body').removeClass('lightbox-opened');
                $('body').css({
                    overflow: 'auto'
                });

                if (scope.backup) {
                    //Restoring stored elements properties
                    _.forEach(scope.backup.overflow, function(element) {
                        $(element).css('overflow', '')
                    })
                    _.forEach(scope.backup.zIndex, function(elementObj) {
                        elementObj.element.css('z-index', elementObj.index)
                    })
                }
            });
        }
    }
});

module.directive('slider', function($compile, $parse) {
    return {
        restrict: 'E',
        scope: true,
        template: '<div class="bar"></div><div class="filled"></div><div class="cursor"></div><legend class="min"></legend><legend class="max"></legend>',
        link: function(scope, element, attributes) {
            element.addClass('drawing-zone');
            var cursor = element.children('.cursor');
            var max = parseInt(attributes.max);
            var min = parseInt(attributes.min);

            var ngModel = $parse(attributes.ngModel);

            var applyValue = function(newVal) {
                var pos = parseInt((newVal - min) * element.children('.bar').width() / (max - min));
                cursor.css({
                    left: pos + 'px',
                    position: 'absolute'
                });
                element.children('.filled').width(cursor.position().left);
            };

            $(window).on('resize', function() {
                applyValue(ngModel(scope));
            });

            scope.$watch(function() {
                return ngModel(scope);
            }, applyValue);

            if (typeof ngModel(scope) !== 'number') {
                ngModel.assign(scope, parseInt(attributes.default));
                applyValue(ngModel(scope));
            }

            element.children('legend.min').html(idiom.translate(attributes.minLegend));
            element.children('legend.max').html(idiom.translate(attributes.maxLegend));

            element.children('.bar, .filled').on('click', function(e) {
                var newPos = e.clientX - element.children('.bar').offset().left;
                var newVal = (newPos * (max - min) / element.children('.bar').width()) + min;
                ngModel.assign(scope, newVal);
                scope.$apply();
            });

            ui.extendElement.draggable(cursor, {
                lock: {
                    vertical: true
                },
                mouseUp: function() {
                    var cursorPosition = cursor.position().left;
                    var newVal = (cursorPosition * (max - min) / element.children('.bar').width()) + min;
                    ngModel.assign(scope, newVal);
                    scope.$apply();
                },
                tick: function() {
                    var cursorPosition = cursor.position().left;
                    element.children('.filled').width(cursorPosition);
                }
            });
        }
    }
});

module.directive('mediaLibrary', function($compile) {
    return {
        restrict: 'E',
        scope: {
            ngModel: '=',
            ngChange: '&',
            multiple: '=',
            fileFormat: '='
        },
        templateUrl: '/' + infraPrefix + '/public/template/media-library.html',
        link: function(scope, element, attributes) {
            scope.$watch(function() {
                return scope.$parent.$eval(attributes.visibility);
            }, function(newVal) {
                scope.visibility = newVal;
                if (!scope.visibility) {
                    scope.visibility = 'protected';
                }
                scope.visibility = scope.visibility.toLowerCase();
            });

            scope.upload = {
                loading: []
            };

            scope.$watch('ngModel', function(newVal) {
                if ((newVal && newVal._id) || (newVal && scope.multiple && newVal.length)) {
                    scope.ngChange();
                }

                scope.upload = {
                    loading: []
                };
            });

            $('body').on('click', '.lightbox-backdrop', function() {
                scope.upload = {
                    loading: []
                };
            });
        }
    }
});

module.directive('linker', function($compile) {
    return {
        restrict: 'E',
        templateUrl: '/' + infraPrefix + '/public/template/linker.html',
        scope: true,
        controller: function($scope) {
            $scope.linker = {
                me: model.me,
                search: {
                    text: '',
                    application: {}
                },
                params: {},
                resource: {}
            };
        },
        link: function(scope, element, attributes) {
            scope.linker.editor = scope.$eval(attributes.editor);
            scope.linker.onChange = function() {
                scope.$eval(attributes.onChange);
            };

            var linkNode = $('<a />');
            var appendText = '';
            scope.$watch(function() {
                return scope.display.chooseLink
            }, function(newVal) {
                if (newVal) {
                    if (!scope.linker.editor) {
                        scope.linker.editor = scope.$eval(attributes.editor);
                    }

                    if (!scope.display.chooseLink[scope.linker.editor.name]) {
                        return;
                    }

                    var contextEditor = scope.linker.editor;
                    var bookmarks = contextEditor.getSelection().createBookmarks(),
                        range = contextEditor.getSelection().getRanges()[0],
                        fragment = range.clone().cloneContents();
                    contextEditor.getSelection().selectBookmarks(bookmarks);

                    var node = $(range.startContainer.getParent().$);
                    if (node[0].nodeName !== 'A') {
                        node = $(range.startContainer.$);
                        if (node[0].nodeName !== 'A') {
                            scope.newNode = true;
                            return;
                        }
                    }

                    scope.newNode = false;

                    scope.linker.params.link = node.attr('href');
                    scope.linker.externalLink = !node.attr('data-id');
                    scope.linker.params.appPrefix = node.attr('data-app-prefix');
                    scope.linker.params.id = node.attr('data-id');
                    scope.linker.params.blank = node.attr('target') === '_blank';
                    scope.linker.params.target = node.attr('target');
                    scope.linker.params.tooltip = node.attr('tooltip');

                    scope.linker.search.application = _.find(scope.linker.apps, function(app) {
                        return app.address.indexOf(node.attr('data-app-prefix')) !== -1;
                    });

                    scope.linker.search.text = scope.params.id;
                    scope.linker.loadApplicationResources(function() {
                        scope.linker.searchApplication();
                        scope.linker.search.text = ' ';
                        scope.linker.$apply();
                    });
                } else {
                    scope.linker.params = {};
                    scope.linker.search.text = '';
                }
            }, true);

            http().get('/resources-applications').done(function(apps) {
                scope.linker.apps = [];
                var n = model.me.apps.length;
                model.me.apps.forEach(function(app) {
                    var prefix = _.find(apps, function(a) {
                        return app.address.indexOf(a) !== -1 && app.icon && app.icon.indexOf('/') === -1
                    });
                    if (prefix) {
                        app.prefix = prefix;
                        app.displayName = lang.translate(app.displayName);
                        Behaviours.loadBehaviours(app.prefix, function(behaviours) {
                            n--;
                            if (behaviours.loadResources) {
                                scope.linker.apps.push(app);
                            }
                            if (n === 0) {
                                var currentApp = _.find(scope.linker.apps, function(app) {
                                    return app.address.indexOf(appPrefix) !== -1 && app.icon.indexOf('/') === -1;
                                });

                                scope.linker.search.application = scope.linker.apps[0];
                                if (currentApp) {
                                    scope.linker.search.application = currentApp;
                                }

                                scope.linker.loadApplicationResources(function() {});
                            }
                        });
                    } else {
                        n--;
                    }
                });
            });

            scope.linker.loadApplicationResources = function(cb) {
                var split = scope.linker.search.application.address.split('/');
                var prefix = split[split.length - 1];
                scope.linker.params.appPrefix = prefix;
                if (!cb) {
                    cb = function() {
                        scope.linker.searchApplication();
                        scope.$apply('linker');
                    };
                }
                Behaviours.applicationsBehaviours[prefix].loadResources(cb);
                scope.linker.addResource = Behaviours.applicationsBehaviours[prefix].create;
            };

            scope.linker.searchApplication = function() {
                var split = scope.linker.search.application.address.split('/');
                var prefix = split[split.length - 1];
                scope.linker.params.appPrefix = prefix;
                Behaviours.loadBehaviours(scope.linker.params.appPrefix, function(appBehaviour) {
                    scope.linker.resources = _.filter(appBehaviour.resources, function(resource) {
                        return scope.linker.search.text !== '' && (lang.removeAccents(resource.title.toLowerCase()).indexOf(lang.removeAccents(scope.linker.search.text).toLowerCase()) !== -1 ||
                            resource._id === scope.linker.search.text);
                    });
                });
            };

            scope.linker.createResource = function() {
                Behaviours.loadBehaviours(scope.linker.params.appPrefix, function(appBehaviour) {
                    appBehaviour.create(scope.linker.resource, function() {
                        scope.linker.search.text = scope.linker.resource.title;
                        scope.linker.searchApplication();
                        scope.$apply();
                    });
                });
            };

            scope.linker.applyLink = function(link) {
                scope.linker.params.link = link;
            };

            scope.linker.applyResource = function(resource) {
                scope.linker.params.link = resource.path;
                scope.linker.params.id = resource._id;
            };

            scope.linker.saveLink = function() {
                if (scope.linker.params.blank) {
                    scope.linker.params.target = '_blank';
                }

                var contextEditor = scope.linker.editor;
                var bookmarks = contextEditor.getSelection().createBookmarks(),
                    range = contextEditor.getSelection().getRanges()[0],
                    fragment = range.clone().cloneContents();
                contextEditor.getSelection().selectBookmarks(bookmarks);

                var linkNode = scope.linker.editor.document.createElement('a');

                if (scope.linker.params.link) {
                    linkNode.setAttribute('href', scope.linker.params.link);

                    if (scope.linker.params.appPrefix) {
                        linkNode.setAttribute('data-app-prefix', scope.linker.params.appPrefix);
                        if (scope.linker.params.appPrefix === appPrefix && !scope.linker.externalLink) {
                            linkNode.data('reload', true);
                        }
                    }
                    if (scope.linker.params.id) {
                        linkNode.setAttribute('data-id', scope.linker.params.id);
                    }
                    if (scope.linker.params.blank) {
                        scope.linker.params.target = '_blank';
                        linkNode.setAttribute('target', scope.linker.params.target);
                    }
                    if (scope.linker.params.tooltip) {
                        linkNode.setAttribute('tooltip', scope.linker.params.tooltip);
                    }
                }

                var appendText = "",
                    childList = fragment.getChildren(),
                    childCount = childList.count();

                if (!scope.newNode && childCount === 0) {
                    appendText = range.startContainer.getText();
                } else {
                    for (var i = 0; i < childCount; i++) {
                        var child = childList.getItem(i);
                        if (child.$.nodeName === 'A' || !child.getOuterHtml) {
                            appendText += child.getText();
                        } else {
                            appendText += child.getOuterHtml();
                        }
                    }
                    if (childCount === 0) {
                        appendText = scope.linker.params.link;
                    }
                }

                linkNode.appendHtml(appendText);
                if (!scope.newNode && childCount === 0) {
                    linkNode.insertAfter(range.startContainer);
                    range.startContainer.remove(false);
                } else {
                    scope.linker.editor.insertElement(linkNode);
                }

                scope.display.chooseLink[scope.linker.editor.name] = false;
                scope.linker.onChange();
                scope.$apply();
            };

            scope.linker.cancel = function() {
                scope.display.chooseLink[scope.linker.editor.name] = false;
            };
        }
    }
});

module.directive('calendar', function($compile) {
    return {
        restrict: 'E',
        scope: true,
        templateUrl: '/' + infraPrefix + '/public/template/calendar.html',
        controller: function($scope, $timeout) {
            var refreshCalendar = function() {
                model.calendar.clearScheduleItems();
                $scope.items = _.where(_.map($scope.items, function(item) {
                    item.beginning = item.startMoment;
                    item.end = item.endMoment;
                    return item;
                }), {
                    is_periodic: false
                });
                model.calendar.addScheduleItems($scope.items);
                $scope.calendar = model.calendar;
                $scope.moment = moment;
                $scope.display.editItem = false;
                $scope.display.createItem = false;

                $scope.editItem = function(item) {
                    $scope.calendarEditItem = item;
                    $scope.display.editItem = true;
                };

                $scope.createItem = function(day, timeslot) {
                    $scope.newItem = {};
                    var year = model.calendar.year;
                    if (day.index < model.calendar.firstDay.dayOfYear()) {
                        year++;
                    }
                    $scope.newItem.beginning = moment().utc().year(year).dayOfYear(day.index).hour(timeslot.start);
                    $scope.newItem.end = moment().utc().year(year).dayOfYear(day.index).hour(timeslot.end);
                    model.calendar.newItem = $scope.newItem;
                    $scope.onCreateOpen();
                };

                $scope.closeCreateWindow = function() {
                    $scope.display.createItem = false;
                    $scope.onCreateClose();
                };

                $scope.updateCalendarWeek = function() {
                    //annoying new year workaround
                    if (moment(model.calendar.dayForWeek).week() === 1 && moment(model.calendar.dayForWeek).dayOfYear() > 7) {
                        model.calendar = new calendar.Calendar({
                            week: moment(model.calendar.dayForWeek).week(),
                            year: moment(model.calendar.dayForWeek).year() + 1
                        });
                    } else if (moment(model.calendar.dayForWeek).week() === 53 && moment(model.calendar.dayForWeek).dayOfYear() < 7) {
                        model.calendar = new calendar.Calendar({
                            week: moment(model.calendar.dayForWeek).week(),
                            year: moment(model.calendar.dayForWeek).year() - 1
                        });
                    } else {
                        model.calendar = new calendar.Calendar({
                            week: moment(model.calendar.dayForWeek).week(),
                            year: moment(model.calendar.dayForWeek).year()
                        });
                    }
                    model.trigger('calendar.date-change');
                    refreshCalendar();
                };

                $scope.previousTimeslots = function() {
                    calendar.startOfDay--;
                    calendar.endOfDay--;
                    model.calendar = new calendar.Calendar({
                        week: moment(model.calendar.dayForWeek).week(),
                        year: moment(model.calendar.dayForWeek).year()
                    });
                    refreshCalendar();
                };

                $scope.nextTimeslots = function() {
                    calendar.startOfDay++;
                    calendar.endOfDay++;
                    model.calendar = new calendar.Calendar({
                        week: moment(model.calendar.dayForWeek).week(),
                        year: moment(model.calendar.dayForWeek).year()
                    });
                    refreshCalendar();
                };
            };

            calendar.setCalendar = function(cal) {
                model.calendar = cal;
                refreshCalendar();
            };

            $timeout(function() {
                refreshCalendar();
                $scope.$watchCollection('items', refreshCalendar);
            }, 0);
            $scope.refreshCalendar = refreshCalendar;
        },
        link: function(scope, element, attributes) {
            var allowCreate;
            scope.display = {};
            scope.display.readonly = false;
            attributes.$observe('createTemplate', function() {
                if (attributes.createTemplate) {
                    template.open('schedule-create-template', attributes.createTemplate);
                    allowCreate = true;
                }
                if (attributes.displayTemplate) {
                    template.open('schedule-display-template', attributes.displayTemplate);
                }
            });
            attributes.$observe('readonly', function(){
                if(attributes.readonly && attributes.readonly !== 'false'){
                    scope.display.readonly = true;
                }
                if(attributes.readonly && attributes.readonly == 'false'){
                    scope.display.readonly = false;
                }
            });

            scope.items = scope.$eval(attributes.items);
            scope.onCreateOpen = function() {
                if (!allowCreate) {
                    return;
                }
                scope.$eval(attributes.onCreateOpen);
                scope.display.createItem = true;
            };
            scope.onCreateClose = function() {
                scope.$eval(attributes.onCreateClose);
            };
            scope.$watch(function() {
                return scope.$eval(attributes.items)
            }, function(newVal) {
                scope.items = newVal;
            });
        }
    }
});

module.directive('scheduleItem', function($compile) {
    return {
        restrict: 'E',
        require: '^calendar',
        template: '<div class="schedule-item" resizable horizontal-resize-lock draggable>' +
            '<container template="schedule-display-template" class="absolute"></container>' +
            '</div>',
        controller: function($scope) {

        },
        link: function(scope, element, attributes) {
            var parentSchedule = element.parents('.schedule');
            var scheduleItemEl = element.children('.schedule-item');
            var dayWidth = parentSchedule.find('.day').width();
            if (scope.item.beginning.dayOfYear() !== scope.item.end.dayOfYear() || scope.item.locked) {
                scheduleItemEl.removeAttr('resizable');
                scheduleItemEl.removeAttr('draggable');
                scheduleItemEl.unbind('mouseover');
                scheduleItemEl.unbind('click');
                scheduleItemEl.data('lock', true)
            }

            var getTimeFromBoundaries = function() {
                // compute element positon added to heiht of 7 hours ao avoid negative value side effect
                var topPos = scheduleItemEl.position().top + (calendar.dayHeight * calendar.startOfDay);
                var startTime = moment().utc();
                startTime.hour(Math.floor(topPos / calendar.dayHeight));
                startTime.minute((topPos % calendar.dayHeight) * 60 / calendar.dayHeight);

                var endTime = moment().utc();
                endTime.hour(Math.floor((topPos + scheduleItemEl.height()) / calendar.dayHeight));
                endTime.minute(((topPos + scheduleItemEl.height()) % calendar.dayHeight) * 60 / calendar.dayHeight);

                startTime.year(model.calendar.year);
                endTime.year(model.calendar.year);

                var days = element.parents('.schedule').find('.day');
                var center = scheduleItemEl.offset().left + scheduleItemEl.width() / 2;
                var dayWidth = days.first().width();
                days.each(function(index, item) {
                    var itemLeft = $(item).offset().left;
                    if (itemLeft < center && itemLeft + dayWidth > center) {
                        var day = index + 1;
                        var week = model.calendar.week;
                        endTime.week(week);
                        startTime.week(week);
                        if (day === 7) {
                            day = 0;
                            endTime.week(week + 1);
                            startTime.week(week + 1);
                        }
                        endTime.day(day);
                        startTime.day(day);
                    }
                });

                return {
                    startTime: startTime,
                    endTime: endTime
                }
            };

            scheduleItemEl.on('stopResize', function() {
                var newTime = getTimeFromBoundaries();
                scope.item.beginning = newTime.startTime;
                scope.item.end = newTime.endTime;
                if (typeof scope.item.calendarUpdate === 'function') {
                    scope.item.calendarUpdate();
                    model.calendar.clearScheduleItems();
                    model.calendar.addScheduleItems(scope.$parent.items);
                    scope.$parent.$apply('items');
                }
            });

            scheduleItemEl.on('stopDrag', function() {
                var newTime = getTimeFromBoundaries();
                scope.item.beginning = newTime.startTime;
                scope.item.end = newTime.endTime;
                if (typeof scope.item.calendarUpdate === 'function') {
                    scope.item.calendarUpdate();
                    model.calendar.clearScheduleItems();
                    model.calendar.addScheduleItems(scope.$parent.items);
                    scope.$parent.$apply('items');
                    parentSchedule.find('schedule-item').each(function(index, item) {
                        var scope = angular.element(item).scope();
                        scope.placeItem()
                    });
                }
            });

            var placeItem = function() {
                var cellWidth = element.parent().width() / 12;
                var startDay = scope.item.beginning.dayOfYear();
                var endDay = scope.item.end.dayOfYear();
                var hours = calendar.getHours(scope.item, scope.day);

                var itemWidth = scope.day.scheduleItems.scheduleItemWidth(scope.item);
                scheduleItemEl.css({
                    width: itemWidth + '%'
                });
                var calendarGutter = 0;
                var collision = true;
                while (collision) {
                    collision = false;
                    scope.day.scheduleItems.forEach(function(scheduleItem) {
                        if (scheduleItem === scope.item) {
                            return;
                        }
                        if (scheduleItem.beginning < scope.item.end && scheduleItem.end > scope.item.beginning) {
                            if (scheduleItem.calendarGutter === calendarGutter) {
                                calendarGutter++;
                                collision = true;
                            }
                        }
                    });
                }
                scope.item.calendarGutter = calendarGutter;
                var beginningMinutesHeight = scope.item.beginning.minutes() * calendar.dayHeight / 60;
                var endMinutesHeight = scope.item.end.minutes() * calendar.dayHeight / 60;
                var top = (hours.startTime - calendar.startOfDay) * calendar.dayHeight + beginningMinutesHeight;
                scheduleItemEl.height(((hours.endTime - hours.startTime) * calendar.dayHeight - beginningMinutesHeight + endMinutesHeight) + 'px');
                scheduleItemEl.css({
                    top: top + 'px',
                    left: (scope.item.calendarGutter * (itemWidth * dayWidth / 100)) + 'px'
                });
                var container = element.find('container')
                if (top < 0) {
                    container.css({
                        top: (Math.abs(top) - 5) + 'px'
                    });
                    container.height(element.children('.schedule-item').height() + top + 5)
                } else {
                    container.css({
                        top: 0 + 'px'
                    });
                    container.css({
                        height: '100%'
                    })
                }
            };
            scope.$parent.$watch('items', placeItem);
            scope.$parent.$watchCollection('items', placeItem);
            scope.$watch('item', placeItem);
            scope.placeItem = placeItem;
        }
    }
});

module.directive('container', function($compile) {
    return {
        restrict: 'E',
        scope: true,
        template: '<div ng-include="templateContainer"></div>',
        link: function(scope, element, attributes) {
            scope.tpl = template;

            template.watch(attributes.template, function() {
                scope.templateContainer = template.containers[attributes.template];
                if (scope.templateContainer === 'empty') {
                    scope.templateContainer = undefined;
                }
            });

            if (attributes.template) {
                scope.templateContainer = template.containers[attributes.template];
            }
        }
    }
});

module.directive('colorSelect', function($compile) {
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
        link: function(scope, element, attributes) {
            scope.colors = ['orange', 'pink', 'purple', 'blue', 'green', 'black', 'white', 'transparent'];
            scope.setColor = function(color) {
                scope.ngModel = color;
            };

            element.find('.colors-opener').on('click', function(e) {
                scope.pickColor = !scope.pickColor;
                scope.$apply('pickColor');
                e.stopPropagation();
                $('body, .main').one('click', function() {
                    scope.pickColor = false;
                    scope.$apply('pickColor');
                });
            });
        }
    }
});

module.directive('imageSelect', function($compile) {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            ngModel: '=',
            thumbnails: '&',
            ngChange: '&',
            default: '@'
        },
        template: '<div><img ng-src="[[ngModel]]?[[getThumbnails()]]" class="pick-file" draggable="false" ng-if="ngModel" style="cursor: pointer" />' +
            '<img skin-src="[[default]]" class="pick-file" draggable="false" ng-if="!ngModel" style="cursor: pointer" />' +
            '<lightbox show="userSelecting" on-close="userSelecting = false;">' +
            '<media-library ' +
            'visibility="selectedFile.visibility"' +
            'ng-change="updateDocument()" ' +
            'ng-model="selectedFile.file" ' +
            'file-format="\'img\'">' +
            '</media-library>' +
            '</lightbox>' +
            '</div>',
        link: function(scope, element, attributes) {
            scope.selectedFile = {
                file: {},
                visibility: 'protected'
            };

            scope.selectedFile.visibility = scope.$parent.$eval(attributes.visibility);
            if (!scope.selectedFile.visibility) {
                scope.selectedFile.visibility = 'protected';
            }
            scope.selectedFile.visibility = scope.selectedFile.visibility.toLowerCase();

            element.on('dragenter', function(e) {
                e.preventDefault();
            });

            element.on('dragover', function(e) {
                element.addClass('droptarget');
                e.preventDefault();
            });

            element.on('dragleave', function() {
                element.removeClass('droptarget');
            });

            element.on('drop', function(e) {
                element.removeClass('droptarget');
                element.addClass('loading-panel');
                e.preventDefault();
                var file = e.originalEvent.dataTransfer.files[0];
                workspace.Document.prototype.upload(file, 'file-upload-' + file.name + '-0', function(doc) {
                    scope.selectedFile.file = doc;
                    scope.updateDocument();
                    element.removeClass('loading-panel');
                }, scope.selectedFile.visibility);
            });

            scope.$watch('thumbnails', function(thumbs) {
                var evaledThumbs = scope.$eval(thumbs);
                if (!evaledThumbs) {
                    return;
                }
                scope.getThumbnails = function() {
                    var link = '';
                    evaledThumbs.forEach(function(th) {
                        link += 'thumbnail=' + th.width + 'x' + th.height + '&';
                    });
                    return link;
                }
            });

            scope.updateDocument = function() {
                scope.userSelecting = false;
                var path = '/workspace/document/';
                if (scope.selectedFile.visibility === 'public') {
                    path = '/workspace/pub/document/'
                }
                scope.ngModel = path + scope.selectedFile.file._id;
                scope.$apply();
                scope.ngChange();
            };
            element.on('click', '.pick-file', function() {
                scope.userSelecting = true;
                scope.$apply('userSelecting');
            });
        }
    }
});

module.directive('soundSelect', function($compile) {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            ngModel: '=',
            ngChange: '&',
            visibility: '@'
        },
        template: '<div><audio ng-src="[[ngModel]]" controls ng-if="ngModel" style="cursor: pointer"></audio>' +
            '<lightbox show="userSelecting" on-close="userSelecting = false;">' +
            '<media-library ' +
            'visibility="selectedFile.visibility"' +
            'ng-change="updateDocument()" ' +
            'ng-model="selectedFile.file" ' +
            'file-format="\'audio\'">' +
            '</media-library>' +
            '</lightbox>' +
            '</div>',
        link: function(scope, element, attributes) {
            scope.selectedFile = {
                file: {},
                visibility: 'protected'
            };

            scope.selectedFile.visibility = scope.$parent.$eval(attributes.visibility);
            if (!scope.selectedFile.visibility) {
                scope.selectedFile.visibility = 'protected';
            }
            scope.selectedFile.visibility = scope.selectedFile.visibility.toLowerCase();

            scope.updateDocument = function() {
                scope.userSelecting = false;
                var path = '/workspace/document/';
                if (scope.selectedFile.visibility === 'public') {
                    path = '/workspace/pub/document/'
                }
                scope.ngModel = path + scope.selectedFile.file._id;
                scope.$apply('ngModel');
                scope.ngChange();
            };
            element.on('click', 'audio', function() {
                scope.userSelecting = true;
                scope.$apply('userSelecting');
            });
        }
    }
});

module.directive('mediaSelect', function($compile) {
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
        compile: function(element, attributes) {

            if (!attributes.mytooltip && attributes.tooltip) {
                console.warn('tooltip attribute is deprecated on media-select tag, use mytooltip instead.');
                element.attr("mytooltip", attributes.tooltip);
                element.removeAttr('tooltip');
                attributes.mytooltip = attributes.tooltip;
                delete attributes.tooltip;
            }

            return function(scope, element, attributes) {
                //link: function(scope, element, attributes){
                scope.selectedFile = {
                    file: {},
                    visibility: 'protected'
                };
                scope.selectedFile.visibility = scope.$parent.$eval(attributes.visibility);
                if (!scope.selectedFile.visibility) {
                    scope.selectedFile.visibility = 'protected';
                }
                scope.selectedFile.visibility = scope.selectedFile.visibility.toLowerCase();

                if (!scope.mytooltip) {
                    element.find('input').removeAttr('tooltip');
                }

                attributes.$observe('label', function(newVal) {
                    element.find('[type=button]').attr('value', lang.translate(newVal));
                });

                scope.$watch('fileFormat', function(newVal) {
                    if (newVal === undefined) {
                        scope.fileFormat = 'img'
                    }
                });
                scope.updateDocument = function() {
                    scope.userSelecting = false;
                    var path = '/workspace/document/';
                    if (scope.selectedFile.visibility === 'public') {
                        path = '/workspace/pub/document/'
                    }
                    scope.ngModel = path + scope.selectedFile.file._id;
                    scope.$apply('ngModel');
                    scope.ngChange();
                };
                element.find('.pick-file').on('click', function() {
                    scope.userSelecting = true;
                    scope.$apply('userSelecting');
                });
            }
        }
    }
});

module.directive('filesPicker', function($compile) {
    return {
        restrict: 'E',
        transclude: true,
        replace: true,
        template: '<input type="button" ng-transclude />',
        scope: {
            ngChange: '&',
            ngModel: '='
        },
        link: function(scope, element, attributes) {
            element.on('click', function() {
                var fileSelector = $('<input />', {
                        type: 'file'
                    })
                    .hide()
                    .appendTo('body');
                if (attributes.multiple !== undefined) {
                    fileSelector.attr('multiple', true);
                }

                fileSelector.on('change', function() {
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

module.directive('filesInputChange', function($compile) {
    return {
        restrict: 'A',
        scope: {
            filesInputChange: '&',
            file: '=ngModel'
        },
        link: function($scope, $element) {
            $element.bind('change', function() {
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
        scope: {
            options: '=',
            class: '@',
            current: '=',
            change: '&'
        },
        link: function(scope, element, attributes) {
            element.bind('change', function() {
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

module.directive('preview', function($compile) {
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
        link: function($scope, $element, $attributes) {
            $scope.$watch('content', function(newValue) {
                var fragment = $(newValue);
                $element.find('.image').html(fragment.find('img').first());

                var paragraph = _.find(fragment.find('p'), function(node) {
                    return $(node).text().length > 0;
                });
                $element.find('.paragraph').text($(paragraph).text());
            })
        }
    }
});

module.directive('bindHtml', function($compile) {
    return {
        restrict: 'A',
        scope: {
            bindHtml: '='
        },
        link: function(scope, element) {
            scope.$watch('bindHtml', function(newVal) {
                var htmlVal = $('<div>' + (newVal || '') + '</div>')
                    //Remove resizable attributes
                htmlVal.find('[resizable]').removeAttr('resizable').css('cursor', 'initial');
                htmlVal.find('[draggable]').removeAttr('draggable').css('cursor', 'initial');
                htmlVal.find('[bind-html]').removeAttr('bind-html');
                htmlVal.find('[ng-include]').removeAttr('ng-include');
                htmlVal.find('[ng-repeat]').removeAttr('ng-repeat');
                htmlVal.find('[ng-transclude]').removeAttr('ng-transclude');
                htmlVal.find('script').remove();
				htmlVal.find('*').each(function(index, item) {
					var attributes = item.attributes;
					for(var i = 0; i < attributes.length; i++){
						if(attributes[i].name.startsWith('on')){
							item.removeAttribute(attributes[i].name);
						}
					}
				});
                if(htmlVal.find('portal').length){
                    var portal = htmlVal.find('portal');
                    htmlVal = $('<div>' + (portal.find('[bind-html]').html() || '') + '</div>')
                }
                var htmlContent = htmlVal[0].outerHTML;
                if (!window.MathJax && !window.MathJaxLoading) {
                    window.MathJaxLoading = true;
                    loader.openFile({
                        async: true,
                        ajax: false,
                        url: '/infra/public/mathjax/MathJax.js',
                        success: function() {
                            window.MathJaxLoading = false;
                            MathJax.Hub.Config({
                                messageStyle: 'none',
                                tex2jax: {
                                    preview: 'none'
                                },
                                jax: ["input/TeX", "output/CommonHTML"],
                                extensions: ["tex2jax.js", "MathMenu.js", "MathZoom.js"],
                                TeX: {
                                    extensions: ["AMSmath.js", "AMSsymbols.js", "noErrors.js", "noUndefined.js"]
                                }
                            });
                            MathJax.Hub.Queue(["Typeset",MathJax.Hub]);
                        }
                    });
                }
                element.html($compile(htmlContent)(scope.$parent));
                //weird browser bug with audio tags
                element.find('audio').each(function(index, item) {
                    var parent = $(item).parent();
                    $(item)
                        .attr("src", item.src)
                        .attr('preload', 'none')
                        .detach()
                        .appendTo(parent);
                });

                if (window.MathJax && window.MathJax.Hub) {
                    MathJax.Hub.Queue(["Typeset",MathJax.Hub]);
                }
            });
        }
    }
});

module.directive('portal', function($compile) {
    return {
        restrict: 'E',
        transclude: true,
        templateUrl: skin.portalTemplate,
        compile: function(element, attributes, transclude) {
            element.find('[logout]').attr('href', '/auth/logout?callback=' + skin.logoutCallback);
            ui.setStyle(skin.theme);
            Http.prototype.bind('disconnected', function() {
                window.location.href = '/';
            })
        }
    }
});

module.directive('adminPortal', function($compile) {
    skin.skin = 'admin';
    skin.theme = '/public/admin/default/';
    return {
        restrict: 'E',
        transclude: true,
        templateUrl: '/public/admin/portal.html',
        compile: function(element, attributes, transclude) {
            $('[logout]').attr('href', '/auth/logout?callback=' + skin.logoutCallback);
            http().get('/userbook/preference/admin').done(function(data) {
                var theme = data.preference ? JSON.parse(data.preference) : null

                if (!theme || !theme.path)
                    ui.setStyle(skin.theme)
                else {
                    ui.setStyle('/public/admin/' + theme.path + '/')
                }
            }).error(function(error) {
                ui.setStyle(skin.theme)
            })
        }
    }
});

module.directive('portalStyles', function($compile) {
    return {
        restrict: 'E',
        compile: function(element, attributes) {
            $('[logout]').attr('href', '/auth/logout?callback=' + skin.logoutCallback);
            ui.setStyle(skin.theme);
        }
    }
});

module.directive('defaultStyles', function($compile) {
    return {
        restrict: 'E',
        link: function(scope, element, attributes) {
            ui.setStyle(skin.theme);
        }
    }
});

module.directive('skinSrc', function($compile) {
    return {
        restrict: 'A',
        scope: '&',
        link: function(scope, element, attributes) {
            if (!$('#theme').attr('href')) {
                return;
            }
            attributes.$observe('skinSrc', function() {
                if (attributes.skinSrc.indexOf('http://') === -1 && attributes.skinSrc.indexOf('https://') === -1 && attributes.skinSrc.indexOf('/workspace/') === -1) {
                    if(attributes.skinSrc[0] === '/'){
                        attributes.skinSrc = attributes.skinSrc.substring(1);
                    }
                    element.attr('src', skin.basePath + attributes.skinSrc);
                } else {
                    element.attr('src', attributes.skinSrc);
                }
            });

        }
    }
});

module.directive('localizedClass', function($compile) {
    return {
        restrict: 'A',
        link: function($scope, $attributes, $element) {
            $element.$addClass(currentLanguage);
        }
    }
});

module.directive('pullDownMenu', function($compile, $timeout) {
    return {
        restrict: 'E',
        transclude: true,
        template: '<div class="pull-down-menu hide" ng-transclude></div>',
        controller: function($scope) {}
    }
});

module.directive('pullDownOpener', function($compile, $timeout) {
    return {
        restrict: 'E',
        require: '^pullDownMenu',
        transclude: true,
        template: '<div class="pull-down-opener" ng-transclude></div>',
        link: function(scope, element, attributes) {
            element.find('.pull-down-opener').on('click', function() {
                var container = element.parents('.pull-down-menu');
                if (container.hasClass('hide')) {
                    setTimeout(function() {
                        $('body').on('click.pulldown', function() {
                            container.addClass('hide');
                            $('body').unbind('click.pulldown');
                        });
                    }, 0);
                    container.removeClass('hide');

                } else {
                    $('body').unbind('click.pulldown');
                    container.addClass('hide');
                }
            });
        }
    }
});

module.directive('pullDownContent', function($compile, $timeout) {
    return {
        restrict: 'E',
        require: '^pullDownMenu',
        transclude: true,
        template: '<div class="wrapper"><div class="arrow"></div><div class="pull-down-content" ng-transclude></div></div>',
        link: function(scope, element, attributes) {}
    }
});

module.directive('topNotification', function() {
    return {
        restrict: 'E',
        template: '<div class="notify-top">' +
            '<div class="notify-top-content" ng-bind-html="content"></div>' +
            '<div class="notify-top-actions">' +
            '<span ng-click="cancel()">[[doConfirm ? labels().cancel : labels().ok]]</span>' +
            '<span ng-click="ok()" ng-show="doConfirm">[[labels().confirm]]</span> ' +
            '<span ng-repeat="add in additional" ng-click="add.action()">[[ add.label ]]</span> ' +
            '</div>' +
            '</div>',
        scope: {
            trigger: '=',
            confirm: '=',
            content: '=',
            additional: '=',
            labels: '&'
        },
        link: function(scope, element, attributes) {
            element.css('display', 'none')
            scope.doConfirm = false
            scope.cancel = function() {
                scope.trigger = false
            }
            scope.ok = function() {
                scope.trigger = false
                scope.confirm()
            }
            if (!scope.labels()) {
                scope.labels = function() {
                    return {
                        confirm: lang.translate('confirm'),
                        cancel: lang.translate('cancel'),
                        ok: lang.translate('ok')
                    }
                }
            }
            if(!scope.additional || ! (scope.additional instanceof Array))
                scope.additional = []
            scope.$watch('trigger', function(newVal) {
                if (newVal)
                    element.slideDown()
                else
                    element.slideUp()
            });
            scope.$watch('confirm', function(newVal) {
                scope.doConfirm = newVal ? true : false
            })
        }
    }
});

module.directive('recorder', function() {
    return {
        restrict: 'E',
        scope: {
            ngModel: '=',
            format: '@',
            onUpload: '&'
        },
        templateUrl: '/infra/public/template/recorder.html',
        link: function(scope, element, attributes) {
            scope.recorder = recorder;
            if (attributes.protected !== undefined) {
                recorder.protected = true;
            }
            recorder.state(function() {
                scope.$apply('recorder');
            });
            scope.switchRecord = function() {
                if (recorder.status === 'recording') {
                    recorder.pause();
                } else {
                    recorder.record();
                }
            };
            scope.time = function() {
                var minutes = parseInt(recorder.elapsedTime / 60);
                if (minutes < 10) {
                    minutes = '0' + minutes;
                }
                var seconds = parseInt(recorder.elapsedTime % 60);
                if (seconds < 10) {
                    seconds = '0' + seconds;
                }
                return minutes + ':' + seconds;
            };
            scope.switchPlay = function() {
                if (recorder.status === 'playing') {
                    recorder.pause();
                } else {
                    recorder.play();
                }
            };
            scope.saveRecord = function() {
                recorder.save(function(doc) {
                    scope.ngModel = doc;
                    scope.onUpload();
                    scope.$apply();
                });
            };
        }
    }
});

module.directive('dropDown', function($compile, $timeout) {
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
        link: function(scope, element, attributes) {
            scope.limit = 6;
            var dropDown = element.find('[data-drop-down]');
            var dropDownContent = element.find('[data-drop-down] ul')

            scope.setDropDownHeight = function() {
                var liHeight = 0;
                var max = Math.min(scope.limit, scope.options.length);
                dropDownContent.find('li').each(function(index, el) {
                    liHeight += $(el).height();
                    return index < max;
                });
                dropDownContent.height(liHeight)
            };
            scope.increaseLimit = function() {
                scope.limit += 5;
                $timeout(function() {
                    scope.setDropDownHeight()
                });
            };
            scope.$watchCollection('options', function(newValue) {
                if (!scope.options || scope.options.length === 0) {
                    dropDownContent.height();
                    dropDown.addClass('hidden');
                    scope.limit = 6;
                    dropDownContent.attr('style', '');
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
                setTimeout(function() {
                    scope.setDropDownHeight()
                }, 100);
            });

            dropDown.detach().appendTo('body');

            dropDown.on('click', 'li', function(e) {
                if ($(e.target).hasClass('display-more')) {
                    return;
                }
                scope.limit = 6;
                dropDown.attr('style', '');
                dropDownContent.attr('style', '');
                scope.current = $(this).scope().option;
                scope.ngModel = $(this).scope().option;
                scope.$apply('ngModel');
                scope.$eval(scope.ngChange);
                scope.$eval(scope.onClose);
                scope.$apply('ngModel');
            });

            var closeDropDown = function(e) {
                if (dropDown.find(e.target).length > 0) {
                    return;
                }
                scope.$eval(scope.onClose);
                scope.$apply();
            };

            $('body').on('click', closeDropDown);
            dropDown.attr('data-opened-drop-down', true);
            element.on('$destroy', function() {
                $('body').unbind('click', closeDropDown);
                dropDown.remove();
            });
        }
    }
});

module.directive('autocomplete', function($compile, $timeout) {
    return {
        restrict: 'E',
        replace: true,
        scope: {
            options: '&',
            ngModel: '=',
            ngChange: '&',
            search: '=?'
        },
        template: '' +
            '<div class="row">' +
            '<input type="text" class="twelve cell" ng-model="search" translate attr="placeholder" placeholder="search" autocomplete="off" />' +
            '<div data-drop-down class="drop-down autocomplete">' +
            '<div>' +
            '<ul class="ten cell right-magnet">' +
            '<li ng-repeat="option in match | limitTo:10" ng-model="option">[[option.toString()]]</li>' +
            '</ul>' +
            '</div>' +
            '</div>' +
            '</div>',
        link: function(scope, element, attributes) {
            var token;
            if (attributes.autocomplete === 'off') {
                return;
            }
            var dropDownContainer = element.find('[data-drop-down]');
            var linkedInput = element.find('input');
            scope.match = [];

            scope.setDropDownHeight = function() {
                var liHeight = 0;
                var max = Math.min(10, scope.match.length);
                dropDownContainer.find('li').each(function(index, el) {
                    liHeight += $(el).height();
                    return index < max;
                })
                dropDownContainer.height(liHeight)
            };

            var placeDropDown = function(){
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
                dropDownContainer.offset(pos);
                dropDownContainer.width(width);
                $timeout(function() {
                    scope.setDropDownHeight();
                }, 1);

                token = requestAnimationFrame(placeDropDown);
            };

            scope.$watch('search', function(newVal) {
                if (!newVal) {
                    scope.search = '';
                    scope.match = [];
                    dropDownContainer.height("");
                    dropDownContainer.addClass('hidden');
                    return;
                }
                scope.match = _.filter(scope.options(), function(option) {
                    var words = newVal.split(' ');
                    return _.find(words, function(word) {
                        var formattedOption = lang.removeAccents(option.toString()).toLowerCase();
                        var formattedWord = lang.removeAccents(word).toLowerCase();
                        return formattedOption.indexOf(formattedWord) === -1
                    }) === undefined;
                });
                if (!scope.match || scope.match.length === 0) {
                    dropDownContainer.height("");
                    dropDownContainer.addClass('hidden');
                    return;
                }
                dropDownContainer.removeClass('hidden');
                cancelAnimationFrame(token);
                placeDropDown();
            });

            element.parent().on('remove', function() {
                dropDownContainer.remove();
                cancelAnimationFrame(token);
            });
            element.find('input').on('blur', function() {
                setTimeout(function() {
                    scope.search = '';
                }, 200);
                cancelAnimationFrame(token);
            });
            dropDownContainer.detach().appendTo('body');

            dropDownContainer.on('click', 'li', function(e) {
                scope.ngModel = $(this).scope().option;
                scope.search = '';
                scope.$apply('ngModel');
                scope.$eval(scope.ngChange);
                scope.$apply('ngModel');
                dropDownContainer.addClass('hidden');
                cancelAnimationFrame(token);
            });
            dropDownContainer.attr('data-opened-drop-down', true);
        }
    }
});

module.directive('dropDownButton', function() {
    return {
        restrict: 'E',
        transclude: 'true',
        controller: function($scope) {},
        template: '<div class="drop-down-button hidden"><div ng-transclude></div></div>',
        link: function(scope, element, attributes) {
            element.on('click', '.opener', function() {
                element.find('.drop-down-button').removeClass('hidden');
                $(document).one('mousedown', function(e) {
                    setTimeout(function() {
                        element.find('.drop-down-button').addClass('hidden');
                    }, 200);
                });
            });
        }
    }
});

module.directive('opts', function() {
    return {
        restrict: 'E',
        require: '^dropDownButton',
        transclude: true,
        template: '<div class="options"><ul ng-transclude></ul></div>',
        link: function(scope, element, attributes) {
            element.on('click', 'li', function() {
                element.parents('.drop-down-button').addClass('hidden');
            });
        }
    }
});

var ckeEditorFixedPositionning = function() {
    var editableElement;
    var toolbox;

    for (var instance in CKEDITOR.instances) {
        var el = $('body').find(CKEDITOR.instances[instance].element.$);
        toolbox = $('#cke_' + CKEDITOR.instances[instance].name);
        if (!el.length) {
            toolbox.remove();
            CKEDITOR.instances[instance].destroy();
            continue;
        }
        if (!el.height()) {
            continue;
        }
        $('head').find('[ckestyle=' + CKEDITOR.instances[instance].name + ']').remove();
        editableElement = $(CKEDITOR.instances[instance].element.$);
        if (editableElement.hasClass('contextual-editor')) {
            continue;
        }

        el.css({
            'margin-top': toolbox.height() + 'px'
        });
        toolbox.width(editableElement.width() + 2 + parseInt(editableElement.css('padding') || 4) * 2);
        $('<style ckestyle="' + CKEDITOR.instances[instance].name + '"></style>').text('#cke_' + CKEDITOR.instances[instance].name + '{' +
            'top:' + (editableElement.offset().top - toolbox.height()) + 'px !important;' +
            'left:' + editableElement.offset().left + 'px !important;' +
            'position: absolute !important;' +
            'display: block !important' +
            '}').appendTo('head');
    }
};

function createCKEditorInstance(editor, scope, $compile, attributes) {
    var cke = {
        instance: null
    };
    CKEDITOR.on('instanceReady', function(ck) {
        if (!(ck.editor.element.$ === editor[0])) {
            return;
        }
        cke.instance = ck;
        editor.html($compile((scope.ngModel(scope) || ''))(scope));
        scope.$apply();

        if (scope.ngModel(scope) && scope.ngModel(scope).indexOf('<img') !== -1) {
            $('img').on('load', ckeEditorFixedPositionning);
        } else {
            ckeEditorFixedPositionning();
        }
        editor.on('focus', function() {
            ckeEditorFixedPositionning();
            editor.css({
                cursor: 'text'
            });
        });
    });

    editor.on('blur', function(e) {
        if (cke.instance.editor.getData() !== scope.ngModel(scope)) {
            scope.ngModel.assign(scope, cke.instance.editor.getData());
            editor.attr('style', '');
            scope.$apply();
        }
        if (attributes.ngChange) {
            scope.$eval(attributes.ngChange);
        }
    });

    editor.on('keyup', function() {
        editor.find('[bind-html], [ng-transclude]').removeAttr('bind-html').removeAttr('ng-transclude');
    });

    return cke;
}

module.directive('textEditor', function($compile) {
    return {
        restrict: 'E',
        scope: {
            ngModel: '=',
            watchCollection: '@',
            notify: '=',
            ngChange: '&'
        },
        template: '<div contenteditable="true" style="width: 100%;" class="contextual-editor"></div>' +
            '<linker editor="contextEditor" on-change="updateContent()"></linker>' +
            '<lightbox show="editHtml" on-close="editHtml = false;">' +
            '<h2><i18n>html</i18n></h2>' +
            '<textarea class="twelve cell" ng-model="selected.html"></textarea>' +
            '<div class="row"><button ng-click="applyHtml()"><i18n>save</i18n></button></div>' +
            '</lightbox>',
        compile: function() {
            CKEDITOR_BASEPATH = '/' + infraPrefix + '/public/ckeditor/';
            if (window.CKEDITOR === undefined) {
                loader.syncLoad('ckeditor');
                CKEDITOR.plugins.basePath = '/' + infraPrefix + '/public/ckeditor/plugins/';
            }
            return function(scope, element, attributes) {
                if (!scope.display) {
                    scope.display = {};
                }
                if (!scope.display.chooseLink) {
                    scope.display.chooseLink = {};
                }
                scope.selected = {};
                var editor = element.find('[contenteditable=true]');
                var parentElement = element.parents('grid-cell');
                if (parentElement.length === 0) {
                    parentElement = editor.parent().parent();
                }
                var instance = CKEDITOR.inline(editor[0], {
                    customConfig: '/' + infraPrefix + '/public/ckeditor/text-config.js'
                });
                scope.contextEditor = instance;
                CKEDITOR.on('instanceReady', function(ck) {
                    editor.html($compile(scope.ngModel)(scope.$parent));
                });

                scope.$watch('ngModel', function(newVal) {
                    if (newVal !== instance.getData()) {
                        editor.html($compile(scope.ngModel)(scope.$parent));
                        resizeParent();
                    }
                });

                editor.on('click', function() {
                    if (parentElement.data('resizing') || parentElement.data('dragging')) {
                        return;
                    }
                    editor.focus();
                });

                parentElement.on('startDrag', function() {
                    editor.blur();
                    editor.attr('contenteditable', false);
                });

                parentElement.on('stopDrag', function() {
                    editor.attr('contenteditable', true);
                });

                var followResize = true;

                function resizeParent() {
                    editor.parent().parent().height(editor.height());
                    editor.parent().parent().trigger('stopResize');

                    if (followResize) {
                        setTimeout(resizeParent, 100);
                    }
                }

                editor.on('focus', function() {
                    followResize = true;

                    resizeParent();
                    $('.' + instance.id).width(editor.width());
                    parentElement.data('lock', true);
                    editor.css({
                        cursor: 'text'
                    });
                });

                editor.on('blur', function() {
                    followResize = false;
                    resizeParent();
                    parentElement.data('lock', false);
                    editor.css({
                        cursor: ''
                    });
                    scope.ngModel = instance.getData();
                    scope.$apply('ngModel');
                    if (scope.ngChange) {
                        scope.ngChange();
                    }
                });

                scope.applyHtml = function() {
                    editor.html(scope.selected.html);
                    scope.editHtml = false;
                };

                $('body').on('click', '#cke_' + instance.name + ' .cke_button__linker', function() {
                    $('#cke_' + instance.name).hide();
                    scope.display.chooseLink[instance.name] = true;
                    scope.$apply('display');
                });
                $('body').on('click', '#cke_' + instance.name + ' .cke_button__html', function() {
                    scope.editHtml = true;
                    scope.selected.html = editor.html();
                    scope.$apply('editHtml');
                });

                element.on('removed', function() {
                    for (var instance in CKEDITOR.instances) {
                        if (CKEDITOR.instances[instance].element.$ === editor[0]) {
                            CKEDITOR.instances[instance].destroy();
                        }
                    }
                });
            }
        }
    }
});

module.directive('htmlEditor', function($compile, $parse) {
    return {
        restrict: 'E',
        transclude: true,
        replace: true,
        scope: true,
        controller: function($scope) {
            $scope.notify = null;
        },
        template: '<div class="twelve cell block-editor">' +
            '<div contenteditable="true" loading-panel="ckeditor-image">' +
            '</div><div class="clear"></div>' +
            '<linker editor="contextEditor" on-change="updateContent()"></linker>' +
            '<lightbox show="editHtml" on-close="editHtml = false;">' +
            '<h2><i18n>html</i18n></h2>' +
            '<textarea class="twelve cell" ng-model="selected.html"></textarea>' +
            '<div class="row"><button ng-click="applyHtml()"><i18n>save</i18n></button></div>' +
            '</lightbox>' +
            '<lightbox show="selectFiles" on-close="selectFiles = false;">' +
            '<media-library ng-model="selected.files" ng-change="addContent()" multiple="true" file-format="format">' +
            '</media-library></lightbox>' +
            '<lightbox show="inputVideo" on-close="inputVideo = false">' +
            '<p style="padding-bottom: 10px;">' +
            '<i18n>info.video.embed</i18n>' +
            '</p>' +
            '<input type="test" ng-model="videoText" style="border: 0; width: 99%; margin-bottom: 10px; height: 20px; border-bottom: 1px dashed black; border-top: 1px dashed black;"/>' +
            '<div style="text-align: center"><button type="button" ng-click="addVideoLink(videoText)">Valider</button></div>' +
            '</lightbox>' +
            '</div>',
        compile: function(element, attributes, transclude) {
            CKEDITOR_BASEPATH = '/' + infraPrefix + '/public/ckeditor/';
            if (window.CKEDITOR === undefined) {
                loader.syncLoad('ckeditor');
                CKEDITOR.plugins.basePath = '/' + infraPrefix + '/public/ckeditor/plugins/';

            }
            return function(scope, element, attributes) {
                scope.ngModel = $parse(attributes.ngModel);
                if (!scope.ngModel(scope)) {
                    scope.ngModel.assign(scope, '');
                }
                scope.notify = scope.$eval(attributes.notify);
                if (!scope.display) {
                    scope.display = {};
                }

                if (!scope.display.chooseLink) {
                    scope.display.chooseLink = {};
                }

                scope.selected = {
                    files: [],
                    link: ''
                };

                var editor = element.find('[contenteditable=true]');

                if (attributes.inlineEditor) {
                    editor.addClass('contextual-editor');
                } else {
                    editor.addClass('editor-container')
                }

                var contextEditor = CKEDITOR.inline(editor[0]);
                scope.contextEditor = contextEditor;

                var cke = createCKEditorInstance(editor, scope, $compile, attributes);

                scope.$watch(function() {
                        return scope.ngModel(scope);
                    },
                    function(newValue) {
                        if (contextEditor.getData() !== newValue) {
                            editor.html($compile(newValue)(scope));
                            //weird browser bug with audio tags
                            editor.find('audio').each(function(index, item) {
                                var parent = $(item).parent();
                                $(item)
                                    .attr("src", item.src)
                                    .detach()
                                    .appendTo(parent);
                            });
                        }
                    }
                );

                element.on('removed', function() {
                    setTimeout(function() {
                        ckeEditorFixedPositionning();
                    }, 0);
                });

                element.on('dragover', function(e) {
                    e.preventDefault();
                    element.addClass('droptarget');
                });

                element.on('dragleave', function() {
                    element.removeClass('droptarget');
                });

                element.on('drop', function(e) {
                    e.preventDefault();
                    element.removeClass('droptarget');
                    var files = e.originalEvent.dataTransfer.files;
                    for (var i = 0; i < files.length; i++) {
                        (function() {
                            var name = files[i].name;
                            workspace.Document.prototype.upload(files[i], 'file-upload-' + name + '-' + i, function(doc) {
                                scope.selected.files = [doc];
                                if (name.indexOf('.mp3') !== -1 || name.indexOf('.wav') !== -1 || name.indexOf('.ogg') !== -1) {
                                    scope.format = 'audio';
                                } else {
                                    scope.format = 'img';
                                }

                                scope.addContent();
                                scope.selected.files = [];
                            });
                        }())

                    }
                });

                $('body').on('click', '#cke_' + contextEditor.name + ' .cke_button__upload', function() {
                    var resize = editor.width();
                    if (workspace.thumbnails.indexOf(resize) === -1) {
                        workspace.thumbnails += '&thumbnail=' + resize + 'x0';
                    }
                    scope.selectFiles = true;
                    scope.format = 'img';
                    scope.$apply('selectFiles');
                    scope.$apply('format');
                });

                $('body').on('click', '#cke_' + contextEditor.name + ' .cke_button__audio', function() {
                    scope.selectFiles = true;
                    scope.format = 'audio';
                    scope.$apply('selectFiles');
                    scope.$apply('format');
                });

                $('body').on('click', '#cke_' + contextEditor.name + ' .cke_button__linker', function() {
                    scope.display.chooseLink[contextEditor.name] = true;
                    scope.$apply('chooseLink');
                });

                $('body').on('click', '#cke_' + contextEditor.name + ' .cke_button__video', function() {
                    scope.inputVideo = true;
                    scope.$apply('inputVideo');
                });

                $('body').on('click', '#cke_' + contextEditor.name + ' .cke_button__html', function() {
                    scope.editHtml = true;
                    scope.selected.html = editor.html();
                    scope.$apply('editHtml');
                });

                scope.applyHtml = function() {
                    editor.html(scope.selected.html);
                    scope.editHtml = false;
                    scope.updateContent();
                };

                scope.addVideoLink = function(htmlText) {
                    //TODO : Escape dangerous html
                    contextEditor.insertHtml(htmlText);
                    scope.inputVideo = false
                };

                scope.updateContent = function() {
                    scope.ngModel.assign(scope, editor.html());
                    if (attributes.ngChange) {
                        scope.$eval(attributes.ngChange);
                    }
                };

                scope.addContent = function() {
                    scope.selected.files.forEach(function(file) {
                        if (!file._id) {
                            return;
                        }

                        if (scope.format === 'img') {
                            var imageLink = contextEditor.document.createElement('a');
                            imageLink.setAttribute('href', '/workspace/document/' + file._id + '?thumbnail=' + editor.width() + 'x0');
                            imageLink.setAttribute('target', '_blank');
                            var image = contextEditor.document.createElement('img');
                            image.setAttribute('src', '/workspace/document/' + file._id + '?thumbnail=' + editor.width() + 'x0');
                            if (attributes.resizeImages)
                                image.setAttribute('resizable', '');
                            imageLink.append(image);
                            contextEditor.insertElement(imageLink);
                        }
                        if (scope.format === 'audio') {
                            var sound = contextEditor.document.createElement('audio');

                            sound.setAttribute('src', '/workspace/document/' + file._id);
                            sound.setAttribute('controls', 'controls');

                            contextEditor.insertElement(sound);
                        }

                        scope.updateContent();
                    });
                    scope.selectFiles = false;
                };

                var replace = function() {
                    ckeEditorFixedPositionning();
                    setTimeout(replace, 500);
                };
                replace();
            }
        }
    }
});

module.directive('loadingIcon', function($compile) {
    return {
        restrict: 'E',
        link: function($scope, $element, $attributes) {
            var addImage = function() {
                if ($('#theme').length === 0)
                    return;

                if($('body').find('admin-portal').length){
                    var loadingIllustrationPath = '/public/' + skin.skin + '/img/icons/anim_loading_small.gif';
                }else{
                    var loadingIllustrationPath = '/assets/themes/' + skin.skin + '/img/icons/anim_loading_small.gif';
                }

                $('<img>')
                    .attr('src', loadingIllustrationPath)
                    .attr('class', $attributes.class)
                    .addClass('loading-icon')
                    .appendTo($element);
            };
            if ($attributes.default === 'loading') {
                addImage();
            }
            http().bind('request-started.' + $attributes.request, function(e) {
                $element.find('img').remove();
                addImage();
            });

            if ($attributes.onlyLoadingIcon === undefined) {
                http().bind('request-ended.' + $attributes.request, function(e) {
                    var loadingDonePath = '/assets/themes/' + skin.skin + '/img/icons/checkbox-checked.png';
                    $element.find('.loading-icon').remove();
                    $('<img>')
                        .attr('src', loadingDonePath)
                        .appendTo($element);
                });
            } else {
                http().bind('request-ended.' + $attributes.request, function(e) {
                    $element.find('img').remove();
                })
            }
        }
    }
})

module.directive('loadingPanel', function($compile) {
    return {
        restrict: 'A',
        link: function($scope, $element, $attributes) {
            $attributes.$observe('loadingPanel', function(val) {
                http().bind('request-started.' + $attributes.loadingPanel, function(e) {
                    var loadingIllustrationPath = '/assets/themes/' + skin.skin + '/img/illustrations/loading.gif';

                    if ($element.children('.loading-panel').length === 0) {
                        $element.append('<div class="loading-panel">' +
                            '<h1>' + lang.translate('loading') + '</h1>' +
                            '<img src="' + loadingIllustrationPath + '" />' +
                            '</div>');
                    }

                })
                http().bind('request-ended.' + $attributes.loadingPanel, function(e) {
                    $element.find('.loading-panel').remove();
                })
            });
        }
    }
});

module.directive('workflow', function($compile) {
    return {
        restrict: 'A',
        link: function(scope, element, attributes) {
            attributes.$observe('workflow', function() {
                var auth = attributes.workflow.split('.');
                var right = model.me.workflow;
                auth.forEach(function(prop) {
                    right = right[prop];
                });
                var content = element.children();
                if (!right) {
                    content.remove();
                    element.hide();
                } else {
                    element.show();
                    element.append(content);
                }
            });
        }
    }
});

module.directive('userRole', function($compile) {
    return {
        restrict: 'A',
        link: function($scope, $element, $attributes) {
            var auth = $attributes.userRole;
            if (!model.me.functions[auth]) {
                $element.hide();
            } else {
                $element.show();
            }
        }
    }
});

module.directive('tooltip', function($compile) {
    return {
        restrict: 'A',
        link: function(scope, element, attributes) {
            if(ui.breakpoints.tablette >= $(window).width()){
                return;
            }
            var tip;
            element.on('mouseover', function() {
                if (!attributes.tooltip || attributes.tooltip === 'undefined') {
                    return;
                }
                tip = $('<div />')
                    .addClass('tooltip')
                    .html($compile('<div class="arrow"></div><div class="content">' + lang.translate(attributes.tooltip) + '</div> ')(scope))
                    .appendTo('body');
                scope.$apply();

                var top = parseInt(element.offset().top + element.height());
                var left = parseInt(element.offset().left + element.width() / 2 - tip.width() / 2);
                if (top < 5) {
                    top = 5;
                }
                if (left < 5) {
                    left = 5;
                }
                tip.offset({
                    top: top,
                    left: left
                });
                tip.fadeIn();
                element.one('mouseout', function() {
                    tip.fadeOut(200, function() {
                        $(this).remove();
                    })
                });
            });

            scope.$on("$destroy", function() {
                if (tip) {
                    tip.remove();
                }

                element.off();
            });

        }
    }
});

module.directive('behaviour', function($compile) {
    return {
        restrict: 'E',
        template: '<div ng-transclude></div>',
        replace: false,
        transclude: true,
        scope: {
            resource: '='
        },
        link: function($scope, $element, $attributes) {
            console.log('This directive is deprecated. Please use "authorize" instead.');
            if (!$attributes.name) {
                throw "Behaviour name is required";
            }
            var content = $element.children('div');
            $scope.$watch('resource', function(newVal) {
                var hide = ($scope.resource instanceof Array && _.find($scope.resource, function(resource) {
                        return !resource.myRights || resource.myRights[$attributes.name] === undefined;
                    }) !== undefined) ||
                    ($scope.resource instanceof Model && (!$scope.resource.myRights || $scope.resource.myRights[$attributes.name] === undefined));

                if (hide) {
                    content.hide();
                } else {
                    content.show();
                }

            });
        }
    }
});

module.directive('resourceRight', function($compile) {
    return {
        restrict: 'EA',
        template: '<div></div>',
        replace: false,
        transclude: true,
        scope: {
            resource: '=',
            name: '@'
        },
        controller: function($scope, $transclude) {
            this.transclude = $transclude;
        },
        link: function(scope, element, attributes, controller) {
            if (attributes.name === undefined) {
                throw "Right name is required";
            }
            var content = element.children('div');
            var transcludeScope;

            var switchHide = function() {
                var hide = attributes.name && (scope.resource instanceof Array && _.find(scope.resource, function(resource) {
                        return !resource.myRights || resource.myRights[attributes.name] === undefined;
                    }) !== undefined) ||
                    (scope.resource instanceof Model && (!scope.resource.myRights || scope.resource.myRights[attributes.name] === undefined));

                if (hide) {
                    if (transcludeScope) {
                        transcludeScope.$destroy();
                        transcludeScope = null;
                    }
                    content.children().remove();
                    element.hide();
                } else {
                    if (!transcludeScope) {
                        controller.transclude(function(clone, newScope) {
                            transcludeScope = newScope;
                            content.append(clone);
                        });
                    }
                    element.show();
                }
            };

            attributes.$observe('name', switchHide);
            scope.$watchCollection('resource', switchHide);
        }
    }
});

module.directive('authorize', function($compile) {
    return {
        restrict: 'EA',
        link: function(scope, element, attributes) {
            if (attributes.name === undefined && attributes.authorize === undefined) {
                throw "Right name is required";
            }
            var content = element.children('div');

            var switchHide = function() {
                var resource = scope.$eval(attributes.resource);
                var name = attributes.name || attributes.authorize;
                var hide = name && (resource instanceof Array && _.find(resource, function(resource) {
                        return !resource.myRights || resource.myRights[name] === undefined;
                    }) !== undefined) ||
                    (resource instanceof Model && (!resource.myRights || resource.myRights[name] === undefined));

                if (hide) {
                    content.remove();
                    element.hide();
                } else {
                    element.append(content);
                    element.show();
                }
            };

            attributes.$observe('name', switchHide);
            attributes.$observe('authorize', switchHide);
            scope.$watch(function() {
                return scope.$eval(attributes.resource);
            }, switchHide);
        }
    }
});

module.directive('bottomScroll', function($compile) {
    return {
        restrict: 'A',
        link: function(scope, element, attributes) {
            var scrollElement = element;
            var getContentHeight = function() {
                return element[0].scrollHeight;
            };
            if (element.css('overflow') !== 'auto') {
                scrollElement = $(window);
                var getContentHeight = function() {
                    return $(document).height();
                };
            }
            scrollElement.scroll(function() {
                var scrollHeight = scrollElement[0].scrollY || scrollElement[0].scrollTop;
                //adding ten pixels to account for system specific behaviours
                scrollHeight += 10;

                if (getContentHeight() - scrollElement.height() < scrollHeight) {
                    scope.$eval(attributes.bottomScroll);
                    if (!scope.$$phase) {
                        scope.$apply();
                    }
                }
            })
        }
    }
});

module.directive('bottomScrollAction', function($compile) {
    return {
        restrict: 'A',
        link: function($scope, $element, $attributes) {
            $element[0].onscroll = function() {
                if (this.scrollHeight - this.scrollTop === this.clientHeight) {
                    this.scrollTop = this.scrollTop - 1
                    $scope.$eval($attributes.bottomScrollAction);
                    if (!$scope.$$phase) {
                        $scope.$apply();
                    }
                }
            }
        }
    }
});

module.directive('drawingZone', function() {
    return function($scope, $element, $attributes) {
        $element.addClass('drawing-zone');
    };
});

module.directive('resizable', function() {
    return {
        restrict: 'A',
        link: function(scope, element, attributes) {

            ui.extendElement.resizable(element, {
                lock: {
                    horizontal: element.attr('horizontal-resize-lock'),
                    vertical: element.attr('vertical-resize-lock')
                }
            });
        }
    }
});

module.directive('drawingGrid', function() {
    return function(scope, element, attributes) {
        element.addClass('drawing-grid');
        element.on('click', function(e) {
            element.parents('grid-cell').data('lock', true);

            $('body').on('click.lock', function() {
                element.parents('grid-cell').data('lock', false);
                $('body').unbind('click.lock')
            });
        });
    };
});

module.directive('gridRow', function($compile) {
    return {
        restrict: 'E',
        transclude: true,
        replace: true,
        template: '<div class="row grid-row" ng-transclude></div>',
        scope: {
            index: '='
        },
        link: function(scope, element, attributes) {
            element.addClass('row');
        }
    }
});

module.directive('gridCell', function($compile) {
    return {
        restrict: 'E',
        scope: {
            w: '=',
            h: '=',
            index: '=',
            row: '=',
            className: '=',
            onIndexChange: '&',
            onRowChange: '&'
        },
        template: '<div class="media-wrapper"><div class="media-container" ng-class="className" ng-transclude></div></div>',
        transclude: true,
        link: function(scope, element, attributes) {
            var cellSizes = ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'eleven', 'twelve'];
            scope.$watch('w', function(newVal, oldVal) {
                element.addClass(cellSizes[newVal]);
                if (newVal !== oldVal) {
                    element.removeClass(cellSizes[oldVal]);
                }
            });

            scope.$watch('h', function(newVal, oldVal) {
                if (ui.breakpoints.checkMaxWidth("tablette")) {
                    element.removeClass('height-' + cellSizes[newVal]);
                } else {
                    element.addClass('height-' + cellSizes[newVal]);
                }
                if (newVal !== oldVal) {
                    element.removeClass('height-' + cellSizes[oldVal]);
                }
            });

            $(window).on('resize', function() {
                if (ui.breakpoints.checkMaxWidth("tablette")) {
                    element.removeClass('height-' + cellSizes[scope.h]);
                } else {
                    element.addClass('height-' + cellSizes[scope.h]);
                }
            });

            scope.$watch('className', function(newVal) {
                newVal.forEach(function(cls) {
                    element.addClass(cls);
                });
            });

            element.on('editor-focus', 'editor', function() {
                element.css({
                    overflow: 'visible',
                    height: 'auto'
                });
                element.find('.media-wrapper').css({
                    position: 'relative',
                    overflow: 'visible',
                    height: 'auto'
                });
            });

            element.on('editor-blur', 'editor', function() {
                element.css({
                    overflow: '',
                    height: ''
                });
                element.find('.media-wrapper').css({
                    position: '',
                    overflow: '',
                    height: ''
                });
            });
        }
    }
});

module.directive('gridResizable', function($compile) {
    return {
        restrict: 'A',
        link: function(scope, element, attributes) {
            $('body').css({
                '-webkit-user-select': 'none',
                '-moz-user-select': 'none',
                'user-select': 'none'
            });

            var cellSizes = ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'eleven', 'twelve'];
            var resizeLimits = {};
            var parent = element.parents('.drawing-grid');

            element.addClass('grid-media');

            var lock = {};

            //cursor styles to indicate resizing possibilities
            element.on('mouseover', function(e) {
                element.on('mousemove', function(e) {
                    if (element.data('resizing') || element.data('lock')) {
                        return;
                    }

                    lock.vertical = (element.find('grid-cell, sniplet, [vertical-lock]').length > 0);

                    var mouse = {
                        x: e.pageX,
                        y: e.pageY
                    };
                    resizeLimits = {
                        horizontal: element.offset().left + element.width() + 5 > mouse.x && mouse.x > element.offset().left + element.width() - 15,
                        vertical: (element.offset().top + (element.height() + parseInt(element.css('padding-bottom'))) +
                            5 > mouse.y && mouse.y > element.offset().top + (element.height() + parseInt(element.css('padding-bottom'))) - 15) && !lock.vertical
                    };

                    var orientations = {
                        'ns': resizeLimits.vertical,
                        'ew': resizeLimits.horizontal,
                        'nwse': resizeLimits.vertical && resizeLimits.horizontal

                    };

                    var cursor = '';
                    for (var orientation in orientations) {
                        if (orientations[orientation]) {
                            cursor = orientation;
                        }
                    }

                    if (cursor) {
                        cursor = cursor + '-resize';
                    }

                    element.css({
                        cursor: cursor
                    });
                    element.children('*').css({
                        cursor: cursor
                    });
                });
                element.on('mouseout', function(e) {
                    element.unbind('mousemove');
                });
            });

            //actual resize
            element.on('mousedown.resize', function(e) {
                if (element.data('lock') === true || element.data('resizing') === true || (!resizeLimits.horizontal && !resizeLimits.vertical)) {
                    return;
                }
                var mouse = {
                    y: e.pageY,
                    x: e.pageX
                };
                var cellWidth = parseInt(element.parent().width() / 12);
                var cells = element.parent().children('grid-cell');
                var interrupt = false;
                var parentData = {
                    pos: element.parents('.grid-row').offset(),
                    size: {
                        width: element.parents('.grid-row').width(),
                        height: element.parents('.grid-row').height()
                    }
                };

                if (resizeLimits.horizontal || resizeLimits.vertical) {
                    cells.data('lock', true);
                }

                function findResizableNeighbour(cell, step) {
                    var neighbour = cell.next('grid-cell');
                    if (neighbour.length < 1) {
                        return;
                    }
                    if (neighbour.width() - step <= cellWidth) {
                        return findResizableNeighbour(neighbour, step);
                    } else {
                        return neighbour;
                    }
                }

                function parentRemainingSpace(diff) {
                    var rowWidth = element.parent().width();
                    var childrenSize = 0;
                    cells.each(function(index, cell) {
                        childrenSize += $(cell).width();
                    });
                    return rowWidth - (childrenSize + diff + 2 * cells.length);
                }

                e.preventDefault();

                $(window).unbind('mousemove.drag');
                $(window).on('mousemove.resize', function(e) {
                    mouse = {
                        y: e.pageY,
                        x: e.pageX
                    };

                    if (element.data('resizing')) {
                        return;
                    }

                    cells.trigger('startResize');
                    cells.removeClass('grid-media');

                    //this makes sure the cursor doesn't change when we move the mouse outside the element
                    $('.main').css({
                        'cursor': element.css('cursor')
                    });

                    element.unbind("click");

                    // the element height is converted in padding-bottom if vertical resize happens
                    // this is done in order to keep it compatible with the grid, which is based on padding
                    if (resizeLimits.vertical) {
                        element.css({
                            'padding-bottom': element.height() + 'px'
                        });
                        element.height(0);
                    }

                    //animation for resizing
                    var resize = function() {
                        //current element resizing
                        var newWidth = 0;
                        var newHeight = 0;
                        var p = element.offset();

                        //horizontal resizing
                        if (resizeLimits.horizontal) {
                            var distance = mouse.x - p.left;
                            if (element.offset().left + distance > parentData.pos.left + parentData.size.width) {
                                distance = (parentData.pos.left + parentData.size.width) - element.offset().left - 2;
                            }
                            newWidth = distance;
                            if (newWidth < cellWidth) {
                                newWidth = cellWidth;
                            }
                            var diff = newWidth - element.width();

                            //neighbour resizing
                            var remainingSpace = parentRemainingSpace(diff);
                            var neighbour = findResizableNeighbour(element, distance - element.width());

                            if (neighbour || remainingSpace >= 0) {
                                if (neighbour && remainingSpace <= 0) {
                                    var neighbourWidth = neighbour.width() + remainingSpace;
                                    if (neighbourWidth < cellWidth) {
                                        newWidth -= cellWidth - neighbourWidth;
                                        neighbourWidth = cellWidth;
                                    }
                                    neighbour.width(neighbourWidth);
                                }
                                element.width(newWidth);
                            }
                        }

                        //vertical resizing
                        if (resizeLimits.vertical) {
                            var distance = mouse.y - p.top;
                            newHeight = distance;
                            element.css({
                                'padding-bottom': newHeight
                            });
                        }

                        if (!interrupt) {
                            requestAnimationFrame(resize);
                        }
                    };
                    resize();
                });

                $(window).on('mouseup.resize', function() {
                    cells.trigger('stopResize');
                    interrupt = true;

                    cells.each(function(index, cell) {
                        var width = $(cell).width();
                        var height = parseInt($(cell).css('padding-bottom'));
                        if (height < cellWidth / 2) {
                            height = 0;
                        }
                        var cellWIndex = Math.round(width * 12 / parentData.size.width);
                        var cellHIndex = Math.round(height * 12 / parentData.size.width);
                        var cellScope = angular.element(cell).scope();
                        cellScope.w = cellWIndex;
                        cellScope.h = cellHIndex;
                        cellScope.$apply('w');
                        cellScope.$apply('h');
                    });

                    setTimeout(function() {
                        cells.data('resizing', false);
                        cells.data('lock', false);
                        cells.attr('style', '');
                        cells.addClass('grid-media');
                        element.find('*').css({
                            cursor: 'inherit'
                        });
                    }, 100);
                    $(window).unbind('mousemove.resize');
                    $(window).unbind('mouseup.resize');
                    $('.main').css({
                        'cursor': ''
                    })
                });
            });
        }
    }
});

module.directive('gridDraggable', function($compile) {
    return {
        restrict: 'A',
        link: function(scope, element, attributes) {
            element.attr('draggable', false);
            element.on('mousedown', function(e) {
                var parent = element.parents('.drawing-grid');
                if (element.data('lock') === true || element.find('editor').data('lock') === true || parent.first().hasClass('blur-grid')) {
                    return;
                }

                var interrupt = false;
                var mouse = {
                    y: e.clientY,
                    x: e.clientX
                };
                var cellSizes = ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'eleven', 'twelve'];
                var row = element.parent();
                var cells = row.children('grid-cell');
                var parentData = {
                    width: element.parents('.row').width(),
                    height: element.parents('.row').height(),
                    left: parent.offset().left,
                    top: parent.offset().top
                };
                var elementPos = {
                    left: element.position().left,
                    top: element.position().top,
                    width: element.width(),
                    height: element.height() + parseInt(element.css('padding-bottom'))
                };

                function rowFull(row) {
                    var cellsWidth = 0;
                    row.children('grid-cell').each(function(index, item) {
                        cellsWidth += $(item).width();
                    });

                    return cellsWidth + elementPos.width > row.width();
                }

                $(window).on('mousemove.drag', function(e) {
                    if (e.clientX === mouse.x && e.clientY === mouse.y) {
                        return;
                    }
                    if (element.data('resizing') !== true && element.data('dragging') !== true) {
                        element.trigger('startDrag');

                        var elementDistance = {
                            y: mouse.y - element.offset().top,
                            x: mouse.x - element.offset().left
                        };

                        $('body').css({
                            '-webkit-user-select': 'none',
                            '-moz-user-select': 'none',
                            'user-select': 'none'
                        });

                        element.parent().height(element.height() + parseInt(element.css('padding-bottom')));

                        element.css({
                            'position': 'absolute',
                            'z-index': '900'
                        });

                        element.next({
                            'margin-left': (element.width() - 2) + 'px'
                        });

                        element.unbind("click");
                        element.find('img, button, div').css({
                            'pointer-events': 'none'
                        });
                        element.data('dragging', true);

                        moveElement(elementDistance);
                    }
                    mouse = {
                        y: e.clientY,
                        x: e.clientX
                    };
                });

                $(window).on('mouseup.drag', function(e) {
                    element.trigger('stopDrag');
                    $('body').css({
                        '-webkit-user-select': 'initial',
                        '-moz-user-select': 'initial',
                        'user-select': 'initial'
                    });
                    interrupt = true;
                    $('body').unbind('mouseup.drag');
                    $(window).unbind('mousemove.drag');

                    if (element.data('dragging') === true) {
                        cells.removeClass('grid-media');
                        elementPos.left = element.position().left;
                        elementPos.top = element.offset().top;

                        var row = element.parent();

                        parent.find('.grid-row').each(function(index, item) {
                            if (elementPos.top + elementPos.height / 2 > $(item).offset().top && elementPos.top + elementPos.height / 2 < $(item).offset().top + $(item).prev().height()) {
                                if (!rowFull($(item))) {
                                    setTimeout(function() {
                                        scope.row = angular.element(item).scope().index;
                                        scope.$apply('row');
                                        scope.onRowChange();
                                    }, 0);
                                }
                                row = $(item);
                                cells = row.children('grid-cell');
                                return false;
                            }
                        });

                        var found = false;
                        var cellI = 0;
                        cells.each(function(index, item) {
                            if (item === element[0]) {
                                return;
                            }
                            if (parseInt($(item).css('margin-left')) > 0) {
                                if (scope.index !== cellI) {
                                    scope.index = cellI;
                                    scope.$apply('index');
                                    if (row[0] === element.parent()[0]) {
                                        scope.onIndexChange();
                                    }
                                }
                                found = true;
                                return false;
                            }
                            cellI++;
                        });

                        if (!found) {
                            var index = cells.length - 1;
                            if (scope.index !== index) {
                                scope.index = index;
                                scope.$apply('index');
                                if (row[0] === element.parent()[0]) {
                                    scope.onIndexChange();
                                }
                            }
                        }
                    }

                    setTimeout(function() {
                        parent.find('grid-cell').css({
                            'margin-left': '0'
                        });
                        setTimeout(function() {
                            cells.addClass('grid-media');
                        }, 100);

                        element.data('dragging', false);
                        element.on('click', function() {
                            scope.$parent.$eval(attributes.ngClick);
                        });
                        element.find('img, button, div').css({
                            'pointer-events': 'all'
                        });
                        element.css({
                            position: '',
                            left: 'auto',
                            top: 'auto'
                        });
                        element.parent().attr('style', ' ');

                    }, 20);
                });

                var moveElement = function(elementDistance) {
                    var parent = element.parents('.drawing-grid');
                    var newOffset = {
                        top: mouse.y - elementDistance.y,
                        left: mouse.x - elementDistance.x
                    };

                    element.offset(newOffset);

                    //preview new cells order
                    var row = element.parent();
                    parent.find('.grid-row').each(function(index, item) {
                        if (newOffset.top + elementPos.height / 2 > $(item).offset().top && newOffset.top + elementPos.height / 2 < $(item).offset().top + $(item).prev().height()) {
                            row = $(item);
                            return false;
                        }
                    });

                    parent.find('grid-cell').css({
                        'margin-left': '0'
                    });

                    cells = row.children('grid-cell');
                    if (row[0] === element.parent()[0] || !rowFull(row)) {
                        var cumulatedWidth = row.offset().left;
                        cells.each(function(index, item) {
                            if (item === element[0]) {
                                return;
                            }
                            cumulatedWidth += $(item).width();
                            if (cumulatedWidth - $(item).width() / 2 > newOffset.left) {
                                $(item).css({
                                    'margin-left': (elementPos.width - 2) + 'px'
                                });
                                return false;
                            }
                        });
                    }

                    if (!interrupt) {
                        requestAnimationFrame(function() {
                            moveElement(elementDistance);
                        });
                    }
                };
            });
        }
    }
});

module.directive('sniplet', function($parse, $timeout) {
    return {
        restrict: 'E',
        scope: true,
        controller: function($scope, $timeout) {
            $timeout(function() {
                Behaviours.loadBehaviours($scope.application, function(behaviours) {
                    var snipletControllerExpansion = behaviours.sniplets[$scope.template].controller;
                    for (var prop in snipletControllerExpansion) {
                        $scope[prop] = snipletControllerExpansion[prop];
                    }
                    if (typeof $scope.init === 'function') {
                        $scope.init();
                    }
                });
            }, 1);
        },
        template: "<div ng-include=\"'/' + application + '/public/template/behaviours/sniplet-' + template + '.html'\"></div>",
        link: function(scope, element, attributes) {
            scope.application = attributes.application;
            scope.template = attributes.template;
            scope.source = scope.$eval(attributes.source);
        }
    }
});

module.directive('snipletSource', function($parse, $timeout) {
    return {
        restrict: 'E',
        scope: true,
        template: "<div ng-include=\"'/' + application + '/public/template/behaviours/sniplet-source-' + template + '.html'\"></div>",
        controller: function($scope, $timeout) {
            $scope.setSnipletSource = function(source) {
                $scope.ngModel.assign($scope, source);
                $scope.ngChange();
            };

            $timeout(function() {
                Behaviours.loadBehaviours($scope.application, function(behaviours) {
                    var snipletControllerExpansion = behaviours.sniplets[$scope.template].controller;
                    for (var prop in snipletControllerExpansion) {
                        $scope[prop] = snipletControllerExpansion[prop];
                    }

                    if (typeof $scope.initSource === 'function') {
                        $scope.initSource();
                    }
                });
            }, 1);
        },
        link: function(scope, element, attributes) {
            scope.application = attributes.application;
            scope.template = attributes.template;
            scope.ngModel = $parse(attributes.ngModel);
            scope.ngChange = function() {
                scope.$eval(attributes.ngChange);
            }
        }
    }
});

module.directive('placedBlock', function($compile) {
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
        link: function(scope, element) {
            element.css({
                'position': 'absolute'
            });
            scope.$watch('x', function(newVal) {
                element.offset({
                    top: element.offset().top,
                    left: parseInt(newVal) + element.parent().offset().left
                });
            });

            scope.$watch('y', function(newVal) {
                element.offset({
                    left: element.offset().left,
                    top: parseInt(newVal) + element.parent().offset().top
                });
            });

            var toTop = function() {
                $(':focus').blur();
                if (scope.z === undefined) {
                    return;
                }
                element.parents('.drawing-zone').find('article[draggable]').each(function(index, item) {
                    var zIndex = $(item).css('z-index');
                    if (!scope.z) {
                        scope.z = 1;
                    }
                    if (parseInt(zIndex) && parseInt(zIndex) >= scope.z) {
                        scope.z = parseInt(zIndex) + 1;
                    }
                });
                if (scope.z) {
                    scope.$apply('z');
                }
            };

            element.on('startDrag', toTop);
            element.on('startResize', function() {
                scope.w = element.width();
                scope.$apply('w');
                scope.h = element.height();
                scope.$apply('h');
                toTop();
            });

            element.on('stopDrag', function() {
                scope.x = element.position().left;
                scope.$apply('x');
                scope.y = element.position().top;
                scope.$apply('y');
            });

            scope.$watch('z', function(newVal) {
                element.css({
                    'z-index': scope.z
                })
            });

            scope.$watch('w', function(newVal) {
                element.width(newVal);
            });
            element.on('stopResize', function() {
                scope.w = element.width();
                scope.$apply('w');
                scope.h = element.height();
                scope.$apply('h');
            });

            scope.$watch('h', function(newVal) {
                element.height(newVal);
            });
        }
    }
});

module.directive('draggable', function($compile) {
    return {
        restrict: 'A',
        link: function(scope, element, attributes) {
            if (attributes.draggable == 'false' || attributes.native !== undefined) {
                return;
            }
            ui.extendElement.draggable(element, {
                mouseUp: function() {
                    element.on('click', function() {
                        scope.$parent.$eval(attributes.ngClick);
                    });
                }
            });
        }
    }
});

module.directive('sharePanel', function($compile) {
    return {
        scope: {
            resources: '=',
            appPrefix: '='
        },
        restrict: 'E',
        templateUrl: '/' + infraPrefix + '/public/template/share-panel.html',
        link: function($scope, $element, $attributes) {
            $scope.shareTable = '/' + infraPrefix + '/public/template/share-panel-table.html';
        }
    }
});


module.directive('sortableList', function($compile) {
    return {
        restrict: 'A',
        controller: function() {},
        compile: function(element, attributes, transclude) {
            var initialHtml = element.html();
            return function(scope, element, attributes) {
                scope.updateElementsOrder = function(el) {
                    var sortables = element.find('[sortable-element]');
                    sortables.removeClass('animated');
                    sortables.each(function(index, item) {
                        if (parseInt($(item).css('margin-top')) > 0) {
                            el.detach().insertBefore(item);
                        }
                    });

                    if (el.offset().top > sortables.last().offset().top + sortables.last().height()) {
                        element.append(el.detach());
                    }

                    //get new elements order
                    sortables = element.find('[sortable-element]');
                    sortables.each(function(index, item) {
                        var itemScope = angular.element(item).scope();
                        if (index !== itemScope.ngModel) {
                            itemScope.ngModel = index;
                        }
                    });

                    sortables.attr('style', '');
                    element.html($compile(initialHtml)(scope));
                    scope.$apply();
                };
            }
        }
    }
});

module.directive('sortableElement', function($parse) {
    return {
        scope: {
            ngModel: '=',
            ngChange: '&'
        },
        require: '^sortableList',
        template: '<div ng-transclude></div>',
        transclude: true,
        link: function(scope, element, attributes) {
            var sortables;
            scope.$watch('ngModel', function(newVal, oldVal) {
                if (newVal !== oldVal && typeof scope.ngChange === 'function') {
                    scope.ngChange();
                }
            });
            ui.extendElement.draggable(element, {
                lock: {
                    horizontal: true
                },
                mouseUp: function() {
                    scope.$parent.updateElementsOrder(element);

                    element.on('click', function() {
                        scope.$parent.$eval(attributes.ngClick);
                    });

                },
                startDrag: function() {
                    sortables = element.parents('[sortable-list]').find('[sortable-element]');
                    sortables.attr('style', '');
                    setTimeout(function() {
                        sortables.addClass('animated');
                    }, 20);
                    element.css({
                        'z-index': 1000
                    });
                },
                tick: function() {
                    var moved = [];
                    sortables.each(function(index, sortable) {
                        if (element[0] === sortable) {
                            return;
                        }
                        var sortableTopDistance = $(sortable).offset().top - parseInt($(sortable).css('margin-top'));
                        if (element.offset().top + element.height() / 2 > sortableTopDistance &&
                            element.offset().top + element.height() / 2 < sortableTopDistance + $(sortable).height()) {
                            $(sortable).css({
                                'margin-top': element.height()
                            });
                            moved.push(sortable);
                        }
                        //first widget case
                        if (element.offset().top + element.height() / 2 - 2 < sortableTopDistance && index === 0) {
                            $(sortable).css({
                                'margin-top': element.height()
                            });
                            moved.push(sortable);
                        }
                    });
                    sortables.each(function(index, sortable) {
                        if (moved.indexOf(sortable) === -1) {
                            $(sortable).css({
                                'margin-top': 0 + 'px'
                            });
                        }
                    })
                }
            });
        }
    };
});

module.directive('widgets', function($compile) {
    return {
        scope: {
            list: '='
        },
        restrict: 'E',
        templateUrl: '/' + infraPrefix + '/public/template/widgets.html',
        link: function(scope, element, attributes) {
            element.on('index-changed', '.widget-container', function(e) {
                var widgetObj = angular.element(e.target).scope().widget;
                element.find('.widget-container').each(function(index, widget) {
                    if (e.target === widget) {
                        return;
                    }
                    if ($(e.target).offset().top + ($(e.target).height() / 2) > $(widget).offset().top &&
                        $(e.target).offset().top + ($(e.target).height() / 2) < $(widget).offset().top + $(widget).height()) {
                        widgetObj.setIndex(index);
                        scope.$apply('widgets');
                    }
                    //last widget case
                    if ($(e.target).offset().top > $(widget).offset().top + $(widget).height() && index === element.find('.widget-container').length - 1) {
                        widgetObj.setIndex(index);
                        scope.$apply('widgets');
                    }
                    //first widget case
                    if ($(e.target).offset().top + $(e.target).height() > $(widget).offset().top && index === 0) {
                        widgetObj.setIndex(index);
                        scope.$apply('widgets');
                    }
                });
                element.find('.widget-container').css({
                    position: 'relative',
                    top: '0px',
                    left: '0px'
                });
            });
            model.widgets.on('change', function() {
                if (element.find('.widget-container').length === 0) {
                    element
                        .parents('.widgets')
                        .next('.widgets-friend')
                        .addClass('widgets-enemy');
                    element.parents('.widgets').addClass('hidden');
                } else {
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

module.directive('progressBar', function($compile) {
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
        link: function(scope, element, attributes) {
            function updateBar() {
                var filledPercent = scope.filled * 100 / scope.max;
                element.find('.filled').width(filledPercent + '%');
                if (filledPercent < 10) {
                    element.find('.filled').addClass('small');
                } else {
                    element.find('.filled').removeClass('small');
                }
            }

            scope.$watch('filled', function(newVal) {
                updateBar();
            });

            scope.$watch('max', function(newVal) {
                updateBar();
            });
        }
    }
});

module.directive('datePicker', function($compile) {
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
        link: function(scope, element, attributes) {
            scope.$watch('ngModel', function(newVal) {
                element.val(moment(scope.ngModel).format('DD/MM/YYYY'));
                if (element.datepicker)
                    element.datepicker('setValue', moment(scope.ngModel).format('DD/MM/YYYY'));
            });

            if (scope.minDate) {
                scope.$watch('minDate', function(newVal) {
                    setNewDate();
                });
            }

            function setNewDate() {
                var minDate = scope.minDate;
                var date = element.val().split('/');
                var temp = date[0];
                date[0] = date[1];
                date[1] = temp;
                date = date.join('/');
                scope.ngModel = new Date(date);

                if (scope.ngModel < minDate) {
                    scope.ngModel = minDate;
                    element.val(moment(minDate).format('DD/MM/YYYY'));
                }

                scope.$apply('ngModel');
                scope.$parent.$eval(scope.ngChange);
                scope.$parent.$apply();
            }

            loader.asyncLoad('/' + infraPrefix + '/public/js/bootstrap-datepicker.js', function() {
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
                    .on('changeDate', function() {
                        setTimeout(setNewDate, 10);

                        $(this).datepicker('hide');
                    });
                element.datepicker('hide');
            });

            var hideFunction = function(e) {
                if (e.originalEvent && (element[0] === e.originalEvent.target || $('.datepicker').find(e.originalEvent.target).length !== 0)) {
                    return;
                }
                element.datepicker('hide');
            };
            $('body, lightbox').on('click', hideFunction);
            $('body, lightbox').on('focusin', hideFunction);

            element.on('focus', function() {
                var that = this;
                $(this).parents('form').on('submit', function() {
                    $(that).datepicker('hide');
                });
                element.datepicker('show');
            });

            element.on('change', setNewDate);

            element.on('$destroy', function() {
                element.datepicker('hide');
            });
        }
    }
});

module.directive('datePickerIcon', function($compile) {
    return {
        scope: {
            ngModel: '=',
            ngChange: '&'
        },
        replace: true,
        restrict: 'E',
        template: '<div class="date-picker-icon"> <input type="text" class="hiddendatepickerform" style="visibility: hidden; width: 0px; height: 0px; float: inherit" data-date-format="dd/mm/yyyy"/> <a ng-click="openDatepicker()"><i class="calendar"/></a> </div>',
        link: function($scope, $element, $attributes) {
            loader.asyncLoad('/' + infraPrefix + '/public/js/bootstrap-datepicker.js', function() {
                var input_element = $element.find('.hiddendatepickerform')
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
                    .on('changeDate', function(event) {
                        $scope.ngModel = event.date
                        $scope.$apply('ngModel')
                        $(this).datepicker('hide');
                        if (typeof $scope.ngChange === 'function') {
                            $scope.ngChange();
                        }
                    });

                input_element.datepicker('hide')

                $scope.openDatepicker = function() {
                    input_element.datepicker('show')
                }
            })

        }
    }
});

module.directive('filters', function() {
    return {
        restrict: 'E',
        template: '<div class="row line filters">' +
            '<div class="filters-icons">' +
            '<ul ng-transclude>' +
            '</ul></div>' +
            '</div><div class="row"></div> ',
        transclude: true,
        link: function(scope, element, attributes) {}
    }
});

module.directive('alphabetical', function($compile, $parse) {
    return {
        restrict: 'E',
        controller: function($scope) {
            $scope.letters = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '#'];
            $scope.matchingElements = {};
            $scope.matching = function(letter) {
                return function(element) {
                    return element[$scope.title][0].toUpperCase() === letter || (letter === '#' && element[$scope.title][0].toLowerCase() === element[$scope.title][0].toUpperCase());
                }
            };

            $scope.updateElements = function() {
                $scope.letters.forEach(function(letter) {
                    $scope.matchingElements[letter] = _.filter($scope.collection($scope), function(element) {
                        return element[$scope.title][0].toUpperCase() === letter || (letter === '#' && element[$scope.title][0].toLowerCase() === element[$scope.title][0].toUpperCase());
                    });
                });
            };

            if (!$scope.display) {
                $scope.display = {};
            }
            $scope.display.pickLetter;
        },
        compile: function(element, attributes) {
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
            return function(scope, element, attributes) {
                scope.title = attributes.title || 'title';
                element.removeAttr('title');
                scope.collection = $parse(collection);
                scope.$watchCollection(collection, function(newVal) {
                    scope.updateElements();
                });
                scope.updateElements();
                scope.viewLetter = function(letter) {
                    document.getElementById('alphabetical-' + letter).scrollIntoView();
                    scope.display.pickLetter = false;
                };
            }
        }
    }
});

module.directive('completeClick', function($parse) {
    return {
        compile: function(selement, attributes) {
            var fn = $parse(attributes.completeClick);
            return function(scope, element, attributes) {
                element.on('click', function(event) {
                    scope.$apply(function() {
                        fn(scope, {
                            $event: event
                        });
                    });
                });
            };
        }
    }
});



module.directive('dragItem', function() {
    return {
        restrict: 'A',
        // lier le scope au parent
        // pas de propriété scope
        link: function(scope, element, attributes) {
            var drag = scope.$eval(attributes.dragItem);
            var matchedElement = undefined;
            var firstTick = true;

            scope.$watch(function () {
                return scope.$eval(attributes.dragItem);
            }, function (newVal) {
                drag = newVal;
            });

            ui.extendElement.draggable(element, {
                mouseUp: function(e) {
                    $("[drop-item]").off("mouseover mouseout");
                    //declencher l'evenement drop
                    $('body').removeClass('dragging');
                    if (matchedElement) {
                        matchedElement.trigger("drop", [drag]);
                    }
                    scope.$apply();
                    firstTick = true;
                    element.attr('style', '');
                },
                tick: function(e, mouse) {
                    matchedElement = undefined;
                    $("[drop-item]").each(function(index, el){
                        if($(el).offset().left < mouse.x && $(el).offset().left + $(el).width() > mouse.x &&
                        $(el).offset().top - (window.scrollY || window.pageYOffset) < mouse.y && $(el).offset().top + $(el).height() - (window.scrollY || window.pageYOffset) > mouse.y)
                        {
                            $(el).addClass('drag-over');
                            matchedElement = $(el);
                        }
                        else{
                            $(el).removeClass('drag-over');
                        }
                    })

                    if (firstTick) {
                        $('[drop-item]').removeClass('drag-over');
                        element.css({
                            'pointer-events': 'none'
                        });
                        $('body').addClass('dragging');
                        scope.$apply();

                        firstTick = false;
                    }
                }

            })


        }
    }
})

module.directive('dropItem', function($parse) {
    return {
        restrict: 'A',
        link: function(scope, element, attributes) {
            var dropConditionFn = $parse(attributes.dropcondition);
            element.on("mouseover", function(event) {
                if (attributes.dropcondition === undefined || dropConditionFn(scope, {
                        $originalEvent: event.originalEvent
                    })) {
                    event.preventDefault();
                    event.stopPropagation();
                    element.addClass("droptarget")
                }
            });
            element.on("mouseout", function(event) {
                element.removeClass("droptarget")
            });
            element.on('drop', function(event, item) {
                scope.$eval(attributes.dropItem, {
                    $item: item
                });
                scope.$apply();
            })
        }
    }
})



module.directive('dragstart', function($parse) {
    return {
        restrict: 'A',
        link: function(scope, element, attributes) {
            var dragStartFn = $parse(attributes.dragstart);
            var ngModel = $parse(attributes.ngModel);
            if (attributes.dragcondition !== undefined && scope.$eval(attributes.dragcondition) === false) {
                element.attr("draggable", "false");
                return
            }

            element.attr("draggable", "true");
            element.attr("native", "");

            element.on("dragstart", function(event) {
                if (ngModel && ngModel(scope)) {
                    try {
                        event.originalEvent.dataTransfer.setData('application/json', JSON.stringify(ngModel(scope)));
                    } catch (e) {
                        event.originalEvent.dataTransfer.setData('Text', JSON.stringify(ngModel(scope)));
                    }
                }
                if (attributes.dragstart === '') {

                    return;
                }
                dragStartFn(scope, {
                    $originalEvent: event.originalEvent
                });
            });

            element.on('$destroy', function() {
                element.off()
            })
        }
    }
})

module.directive('dragdrop', function($parse) {
    return {
        restrict: 'A',
        link: function(scope, element, attributes) {
            var dropFn = $parse(attributes.dragdrop);
            var dropConditionFn = $parse(attributes.dropcondition);
            element.on("dragover", function(event) {
                if (attributes.dropcondition === undefined || dropConditionFn(scope, {
                        $originalEvent: event.originalEvent
                    })) {
                    event.preventDefault();
                    event.stopPropagation();
                    element.addClass("droptarget")
                }
            });

            element.on("dragleave", function(event) {
                event.preventDefault();
                event.stopPropagation();
                element.removeClass("droptarget");
            });

            element.on("drop", function(event) {
                event.originalEvent.preventDefault();
                element.removeClass("droptarget");
                var item;
                try {
                    item = JSON.parse(event.originalEvent.dataTransfer.getData('application/json'));
                } catch (e) {
                    item = JSON.parse(event.originalEvent.dataTransfer.getData('Text'));
                }
                dropFn(scope, {
                    $originalEvent: event.originalEvent,
                    $item: item
                });
            });

            element.on('$destroy', function() {
                element.off()
            });
        }
    }
});

module.directive('dropFiles', function($parse) {
    return {
        link: function(scope, element, attributes) {
            var ngModel = $parse(attributes.dropFiles);
            element.on('dragover', function(e) {
                e.preventDefault();
                scope.$eval(attributes.onDrag);
                element.addClass('droptarget');
            });

            element.on('dragleave', function(e) {
                e.preventDefault();
                scope.$eval(attributes.onLeave);
                element.removeClass('droptarget');
            });

            element.on('drop', function(e) {
                e.preventDefault();
                ngModel.assign(scope, e.originalEvent.dataTransfer.files);
                scope.$eval(attributes.onDrop);
                scope.$apply();
                element.removeClass('droptarget');
            });
        }
    }
});

module.directive('attachments', function($parse) {
    return {
        scope: true,
        restrict: 'E',
        templateUrl: '/' + infraPrefix + '/public/template/attachments.html',
        controller: function($scope) {
            $scope.linker = {
                resource: {}
            }
            $scope.attachments = {
                me: model.me,
                display: {
                    search: {
                        text: '',
                        application: {}
                    },
                    pickFile: false
                },
                addAttachment: function(resource) {
                    resource.provider = $scope.attachments.display.search.application;
                    if ($scope.ngModel($scope) instanceof Array) {
                        $scope.ngModel($scope).push(resource);
                    } else {
                        $scope.ngModel.assign($scope, [resource]);
                    }
                    $scope.attachments.display.pickFile = false;
                },
                removeAttachment: function(resource) {
                    $scope.ngModel.assign($scope, _.reject($scope.ngModel($scope), function(item) {
                        return item === resource;
                    }));
                },
                attachmentsList: function() {
                    $scope.list = $scope.ngModel($scope);
                    return $scope.list;
                }
            };
        },
        link: function(scope, element, attributes) {
            scope.ngModel = $parse(attributes.ngModel);
            scope.attachments.onChange = function() {
                scope.$eval(attributes.onChange);
            };
            scope.apps = scope.$eval(attributes.apps);
            scope.$watch(
                function() {
                    return scope.attachments.display.pickFile
                },
                function(newVal) {
                    if (newVal) {
                        scope.attachments.loadApplicationResources(function() {
                            scope.attachments.searchApplication();
                            scope.attachments.display.search.text = ' ';
                            scope.$apply('attachments');
                        });
                    } else {
                        scope.attachments.display.search.text = '';
                    }
                },
                true
            );

            http().get('/resources-applications').done(function(apps) {
                scope.attachments.apps = _.filter(model.me.apps, function(app) {
                    return _.find(apps, function(match) {
                        return app.address.indexOf(match) !== -1 && app.icon
                    }) && _.find(scope.apps, function(match) {
                        return app.address.indexOf(match) !== -1
                    });
                });

                scope.attachments.display.search.application = scope.attachments.apps[0];
                scope.attachments.loadApplicationResources(function() {});

                scope.$apply('attachments');
            });

            scope.attachments.loadApplicationResources = function(cb) {
                if (!cb) {
                    cb = function() {
                        scope.attachments.display.searchApplication();
                        scope.$apply('attachments');
                    };
                }

                var split = scope.attachments.display.search.application.address.split('/');
                scope.prefix = split[split.length - 1];

                Behaviours.loadBehaviours(scope.prefix, function(appBehaviour) {
                    appBehaviour.loadResources(cb);
                    scope.attachments.addResource = appBehaviour.create;
                });
            };

            scope.attachments.searchApplication = function() {
                var split = scope.attachments.display.search.application.address.split('/');
                scope.prefix = split[split.length - 1];

                Behaviours.loadBehaviours(scope.prefix, function(appBehaviour) {
                    scope.attachments.resources = _.filter(appBehaviour.resources, function(resource) {
                        return scope.attachments.display.search.text !== '' && (lang.removeAccents(resource.title.toLowerCase()).indexOf(lang.removeAccents(scope.attachments.display.search.text).toLowerCase()) !== -1 ||
                            resource._id === scope.attachments.display.search.text);
                    });
                });
            };

            scope.linker.createResource = function() {
                var split = scope.attachments.display.search.application.address.split('/');
                var prefix = split[split.length - 1];

                Behaviours.loadBehaviours(prefix, function(appBehaviour) {
                    appBehaviour.create(scope.linker.resource, function(resources, newResource) {
                        if (!(scope.ngModel(scope) instanceof Array)) {
                            scope.ngModel.assign(scope, []);
                        }
                        scope.attachments.display.pickFile = false;
                        var resource = _.find(resources, function(resource) {
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

module.directive('wizard', function() {
    return {
        restrict: 'E',
        templateUrl: '/infra/public/template/wizard.html',
        scope: {
            onCancel: '&',
            onFinish: '&',
            finishCondition: '&'
        },
        transclude: true,
        compile: function(element, attributes, transclude) {
            return function(scope, element, attributes) {
                element.find('div.steps').hide();
                scope.currentStep = 0;

                var currentStepContent;
                var steps = element.find('div.steps step');
                scope.nbSteps = steps.length;

                var nav = element.find('nav.steps');
                nav.append('<ul></ul>');
                steps.each(function(index, item) {
                    nav.children('ul').append('<li>' + $(item).find('h1').html() + '</li>');
                });

                function displayCurrentStep() {
                    transclude(scope.$parent.$new(), function(content) {
                        currentStepContent = _.filter(content, function(el) {
                            return el.tagName === 'STEP';
                        })[scope.currentStep];
                        currentStepContent = $(currentStepContent);
                        element
                            .find('.current-step .step-content')
                            .html('')
                            .append(currentStepContent);
                    });

                    $('nav.steps li').removeClass('active');
                    $(element.find('nav.steps li')[scope.currentStep]).addClass('active');
                }

                scope.nextCondition = function() {
                    var stepScope = angular.element(currentStepContent[0]).scope();
                    if (typeof stepScope.nextCondition() === 'undefined')
                        return true;
                    return stepScope.nextCondition();
                };

                scope.nextStep = function() {
                    if (!scope.nextCondition())
                        return

                    var stepScope = angular.element(currentStepContent[0]).scope();
                    stepScope.onNext();
                    scope.currentStep++;
                    displayCurrentStep();
                };

                scope.previousStep = function() {
                    var stepScope = angular.element(currentStepContent[0]).scope();
                    stepScope.onPrevious();
                    scope.currentStep--;
                    displayCurrentStep();
                };

                scope.cancel = function() {
                    scope.currentStep = 0;
                    displayCurrentStep();
                    scope.onCancel();
                };

                scope.checkFinishCondition = function() {
                    if (typeof scope.finishCondition() === 'undefined')
                        return true;
                    return scope.finishCondition();
                };

                scope.finish = function() {
                    if (!scope.checkFinishCondition())
                        return

                    scope.currentStep = 0;
                    displayCurrentStep();
                    scope.onFinish();
                };

                displayCurrentStep();
            }
        }
    }
});

module.directive('step', function() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            onNext: '&',
            onPrevious: '&',
            nextCondition: '&'
        },
        template: '<div class="step" ng-transclude></div>'
    }
});

module.directive('carousel', function() {
    return {
        scope: {
            items: '='
        },
        restrict: 'E',
        templateUrl: '/infra/public/template/carousel.html',
        link: function(scope, element, attributes) {
            var interrupt = 0;
            if (attributes.transition) {
                element.addClass(scope.$parent.$eval(attributes.transition));
            }
            if (attributes.buttons) {
                element.addClass(scope.$parent.$eval(attributes.buttons));
            }

            scope.current = {
                image: scope.items[0],
                index: 0
            };
            scope.images = _.filter(scope.items, function(item) {
                return item.icon !== undefined;
            });
            scope.$watchCollection('items', function(newVal) {
                scope.images = _.filter(scope.items, function(item) {
                    return item.icon !== undefined;
                });
                scope.current = {
                    image: scope.items[0],
                    index: 0
                };
            });
            scope.openCurrentImage = function() {
                window.location = scope.current.image.link;
            };
            scope.openSelectImage = function(item, index) {
                if (scope.current.image === item) {
                    scope.openCurrentImage();
                } else {
                    scope.current.image = item;
                    scope.current.index = index;
                }
                cancelAnimationFrame(animrequest);
                imageHeight();
                interrupt--;
                setTimeout(infiniteRun, 5000);
            };
            scope.getPilePosition = function(index) {
                if (index < scope.current.index) {
                    return 100 + index;
                } else {
                    return 100 - index;
                }
            };
            var animrequest;
            var imageHeight = function() {
                element.find('.current img').height(element.find('.current .image-container').height());
                animrequest = requestAnimationFrame(imageHeight);
            }
            var infiniteRun = function() {
                cancelAnimationFrame(animrequest);
                if (interrupt < 0) {
                    interrupt++;
                    return;
                }
                scope.current.index++;
                if (scope.current.index === scope.items.length) {
                    scope.current.index = 0;
                }
                scope.current.image = scope.items[scope.current.index];
                scope.$apply('current');

                imageHeight();

                setTimeout(infiniteRun, 4000);
            };
            infiniteRun();
        }
    }
});

module.directive('pdfViewer', function() {
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
        link: function(scope, element, attributes) {
            var pdf;
            scope.pageIndex = 1;
            scope.nextPage = function() {
                if (scope.pageIndex < scope.numPages) {
                    scope.pageIndex++;
                    scope.openPage();
                }
            };
            scope.previousPage = function() {
                if (scope.pageIndex > 0) {
                    scope.pageIndex--;
                    scope.openPage();
                }
            };
            scope.openPage = function() {
                var pageNumber = parseInt(scope.pageIndex);
                if (!pageNumber) {
                    return;
                }
                if (pageNumber < 1) {
                    pageNumber = 1;
                }
                if (pageNumber > scope.numPages) {
                    pageNumber = scope.numPages;
                }
                pdf.getPage(pageNumber).then(function(page) {
                    var viewport;
                    if (!$(canvas).hasClass('fullscreen')) {
                        viewport = page.getViewport(1);
                        var scale = element.width() / viewport.width;
                        viewport = page.getViewport(scale);
                    } else {
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

            window.PDFJS = {
                workerSrc: '/infra/public/js/viewers/pdf.js/pdf.worker.js'
            };
            var canvas = document.createElement('canvas');
            $(canvas).addClass('render');
            element.append(canvas);
            loader.openFile({
                url: '/infra/public/js/viewers/pdf.js/pdf.js',
                success: function() {
                    PDFJS
                        .getDocument(attributes.ngSrc)
                        .then(function(file) {
                            pdf = file;
                            scope.numPages = pdf.pdfInfo.numPages;
                            scope.$apply('numPages');
                            scope.openPage();
                        });
                }
            });
        }
    }
});

module.directive('fileViewer', function() {
    return {
        restrict: 'E',
        scope: {
            ngModel: '='
        },
        templateUrl: '/infra/public/template/file-viewer.html',
        link: function(scope, element, attributes) {
            scope.contentType = scope.ngModel.metadata.contentType;
            scope.isFullscreen = false;

            scope.download = function() {
                window.location.href = scope.ngModel.link;
            };
            var renderElement;
            var renderParent;
            scope.fullscreen = function(allow) {
                scope.isFullscreen = allow;
                if (allow) {
                    var container = $('<div class="fullscreen-viewer"></div>');
                    container.hide();
                    container.on('click', function(e) {
                        if (!$(e.target).hasClass('render')) {
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
                    if (typeof scope.render === 'function') {
                        scope.render();
                    }
                } else {
                    renderElement.removeClass('fullscreen').appendTo(renderParent);
                    element.children('.embedded-viewer').removeClass('fullscreen');
                    var fullscreenViewer = $('body').find('.fullscreen-viewer');
                    fullscreenViewer.fadeOut(400, function() {
                        fullscreenViewer.remove();
                    });

                    if (typeof scope.render === 'function') {
                        scope.render();
                    }
                }
            }
        }
    }
});

module.directive('ngTouchstart', function() {
    return {
        restrict: 'A',
        scope: true,
        link: function(scope, element, attributes) {
            element.on('touchstart', function() {
                scope.$eval(attributes['ngTouchstart']);
            });

            if (attributes['ngTouchend']) {
                $('body').on('touchend', function() {
                    scope.$eval(attributes['ngTouchend']);
                });
            }
        }
    }
})

module.directive('inputPassword', function() {
    return {
        restrict: 'E',
        replace: false,
        template: '<input type="password"/>' +
            '<button type="button" \
				ng-mousedown="show(true)" \
				ng-touchstart="show(true)" \
				ng-touchend="show(false)" \
				ng-mouseup="show(false)" \
				ng-mouseleave="show(false)"></button>',
        scope: true,
        compile: function(element, attributes) {
            element.addClass('toggleable-password');
            var passwordInput = element.children('input[type=password]');
            for (var prop in attributes.$attr) {
                passwordInput.attr(attributes.$attr[prop], attributes[prop]);
                element.removeAttr(attributes.$attr[prop]);
            }
            return function(scope) {
                scope.show = function(bool) {
                    passwordInput[0].type = bool ? "text" : "password"
                }
            };
        }
    }
});

module.directive('sidePanel', function() {
    return {
        restrict: 'E',
        transclude: true,
        template: '<div class="opener"></div>' +
            '<div class="toggle">' +
            '<div class="content" ng-transclude></div>' +
            '</div>',
        link: function(scope, element, attributes) {
            element.addClass('hidden');
            element.children('.opener').on('click', function(e) {
                if (!element.hasClass('hidden')) {
                    return;
                }
                element.removeClass('hidden');
                setTimeout(function() {
                    $('body').on('click.switch-side-panel', function(e) {
                        if (!(element.children('.toggle').find(e.originalEvent.target).length)) {
                            element.addClass('hidden');
                            $('body').off('click.switch-side-panel');
                        }
                    });
                }, 0);
            });
        }
    };
});

module.directive('plus', function($compile) {
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
        link: function(scope, element, attributes) {
            element.children('.toggle-buttons').addClass('hide');
            element.children('.opener').addClass('plus');
            element.children('.opener').on('click', function(e) {
                if (!element.children('.toggle-buttons').hasClass('hide')) {
                    return;
                }
                element.children('.toggle-buttons').removeClass('hide');
                element.children('.opener').removeClass('plus').addClass('minus');
                setTimeout(function() {
                    $('body').on('click.switch-plus-buttons', function(e) {
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

module.directive('help', function() {
    var helpText;
    return {
        restrict: 'E',
        scope: {},
        template: '<i class="help"></i>' +
            '<lightbox show="display.read" on-close="display.read = false"><div></div></lightbox>',
        link: function(scope, element) {
            scope.display = {};
            scope.helpPath = '/help/application/' + appPrefix + '/';
            if (appPrefix === '.' && window.location.pathname !== '/adapter') {
                scope.helpPath = '/help/application/portal/';
            } else if (window.location.pathname === '/adapter') {
                scope.helpPath = '/help/application/' + window.location.search.split('eliot=')[1].split('&')[0] + '/'
            }

            var helpContent;

            var setHtml = function(content) {
                helpContent = $('<div>' + content + '</div>');
                helpContent.find('img').each(function(index, item) {
                    $(item).attr('src', scope.helpPath + $(item).attr('src'));
                });
                helpContent.find('script').remove();
                element.find('div.content').html(helpContent.html());
                element.find('a').on('click', function(e) {
                    element.find('.app-content-section').slideUp();
                    $('#' + $(e.target).attr('href').split('#')[1]).slideDown();
                });
                element.find('a').first().click();
                scope.display.read = true;
                scope.$apply('display');
            };

            element.children('i.help').on('click', function() {
                if (helpText) {
                    setHtml(helpText);
                } else {
                    http().get(scope.helpPath)
                        .done(function(content) {
                            helpText = content;
                            setHtml(helpText);
                        })
                        .e404(function() {
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

module.directive('floatingNavigation', function() {
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
        link: function(scope, element, attributes) {

            var initialPosition;
            scope.step = 0;
            setTimeout(function() {
                initialPosition = element.offset();
                element.height($(window).height() - parseInt(element.css('margin-bottom')) - 100);
                scope.stepsLength = parseInt(element.find('.content')[0].scrollHeight / element.height());
                scope.$apply();
            }, 800);
            element.find('.arrow.next').on('click', function() {
                scope.step++;
                scope.$apply();
                element.find('.content').animate({
                    scrollTop: element.height() * scope.step
                }, 450);
            });
            element.find('.arrow.previous').on('click', function() {
                scope.step--;
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
        templateUrl: '/' + infraPrefix + '/public/template/multi-combo.html',
        controller: function($scope, $filter, $timeout) {
            /* Search input */
            $scope.search = {
                input: '',
                reset: function() {
                    this.input = ""
                }
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
                return
            }

            /* Max n° of elements selected limit */
            scope.maxSelected = parseInt(scope.maxSelected)
            if (!isNaN(scope.maxSelected) && scope.maxSelected < 1) {
                throw '[<multi-combo> directive] Error: max-selected must be an integer greater than 0.'
                return
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

module.directive('slide', function() {
    return {
        restrict: 'A',
        scope: false,
        link: function(scope, element, attributes) {
            scope.$watch(
                function() {
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

            if (!scope.$eval(attributes.slide)) {
                element.hide();
            }
        }
    }
});

module.directive('sideNav', function() {
    return {
        restrict: 'AE',
        link: function(scope, element, attributes) {
            $('.mobile-nav-opener').addClass('visible');
            var maxWidth = ui.breakpoints.tablette;
            var target = attributes.targetElement || '.navbar';

            element.addClass('side-nav');
            $('body').addClass('transition');

            var opener = $('.mobile-nav-opener');
            opener.on('click', function() {
                if (!element.hasClass('slide')) {
                    element.addClass('slide');
                    $('body').addClass('point-out');
                } else {
                    element.removeClass('slide');
                    $('body').removeClass('point-out');
                }

            });

            $('body').on('click', function(e) {
                if (element[0] === e.target || element.find(e.target).length || $('.mobile-nav-opener')[0] === e.target) {
                    return;
                }
                element.removeClass('slide');
                $('body').removeClass('point-out');

            })

            if (attributes.maxWidth) {
                maxWidth = parseInt(attributes.maxWidth);
            }

            function addRemoveEvents() {
                if (ui.breakpoints.checkMaxWidth(maxWidth)) {

                    element.height($(window).height());
                    var body = $('body');

                    if ($('.mobile-nav-opener').hasClass('visible')) {
                        body.find('.application-title').addClass('move-right');
                    }

                    ui.extendElement.touchEvents(body, {
                         exclude: ['longclick'],
                        allowDefault: true
                    });

                    ui.extendElement.touchEvents(element, {
                        exclude: ['longclick']
                    });

                    body.on('swipe-right', function() {
                        element.addClass('slide');
                    });
                    element.on('swipe-left', function() {
                        element.removeClass('slide');
                        $('body').removeClass('point-out');
                    });
                } else {
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

module.directive('appTitle', function($compile) {
    return {
        restrict: 'E',
        link: function(scope, element, attributes) {
            element.addClass('zero-mobile');
            element.find('h1').addClass('application-title');

            function setHeader() {
                var header = $('app-title').html();
                var mobileheader = $('header.main .application-title');

                if (ui.breakpoints.checkMaxWidth("tablette")) {
                    if (!mobileheader.length)
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

module.directive('microbox', function($compile) {
    return {
        restrict: 'E',
        compile: function(element, attributes, transclude) {
            var content = element.html();

            return function(scope, element, attributes) {
                var microtitle = lang.translate(attributes.microtitle);
                var closeBox = lang.translate(attributes.close);
                element.addClass('zero-mobile');

                function setBox(apply) {
                    if (ui.breakpoints.checkMaxWidth("tablette")) {

                        if (!$('.microbox-wrapper').length) {
                            //creer la box
                            $('body').append('<div class="microbox-wrapper zero">' +
                                '<div class="microbox-content">' +
                                '<i class="close-2x"></i>' +
                                '<div class="microbox-material"></div>' +
                                '<button class="microbox-close">' + closeBox + '</button>' +
                                '</div></div>');

                            $('.microbox-material').html($compile(content)(scope));
                            element.after('<button class="microbox">' + microtitle + '</button>');

                            $('button.microbox').on('click', function() {
                                if ($('.microbox-wrapper').hasClass('zero')) {
                                    $('.microbox-wrapper').removeClass('zero');
                                }
                            });

                            $('button.microbox-close, .microbox-content i.close-2x').on('click', function() {
                                if (!$('.microbox-wrapper').hasClass('zero')) {
                                    $('.microbox-wrapper').addClass('zero');
                                }
                            });

                            if (apply) {
                                scope.$apply();
                            }
                        }
                    } else {
                        $('.microbox-wrapper').remove();
                        $('button.microbox').remove();
                    }
                }

                setBox();
                $(window).on('resize', function() {
                    setBox(true)
                });

                scope.$on("$destroy", function() {
                    $('body').find('button.microbox').remove();
                    $('body').find('.microbox-content').remove();
                });
            }
        }
    }
});

module.directive('subtitle', function() {
    return {
        restrict: 'A',
        scope: false,
        link: function(scope, element, attributes) {
            $('section.main').addClass('subtitle-push');

            scope.$on("$destroy", function() {
                $('section.main').removeClass('subtitle-push');
            });
        }
    }
});

module.directive('whereami', function() {
    return {
        restrict: 'A',
        scope: false,
        link: function(scope, element, attributes) {
            element.addClass('whereami');
            var current = $('nav.side-nav a.selected').text();
            $('body').on('whereami.update', function() {
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

module.directive('checkTool', function() {
    return {
        restrict: 'E',
        scope: {
            ngModel: '=',
            ngClick: '&',
            ngChange: '&',
        },
        template: '<div class="check-tool"><i class="check-status"></i></div>',
        link: function(scope, element, attributes) {

            element.on('click', function() {
                scope.ngModel = !scope.ngModel;
                //scope.$apply('ngModel');
                checkToolDelay(scope)

                if (scope.ngModel) {
                    element.addClass('selected')
                } else {
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

            $('body').on('click', function(e) {
                if ($(e.target).parents('.check-tool, .toggle, .lightbox').length === 0 && e.target.nodeName !== "CHECK-TOOL" && $('body').find(e.target).length !== 0) {
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


module.directive('explorer', function() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            ngModel: '=',
            ngClick: '&',
            ngChange: '&',
            onOpen: '&',
        },
        template: '<div class="explorer" ng-transclude></div>',
        link: function(scope, element, attributes) {

            function select() {
                scope.ngModel = !scope.ngModel;
                if (scope.ngModel) {
                    element.addClass('selected')
                } else {
                    element.removeClass('selected')
                }
                if (scope.ngClick) {
                    scope.ngClick();
                }
                if (scope.ngChange) {
                    scope.ngChange();
                }
                scope.$apply('ngModel');
            }

            $('body').on('click', function(e) {
                if ($(e.target).parents('explorer, .toggle, .lightbox').length === 0 && e.target.nodeName !== "EXPLORER") {
                    scope.ngModel = false;
                    element.removeClass('selected');
                    scope.$apply('ngModel');
                }
            })

            function setGest(apply) {

                if (ui.breakpoints.checkMaxWidth("tablette")) {

                    element.off('click dblclick')

                    //all events default :
                    ui.extendElement.touchEvents(element);
                    //new version example
                    // ui.extendElement.touchEvents(element, {
                    //     include: ['longclick', 'doubletap']
                    // });
                    // ui.extendElement.touchEvents(element, {
                    //     exclude: ['longclick', 'doubletap']
                    // });
                    // ui.extendElement.touchEvents(element, {
                    //     include: ['truc', 'doubletap'],
                    //     exclude: ['longclick']
                    // });

                    element.on('contextmenu', function(event) {
                        event.preventDefault()
                    })

                    element.on('click', function(e, position) {
                        select();
                        scope.$apply('ngModel');
                    })

                    element.on('doubletap dblclick', function() {
                        scope.ngModel = false;
                        scope.onOpen();
                        scope.$apply('ngModel');
                    });

                    // element.on('longclick', function(e, position) {
                    //     select();
                    //     scope.$apply();
                    // })

                    // element.on('click', function() {
                    //     scope.ngModel = false;
                    //     scope.onOpen();
                    //     scope.$apply();
                    // });

                } else {
                    element.off('click dblclick doubletap contextmenu')

                    element.on('click', function() {
                        select();
                        scope.$apply('ngModel');
                    });
                    element.on('dblclick', function() {
                        scope.onOpen();
                        scope.ngModel = false;
                        scope.$apply('ngModel');

                    })
                }
            }
            setGest();
            $(window).on('resize', function() {
                setGest(true)
            });

        }
    }
});

module.directive('embedder', function($compile){
    return {
        restrict: 'E',
        scope: {
            ngModel: '=',
            onChange: '&',
            show: '='
        },
        templateUrl: '/' + infraPrefix + '/public/template/embedder.html',
        link: function(scope, element, attributes){
            scope.display = {
                provider: {
                    name: 'none'
                }
            };

            element.on('focus', 'textarea', function (e) {
                $(e.target).next().addClass('focus');
                $(e.target).next().addClass('move');
                $(e.target).prev().addClass('focus');
            });

            element.on('blur', 'textarea', function (e) {
                if (!$(e.target).val()) {
                    $(e.target).next().removeClass('move');
                }
                $(e.target).next().removeClass('focus');
                $(e.target).prev().removeClass('focus');
            });

            scope.providers = [];

            scope.$watch('show', function(){
                scope.unselectProvider();
            });

            http().get('/infra/embed/default').done(function (providers) {
                providers.forEach(function (provider) {
                    scope.providers.push(provider);
                });
                scope.providers.sort(function (p1, p2) {
                    return p1.name > p2.name
                });
            });

            http().get('/infra/embed/custom').done(function (providers) {
                providers.forEach(function (provider) {
                    provider.name = provider.name.toLowerCase().replace(/\ |\:|\?|#|%|\$|£|\^|\*|€|°|\(|\)|\[|\]|§|'|"|&|ç|ù|`|=|\+|<|@/g, '')
                    scope.providers.push(provider);
                });
                scope.providers.sort(function (p1, p2) {
                    return p1.name > p2.name
                });
            });

            scope.applyHtml = function (template) {
                scope.show = false;
                scope.ngModel = scope.display.htmlCode;
                scope.$apply();
                scope.onChange();
                scope.unselectProvider();
            };

            scope.cancel = function () {
                scope.show = false;
                scope.unselectProvider();
            };

            scope.unselectProvider = function () {
                var preview = element.find('.' + scope.display.provider.name + ' .preview');
                preview.html('');
                scope.display.provider = { name: 'none' };
                scope.display.url = '';
                scope.display.htmlCode = '';
                scope.display.invalidPath = false;
            };

            scope.$watch(
                function(){
                    return scope.display.htmlCode;
                },
                function(newVal){
                    setTimeout(function () {
                        scope.updatePreview();
                    }, 20);
                }
            );

            scope.$watch(
                function () {
                    return scope.display.url;
                },
                function (newVal) {
                    setTimeout(function () {
                        scope.updatePreview();
                    }, 20);
                }
            );

            scope.updatePreview = function () {
                if(scope.display.provider.name === 'other'){
                    var preview = element.find('.' + scope.display.provider.name + ' .preview');
                    preview.html(
                        scope.display.htmlCode
                    );
                    return;
                }
                if(!scope.display.url){
                    return;
                }
                scope.display.invalidPath = false;
                var agnosticUrl = scope.display.url.split('//')[1];
                var firstPartUrl = agnosticUrl.split('/')[0].split('?')[0];
                agnosticUrl = agnosticUrl.replace(firstPartUrl, firstPartUrl.toLowerCase());
                var matchParams = new RegExp('\{[a-zA-Z0-9_.]+\}', ["g"]);
                var params = scope.display.provider.url.match(matchParams);

                var computedEmbed = scope.display.provider.embed;

                params.splice(1, params.length);
                params.forEach(function (param) {
                    var paramBefore = scope.display.provider.url.split(param)[0];
                    var additionnalSplit = paramBefore.split('}')
                    if (additionnalSplit.length > 1) {
                        paramBefore = additionnalSplit[additionnalSplit.length - 1];
                    }
                    var paramAfter = scope.display.provider.url.split(param)[1].split('{')[0];
                    var paramValue = agnosticUrl.split(paramBefore)[1];
                    if (!paramValue) {
                        scope.display.invalidPath = true;
                    }
                    if(paramAfter){
                        paramValue = paramValue.split(paramAfter)[0];
                    }

                    var replace = new RegExp('\\' + param.replace(/}/, '\\}'), 'g');
                    scope.display.htmlCode = computedEmbed.replace(replace, paramValue);
                });

                var preview = element.find('.' + scope.display.provider.name + ' .preview');
                preview.html(
                    scope.display.htmlCode
                );
            };
        }
    }
});

module.directive('assistant', function(){
    return {
        restrict: 'E',
        templateUrl: '/infra/public/template/assistant.html',
        link: function(scope, element, attributes){

            if($(window).width() <= ui.breakpoints.fatMobile){
                return;
            }

            scope.show = { assistant: false };

            var token;
            var hidePulsars = function(){
                $('.pulsar-button').addClass('hidden');
                token = setTimeout(hidePulsars, 50);
                // cache TOUS les pulsars
            }

            quickstart.load(function(){
                if(quickstart.state.assistant === -1 || quickstart.mySteps.length === 0){
                    return;
                }

                hidePulsars();
                scope.show.assistant = true;
                scope.currentStep = quickstart.assistantIndex;
                scope.steps = quickstart.mySteps;
                scope.$apply();
            });

            scope.goTo = function(step){
                quickstart.goTo(step.index);
                scope.currentStep = quickstart.assistantIndex;
            };

            scope.next = function(){
                quickstart.nextAssistantStep();
                scope.currentStep = quickstart.assistantIndex;
                if(!quickstart.assistantIndex){
                    scope.show.assistant = false;
                    $('.pulsar-button').removeClass('hidden');
                    clearTimeout(token);
                }
            };

            scope.previous = function(){
                quickstart.previousAssistantStep();
                scope.currentStep = quickstart.assistantIndex;
            };

            scope.seeLater = function(){
                scope.show.assistant = false;
                $('.pulsar-button').removeClass('hidden');
                clearTimeout(token);
            };

            scope.closeAssistant = function(){
                scope.show.assistant = false;
                $('.pulsar-button').removeClass('hidden');
                clearTimeout(token);
                quickstart.state.assistant = -1;
                quickstart.save();
            };
        }
    }
});

module.directive('pulsar', function($compile){
    return {
        restrict: 'A',
        scope: true,
        link: function(scope, element, attributes){

            if($(window).width() <= ui.breakpoints.tablette || !model.me.hasWorkflow('org.entcore.portal.controllers.PortalController|quickstart')){
                return;
            }
            //if wokflow console false quit quickstart

            let pulsarInfos = scope.$eval(attributes.pulsar);
            //contenu des attrs

            if(!model.me.hasWorkflow(pulsarInfos.workflow)){
                element.removeAttr('pulsar') // ne remonte pas dans liste steps
                return;
            }

            if(pulsarInfos.workflow && !model.me.hasWorkflow(pulsarInfos.workflow)){
                element.data('skip-pulsar', 'true');// ne remonte pas dans liste steps
                //vire les pulsars qui ont pas les droits (pb si dernier & premier !!)
                return;
            }

            scope.pulsarInfos = pulsarInfos;
            scope.pulsarInfos.steps = [];

            let pulsars = $('[pulsar]');
            pulsars.each(function(index, element){
                let infos = angular.element(element).scope().$eval($(element).attr('pulsar'));
                infos.el = element;
                scope.pulsarInfos.steps.push(infos);
            });



            if(typeof pulsarInfos !== 'object' || pulsarInfos.index === undefined){
                console.error('Invalid pulsar object. Should look like pulsar="{ index: 0, i18n: \'my.key\', position: \'top bottom\'}"')
            }

            let pulsarButton;
            let pulsarElement;
            // content box

            let pulsarSize = 40;
            let pulsarMarge = 5;
            let pulsarLayerMarge = 10;


            let paintPulsar = function(){
                if(!pulsarInfos.position){
                    pulsarInfos.position = 'center center';
                }
                let xPosition = 'center';
                if(pulsarInfos.position.indexOf('left') !== -1){
                    xPosition = 'left';
                }
                if(pulsarInfos.position.indexOf('right') !== -1){
                    xPosition = 'right';
                }

                let yPosition = 'center';
                if(pulsarInfos.position.indexOf('top') !== -1){
                    yPosition = 'top';
                }
                if(pulsarInfos.position.indexOf('bottom') !== -1){
                    yPosition = 'bottom';
                }

                pulsarButton = $('<div class="pulsar-button"><div class="pulse"></div><div class="pulse2"></div><div class="pulse-spot"></div></div>')
                    .appendTo('body');
                if(pulsarInfos.className){
                    pulsarInfos.className.split(' ').forEach(function(cls){
                        pulsarButton.addClass(cls);
                    });
                }

                pulsarButton.data('active', true);

                let firstCycle = true;
                let placePulsar = function(){
                    let deltaX = 0;
                    let deltaY = 0;

                    if(pulsarInfos.delta){
                        pulsarInfos.delta = lang.translate(pulsarInfos.delta)

                        deltaX = parseInt(pulsarInfos.delta.split(' ')[0]);
                        deltaY = parseInt(pulsarInfos.delta.split(' ')[1]);
                    }


                    let xPositions = {
                        left: element.offset().left - (pulsarSize + pulsarMarge),
                        right: element.offset().left + element.width() + pulsarMarge,
                        center: element.offset().left + (element.width() / 2) - pulsarSize / 2
                    };

                    let yPositions = {
                        top: element.offset().top,
                        bottom: element.offset().top + element.height() + pulsarMarge,
                        center: element.offset().top + (element.height() / 2) - pulsarSize / 2
                    };

                    if(pulsarInfos.position === 'top center'){
                        yPositions.top = element.offset().top - pulsarSize - pulsarMarge;
                    }


                    if(pulsarButton.css('display') !== 'none'){
                        pulsarButton.offset({ left: parseInt(xPositions[xPosition] + deltaX), top: parseInt(yPositions[yPosition] + deltaY) });
                    }

                    if(pulsarElement && pulsarElement.find('.arrow').length){

                        let left = xPositions[xPosition] - pulsarElement.children('.content').width() - pulsarSize / 2
                        let top = yPositions[yPosition] - pulsarLayerMarge ;

                    // place pulsarElement // element

                        if(yPosition === 'top' && xPosition === 'center'){
                            top = yPositions[yPosition] - pulsarElement.children('.content').height() - pulsarSize / 2;
                        }
                        if(yPosition === 'center'){
                            top = yPositions[yPosition] - (pulsarElement.children('.content').height() / 2);

                            //top = yPositions[yPosition] - (pulsarElement.children('.content').width() / 2) + pulsarLayerMarge + pulsarMarge * 2;
                        }
                        if(xPosition === 'center'){
                            left = (xPositions[xPosition] - (pulsarElement.children('.content').width() / 2)) -2;
                        }
                        if(yPosition === 'center' && xPosition === 'center'){
                            pulsarElement.addClass('middle');
                            yPosition = 'bottom';
                        }
                        if(xPosition === 'right'){
                            left = xPositions[xPosition] + pulsarLayerMarge + pulsarMarge * 2;
                        }
                        if(yPosition === 'bottom'){
                            if(xPosition === 'center'){
                                top = yPositions[yPosition] + pulsarLayerMarge + pulsarMarge * 2;
                            }else{
                                top = yPositions[yPosition] - (pulsarLayerMarge + pulsarMarge);
                            }
                        }


                    // If pulsarElement position is cropped by browser:

                        var pulsarElementMarge = 15;

                        var oldTop = top;
                        var oldLeft = left

                        var maxX = oldLeft + (pulsarElement.width()) + pulsarElementMarge;
                        var maxY = oldTop + (pulsarElement.height()) + pulsarElementMarge

                        var newLeft = $(window).width() - (pulsarElement.width() + pulsarElementMarge);
                        var newBottom = $(window).height() - (pulsarElement.height() + pulsarElementMarge);

                        var gapLeft = oldLeft - newLeft;
                        var gapRight = pulsarElementMarge - (oldLeft);
                        var gapBottom = oldTop - newBottom;
                        var gapTop = pulsarElementMarge - (oldTop);

                        var arrow = pulsarElement.find('.arrow');
                        var arrowXpos = arrow.position().left;
                        var arrowYpos = arrow.position().top;

                        //console.log('init posY ' + arrowYpos);

                        //// X CORRECT

                        //right
                        if(maxX > $(window).width()){

                            left = $(window).width() - (pulsarElement.width() + pulsarElementMarge);
                            if(xPosition === 'center'){
                                if(firstCycle){
                                    arrow.css({left : arrowXpos + gapLeft, right : 'auto'});
                                    firstCycle = false;
                                 }
                            }
                        }
                        //left
                        if(left <= pulsarElementMarge){

                            left = pulsarElementMarge;
                            if(xPosition === 'center'){
                                if(firstCycle){
                                    arrow.css({left : gapRight + pulsarLayerMarge, right : 'auto'});
                                    firstCycle = false;
                                 }
                            }
                        }

                        //// Y CORRECT

                        //bottom
                        if(maxY > $(window).height()){
                            top = newBottom;
                            if(yPosition === 'bottom'){
                                if(firstCycle){
                                    arrow.css({top : gapBottom + pulsarMarge, bottom : 'auto'});
                                    firstCycle = false;
                                 }
                            }
                        }
                        //top --ok
                        if(top < pulsarElementMarge){
                            top = pulsarElementMarge;

                            if(yPosition === 'center' && xPosition !== 'center'){
                                if(firstCycle){
                                    arrow.css({top : (arrowYpos - gapTop), bottom : 'auto'});
                                    firstCycle = false;

                                 }
                            }
                        }

                        // apply content box position
                        pulsarElement.offset({
                            left: parseInt(left + deltaX),
                            top: parseInt(top + deltaY)
                        });
                    }
                    setTimeout(placePulsar, 100);
                }
                placePulsar();


                var placeLayers = function(){
                    $('.pulsar-layer').remove();

                    var check = '[pulsar-highlight=' + pulsarInfos.index +']';
                    var highlight = $( "body" ).find(check);
                    if(highlight.length !== 0){
                        var pulsarHighlight = highlight;
                    }

                    if(!pulsarHighlight){
                        var pulsarHighlight = element;
                    }

                     $('<div class="pulsar-layer"></div>')

                        .width(pulsarHighlight.width() + pulsarLayerMarge * 2)
                        .height(pulsarHighlight.height() + pulsarLayerMarge *  2)
                        .offset({ top: pulsarHighlight.offset().top - pulsarLayerMarge, left: pulsarHighlight.offset().left - pulsarLayerMarge, })
                        .hide()
                        .appendTo('body')
                        .fadeIn("slow");
                };

                pulsarButton.on('click', function(){
                    $('body').css('pointer-events', 'none');
                    $('body').on('scroll touchmove mousewheel', function(e){
                      e.preventDefault();
                      e.stopPropagation();
                      return false;
                    })

                    scope.pulsarInfos.steps = [];
                    pulsars = $('[pulsar]');
                    //recup tt les pulsar

                    pulsars.each(function(index, element){
                        //on recup les infos de chaque pulsar
                        let infos = angular.element(element).scope().$eval($(element).attr('pulsar'));
                        infos.el = element;
                        scope.pulsarInfos.steps.push(infos);
                    });

                    // create content box
                    pulsarElement = $('<pulsar></pulsar>')
                        .addClass(xPosition)
                        .addClass(yPosition);
                    if(pulsarInfos.className){
                        pulsarInfos.className.split(' ').forEach(function(cls){
                            pulsarElement.addClass(cls);
                        });
                    }
                    pulsarButton.hide();
                    placeLayers();
                    $(window).on('resize.placeLayers', placeLayers);
                    //scroll voir hauteur document ou bloquer scroll
                    // ok pour xp on layers

                    http().get('/infra/public/template/pulsar.html').done(function(html){
                        pulsarElement.html(
                            $compile(html)(scope)
                        );
                    });
                    $('body').append(pulsarElement);
                });
            }

            $(window).on('resize', function(){
                if(!pulsarButton){
                    return;
                }
                if($(window).width() <= ui.breakpoints.tablette){
                    pulsarButton.hide();
                }
                else if(pulsarButton.data('active')){
                    pulsarButton.show();
                }
            })

            function undraw(){
                // chaque nextStep + end
                pulsarElement.find('button').css('pointer-events', 'none');
                $(window).off('resize.placeLayers');
                pulsarButton.fadeOut('slow', function(){ pulsarButton.remove() });
                pulsarElement.fadeOut('slow', function(){ pulsarElement.remove() });
                $('.pulsar-layer').remove();
                $('body').off('scroll touchmove mousewheel');
                $('body').css('pointer-events', '');
                let firstCycle = true;
                pulsarButton.data('active', false);
            }

            scope.closePulsar = function(){
                if(!pulsarElement || !pulsarButton){
                    return
                }
                pulsarElement.fadeOut(0 , function(){ pulsarElement.remove() });
                pulsarButton.removeClass('hidden');
                $('.pulsar-layer').fadeOut(0 , function(){ $('.pulsar-layer').remove() });
                $('body').off('scroll touchmove mousewheel');
                $('body').css('pointer-events', '');
                let firstCycle = true;
                pulsarButton.data('active', false);

            };
            $(document).on('click', function(e){
                if(
                    $(e.target).parents('pulsar').length > 0 ||
                    $(e.target).parents('.pulsar-button').length > 0 ||
                    $(e.target).hasClass('pulsar-button')
                ){
                    return;
                }
                scope.closePulsar();
            });

            scope.paintPulsar = function(){
                paintPulsar();
                pulsarButton.trigger('click');
            };

            scope.isLastStep = function(){
                return _.find(scope.pulsarInfos.steps, function(step){
                    return step.index > scope.pulsarInfos.index;
                }) === undefined;
            };

            scope.goTo = function(step){
                undraw();
                quickstart.goToAppStep(step.index);
                angular.element(step.el).scope().paintPulsar();

            };

            scope.next = function(){

                undraw();
                let index = quickstart.nextAppStep();
                if(_.findWhere(scope.pulsarInfos.steps, { index: index}) === undefined){
                    if(_.find(scope.pulsarInfos.steps, function(item){ return item.index > index}) !== undefined){
                        scope.next();
                    }
                    return;
                }
                for(let i = 0; i < scope.pulsarInfos.steps.length; i++){
                    let item = scope.pulsarInfos.steps[i];
                    if(item.index === index){
                        if($(item.el).data('skip-pulsar')){
                            scope.next();
                            return;
                        }
                        angular.element(item.el).scope().paintPulsar();
                    }
                }
            };

            scope.previous = function(){
                undraw();
                let index = quickstart.previousAppStep();
                if(_.findWhere(scope.pulsarInfos.steps, { index: index}) === undefined){
                    if(_.find(scope.pulsarInfos.steps, function(item){ return item.index < index}) !== undefined){
                        scope.previous();
                    }
                    return;
                }
                for(let i = 0; i < scope.pulsarInfos.steps.length; i++){
                    let item = scope.pulsarInfos.steps[i];
                    if(item.index === index){
                        if($(item.el).data('skip-pulsar')){
                            scope.previous();
                            return;
                        }
                        angular.element(item.el).scope().paintPulsar();
                    }
                }
            };

            quickstart.load(function(){
                if(quickstart.appIndex() !== pulsarInfos.index){
                    return;
                }

                paintPulsar();
            });
        }
    }
});

$(document).ready(function() {
    setTimeout(function() {
        bootstrap(function() {
            RTE.addDirectives(module);
            if (window.AngularExtensions && window.AngularExtensions.init) {
                window.AngularExtensions.init(module);
            }
            model.build();

            lang.addDirectives(module);


            function start() {
                lang.addBundle('/i18n', function() {
                    lang.addBundle('/' + appPrefix + '/i18n', function() {
                        angular.bootstrap($('html'), ['app']);
                        model.sync();
                    });
                });
            }

            loader.openFile({
                url: skin.basePath + 'js/directives.js',
                success: function() {
                    if (typeof skin.addDirectives === 'function') {
                        skin.addDirectives(module, start);
                    } else {
                        start();
                    }
                },
                error: function() {
                    start();
                }
            })

        });
    }, 10);
});

function SearchPortal($scope, $window) {
    'use strict';
    $scope.launchSearch = function(event) {
        var words = $scope.mysearch;
        if (event != "link") event.stopPropagation();
        if ((event == "link" || event.keyCode == 13)) {
            words = (!words || words === '') ? ' ' : words;
            $scope.mysearch = "";
            $window.location.href = '/searchengine#/' + words;
        }
    };
}

function Account($scope) {
    "use strict";

    $scope.nbNewMessages = 0;
    $scope.me = model.me;
    $scope.rand = Math.random();
    $scope.skin = skin;
    $scope.lang = lang;
    $scope.refreshAvatar = function() {
        http().get('/userbook/api/person', {}, {
            requestName: "refreshAvatar"
        }).done(function(result) {
            $scope.avatar = result.result['0'].photo;
            if (!$scope.avatar || $scope.avatar === 'no-avatar.jpg' || $scope.avatar === 'no-avatar.svg') {
                $scope.avatar = '/assets/themes/' + skin.skin + '/img/illustrations/no-avatar.svg';
            }
            $scope.username = result.result['0'].displayName;
            model.me.profiles = result.result['0'].type;
            $scope.$apply();
        });
    };

    $scope.refreshMails = function() {
        http().get('/conversation/count/INBOX', {
            unread: true
        }).done(function(nbMessages) {
            $scope.nbNewMessages = nbMessages.count;
            $scope.$apply('nbNewMessages');
        });
    };

    $scope.openApps = function(event) {
        if ($(window).width() <= 700) {
            event.preventDefault();
        }
    }

    http().get('/directory/userbook/' + model.me.userId).done(function(data) {
        model.me.userbook = data;
        $scope.$apply('me');
    });

    skin.listThemes(function(themes) {
        $scope.themes = themes;
        $scope.$apply('themes');
    });

    $scope.$root.$on('refreshMails', $scope.refreshMails);

    $scope.refreshMails();
    $scope.refreshAvatar();
    $scope.currentURL = window.location.href;
}

function Share($rootScope, $scope, ui, _, lang) {
    if (!$scope.appPrefix) {
        $scope.appPrefix = appPrefix;
    }
    if ($scope.resources instanceof Model) {
        $scope.resources = [$scope.resources];
    }

    if (!($scope.resources instanceof Array)) {
        throw new TypeError('Resources in share panel must be instance of Model or Array');
    }

    $scope.sharing = {};
    $scope.found = [];
    $scope.maxResults = 5;

    $scope.editResources = [];
    $scope.sharingModel = {
        edited: []
    };

    $scope.addResults = function() {
        $scope.maxResults += 5;
    };

    var actionsConfiguration = {};

    http().get('/' + infraPrefix + '/public/json/sharing-rights.json').done(function(config) {
        actionsConfiguration = config;
    });

    $scope.translate = lang.translate;

    function actionToRights(item, action) {
        var actions = [];
        _.where($scope.actions, {
            displayName: action.displayName
        }).forEach(function(item) {
            item.name.forEach(function(i) {
                actions.push(i);
            });
        });

        return actions;
    }

    function rightsToActions(rights, http) {
        var actions = {};

        rights.forEach(function(right) {
            var action = _.find($scope.actions, function(action) {
                return action.name.indexOf(right) !== -1
            });

            if (!action) {
                return;
            }

            if (!actions[action.displayName]) {
                actions[action.displayName] = true;
            }
        });

        return actions;
    }

    function setActions(actions) {
        $scope.actions = actions;
        $scope.actions.forEach(function(action) {
            var actionId = action.displayName.split('.')[1];
            if (actionsConfiguration[actionId]) {
                action.priority = actionsConfiguration[actionId].priority;
                action.requires = actionsConfiguration[actionId].requires;
            }
        });
    }

    function dropRights(callback) {
        function drop(resource, type) {
            var done = 0;
            for (var element in resource[type].checked) {
                var path = '/' + $scope.appPrefix + '/share/remove/' + resource._id;
                var data = {};
                if (type === 'users') {
                    data.userId = element;
                } else {
                    data.groupId = element;
                }
                http().put(path, http().serialize(data));
            }
        }
        $scope.editResources.forEach(function(resource) {
            drop(resource, 'users');
            drop(resource, 'groups');
        });
        callback();
        $scope.varyingRights = false;
    }

    function differentRights(model1, model2) {
        var result = false;

        function different(type) {
            for (var element in model1[type].checked) {
                if (!model2[type].checked[element]) {
                    return true;
                }

                model1[type].checked[element].forEach(function(right) {
                    result = result || model2[type].checked[element].indexOf(right) === -1
                });
            }

            return result;
        }

        return different('users') || different('groups');
    }

    var feedData = function() {
        var initModel = true;
        $scope.resources.forEach(function(resource) {
            var id = resource._id;
            http().get('/' + $scope.appPrefix + '/share/json/' + id).done(function(data) {
                if (initModel) {
                    $scope.sharingModel = data;
                    $scope.sharingModel.edited = [];
                }

                data._id = resource._id;
                $scope.editResources.push(data);
                var editResource = $scope.editResources[$scope.editResources.length - 1];
                if (!$scope.sharing.actions) {
                    setActions(data.actions);
                }

                function addToEdit(type) {
                    for (var element in editResource[type].checked) {
                        var rights = editResource[type].checked[element];

                        var groupActions = rightsToActions(rights);
                        var elementObj = _.findWhere(editResource[type].visibles, {
                            id: element
                        });
                        if (elementObj) {
                            elementObj.actions = groupActions;
                            if (initModel) {
                                $scope.sharingModel.edited.push(elementObj);
                            }

                            elementObj.index = $scope.sharingModel.edited.length;
                        }
                    }
                }

                addToEdit('groups');
                addToEdit('users');

                if (!initModel) {
                    if (differentRights(editResource, $scope.sharingModel) || differentRights($scope.sharingModel, editResource)) {
                        $scope.varyingRights = true;
                        $scope.sharingModel.edited = [];
                    }
                }
                initModel = false;

                $scope.$apply('sharingModel.edited');
            });
        })
    };

    $scope.$watch('resources', function() {
        $scope.actions = [];
        $scope.sharingModel.edited = [];
        $scope.search = '';
        $scope.found = [];
        $scope.varyingRights = false;
        feedData();
    })

    $scope.addEdit = function(item) {
        item.actions = {};
        $scope.sharingModel.edited.push(item);
        item.index = $scope.sharingModel.edited.length;
        var addedIndex = $scope.found.indexOf(item);
        $scope.found.splice(addedIndex, 1);

        var defaultActions = []
        $scope.actions.forEach(function(action) {
            var actionId = action.displayName.split('.')[1];
            if (actionsConfiguration[actionId].default) {
                item.actions[action.displayName] = true;
                defaultActions.push(action);
            }
        });

        var index = -1;
        var loopAction = function() {
            if (++index < defaultActions.length) {
                $scope.saveRights(item, defaultActions[index], loopAction);
            }
        }
        loopAction()

    };

    $scope.findUserOrGroup = function() {
        var searchTerm = lang.removeAccents($scope.search).toLowerCase();
        $scope.found = _.union(
            _.filter($scope.sharingModel.groups.visibles, function(group) {
                var testName = lang.removeAccents(group.name).toLowerCase();
                return testName.indexOf(searchTerm) !== -1;
            }),
            _.filter($scope.sharingModel.users.visibles, function(user) {
                var testName = lang.removeAccents(user.lastName + ' ' + user.firstName).toLowerCase();
                var testNameReversed = lang.removeAccents(user.firstName + ' ' + user.lastName).toLowerCase();
                return testName.indexOf(searchTerm) !== -1 || testNameReversed.indexOf(searchTerm) !== -1;
            })
        );
        $scope.found = _.filter($scope.found, function(element) {
            return $scope.sharingModel.edited.indexOf(element) === -1;
        })
    };

    $scope.remove = function(element) {
        var data;
        if (element.login !== undefined) {
            data = {
                userId: element.id
            }
        } else {
            data = {
                groupId: element.id
            }
        }

        $scope.sharingModel.edited = _.reject($scope.sharingModel.edited, function(item) {
            return item.id === element.id;
        });

        $scope.resources.forEach(function(resource) {
            var path = '/' + $scope.appPrefix + '/share/remove/' + resource._id;
            http().put(path, http().serialize(data)).done(function() {
                $rootScope.$broadcast('share-updated', data);
            });
        })
    }

    $scope.maxEdit = 3;

    $scope.displayMore = function() {
        var displayMoreInc = 5;
        $scope.maxEdit += displayMoreInc;
    }

    function applyRights(element, action, cb) {
        var data;
        if (element.login !== undefined) {
            data = {
                userId: element.id
            }
        } else {
            data = {
                groupId: element.id
            }
        }
        data.actions = actionToRights(element, action);

        var setPath = 'json';
        if (!element.actions[action.displayName]) {
            setPath = 'remove';
            _.filter($scope.actions, function(item) {
                    return _.find(item.requires, function(dependency) {
                        return action.displayName.split('.')[1].indexOf(dependency) !== -1;
                    }) !== undefined
                })
                .forEach(function(item) {
                    if (item) {
                        element.actions[item.displayName] = false;
                        data.actions = data.actions.concat(actionToRights(element, item));
                    }
                })
        } else {
            action.requires.forEach(function(required) {
                var action = _.find($scope.actions, function(action) {
                    return action.displayName.split('.')[1].indexOf(required) !== -1;
                });
                if (action) {
                    element.actions[action.displayName] = true;
                    data.actions = data.actions.concat(actionToRights(element, action));
                }
            });
        }

        var times = $scope.resources.length
        var countdownAction = function() {
            if (--times <= 0 && typeof cb === 'function') {
                cb()
            }
        }

        $scope.resources.forEach(function(resource) {
            http().put('/' + $scope.appPrefix + '/share/' + setPath + '/' + resource._id, http().serialize(data)).done(function() {
                if (setPath === 'remove') {
                    $rootScope.$broadcast('share-updated', {
                        removed: {
                            groupId: data.groupId,
                            userId: data.userId,
                            actions: rightsToActions(data.actions)
                        }
                    });
                } else {
                    $rootScope.$broadcast('share-updated', {
                        added: {
                            groupId: data.groupId,
                            userId: data.userId,
                            actions: rightsToActions(data.actions)
                        }
                    });
                }
                countdownAction()
            });
        });
    }

    $scope.saveRights = function(element, action, cb) {
        if ($scope.varyingRights) {
            dropRights(function() {
                applyRights(element, action, cb)
            });
        } else {
            applyRights(element, action, cb)
        }
    };
}

function Admin($scope) {
    $scope.urls = [];
    http().get('/admin-urls').done(function(urls) {
        $scope.urls = urls;
        $scope.$apply('urls');
    });
    $scope.getHighlight = function(url) {
        return window.location.href.indexOf(url.url) >= 0
    }
    $scope.orderUrls = function(url) {
        return !$scope.getHighlight(url) ? 1 : 0
    }
    $scope.filterUrls = function(url) {
        return !url.allowed || !url.allowed instanceof Array ? true : _.find(model.me.functions, function(f) {
            return _.contains(url.allowed, f.code)
        })
    }

    $scope.scrollUp = ui.scrollToTop;
}

function Widget() {}

Widget.prototype.switchHide = function() {
    if (this.mandatory)
        return
    if (!this.hide) {
        this.hide = false;
    }
    this.hide = !this.hide;
    this.trigger('change');
    model.widgets.trigger('change');
    model.widgets.savePreferences();
}

function WidgetModel() {
    model.makeModels([Widget]);
    model.collection(Widget, {
        preferences: {},
        savePreferences: function() {
            var that = this;
            this.forEach(function(widget) {
                that.preferences[widget.name].hide = widget.hide;
                that.preferences[widget.name].index = widget.index;
            });
            http().putJson('/userbook/preference/widgets', this.preferences);
        },
        sync: function() {
            var that = this;
            var data = model.me.widgets

            http().get('/userbook/preference/widgets').done(function(pref) {
                if (!pref.preference) {
                    this.preferences = {};
                } else {
                    this.preferences = JSON.parse(pref.preference);
                }

                data = data.map(function(widget, i) {
                    if (!that.preferences[widget.name]) {
                        that.preferences[widget.name] = {
                            index: i,
                            show: true
                        };
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
                                loader.loadFile(widget.js)
                            })
                        } else {
                            that.push(widget)
                            loader.loadFile(widget.js)
                        }
                    })(widget)
                }
            }.bind(this))
        },
        findWidget: function(name) {
            return this.findWhere({
                name: name
            });
        },
        apply: function() {
            model.trigger('widgets.change');
        }
    });
}

function Widgets($scope, model, lang, date) {
    if (!model.widgets) {
        WidgetModel();
        model.widgets.sync();
    }

    $scope.widgets = model.widgets;

    $scope.allowedWidget = function(widget) {
        return (!$scope.list || $scope.list.indexOf(widget.name) !== -1) && !widget.hide;
    }

    model.on('widgets.change', function() {
        if (!$scope.$$phase) {
            $scope.$apply('widgets');
        }
    });

    $scope.translate = lang.translate;
    $scope.switchHide = function(widget, $event) {
        widget.switchHide();
        $event.stopPropagation();
    }
}

var workspace = {
    thumbnails: "thumbnail=120x120&thumbnail=150x150&thumbnail=100x100&thumbnail=290x290&thumbnail=48x48&thumbnail=82x82&thumbnail=381x381",
    Document: function(data) {
        if (data.metadata) {
            var dotSplit = data.metadata.filename.split('.');
            if (dotSplit.length > 1) {
                dotSplit.length = dotSplit.length - 1;
            }
            this.title = dotSplit.join('.');
        }

        if (data.created) {
            this.created = moment(data.created.split('.')[0]);
        }

        this.protectedDuplicate = function(callback) {
            Behaviours.applicationsBehaviours.workspace.protectedDuplicate(this, function(data) {
                callback(new workspace.Document(data))
            });
        };

        this.publicDuplicate = function(callback) {
            Behaviours.applicationsBehaviours.workspace.publicDuplicate(this, function(data) {
                callback(new workspace.Document(data))
            });
        };

        this.role = function() {
            var types = {
                'doc': function(type) {
                    return type.indexOf('document') !== -1 && type.indexOf('wordprocessing') !== -1;
                },
                'xls': function(type) {
                    return (type.indexOf('document') !== -1 && type.indexOf('spreadsheet') !== -1) || (type.indexOf('ms-excel') !== -1);
                },
                'img': function(type) {
                    return type.indexOf('image') !== -1;
                },
                'pdf': function(type) {
                    return type.indexOf('pdf') !== -1;
                },
                'ppt': function(type) {
                    return (type.indexOf('document') !== -1 && type.indexOf('presentation') !== -1) || type.indexOf('powerpoint') !== -1;
                },
                'video': function(type) {
                    return type.indexOf('video') !== -1;
                },
                'audio': function(type) {
                    return type.indexOf('audio') !== -1;
                }
            };

            for (var type in types) {
                if (types[type](this.metadata['content-type'])) {
                    return type;
                }
            }

            return 'unknown';
        }
    },
    Folder: function(data) {
        this.updateData(data);

        this.collection(workspace.Folder, {
            sync: function() {
                this.load(_.filter(model.mediaLibrary.myDocuments.folders.list, function(folder) {
                    return folder.folder.indexOf(data.folder + '_') !== -1;
                }));
            }
        });

        this.collection(workspace.Document, {
            sync: function() {
                http().get('/workspace/documents/' + data.folder, {
                    filter: 'owner',
                    hierarchical: true
                }).done(function(documents) {
                    this.load(documents);
                }.bind(this));
            }
        });

        this.closeFolder = function() {
            this.folders.all = [];
        };

        this.on('documents.sync', function() {
            this.trigger('sync');
        }.bind(this));
    },
    MyDocuments: function() {
        this.collection(workspace.Folder, {
            sync: function() {
                if (model.me.workflow.workspace.create) {
                    http().get('/workspace/folders/list', {
                        filter: 'owner'
                    }).done(function(data) {
                        this.list = data;
                        this.load(_.filter(data, function(folder) {
                            return folder.folder.indexOf('_') === -1;
                        }))
                    }.bind(this));
                }
            },
            list: []
        });

        this.collection(workspace.Document, {
            sync: function() {
                http().get('/workspace/documents', {
                    filter: 'owner',
                    hierarchical: true
                }).done(function(documents) {
                    this.load(documents);
                }.bind(this))
            }
        });

        this.on('folders.sync, documents.sync', function() {
            this.trigger('sync');
        }.bind(this));
    },
    SharedDocuments: function() {
        this.collection(workspace.Document, {
            sync: function() {
                if (model.me.workflow.workspace.list) {
                    http().get('/workspace/documents', {
                        filter: 'shared'
                    }).done(function(documents) {
                        this.load(documents);
                    }.bind(this));
                }
            }
        });
        this.on('documents.sync', function() {
            this.trigger('sync');
        }.bind(this));
    },
    PublicDocuments: function() {
        this.collection(workspace.Document, {
            sync: function() {
                http().get('/workspace/documents', {
                    filter: 'public',
                    application: 'media-library'
                }).done(function(documents) {
                    this.load(_.filter(documents, function(doc) {
                        return doc.folder !== 'Trash';
                    }));
                }.bind(this))
            }
        });
        this.on('documents.sync', function() {
            this.trigger('sync');
        }.bind(this));
    },
    AppDocuments: function() {
        this.collection(workspace.Document, {
            sync: function() {
                http().get('/workspace/documents', {
                    filter: 'protected'
                }).done(function(documents) {
                    this.load(_.filter(documents, function(doc) {
                        return doc.folder !== 'Trash';
                    }));
                }.bind(this))
            }
        });
        this.on('documents.sync', function() {
            this.trigger('sync');
        }.bind(this));
    }
};

workspace.Document.prototype.upload = function(file, requestName, callback, visibility) {
    if (!visibility) {
        visibility = 'protected';
    }
    var formData = new FormData();
    formData.append('file', file, file.name);
    http().postFile('/workspace/document?' + visibility + '=true&application=media-library&quality=0.7&' + workspace.thumbnails, formData, {
        requestName: requestName
    }).done(function(data) {
        if (typeof callback === 'function') {
            callback(data);
        }
    }).e400(function(e) {
        var error = JSON.parse(e.responseText);
        notify.error(error.error);
    });
};

function MediaLibrary($scope) {
    if (!model.mediaLibrary) {
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

    $scope.show = function(tab) {
        $scope.display.show = tab;
        $scope.upload.loading = [];
    };

    $scope.listFrom = function(listName) {
        $scope.display.listFrom = listName;
        model.mediaLibrary[$scope.display.listFrom].sync();
    };

    $scope.openFolder = function(folder) {
        if ($scope.openedFolder.closeFolder && folder.folder.indexOf($scope.openedFolder.folder + '_') === -1) {
            $scope.openedFolder.closeFolder();
        }

        $scope.openedFolder = folder;
        folder.sync();
        folder.on('sync', function() {
            $scope.documents = filteredDocuments(folder);
            $scope.folders = folder.folders.all;
            $scope.$apply('documents');
            $scope.$apply('folders');
        });
    };

    $scope.$watch('visibility', function(newVal) {
        if (model.me.workflow.workspace.create) {
            if ($scope.visibility === 'public') {
                $scope.display.listFrom = 'publicDocuments';
            } else {
                $scope.display.listFrom = 'appDocuments';
            }
        } else if (model.me.workflow.workspace.list) {
            $scope.display.listFrom = 'sharedDocuments';
        }

        model.mediaLibrary.on('myDocuments.sync, sharedDocuments.sync, appDocuments.sync, publicDocuments.sync', function() {
            $scope.documents = filteredDocuments(model.mediaLibrary[$scope.display.listFrom]);
            if (model.mediaLibrary[$scope.display.listFrom].folders) {
                $scope.folders = model.mediaLibrary[$scope.display.listFrom].folders.filter(function(folder) {
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

        $scope.$watch('fileFormat', function(newVal) {
            if (!newVal) {
                return;
            }

            if (newVal === 'audio') {
                $scope.display.show = 'record';
            } else {
                $scope.display.show = 'browse';
            }

            if (model.mediaLibrary[$scope.display.listFrom].documents.length() === 0) {
                model.mediaLibrary[$scope.display.listFrom].sync();
            } else {
                model.mediaLibrary[$scope.display.listFrom].trigger('sync');
            }
        });
    });

    function filteredDocuments(source) {
        return source.documents.filter(function(doc) {
            return (doc.role() === $scope.fileFormat || $scope.fileFormat === 'any') &&
                lang.removeAccents(doc.metadata.filename.toLowerCase()).indexOf(lang.removeAccents($scope.display.search.toLowerCase())) !== -1;
        });
    }

    $scope.insertRecord = function() {
        model.mediaLibrary.appDocuments.documents.sync();
        $scope.display.show = 'browse';
        $scope.listFrom('appDocuments');
    };

    $scope.selectDocument = function(document) {
        if (($scope.folder === model.mediaLibrary.appDocuments && $scope.visibility === 'protected') ||
            ($scope.folder === model.mediaLibrary.publicDocuments && $scope.visibility === 'public')) {
            if ($scope.multiple) {
                $scope.$parent.ngModel = [document];
            } else {
                $scope.$parent.ngModel = document;
            }
        } else {
            var copyFn = document.protectedDuplicate;
            if ($scope.visibility === 'public') {
                copyFn = document.publicDuplicate;
            }
            $scope.display.loading = [document];
            copyFn.call(document, function(newFile) {
                $scope.display.loading = [];
                if ($scope.multiple) {
                    $scope.$parent.ngModel = [newFile];
                    $scope.$parent.$apply('ngModel');
                } else {
                    $scope.$parent.ngModel = newFile;
                    $scope.$parent.$apply('ngModel');
                }
            });
        }
    };

    $scope.selectDocuments = function() {
        var selectedDocuments = _.where($scope.documents, {
            selected: true
        });
        if (($scope.folder === model.mediaLibrary.appDocuments && $scope.visibility === 'protected') ||
            ($scope.folder === model.mediaLibrary.publicDocuments && $scope.visibility === 'public')) {
            $scope.$parent.ngModel = selectedDocuments;
        } else {
            var duplicateDocuments = [];
            var documentsCount = 0;
            $scope.display.loading = selectedDocuments;
            selectedDocuments.forEach(function(doc) {
                var copyFn = doc.protectedDuplicate.bind(doc);
                if ($scope.visibility === 'public') {
                    copyFn = doc.publicDuplicate.bind(doc);
                }

                copyFn(function(newFile) {
                    $scope.display.loading = [];
                    duplicateDocuments.push(newFile);
                    documentsCount++;
                    if (documentsCount === selectedDocuments.length) {
                        $scope.$parent.ngModel = duplicateDocuments;
                        $scope.$parent.$apply('ngModel');
                    }
                });
            })
        }
    };

    $scope.setFilesName = function() {
        $scope.upload.names = '';
        for (var i = 0; i < $scope.upload.files.length; i++) {
            if (i > 0) {
                $scope.upload.names += ', '
            }
            $scope.upload.names += $scope.upload.files[i].name;
        }
    };

    $scope.importFiles = function() {
        var waitNumber = $scope.upload.files.length;
        for (var i = 0; i < $scope.upload.files.length; i++) {
            $scope.upload.loading.push($scope.upload.files[i]);
            workspace.Document.prototype.upload($scope.upload.files[i], 'file-upload-' + $scope.upload.files[i].name + '-' + i, function() {
                waitNumber--;
                model.mediaLibrary.appDocuments.documents.sync();
                if (!waitNumber) {
                    $scope.display.show = 'browse';
                    if ($scope.visibility === 'public') {
                        $scope.listFrom('publicDocuments');
                    } else {
                        $scope.listFrom('appDocuments');
                    }
                }
                $scope.$apply('display');
            }, $scope.visibility);
        }
        $scope.upload.files = undefined;
        $scope.upload.names = '';
    };

    $scope.updateSearch = function() {
        $scope.documents = filteredDocuments($scope.openedFolder);
    }
}
