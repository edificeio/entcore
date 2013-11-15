var lang = (function(){
	var bundle = {};
	$.ajax({url: '/' + appPrefix + '/i18n', async: false})
		.done(function(data){
			bundle = data;
		})

	return {
		translate: function(key){
			return bundle[key] === undefined ? key : bundle[key];
		}
	}
}());

var http = (function(){
	var statusEvents = ['done', 'error', 'e401', 'e404', 'e500', 'e400'];


	function Http(){
		this.statusCallbacks = {};
	}

	Http.prototype = {
		serialize: function(obj){
			var str = [];
			for(var p in obj){
				if (obj.hasOwnProperty(p)) {
					if(obj[p] instanceof Array){
						for(var i = 0; i < obj[p].length; i++){
							str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p][i]))
						}
					}
					else{
						str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
					}
				}
			}


			return str.join("&");
		},
		events: {
		},
		bind: function(eventName, handler){
			Http.prototype.events[eventName] = handler;
		},
		request: function(url, params){
			var that = this;
			params.url = url;
			params.cache = false;

			var requestName = params.requestName;
			if(requestName && that.events['request-started.' + requestName]){
				that.events['request-started.' + requestName]();
			}

			$.ajax(params)
				.done(function(e, statusText, xhr){
					if(typeof that.statusCallbacks.done === 'function'){
						if(document.cookie === '' && typeof Http.prototype.events.disconnected === 'function'){
							that.events.disconnected(e, statusText, xhr);
						}
						that.statusCallbacks.done(e, statusText, xhr);
					}
					if(requestName && that.events['request-ended.' + requestName]){
						that.events['request-ended.' + requestName]();
					}
				})
				.fail(function(e){
					if(typeof that.statusCallbacks['e' + e.status] === 'function'){
						that.statusCallbacks['e' + e.status].call(that, e);
					}
					else{
						if(parent !== window){
							messenger.notify('error', 'e' + e.status);
						}
						else{
							humane.spawn({ addnCls: 'humane-original-error' })(lang.translate("e" + e.status));
						}
					}

					if(requestName && that.events['request-ended.' + requestName]){
						that.events['request-ended.' + requestName]();
					}
					console.log('HTTP error:' + e.status);
					console.log(e);
				});
			return this;
		}
	};

	statusEvents.forEach(function(event){
		Http.prototype[event] = function(callback){
			this.statusCallbacks[event] = callback;
			return this;
		}
	});

	var requestTypes = ['get', 'post', 'put', 'delete'];
	requestTypes.forEach(function(type){
		Http.prototype[type] = function(url, data, params, requestName){
			var that = this;

			if(!params){
				params = {};
			}
			if(typeof data === 'object' || typeof  data === 'string'){
				if(type === 'get' || type === 'delete'){
					if(url.indexOf('?') !== -1){
						url += '&' + that.serialize(data);
					}
					else{
						url += '?' + that.serialize(data);
					}
				}
				else{
					params.data = data;
				}
			}
			params.type = type.toUpperCase();
			return this.request(url, params, requestName);
		};
	});

	return function(){
		return new Http();
	}
}());



One = {
	get: function(url, data, params){
		return http().get(url, data, params);
	},
	post: function(url, data, params){
		return http().post(url, data, params);
	},
	delete: function(url, data, params){
		return http().delete(url, data, params);
	},
	put: function(url, data, params){
		return http().put(url, data, params);
	},
	postFile: function(url, data, params){
		if(typeof params !== 'object'){
			params = {};
		}
		params.contentType = false;
		params.processData = false;

		return http().post(url, data, params)
	},
	putFile: function(url, data, params){
		if(typeof params !== 'object'){
			params = {};
		}
		params.contentType = false;
		params.processData = false;

		return http().put(url, data, params)
	},
	bind: function(eventName, handler){
		http().bind(eventName, handler);
	},
	load: function(libName, callback){
		return loader.load(libName, callback);
	},
	translate: function(key){
		return lang.translate(key);
	},
	serialize: function(obj){
		return http().serialize(obj);
	}
};

var me = (function(){
	var infos = {};
	$.get('/auth/oauth2/userinfo', function(data){
		infos = data;
	});

	return {
		userInfos: function(){
			return infos;
		},
		allowedPaths: function(){

		}
	}
}())

var navigation = (function(){
	function NavigationContext(parentContext){
		this.position = parentContext.position;
		this.views = [];
		parentContext.views.forEach(function(view){
			this.views.push(view);
		});
		this.positionPath = parentContext.positionPath;
		this.callbacks = [];
		this.children = [];
		parentContext.children.push(this);
	}

	var navigationTree = { '/': {}};
	var rootContext = new NavigationContext({
		position: navigationTree['/'],
		positionPath: '/',
		scope: {},
		views: [],
		children: []
	});

	/*$.get('public/json/navigation.json').done(function(data){
		navigationTree = data;
		setRoot(rootContext);
		notify(rootContext);
	});*/

	function setRoot(context){
		context.position = navigationTree['/'];
		context.positionPath = '/';
		context.scope = {};
	}

	function findParent(p, current){
		var result;
		for(var route in current.routes){
			if(current.routes[route] === p){
				return current;
			}
			result = result || findParent(p, current.routes[route]);
		}
		return result;
	}

	function traverse(context, path){
		if(path[0] === '/'){
			setRoot(context);
		}
		else{
			context.position = findParent(context.position, navigationTree['/']);
		}
		var subPaths = path.split('/');

		subPaths.forEach(function(subPath){
			if(!subPath){
				return;
			}
			if(!context.position.routes[subPath]){
				throw 'Path not found in current position: ' + subPath + ' -- current position: ' + context.positionPath;
			}
			else{
				context.position = context.position.routes[subPath];
				for(var view in context.position.views){
					context.views[view] = context.position.views[view];
				}
			}
		})
		context.positionPath = path;
	}

	function notify(context){
		context.callbacks.forEach(function(cb){
			if(typeof cb === 'function'){
				cb();
			}
		});

		context.children.forEach(function(child){
			notify(child);
		})
	}

	return {
		navigate: function(context, path){
			traverse(context, path);
			notify(context);
		},
		execute: function(context, path){
			var initialPosition = context.position;
			this.navigate(context, path);
			context.position = initialPosition;
		},
		position: function(context){
			return context.position;
		},
		views: function(context){
			return context.views;
		},
		listen: function(callback, context){
			if(!context){
				context = rootContext;
			}
			context.callbacks.push(callback);
		},
		addContext: function(parentContext){
			if(!parentContext){
				parentContext = rootContext;
			}
			return new NavigationContext(parentContext);
		}
	}
}())
