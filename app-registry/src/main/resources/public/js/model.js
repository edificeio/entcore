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
function Action(data){

}

function Application(data){
	this.grantType = 'authorization_code';
	if(data && data.scope){
		this.transferSession = data.scope.indexOf('userinfo') !== -1;
	}

	this.collection(Action, {

	});

	if(data.actions){
		this.actions.load(_.map(data.actions, function(action){
			return {
				name: action[0],
				displayName: action[1],
				type: action[2]
			}
		}));
	}
}

Application.prototype.open = function(){
	if(!this.id){
		return;
	}

	http().get('/appregistry/application/conf/' + this.id).done(function(data){
		if(!data)
			return;

		data.target = data.target || '';
		if(data.address && data.address.indexOf('/adapter#') !== -1){
			data.target = 'adapter';
			data.address = data.address.split('/adapter#')[1];
		}
		this.updateData(data);

	}.bind(this));
};

Application.prototype.createApplication = function(){
	http().postJson('/appregistry/application/external', {
		grantType: this.grantType || '',
		displayName: this.displayName,
		secret: this.secret || '',
		address: this.address,
		icon: this.icon || '',
		target: this.target || '',
		scope: this.scope || '',
		name: this.name
	})
	.done(function(){
		model.applications.sync();
		notify.message('success', lang.translate('appregistry.notify.createApp'));
	});
};

Application.prototype.saveChanges = function(){
	http().putJson('/appregistry/application/conf/'+this.id, {
		grantType: this.grantType || '',
		displayName: this.displayName,
		secret: this.secret || '',
		address: this.address,
		icon: this.icon || '',
		target: this.target || '',
		scope: this.scope || '',
		name: this.name
	})
	.done(function(){
		model.applications.sync();
		notify.info(lang.translate('appregistry.notify.modified'));
	});
};

Application.prototype.save = function(){
	if(this.target === 'adapter' && this.target.indexOf('/adapter#') === -1){
		this.target = '';
		this.address = '/adapter#' + this.address;
	}
	if(this.id){
		this.saveChanges();
	}
	else{
		this.createApplication();
	}
};

Application.prototype.delete = function(){
	http().delete('/appregistry/application/conf/'+this.id).done(function(){
		model.applications.sync();
		notify.info(lang.translate('appregistry.notify.deleteApp'));
	})
}

function Role(data){
	this.switch = function(action){
		var existingAuth = this.actions.findWhere({ name: action.name });
		if(existingAuth){
			this.actions.remove(existingAuth);
		}
		else{
			this.actions.push(action);
		}
	};

	this.hasAction = function(action){
		return this.actions.findWhere({ name: action.name }) !== undefined;
	};

	this.collection(Action, {

	});

	if(data){
		this.actions.load(data.actions);
	}

	this.crossSwitch = function(approle){
		role = this
		var index = role.appRoles.indexOf(approle)
		if(index >= 0)
			role.appRoles.splice(index, 1)
		else
			role.appRoles.push(approle)
	}

	this.crossRoleContains = function(approle){
		var role = this
		return approle.actions.every(function(action){
			return role.actions.find(function(item){ return item.name === action.name && item.type === action.type && item.displayName === action.displayName})
		})
	}
}

Role.prototype.createRole = function(hook){
	http().postJson('/appregistry/role', { role: this.name, actions:
		_.map(this.actions.all, function(action){
			return action.name;
		})
	}).done(function(){
		notify.message('success', lang.translate("appregistry.notify.createRole"))
		typeof hook === "function" ? hook() : null
	});
}

Role.prototype.updateRole = function(hook, skipNotify){
	http().putJson('/appregistry/role/' + this.id, {
		role: this.name,
		actions: _.map(this.actions.all, function(action){
			return action.name;
		})
	}).done(function(){
		if(!skipNotify)
			notify.info(lang.translate("appregistry.notify.modifyRole"))
		typeof hook === "function" ? hook() : null
	})
}

Role.prototype.delete = function(hook){
	http().delete('/appregistry/role/' + this.id).done(function(){
		notify.info(lang.translate("appregistry.notify.deleteRole"))
		typeof hook === "function" ? hook() : null
	})
}

Role.prototype.save = function(hook){
	if(!this.id){
		this.createRole(hook)
	} else
		this.updateRole(hook)
}

Role.prototype.saveCross = function(hook, skipNotify){
	//Aggregate app-roles actions before saving
	var role = this
	role.actions.all = []
	_.forEach(role.appRoles, function(approle){
		approle.actions.forEach(function(action){
			if(role.actions.indexOf(action) < 0)
				role.actions.push(action)
		})
	})
	if(!this.id){
		this.createRole(hook, skipNotify)
	} else {
		this.updateRole(hook, skipNotify)
	}
}

function Group(){
	this.link = function(){
		http().postJson('/appregistry/authorize/group', {
			groupId: this.id,
			roleIds: this.roles
		})
	}
}

function School(){
	this.collection(Group, {
		sync: function(hook){
			http().get('/appregistry/groups/roles', { structureId: this.model.id }).done(function(groups){
				this.load(groups)
				if(typeof hook === 'function')
					hook()
			}.bind(this));
		}
	});
}

model.build = function(){
	this.makeModels([Application, Role, Action, School, Group]);

	this.collection(Application, {
		sync: function(){
			http().get('/appregistry/applications/actions?actionType=WORKFLOW').done(function(data){
				this.load(data)
			}.bind(this))
		}
	});

	this.collection(Role, {
		sync: function(hook){
			http().get('/appregistry/roles/actions').done(function(data){
				this.load(_.map(data, function(role){
					return {
						id: role.id,
						name: role.name,
						actions: _.map(role.actions, function(action){
							return {
								name: action[0],
								displayName: action[1],
								type: action[2]
							};
						})
					};
				}))
				if(typeof hook === "function")
					hook()
			}.bind(this));
		},
		applicationRoles: function(application){
			return this.filter(function(role){
				return role.actions.find(function(action){
					return application.actions.findWhere({ name: action.name }) !== undefined;
				}) !== undefined;
			});
		},
		applicationRolesExclusive: function(application){
			return this.filter(function(role){
        		return role.actions.every(function(action){
            		return application.actions.findWhere({name: action.name})
        		})
    		})
		},
		crossAppRoles: function(applications){
			return this.filter(function(role){
   				return applications.every(function(app){
      				return !role.actions.every(function(action){
          				return app.actions.findWhere({name: action.name})
      				})
   				})
			})
		}
	});

	this.collection(School, {
		sync: function(){
			var that = this
			http().get('/directory/structure/admin/list').done(function(data){
				that.load(data)
				//this.forEach(function(school){ school.sync() })
				_.forEach(that.all, function(struct){
					struct.parents = _.filter(struct.parents, function(parent){
						var parentMatch = _.findWhere(that.all, {id: parent.id})
						if(parentMatch){
							parentMatch.children = parentMatch.children ? parentMatch.children : []
							parentMatch.children.push(struct)
							return true
						} else
							return false
					})
					if(struct.parents.length === 0)
						delete struct.parents
				})
			}.bind(this));
		}
	});
};
