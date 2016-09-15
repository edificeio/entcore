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
function Document(item){
	var fileNameSplit = item.metadata.filename.split('.');
	this.metadata.extension = '';
	if(item.name.split('.').length > 1){
		this.metadata.extension = fileNameSplit[fileNameSplit.length - 1];
		this.name = item.name.split('.' + item.metadata.extension)[0];
	}
	this.owner = { userId: item.owner };

	if(item.created){
		this.created = item.created.split('.')[0] + ':' + item.created.split('.')[1].substring(0, 2);
	}
	else{
		this.created = item.sent.split('.')[0] + ':' + item.sent.split('.')[1].substring(0, 2);
	}

	this.metadata.contentType = Document.prototype.roleFromFileType(item.metadata['content-type']);
	this.link = '/workspace/document/' + item._id;
	if(this.metadata.contentType === 'img'){
		this.icon = this.link;
	}
	this.version = parseInt(Math.random() * 100);
}

Document.prototype.roleFromFileType = function(fileType) {
	var types = {
		'doc': function (type) {
			return type.indexOf('document') !== -1 && type.indexOf('wordprocessing') !== -1;
		},
		'xls': function (type) {
			return (type.indexOf('document') !== -1 && type.indexOf('spreadsheet') !== -1) || (type.indexOf('ms-excel') !== -1);
		},
		'img': function (type) {
			return type.indexOf('image') !== -1;
		},
		'pdf': function (type) {
			return type.indexOf('pdf') !== -1 || type === 'application/x-download';
		},
		'ppt': function (type) {
			return (type.indexOf('document') !== -1 && type.indexOf('presentation') !== -1) || type.indexOf('powerpoint') !== -1;
		},
		'video': function (type) {
			return type.indexOf('video') !== -1;
		},
		'audio': function (type) {
			return type.indexOf('audio') !== -1;
		},
		'zip': function (type) {
			return type.indexOf('zip') !== -1 ||
				type.indexOf('rar') !== -1 ||
				type.indexOf('tar') !== -1 ||
				type.indexOf('7z') !== -1;
		}
	};

	for (var type in types) {
		if (types[type](fileType)) {
			return type;
		}
	}

	return 'unknown';
};

function Folder(){

}

function Tree(){
}

function Quota(){

}

Quota.prototype.sync = function(){
	http().get('/workspace/quota/user/' + model.me.userId).done(function(data){
		//to mo
		this.unit = 'mb';
		this.isStructureQuota = false; // is used to indicate in the workspace if the insuffisciency of storage left is due to the quota of the structure

		// if the remaining storage of the user is larger than the remaining storage of the structure, then we make our calculations
		// on the structure.
		if( data.quota - data.storage > data.quotastructure - data.storagestructure ) {
			data.quota = data.quotastructure;
			data.storage = data.storagestructure;
			this.isStructureQuota = true;
		}

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
	this.makeModels([Document, Folder, Tree, Quota]);
	this.myDocuments = new Tree();
	this.trash = new Tree();
	this.appDocuments = new Tree();
	this.sharedDocuments = new Tree();
	this.quota = new Quota();
	this.quota.sync();
};
