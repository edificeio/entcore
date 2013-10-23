var loader = (function(){
	var configurations = {
		'portal': [
			{ path: 'jquery-1.9.1.js', async: true },
			{ path: 'angular.min.js', async: true },
			{ path: 'angular-sanitize.min.js', async: true },
			{ path: 'one-app.js', async: true },
			{ path: 'one.js', async: true },
			{ path: 'ui.js', async: true },
			{ path: 'humane.min.js', async: true }],
		'app': [
			{ path: 'jquery-1.9.1.js', async: true },
			{ path: 'angular.min.js', async: true },
			{ path: 'angular-sanitize.min.js', async: true },
			{ path: 'one.js', async: true },
			{ path: 'iframe.js', async: true},
			{ path: 'ui.js', async: true },
			{ path: 'one-app.js', async: true }]
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

	var loadScript = function(script){
		var element = document.createElement('script');

		element.async = false;
		element.src = basePath + '/' + script;
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
	}

	var load = function(script){
		if(script.async){
			loadScript(script.path);
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
		syncLoad: function(library){
			if(!loadedScripts[library]){
				syncLoadScript(libraries[library]);
			}
		}
	}
}())

document.addEventListener('DOMContentLoaded', function(){
	if(window !== parent){
		document.getElementsByTagName('body')[0].style.display = 'none';
	}
})


