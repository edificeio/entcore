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