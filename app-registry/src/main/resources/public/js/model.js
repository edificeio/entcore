//////// ACTION ////////

function Action(data){}

//////// APPLICATION ////////

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
        if(data.casType){
            data.hasCas = true
        }
        if(data.levelsOfEducation) {
			data.levelsOfEducation = data.levelsOfEducation.map(function(level) {
				return level.toString();
			});
		}
		this.updateData(data);

	}.bind(this));
};

Application.prototype.createApplication = function(){
	http().postJson('/appregistry/application', {
		grantType: this.grantType || '',
		displayName: this.displayName,
		secret: this.secret || '',
		address: this.address,
		icon: this.icon || '',
		target: this.target || '',
		scope: this.scope || '',
        casType: this.casType || '',
        pattern: this.pattern || '',
		name: this.name
	})
	.done(function(){
		model.applications.sync();
		notify.message('success', lang.translate('appregistry.notify.createApp'));
	})
    .e400(function(e){
		notify.error(e.responseJSON.error);
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
        casType: this.casType || '',
        pattern: this.pattern || '',
		name: this.name
	})
	.done(function(){
		model.applications.sync();
		notify.info(lang.translate('appregistry.notify.modified'));
	})
    .e400(function(e){
		notify.error(e.responseJSON.error);
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

Application.prototype.delete = function(callback){
	return http().delete('/appregistry/application/conf/' + this.id).done(function(){
		model.applications.sync();
		notify.info(lang.translate('appregistry.notify.deleteApp'));
        if(typeof callback === 'function')
            callback()
	})
}

//////// EXTERNAL APPLICATION ////////

function ExternalApplication(){}
ExternalApplication.prototype.lock = function(){
	return http().put('/appregistry/application/external/' + this.data.id + "/lock")
}
ExternalApplication.prototype.createApplication = function(structureId){
	return http().postJson('/appregistry/application/external?structureId=' + structureId, {
		grantType: this.data.grantType || '',
		displayName: this.data.displayName,
		secret: this.data.secret || '',
		address: this.data.address,
		icon: this.data.icon || '',
		target: this.data.target || '',
		scope: this.data.scope || '',
        casType: this.data.casType || '',
        pattern: this.data.pattern || '',
		name: this.data.name,
		inherits: this.data.inherits
	})
};
ExternalApplication.prototype.saveChanges = function(){
	return http().putJson('/appregistry/application/conf/' + this.data.id + '?structureId=' + this.data.structureId, {
		grantType: this.data.grantType || '',
		displayName: this.data.displayName,
		secret: this.data.secret || '',
		address: this.data.address,
		icon: this.data.icon || '',
		target: this.data.target || '',
		scope: this.data.scope || '',
        casType: this.data.casType || '',
        pattern: this.data.pattern || '',
		name: this.data.name,
		inherits: this.data.inherits
	})
};
ExternalApplication.prototype.save = function(structureId){
	if(this.data.target === 'adapter' && this.data.target.indexOf('/adapter#') === -1){
		this.data.target = '';
		this.data.address = '/adapter#' + this.data.address;
	}
	if(this.data.id){
		return this.saveChanges();
	}
	else{
		return this.createApplication(structureId);
	}
};
ExternalApplication.prototype.delete = function(){
    return http().delete('/appregistry/application/external/' + this.data.id)
}
ExternalApplication.prototype.massAuthorize = function(profiles){
    var profilesParams = ""
    profiles.forEach(function(p){
        if(profilesParams)
            profilesParams += "&profile=" + p
        else profilesParams += "?profile=" + p
    })
    return http().put('/appregistry/application/external/' + this.data.id + '/authorize' + profilesParams)
}
ExternalApplication.prototype.massUnauthorize = function(profiles){
    var profilesParams = ""
    profiles.forEach(function(p){
        if(profilesParams)
            profilesParams += "&profile=" + p
        else profilesParams += "?profile=" + p
    })
    return http().delete('/appregistry/application/external/' + this.data.id + '/authorize' + profilesParams)
}

//////// WIDGETS ////////

function WidgetApp(){}
WidgetApp.prototype.get = function(structureId, hook){
    return http().get('/appregistry/widget/' + this.id, { structureId: structureId }).done(function(data){
        this.infos = data
        if(typeof hook === 'function'){
            hook()
        }
    }.bind(this))
}
WidgetApp.prototype.delete = function(){
    return http().delete('/appregistry/widget/' + this.id)
}
WidgetApp.prototype.lock = function(){
    return http().put('/appregistry/widget/' + this.id + '/lock')
}
WidgetApp.prototype.linkWidget = function(groupId){
    return http().post('/appregistry/widget/' + this.id + '/link/' + groupId)
}
WidgetApp.prototype.unlinkWidget = function(groupId){
    return http().delete('/appregistry/widget/' + this.id + '/link/' + groupId)
}
WidgetApp.prototype.setMandatory = function(groupId){
    return http().put('/appregistry/widget/' + this.id + '/mandatory/' + groupId)
}
WidgetApp.prototype.removeMandatory = function(groupId){
    return http().delete('/appregistry/widget/' + this.id + '/mandatory/' + groupId)
}
WidgetApp.prototype.massAuthorize = function(structureId, profiles){
    var profilesParams = ""
    profiles.forEach(function(p){
        if(profilesParams)
            profilesParams += "&profile=" + p
        else profilesParams += "?profile=" + p
    })
    return http().put('/appregistry/widget/' + this.data.id + '/authorize/' + structureId + profilesParams)
}
WidgetApp.prototype.massUnauthorize = function(structureId, profiles){
    var profilesParams = ""
    profiles.forEach(function(p){
        if(profilesParams)
            profilesParams += "&profile=" + p
        else profilesParams += "?profile=" + p
    })
    return http().delete('/appregistry/widget/' + this.data.id + '/authorize/' + structureId + profilesParams)
}
WidgetApp.prototype.massSetMandatory = function(structureId, profiles){
    var profilesParams = ""
    profiles.forEach(function(p){
        if(profilesParams)
            profilesParams += "&profile=" + p
        else profilesParams += "?profile=" + p
    })
    return http().put('/appregistry/widget/' + this.data.id + '/mandatory/' + structureId + '/mass' + profilesParams)
}
WidgetApp.prototype.massRemoveMandatory = function(structureId, profiles){
    var profilesParams = ""
    profiles.forEach(function(p){
        if(profilesParams)
            profilesParams += "&profile=" + p
        else profilesParams += "?profile=" + p
    })
    return http().delete('/appregistry/widget/' + this.data.id + '/mandatory/' + structureId + '/mass' +  profilesParams)
}

//////// ROLE ////////

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
		if(typeof hook === "function") hook();
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
		if(typeof hook === "function") hook();
	})
}

