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
					str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
				}
			}
			return str.join("&");
		},
		filters: {
		},
		addFilter: function(filter, handler){
			Http.prototype.filters[filter] = handler;
		},
		notify: function(type, message){
			message = oneApp.i18n.translate(message);
			if(parent !== window){
				messenger.notify(type, message);
			}
			else{
				oneApp.notify[type](message);
			}
		},
		request: function(url, params){
			var that = this;
			params.url = url;
			params.cache = false;

			$.ajax(params)
				.done(function(e, statusText, xhr){
					if(typeof that.statusCallbacks.done === 'function'){
						if(document.cookie === '' && typeof Http.prototype.filters.disconnected === 'function'){
							that.filters.disconnected(e, statusText, xhr);
						}
						that.statusCallbacks.done(e, statusText, xhr);
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
							oneApp.notify.error(oneApp.i18n.i18n()("e" + e.status));
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

	var requestTypes = ['get', 'post', 'put', 'delete'];
	requestTypes.forEach(function(type){
		Http.prototype[type] = function(url, data, params){
			var that = this;

			if(!params){
				params = {};
			}
			if(typeof data === 'object' || typeof  data === 'string'){
				params.data = data;
				if(type === 'get' || type === 'delete'){
					if(url.indexOf('?') !== -1){
						url += '&' + that.serialize(data);
					}
					else{
						url += '?' + that.serialize(data);
					}
				}
			}
			params.type = type.toUpperCase();
			return this.request(url, params);
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
		return http().post(url, data, params);
	},
	filter: function(filter, handler){
		http().addFilter(filter, handler);
	}
};
