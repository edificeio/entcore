function User(){

}

function Document(){

}

function Folder(data){
	this.updateData(data);
	this.collection(Document,  {

	})
}


Model.build = function(){
	this.collection(User, {

	});

	this.collection(Folder, {
	});

	this.folders.rack = new Folder();
	this.folders.myDocuments = new Folder({
		children: [],
		appendFolderToParent: function(folder){
			var parentsFolders = folder.folder.split('_');
			if(parentsFolders)
			var parentName = parentsFolders[parentsFolders.length - 2];
			this.children.forEach(function(folder){
				if(parentsFolders.indexOf(folder.name) !== -1){

				}
			})
		}
	});
	this.folders.shared = new Folder();
	this.folders.trash = new Folder();
};