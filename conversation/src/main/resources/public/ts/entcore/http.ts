import { idiom as lang } from './idiom';
import { _ } from './libs/underscore/underscore';

var loadedScripts = {};

export function Http(){
	this.statusCallbacks = {};
}

export var http = (function(){
	var statusEvents = ['done', 'error', 'e401', 'e404', 'e409', 'e500', 'e400', 'e413', 'e504', 'e0'];
	var xsrfCookie;
	if(document.cookie){
		var cookies = _.map(document.cookie.split(';'), function(c){
			return {
				name: c.split('=')[0].trim(), 
				val: c.split('=')[1].trim()
			};
		});
		xsrfCookie = _.findWhere(cookies, { name: 'XSRF-TOKEN' });
	}

	Http.prototype = {
		serialize: function(obj){
			var str = [];
			for(var p in obj){
				if (obj.hasOwnProperty(p)) {
					if(obj[p] instanceof Array){
						for(var i = 0; i < obj[p].length; i++){
							if(typeof obj[p][i] === 'object'){
								throw new TypeError("Arrays sent as URIs can't contain objects");
							}
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
		parseUrl: function (path, item){
			var matchParams = new RegExp(':[a-zA-Z0-9_.]+', "g");
			var params = path.match(matchParams);
			if(!params){
				params = [];
			}
			var getProp = function (prop, obj) {
			    if (prop.indexOf('.') === -1) {
			        return obj[prop];
			    }
			    return getProp(prop.split('.').splice(1).join('.'), obj[prop.split('.')[0]])
			}
			params.forEach(function(param){
			    var prop = param.split(':')[1];
                
			    var data = getProp(prop, item) || '';
			    path = path.replace(param, data);
			});
			return path;
		},
		request: function(url, params){
			var that = this;
			params.url = url;
			params.cache = false;

			if(!params.headers){
				params.headers = {};
			}
			if(xsrfCookie){
				params.headers['X-XSRF-TOKEN'] = xsrfCookie.val;
			}

			var requestName = params.requestName;
			if(requestName && that.events['request-started.' + requestName]){
				that.events['request-started.' + requestName]();
			}

			this.xhr = $.ajax(params)
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
					if(requestName && that.events['request-ended.' + requestName]){
						that.events['request-ended.' + requestName]();
					}

					if(typeof that.statusCallbacks['e' + e.status] === 'function'){
						that.statusCallbacks['e' + e.status].call(that, e);
					}
					else if(typeof that.statusCallbacks.error === 'function'){
						that.statusCallbacks.error.call(that, e);
					}
					else{
						if(!params.disableNotifications && e.status !== 0){
							humane.spawn({ addnCls: 'humane-original-error' })(lang.translate("e" + e.status));
						}
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

	Http.prototype.postFile = function(url, data, params){
		if(typeof params !== 'object'){
			params = {};
		}
		params.contentType = false;
		params.processData = false;

		return this.post(url, data, params)
	};

	Http.prototype.putFile = function(url, data, params){
		if(typeof params !== 'object'){
			params = {};
		}
		params.contentType = false;
		params.processData = false;

		return this.put(url, data, params)
	};

	Http.prototype.loadScript = function(url): Promise<any> {
		return new Promise((resolve, reject) => {
			if(loadedScripts[url]){
				resolve(loadedScripts[url]);
				return;
			}
			this.get(url).done((content) => {
				var f = new Function(content);
				loadedScripts[url] = f;
				try{
					f();
					resolve(f);
				}
				catch(e){
					reject(e);
				}
			});
		});
	}

	var requestTypes = ['get', 'post', 'put', 'delete'];
	requestTypes.forEach(function(type){
		Http.prototype[type + 'Json'] = function(url, data, params, requestName){
			if(!params){
				params = {};
            }
            params.contentType = 'application/json';
			params.data = (window as any).angular.toJson(data);
			params.type = type.toUpperCase();
			return this.request(url, params, requestName);
		};
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

export let toFormData : (obj) => string = http().serialize;

var $ = require('jquery');
var humane = require('humane-js');

if(!(window as any).entcore){
	(window as any).entcore = {};
}
(window as any).entcore.http = http;
(window as any).entcore.toFormData = http().serialize;
(window as any).http = http;