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

import { routes, ng, template, idiom as lang, http, notify, Document, quota } from 'entcore';
import { workspace, containsFolder, folderToString } from './model';
import { _ } from 'entcore';
import { $ } from 'entcore';
import { moment } from 'entcore';
import { Mix } from 'entcore-toolkit';
import { importFiles } from './directives/import';

export let workspaceController = ng.controller('Workspace', ['$scope', '$rootScope', '$timeout', 'model', 'route', ($scope, $rootScope, $timeout, model, route) => {

	route({
		viewFolder: function(params){
			if($scope.lastRoute === window.location.href)
				return;
			$scope.lastRoute = window.location.href
			if($scope.initSequence && $scope.initSequence.executed){
		  		$scope.openFolderById(params.folderId)
			} else {
		  		$scope.initSequence = {
		        	executed: false,
		  			type: 'openFolder',
		  			folderId: params.folderId
		  		}
			}
		},
		viewSharedFolder: function(params){
			if($scope.lastRoute === window.location.href)
				return
			$scope.lastRoute = window.location.href;
		    $scope.currentTree = trees[1];
		    $scope.currentFolderTree = $scope.folder.children[1];
		    $scope.openFolder($scope.folder.children[1]);
		    if($scope.initSequence && $scope.initSequence.executed){
				$scope.openFolderById(params.folderId);
		    } else {
				$scope.initSequence = {
					executed: false,
					type: 'openSharedFolder',
		        	folderId: params.folderId
				};
			}
		},
		openShared: function(params){
			if($scope.lastRoute === window.location.href)
				return;
			$scope.lastRoute = window.location.href;
			$scope.currentTree = trees[1];
			$scope.currentFolderTree = $scope.folder.children[1];
			$scope.openFolder($scope.folder.children[1])
		}
	});

	$scope.lang = lang;
	$scope.model = model;
	$scope.display = {
	    nbFiles: 50
	};
	$scope.template = template;
	template.open('documents', 'icons');
	template.open('lightboxes', 'lightboxes');
	template.open('toaster', 'toaster');

	$rootScope.$on('share-updated', function(event, changes){
		if($scope.sharedDocuments)
			//Sharing documents
			$scope.openFolder($scope.openedFolder.folder)
		else{
			//Sharing folders
			var way = changes.added ? "added" : changes.removed ? "removed" : undefined
			var idField;

			if(way){
				var actions = changes.added.actions;
				idField = changes.added.groupId ? "groupId" : "userId";

				$scope.sharedFolders.forEach(function(folder){
					var sharedItem = _.find(folder.shared, function(item){
						return item[idField] === changes.added[idField]
					})
					if(!sharedItem){
						sharedItem = {}
						sharedItem[idField] = changes.added[idField]
						folder.shared = folder.shared ? folder.shared : []
						folder.shared.push(sharedItem);
					}
					if(way === "added"){
						_.each(actions, function(action){
							sharedItem[action] = true
						})
					} else {
						_.each(actions, function(action){
							delete sharedItem[action]
						})
					}
				})
			} else {
				idField = changes.groupId ? "groupId" : "userId"
				$scope.sharedFolders.forEach(function(folder){
					folder.shared = _.reject(folder.shared, function(item){ return item[idField] === changes[idField] })
				})
			}
			$scope.openFolder($scope.openedFolder.folder)
		}
	});

	$scope.folder = { children: [ { name: 'documents' }, { name: 'shared' }, { name: 'appDocuments' }, { name: 'trash', children: [] }] };
	$scope.users = [];
	$scope.me = model.me;

	$scope.folderTreeTemplate = 'folder-content';
	$scope.totalFilesSize = function(fileList){
		var size = 0
		if(!fileList.files)
			return size
		for(var i = 0; i < fileList.files.length; i++){
			size += fileList.files[i].size
		}
		return size
	}

	function formatDocuments(documents, callback){
		documents = _.filter(documents, function(doc){
			return doc.metadata['content-type'] !== 'application/json' &&
				(($scope.currentFolderTree.name !== 'trash' && doc.folder !== 'Trash') || ($scope.currentFolderTree.name === 'trash' && doc.folder === 'Trash'));
		});
		documents = documents.map((item) => {
			item = Mix.castAs(Document, item) as Document;
			item.rights.fromBehaviours();
			return item;
		});

		callback(documents);
	}

	$scope.inInterval = function(document, first, last){
		if(first < 0){
			first = 0;
		}
		for(var i = first; i <= last; i++){
			if($scope.openedFolder.content[i] === document){
				return true;
			}
		}
		return false;
	};

	$scope.viewFile = function(document){
		$scope.display.viewFile = document;
		template.open('documents', 'viewer');
	};

	$scope.downloadFile = function (document) {
	    window.location.href = '/workspace/document/' + document._id;
	};

	$scope.openNewFolderView = function(){
		$scope.newFolder = { name: '' };
		template.open('lightbox', 'create-folder');
	};

	$scope.targetDocument = {};
	$scope.openCommentView = function(document){
		$scope.targetDocument = document;
		template.open('lightbox', 'comment');
	};

	$scope.targetFolder = {};
	$scope.openCommentFolderView = function(folder){
		$scope.targetFolder = folder;
		template.open('lightbox', 'comment-folder');
	};

	$scope.openRenameView = function(document){
		document.newName = document.newProperties.name;
		$scope.renameTarget = document;
		template.open('lightbox', 'rename');
	};

	$scope.nbFolders = function(){
		if(!$scope.openedFolder.folder.children){
			return 0;
		}
		return $scope.openedFolder.folder.children.length;
	};

	$scope.openShareView = function(){
		$scope.sharedFolders = undefined
		$scope.sharedDocuments = $scope.selectedDocuments();
		$scope.display.share = true;
		template.open('share', 'share/share');
	};

	$scope.openShareFolderView = function(){
		$scope.sharedDocuments = undefined
		$scope.sharedFolders = $scope.selectedFolders();
		$scope.display.share = true;
		template.open('share', 'share/share-folders-warning');
	};

	var refreshFolders = function(){
		var folder = $scope.openedFolder;
		$scope.openedFolder = { folder: {} };

		setTimeout(function(){
			$scope.openedFolder = folder;
			$scope.$apply('openedFolder');
		}, 1);
	};

	$scope.deleteConfirm = function (url) {
	    template.open('lightbox', 'confirm-delete');
	    $scope.confirm = function () {
	        $scope.template.close('lightbox');
	        $scope.remove();
	    };
	};

	$scope.toTrashConfirm = function(url){
		template.open('lightbox', 'confirm');
		$scope.confirm = function(){
			$scope.selectedDocuments().forEach(function(document){
				http().put(url + "/" + document._id);
			});
			$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(doc){ return doc.selected; });

			notify.info('workspace.removed.message');
			template.close('lightbox');
		};
	};

	$scope.toTrash = function(url){
		$scope.selectedFolders().forEach(function(folder){
			http().put('/workspace/folder/trash/' + folder._id).done($scope.reloadFolderView);
			$scope.openedFolder.folder.children = _.reject($scope.openedFolder.folder.children, function(folder){
				return folder.selected;
			});
		});
		$scope.selectedDocuments().forEach(function(document){
			http().put(url + "/" + document._id)
			$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(doc){ return doc.selected; });
		});

		refreshFolders();
		notify.info('workspace.removed.message');
	};

	$scope.dragToTrash = function(item){

		if(item.file){
			http().put('/workspace/document/trash/' + item._id)
			$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(doc){ return doc._id === item._id; })
		} else {
			http().put('/workspace/folder/trash/' + item._id).done($scope.reloadFolderView)
			$scope.openedFolder.folder.children = _.reject($scope.openedFolder.folder.children, function(folder){
				return folder._id === item._id;
			});
		}

		refreshFolders();
		notify.info('workspace.removed.message');
	};

	$scope.openMoveFileView = function(action){
		targetFolders = [$scope.folder.children[0]];
		$scope.newFolder = { name: '' };
		template.open('lightbox', action);
	};

	$scope.remove = function(){
		$scope.selectedDocuments().forEach(function(document){
			$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(item){
				return item === document;
			});
			http().delete('document/' + document._id)
				.done(function(){
					quota.refresh();
				})
		});

		$scope.selectedFolders().forEach(function(folder){
			$scope.openedFolder.folder.children = _.reject($scope.openedFolder.folder.children, function(item){
				return item === folder;
			});
			http().delete('/workspace/folder/' + folder._id);
		})
	};

	$scope.restore = function(){
		$scope.selectedDocuments().forEach(function(document){
			$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(item){
				return item === document;
			});

			http().put('restore/document/' + document._id)
		});

		$scope.selectedFolders().forEach(function(folder){
			$scope.openedFolder.folder.children = _.reject($scope.openedFolder.folder.children, function(item){
				return item === folder;
			});

			http().put('/workspace/folder/restore/' + folder._id).done($scope.reloadFolderView);
		});
	};

	$scope.emptyTrash = function(){
		$scope.boxes.selectAll = true
		$scope.switchSelectAll()
		$scope.boxes.selectAll = false
		$scope.remove()
	};

	$scope.sendComment = function(){
		template.close('lightbox');
		http().post('document/' + $scope.targetDocument._id + '/comment', 'comment=' + encodeURIComponent($scope.targetDocument.comment)).done(function(result){
			if(!$scope.targetDocument.comments){
				$scope.targetDocument.comments = [];
			}
			$scope.targetDocument.comments.push({
				id: result.id,
				author: $scope.me.userId,
				authorName: $scope.me.username,
				comment: $scope.targetDocument.comment,
				posted: undefined
			});
			$scope.documentComment = $scope.targetDocument;
			$scope.targetDocument.comment = "";
			$scope.$apply();
		});
	};

	$scope.sendFolderComment = function(folder){
		if(folder){
			$scope.targetFolder = folder;
		}
		template.close('lightbox');
		http().post('folder/' + $scope.targetFolder._id + '/comment', 'comment=' + encodeURIComponent($scope.targetFolder.comment)).done(function(result){
			if(!$scope.targetFolder.comments){
				$scope.targetFolder.comments = [];
			}
			$scope.targetFolder.comments.push({
				id: result.id,
				author: $scope.me.userId,
				authorName: $scope.me.username,
				comment: $scope.targetFolder.comment,
				posted: undefined
			});
			$scope.folderComment = $scope.targetFolder;
			$scope.targetFolder.comment = "";
			$scope.$apply();
		});
	};

	$scope.removeComment = function(item, comment){
		http().delete('document/'+ item._id +'/comment/' + comment.id).done(function(){
			item.comments.splice(item.comments.indexOf(comment), 1)
			$scope.$apply()
		})
	}

	var trees = [{
		name: 'documents',
		path: 'documents',
		filter: 'owner',
		hierarchical: true,
		buttons: [
			{ text: 'workspace.add.document', action: () => $scope.display.importFiles = true, icon: true, workflow: 'workspace.create' }
		],
		contextualButtons: [
			{ text: 'workspace.move', action: $scope.openMoveFileView, url: 'move-files' },
			{ text: 'workspace.copy', action: $scope.openMoveFileView, url: 'copy-files' },
			{ text: 'workspace.move.trash', action: $scope.toTrash, url: 'document/trash' }
		]
	}, {
		name: 'trash',
		path: ['documents/Trash', 'documents'],
		filter: ['owner', 'protected'],
		buttons: [],
		contextualButtons: [
			{ text: 'workspace.trash.restore', action: $scope.restore },
			{ text: 'workspace.move.trash', action: $scope.deleteConfirm }
		]
	}, {
		name: 'shared',
		filter: 'shared',
		hierarchical: true,
		path: 'documents',
		buttons: [],
		contextualButtons: [
			{ text: 'workspace.move.racktodocs', action: $scope.openMoveFileView, url: 'copy-files', allow: function(){
				return $scope.selectedFolders().length === 0
  			} },
			{ text: 'workspace.move.trash', action: $scope.toTrash, url: 'document/trash', right: 'moveTrash' }
		]
	}, {
		name: 'appDocuments',
		filter: 'protected',
		hierarchical: false,
		path: 'documents',
		contextualButtons: [
			{ text: 'workspace.move.racktodocs', action: $scope.openMoveFileView, url: 'copy-files' },
			{ text: 'workspace.move.trash', action: $scope.toTrashConfirm, url: 'document/trash', right: 'moveTrash' }
		]
	}];

	var selection = [];
	$scope.selectedItems = function(){
		var sel = $scope.selectedDocuments().concat($scope.selectedFolders());
		if(sel.length != selection.length){
			selection = sel;
		}
		return selection;
	};

	$scope.selectedDocuments = function(){
		return _.where($scope.openedFolder.content, {selected: true});
	};

	$scope.selectedFolders = function(){
		return _.where($scope.openedFolder.folder.children, { selected: true });
	};

	$scope.openedFolder = {};

	$scope.loadFolderContent = function(path, folder, params, clear){
		if($scope.currentTree.hierarchical){
			params.hierarchical = true;
		}

		//Shared folders - special case
		var folderString = ''
		if(params.filter === "shared"){
			if(!folder.folder)
				delete params.hierarchical
			else{
				folderString = folder.folder
				params.owner = { userId: folder.owner }
			}
		} else {
			folderString = folderToString($scope.currentFolderTree, folder);
		}

		if(folderString !== ''){
			path += '/' + encodeURIComponent(folderString);
		}

		http().get(path, params).done(function(documents){
			if(clear){
				$scope.openedFolder.content = [];
			}

			//Shared folders - special case
			//Checks wether the document folder is shared or not.
			//Excldes documents that can be folded into shared folders
			if(params.filter === "shared" && !folderString){
				var recursiveCheck = function(root, doc){
					if(doc.folder === undefined)
						return false

					var documentFolder = doc.folder
					if(doc.folder === root.folder && doc.owner === root.owner)
						return true

					if(!root.children)
						return false

					var childCheck = false
					for(var i = 0; i < root.children.length; i++){
						if(!childCheck)
							childCheck = recursiveCheck(root.children[i], doc)
					}

					return childCheck
				}

				documents = _.reject(documents, function(document){
					return recursiveCheck($scope.currentFolderTree, document)
				})
			}

			formatDocuments(documents, function(result){
				$scope.openedFolder.content = $scope.openedFolder.content.concat(result);
				$scope.openedFolder.content.sort(function(a, b){
					if(moment(a.created) > moment(b.created)){
						return -1;
					}
					if(moment(a.created) < moment(b.created)){
						return 1;
					}
					return 0;
				});
				$scope.$apply();
			});
		});
	};

	$scope.openFolder = function(folder){

		if(template.contains('documents', 'viewer')){
			template.open('documents', 'icons');
		}

		$timeout(function(){
			$('body').trigger('whereami.update');
		}, 100)

		if(folder.folder && folder.folder.indexOf('Trash') === 0)
			return

		if(folder !== $scope.openedFolder.folder){
			$scope.loadingFiles = [];
		}

		$scope.openedFolder.folder = folder;
		if($scope.folder.children.indexOf(folder) !== -1){
			currentTree();
		}
		$scope.openedFolder.content = []
		if($scope.currentTree.filter instanceof Array){
			for(var i = 0; i < $scope.currentTree.filter.length; i++){
				var params = {
					filter: $scope.currentTree.filter[i]
				};
				$scope.loadFolderContent($scope.currentTree.path[i], folder, params);
			}
		}
		else{
			var params = {
				filter: $scope.currentTree.filter
			};
			$scope.loadFolderContent($scope.currentTree.path, folder, params, true);
		}
	};

	$scope.loadingFiles = [];

	$scope.cancelRequest = function(file){
		file.request.abort();
	};

	$scope.isUploadedImage = function(){
		return _.find($scope.newFile.chosenFiles, function(file){
		    return file.extension.toLowerCase() === 'png' || file.extension.toLowerCase() === 'jpg' ||
				file.extension.toLowerCase() === 'jpeg' || file.extension.toLowerCase() === 'bmp';
		}) !== undefined;
	};

	$scope.translate = function(key){
		return lang.translate(key);
	};

	$scope.longDate = function(dateString){
		if(!dateString){
			return moment().format('D MMMM YYYY');
		}

		return moment(dateString.split(' ')[0]).format('D MMMM YYYY');
	}

	$scope.shortDate = function(dateItem){
		if(!dateItem){
			return moment().format('L');
		}

		if(typeof dateItem === "number")
			return moment(dateItem).format('L');

		if(typeof dateItem === "string")
			return moment(dateItem.split(' ')[0]).format('L');

		return moment().format('L');
	}

	$scope.toggleComments = function(document){
		document.showComments = !document.showComments;
	};

	$scope.toggleFolderComments = function(folder){
		folder.showComments = !folder.showComments;
	};

	$scope.showComments = function(document, $event){
		if($event){
			$event.preventDefault();
		}
		$scope.targetDocument = document;
		$scope.selectedDocuments().forEach(function(document){
			document.selected = false;
			document.showComments = false;
		});
		$scope.selectedFolders().forEach(function(folder){
			folder.selected = false;
			folder.showComments = false;
		});

		document.selected = true;
		document.showComments = true;
	}

	$scope.showFolderComments = function(folder, $event){
		if($event){
			$event.preventDefault();
		}
		$scope.selectedFolders().forEach(function(folder){
			folder.selected = false;
			folder.showComments = false;
		});
		$scope.selectedDocuments().forEach(function(document){
			document.selected = false;
			document.showComments = false;
		});

		folder.selected = true;
		folder.showComments = true;
	};

	$scope.containsFolder = (container, child) => containsFolder(container, child);

	$scope.containsCurrentFolder = function(folder){
		return $scope.containsFolder(folder, $scope.openedFolder.folder);
	};

	function currentTree(){
		$scope.folder.children.forEach(function(tree){
			if($scope.containsCurrentFolder(tree)){
				$scope.currentTree = _.findWhere(trees, { name: tree.name });
				$scope.currentFolderTree = tree;
			}
		})

		return $scope.currentTree;
	}

	$scope.currentTree = trees[0];
	$scope.currentFolderTree = $scope.folder.children[0];
	$scope.openFolder($scope.folder.children[0]);

	var getFolders = function(tree, params, hook?){
		http().get('/workspace/folders/list', params).done(function(folders){
			_.sortBy(folders, function(folder){ return folder.folder.split("_").length }).forEach(function(folder){
				folder = new workspace.Folder(folder);
				folder.behaviours('workspace');
				folder.created = folder.created.split('.')[0] + ':' + folder.created.split('.')[1].substring(0, 2)
				if(folder.folder.indexOf('Trash') !== -1){
					if(_.where($scope.folder.children[$scope.folder.children.length - 1].children, { folder: folder.folder }).length === 0){
						$scope.folder.children[$scope.folder.children.length - 1].children.push(folder);
					}
					return;
				}

				var subFolders = folder.folder.split('_');
				var cursor = tree;

				if(tree.name === 'shared'){

					if(cursor === undefined){
						cursor = {};
					}
					if(cursor.children === undefined){
						cursor.children = [];
					}

					cursor.children.push(folder)

					if($scope.initSequence && !$scope.initSequence.executed && $scope.initSequence.type === 'openSharedFolder' && folder._id === $scope.initSequence.folderId){
						$scope.openFolder(folder)
						$scope.initSequence.executed = true
					}

				} else {

					subFolders.forEach(function(subFolder){
						if(cursor === undefined){
							cursor = {};
						}
						if(cursor.children === undefined){
							cursor.children = [];
						}
						if(_.where(cursor.children, { name: subFolder }).length === 0){
							cursor.children.push(folder)
						}
						cursor = _.findWhere(cursor.children, { name: subFolder });
					})

					if($scope.initSequence && !$scope.initSequence.executed && $scope.initSequence.type === 'openFolder' && folder._id === $scope.initSequence.folderId){
						$scope.openFolder(folder)
						$scope.initSequence.executed = true
					}
				}
			});

			if(tree.name === 'shared'){

				var recursiveFolderFolding = function(root){
					if(!root.children)
						return

					var spliceList = []

					for(var i = 0; i < root.children.length; i++){
						var folder = root.children[i]
						var subFolders = folder.folder.split('_');

						if(subFolders.length >= 2){
							var parentFolderName = subFolders[subFolders.length-2]
							var parentFolderPath = folder.folder.substring(0, folder.folder.lastIndexOf("_"))
							var parentOwner = folder.owner
							var parentFolder = _.findWhere(root.children, { name: parentFolderName, folder: parentFolderPath, owner: parentOwner })
							if(parentFolder){
								if(parentFolder.children === undefined){
									parentFolder.children = [];
								}
								parentFolder.children.push(folder)
								spliceList.push(folder)
							}
						}
					}

					for(i = 0; i < spliceList.length; i++)
						root.children.splice(root.children.indexOf(spliceList[i]), 1)

					for(i = 0; i < root.children.length; i++){
						recursiveFolderFolding(root.children[i])
					}
				}

				recursiveFolderFolding(tree)

			}

			if(typeof hook === 'function'){
				hook()
			}
			$scope.$apply()
		});
	};

	var targetFolders = [];

	$scope.addTargetFolder = function(folder){
		if(targetFolders.indexOf(folder) !== -1 || $scope.selectedFolders().indexOf(folder) !== -1){
			targetFolders = _.reject(targetFolders, function(el){
				return el === folder;
			});
		}
		else{
			targetFolders = [];
			targetFolders.push(folder);
		}
	};

	$scope.isTargetFolder = function(folder){
		return targetFolders.indexOf(folder) !== -1;
	};

	$scope.selectedFolder = { folder: {}, name: '' };
	$scope.setFolder = function(key, value){
		$scope.selectedFolder.name = key;
		$scope.selectedFolder.folder = value;
	};

	function updateFolders(hook?){
		getFolders($scope.folder.children[0], { filter: 'owner' }, hook);
	}

	function updateSharedFolders(){
		getFolders($scope.folder.children[1], { filter: 'shared' });
	}

	$scope.move = function(){
		template.close('lightbox');
		var folderString = folderToString($scope.folder.children[0], targetFolders[0]);

		var data = {} as any;
		if(folderString !== ''){
			data.path = folderString;
		}

		if($scope.selectedDocuments().length > 0){
			var selectedDocumentsIds = _.pluck($scope.selectedDocuments(), '_id').join(',');
			var basePath = 'documents/move/' + selectedDocumentsIds;
			if(folderString !== ''){
				basePath += '/' + encodeURIComponent(folderString);
			}

			http().put(basePath).done(function(){
				$scope.openFolder($scope.openedFolder.folder);
			});
		}
		$scope.selectedFolders().forEach(function(folder){
			$scope.openedFolder.folder.children = _.reject($scope.openedFolder.folder.children, function(child){
				return child._id === folder._id;
			});
			http().put('/workspace/folder/move/' + folder._id, data)
				.done(function(){
					$scope.reloadFolderView()
				})
				.e400(function(e){
					var error = JSON.parse(e.responseText);
					notify.error(error.error);
					$scope.openedFolder.folder.children.push(folder);
					refreshFolders();
				});
		})
	};

	$scope.dragMove = function(origin, target){
		template.close('lightbox');
		var folderString = folderToString($scope.folder.children[0], target);

		var data = {} as any;
		if(folderString !== ''){
			data.path = folderString;
		}

		if(origin.metadata){
			var basePath = 'documents/move/' + origin._id;
			if(folderString !== ''){
				basePath += '/' + encodeURIComponent(folderString);
			}

			http().put(basePath).done(function(){
				$scope.openFolder($scope.openedFolder.folder);
			});
		} else {
			$scope.openedFolder.folder.children = _.reject($scope.openedFolder.folder.children, function(child){
				return child._id === origin._id;
			});
			refreshFolders();
			http().put('/workspace/folder/move/' + origin._id, data)
			.done(function(){
				$scope.reloadFolderView()
			})
			.e400(function(e){
				var error = JSON.parse(e.responseText);
				notify.error(error.error);
				$scope.openedFolder.folder.children.push(origin);
				refreshFolders();
			});
		}
	};

	$scope.copy = function(){
		template.close('lightbox');
		var selectedDocumentsIds = _.pluck($scope.selectedDocuments(), '_id').join(',');
		targetFolders.forEach(function(folder){
			var basePath = 'documents';
			basePath += '/copy/' + selectedDocumentsIds;
			var folderString = folderToString($scope.folder.children[0], folder);
			if(folderString !== ''){
				basePath += '/' + encodeURIComponent(folderString);
			}

			if(selectedDocumentsIds.length > 0){
				http().post(basePath).done(function(){
					updateFolders();
				});
			}
			$scope.selectedFolders().forEach(function(folder){
				var param = { name: folder.name } as any;
				if(folderString !== ''){
					param.path = folderString;
				}
				http().put('/workspace/folder/copy/' + folder._id, param).done(function(){
					updateFolders();
				});
			})
		})
	};

	updateFolders();

	$scope.boxes = { selectAll: false }
	$scope.switchSelectAll = function(){
		$scope.openedFolder.content.forEach(function(document){
			document.selected = $scope.boxes.selectAll;
		});

		if($scope.openedFolder.folder.children){
			$scope.openedFolder.folder.children.forEach(function(folder){
				folder.selected = $scope.boxes.selectAll;
			});
		}
	};

	$scope.setAll = function () {
	    var all = true;
	    $scope.openedFolder.content.forEach(function (document) {
	        all = all && document.selected;
	    });

	    if ($scope.openedFolder.folder.children) {
	        $scope.openedFolder.folder.children.forEach(function (folder) {
	            all = all && folder.selected;
	        });
	    }

	    $scope.boxes.selectAll = all;
	};

	$scope.selectedFolder = { folder: {}, name: '' };
	$scope.setFolder = function(key, value){
		$scope.selectedFolder.name = key;
		$scope.selectedFolder.folder = value;
	};

	$scope.order = {
		field: 'created', desc: true
	}
	$scope.order.order = function(item){
		if($scope.order.field === 'created' && item.created){
			return moment(item.created);
		}
		if($scope.order.field === 'name'){
			return lang.removeAccents(item[$scope.order.field]);
		}
		if($scope.order.field.indexOf('.') >= 0){
			var splitted_field = $scope.order.field.split('.')
			var sortValue = item
			for(var i = 0; i < splitted_field.length; i++){
				sortValue = typeof sortValue === 'undefined' ? undefined : sortValue[splitted_field[i]]
			}
			return sortValue
		} else
			return item[$scope.order.field];
	}
	$scope.orderByField = function(fieldName){
		if(fieldName === $scope.order.field){
			$scope.order.desc = !$scope.order.desc;
		}
		else{
			$scope.order.desc = false;
			$scope.order.field = fieldName;
		}
	};

	updateFolders();
	updateSharedFolders();

	$scope.createFolder = function(){
		template.close('lightbox');
		var path = folderToString($scope.currentFolderTree, $scope.openedFolder.folder);
		if(path !== ''){
			$scope.newFolder.path = path;
		}

		http().post('/workspace/folder', $scope.newFolder).done(function(newFolder){
			updateFolders();
		})
		.e400(function(e){
			var error = JSON.parse(e.responseText);
			notify.error(error.error);
		});
	};

	$scope.isInSelectedFolder = function(folder){
		var result = false;
		var isInFolder = function(target){
			if(folder === target){
				return true;
			}

			if(!target.children){
				return false;
			}

			var result  = false;
			target.children.forEach(function(child){
				result = result || isInFolder(child)
			})
		}
		$scope.selectedFolders().forEach(function(targetFolder){
			result = result || isInFolder(targetFolder);
		});

		return result;
	}

	$scope.createEditFolder = function(){
		targetFolders.forEach(function(folder){
			var path = folderToString($scope.folder.children[0], folder);
			if(path !== ''){
				$scope.newFolder.path = path;
			}

			http().post('/workspace/folder', $scope.newFolder).done(function(newFolder){
				updateFolders();
				$scope.newFolder = { name: '' };
				targetFolders = []
			});
		});
	};

	$scope.to = {
		id: ''
	};


	$scope.editFolder = function(){
		$scope.newFolder.editing = true;
	}

	$scope.anyTargetFolder = function(){
		return targetFolders.length > 0;
	}

	//Given a data size in bytes, returns a more "user friendly" representation.
	$scope.getAppropriateDataUnit = function(bytes){
		var order = 0
		var orders = {
			0: lang.translate("byte"),
			1: "Ko",
			2: "Mo",
			3: "Go",
			4: "To"
		}
		var finalNb = bytes
		while(finalNb >= 1024 && order < 4){
			finalNb = finalNb / 1024
			order++
		}
		return {
			nb: finalNb,
			order: orders[order]
		}
	}

	$scope.formatDocumentSize = function(size){
		var formattedData = $scope.getAppropriateDataUnit(size)
		return (Math.round(formattedData.nb*10)/10)+" "+formattedData.order
	}

	$scope.rename = function(item, newName){
		template.close('lightbox');
		if(!item.file){
			//Rename folder
			http().putJson("/workspace/folder/rename/" + item._id, {name: newName}).done(function(){
				$scope.openedFolder.folder.children = []
				$scope.reloadFolderView()
			})
		} else {
			http().putJson("/workspace/rename/document/" + item._id, {name: newName}).done(function(){
				$scope.openFolder($scope.openedFolder.folder)
			})
		}
	}

	$scope.drag = function(item, $originalEvent){
		try{
			$originalEvent.dataTransfer.setData('application/json', JSON.stringify(item));
		} catch(e) {
			$originalEvent.dataTransfer.setData('Text', JSON.stringify(item));
		}
	};

	$scope.dragCondition = function(item){
		return $scope.currentFolderTree.name === 'documents' && item.folder
	}
	$scope.dropCondition = function(targetItem){
		return function(event){
			var dataField = event.dataTransfer.types.indexOf && event.dataTransfer.types.indexOf("application/json") > -1 ? "application/json" : //Chrome & Safari
							event.dataTransfer.types.contains && event.dataTransfer.types.contains("application/json") ? "application/json" : //Firefox
							event.dataTransfer.types.contains && event.dataTransfer.types.contains("Text") ? "Text" : //IE
							undefined

			if(!dataField || targetItem.name === 'shared' || targetItem.name === 'appDocuments')
				return false

			return dataField
		}
	}

	$scope.dropTo = function(targetItem, $originalEvent){
		var dataField = $scope.dropCondition(targetItem)($originalEvent);
		var originalItem = JSON.parse($originalEvent.dataTransfer.getData(dataField));

		if(originalItem._id === targetItem._id)
			return;

		if(targetItem.name === 'trash')
			$scope.dropTrash(originalItem);
		else
			$scope.dropMove(originalItem, targetItem);
	};

	$scope.dropMove = function(originalItem, targetItem){
		$scope.dragMove(originalItem, targetItem)
	}
	$scope.dropTrash = function(originalItem){
		$scope.dragToTrash(originalItem)
	}

	$scope.openFolderById = function(folderId){
		var recursive = function(root){
			if(root._id === folderId)
				return $scope.openFolder(root)

			if(!root.children || !root.children.length)
				return

			for(var i = 0; i < root.children.length; i++){
				recursive(root.children[i])
			}
		}
		return recursive($scope.currentFolderTree)
	}

	$scope.reloadFolderView = function(){
		var backupId = $scope.openedFolder.folder._id
		updateFolders(function(){
			$scope.folderTreeTemplate = ''
			$scope.$apply()
			$timeout(function(){
				$scope.folderTreeTemplate = 'folder-content'
				$scope.openFolderById(backupId)
			}, 10)
		})
	}

	$scope.refreshHistory = function(doc, hook){
		http().get("document/"+doc._id+"/revisions").done(function(revisions){
			doc.revisions = revisions
			if(typeof hook === 'function'){
				hook()
			}
			$scope.$apply()
		})
	}

	$scope.openHistory = function(document){
		$scope.targetDocument = document
		$scope.refreshHistory(document, function(){
			$scope.orderByField('date.$date');
			$scope.order.desc = true;
			template.open('lightbox', 'versions')
			$scope.$apply()
		})
	}

	$scope.revisionInProgress = {}
	$scope.createRevision = function(newFiles){
		if(newFiles.length < 1)
			return

		var data = new FormData()
		data.append("file", newFiles[0])

		http().bind('request-started.add-revision', function(){
			$scope.revisionInProgress.pending = true
			$scope.revisionInProgress.file = newFiles[0]
		});
		http().bind('request-ended.add-revision', function(){
			$scope.revisionInProgress = {}
		});

		http().putFile("document/" + $scope.targetDocument._id + "?thumbnail=120x120&thumbnail=290x290", data, {requestName: 'add-revision'}).done(function(){
			delete $scope.revisionInProgress;
			$scope.openFolder($scope.openedFolder.folder);
			quota.refresh();
			template.close('lightbox');
			//$scope.refreshHistory($scope.targetDocument);
		}).e400(function(e){
			delete $scope.revisionInProgress
			var error = JSON.parse(e.responseText);
			notify.error(error.error);
		});
	}

	$scope.deleteRevision = function(revision){
		http().delete("document/"+revision.documentId+"/revision/"+revision._id).done(function(){
			$('.tooltip').remove()
			$scope.openHistory($scope.targetDocument)
			quota.refresh();
		})
	}
}]);