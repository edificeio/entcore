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
var lang = (function(){
	var bundle = {};
	$.ajax({url: '/' + appPrefix + '/i18n', async: false})
		.done(function(data){
			bundle = data;
		});
	$.ajax({url: '/i18n', async: false})
		.done(function(data){
			for(var prop in data){
				bundle[prop] = data[prop];
			}
		});

	var defaultDiacriticsRemovalMap = [
		{'base':'A', 'letters':/[\u0041\u24B6\uFF21\u00C0\u00C1\u00C2\u1EA6\u1EA4\u1EAA\u1EA8\u00C3\u0100\u0102\u1EB0\u1EAE\u1EB4\u1EB2\u0226\u01E0\u00C4\u01DE\u1EA2\u00C5\u01FA\u01CD\u0200\u0202\u1EA0\u1EAC\u1EB6\u1E00\u0104\u023A\u2C6F]/g},
		{'base':'AA','letters':/[\uA732]/g},
		{'base':'AE','letters':/[\u00C6\u01FC\u01E2]/g},
		{'base':'AO','letters':/[\uA734]/g},
		{'base':'AU','letters':/[\uA736]/g},
		{'base':'AV','letters':/[\uA738\uA73A]/g},
		{'base':'AY','letters':/[\uA73C]/g},
		{'base':'B', 'letters':/[\u0042\u24B7\uFF22\u1E02\u1E04\u1E06\u0243\u0182\u0181]/g},
		{'base':'C', 'letters':/[\u0043\u24B8\uFF23\u0106\u0108\u010A\u010C\u00C7\u1E08\u0187\u023B\uA73E]/g},
		{'base':'D', 'letters':/[\u0044\u24B9\uFF24\u1E0A\u010E\u1E0C\u1E10\u1E12\u1E0E\u0110\u018B\u018A\u0189\uA779]/g},
		{'base':'DZ','letters':/[\u01F1\u01C4]/g},
		{'base':'Dz','letters':/[\u01F2\u01C5]/g},
		{'base':'E', 'letters':/[\u0045\u24BA\uFF25\u00C8\u00C9\u00CA\u1EC0\u1EBE\u1EC4\u1EC2\u1EBC\u0112\u1E14\u1E16\u0114\u0116\u00CB\u1EBA\u011A\u0204\u0206\u1EB8\u1EC6\u0228\u1E1C\u0118\u1E18\u1E1A\u0190\u018E]/g},
		{'base':'F', 'letters':/[\u0046\u24BB\uFF26\u1E1E\u0191\uA77B]/g},
		{'base':'G', 'letters':/[\u0047\u24BC\uFF27\u01F4\u011C\u1E20\u011E\u0120\u01E6\u0122\u01E4\u0193\uA7A0\uA77D\uA77E]/g},
		{'base':'H', 'letters':/[\u0048\u24BD\uFF28\u0124\u1E22\u1E26\u021E\u1E24\u1E28\u1E2A\u0126\u2C67\u2C75\uA78D]/g},
		{'base':'I', 'letters':/[\u0049\u24BE\uFF29\u00CC\u00CD\u00CE\u0128\u012A\u012C\u0130\u00CF\u1E2E\u1EC8\u01CF\u0208\u020A\u1ECA\u012E\u1E2C\u0197]/g},
		{'base':'J', 'letters':/[\u004A\u24BF\uFF2A\u0134\u0248]/g},
		{'base':'K', 'letters':/[\u004B\u24C0\uFF2B\u1E30\u01E8\u1E32\u0136\u1E34\u0198\u2C69\uA740\uA742\uA744\uA7A2]/g},
		{'base':'L', 'letters':/[\u004C\u24C1\uFF2C\u013F\u0139\u013D\u1E36\u1E38\u013B\u1E3C\u1E3A\u0141\u023D\u2C62\u2C60\uA748\uA746\uA780]/g},
		{'base':'LJ','letters':/[\u01C7]/g},
		{'base':'Lj','letters':/[\u01C8]/g},
		{'base':'M', 'letters':/[\u004D\u24C2\uFF2D\u1E3E\u1E40\u1E42\u2C6E\u019C]/g},
		{'base':'N', 'letters':/[\u004E\u24C3\uFF2E\u01F8\u0143\u00D1\u1E44\u0147\u1E46\u0145\u1E4A\u1E48\u0220\u019D\uA790\uA7A4]/g},
		{'base':'NJ','letters':/[\u01CA]/g},
		{'base':'Nj','letters':/[\u01CB]/g},
		{'base':'O', 'letters':/[\u004F\u24C4\uFF2F\u00D2\u00D3\u00D4\u1ED2\u1ED0\u1ED6\u1ED4\u00D5\u1E4C\u022C\u1E4E\u014C\u1E50\u1E52\u014E\u022E\u0230\u00D6\u022A\u1ECE\u0150\u01D1\u020C\u020E\u01A0\u1EDC\u1EDA\u1EE0\u1EDE\u1EE2\u1ECC\u1ED8\u01EA\u01EC\u00D8\u01FE\u0186\u019F\uA74A\uA74C]/g},
		{'base':'OI','letters':/[\u01A2]/g},
		{'base':'OO','letters':/[\uA74E]/g},
		{'base':'OU','letters':/[\u0222]/g},
		{'base':'P', 'letters':/[\u0050\u24C5\uFF30\u1E54\u1E56\u01A4\u2C63\uA750\uA752\uA754]/g},
		{'base':'Q', 'letters':/[\u0051\u24C6\uFF31\uA756\uA758\u024A]/g},
		{'base':'R', 'letters':/[\u0052\u24C7\uFF32\u0154\u1E58\u0158\u0210\u0212\u1E5A\u1E5C\u0156\u1E5E\u024C\u2C64\uA75A\uA7A6\uA782]/g},
		{'base':'S', 'letters':/[\u0053\u24C8\uFF33\u1E9E\u015A\u1E64\u015C\u1E60\u0160\u1E66\u1E62\u1E68\u0218\u015E\u2C7E\uA7A8\uA784]/g},
		{'base':'T', 'letters':/[\u0054\u24C9\uFF34\u1E6A\u0164\u1E6C\u021A\u0162\u1E70\u1E6E\u0166\u01AC\u01AE\u023E\uA786]/g},
		{'base':'TZ','letters':/[\uA728]/g},
		{'base':'U', 'letters':/[\u0055\u24CA\uFF35\u00D9\u00DA\u00DB\u0168\u1E78\u016A\u1E7A\u016C\u00DC\u01DB\u01D7\u01D5\u01D9\u1EE6\u016E\u0170\u01D3\u0214\u0216\u01AF\u1EEA\u1EE8\u1EEE\u1EEC\u1EF0\u1EE4\u1E72\u0172\u1E76\u1E74\u0244]/g},
		{'base':'V', 'letters':/[\u0056\u24CB\uFF36\u1E7C\u1E7E\u01B2\uA75E\u0245]/g},
		{'base':'VY','letters':/[\uA760]/g},
		{'base':'W', 'letters':/[\u0057\u24CC\uFF37\u1E80\u1E82\u0174\u1E86\u1E84\u1E88\u2C72]/g},
		{'base':'X', 'letters':/[\u0058\u24CD\uFF38\u1E8A\u1E8C]/g},
		{'base':'Y', 'letters':/[\u0059\u24CE\uFF39\u1EF2\u00DD\u0176\u1EF8\u0232\u1E8E\u0178\u1EF6\u1EF4\u01B3\u024E\u1EFE]/g},
		{'base':'Z', 'letters':/[\u005A\u24CF\uFF3A\u0179\u1E90\u017B\u017D\u1E92\u1E94\u01B5\u0224\u2C7F\u2C6B\uA762]/g},
		{'base':'a', 'letters':/[\u0061\u24D0\uFF41\u1E9A\u00E0\u00E1\u00E2\u1EA7\u1EA5\u1EAB\u1EA9\u00E3\u0101\u0103\u1EB1\u1EAF\u1EB5\u1EB3\u0227\u01E1\u00E4\u01DF\u1EA3\u00E5\u01FB\u01CE\u0201\u0203\u1EA1\u1EAD\u1EB7\u1E01\u0105\u2C65\u0250]/g},
		{'base':'aa','letters':/[\uA733]/g},
		{'base':'ae','letters':/[\u00E6\u01FD\u01E3]/g},
		{'base':'ao','letters':/[\uA735]/g},
		{'base':'au','letters':/[\uA737]/g},
		{'base':'av','letters':/[\uA739\uA73B]/g},
		{'base':'ay','letters':/[\uA73D]/g},
		{'base':'b', 'letters':/[\u0062\u24D1\uFF42\u1E03\u1E05\u1E07\u0180\u0183\u0253]/g},
		{'base':'c', 'letters':/[\u0063\u24D2\uFF43\u0107\u0109\u010B\u010D\u00E7\u1E09\u0188\u023C\uA73F\u2184]/g},
		{'base':'d', 'letters':/[\u0064\u24D3\uFF44\u1E0B\u010F\u1E0D\u1E11\u1E13\u1E0F\u0111\u018C\u0256\u0257\uA77A]/g},
		{'base':'dz','letters':/[\u01F3\u01C6]/g},
		{'base':'e', 'letters':/[\u0065\u24D4\uFF45\u00E8\u00E9\u00EA\u1EC1\u1EBF\u1EC5\u1EC3\u1EBD\u0113\u1E15\u1E17\u0115\u0117\u00EB\u1EBB\u011B\u0205\u0207\u1EB9\u1EC7\u0229\u1E1D\u0119\u1E19\u1E1B\u0247\u025B\u01DD]/g},
		{'base':'f', 'letters':/[\u0066\u24D5\uFF46\u1E1F\u0192\uA77C]/g},
		{'base':'g', 'letters':/[\u0067\u24D6\uFF47\u01F5\u011D\u1E21\u011F\u0121\u01E7\u0123\u01E5\u0260\uA7A1\u1D79\uA77F]/g},
		{'base':'h', 'letters':/[\u0068\u24D7\uFF48\u0125\u1E23\u1E27\u021F\u1E25\u1E29\u1E2B\u1E96\u0127\u2C68\u2C76\u0265]/g},
		{'base':'hv','letters':/[\u0195]/g},
		{'base':'i', 'letters':/[\u0069\u24D8\uFF49\u00EC\u00ED\u00EE\u0129\u012B\u012D\u00EF\u1E2F\u1EC9\u01D0\u0209\u020B\u1ECB\u012F\u1E2D\u0268\u0131]/g},
		{'base':'j', 'letters':/[\u006A\u24D9\uFF4A\u0135\u01F0\u0249]/g},
		{'base':'k', 'letters':/[\u006B\u24DA\uFF4B\u1E31\u01E9\u1E33\u0137\u1E35\u0199\u2C6A\uA741\uA743\uA745\uA7A3]/g},
		{'base':'l', 'letters':/[\u006C\u24DB\uFF4C\u0140\u013A\u013E\u1E37\u1E39\u013C\u1E3D\u1E3B\u017F\u0142\u019A\u026B\u2C61\uA749\uA781\uA747]/g},
		{'base':'lj','letters':/[\u01C9]/g},
		{'base':'m', 'letters':/[\u006D\u24DC\uFF4D\u1E3F\u1E41\u1E43\u0271\u026F]/g},
		{'base':'n', 'letters':/[\u006E\u24DD\uFF4E\u01F9\u0144\u00F1\u1E45\u0148\u1E47\u0146\u1E4B\u1E49\u019E\u0272\u0149\uA791\uA7A5]/g},
		{'base':'nj','letters':/[\u01CC]/g},
		{'base':'o', 'letters':/[\u006F\u24DE\uFF4F\u00F2\u00F3\u00F4\u1ED3\u1ED1\u1ED7\u1ED5\u00F5\u1E4D\u022D\u1E4F\u014D\u1E51\u1E53\u014F\u022F\u0231\u00F6\u022B\u1ECF\u0151\u01D2\u020D\u020F\u01A1\u1EDD\u1EDB\u1EE1\u1EDF\u1EE3\u1ECD\u1ED9\u01EB\u01ED\u00F8\u01FF\u0254\uA74B\uA74D\u0275]/g},
		{'base':'oi','letters':/[\u01A3]/g},
		{'base':'ou','letters':/[\u0223]/g},
		{'base':'oo','letters':/[\uA74F]/g},
		{'base':'p','letters':/[\u0070\u24DF\uFF50\u1E55\u1E57\u01A5\u1D7D\uA751\uA753\uA755]/g},
		{'base':'q','letters':/[\u0071\u24E0\uFF51\u024B\uA757\uA759]/g},
		{'base':'r','letters':/[\u0072\u24E1\uFF52\u0155\u1E59\u0159\u0211\u0213\u1E5B\u1E5D\u0157\u1E5F\u024D\u027D\uA75B\uA7A7\uA783]/g},
		{'base':'s','letters':/[\u0073\u24E2\uFF53\u00DF\u015B\u1E65\u015D\u1E61\u0161\u1E67\u1E63\u1E69\u0219\u015F\u023F\uA7A9\uA785\u1E9B]/g},
		{'base':'t','letters':/[\u0074\u24E3\uFF54\u1E6B\u1E97\u0165\u1E6D\u021B\u0163\u1E71\u1E6F\u0167\u01AD\u0288\u2C66\uA787]/g},
		{'base':'tz','letters':/[\uA729]/g},
		{'base':'u','letters':/[\u0075\u24E4\uFF55\u00F9\u00FA\u00FB\u0169\u1E79\u016B\u1E7B\u016D\u00FC\u01DC\u01D8\u01D6\u01DA\u1EE7\u016F\u0171\u01D4\u0215\u0217\u01B0\u1EEB\u1EE9\u1EEF\u1EED\u1EF1\u1EE5\u1E73\u0173\u1E77\u1E75\u0289]/g},
		{'base':'v','letters':/[\u0076\u24E5\uFF56\u1E7D\u1E7F\u028B\uA75F\u028C]/g},
		{'base':'vy','letters':/[\uA761]/g},
		{'base':'w','letters':/[\u0077\u24E6\uFF57\u1E81\u1E83\u0175\u1E87\u1E85\u1E98\u1E89\u2C73]/g},
		{'base':'x','letters':/[\u0078\u24E7\uFF58\u1E8B\u1E8D]/g},
		{'base':'y','letters':/[\u0079\u24E8\uFF59\u1EF3\u00FD\u0177\u1EF9\u0233\u1E8F\u00FF\u1EF7\u1E99\u1EF5\u01B4\u024F\u1EFF]/g},
		{'base':'z','letters':/[\u007A\u24E9\uFF5A\u017A\u1E91\u017C\u017E\u1E93\u1E95\u01B6\u0225\u0240\u2C6C\uA763]/g}
	];

	return {
		translate: function(key){
			if(key === undefined){
				key = '';
			}
			return bundle[key] === undefined ? key : bundle[key];
		},
		addBundle: function(path, callback){
			$.get(path, function(newBundle){
				for(var property in newBundle){
					bundle[property] = newBundle[property];
				}

				if(typeof callback === "function"){
					callback();
				}
			})
		},
		addKeys: function(keys){
			for(var property in keys){
				bundle[property] = keys[property];
			}
		},
		removeAccents: function(str){
			for(var i=0; i<defaultDiacriticsRemovalMap.length; i++) {
				str = str.replace(defaultDiacriticsRemovalMap[i].letters, defaultDiacriticsRemovalMap[i].base);
			}
			return str;
		}
	}
}());

