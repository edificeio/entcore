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
var tools = (function(){
	return {
		roleFromFileType: function(fileType){
			var types = {
				'doc': function(type){
					return type.indexOf('document') !== -1 && type.indexOf('wordprocessing') !== -1;
				},
				'xls': function(type){
					return (type.indexOf('document') !== -1 && type.indexOf('spreadsheet') !== -1) || (type.indexOf('ms-excel') !== -1);
				},
				'img': function(type){
					return type.indexOf('image') !== -1;
				},
				'pdf': function(type){
					return type.indexOf('pdf') !== -1 || type === 'application/x-download';
				},
				'ppt': function(type){
					return (type.indexOf('document') !== -1 && type.indexOf('presentation') !== -1) || type.indexOf('powerpoint') !== -1;
				},
				'video': function(type){
					return type.indexOf('video') !== -1;
				},
				'audio': function(type){
					return type.indexOf('audio') !== -1;
				},
				'zip': function(type){
					return 	type.indexOf('zip') !== -1 ||
							type.indexOf('rar') !== -1 ||
							type.indexOf('tar') !== -1 ||
							type.indexOf('7z') !== -1;
				}
			};

			for(var type in types){
				if(types[type](fileType)){
					return type;
				}
			}

			return 'unknown';
		},
		resolveMyRights: function(me){
			me.myRights = {

			}
		}
	}
}());

routes.define(function($routeProvider) {
	$routeProvider
	.when('/shared/folder/:folderId', {
		action: 'viewFolder'
	})
	.otherwise({
		redirectTo: '/'
	})
})

