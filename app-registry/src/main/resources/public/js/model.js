//Copyright. Tous droits réservés. WebServices pour l’Education.
function Authorization(data){

}

function Application(data){
	if(data && data.scope){
		this.transferSession = data.scope.indexOf('userinfo') !== -1;
	}

	this.updateData(data);

	this.collection(Authorization, {

	});

	if(data.authorizations){
		this.authorizations.load(data.authorizations);
	}
}

function Role(data){
	this.switch = function(authorization){
		var existingAuth = this.authorizations.findWhere({ name: authorization.name });
		if(existingAuth){
			this.authorizations.remove(existingAuth);
		}
		else{
			this.authorizations.push(authorization);
		}
	};

	this.hasAuthorization = function(authorization){
		return this.authorizations.findWhere({ name: authorization.name }) !== undefined;
	};

	this.collection(Authorization, {

	});

	this.authorizations.load(data.authorizations);
}

model.build = function(){
	this.makeModels([Application, Role, Authorization]);

	this.collection(Application, {
		sync: function(){
			http().get('/appregistry/public/json/applications.json').done(function(apps){
				this.load(apps);
			}.bind(this))
		}
	});

	this.collection(Role, {
		sync: function(){
			http().get('/appregistry/public/json/roles.json').done(function(roles){
				this.load(roles);
			}.bind(this));
		},
		applicationRoles: function(application){
			return this.filter(function(role){
				return role.authorizations.find(function(auth){
					return application.authorizations.findWhere({ name: auth.name }) !== undefined;
				}) !== undefined;
			});
		}
	});
};