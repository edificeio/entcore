var http = (function(){
	var statusEvents = ['done', 'error', 'e401', 'e404', 'e500'];


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
		request: function(url, params){
			var that = this;
			params.url = url;
			params.contentType=false;
			params.processData = false;
			params.cache = false;
			$.ajax(params)
				.done(function(e){
					if(typeof that.statusCallbacks.done === 'function'){
						that.statusCallbacks.done(e);
					}
				})
				.fail(function(e){
					if(typeof that.statusCallbacks['e' + e.status] === 'function'){
						that.statusCallbacks['e' + e.status](e);
					}

					if(parent !== window){
						messenger.notify('error', 'e' + e.status);
					}
					else{
						oneApp.notify.error(oneApp.i18n.i18n()("e" + e.status));
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
					url += '?' + that.serialize(data);
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
		params.contentType = false;
		params.processData = false;
		return http().post(url, data, params);
	}
};
