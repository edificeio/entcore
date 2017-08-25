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

import { model, http } from 'entcore';

export let workspace = {
	Folder: function(data?){},
	Tree: function(){},
	Quota: function(){}
}

workspace.Quota.prototype.sync = function(){
	http().get('/workspace/quota/user/' + model.me.userId).done(function(data){
		//to mo
		this.unit = 'mb';
		data.quota = data.quota / (1024 * 1024);
		data.storage = data.storage / (1024 * 1024);

		if(data.quota > 2000){
			data.quota = Math.round((data.quota / 1024) * 10) / 10;
			data.storage = Math.round((data.storage / 1024) * 10) / 10;
			this.unit = 'gb';
		}
		else{
			data.quota = Math.round(data.quota);
			data.storage = Math.round(data.storage);
			this.unit = 'mb';
		}

		this.max = data.quota;
		this.used = data.storage;
		this.trigger('change');
	}.bind(this));
};

model.build = function(){
	this.makeModels(workspace);
	this.myDocuments = new workspace.Tree();
	this.trash = new workspace.Tree();
	this.appDocuments = new workspace.Tree();
	this.sharedDocuments = new workspace.Tree();
	this.quota = new workspace.Quota();
	this.quota.sync();
};