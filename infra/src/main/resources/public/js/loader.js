if(window.appPrefix === undefined){
	if(window.location.pathname.split('/').length > 0){
		window.appPrefix = window.location.pathname.split('/')[1]
	}
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
	}
	request.send(null);
}());


var loader = (function(){
	var configurations = {
		'portal': [
			{ path: 'moment+langs.js', async: true },
			{ path: 'jquery-1.9.1.js', async: true },
			{ path: 'angular.min.js', async: true },
			{ path: 'angular-sanitize.min.js', async: true },
			{ path: 'angular-route.min.js', async: true },
			{ path: 'lib.js', async: true },
			{ path: 'ui.js', async: true },
			{ path: 'angular-app.js', async: true },
			{ path: 'underscore-min-1.4.4.js', async: true },
			{ path: 'humane.min.js', async: true }],
		'app': [
			{ path: 'jquery-1.9.1.js', async: true },
			{ path: 'angular.min.js', async: true },
			{ path: 'angular-sanitize.min.js', async: true },
			{ path: 'lib.js', async: true },
			{ path: 'iframe.js', async: true},
			{ path: 'ui.js', async: true },
			{ path: 'angular-app.js', async: true }]
	};

	var loadedScripts = {};

	var libraries = {
		moment: 'moment+langs.js',
		humane: 'humane.min.js',
		iframe: 'iframe.js',
		underscore: 'underscore-min-1.4.4.js',
		ckeditor: '../ckeditor/ckeditor.js'
	}
	var basePath = document.getElementById('context').getAttribute('src').split('/');
	basePath.length = basePath.length - 1;
	basePath = basePath.join('/');

	var loadScript = function(script, completePath){
		var element = document.createElement('script');

		element.async = false;
		if(!completePath){
			element.src = basePath + '/' + script;
		}
		else{
			element.src = script;
		}

		element.type = 'text/javascript';
		document.getElementsByTagName('head')[0].appendChild(element);
	};

	var syncLoadScript = function(script){
		var request = new XMLHttpRequest();
		request.open('GET', basePath + '/' + script, false);
		request.onreadystatechange = function(){
			if(request.readyState === 4 && request.status === 200){
				var lib = new Function(request.responseText);
				lib.name = script.path;
				lib();
			}
		};
		request.send(null);
	};

	var asyncLoadScript = function(path, callback){
		var request = new XMLHttpRequest();
		request.open('GET', path);
		request.onload = function(){
			if(request.status === 200){
				var lib = new Function(request.responseText);
				lib.name = path;
				lib();

				if(typeof callback === 'function'){
					callback();
				}
				loadedScripts[path] = lib;
			}
		}
		request.send(null);
	}

	var load = function(script){
		if(script.async){
			loadScript(script.path, script.completePath);
		}
		else{
			syncLoadScript(script.path);
		}
	}

	if(parent !== window){
		configurations.app.forEach(load);
	}
	else{
		configurations.portal.forEach(load);
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
		asyncLoad: function(path, callback){
			if(!loadedScripts[path]){
				asyncLoadScript(path, callback);
			}
		}
	}
}())

document.addEventListener('DOMContentLoaded', function(){
	document.getElementsByTagName('body')[0].style.display = 'none';
});

var routes = {
	define: function(routing){
		this.routing = routing;
	}
};

var Model = {
	build: function(){}
}

