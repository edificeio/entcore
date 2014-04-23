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

function MyDocuments(){
	this.collection(Folder, {
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

	this.collection(Document,  {
		sync: function(){
			http().get('/workspace/documents', { filter: 'owner' }).done(function(documents){
				this.load(documents);
			})
		}
	});
}

function SharedDocuments(){
	this.collection(Document,  {
		sync: function(){
			http().get('/workspace/documents', { filter: 'shared' }).done(function(documents){
				this.load(documents);
			})
		}
	});
}

function AppDocuments(){
	this.collection(Document, {
		sync: function(){
			http().get('/workspace/documents', { filter: 'owner' }).done(function(documents){
				this.load(documents);
			})
		}
	})
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


model.build = function(){
	this.makeModels([User, Folder, Document, MyDocuments, SharedDocuments, Rack]);
	model.me.workflow.load(['workspace']);

	this.rack = new Rack();
	this.myDocuments = new MyDocuments();
	this.shared = new SharedDocuments();
	this.trash = new Folder();
};