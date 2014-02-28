function Application(data){
	this.transferSession = data.scope.indexOf('userinfo') !== -1;
}

model.build = function(){
	this.makeModels([Application])
	this.collection(Application, {
		sync: function(){
			http().get('/appregistry/public/json/applications.json').done(function(apps){
				this.load(apps);
			}.bind(this))
		}
	});
}