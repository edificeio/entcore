var loader = (function(){
	var loadedScripts = {};
	var coreScripts = [
		'jquery-1.9.1.js', 'angular.min.js', 'angular-sanitize-1.0.1.js', 'one-app.js', 'one.js', 'ui.js'
	]
	var libraries = {
		moment: 'moment+langs.js',
		humane: 'humane.min.js',
		iframe: 'iframe.js',
		underscore: 'underscore-min-1.4.4.js'
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
				lib();
			}
		};
		request.send(null);
	}

	coreScripts.forEach(function(script){
		loadScript(script);
	});

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

if(window !== parent){
	loader.load('iframe');
}