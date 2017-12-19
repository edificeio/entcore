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

window.entcore = {
    appPrefix: appPrefix,
	infraPrefix: infraPrefix,
	MediaLibrary: {
		thumbnails: "thumbnail=120x120&thumbnail=100x100&thumbnail=290x290&thumbnail=381x381&thumbnail=1600x0"
	}
};

var currentLanguage = '';
(function(){
	var xsrfCookie;
	if(document.cookie){
		var cookiesSplit = document.cookie.split(';');
		var cookies = [];
		for(var i = 0; i < cookiesSplit.length; i++){
			var cookie = {
				name: cookiesSplit[i].split('=')[0].trim(), 
				val: cookiesSplit[i].split('=')[1].trim()
			};
			cookies.push(cookie);
			if(cookie.name === 'XSRF-TOKEN'){
				xsrfCookie = cookie;
			}
		}
	}

	// Fallback
	var fallBack = function(){
		// Fallback : navigator language
		var request = new XMLHttpRequest();
		request.open('GET', '/locale', false);
		if(xsrfCookie){
			request.setRequestHeader('X-XSRF-TOKEN', xsrfCookie.val);
		}
		request.async = false;
		request.onload = function() {
			if(request.status === 200){
				currentLanguage = JSON.parse(request.responseText).locale;
			} else {
				currentLanguage = 'fr'
			}
		};
		try {
			request.send(null);
		} catch (e) {
			currentLanguage = 'fr'
		}
	}

    // User preferences language
    var preferencesRequest = new XMLHttpRequest();
	preferencesRequest.open('GET', '/userbook/preference/language', false);
	if(xsrfCookie){
		preferencesRequest.setRequestHeader('X-XSRF-TOKEN', xsrfCookie.val);
	}
	preferencesRequest.async = false;

	preferencesRequest.onload = function(){

        if(preferencesRequest.status === 200){
            try {
    			currentLanguage = JSON.parse(JSON.parse(preferencesRequest.responseText).preference)['default-domain'];
    		} catch(e) {
    			fallBack();
    		}
        }

        if(!currentLanguage)
            fallBack();
    };
	try {
		preferencesRequest.send(null);
	} catch(e) {
		fallBack()
	}

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

var userAgent = navigator.userAgent;
var findVersion = function(){
	if(userAgent.indexOf('Chrome') !== -1 && userAgent.indexOf('Edge') === -1){
		version = parseInt(userAgent.split('Chrome/')[1].split('.')[0]);
		return {
			browser: 'Chrome',
			version: version,
			outdated: version < 39
		}
	}
	else if(userAgent.indexOf('IEMobile') !== -1){
		version = parseInt(navigator.userAgent.split('IEMobile/')[1].split(';')[0]);
		return {
			browser: 'MSIE',
			version: version,
			outdated: version < 10
		}
	}
	else if(userAgent.indexOf('AppleWebKit') !== -1 && userAgent.indexOf('Chrome') === -1){
		version = parseInt(userAgent.split('Version/')[1].split('.')[0]);
		return {
			browser: 'Safari',
			version: version,
			outdated: version < 7
		}
	}
	else if(userAgent.indexOf('Firefox') !== -1){
		version = parseInt(userAgent.split('Firefox/')[1].split('.')[0]);
		return {
			browser: 'Firefox',
			version: version,
			outdated: version < 34
		}
	}
	else if(userAgent.indexOf('Edge') !== -1){
		version = parseInt(userAgent.split('Edge/')[1]);
		return {
			browser: 'MSIE',
			version: version,
			outdated: version < 10
		}
	}
	else if(userAgent.indexOf('.NET CLR') !== -1 && userAgent.indexOf('Trident') !== -1){
		version = parseInt(userAgent.split('rv:')[1].split('.')[0]);
		return {
			browser: 'MSIE',
			version: version,
			outdated: version < 10
		}
	}
}

var version = findVersion();
if(version.outdated){
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
			{ path: 'underscore-min-1.8.3.js' },
			{ path: 'idiom.js' },
			{ path: 'editor.js' },
			{ path: 'lib.js' },
			{ path: 'ui.js' },
			{ path: 'humane.min.js' },
			{ path: 'angular-app.js' }]
	};

	var loadedScripts = {};

	var libraries = {
		humane: 'humane.min.js',
		underscore: 'underscore-min-1.8.3.js',
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
