import { http } from './http';
import { idiom as lang } from './idiom';
import { ui } from './ui';
import { notify } from './notify';
import { Behaviours } from './behaviours';
import { calendar } from './calendar';
import { currentLanguage, appPrefix, infraPrefix } from './globals';
import { Collection, Model, model } from './modelDefinitions';
import { skin } from './skin';

var _ = require('underscore');
var moment = require('moment');

(function(){
	function pluralizeName(obj){
        var name = (obj.name || obj._name);
        if (!name) {
            name = obj.toString().split('function')[1].split('(')[0].trim()
        }
		if(name[name.length - 1] === 'y' && name[name.length - 2] !== 'a' && name[name.length - 2] !== 'e'){
			return name[0].toLowerCase() + name.substr(1, name.length - 2) + 'ies';
		}
		return name[0].toLowerCase() + name.substr(1) + 's';
	}

    Collection.prototype = {
        reject: undefined,
		obj: undefined,
		callbacks: {},
		sync: undefined,
		all: [],
        composer: undefined,
        map: undefined,
        find: undefined,
        filter: undefined,
        findWhere: undefined,
        where: undefined,
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
			_.filter(this.all, predicate).forEach(function(item){
				item.selected = true;
			});
		},
		deselect: function(predicate){
			_.filter(this.all, predicate).forEach(function(item){
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
		splice: function(...args){
			this.all.splice.apply(this.all, args);
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
					var newObj = (Model as any).create(that.obj, item);
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
        if(ctr.name === undefined){
            ctr.name = fn.name;
        }

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

	(Model as any).create = function(func, data){
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

export function bootstrap(func) {
    if (currentLanguage === 'fr') {
        moment.updateLocale(currentLanguage, {
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
        moment.locale(currentLanguage);
    }

	if((window as any).notLoggedIn){
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
	http().get('/auth/oauth2/userinfo').done(async (data): Promise<void> => {
		await skin.loadConnected();
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
			let rightName = right.right || right;

			var currentSharedRights = _.filter(resource.shared, function(sharedRight){
				return model.me.groupsIds.indexOf(sharedRight.groupId) !== -1
					|| sharedRight.userId === model.me.userId;
			});

			var resourceRight = _.find(currentSharedRights, function(resourceRight){
				return resourceRight[rightName] || resourceRight.manager;
			}) !== undefined;

			var workflowRight = true;
			if(right.workflow){
				workflowRight = this.hasWorkflow(right.workflow);
			}

			return resourceRight && workflowRight;
		};

		model.me.workflow = {
			load: async function(services): Promise<void>{
				for(let service of services){
					let workflows = await Behaviours.findWorkflow(service);
					console.log('Workflows loaded from ' + service);
					console.log(workflows);
					this[service] = workflows;
				}
			}
		};

		await model.me.workflow.load(['workspace', appPrefix]);
		model.trigger('me.change');

		calendar.init();
		await skin.loadBookmarks();
		func();
	})
	.e404(function(){
		func();
	});
}
