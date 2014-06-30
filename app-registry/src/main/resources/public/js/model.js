//Copyright. Tous droits réservés. WebServices pour l’Education.
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
		data.result[0].target = data.result[0].target || '';
		if(data.result[0].address && data.result[0].address.indexOf('/adapter#') !== -1){
			data.result[0].target = 'adapter';
			data.result[0].address = data.result[0].address.split('/adapter#')[1];
		}
		this.updateData(data.result[0]);
	}.bind(this));
};

Application.prototype.createApplication = function(){
	http().post('/appregistry/application/external', {
		grantType: this.grantType,
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
		notify.success('Application créée');
	});
};

Application.prototype.saveChanges = function(){
	http().post('/appregistry/application/conf', {
		applicationId: this.id,
		grantType: this.grantType,
		displayName: this.displayName,
		secret: this.secret,
		address: this.address,
		icon: this.icon,
		target: this.target,
		scope: this.scope,
		name: this.name
	})
	.done(function(){
		model.applications.sync();
		notify.info('Modifications enregistrées');
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
}

Role.prototype.createRole = function(){
	http().post('/appregistry/role', { role: this.name, actions:
		_.map(this.actions.all, function(action){
			return action.name;
		}).join(',')
	})
		.done(function(){
			model.roles.sync();
		});
};

Role.prototype.save = function(){
	if(!this.id){
		this.createRole();
	}
};

model.build = function(){
	this.makeModels([Application, Role, Action]);

	this.collection(Application, {
		sync: function(){
			http().get('/appregistry/applications/actions?actionType=WORKFLOW').done(function(data){
				this.load(_.map(data.result, function(app){
					return app;
				}));
			}.bind(this))
		}
	});

	this.collection(Role, {
		sync: function(){
			http().get('/appregistry/roles/actions').done(function(data){
				this.load(_.map(data.result, function(role){
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
				}));
			}.bind(this));
		},
		applicationRoles: function(application){
			return this.filter(function(role){
				return role.actions.find(function(action){
					return application.actions.findWhere({ name: action.name }) !== undefined;
				}) !== undefined;
			});
		}
	});
};