var http = (function(){
	var statusEvents = ['done', 'error', 'e401', 'e404', 'e500', 'e400', 'e413'];


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
		request: function(url, params){
			var that = this;
			params.url = url;
			params.cache = false;

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

	var requestTypes = ['get', 'post', 'put', 'delete'];
	requestTypes.forEach(function(type){
		Http.prototype[type + 'Json'] = function(url, data, params, requestName){
			if(!params){
				params = {};
			}
			params.data = angular.toJson(data);
			params.type = type.toUpperCase();
			return this.request(url, params, requestName);
		}
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

function Collection(obj){
	this.all = [];
	this.obj = obj;
	this.callbacks = {};
	this.sync = function(){}
}

(function(){
	function pluralizeName(obj){
		var name = (obj.name || obj._name);
		if(name[name.length - 1] === 'y' && name[name.length - 2] !== 'a' && name[name.length - 2] !== 'e'){
			return name[0].toLowerCase() + name.substr(1, name.length - 2) + 'ies';
		}
		return name[0].toLowerCase() + name.substr(1) + 's';
	}

	Collection.prototype = {
		on: function(eventName, cb){
			if(typeof cb !== 'function'){
				return;
			}
			if(!this.callbacks[eventName]){
				this.callbacks[eventName] = [];
			}
			this.callbacks[eventName].push(cb);
		},
		trigger: function(event){
			if(this.composer && this.composer.trigger){
				this.composer.trigger(pluralizeName(this.obj) + '.' + event);
			}

			if(!this.callbacks){
				this.callbacks = {};
			}

			var col = this;
			if(this.callbacks[event] instanceof Array){
				this.callbacks[event].forEach(function(cb){
					if(typeof cb === 'function'){
						cb.call(col);
					}
				});
			}
		},
		unbind: function(event){
			var events = event.split(',');
			var that = this;
			events.forEach(function(e){
				var eventName = e.trim();
				if(!that.callbacks){
					that.callbacks = {};
				}
				if(!that.callbacks[eventName]){
					that.callbacks[eventName] = [];
				}
				that.callbacks[eventName].pop();
			}.bind(this));
		},
		one: function(event, cb){
			this.on(event, function(){
				this.unbind(event);
				if(typeof cb === 'function'){
					cb();
				}
			}.bind(this));
		},
		forEach: function(cb){
			this.all.forEach(cb);
		},
		first: function(){
			return this.all[0];
		},
		select: function(predicate){
			_.find(this.all, predicate).forEach(function(item){
				item.selected = true;
			});
		},
		deselect: function(predicate){
			_.find(this.all, predicate).forEach(function(item){
				item.selected = false;
			});
		},
		selectAll: function(){
			this.all.forEach(function(item){
				item.selected = true;
			});
		},
		deselectAll: function(){
			this.all.forEach(function(item){
				item.selected = false;
			});
		},
		concat: function(col){
			return this.all.concat(col.all);
		},
		closeAll: function(){
			this.all.forEach(function(item){
				if(item.opened){
					item.opened = false;
				}
			})
		},
		current: null,
		setCurrent: function(item){
			this.current = item;
			this.trigger('change');
		},
		push: function(element, notify){
			var newItem = element;
			if(this.obj === undefined){
				this.obj = Model;
			}
			if(!(newItem instanceof this.obj)){
				newItem = new this.obj(element);
			}
			if(this.behaviours){
				newItem.behaviours(this.behaviours);
			}

			this.all.push(newItem);
			newItem.on('change', function(){
				this.trigger('change');
			}.bind(this));
			if(notify === false){
				return;
			}
			this.trigger('change');
			this.trigger('push');
		},
		remove: function(item, trigger){
			this.all = _.reject(this.all, function(element){
				return item === element;
			});
			if(trigger !== false){
				this.trigger('remove');
				this.trigger('change');
			}
		},
		removeAt: function(index){
			var element = this.all[index];
			this.remove(element);
		},
		insertAt: function(index, item){
			this.all.splice(index, 0, item);
			this.trigger('push');
			this.trigger('change');
		},
		getIndex: function(item){
			for(var i = 0; i < this.all.length; i++){
				if(this.all[i] === item){
					return i;
				}
			}
		},
		splice: function(){
			this.all.splice.apply(this.all, arguments);
			if(arguments.length > 2){
				this.trigger('push');
			}
			if(arguments[1] > 0){
				this.trigger('remove');
			}
			this.trigger('change');
		},
		selectItem: function(item){
			item.selected = true;
			this.trigger('change');
		},
		selection: function(){
			//returning the new array systematically breaks the watcher
			//due to the reference always being updated
			var currentSelection = _.where(this.all, { selected: true }) || [];
			var mismatch = !this._selection || this._selection.length !== currentSelection.length;
			if(!this._selection || this._selection.length !== currentSelection.length){
				this._selection = currentSelection;
			}
			return this._selection;
		},
		removeSelection: function(){
			this.all = _.reject(this.all, function(item){ return item.selected });
		},
		addRange: function(data, cb, notify){
			var that = this;
			data.forEach(function(item){
				if(!(newObj instanceof that.obj)){
					var newObj = Model.create(that.obj, item);
				}

				that.push(newObj, false);
				if(typeof cb === 'function'){
					cb(newObj);
				}
			});
			if(notify === false){
				return;
			}
			this.model.trigger(pluralizeName(this.obj) + '.change');
			this.trigger('change');
			this.trigger('push');
		},
		load: function(data, cb, notify){
			this.all = [];
			this.addRange(data, cb, notify);
			this.trigger('sync');
		},
		empty: function(){
			return this.all.length === 0;
		},
		length: function(){
			return this.all.length;
		},
		request: function(method, path, cb){
			var col = this;
			function parseUrl(path, item){
				var matchParams = new RegExp(':[a-zA-Z0-9_]+', ["g"]);
				var params = path.match(matchParams);
				params.forEach(function(param){
					var prop = param.split(':')[1];
					var data = item[prop] || col[prop] || col.model[prop] || '';
					path = path.replace(param, data);
				});
				return path;
			}
			this.selection().forEach(function(item){
				http()[method](parseUrl(path, item), {}).done(function(data){
					if(typeof cb === 'function'){
						cb(data);
					}
				});
			})
		},
		toJSON: function(){
			return this.all;
		},
		contains: function(obj){
			return this.all.indexOf(obj) !== -1;
		},
		last: function(){
			return this.all[this.all.length - 1];
		},
		removeAll: function(){
			this.all = [];
		}
	};

	for(var property in _){
		(function(){
			if(_.hasOwnProperty(property) && typeof _[property] === 'function'){
				var func = property;
				Collection.prototype[func] = function(arg){
					return _[func](this.all, arg);
				}
			}
		}());
	}

	Model.prototype.models = [];

	Model.prototype.makeModel = function(fn, methods, namespace){
		if(typeof fn !== 'function'){
			throw new TypeError('Only functions can be models');
		}
		if(!namespace){
			namespace = window;
		}
		// this cryptic code is meant to :
		// 1. make the instances of the constructor answer true to both instanceof ctr and instanceof Model
		// 2. force ctr to run Model constructor before itself
		// 3. apply name prop, which misses in IE (even modern versions)
		// 4. extend fn prototype with whatever functions the user sent
		if(fn.name === undefined){
			// grabs function name from function string
			fn.name = fn._name || fn.toString().match(/^function\s*([^\s(]+)/)[1];
		}
		// overwrites user ctr with a version calling parent ctr
		// fn is passed as parameter to keep its written behaviour
		var ctr = new Function("fn", "return function " + (fn.name || fn._name) + "(data){ Model.call(this, data); fn.call(this, data); }")(fn);
		ctr.prototype = Object.create(Model.prototype);
		ctr.name = fn.name;

		for(var method in methods){
			ctr.prototype[method] = methods[method];
		}

		for(var prop in fn.prototype){
			ctr.prototype[prop] = fn.prototype[prop];
		}
		// overwrites fn with custom ctr
		namespace[(ctr.name || ctr._name)] = ctr;
		Model.prototype.models.push(ctr);
	};

	Model.prototype.makeModels = function(constructors){
		if(!(constructors instanceof Array)){
			for(var ctr in constructors){
				if(constructors.hasOwnProperty(ctr)){
					if(ctr[0] === ctr.toUpperCase()[0]){
						constructors[ctr]._name = ctr;
						this.makeModel(constructors[ctr], {}, constructors);
					}
				}
			}
		}
		else{
			constructors.forEach(function(item){
				this.makeModel(item);
			}.bind(this));
		}
	};

	Model.prototype.collection = function(obj, methods){
		var col = new Collection(obj);
		col.composer = this;
		this[pluralizeName(obj)] = col;

		for(var method in methods){
			col[method] = methods[method];
		}

		col.model = this;
		return col;
	};

	Model.prototype.toJSON = function(){
		var dup = {};
		for(var prop in this){
			if(this.hasOwnProperty(prop) && prop !== 'callbacks' && prop !== 'data' && prop !== '$$hashKey'){
				dup[prop] = this[prop];
			}
		}
		return dup;
	};

	Model.prototype.toURL = function(){

	};

	Model.prototype.makePermanent = function(obj, methods){
		function setCol(col){
			col.composer = this;
			for(var method in methods){
				col[method] = methods[method];
			}

			col.model = this;
			col.behaviours = 'workspace';
		}


		var applicationPrefix = (methods && methods.fromApplication) || appPrefix;

		this[pluralizeName(obj)] = new Model();
		this[pluralizeName(obj)].mine = new Collection(obj);
		this[pluralizeName(obj)].shared = new Collection(obj);
		this[pluralizeName(obj)].trash = new Collection(obj);
		this[pluralizeName(obj)].mixed = new Collection(obj);

		var colContainer = this[pluralizeName(obj)];
		var mine = this[pluralizeName(obj)].mine;
		var trash = this[pluralizeName(obj)].trash;
		var shared = this[pluralizeName(obj)].shared;
		var mixed = this[pluralizeName(obj)].mixed;

		mine.on('change', function(){ colContainer.trigger('change') });
		shared.on('change', function(){ colContainer.trigger('change') });
		trash.on('change', function(){ colContainer.trigger('change') });
		mixed.on('change', function(){ colContainer.trigger('change') });
		mine.on('sync', function(){ colContainer.trigger('sync') });
		shared.on('sync', function(){ colContainer.trigger('sync') });
		trash.on('sync', function(){ colContainer.trigger('sync') });
		mixed.on('sync', function(){ colContainer.trigger('sync') });

		setCol.call(this, mine);
		setCol.call(this, trash);
		setCol.call(this, shared);
		setCol.call(this, mixed);

		mine.sync = function(){
			http().get('/workspace/documents', { filter: 'owner', application: applicationPrefix+ '-' + pluralizeName(obj) }).done(function(docs){
				docs = _.map(docs, function(doc){
					doc.title = doc.name.split('.json')[0];
					doc.modified = moment(doc.modified.split('.')[0]);
					doc.created = moment(doc.created.split('.')[0]);
					return doc;
				});
				mine.load(docs);
				mine.trigger('sync');
			});
		};

		mixed.sync = function(){
			http().get('/workspace/documents', { application: applicationPrefix+ '-' + pluralizeName(obj) }).done(function(docs){
				docs = _.map(docs, function(doc){
					doc.title = doc.name.split('.json')[0];
					doc.modified = moment(doc.modified.split('.')[0]);
					doc.created = moment(doc.created.split('.')[0]);
					return doc;
				});
				mixed.load(docs);
				mixed.trigger('sync');
			});
		};

		shared.sync = function(){
			http().get('/workspace/documents', { filter: 'shared', application: applicationPrefix+ '-' + pluralizeName(obj) }).done(function(docs){
				docs = _.map(docs, function(doc){
					doc.title = doc.name.split('.json')[0];
					doc.modified = moment(doc.modified.split('.')[0]);
					doc.created = moment(doc.created.split('.')[0]);
					return doc;
				});
				shared.load(docs);
				shared.trigger('sync');
			});
		};

		trash.sync = function(){
			http().get('/workspace/documents/Trash', { filter: 'owner', application: applicationPrefix+ '-' + pluralizeName(obj) }).done(function(docs){
				docs = _.map(docs, function(doc){
					doc.title = doc.name.split('.json')[0];
					doc.modified = moment(doc.modified.split('.')[0]);
					doc.created = moment(doc.created.split('.')[0]);
				});
				trash.load(docs);
				trash.trigger('sync');
			});
		};

		obj.prototype.save = function(){
			var toJson = JSON.parse(JSON.stringify(this));
			var tdl = ['callbacks', '_id', 'created', 'myRights', 'file', 'owner', 'ownerName', 'opened', 'shared', 'modified', 'metadata'];
			tdl.forEach(function(prop){
				delete toJson[prop];
			});

			var blob = new Blob([JSON.stringify(toJson)], { type: 'application/json' });
			var form = new FormData();
			form.append('blob', blob, this.title + '.json');

			if(this._id !== undefined){
				notify.info('notify.object.saved');
				http().putFile('/workspace/document/' + this._id, form);
			}
			else{
				http().postFile('/workspace/document?application=' + applicationPrefix+ '-' + pluralizeName(obj),  form).done(function(e){
					this._id = e._id;
					mine.sync();
					mixed.sync();
				}.bind(this));
			}
		};

		obj.prototype.move = function(){
			mine.sync();
		};

		obj.prototype.trash = function(){
			mine.sync();
			shared.sync();
			trash.sync();
		};

		obj.prototype.remove = function(){
			notify.info('notify.object.remove');
			http().delete('/workspace/document/' + this._id);
			mine.remove(this, false);
			trash.remove(this, false);
			shared.remove(this, false);
			mixed.remove(this, false);
		};

		obj.prototype.open = function(){
			this.opened = true;
			http().get('/workspace/document/' + this._id).done(function(content){
				delete content.$$hashKey;
				this.updateData(content);
				this.trigger('sync');
			}.bind(this))
		};

		obj.prototype.close = function(){
			if(this.opened === true){
				this.opened = false;
			}
		};

		mine.sync();
		shared.sync();
		trash.sync();
		mixed.sync();
	};

	Model.prototype.sync = function(){
		for(var col in this){
			if(this[col] instanceof Collection){
				this[col].sync();
			}
		}
	};

	Model.prototype.updateData = function(newData, triggerEvent){
		this.data = newData;
		if(typeof newData !== 'object'){
			return this;
		}

		for(var property in newData){
			if(newData.hasOwnProperty(property) && !(this[property] instanceof Collection)){
				this[property] = newData[property];
			}
			if(newData.hasOwnProperty(property) && this[property] instanceof Collection){
				this[property].load(newData[property]);
			}
		}

		if(triggerEvent !== false){
			this.trigger('change');
		}
	};

	Model.create = function(func, data){
		var newItem = new func(data);
		newItem.data = data;
		if(typeof data !== 'object'){
			return newItem;
		}

		for(var property in data){
			if(data.hasOwnProperty(property) && !newItem.hasOwnProperty(property)){
				newItem[property] = data[property];
			}
		}
		return newItem;
	};

	Model.prototype.on = function(event, cb){
		if(typeof cb !== 'function'){
			return;
		}
		var events = event.split(',');
		var that = this;
		events.forEach(function(e){
			var eventName = e.trim();

			if(!that.callbacks){
				that.callbacks = {}
			}
			if(!that.callbacks[eventName]){
				that.callbacks[eventName] = []
			}
			that.callbacks[eventName].push(cb);

			var propertiesChain = eventName.split('.');
			if(propertiesChain.length > 1){
				var prop = propertiesChain[0];
				propertiesChain.splice(0, 1);
				if(!this[prop] || !this[prop].on){
					throw "Property " + prop + " is undefined in " + eventName;
				}
				this[prop].on(propertiesChain.join('.'), cb);
			}
		}.bind(this));
	};

	Model.prototype.unbind = function(event){
		var events = event.split(',');
		var that = this;
		events.forEach(function(e){
			var eventName = e.trim();
			if(!that.callbacks){
				that.callbacks = {};
			}
			if(!that.callbacks[eventName]){
				that.callbacks[eventName] = [];
			}
			that.callbacks[eventName].pop();

			var propertiesChain = eventName.split('.');
			if(propertiesChain.length > 1){
				var prop = propertiesChain[0];
				propertiesChain.splice(0, 1);
				if(!this[prop] || !this[prop].on){
					throw "Property " + prop + " is undefined in " + eventName;
				}
				this[prop].unbind(propertiesChain.join('.'));
			}
		}.bind(this));
	};

	Model.prototype.one = function(event, cb){
		this.on(event, function(){
			this.unbind(event);
			if(typeof cb === 'function'){
				cb();
			}
		}.bind(this));
	};

	Model.prototype.trigger = function(event){
		if(!this.callbacks || !this.callbacks[event]){
			return;
		}
		for(var i = 0; i < this.callbacks[event].length; i++){
			if(typeof this.callbacks[event][i] === 'function'){
				this.callbacks[event][i].call(this);
			}
		}
	};

	Model.prototype.behaviours = function(serviceName){
		return Behaviours.findBehaviours(serviceName, this);
	};

	Model.prototype.inherits = function(targetFunction, prototypeFunction){
		var targetProps = {};
		for(var property in targetFunction.prototype){
			if(targetFunction.prototype.hasOwnProperty(property)){
				targetProps[property] = targetFunction.prototype[property];
			}
		}
		targetFunction.prototype = new prototypeFunction();
		for(var property in targetProps){
			targetFunction.prototype[property] = targetProps[property];
		}
	}
}());

var skin = (function(){
	return {
		templateMapping: {},
		skin: 'raw',
		theme: '/assets/themes/raw/default/',
		portalTemplate: '/assets/themes/raw/portal.html',
		logoutCallback: '/',
		loadDisconnected: function(){
			var rand = Math.random();
			var that = this;
			http().get('/skin', { token: rand }, {
				async: false,
				success: function(data){
					that.skin = data.skin;
					that.theme = '/assets/themes/' + data.skin + '/default/';
					http().get('/assets/themes/' + data.skin + '/i18n/' + (currentLanguage || 'en') + '.json', { token: rand }, {
						async: false,
						disableNotifications: true,
						success: function(translations){
							lang.addKeys(translations);
						}
					}).e404(function(){});

					http().get('/assets/themes/' + data.skin + '/template/override.json', { token: rand }, {
						async: false,
						disableNotifications: true,
						success: function(override){
							that.templateMapping = override;
						}
					}).e404(function(){});
				}
			});
		},
		listThemes: function(cb){
			http().get('/themes').done(function(themes){
				if(typeof cb === 'function'){
					cb(themes);
				}
			});
		},
		setTheme: function(theme){
			ui.setStyle(theme.path);
			http().get('/userbook/api/edit-userbook-info?prop=theme&value=' + theme._id);
		},
		loadConnected: function(){
			var rand = Math.random();
			var that = this;
			http().get('/theme', {}, {
				async: false,
				success: function(data){
					that.theme = data.skin;
					that.skin = that.theme.split('/assets/themes/')[1].split('/')[0];
					that.portalTemplate = '/assets/themes/' + that.skin + '/portal.html';
					that.logoutCallback = data.logoutCallback

					http().get('/assets/themes/' + that.skin + '/i18n/' + (window.currentLanguage || 'en') + '.json', { token: rand }, {
						async: false,
						disableNotifications: true,
						success: function(translations){
							lang.addKeys(translations);
						}
					});

					http().get('/assets/themes/' + that.skin + '/template/override.json', { token: rand }, {
						async: false,
						disableNotifications: true,
						success: function(override){
							that.templateMapping = override;
						}
					});
				}
			});
		}
	}
}());

var workspace = {
	thumbnails: "thumbnail=120x120&thumbnail=150x150&thumbnail=100x100&thumbnail=290x290&thumbnail=48x48&thumbnail=82x82&thumbnail=381x381",
	Document: function(data){
		if(data.metadata){
			var dotSplit = data.metadata.filename.split('.');
			if(dotSplit.length > 1){
				dotSplit.length = dotSplit.length - 1;
			}
			this.title = dotSplit.join('.');
		}

		this.protectedDuplicate = function(callback){
			var document = this;
			var xhr = new XMLHttpRequest();
			xhr.open('GET', '/workspace/document/' + this._id, true);
			xhr.responseType = 'blob';
			xhr.onload = function(e) {
				if (this.status == 200) {
					var blobDocument = this.response;
					var formData = new FormData();
					formData.append('file', blobDocument, document.metadata.filename);
					http().postFile('/workspace/document?protected=true&application=media-library&' + workspace.thumbnails, formData).done(function(data){
						if(typeof callback === 'function'){
							callback(new workspace.Document(data));
						}
					});
				}
			};
			xhr.send();
		};

		this.role = function(){
			var types = {
				'doc': function(type){
					return type.indexOf('document') !== -1 && type.indexOf('wordprocessing') !== -1;
				},
				'xls': function(type){
					return (type.indexOf('document') !== -1 && type.indexOf('spreadsheet') !== -1) || (type.indexOf('ms-excel') !== -1);
				},
				'img': function(type){
					return type.indexOf('image') !== -1;
				},
				'pdf': function(type){
					return type.indexOf('pdf') !== -1;
				},
				'ppt': function(type){
					return (type.indexOf('document') !== -1 && type.indexOf('presentation') !== -1) || type.indexOf('powerpoint') !== -1;
				},
				'video': function(type){
					return type.indexOf('video') !== -1;
				},
				'audio': function(type){
					return type.indexOf('audio') !== -1;
				}
			};

			for(var type in types){
				if(types[type](this.metadata['content-type'])){
					return type;
				}
			}

			return 'unknown';
		}
	},
	Folder: function(data){
		this.updateData(data);

		this.collection(workspace.Folder, {
			sync: function(){
				this.load(_.filter(model.mediaLibrary.myDocuments.folders.list, function(folder){
					return folder.folder.indexOf(data.folder + '_') !== -1;
				}));
			}
		});

		this.collection(workspace.Document,  {
			sync: function(){
				http().get('/workspace/documents/' + data.folder, { filter: 'owner', hierarchical: true }).done(function(documents){
					this.load(documents);
				}.bind(this));
			}
		});

		this.closeFolder = function(){
			this.folders.all = [];
		};

		this.on('documents.sync', function(){
			this.trigger('sync');
		}.bind(this));
	},
	MyDocuments: function(){
		this.collection(workspace.Folder, {
			sync: function(){
				if(model.me.workflow.workspace.documents.create){
					http().get('/workspace/folders/list', { filter: 'owner' }).done(function(data){
						this.list = data;
						this.load(_.filter(data, function(folder){
							return folder.folder.indexOf('_') === -1;
						}))
					}.bind(this));
				}
			},
			list: []
		});

		this.collection(workspace.Document,  {
			sync: function(){
				http().get('/workspace/documents', { filter: 'owner', hierarchical: true }).done(function(documents){
					this.load(documents);
				}.bind(this))
			}
		});

		this.on('folders.sync, documents.sync', function(){
			this.trigger('sync');
		}.bind(this));
	},
	SharedDocuments: function(){
		this.collection(workspace.Document,  {
			sync: function(){
				if(model.me.workflow.workspace.documents.list){
					http().get('/workspace/documents', { filter: 'shared' }).done(function(documents){
						this.load(documents);
					}.bind(this));
				}
			}
		});
		this.on('documents.sync', function(){
			this.trigger('sync');
		}.bind(this));
	},
	AppDocuments: function(){
		this.collection(workspace.Document, {
			sync: function(){
				http().get('/workspace/documents', { filter: 'protected' }).done(function(documents){
					this.load(_.filter(documents, function(doc){
						return doc.folder !== 'Trash';
					}));
				}.bind(this))
			}
		});
		this.on('documents.sync', function(){
			this.trigger('sync');
		}.bind(this));
	}
};

workspace.Document.prototype.upload = function(file, requestName, callback){
	var formData = new FormData();
	formData.append('file', file, file.name);
	http().postFile('/workspace/document?protected=true&application=media-library&' + workspace.thumbnails, formData, { requestName: requestName }).done(function(data){
		if(typeof callback === 'function'){
			callback(data);
		}
	});
};

var Behaviours = {};

var calendar = {
	getHours: function(scheduleItem, day){
		var startTime = 7;
		var endTime = 20;

		if(scheduleItem.beginning.dayOfYear() === day.index){
			startTime = scheduleItem.beginning.hours();
		}

		if(scheduleItem.end.dayOfYear() === day.index){
			endTime = scheduleItem.end.hours();
		}

		return {
			startTime: startTime,
			endTime: endTime
		}
	},
	TimeSlot: function(data){
	},
	ScheduleItem: function(){

	},
	Day: function(data){
		var day = this;
		this.collection(calendar.ScheduleItem, {
			scheduleItemWidth: function(scheduleItem){
				var nbConcurrentItems = 1;
				var scheduleItemTime = calendar.getHours(scheduleItem, day);

				for(var i = scheduleItemTime.startTime; i < scheduleItemTime.endTime; i++){
					var nbScheduleItems = this.filter(function(item){
						var itemHours = calendar.getHours(item, day);
						return itemHours.startTime <= i && itemHours.endTime > i;
					}).length;
					if(nbConcurrentItems < nbScheduleItems){
						nbConcurrentItems = nbScheduleItems;
					}
				}

				return Math.floor(12 / nbConcurrentItems);
			}
		});
		this.collection(calendar.TimeSlot);
		for(var i = calendar.startOfDay; i < calendar.endOfDay; i++){
			this.timeSlots.push(new calendar.TimeSlot({ start: i, end: i+1 }))
		}
	},
	Calendar: function(data){
		this.week = data.week;
		this.collection(calendar.Day);
		this.dayForWeek = new Date();
		function dayOfYear(dayOfWeek){
			var week = data.week;
			if(dayOfWeek === 0){
				week ++;
			}
			return moment().week(week).day(dayOfWeek).dayOfYear();
		}

		this.days.load([{ name: 'monday', index:  dayOfYear(1) },
			{ name: 'tuesday', index: dayOfYear(2) },
			{ name: 'wednesday', index: dayOfYear(3) },
			{ name: 'thursday', index: dayOfYear(4) },
			{ name: 'friday', index: dayOfYear(5) },
			{ name: 'saturday', index: dayOfYear(6) },
			{ name: 'sunday', index: dayOfYear(0) }]);

		this.collection(calendar.TimeSlot);
		for(var i = calendar.startOfDay; i < calendar.endOfDay; i++){
			this.timeSlots.push(new calendar.TimeSlot({ beginning: i, end: i+1 }))
		}

		this.firstDay = moment().week(this.week).day(1);
	},
	startOfDay: 7,
	endOfDay: 20,
	dayHeight: 40,
	init: function(){
		model.makeModels(calendar);
		model.calendar = new calendar.Calendar({ week: moment().week() });
	}
};

calendar.Calendar.prototype.addScheduleItems = function(items){
	var schedule = this;
	items.forEach(function(item){
		if(item.beginning.dayOfYear() > model.calendar.days.last().index || item.end.dayOfYear() < model.calendar.days.first().index){
			return;
		}

		var startDay = item.beginning.dayOfYear();
		var endDay = item.end.dayOfYear();

		for(var i = startDay; i <= endDay; i++){
			var day = schedule.days.findWhere({index: i});
			if(day){
				day.scheduleItems.push(item);
			}
		}
	});
};

calendar.Calendar.prototype.clearScheduleItems = function(){
	this.days.forEach(function(day){
		day.scheduleItems.removeAll();
	});
}

function bootstrap(func){
	calendar.init();
	http().get('/auth/oauth2/userinfo').done(function(data){
		if(typeof data !== 'object'){
			skin.loadDisconnected();
			func();
			return;
		}

		skin.loadConnected();
		model.me = data;
		model.me.preferences = {
			save: function(pref, data){
				if(data !== undefined){
					this[pref] = data;
				}

				model.trigger('preferences-updated');
			}
		};
		model.trigger('preferences-updated');

		model.me.hasWorkflow = function(workflow){
			return _.find(model.me.authorizedActions, function(workflowRight){
				return workflowRight.name === workflow;
			}) !== undefined || workflow === undefined;
		};

		model.me.hasRight = function(resource, right){
			if(right === 'owner'){
				return resource.owner.userId === model.me.userId;
			}

			var currentSharedRights = _.filter(resource.shared, function(sharedRight){
				return model.me.profilGroupsIds.indexOf(sharedRight.groupId) !== -1
					|| sharedRight.userId === model.me.userId;
			});

			var resourceRight = _.find(currentSharedRights, function(resourceRight){
				return resourceRight[right.right];
			}) !== undefined;

			var workflowRight = this.hasWorkflow(right.workflow);

			return resourceRight && workflowRight;
		};

		model.me.workflow = {
			load: function(services){
				services.forEach(function(serviceName){
					this[serviceName] = Behaviours.findWorkflow(serviceName);
				}.bind(this));
			}
		};

		model.me.workflow.load(['workspace', appPrefix]);
		model.trigger('me.change');
		func();
	})
	.e404(function(){
		func();
	});

	Behaviours = (function(){
		return {
			register: function(application, appBehaviours){
				this.applicationsBehaviours[application] = {};
				this.applicationsBehaviours[application] = appBehaviours;
			},
			findBehaviours: function(serviceName, resource){
				if(this.applicationsBehaviours[serviceName]){
					if(!resource.myRights){
						resource.myRights = {};
					}

					if(typeof this.applicationsBehaviours[serviceName].resource !== 'function'){
						var resourceRights = this.applicationsBehaviours[serviceName].rights.resource;

						this.applicationsBehaviours[serviceName].resource = function(element){
							for(var behaviour in resourceRights){
								if(model.me.hasRight(element, resourceRights[behaviour]) || model.me.userId === element.owner.userId){
									element.myRights[behaviour] = resourceRights[behaviour];
								}
							}
						}
					}
					return this.applicationsBehaviours[serviceName].resource(resource);
				}

				if(serviceName !== '.'){
					loader.syncLoadFile('/' + serviceName + '/public/js/behaviours.js');
					return this.applicationsBehaviours[serviceName].resource(resource);
				}

				return {}
			},
			loadBehaviours: function(serviceName, callback){
				if(this.applicationsBehaviours[serviceName]){
					callback(this.applicationsBehaviours[serviceName]);
					return;
				}

				if(serviceName === '.') {
					return;
				}

				loader.asyncLoad('/' + serviceName + '/public/js/behaviours.js', function(){
					callback(this.applicationsBehaviours[serviceName])
				}.bind(this));
			},
			findWorkflow: function(serviceName){
				var returnWorkflows = function(){
					if(!this.applicationsBehaviours[serviceName]){
						return {};
					}
					if(typeof this.applicationsBehaviours[serviceName].workflow === 'function'){
						return this.applicationsBehaviours[serviceName].workflow();
					}
					else{
						if(typeof this.applicationsBehaviours[serviceName].rights === 'object' && this.applicationsBehaviours[serviceName].rights.workflow){
							if(!this.applicationsBehaviours[serviceName].dependencies){
								this.applicationsBehaviours[serviceName].dependencies = {};
							}
							return this.workflowsFrom(this.applicationsBehaviours[serviceName].rights.workflow, this.applicationsBehaviours[serviceName].dependencies.workflow)
						}
					}
				}.bind(this);

				if(this.applicationsBehaviours[serviceName]){
					return returnWorkflows();
				}

				if(window.loader){
					loader.syncLoadFile('/' + serviceName + '/public/js/behaviours.js');
					return returnWorkflows();
				}
			},
			workflowsFrom: function(obj, dependencies){
				if(typeof obj !== 'object'){
					return {};
				}
				if(typeof dependencies !== 'object'){
					dependencies = {};
				}
				var workflow = { };
				for(var prop in obj){
					if(model.me.hasWorkflow(obj[prop])){
						workflow[prop] = true;
						if(typeof dependencies[prop] === 'string'){
							workflow[prop] = workflow[prop] && model.me.hasWorkflow(dependencies[prop]);
						}
					}
				}

				return workflow;
			}
		}
	}());

	Behaviours.applicationsBehaviours = {};
}