Role.prototype.delete = function(hook){
	http().delete('/appregistry/role/' + this.id).done(function(){
		notify.info(lang.translate("appregistry.notify.deleteRole"))
		if(typeof hook === "function") hook();
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

	var actionsSum = []
	var equalityCheck = true

	_.forEach(role.appRoles, function(approle){
		approle.actions.forEach(function(action){
			if(_.findWhere(actionsSum, {displayName: action.displayName, name: action.name, type: action.type}) === undefined)
				actionsSum.push(action)
			if(_.findWhere(role.actions.all, {displayName: action.displayName, name: action.name, type: action.type}) === undefined)
				equalityCheck = false
		})
	})

	//If the new aggregation does not bring any changes
	if(equalityCheck && actionsSum.length === role.actions.all.length)
		return typeof hook === "function" ? hook() : null

	role.actions.all = actionsSum

	if(!this.id){
		this.createRole(hook, skipNotify)
	} else {
		this.updateRole(hook, skipNotify)
	}
}

//////// GROUP ////////

function Group(){}
Group.prototype.link = function(){
    return http().postJson('/appregistry/authorize/group', {
        groupId: this.id,
        roleIds: this.roles
    })
}
Group.prototype.addLink = function(roleId){
    return http().put('/appregistry/authorize/group/' + this.id + '/role/' + roleId)
}
Group.prototype.removeLink = function(roleId){
    return http().delete('/appregistry/authorize/group/' + this.id + '/role/' + roleId)
}

//////// SCHOOL ////////

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
School.prototype.syncExternalApps = function(hook){
	http().get('/appregistry/external-applications', {structureId: this.id}).done(function(data){
		this.externalApplications = _.map(data, function(item){
			var app = new ExternalApplication()
			app.updateData(item)
            if(app.data.address && app.data.address.indexOf('/adapter#') !== -1){
    			app.data.target = 'adapter';
    			app.data.address = app.data.address.split('/adapter#')[1];
    		}
			if(app.data.scope)
				app.data.transferSession = app.data.scope.indexOf('userinfo') !== -1
            if(app.data.casType){
                app.data.hasCas = true
            }
			return app
		})
		if(typeof hook === 'function')
			hook()
	}.bind(this))
}
School.prototype.toString = function(){ return this.name }

//////// MODEL BUILD ////////

model.build = function(){
	this.makeModels([Application, ExternalApplication, WidgetApp, Action, Role, School, Group]);

	this.collection(Application, {
		syncApps: function(hook){
			http().get('/appregistry/applications/actions?actionType=WORKFLOW&structureId=0').done(function(data){
				this.load(data)
				if(typeof hook === "function")
					hook()
			}.bind(this))
		}
	});

	this.collection(Role, {
		syncRoles: function(hook){
			http().get('/appregistry/roles/actions?structureId=0').done(function(data){
				this.load(_.map(data, function(role){
					return {
						id: role.id,
						name: role.name,
						distributions: role.distributions,
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
		organizeParents: function(){
			var that = this
			_.forEach(this.all, function(struct){
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
		},
		sync: function(){
			http().get('/directory/structure/admin/list').done(function(data){
				this.load(data)
				this.organizeParents()
			}.bind(this));
		}
	});

    this.collection(WidgetApp, {
        sync: function(){
            http().get('/appregistry/widgets').done(function(data){
				this.load(data.widgets)
                data.widgets.forEach(function(widget){
                    lang.addTranslations((widget.application.address ? widget.application.address : '') + widget.i18n)
                })
			}.bind(this));
        }
    })
};
