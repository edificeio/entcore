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
if(window.appPrefix === undefined){
	if(window.location.pathname.split('/').length > 0){
		window.appPrefix = window.location.pathname.split('/')[1]
	}
}

if(window.infraPrefix === undefined){
	window.infraPrefix = 'infra';
}

var currentLanguage = '';
(function(){
	var request = new XMLHttpRequest();
	request.open('GET', '/locale');
	request.async = false;
	request.onload = function(){
		if(request.status === 200){
			currentLanguage = JSON.parse(request.responseText).locale;
		}
	};
	request.send(null);
}());

if(document.addEventListener){
	document.addEventListener('DOMContentLoaded', function(){
		document.getElementsByTagName('body')[0].style.display = 'none';
	});
}

var routes = {
	define: function(routing){
		this.routing = routing;
	}
};

if(!Array.prototype.forEach){
	window.location.href = "/auth/upgrade";
}

function Model(data){
	if(typeof this.updateData === 'function'){
		this.updateData(data, false);
	}
}
Model.prototype.build = function(){};
var model = new Model();

var loader = (function(){
	var configurations = {
		'portal': [
			{ path: 'moment+langs.js' },
			{ path: 'jquery-1.10.2.min.js' },
			{ path: 'angular.min.js' },
			{ path: 'angular-sanitize.min.js' },
			{ path: 'angular-route.min.js' },
			{ path: 'underscore-min-1.4.4.js' },
			{ path: 'idiom.js' },
			{ path: 'editor.js' },
			{ path: 'lib.js' },
			{ path: 'ui.js' },
			{ path: 'humane.min.js' },
			{ path: 'angular-app.js' },
			{ path: '../mathjax/MathJax.js?config=TeX-AMS-MML_HTMLorMML' }]
	};

	var loadedScripts = {};

	var libraries = {
		humane: 'humane.min.js',
		underscore: 'underscore-min-1.4.4.js',
		ckeditor: '../ckeditor/ckeditor.js'
	};
	var basePath = document.getElementById('context').getAttribute('src').split('/');
	basePath.length = basePath.length - 1;
	basePath = basePath.join('/');

	var loadScript = function(script, completePath, async, callback){
		var element = document.createElement('script');
		element.type = "text/javascript";

		var xhr = new XMLHttpRequest();
		var src = '';
		if(completePath){
			src = script;
			if(async === true){
				element.onload = callback;
			}
			element.src = src;
		}
		else{
			src = basePath + '/' + script;
			if(element.async){
				element.async = false;
				element.src = src;
			}
			else{
				xhr.open('GET', src, false);
				xhr.send('');

				element.text = xhr.responseText;
			}
		}

		document.getElementsByTagName('head')[0].appendChild(element);
	};

	var syncLoadScript = function(script, completePath){
		var request = new XMLHttpRequest();
		var path = script;
		if(!completePath){
			path = basePath + '/' + script;
		}

		request.open('GET', path, false);
		request.onload = function(){
			if(request.status === 200){
				try{
					var lib = new Function(request.responseText);
					lib.name = script.path;
					lib();
					loadedScripts[path] = lib;
				}
				catch(e){

				}
			}
		};

		request.send(null);
	};

	var asyncLoadScript = function(path, callback){
		var request = new XMLHttpRequest();
		request.open('GET', path);
		request.onload = function(){
			if(request.status === 200){
				try{
					var lib = new Function(request.responseText);
					lib.name = path;
					lib();

					if(typeof callback === 'function'){
						callback();
					}
					loadedScripts[path] = lib;
				}
				catch(e){

				}
			}
		};
		request.send(null);
	};

	var load = function(script){
		loadScript(script.path, script.completePath);
	};

	if(document.addEventListener){
		document.addEventListener('DOMContentLoaded', function(){
			configurations.portal.forEach(load);
		});
	}

	return {
		load: function(library){
			if(!loadedScripts[library]){
				loadScript(libraries[library]);
			}
		},
		loadFile: function(library){
			if(!loadedScripts[library]){
				loadScript(library, true);
			}
		},
		syncLoad: function(library){
			if(!loadedScripts[library]){
				syncLoadScript(libraries[library]);
			}
		},
		syncLoadFile: function(path){
			if(!loadedScripts[path]){
				syncLoadScript(path, true);
			}
		},
		asyncLoad: function(path, callback){
			if(!loadedScripts[path]){
				asyncLoadScript(path, callback);
			} else {
				if(typeof callback === 'function'){
					callback();
				}
			}
		},
		openFile: function(params){
			if(params.async !== false){
				if(params.ajax !== false){
					var request = new XMLHttpRequest();
					request.open('GET', params.url);
					request.onload = function(){
						if(request.status === 200){
							try{
								var lib = new Function(request.responseText);
								lib.name = params.url;
								lib();

								if(typeof params.success === 'function'){
									params.success();
								}
								loadedScripts[params.url] = lib;
							}
							catch(e){
								if(typeof params.error === 'function'){
									params.error();
								}
							}
						}
						if(request.status === 404){
							if(typeof params.error === 'function'){
								params.error();
							}
						}
					};
					request.onerror = function(){
						if(typeof params.error === 'function'){
							params.error();
						}
					};
					request.send(null);
				}
				else{
					loadScript(params.url, true, true, params.success);
				}
			}
			else{
				if(params.ajax){
					syncLoadScript(params.url, true);
				}
				else{
					loadScript(params.url, true);
				}
			}
		}
	}
}());
