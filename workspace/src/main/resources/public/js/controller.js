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
					console.log(type);
					return (type.indexOf('document') !== -1 && type.indexOf('presentation') !== -1) || type.indexOf('powerpoint') !== -1;
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
		},
		resolveMyRights: function(me){
			me.myRights = {

			}
		}
	}
}());

function Workspace($scope, date, ui, notify, _, $rootScope){
	$rootScope.$on('share-updated', function(){
		$scope.openFolder($scope.openedFolder.folder);
	})

	$scope.folder = { children: [ { name: 'documents' }, { name: 'shared' }, { name: 'rack' }, { name: 'trash', children: [] }] };
	$scope.users = [];

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
		}

		if(document.owner === $scope.me.userId){
			document.myRights.share = document.myRights.share &&
				_.where($scope.me.authorizedActions, { name: 'org.entcore.workspace.service.WorkspaceService|share'}).length > 0;
			return;
		}

		var currentSharedRights = _.filter(document.shared, function(sharedRight){
			return $scope.me.profilGroupsIds.indexOf(sharedRight.groupId) !== -1
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
		document.myRights.document.share = false;
	}

	$scope.documentPath = function(document){
		if($scope.currentTree.name === 'rack'){
			return '/workspace/rack/' + document._id;
		}
		else{
			return '/workspace/document/' + document._id;
		}
	}

	function formatDocuments(documents, callback){
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
		})

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
	}

	$scope.targetDocument = {};
	$scope.openCommentView = function(document){
		$scope.targetDocument = document;
		ui.showLightbox();
		$scope.currentViews.lightbox = $scope.views.lightbox.comment;
	};

	$scope.workflowRight = function(name){
		var workflowRights = {
			share: 'org.entcore.workspace.service.WorkspaceService|shareJson'
		}

		return _.where($scope.me.authorizedActions, { name: workflowRights[name] }).length > 0;
	}

	$scope.nbFolders = function(){
		if(!$scope.openedFolder.folder.children){
			return 0;
		}
		return $scope.openedFolder.folder.children.length;
	};

	$scope.openShareView = function(){
		$scope.sharedDocuments = $scope.selectedDocuments();
		ui.showLightbox();
		$scope.currentViews.lightbox = $scope.views.lightbox.share;
	};

	refreshFolders = function(){
		var folder = $scope.openedFolder;
		$scope.openedFolder = { folder: {} };

		setTimeout(function(){
			$scope.openedFolder = folder;
			$scope.$apply('openedFolder');
		}, 1);
	};

	$scope.toTrash = function(url){
		$scope.selectedFolders().forEach(function(folder){
			http().put('/workspace/folder/trash/' + folder._id);
		})
		$scope.selectedDocuments().forEach(function(document){
			http().put(url + "/" + document._id);
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

	$scope.openSendRackView = function(){
		ui.showLightbox();
		$scope.newFile = { name: $scope.translate('nofile'), chosenFiles: [] };
		$scope.currentViews.lightbox = $scope.views.lightbox.sendRack;
	};

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
			http().delete('document/' + document._id);
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
			http().put('restore/document/' + document._id);
		});

		$scope.selectedFolders().forEach(function(folder){
			$scope.openedFolder.folder.children = _.reject($scope.openedFolder.folder.children, function(item){
				return item === folder;
			});

			http().put('/workspace/folder/restore/' + folder._id);
		});
		updateFolders();
	};

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
				posted: new Date()
			});
			$scope.documentComment = $scope.targetDocument;
			$scope.$apply();
		});
	}

	var trees = [{
		name: 'documents',
		path: 'documents',
		filter: 'owner',
		hierarchical: true,
		buttons: [
			{ text: 'workspace.add.document', action: $scope.openNewDocumentView, icon: true, allow: function(){
				return _.where($scope.me.authorizedActions, { name: 'org.entcore.workspace.service.WorkspaceService|addDocument'}).length > 0;
			} },
			{ text: 'workspace.send.rack', action: $scope.openSendRackView, allow: function(){
				return _.where($scope.me.authorizedActions, { name: 'org.entcore.workspace.service.WorkspaceService|addRackDocument'}).length > 0;
			} }
		],
		contextualButtons: [
			{ text: 'workspace.move.trash', action: $scope.toTrash, url: 'document/trash', contextual: true, allow: function(){ return true } },
			{ text: 'workspace.move', action: $scope.openMoveFileView, url: 'moveFile', contextual: true, allow: function(){ return true } },
			{ text: 'workspace.copy', action: $scope.openMoveFileView, url: 'copyFile', contextual: true, allow: function(){ return true } }
		]
	}, {
		name: 'rack',
		path: 'rack/documents',
		buttons: [
			{ text: 'workspace.send.rack', action: $scope.openSendRackView, allow: function(){
				return _.where($scope.me.authorizedActions, { name: 'org.entcore.workspace.service.WorkspaceService|addRackDocument'}).length > 0;
			} }
		],
		contextualButtons: [
			{ text: 'workspace.move.racktodocs', action: $scope.openMoveFileView, url: 'copyFile', contextual: true, allow: function(){ return true } },
			{ text: 'workspace.move.trash', action: $scope.toTrash, url: 'rack/trash', contextual: true, allow: function(){ return true } }
		]
	}, {
		name: 'trash',
		path: 'documents/Trash',
		filter: 'owner',
		buttons: [],
		contextualButtons: [
			{ text: 'workspace.move.trash', action: $scope.remove, contextual: true, allow: function(){ return true } },
			{ text: 'workspace.trash.restore', action: $scope.restore, contextual: true, allow: function(){ return true } }
		]
	}, {
		name: 'shared',
		filter: 'shared',
		hierarchical: false,
		path: 'documents',
		buttons: [
			{ text: 'workspace.send.rack', action: $scope.openSendRackView, allow: function(){
				return _.where($scope.me.authorizedActions, { name: 'org.entcore.workspace.service.WorkspaceService|addRackDocument'}).length > 0;
			} }
		],
		contextualButtons: [
			{ text: 'workspace.move.racktodocs', action: $scope.openMoveFileView, url: 'copyFile', contextual: true, allow: function(){ return true } },
			{ text: 'workspace.move.trash', action: $scope.toTrash, url: 'document/trash', contextual: true, allow: function(){
				return _.find($scope.selectedDocuments(), function(doc){ return doc.myRights.document.moveTrash === false }) === undefined;
			} }
		]
	}];

	$scope.selectedDocuments = function(){
		return _.where($scope.openedFolder.content, {selected: true});
	};

	$scope.selectedFolders = function(){
		return _.where($scope.openedFolder.folder.children, { selected: true });
	}

	$scope.views = {
		lightbox: {
			createFile: 'public/template/create-file.html',
			createFolder: 'public/template/create-folder.html',
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
	};

	$scope.openedFolder = {};
	$scope.openFolder = function(folder){
		if(folder !== $scope.openedFolder.folder){
			$scope.loadingFiles = [];
		}

		$scope.openedFolder.folder = folder;
		if($scope.folder.children.indexOf(folder) !== -1){
			currentTree();
		}

		var params = {
			filter: $scope.currentTree.filter
		};

		if($scope.currentTree.hierarchical){
			params.hierarchical = true;
		}

		var path = $scope.currentTree.path;
		var folderString = folderToString($scope.currentFolderTree, folder)
		if(folderString !== ''){
			path += '/' + folderString;
		}

		http().get(path, params).done(function(documents){
			formatDocuments(documents, function(result){
				$scope.openedFolder.content = result;
				if($scope.currentTree.name === 'shared'){
					$scope.openedFolder.content = _.reject($scope.openedFolder.content, function(document){
						return document.folder === 'Trash';
					});
				}
				$scope.$apply();
			});

		})
	}

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
				}).xhr;

			$scope.loadingFiles.push({
				file: file.file,
				request: request
			});
		});

	}

	$scope.translate = function(key){
		return lang.translate(key);
	};

	$scope.longDate = function(dateString){
		if(!dateString){
			return '';
		}
		return date.format(dateString, 'D MMMM YYYY')
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
		}

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

	var getFolders = function(tree, params){
		http().get('/workspace/folders/list', params).done(function(folders){
			folders.forEach(function(folder){
				folder.created = folder.created.split('.')[0] + ':' + folder.created.split('.')[1].substring(0, 2)
				if(folder.folder.indexOf('Trash') !== -1){
					if(_.where($scope.folder.children[$scope.folder.children.length - 1].children, { folder: folder.folder }).length === 0){
						$scope.folder.children[$scope.folder.children.length - 1].children.push(folder);
					}
					return;
				}
				var subFolders = folder.folder.split('_');
				var cursor = tree;
				subFolders.forEach(function(subFolder){
					if(cursor === undefined){
						cursor = {};
					}
					if(cursor.children === undefined){
						cursor.children = [];
					}
					if(_.where(cursor.children, {name: subFolder }).length === 0){
						cursor.children.push(folder)
					}
					cursor = _.findWhere(cursor.children, { name: subFolder });
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

	function updateFolders(){
		getFolders($scope.folder.children[0], { filter: 'owner' });
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
					updateFolders();
				})
				.e400(function(e){
					var error = JSON.parse(e.responseText);
					notify.error(error.error);
					$scope.openedFolder.folder.children.push(folder);
					refreshFolders();
				});
		})
	};

	$scope.copy = function(){
		ui.hideLightbox();
		var selectedDocumentsIds = _.pluck($scope.selectedDocuments(), '_id').join(',');
		targetFolders.forEach(function(folder){
			var basePath = 'documents';
			if($scope.currentTree.name === 'rack'){
				basePath = 'rack/' + basePath;
			}
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
	$scope.me = { authorizedActions: [] };
	http().get('/auth/oauth2/userinfo').done(function(data){
		$scope.me = data;
		$scope.openFolder($scope.folder.children[0]);
		if(_.where($scope.me.authorizedActions, {name: 'org.entcore.workspace.service.WorkspaceService|listRackDocuments' }).length === 0){
			$scope.folder.children = _.reject($scope.folder.children, function(folder){
				return folder.name === 'rack';
			});
			refreshFolders();
		}
		else{
			http().get("users/available-rack").done(function(response){
				$scope.users = response;
			});
		}
	});

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

	$scope.setFilesName = function(){
		$scope.newFile.name = '';
		$scope.newFile.chosenFiles = [];
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

	}

	$scope.to = {
		id: ''
	};

	$scope.sendRackFiles = function(){
		for(var i = 0; i < $scope.newFile.files.length; i++){
			var formData = new FormData();

			formData.append('file', $scope.newFile.files[i]);

			var url = 'rack/' + $scope.to.id;
			$scope.loading = $scope.translate('loading');
			http().postFile(url + '?thumbnail=120x120',  formData, { requestName: 'file-upload' }).done(function(e){
				ui.hideLightbox();
				$scope.loading = '';
				$scope.openFolder($scope.openedFolder.folder);
			});
		}
	};



	$scope.editFolder = function(){
		$scope.newFolder.editing = true;
	}

	$scope.anyTargetFolder = function(){
		return targetFolders.length > 0;
	}
};