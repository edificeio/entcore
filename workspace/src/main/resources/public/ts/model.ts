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

import { model, http, _ } from 'entcore';

export let workspace = {
	Folder: function(data?){},
	Tree: function(){}
}

export function containsFolder(container, child){
	var checkSubFolders = function(currentFolder){
		if(child === currentFolder){
			return true;
		}

		if(!currentFolder || !currentFolder.children){
			return;
		}

		for(var i = 0; i < currentFolder.children.length; i++){
			if(checkSubFolders(currentFolder.children[i])){
				return true;
			}
		}
	};

	return checkSubFolders(container);
}

export function folderToString(tree, folder){
	var folderString = '';
	function childString(cursor){
		var result = cursor.name;

		if(!cursor.children){
			return result;
		}

		for(var i = 0; i < cursor.children.length; i++){
			if(containsFolder(cursor.children[i], folder)){
				result = result + '_' + childString(cursor.children[i])
			}
		}

		return result;
	}

	var basePath = childString(tree);
	return _.reject(basePath.split('_'), function(path){ return path === tree.name }).join('_');
}

model.build = function(){
	this.makeModels(workspace);
	this.myDocuments = new workspace.Tree();
	this.trash = new workspace.Tree();
	this.appDocuments = new workspace.Tree();
	this.sharedDocuments = new workspace.Tree();
};