/// <reference path="./loader.js"  />
/// <reference path="./angular.min.js"  />

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

window.entcore.$ = $;
window.entcore.moment = moment;
window.entcore.angular = angular;
window.entcore._ = _;

function Http(){
	this.statusCallbacks = {};
}

var http = (function(){
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
			var matchParams = new RegExp(':[a-zA-Z0-9_.]+', ["g"]);
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

			    var data = getProp(prop, item) || getProp(prop, col) || getProp(prop, col.model) || '';
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

	var requestTypes = ['get', 'post', 'put', 'delete'];
	requestTypes.forEach(function(type){
		Http.prototype[type + 'Json'] = function(url, data, params, requestName){
			if(!params){
				params = {};
			}
			params.contentType = "application/json";
			params.data = angular.toJson(data);
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
} ());

window.entcore.http = http;

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
		unbind: function(event, cb){
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
				if(!cb){
					that.callbacks[eventName].pop();
				}
				else{
					that.callbacks[eventName] = _.without(
						that.callbacks[eventName], _.find(
							that.callbacks[eventName], function(item){
								return item.toString() === cb.toString()
							}
						)
					);
				}
			}.bind(this));
		},
		one: function(event, cb){
			var that = this;
			var uniqueRun = function(){
				that.unbind(event, uniqueRun);
				if(typeof cb === 'function'){
					cb();
				}
			};
			this.on(event, uniqueRun);
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
		slice: function(a, b){
			return this.all.slice(a, b);
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
			else{
				newItem.behaviours(appPrefix);
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
		moveUp: function(item){
			var itemIndex = this.getIndex(item);
			var swap = this.all[itemIndex - 1];
			this.all[itemIndex - 1] = item;
			this.all[itemIndex] = swap;
			this.trigger('change');
		},
		moveDown: function(item){
			var itemIndex = this.getIndex(item);
			var swap = this.all[itemIndex + 1];
			this.all[itemIndex + 1] = item;
			this.all[itemIndex] = swap;
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
			if(!this._selection || this._selection.length !== currentSelection.length){
				this._selection = currentSelection;
			}
			return this._selection;
		},
		removeSelection: function(){
			if(this.obj.prototype.api){
				this.selection().forEach(function(item){
					item.remove();
				});
			}
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
			this.all.splice(0, this.all.length);
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

			this.selection().forEach(function(item){
				http()[method](http().parseUrl(path, item), {}).done(function(data){
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

	Model.prototype.sync = function(){
		http().get(http().parseUrl(this.api.get, this)).done(function(data){
			this.updateData(data);
		}.bind(this));
	};

	Model.prototype.saveModifications = function(){
		http().putJson(http().parseUrl(this.api.put, this), this);
	}

	Model.prototype.remove = function(){
		http().delete(http().parseUrl(this.api.delete, this));
	}

	Model.prototype.create = function(){
		http().postJson(http().parseUrl(this.api.post, this), this).done(function(data){
			this.updateData(data);
		}.bind(this));
	}

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

		if(fn.prototype.api){
			if(fn.prototype.api.get){
				fn.prototype.sync = function(){
					http().get(http().parseUrl(this.api.get, this)).done(function(data){
						this.updateData(data);
					}.bind(this));
				};
			}
			if(fn.prototype.api.put){
				fn.prototype.saveModifications = function(){
					http().putJson(http().parseUrl(this.api.put, this), this);
				};
			}
			if(fn.prototype.api.delete){
				fn.prototype.remove = function(){
					http().delete(http().parseUrl(this.api.delete, this));
				};
			}
			if(fn.prototype.api.post){
				fn.prototype.create = function(){
					http().postJson(http().parseUrl(this.api.post, this), this).done(function(data){
						this.updateData(data);
					}.bind(this));
				};
			}
			if(fn.prototype.api.post && fn.prototype.api.put && typeof fn.prototype.save !== 'function'){
				fn.prototype.save = function(){
					if(this._id || this.id){
						this.saveModifications();
					}
					else{
						this.create();
					}
				}
			}
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
			if(method === 'sync' && typeof methods[method] === 'string'){
				(function(){
					var path = methods[method];
					col[method] = function(){
						http().get(http().parseUrl(path, this.composer)).done(function(data){
							this.load(data);
						}.bind(this));
					}
				}());
			}
			else{
				col[method] = methods[method];
			}
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

	Model.prototype.unbind = function(event, cb){
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
			if(!cb){
				that.callbacks[eventName].pop();
			}
			else{
				that.callbacks[eventName] = _.without(
					that.callbacks[eventName], _.find(
						that.callbacks[eventName], function(item){
							return item.toString() === cb.toString()
						}
					)
				);
			}

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
		var that = this;
		var uniqueRun = function(){
			that.unbind(event, uniqueRun);
			if(typeof cb === 'function'){
				cb();
			}
		};
		this.on(event, uniqueRun);
	};

	Model.prototype.trigger = function(event, eventData){
		if(!this.callbacks || !this.callbacks[event]){
			return;
		}
		for(var i = 0; i < this.callbacks[event].length; i++){
			if(typeof this.callbacks[event][i] === 'function'){
				this.callbacks[event][i].call(this, eventData);
			}
		}
	};

	Model.prototype.behaviours = function(serviceName){
		if(this.shared || this.owner){
			return Behaviours.findRights(serviceName, this);
		}
		return {};
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

var quickstart = {
	steps: {},
	state: {},
	types: {
		ENSEIGNANT: 'teacher',
		ELEVE: 'student',
		PERSEDUCNAT: 'personnel',
		PERSRELELEVE: 'relative'
	},
	mySteps: [],
	assistantIndex: {},
	assistantStep: function(index){
		return skin.basePath + 'template/assistant/' + this.types[model.me.type] + '/' + index + '.html';
	},
	nextAssistantStep: function(){
		this.state.assistant ++;
		if(this.state.assistant === this.steps[this.types[model.me.type]]){
			this.state.assistant = -1;
			this.assistantIndex = undefined;
		}
		else{
			this.assistantIndex = this.mySteps[this.state.assistant];
		}

		this.save();
	},
	nextAppStep: function(){
		this.state[skin.skin][this.app]++;
		this.save();
		return this.state[skin.skin][this.app];
	},
	previousAppStep: function(){
		this.state[skin.skin][this.app]--;
		this.save();
		return this.state[skin.skin][this.app];

	},
	goToAppStep: function(index){
		this.state[skin.skin][this.app] = index;
		this.save();
		return index;
	},
	closeApp: function(){
		this.state[skin.skin][this.app] = -1;
		this.save();
	},
	appIndex: function(){
		this.app = window.location.href.split('//')[1].split('/').slice(1).join('/');
		if(!this.state[skin.skin]){
			this.state[skin.skin] = {};
		}
		if(!this.state[skin.skin][this.app]){
			this.state[skin.skin][this.app] = 0;
		}
		return this.state[skin.skin][this.app];
	},
	previousAssistantStep: function(){
		this.state.assistant --;
		if(this.state.assistant < 0){
			this.state.assistant = 0;
		}
		this.assistantIndex = this.mySteps[this.state.assistant];
		this.save();
	},
	save: function(cb){
		http().putJson('/userbook/preference/quickstart', this.state).done(function(){
			if(typeof cb === 'function'){
				cb();
			}
		});
	},
	goTo: function(index){
		if(index > this.mySteps.length){
			index = -1;
		}
		this.state.assistant = index;
		if(index !== -1){
			this.assistantIndex = this.mySteps[index];
		}

		this.save();
	},
	seeAssistantLater: function(){
		this.state.assistantTimer = moment().format('MM/DD/YYYY HH:mm');
		this.save();
	},
	loaded: false,
	awaiters: [],
	load: function(cb){
		if(this.loaded){
			if(typeof cb === 'function'){
				cb();
			}
			return;
		}

		this.awaiters.push(cb);
		if(this.loading){
			return;
		}
		this.loading = true;
		http().get('/userbook/preference/quickstart').done(function(pref){
			var preferences;
			if(pref.preference){
				try{
					preferences = JSON.parse(pref.preference);
				}
				catch(e){
					console.log('Error parsing quickstart preferences');
				}
			}
			if(!preferences){
				preferences = {};
			}

			if(!preferences.assistant){
				preferences.assistant = 0;
			}
			if(!preferences[skin.skin]){
				preferences[skin.skin] = {};
			}

			this.state = preferences;

			if(
				preferences.assistant !== -1 && !(
					preferences.assistantTimer 
					&& moment(preferences.assistantTimer).year() === moment().year() 
					&& moment(preferences.assistantTimer).dayOfYear() === moment().dayOfYear() 
					&& moment(preferences.assistantTimer).hour() === moment().hour()
				)
			){
				http().get(skin.basePath + 'template/assistant/steps.json').done(function(steps){
					this.steps = steps;
					var nbSteps = this.steps[this.types[model.me.type]];
					for(var i = 0; i < nbSteps; i++){
						this.mySteps.push({
							index: i,
							path: this.assistantStep(i)
						});
						this.assistantIndex = this.mySteps[this.state.assistant];
					}
					this.loaded = true;
					this.awaiters.forEach(function(cb){
						if(typeof cb === 'function'){
							cb();
						}
					});
				}.bind(this));
			}
			else{
				this.loaded = true;
				this.awaiters.forEach(function(cb){
					if(typeof cb === 'function'){
						cb();
					}
				});
			}
		}.bind(this));
	}
}

var skin = (function(){
	return {
		templateMapping: {},
		skin: 'raw',
		theme: '/assets/themes/raw/skins/default/',
		portalTemplate: '/assets/themes/raw/portal.html',
		basePath: '',
		logoutCallback: '/',
		loadDisconnected: function(){
			var rand = Math.random();
			var that = this;
			http().get('/skin', { token: rand }, {
				async: false,
				success: function(data){
					that.skin = data.skin;
					that.theme = '/assets/themes/' + data.skin + '/skins/default/';
					that.basePath = that.theme + '../../';

					http().get('/assets/themes/' + data.skin + '/template/override.json', { token: rand }, {
						async: false,
						disableNotifications: true,
						success: function(override){
							this.templateMapping = override;
						}.bind(this)
					}).e404(function(){});
				}.bind(this)
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
			http().get('/userbook/api/edit-userbook-info?prop=theme-'+ skin + '&value=' + theme._id);
		},
		loadConnected: function(){
			var rand = Math.random();
			var that = this;
			http().get('/theme', {}, {
				async: false,
				success: function(data){
					that.theme = data.skin;
					that.basePath = that.theme + '../../';
					that.skin = that.theme.split('/assets/themes/')[1].split('/')[0];
					that.portalTemplate = '/assets/themes/' + that.skin + '/portal.html';
					that.logoutCallback = data.logoutCallback;

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
	}).e400(function(e){
		var error = JSON.parse(e.responseText);
		notify.error(error.error);
	});
};

var Behaviours = (function(){
	return {
		copyRights: function(params){
			if(!params.provider.resource.shared){
				return;
			}
			http().get('/' + infraPrefix + '/public/json/sharing-rights.json').done(function(config){
				http().get('/' + params.provider.application + '/rights/sharing').done(function(providerSharing){
					http().get('/' + params.target.application + '/rights/sharing').done(function(targetSharing){
						params.provider.resource.shared.forEach(function(share){
							if(share.userId === model.me.userId){
								return;
							}
							var data = {  };
							if(share.groupId){
								data.groupId = share.groupId;
							}
							else{
								data.userId = share.userId;
							}

							var bundles = { read: false, contrib: false, publish: false, comment: false, manager: false };
							for(var property in share){
								for(var bundle in providerSharing){
									if(providerSharing[bundle].indexOf(property) !== -1){
										var bundleSplit = bundle.split('.');
										bundles[bundleSplit[bundleSplit.length - 1]] = true;
										config[bundleSplit[bundleSplit.length - 1]].requires.forEach(function(required){
											bundles[required] = true;
										});
									}
								}
							}

							function addRights(targetResource){
								data.actions = [];
								for(var bundle in bundles){
									if(!bundles[bundle]){
										continue;
									}
									for(var targetBundle in targetSharing){
										var targetBundleSplit = targetBundle.split('.');
										if(targetBundleSplit[targetBundleSplit.length - 1].indexOf(bundle) !== -1){
											targetSharing[targetBundle].forEach(function(right){
												data.actions.push(right);
											});
										}
									}
								}

								http().put('/' + params.target.application + '/share/json/' + targetResource, http().serialize(data)).e401(function(){});
							}

							//drop rights if I'm not part of the group
							if(model.me.groupsIds.indexOf(share.groupId) === -1){
								params.target.resources.forEach(function(targetResource){
									http().put('/' + params.target.application + '/share/remove/' + targetResource, data).done(function(){
										addRights(targetResource);
									}).e401(function(){});
								})
							}
							//simply add rights bundles (don't want to remove my own manager right)
							else{
								params.target.resources.forEach(addRights);
							}
						});
					});
				});
			}.bind(this));
		},
		register: function(application, appBehaviours){
			this.applicationsBehaviours[application] = {};
			this.applicationsBehaviours[application] = appBehaviours;
		},
		findRights: function(serviceName, resource){
			if(this.applicationsBehaviours[serviceName]){
				if(!resource.myRights){
					resource.myRights = {};
				}

				if(typeof this.applicationsBehaviours[serviceName].resource === 'function' ){
					console.log('resource method in behaviours is deprecated, please use rights object or rename to resourceRights');
					this.applicationsBehaviours[serviceName].resourceRights = this.applicationsBehaviours[serviceName].resource;
				}

				if(typeof this.applicationsBehaviours[serviceName].resourceRights !== 'function' && typeof this.applicationsBehaviours[serviceName].rights === 'object'){
					var resourceRights = this.applicationsBehaviours[serviceName].rights.resource;

					this.applicationsBehaviours[serviceName].resourceRights = function(element){
						for(var behaviour in resourceRights){
							if(model.me && (model.me.hasRight(element, resourceRights[behaviour]) ||
								(element.owner && (model.me.userId === element.owner.userId || model.me.userId === element.owner)))){
								element.myRights[behaviour] = resourceRights[behaviour];
							}
						}
					}
				}
				if(typeof this.applicationsBehaviours[serviceName].resourceRights === 'function'){
					return this.applicationsBehaviours[serviceName].resourceRights(resource);
				}
				else{
					return {};
				}
			}

			if(serviceName !== '.'){
				loader.syncLoadFile('/' + serviceName + '/public/js/behaviours.js');
				if(this.applicationsBehaviours[serviceName] && typeof this.applicationsBehaviours[serviceName].resource === 'function'){
					return this.applicationsBehaviours[serviceName].resourceRights(resource);
				}
				else{
					this.applicationsBehaviours[serviceName] = {};
					return this.applicationsBehaviours[serviceName];
				}
			}

			return {}
		},
		findBehaviours: function(serviceName, resource){
			console.log('Deprecated, please use findRights');
			this.findRights(serviceName, resource);
		},
		loadBehaviours: function(serviceName, callback){
			var actions = {
				error: function(cb){
					err = cb;
				}
			};

			var err = undefined;
			if(this.applicationsBehaviours[serviceName]){
				callback(this.applicationsBehaviours[serviceName]);
				return actions;
			}

			if(serviceName === '.') {
				return actions;
			}

			loader.openFile({
				url: '/' + serviceName + '/public/js/behaviours.js',
				success: function(){
					callback(this.applicationsBehaviours[serviceName])
				}.bind(this),
				error: function(){
					if(typeof err === 'function'){
						err();
					}
				}
			});

			return actions;
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

			if(window.loader && serviceName !== '.'){
				loader.syncLoadFile('/' + serviceName + '/public/js/behaviours.js');
				return returnWorkflows();
			}
			else{
				return {};
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
		},
		applicationsBehaviours: {}
	}
} ());

window.entcore.Behaviours = Behaviours;

var calendar = {
    setCalendar: function(cal){
        model.calendar = cal;
    },
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
			beforeCalendar: function(){
				return this.filter(function(scheduleItem){
					return scheduleItem.beginning.hour() < calendar.startOfDay;
				}).length;
			},
			afterCalendar: function(){
				return this.filter(function(scheduleItem){
					return scheduleItem.end.hour() > calendar.endOfDay;
				}).length;
			},
			scheduleItemWidth: function(scheduleItem){
				var concurrentItems = this.filter(function(item){
					return item.beginning.unix() < scheduleItem.end.unix() && item.end.unix() > scheduleItem.beginning.unix()
				});
				var maxGutter = 0
				_.forEach(concurrentItems, function(item){
					if(item.calendarGutter && item.calendarGutter > maxGutter){
						maxGutter = item.calendarGutter
					}
				});
				maxGutter++;
				return Math.floor(99 / maxGutter);
			}
		});
		this.collection(calendar.TimeSlot);
		for(var i = calendar.startOfDay; i < calendar.endOfDay; i++){
			this.timeSlots.push(new calendar.TimeSlot({ start: i, end: i+1 }))
		}
	},
	Calendar: function(data){
		if(!data.year){
			data.year = moment().year();
		}
		this.week = data.week;
		this.year = data.year;

	    // change of year in moment is buggy (last/first week is on the wrong year)
        // weird syntax is a workaround
        this.dayForWeek = moment(
            this.year +
            "-W" + (this.week < 10 ? "0" + this.week : this.week) +
            "-1")

		var that = this;

		this.collection(calendar.Day, {
			sync: function(){
				function dayOfYear(dayOfWeek){
					var week = that.dayForWeek.week();
					var year = that.dayForWeek.year();
					if(dayOfWeek === 0){
                        return moment(that.dayForWeek).day(dayOfWeek).add(1, 'w').dayOfYear()
					}
					return moment(that.dayForWeek).day(dayOfWeek).dayOfYear()
				}

				that.days.load([{ name: 'monday', index:  dayOfYear(1) },
					{ name: 'tuesday', index: dayOfYear(2) },
					{ name: 'wednesday', index: dayOfYear(3) },
					{ name: 'thursday', index: dayOfYear(4) },
					{ name: 'friday', index: dayOfYear(5) },
					{ name: 'saturday', index: dayOfYear(6) },
					{ name: 'sunday', index: dayOfYear(0) }]);

				that.firstDay = moment(that.dayForWeek).day(1)
			}
		});

		this.days.sync();

		this.collection(calendar.TimeSlot);
		for(var i = calendar.startOfDay; i < calendar.endOfDay; i++){
			this.timeSlots.push(new calendar.TimeSlot({ beginning: i, end: i+1 }))
		}
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
	items = _.filter(items, function (item) {
	    return moment(item.end).year() >= schedule.firstDay.year()
	});
	_.filter(items, function (item) {
	    return moment(item.end).month() >= schedule.firstDay.month() || moment(item.end).year() > schedule.firstDay.year()
	});
	items.forEach(function(item){
		var startDay = moment(item.beginning);
		var endDay = moment(item.end);

		var refDay = moment(schedule.firstDay)
		schedule.days.forEach(function(day){
			if(startDay.isBefore(moment(refDay).endOf('day')) && endDay.isAfter(moment(refDay).startOf('day')))
				day.scheduleItems.push(item);
			refDay.add('day', 1);
		});
	});
};

calendar.Calendar.prototype.setDate = function(momentDate){
	this.dayForWeek = momentDate;
	this.days.sync();
	this.trigger('date-change');
};

calendar.Calendar.prototype.clearScheduleItems = function(){
	this.days.forEach(function(day){
		day.scheduleItems.removeAll();
	});
};

var recorder = (function(){
	//vendor prefixes
	navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia ||
		navigator.mozGetUserMedia || navigator.msGetUserMedia;
	window.AudioContext = window.AudioContext || window.webkitAudioContext;

	var context,
		ws = null,
		intervalId,
		gainNode,
		recorder,
		player = new Audio();
	var leftChannel = [],
		rightChannel = [];

	var bufferSize = 16384,
		loaded = false,
		recordingLength = 0,
		lastIndex = 0,
		encoder = new Worker('/infra/public/js/audioEncoder.js'),
		followers = [];

	function uuid() {
		return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
            return v.toString(16);
        });
	}

	function sendWavChunk() {
		var	index = rightChannel.length;
		if (!(index > lastIndex)) return;
		encoder.postMessage(['chunk', leftChannel.slice(lastIndex, index), rightChannel.slice(lastIndex, index), (index - lastIndex) * bufferSize]);
		encoder.onmessage = function(e) {
			var deflate = new Zlib.Deflate(e.data);
			ws.send(deflate.compress());
		};
		lastIndex = index;
	}

	function closeWs() {
		if (ws) {
			if (ws.readyState === 1) {
				ws.close()
			}
		}
        clearWs();
	}

	function clearWs() {
		ws = null;
        leftChannel = [];
		rightChannel = [];
		lastIndex = 0;
	}

	function notifyFollowers(status, data){
		followers.forEach(function(follower){
			if(typeof follower === 'function'){
				follower(status, data);
			}
		})
	}

	return {
		elapsedTime: 0,
		loadComponents: function () {
		    this.title = lang.translate('recorder.filename') + moment().format('DD/MM/YYYY');
			loaded = true;
			
			navigator.getUserMedia({
			audio: true
			}, function(mediaStream){
				context = new AudioContext();
				var audioInput = context.createMediaStreamSource(mediaStream);
				gainNode = context.createGain();
				audioInput.connect(gainNode);

				recorder = context.createScriptProcessor(bufferSize, 2, 2);
				recorder.onaudioprocess = function(e){
					if(this.status !== 'recording'){
						return;
					}
					var left = new Float32Array(e.inputBuffer.getChannelData (0));
					leftChannel.push (left);
					var right = new Float32Array(e.inputBuffer.getChannelData (1));
					rightChannel.push (right);

					recordingLength += bufferSize;

					this.elapsedTime += e.inputBuffer.duration;

					sendWavChunk();

					notifyFollowers(this.status);
				}.bind(this);

				gainNode.connect (recorder);
				recorder.connect (context.destination);

			}.bind(this), function(err){

			});
			
		},
		isCompatible: function(){
			return navigator.getUserMedia !== undefined && window.AudioContext !==undefined;
		},
		stop: function(){
			if (ws) {
				ws.send("cancel");
			}
			this.status = 'idle';
			player.pause();
			if(player.currentTime > 0){
				player.currentTime = 0;
			}
			leftChannel = [];
			rightChannel = [];
			notifyFollowers(this.status);
		},
		flush: function(){
			this.stop();
			this.elapsedTime = 0;
			leftChannel = [];
			rightChannel = [];
			notifyFollowers(this.status);
		},
		record: function(){
			player.pause();
			var that = this;
			if (ws) {
				that.status = 'recording';
				notifyFollowers(that.status);
				if(!loaded){
					that.loadComponents();
				}
			} else {
				ws = new WebSocket((window.location.protocol === "https:" ? "wss": "ws") + "://" +
						window.location.host + "/audio/" + uuid());
				ws.onopen = function () {
					if(player.currentTime > 0){
						player.currentTime = 0;
					}

					that.status = 'recording';
					notifyFollowers(this.status);
					if(!loaded){
						http().get('/infra/public/js/zlib.min.js').done(function(){
							that.loadComponents();
						}.bind(this));
					}
				};
				ws.onerror = function (event) {
					console.log(event);
					that.status = 'stop';
                    notifyFollowers(that.status);
                    closeWs();
                    notify.info(event.data);
				}
                ws.onmessage = function (event) {
                	if (event.data && event.data.indexOf("error") !== -1) {
                		console.log(event.data);
						closeWs();
						notify.info(event.data);
                	} else if (event.data && event.data === "ok") {
                		closeWs();
                		notify.info("recorder.saved");
                	}

                }
                ws.onclose = function (event) {
                	that.status = 'stop';
                    notifyFollowers(that.status);
                    clearWs();
                }
			}
		},
		pause: function(){
			this.status = 'paused';
			player.pause();
			notifyFollowers(this.status);
		},
		play: function(){
			this.pause();
			this.status = 'playing';
			var encoder = new Worker('/infra/public/js/audioEncoder.js');
			encoder.postMessage(['wav', rightChannel, leftChannel, recordingLength]);
			encoder.onmessage = function(e) {
				player.src = window.URL.createObjectURL(e.data);
				player.play();
			};
			notifyFollowers(this.status);
		},
		state: function(callback){
			followers.push(callback);
		},
		title: "",
		status: 'idle',
		save: function(callback, format){
//			this.stop();
			sendWavChunk();
			ws.send("save-" +  this.title);
			this.status = 'encoding';
			notifyFollowers(this.status);
		},
		mute: function(mute){
			if(mute){
				gainNode.gain.value = 0;
			}
			else{
				gainNode.gain.value = 1;
			}
		}
	}
}());

var sniplets = {
	load: function(callback){
		var sniplets = this;
		http().get('/resources-applications').done(function(apps) {
			if(model.me){
				apps = model.me.apps.filter(function (app) {
					return _.find(apps, function (match) {
						return app.address.indexOf(match) !== -1 && app.icon.indexOf('/') === -1
					});
				});
				apps.push({
					displayName: 'directory',
					address: '/directory'
				})
			}
			else{
				apps = [appPrefix, 'workspace'];
			}

			var all = apps.length;
			apps.forEach(function(app){
				var appPrefix = app.address ? app.address.split('/')[1] : app;
				Behaviours.loadBehaviours(appPrefix, function(behaviours){
					if(behaviours.sniplets){
						sniplets.sniplets = sniplets.sniplets.concat(_.map(behaviours.sniplets, function(sniplet, name){ return { application: appPrefix, template: name, sniplet: sniplet } }));
						idiom.addBundle('/' + appPrefix + '/i18n');
					}
					all --;
					if(typeof callback === 'function' && all === 0){
						callback();
					}
				})
				.error(function(){
					all --;
				});
			});
		})
	},
	sniplets: []
};

function bootstrap(func) {
    if (currentLanguage === 'fr') {
        moment.lang(currentLanguage, {
            calendar: {
                lastDay: '[Hier à] HH[h]mm',
                sameDay: '[Aujourd\'hui à] HH[h]mm',
                nextDay: '[Demain à] HH[h]mm',
                lastWeek: 'dddd [dernier à] HH[h]mm',
                nextWeek: 'dddd [prochain à] HH[h]mm',
                sameElse: 'dddd LL'
            }
        });
    }
    else {
        moment.lang(currentLanguage);
    }

	if(window.notLoggedIn){
		Behaviours.loadBehaviours(appPrefix, function(){
			skin.loadDisconnected();
			func();
		})
		.error(function(){
			skin.loadDisconnected();
			func();
		});
		return;
	}
	http().get('/auth/oauth2/userinfo').done(function(data){
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
				return resource.owner && resource.owner.userId === model.me.userId;
			}

			var currentSharedRights = _.filter(resource.shared, function(sharedRight){
				return model.me.groupsIds.indexOf(sharedRight.groupId) !== -1
					|| sharedRight.userId === model.me.userId;
			});

			var resourceRight = _.find(currentSharedRights, function(resourceRight){
				return resourceRight[right.right] || resourceRight.manager;
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

		calendar.init();

		http().get('/userbook/preference/apps').done(function(data){
			if(!data.preference){
				data.preference = null;
			}
			model.me.bookmarkedApps = JSON.parse(data.preference) || [];
			var upToDate = true;
			var remove = [];
			model.me.bookmarkedApps.forEach(function(app, index){
				var foundApp = _.findWhere(model.me.apps, { name: app.name });
				var updateApp = true;
				if(foundApp){
					updateApp = JSON.stringify(foundApp) !== JSON.stringify(app);
					if(updateApp){
						for(var property in foundApp){
							app[property] = foundApp[property];
						}
					}
				}
				else{
					remove.push(app);
				}

				upToDate = upToDate && !updateApp;
			});
			remove.forEach(function(app) {
				var index = model.me.bookmarkedApps.indexOf(app);
				model.me.bookmarkedApps.splice(index, 1);
			});
			if(!upToDate){
				http().putJson('/userbook/preference/apps', model.me.bookmarkedApps);
			}

			func();
		});
	})
	.e404(function(){
		func();
	});
}