function Workspace($scope, date, ui, notify, _, route, $rootScope, $timeout){
	route({
		viewFolder: function(params){
			if($scope.lastRoute === window.location.href)
				return
			$scope.lastRoute = window.location.href
			$scope.currentTree = trees[1];
			$scope.currentFolderTree = $scope.folder.children[1];
			$scope.openFolder($scope.folder.children[1]);
			if($scope.initSequence && $scope.initSequence.executed){
				$scope.openFolderById(params.folderId)
			} else {
				$scope.initSequence = {
					executed: false,
					type: 'openSharedFolder',
					folderId: params.folderId
				}
			}
		}
	})

	$rootScope.$on('share-updated', function(event, changes){
		if($scope.sharedDocuments)
			//Sharing documents
			$scope.openFolder($scope.openedFolder.folder)
		else{
			//Sharing folders
			var way = changes.added ? "added" : changes.removed ? "removed" : undefined
			var idField

			if(way){
				var actions = changes.added.actions
				idField = changes.added.groupId ? "groupId" : "userId"

				$scope.sharedFolders.forEach(function(folder){
					var sharedItem = _.find(folder.shared, function(item){
						return item[idField] === changes.added[idField]
					})
					if(!sharedItem){
						sharedItem = {}
						sharedItem[idField] = changes.added[idField]
						folder.shared = folder.shared ? folder.shared : []
						folder.shared.push(sharedItem)
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

	$scope.maxQuota = 8;
	$scope.usedQuota = 4;

	$scope.quota = {
		max: 1,
		used: 0,
		unit: 'Mo'
	};

	$scope.folderTreeTemplate = 'folder-content'

	function getQuota(){
		http().get('/workspace/quota/user/' + model.me.userId).done(function(data){
			//to mo
			data.quota = data.quota / (1024 * 1024);
			data.storage = data.storage / (1024 * 1024);

			if(data.quota > 2000){
				data.quota = Math.round((data.quota / 1024) * 10) / 10;
				data.storage = Math.round((data.storage / 1024) * 10) / 10;
				$scope.quota.unit = 'Go';
			}
			else{
				data.quota = Math.round(data.quota);
				data.storage = Math.round(data.storage);
			}

			$scope.quota.max = data.quota;
			$scope.quota.used = data.storage;
			$scope.$apply('quota');
		});
	}

	getQuota();

	var setDocumentRights = function(document){
		document.myRights = {
			document: {
				remove: true,
				move: true,
				copy: true,
				moveTrash: true,
				share: true
			},
			comment: {
				post: true
			}
		};

		if(document.owner === $scope.me.userId){
			document.myRights.share = document.myRights.share &&
				_.where($scope.me.authorizedActions, { name: 'org.entcore.workspace.service.WorkspaceService|share'}).length > 0;
			return;
		}

		var currentSharedRights = _.filter(document.shared, function(sharedRight){
			return model.me.groupsIds.indexOf(sharedRight.groupId) !== -1
				|| sharedRight.userId === $scope.me.userId;
		});

		function setRight(path){
			return _.find(currentSharedRights, function(right){
				return right[path];
			}) !== undefined;
		}

		document.myRights.document.moveTrash = setRight('org-entcore-workspace-service-WorkspaceService|moveTrash');
		document.myRights.document.move = setRight('org-entcore-workspace-service-WorkspaceService|moveDocument');
		document.myRights.document.copy = setRight('org-entcore-workspace-service-WorkspaceService|moveDocument');
		document.myRights.comment.post = setRight('org-entcore-workspace-service-WorkspaceService|commentDocument');
		document.myRights.document.share = setRight('org-entcore-workspace-service-WorkspaceService|shareJsonSubmit');
	};

	$scope.documentPath = function(document){
		if($scope.currentTree.name === 'rack'){
			return '/workspace/rack/' + document._id;
		}
		else{
			return '/workspace/document/' + document._id;
		}
	};

	function formatDocuments(documents, callback){
		documents = _.filter(documents, function(doc){
			return doc.metadata['content-type'] !== 'application/json' &&
				(($scope.currentFolderTree.name !== 'trash' && doc.folder !== 'Trash') || ($scope.currentFolderTree.name === 'trash' && doc.folder === 'Trash'));
		});
		documents.forEach(function(item){
			if(item.created){
				item.created = item.created.split('.')[0] + ':' + item.created.split('.')[1].substring(0, 2);
			}
			else{
				item.created = item.sent.split('.')[0] + ':' + item.sent.split('.')[1].substring(0, 2);
			}
			item.metadata.contentType = tools.roleFromFileType(item.metadata['content-type']);
			var fileNameSplit = item.metadata.filename.split('.');
			item.metadata.extension = '';
			if(item.name.split('.').length > 1){
				item.metadata.extension = fileNameSplit[fileNameSplit.length - 1];
				item.name = item.name.split('.' + item.metadata.extension)[0];
			}

			if(item.from){
				item.ownerName = item.fromName;
				item.owner = item.from;
			}

			setDocumentRights(item);
		});

		callback(documents);
	}

	function folderToString(tree, folder){
		var folderString = '';
		function childString(cursor){
			var result = cursor.name;

			if(!cursor.children){
				return result;
			}

			for(var i = 0; i < cursor.children.length; i++){
				if($scope.containsFolder(cursor.children[i], folder)){
					result = result + '_' + childString(cursor.children[i])
				}
			}

			return result;
		}

		var basePath = childString(tree);
		return _.reject(basePath.split('_'), function(path){ return path === tree.name }).join('_');
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

	$scope.openNewDocumentView = function(){
		ui.showLightbox();
		$scope.loadingFiles = [];
		$scope.newFile = { name: $scope.translate('nofile'), chosenFiles: [] };
		$scope.currentViews.lightbox = $scope.views.lightbox.createFile;
	};

	$scope.openNewFolderView = function(){
		ui.showLightbox();
		$scope.newFolder = { name: '' };
		$scope.currentViews.lightbox = $scope.views.lightbox.createFolder;
	};

	$scope.targetDocument = {};
	$scope.openCommentView = function(document){
		$scope.targetDocument = document;
		ui.showLightbox();
		$scope.currentViews.lightbox = $scope.views.lightbox.comment;
	};

	$scope.openRenameView = function(document){
		$scope.newName = document.name
		$scope.renameTarget = document
		ui.showLightbox()
		$scope.currentViews.lightbox = $scope.views.lightbox.rename
	}

	$scope.workflowRight = function(name){
		var workflowRights = {
			renameFolder: 'org.entcore.workspace.service.WorkspaceService|renameFolder',
			renameDocument: 'org.entcore.workspace.service.WorkspaceService|renameDocument'
		};

		return _.where($scope.me.authorizedActions, { name: workflowRights[name] }).length > 0;
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
		ui.showLightbox();
		$scope.currentViews.lightbox = $scope.views.lightbox.share;
	};

	$scope.openShareFolderView = function(){
		$scope.sharedDocuments = undefined
		$scope.sharedFolders = $scope.selectedFolders();
		ui.showLightbox();
		$scope.currentViews.lightbox = $scope.views.lightbox.shareFoldersWarning;
	}

	var refreshFolders = function(){
		var folder = $scope.openedFolder;
		$scope.openedFolder = { folder: {} };

		setTimeout(function(){
			$scope.openedFolder = folder;
			$scope.$apply('openedFolder');
		}, 1);
	};

	$scope.toTrashConfirm = function(url){
		$scope.currentViews.lightbox = $scope.views.lightbox.confirm;
		ui.showLightbox();
		$scope.confirm = function(){
			$scope.selectedDocuments().forEach(function(document){
				http().put(url + "/" + document._id);
			});
			$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(doc){ return doc.selected; });

			notify.info('workspace.removed.message');
			ui.hideLightbox();
		};
	};

	$scope.toTrash = function(url){
		$scope.selectedFolders().forEach(function(folder){
			http().put('/workspace/folder/trash/' + folder._id);
		});
		$scope.selectedDocuments().forEach(function(document){
			http().put(url + "/" + document._id).done($scope.reloadFolderView)
		});
		$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(doc){ return doc.selected; });

		$scope.folder.children[$scope.folder.children.length - 1].children = $scope.folder.children[$scope.folder.children.length - 1].children.concat(
			_.where($scope.openedFolder.folder.children, { selected: true })
		);
		$scope.openedFolder.folder.children = _.reject($scope.openedFolder.folder.children, function(folder){
			return folder.selected;
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
			$scope.folder.children[$scope.folder.children.length - 1].children = $scope.folder.children[$scope.folder.children.length - 1].children.concat(
				_.where($scope.openedFolder.folder.children, { _id: item._id })
			);
			$scope.openedFolder.folder.children = _.reject($scope.openedFolder.folder.children, function(folder){
				return folder._id === item._id;
			});
		}

		refreshFolders();
		notify.info('workspace.removed.message');

	}

	$scope.openMoveFileView = function(action){
		targetFolders = [$scope.folder.children[0]];
		$scope.newFolder = { name: '' };
		ui.showLightbox();
		$scope.currentViews.lightbox = $scope.views.lightbox[action];
	};

	$scope.remove = function(){
		$scope.selectedDocuments().forEach(function(document){
			$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(item){
				return item === document;
			});
			http().delete('document/' + document._id)
				.done(function(){
					getQuota();
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

			http().put('/workspace/folder/restore/' + folder._id);
		});
		updateFolders();
	};

	$scope.emptyTrash = function(){
		$scope.boxes.selectAll = true
		$scope.switchSelectAll()
		$scope.boxes.selectAll = false
		$scope.remove()
	}

	$scope.sendComment = function(){
		ui.hideLightbox();
		http().post('document/' + $scope.targetDocument._id + '/comment', 'comment=' + $scope.targetDocument.comment).done(function(){
			if(!$scope.targetDocument.comments){
				$scope.targetDocument.comments = [];
			}
			$scope.targetDocument.comments.push({
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

	var trees = [{
		name: 'documents',
		path: 'documents',
		filter: 'owner',
		hierarchical: true,
		buttons: [
			{ text: 'workspace.add.document', action: $scope.openNewDocumentView, icon: true, allow: function(){
				return _.where($scope.me.authorizedActions, { name: 'org.entcore.workspace.service.WorkspaceService|addDocument'}).length > 0;
			} }
		],
		contextualButtons: [
			{ text: 'workspace.move', action: $scope.openMoveFileView, url: 'moveFile', contextual: true, allow: function(){ return true } },
			{ text: 'workspace.copy', action: $scope.openMoveFileView, url: 'copyFile', contextual: true, allow: function(){ return true } },
			{ text: 'workspace.move.trash', action: $scope.toTrash, url: 'document/trash', contextual: true, allow: function(){ return true } }
		]
	}, {
		name: 'trash',
		path: ['documents/Trash', 'documents'],
		filter: ['owner', 'protected'],
		buttons: [],
		contextualButtons: [
			{ text: 'workspace.trash.restore', action: $scope.restore, contextual: true, allow: function(){ return true } },
			{ text: 'workspace.move.trash', action: $scope.remove, contextual: true, allow: function(){ return true } }
		]
	}, {
		name: 'shared',
		filter: 'shared',
		hierarchical: true,
		path: 'documents',
		buttons: [],
		contextualButtons: [
			{ text: 'workspace.move.racktodocs', action: $scope.openMoveFileView, url: 'copyFile', contextual: true, allow: function(){ return $scope.selectedDocuments().length > 0 } },
			{ text: 'workspace.move.trash', action: $scope.toTrash, url: 'document/trash', contextual: true, allow: function(){
				return $scope.selectedFolders().length === 0 && _.find($scope.selectedDocuments(), function(doc){ return doc.myRights.document.moveTrash === false }) === undefined;
			} }
		]
	}, {
		name: 'appDocuments',
		filter: 'protected',
		hierarchical: false,
		path: 'documents',
		contextualButtons: [
			{ text: 'workspace.move.racktodocs', action: $scope.openMoveFileView, url: 'copyFile', contextual: true, allow: function(){ return true } },
			{ text: 'workspace.move.trash', action: $scope.toTrashConfirm, url: 'document/trash', contextual: true, allow: function(){
				return _.find($scope.selectedDocuments(), function(doc){ return doc.myRights.document.moveTrash === false }) === undefined;
			} }
		]
	}];

	$scope.selectedDocuments = function(){
		return _.where($scope.openedFolder.content, {selected: true});
	};

	$scope.selectedFolders = function(){
		return _.where($scope.openedFolder.folder.children, { selected: true });
	};

	$scope.views = {
		lightbox: {
			createFile: 'public/template/create-file.html',
			createFolder: 'public/template/create-folder.html',
			moveFile: 'public/template/move-files.html',
			copyFile: 'public/template/copy-files.html',
			comment: 'public/template/comment.html',
			share: 'public/template/share.html',
			shareFolders: 'public/template/share-folders.html',
			shareFoldersWarning: 'public/template/share-folders-warning.html',
			confirm: 'public/template/confirm.html',
			rename: 'public/template/rename.html'
		},
		documents: {
			list: 'public/template/list-view.html',
			icons: 'public/template/icons-view.html'
		}
	};

	$scope.currentViews = {
		lightbox: '',
		documents: $scope.views.documents.icons
	};

	$scope.switchView = function(view, value){
		$scope.currentViews[view] = $scope.views[view][value];
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
				params.ownerId = folder.owner
			}
		} else {
			folderString = folderToString($scope.currentFolderTree, folder);
		}

		if(folderString !== ''){
			path += '/' + folderString;
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

		})
	};

	$scope.openFolder = function(folder){
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
	$scope.addLoadingFiles = function(){
		var chosenNames = $scope.newFile.name.split(', ');
		$scope.newFile.chosenFiles.forEach(function(file, i){
			var formData = new FormData();
			var index = $scope.loadingFiles.length;

			if(chosenNames[i]){
				formData.append('file', file.file, chosenNames[i] + '.' + file.extension);
			}
			else{
				formData.append('file', file.file);
			}

			var url = 'document';
			var request = http().postFile(url + '?thumbnail=120x120',  formData, {
				requestName: 'file-upload-' + file.file.name + '-' + index
			})
				.done(function(e){
					var path = folderToString($scope.currentFolderTree, $scope.openedFolder.folder);
					if(path !== ''){
						http().put("documents/move/" + e._id + '/' + path).done(function(){
							$scope.openFolder($scope.openedFolder.folder);
						});
					}
					else{
						$scope.openFolder($scope.openedFolder.folder);
					}

					getQuota();
				}).xhr;

			$scope.loadingFiles.push({
				file: file.file,
				request: request
			});
		});
		$scope.newFile.blockIdentical = true;

	}
	$scope.confirmIdentical = function(){
		$scope.newFile.blockIdentical = false;
	}

	$scope.translate = function(key){
		return lang.translate(key);
	};

	$scope.longDate = function(dateString){
		if(!dateString){
			return moment().format('D MMMM YYYY');
		}

		return date.format(dateString.split(' ')[0], 'D MMMM YYYY')
	}

	$scope.toggleComments = function(document){
		document.showComments = !document.showComments;
	};

	$scope.showComments = function(document, $event){
		if($event){
			$event.preventDefault();
		}
		$scope.selectedDocuments().forEach(function(document){
			document.selected = false;
			document.showComments = false;
		});

		document.selected = true;
		document.showComments = true;

	}

	$scope.$watch('targetDocument', function(newVal){
		console.log(newVal);
	})

	$scope.containsFolder = function(container, child){
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

	var getFolders = function(tree, params, hook){
		http().get('/workspace/folders/list', params).done(function(folders){
			_.sortBy(folders, function(folder){ return folder.folder.split("_").length }).forEach(function(folder){
				setDocumentRights(folder);

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

	function updateFolders(hook){
		getFolders($scope.folder.children[0], { filter: 'owner' }, hook);
	}

	function updateSharedFolders(){
		getFolders($scope.folder.children[1], { filter: 'shared' });
	}

	$scope.move = function(){
		ui.hideLightbox();
		var folderString = folderToString($scope.folder.children[0], targetFolders[0]);

		var data = {};
		if(folderString !== ''){
			data.path = folderString;
		}

		if($scope.selectedDocuments().length > 0){
			var selectedDocumentsIds = _.pluck($scope.selectedDocuments(), '_id').join(',');
			var basePath = 'documents/move/' + selectedDocumentsIds;
			if(folderString !== ''){
				basePath += '/' + folderString;
			}

			http().put(basePath).done(function(){
				$scope.openFolder($scope.openedFolder.folder);
			});
		}
		$scope.selectedFolders().forEach(function(folder){
			$scope.openedFolder.folder.children = _.reject($scope.openedFolder.folder.children, function(child){
				return child._id === folder._id;
			});
			refreshFolders();
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
		ui.hideLightbox();
		var folderString = folderToString($scope.folder.children[0], target);

		var data = {};
		if(folderString !== ''){
			data.path = folderString;
		}

		if(origin.file){
			var basePath = 'documents/move/' + origin._id;
			if(folderString !== ''){
				basePath += '/' + folderString;
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
	}

	$scope.copy = function(){
		ui.hideLightbox();
		var selectedDocumentsIds = _.pluck($scope.selectedDocuments(), '_id').join(',');
		targetFolders.forEach(function(folder){
			var basePath = 'documents';
			basePath += '/copy/' + selectedDocumentsIds;
			var folderString = folderToString($scope.folder.children[0], folder);
			if(folderString !== ''){
				basePath += '/' + folderString;
			}

			if(selectedDocumentsIds.length > 0){
				http().post(basePath).done(function(){
					updateFolders();
				});
			}
			$scope.selectedFolders().forEach(function(folder){
				var param = { name: folder.name };
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

	$scope.setFilesName = function(){
		$scope.newFile.name = '';
		$scope.newFile.chosenFiles = [];
		$scope.newFile.blockIdentical = false;
		var checkAlreadyLoaded = function(newFile){
			_.forEach($scope.loadingFiles, function(loadedFile){
				var fileName = loadedFile.file.name
				var fileSize = loadedFile.file.size
				if(newFile.file.name === fileName && newFile.file.size === fileSize)
					$scope.newFile.blockIdentical = true
			})
		}

		for(var i = 0; i < $scope.newFile.files.length ; i++){
			var file = $scope.newFile.files[i];
			var splitList = file.name.split('.');
			var extension = splitList[splitList.length - 1];

			var newFile = { file: file, name: file.name.split('.' + extension)[0] };
			if($scope.newFile.name !== ''){
				$scope.newFile.name = $scope.newFile.name + ', ';
			}
			$scope.newFile.name = $scope.newFile.name + file.name.split('.' + extension)[0];
			if(splitList.length > 1){
				newFile.extension = extension;
			}
			else{
				newFile.extension = '';
			}
			$scope.newFile.chosenFiles.push(newFile);
			checkAlreadyLoaded(newFile)
		}
	};

	$scope.createFolder = function(){
		ui.hideLightbox();
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
		return (Math.round(formattedData.nb*100)/100)+" "+formattedData.order
	}

	$scope.rename = function(item, newName){
		ui.hideLightbox();
		if(!item.file){
			//Rename folder
			http().putJson("/workspace/folder/rename", {id: item._id, name: newName}).done(function(){
				$scope.openedFolder.folder.children = []
				$scope.reloadFolderView()
			})
		} else {
			//Rename file
			http().putJson("/workspace/rename/document", {id: item._id, name: newName}).done(function(){
				$scope.openFolder($scope.openedFolder.folder)
			})
		}
	}

	$scope.drag = function(item, event){
		return function(event){
			try{
				event.dataTransfer.setData('application/json', JSON.stringify(item));
			} catch(e) {
				event.dataTransfer.setData('Text', JSON.stringify(item));
			}
		}
	}
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

	$scope.dropTo = function(targetItem){
		return function(event){
			event.preventDefault()

			var dataField = $scope.dropCondition(targetItem)(event)
			var originalItem = JSON.parse(event.dataTransfer.getData(dataField))

			if(originalItem._id === targetItem._id)
				return

			if(targetItem.name === 'trash')
				$scope.dropTrash(originalItem)
			else
				$scope.dropMove(originalItem, targetItem)
		}
	}

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
}
