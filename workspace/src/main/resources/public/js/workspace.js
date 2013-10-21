var tools = (function(){
	return {
		roleFromFileType: function(fileType){
			var types = {
				'doc': function(type){
					return type.indexOf('document') !== -1 && type.indexOf('wordprocessing') !== -1;
				},
				'xls': function(type){
					return type.indexOf('document') !== -1 && type.indexOf('spreadsheet') !== -1;
				},
				'img': function(type){
					return type.indexOf('image') !== -1;
				},
				'pdf': function(type){
					return type.indexOf('pdf') !== -1;
				},
				'ppt': function(type){
					return type.indexOf('document') !== -1 && type.indexOf('presentation') !== -1;
				},
				'video': function(type){
					return type.indexOf('video') !== -1;
				},
				'audio': function(type){
					return type.indexOf('audio') !== -1;
				}
			}

			for(var type in types){
				if(types[type](fileType)){
					return type;
				}
			}

			return 'unknown';
		}
	}
}());

function Workspace($scope, http, lang, date, ui, notify, _){
	$scope.folders = { documents: {}, rack: {}, trash: {}, shared: {} };
	$scope.users = [];

	$scope.documentPath = function(document){
		if($scope.currentTree.name === 'rack'){
			return '/workspace/rack/' + document._id;
		}
		else{
			return '/workspace/document/' + document._id;
		}
	}

	function formatDocuments(documents, callback){
		var findAuthorizations = function(authorizations, owner){
			if(!(authorizations instanceof Array) || owner === $scope.me.userId){
				return {
					comment: true,
					share: true
				}
			}

			var myRights = {
				comment: false,
				share: false
			}

			authorizations.forEach(function(auth){
				if(auth.userId !== $scope.me.userId){
					return;
				}

				for(var property in auth){
					if(property.indexOf('comment') !== -1){
						myRights.comment = true;
					}
				}
			});
			return myRights;
		}

		documents.forEach(function(item){
			item.metadata.contentType = tools.roleFromFileType(item.metadata['content-type']);
			var fileNameSplit = item.metadata.filename.split('.');
			item.metadata.extension = fileNameSplit[fileNameSplit.length - 1];

			if(item.from){
				item.ownerName = item.fromName;
			}

			item.myRights = findAuthorizations(item.shared, item.owner);
		})

		callback(documents);
	}

	function folderToString(tree, treeName, folder, key){
		var folderString = function(cursor, key){
			if(folder === cursor){
				return key;
			}

			for(var subFolder in  cursor){
				var folderPath = folderString(cursor[subFolder], subFolder);
				if(folderPath && key !== treeName){
					return key + '_' + folderPath;
				}
				else if(folderPath && key === treeName){
					return folderPath;
				}
			}
		};
		if(treeName === key){
			return '';
		}
		return '/' + folderString(tree, treeName)
	}

	$scope.openNewDocumentView = function(){
		ui.showLightbox();
		$scope.currentViews.lightbox = $scope.views.lightbox.createFile;
	};

	$scope.targetDocument = {};
	$scope.openCommentView = function(document){
		$scope.targetDocument = document;
		ui.showLightbox();
		$scope.currentViews.lightbox = $scope.views.lightbox.comment;
	};

	$scope.openShareView = function(document){
		$scope.targetDocument = document;
		ui.showLightbox();
		$scope.currentViews.lightbox = $scope.views.lightbox.share;
	}

	$scope.toTrash = function(url){
		$scope.selectedDocuments().forEach(function(document){
			One.put(url + "/" + document._id);
		});
		$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(doc){ return doc.selected; });
		notify.info('workspace.removed.message');
	};

	$scope.openSendRackView = function(){
		ui.showLightbox();
		$scope.currentViews.lightbox = $scope.views.lightbox.sendRack;
	};

	$scope.openMoveFileView = function(action){
		ui.showLightbox();
		$scope.currentViews.lightbox = $scope.views.lightbox[action];
	};

	$scope.remove = function(){
		$scope.selectedDocuments().forEach(function(document){
			http.delete('document/' + document._id).done(function(){
				$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(item){
					return item === document;
				});
				$scope.$apply();
			});
		});
	};

	$scope.restore = function(){
		$scope.selectedDocuments().forEach(function(document){
			http.put('restore/document/' + document._id).done(function(){
				$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(item){
					return item === document;
				});
				$scope.$apply();
			});
		});
	};

	$scope.sendComment = function(){
		ui.hideLightbox();
		http.post('document/' + $scope.targetDocument._id + '/comment', 'comment=' + $scope.targetDocument.comment).done(function(){
			if(!$scope.targetDocument.comments){
				$scope.targetDocument.comments = [];
			}
			$scope.targetDocument.comments.push({
				author: $scope.me.userId,
				authorName: $scope.me.username,
				comment: $scope.targetDocument.comment,
				posted: new Date()
			});
			$scope.targetDocument.showComments = true;
			$scope.$apply();
		});
	}

	var trees = [{
		name: 'documents',
		path: 'documents',
		filter: 'owner',
		buttons: [
			{ text: 'workspace.add.document', action: $scope.openNewDocumentView, icon: true },
			{ text: 'workspace.send.rack', action: $scope.openSendRackView },
			{ text: 'workspace.move.trash', action: $scope.toTrash, url: 'document/trash', contextual: true },
			{ text: 'workspace.move', action: $scope.openMoveFileView, url: 'moveFile', contextual: true },
			{ text: 'workspace.copy', action: $scope.openMoveFileView, url: 'copyFile', contextual: true }
		]
	}, {
		name: 'rack',
		path: 'rack/documents',
		buttons: [
			{ text: 'workspace.send.rack', action: $scope.openSendRackView },
			{ text: 'workspace.move.racktodocs', action: $scope.openMoveFileView, url: 'copyFile', contextual: true },
			{ text: 'workspace.move.trash', action: $scope.toTrash, url: 'rack/trash', contextual: true }
		]
	}, {
		name: 'trash',
		path: 'documents/Trash',
		buttons: [
			{ text: 'workspace.move.trash', action: $scope.remove, contextual: true },
			{ text: 'workspace.trash.restore', action: $scope.restore, contextual: true }
		]
	}, {
		name: 'shared',
		filter: 'shared',
		path: 'documents',
		buttons: [
			{ text: 'workspace.send.rack', action: $scope.openSendRackView },
			{ text: 'workspace.move.trash', action: $scope.toTrash, url: 'document/trash', contextual: true },
			{ text: 'workspace.move', action: $scope.openMoveFileView, url: 'moveFile', contextual: true },
			{ text: 'workspace.copy', action: $scope.openMoveFileView, url: 'copyFile', contextual: true }
		]
	}];

	function currentTree(){
		var currentTree = '';
		trees.forEach(function(tree){
			if($scope.containsCurrentFolder($scope.folders[tree.name])){
				currentTree = tree;
			}
		});
		$scope.currentTree = currentTree;
		return currentTree;
	};
	$scope.currentTree = currentTree;

	$scope.selectedDocuments = function(){
		return _.where($scope.openedFolder.content, {selected: true});
	}

	$scope.views = {
		lightbox: {
			createFile: 'public/template/create-file.html',
			sendRack: 'public/template/send-rack.html',
			moveFile: 'public/template/move-files.html',
			copyFile: 'public/template/copy-files.html',
			comment: 'public/template/comment.html',
			share: 'public/template/share.html'
		},
		documents: {
			list: 'public/template/list-view.html',
			icons: 'public/template/icons-view.html'
		}
	};

	$scope.currentViews = {
		lightbox: '',
		documents: $scope.views.documents.list
	};

	$scope.switchView = function(view, value){
		$scope.currentViews[view] = $scope.views[view][value];
	}

	$scope.openedFolder = {
		folder: $scope.folders.documents,
		content: {}
	};

	$scope.openFolder = function(folder, folderName, tree){
		$scope.openedFolder.folder = folder;
		$scope.openedFolder.name = folderName;
		if(!tree){
			var tree = currentTree();
		}

		http.get(tree.path +  folderToString($scope.folders[tree.name], tree.name, folder, folderName), { hierarchical: true, filter: tree.filter }).done(function(documents){
			formatDocuments(documents, function(result){
				$scope.openedFolder.content = result;
				$scope.$apply();
			});
		})
	}

	$scope.translate = function(key){
		return lang.translate(key);
	};

	$scope.longDate = function(dateString){
		if(!dateString){
			return '';
		}
		return date.format(dateString, 'D MMMM')
	}

	$scope.toggleComments = function(document){
		document.showComments = !document.showComments;
	}

	$scope.containsCurrentFolder = function(folder){
		var checkSubFolders = function(currentFolder){
			if($scope.openedFolder.folder === currentFolder){
				return true;
			}

			for(var subFolder in currentFolder){
				if(checkSubFolders(currentFolder[subFolder])){
					return true;
				}
			}
		}

		return checkSubFolders(folder);
	};

	var getFolders = function(tree, params){
		http.get('folders', params).done(function(folders){
			folders.forEach(function(folder){
				if(folder === 'Trash'){
					return;
				}
				var subFolders = folder.split('_');
				var cursor = tree;
				subFolders.forEach(function(subFolder){
					if(cursor[subFolder] === undefined){
						cursor[subFolder] = {};
					}
					cursor = cursor[subFolder];
				})
			});

			$scope.$apply()
		});
	};

	var editViews = [];
	var editStarted = null;
	$scope.openEditView = function(value){
		editViews.push(value);
	};

	$scope.isEditViewOpened = function(value){
		return editViews.indexOf(value) !== -1;
	};

	$scope.startEditing = function(value){
		editStarted = value;
	};

	$scope.editStarted = function(value){
		return editStarted === value;
	};

	$scope.addFolder = function(node, subNodeName){
		node[subNodeName] = {};
		editViews = _.reject(editViews, function(openedNode){
			return openedNode === node;
		});
		if($scope.editMode === 'single'){
			$scope.setFolder(subNodeName, node[subNodeName]);
		}
		else{
			$scope.switchTargetFolder(subNodeName, node[subNodeName]);
		}
		$scope.$apply();
	};

	var targetFolders = [];
	$scope.switchTargetFolder = function(key, value){
		var stringFolder = folderToString($scope.editTree, 'documents', value, key);
		if($scope.isTargetFolder(key, value)){
			targetFolders = _.reject(targetFolders, function(item){
				item === stringFolder;
			})
		}
		else{
			targetFolders.push(stringFolder);
		}
	};

	$scope.isTargetFolder = function(key, value){
		return targetFolders.indexOf(folderToString($scope.editTree, 'documents', value, key)) !== -1;
	}

	$scope.selectedFolder = { folder: {}, name: '' };
	$scope.setFolder = function(key, value){
		$scope.selectedFolder.name = key;
		$scope.selectedFolder.folder = value;
	}

	$scope.editTree = {};
	$scope.openEditView($scope.editTree);
	function updateFolders(){
		getFolders($scope.folders.documents, { filter: 'owner' });
		getFolders($scope.editTree, { filter: 'owner' });
		getFolders($scope.folders.shared, { filter: 'shared' });
	}

	$scope.move = function(){
		ui.hideLightbox();
		var selectedDocumentsIds = _.pluck($scope.selectedDocuments(), '_id').join(',');
		var folderString = folderToString($scope.editTree, 'documents', $scope.selectedFolder.folder, $scope.selectedFolder.name);
		http.put('documents/move/' + selectedDocumentsIds + folderString).done(function(){
			updateFolders();
		});
	};

	$scope.copy = function(){
		ui.hideLightbox();
		var selectedDocumentsIds = _.pluck($scope.selectedDocuments(), '_id').join(',');
		targetFolders.forEach(function(folder){
			var basePath = 'documents';
			if($scope.currentTree.name === 'rack'){
				basePath = 'rack/' + basePath;
			}
			http.post(basePath + '/copy/' + selectedDocumentsIds + folder).done(function(){
				updateFolders();
			});
		})
	};

	updateFolders();
	http.get('/auth/oauth2/userinfo').done(function(data){
		$scope.me = data;
		$scope.openFolder($scope.folders.documents, 'documents', trees[0]);
	});

	$scope.newFile = { name: $scope.translate('nofile'), file: null };
	$scope.setFileName = function(){
		$scope.newFile.name = $scope.newFile.file.name.split('.')[0];
	};

	$scope.boxes = { selectAll: false }
	$scope.switchSelectAll = function(){
		$scope.openedFolder.content.forEach(function(document){
			document.selected = $scope.boxes.selectAll;
		});
	};

	var targetFolders = [];
	$scope.switchTargetFolder = function(key, value){
		var stringFolder = folderToString($scope.editTree, 'documents', value, key);
		if($scope.isTargetFolder(key, value)){
			targetFolders = _.reject(targetFolders, function(item){
				item === stringFolder;
			})
		}
		else{
			targetFolders.push(stringFolder);
		}
	};

	$scope.isTargetFolder = function(key, value){
		return targetFolders.indexOf(folderToString($scope.editTree, 'documents', value, key)) !== -1;
	}

	$scope.selectedFolder = { folder: {}, name: '' };
	$scope.setFolder = function(key, value){
		$scope.selectedFolder.name = key;
		$scope.selectedFolder.folder = value;
	}

	$scope.editTree = {};
	$scope.openEditView($scope.editTree);
	function updateFolders(){
		getFolders($scope.folders.documents, { filter: 'owner' });
		getFolders($scope.editTree, { filter: 'owner' });
		getFolders($scope.folders.shared, { filter: 'shared' });
	}

	$scope.move = function(){
		ui.hideLightbox();
		var selectedDocumentsIds = _.pluck($scope.selectedDocuments(), '_id').join(',');
		var folderString = folderToString($scope.editTree, 'documents', $scope.selectedFolder.folder, $scope.selectedFolder.name);
		http.put('documents/move/' + selectedDocumentsIds + folderString).done(function(){
			updateFolders();
		});
	};

	$scope.copy = function(){
		ui.hideLightbox();
		var selectedDocumentsIds = _.pluck($scope.selectedDocuments(), '_id').join(',');
		targetFolders.forEach(function(folder){
			var basePath = 'documents';
			if($scope.currentTree.name === 'rack'){
				basePath = 'rack/' + basePath;
			}
			http.post(basePath + '/copy/' + selectedDocumentsIds + folder).done(function(){
				updateFolders();
			});
		})
	};

	updateFolders();
	$scope.openFolder($scope.folders.documents, 'documents');

	$scope.newFile = { name: $scope.translate('nofile'), file: null };
	$scope.setFileName = function(){
		$scope.newFile.name = $scope.newFile.file.name.split('.')[0];
	};

	$scope.to = {
		id: ''
	}
	$scope.sendNewFile = function(context){
		var formData = new FormData();
		formData.append('file', $scope.newFile.file);
		var url = '';
		if (context === 'rack') {
			url = 'rack/' + $scope.to.id;
		}
		else{
			url = 'document'
		}
		$scope.loading = $scope.translate('loading');
		http.postFile(url + '?name=' + $scope.newFile.name,  formData).done(function(e){
			ui.hideLightbox();
			$scope.loading = '';
			var path = folderToString($scope.folders[$scope.currentTree.name], $scope.currentTree.name, $scope.openedFolder.folder, $scope.openedFolder.name);
			if(context !== 'rack' && path !== ''){
				http.put("documents/move/" + e._id + path).done(function(){
					$scope.openFolder($scope.openedFolder.folder, $scope.openedFolder.name);
				});
			}
			else{
				$scope.openFolder($scope.openedFolder.folder, $scope.openedFolder.name);
			}
		});
	};

	http.get("users/available-rack").done(function(response){
		$scope.users = response;
	});
};
