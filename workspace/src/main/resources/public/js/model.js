//Copyright. Tous droits réservés. WebServices pour l’Education.
function User(){

}

function Document(){

}

function Folder(data){
	this.updateData(data);

	this.collection(Folder, {
		sync: function(){
			this.load(model.myDocuments.folders.list.filter(function(folder){
				return folder.folder.indexOf(data.folder + '_') !== -1;
			}))
		}
	});

	this.collection(Document,  {
		sync: function(){
			http().get('/workspace/documents/' + data.folder, { filter: 'owner' }).done(function(documents){
				this.load(documents);
			}.bind(this));
		}
	});
}

function Rack(){
	this.collection(User, {
		sync: function(){
			http().get('/users/available-rack').done(function(users){
				this.load(users);
			});
		}
	})
}

function Trash(){

}

model.build = function(){
	this.makeModels([User, Rack]);
	this.makeModels(workspace);

	this.me.workflow.load(['workspace']);

	this.mediaLibrary = new Model();
	this.mediaLibrary.myDocuments = new workspace.MyDocuments();
	this.mediaLibrary.sharedDocuments = new workspace.SharedDocuments();
	this.mediaLibrary.appDocuments = new workspace.AppDocuments();

	this.rack = new Rack();
	this.trash = new Trash();
};