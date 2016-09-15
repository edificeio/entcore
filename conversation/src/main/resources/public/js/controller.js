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
routes.define(function($routeProvider){
	$routeProvider
		.when("/read-mail/:mailId", {
			action: "readMail"
		})
		.when("/write-mail/:userId", {
			action: "writeMail"
		})
		.when('/inbox', {
			action: 'inbox'
		})
		.otherwise({
			redirectTo: "/inbox"
		})
});

function Conversation($scope, $timeout, date, notify, route, model){
	route({
		readMail: function(params){
			model.folders.openFolder('inbox');
			$scope.openView('folders', 'page');
			$scope.readMail(new Mail({ id: params.mailId }));
		},
		writeMail: function(params){
			model.folders.openFolder('inbox');
			model.users.on('sync', function(){
				if(this.findWhere({ id: params.userId })){
					$scope.openView('folders', 'page');
					new User({ id: params.userId }).findData(function(){
						$scope.openView('write-mail', 'main');
						$scope.addUser(this);
					});
				}
				else{
					$scope.openView('e401', 'page');
				}
			});
		},
		inbox: function(){
			$scope.openView('folders', 'page');
		}
	});

	$scope.formatFileType = function(fileType){
		if(!fileType)
			return 'unknown'

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
	}

	$scope.clearSearch = function(){
		$scope.users.found = [];
		$scope.users.search = '';
	};

    $scope.clearCCSearch = function(){
		$scope.users.foundCC = [];
		$scope.users.searchCC = '';
	};

	$scope.resetScope = function(){
		$scope.openInbox();
	};

	$scope.viewsContainers = {};
	$scope.openView = function(view, name){
		$scope.clearSearch();
		$scope.newItem = new Mail();
		$scope.selection.selectAll = false;
		var viewsPath = '/conversation/public/template/';
		$scope.viewsContainers[name] = viewsPath + view + '.html';
	};

	$scope.containsView = function(name, view){
		var viewsPath = '/conversation/public/template/';
		return $scope.viewsContainers[name] === viewsPath + view + '.html';
	};

	$scope.openFolder = function(folderName, cb){
		if(!folderName){
			if(model.folders.current instanceof UserFolder){
				$scope.openUserFolder(model.folders.current, {});
				return;
			}
			folderName = model.folders.current.folderName;
		}
		$scope.mail = undefined;
		model.folders.openFolder(folderName, cb);
		$scope.openView(folderName, 'main');
	};

	$scope.openUserFolder = function(folder, obj){
		$scope.mail = undefined
		model.folders.current = folder
        folder.mails.full = false
        folder.pageNumber = 0
		obj.template = ''
		folder.userFolders.sync(function(){
			$timeout(function(){
				obj.template = 'folder-content'
			}, 10)
		})
		folder.mails.sync()
		$scope.openView('folder', 'main');

		$timeout(function(){
			$('body').trigger('whereami.update');
		}, 100)
	};

	$scope.isParentOf = function(folder, targetFolder){
		if(!targetFolder || !targetFolder.parentFolder)
			return false

		var ancestor = targetFolder.parentFolder
		while(ancestor){
			if(folder.id === ancestor.id)
				return true
			ancestor = ancestor.parentFolder
		}
		return false
	}

    $scope.getSystemFolder = function(mail){
        if(mail.from !== model.me.userId && mail.state === "SENT")
			return 'INBOX'
		if(mail.from === model.me.userId && mail.state === "SENT")
			return 'OUTBOX'
		if(mail.from === model.me.userId && mail.state === "DRAFT")
			return 'DRAFT'
        return ''
    }

	$scope.matchSystemIcon = function(mail){
        var systemFolder = $scope.getSystemFolder(mail)
		if(systemFolder === "INBOX")
			return 'mail-in'
		if(systemFolder === "OUTBOX")
			return 'mail-out'
		if(systemFolder === "DRAFT")
			return 'mail-new'
		return ''
	}

	$scope.variableMailAction = function(mail){
        var systemFolder = $scope.getSystemFolder(mail)
        if(systemFolder === "DRAFT")
			return $scope.editDraft(mail)
		else if(systemFolder === "OUTBOX")
			return $scope.viewMail(mail)
		else
			return $scope.readMail(mail)
	}

	$scope.removeFromUserFolder = function(event, mail){
		model.folders.current.mails.selection().forEach(function(mail){
			mail.removeFromFolder();
		});
		model.folders.current.mails.removeSelection();
	};

	$scope.nextPage = function(){
		model.folders.current.nextPage();
	};

	$scope.selection = {
		selectAll: false
	};

	$scope.switchSelectAll = function(){
		if($scope.selection.selectAll){
			model.folders.current.mails.selectAll();
			if(model.folders.current.userFolders)
				model.folders.current.userFolders.selectAll()
		}
		else{
			model.folders.current.mails.deselectAll();
			if(model.folders.current.userFolders)
				model.folders.current.userFolders.deselectAll()
		}
	};

	function setCurrentMail(mail, doNotSelect){
		model.folders.current.mails.current = mail;
		model.folders.current.mails.deselectAll();
		if(!doNotSelect)
			model.folders.current.mails.current.selected = true;
		$scope.mail = mail;
	}

	$scope.viewMail = function(mail){
		$scope.openView('view-mail', 'main');
		setCurrentMail(mail);
		mail.open();
	};

	$scope.refresh = function(){
		notify.info('updating');
		model.folders.current.mails.refresh();
		model.folders.inbox.countUnread();
	};

	$scope.readMail = function(mail){
		$scope.openView('read-mail', 'main');
		setCurrentMail(mail, true);
		mail.open(function(){
			if(!mail.state){
				$scope.openView('e404', 'page');
			}
			$scope.$root.$emit('refreshMails');
		});
	};

	var format = {
		reply: {
			prefix: lang.translate('reply.re')
		},
		transfer: {
			prefix: lang.translate('reply.fw')
		}
	};

	http().get('/conversation/public/template/mail-content/transfer.html').done(function(content){
		format.transfer.content = content;
	});

	http().get('/conversation/public/template/mail-content/reply.html').done(function(content){
		format.reply.content = content;
	});

	function setMailContent(mailType, copyReceivers){
		if($scope.mail.subject.indexOf(format[mailType].prefix) === -1){
			$scope.newItem.subject = format[mailType].prefix + $scope.mail.subject;
		}
		else{
			$scope.newItem.subject = $scope.mail.subject;
		}

		if(copyReceivers){
            $scope.newItem.cc = $scope.mail.cc;
            $scope.newItem.to = $scope.mail.to;
        }
		$scope.newItem.body = format[mailType].content + '<blockquote>' + $scope.mail.body + '</blockquote>';
	}

	$scope.transfer = function(){
		$scope.openView('write-mail', 'main');
        $scope.newItem.parentConversation = $scope.mail;
		setMailContent('transfer');
		model.folders.draft.saveDraft($scope.newItem, function(id){
			http().put("message/"+ $scope.newItem.id +"/forward/" + $scope.mail.id).done(function(){
				if(!$scope.newItem.attachments)
					$scope.newItem.attachments = []
				for(var i = 0; i < $scope.mail.attachments.length; i++){
					$scope.newItem.attachments.push(JSON.parse(JSON.stringify($scope.mail.attachments[i])))
				}
				$scope.getQuota()
			}).error(function(data){
				notify.error(data.error)
			})
		});
	};

	$scope.reply = function(){
		$scope.openView('write-mail', 'main');
		$scope.newItem.parentConversation = $scope.mail;
        setMailContent('reply');
		$scope.addUser($scope.mail.sender());
	};

	$scope.replyAll = function(){
		$scope.openView('write-mail', 'main');
		$scope.newItem.parentConversation = $scope.mail;
		setMailContent('reply', true);
        $scope.newItem.to = _.filter($scope.newItem.to, function(user){ return user.id !== model.me.userId })
        $scope.newItem.cc = _.filter($scope.newItem.cc, function(user){
            return user.id !== model.me.userId  && !_.findWhere($scope.newItem.to, {id: user.id })
        })
        if(!_.findWhere($scope.newItem.to, { id: $scope.mail.sender().id }))
            $scope.addUser($scope.mail.sender());
	};

	$scope.editDraft = function(draft){
		$scope.openView('write-mail', 'main');
		draft.open();
		$scope.newItem = draft;
	};

	$scope.saveDraft = function(){
		notify.info('draft.saved');

		model.folders.draft.saveDraft($scope.newItem);
		$scope.openFolder(model.folders.draft.folderName);
	};

	$scope.sendMail = function(){
		$scope.newItem.send(function(result){
			if(parseInt(result.sent) > 0){
				notify.info('mail.sent');
			}
			var inactives = '';
			result.inactive.forEach(function(name){
				inactives += name + lang.translate('invalid') + '<br />';
			});
			if(result.inactive.length > 0){
				notify.info(inactives);
			}
			var undelivered = result.undelivered.join(', ');
			if(result.undelivered.length > 0){
				notify.error(undelivered + lang.translate('undelivered'));
			}
		});
		$scope.openFolder(model.folders.outbox.folderName);
	};

	$scope.restore = function(){
		if(model.folders.trash.mails.selection().length > 0)
			model.folders.trash.mails.restoreMails();
		if(model.folders.trash.userFolders){
			var launcher = new Launcher(model.folders.trash.userFolders.selection().length, function(){
				model.folders.trash.userFolders.sync()
				$scope.refreshFolders()
			})
			_.forEach(model.folders.trash.userFolders.selection(), function(userFolder){
				userFolder.restore().done(function(){
					launcher.launch()
				})
			})
		}
	};

	$scope.removeSelection = function(){
		if(model.folders.current.mails.selection().length > 0){
			model.folders.current.mails.removeMails().done(function(){
				$scope.getQuota()
			});
		}
		if(model.folders.current.userFolders){
			var launcher = new Launcher(model.folders.current.userFolders.selection().length, function(){
				model.folders.current.userFolders.sync()
				$scope.refreshFolders()
				$scope.getQuota()
			})
			_.forEach(model.folders.current.userFolders.selection(), function(userFolder){
				userFolder.delete().done(function(){
					launcher.launch()
				})
			})
		}
	};

    $scope.allReceivers = function(mail){
        var receivers = mail.to.slice(0);
        mail.toName && mail.toName.forEach(function(deletedReceiver){
            receivers.push({
                deleted: true,
                displayName: deletedReceiver
            });
        });
        return receivers;
    }

	$scope.filterUsers = function(mail){
		return function(user){
            if(user.deleted){
                return true
            }
			var mapped = mail.map(user)
			return typeof mapped !== 'undefined' && typeof mapped.displayName !== 'undefined' && mapped.displayName.length > 0
		}
	}

	$scope.updateFoundCCUsers = function(){
		var include = [];
		var exclude = $scope.newItem.cc || [];
		if($scope.mail){
			include = _.map($scope.mail.displayNames, function(item){
				return new User({ id: item[0], displayName: item[1] });
			});
		}
		$scope.users.foundCC = model.users.find($scope.users.searchCC, include, exclude);
	};

	$scope.updateFoundUsers = function(){
		var include = [];
		var exclude = $scope.newItem.to || [];
		if($scope.mail){
			include = _.map($scope.mail.displayNames, function(item){
				return new User({ id: item[0], displayName: item[1] });
			});
		}
		$scope.users.found = model.users.find($scope.users.search, include, exclude);
	};

	$scope.addUser = function(user){
		if(!$scope.newItem.to){
			$scope.newItem.to = [];
		}
		if(user){
			$scope.newItem.currentReceiver = user;
		}
		$scope.newItem.to.push($scope.newItem.currentReceiver);
	};

	$scope.removeUser = function(user){
		$scope.newItem.to = _.reject($scope.newItem.to, function(item){ return item === user; });
	};

	$scope.addCCUser = function(user){
		if(!$scope.newItem.cc){
			$scope.newItem.cc = [];
		}
		if(user){
			$scope.newItem.currentCCReceiver = user;
		}
		$scope.newItem.cc.push($scope.newItem.currentCCReceiver);
	};

	$scope.removeCCUser = function(user){
		$scope.newItem.cc = _.reject($scope.newItem.cc, function(item){ return item === user; });
	};

	$scope.template = template
	$scope.lightbox = {}

	$scope.rootFolderTemplate = { template: 'folder-root-template' }
	$scope.refreshFolders = function(){
		$scope.userFolders.sync(function(){
			$scope.rootFolderTemplate.template = ""
			$timeout(function(){
				$scope.$apply()
				$scope.rootFolderTemplate.template = 'folder-root-template'
			}, 100)
		})
	}

	$scope.currentFolderDepth = function(){
		if(!($scope.folders.current instanceof UserFolder))
			return 0

		var depth = 1
		var ancestor = $scope.folders.current.parentFolder
		while(ancestor){
			ancestor = ancestor.parentFolder
			depth = depth + 1
		}
		return depth
	}

	$scope.moveSelection = function(){
		$scope.destination = {}
		$scope.lightbox.show = true
		template.open('lightbox', 'move-mail')
	}

	$scope.moveToFolderClick = function(folder, obj){
		obj.template = ''

		if(folder.userFolders.length() > 0){
			$timeout(function(){
				obj.template = 'move-folders-content'
			}, 10)
			return
		}

		folder.userFolders.sync(function(){
			$timeout(function(){
				obj.template = 'move-folders-content'
			}, 10)
		})
	}

	$scope.moveMessages = function(folderTarget){
		$scope.lightbox.show = false
		template.close('lightbox')
		$scope.folders.current.mails.moveSelection(folderTarget)
	}

	$scope.openNewFolderView = function(){
		$scope.newFolder = new UserFolder()
		$scope.newFolder.parentFolderId = $scope.folders.current.id
		$scope.lightbox.show = true
		template.open('lightbox', 'create-folder')
	}
	$scope.createFolder = function(){
		$scope.newFolder.create().done(function(){
			$scope.refreshFolders()
		})
		$scope.lightbox.show = false
		template.close('lightbox')
	}
	$scope.openRenameFolderView = function(folder){
		$scope.targetFolder = new UserFolder()
		$scope.targetFolder.name = folder.name
		$scope.targetFolder.id = folder.id
		$scope.lightbox.show = true
		template.open('lightbox', 'update-folder')
	}
	$scope.updateFolder = function(){
		var current = $scope.folders.current
		$scope.targetFolder.update().done(function(){
			current.name = $scope.targetFolder.name
			$scope.$apply()
		})
		$scope.lightbox.show = false
		template.close('lightbox')
	}
	$scope.trashFolder = function(folder){
		folder.trash().done(function(){
			$scope.refreshFolders()
			$scope.openFolder('trash')
		})
	}
	$scope.restoreFolder = function(folder){
		folder.restore().done(function(){
			$scope.refreshFolders()
		})
	}
	$scope.deleteFolder = function(folder){
		folder.delete().done(function(){
			$scope.refreshFolders()
		})
	}

	var letterIcon = document.createElement("img")
	letterIcon.src = skin.theme +".."+"/img/icons/message-icon.png"
	$scope.drag = function(item, $originalEvent){
		$originalEvent.dataTransfer.setDragImage(letterIcon, 0, 0);
		try{
			$originalEvent.dataTransfer.setData('application/json', JSON.stringify(item));
		} catch(e) {
			$originalEvent.dataTransfer.setData('Text', JSON.stringify(item));
		}
	};
	$scope.dropCondition = function(targetItem){
		return function(event){
			var dataField = event.dataTransfer.types.indexOf && event.dataTransfer.types.indexOf("application/json") > -1 ? "application/json" : //Chrome & Safari
							event.dataTransfer.types.contains && event.dataTransfer.types.contains("application/json") ? "application/json" : //Firefox
							event.dataTransfer.types.contains && event.dataTransfer.types.contains("Text") ? "Text" : //IE
							undefined

			if(!dataField || targetItem.foldersName && targetItem.foldersName !== 'trash')
				return false

			return dataField
		}
	};

	$scope.dropTo = function(targetItem, $originalEvent){
		var dataField = $scope.dropCondition(targetItem)($originalEvent)
		var originalItem = JSON.parse($originalEvent.dataTransfer.getData(dataField))

		if(targetItem.folderName === 'trash')
			$scope.dropTrash(originalItem);
		else
			$scope.dropMove(originalItem, targetItem);
	};

	$scope.dropMove = function(mail, folder){
		var mailObj = new Mail()
		mailObj.id = mail.id
		mailObj.move(folder)
	}
	$scope.dropTrash = function(mail){
		var mailObj = new Mail()
		mailObj.id = mail.id
		mailObj.trash()
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

	$scope.formatSize = function(size){
		var formattedData = $scope.getAppropriateDataUnit(size)
		return (Math.round(formattedData.nb*10)/10)+" "+formattedData.order
	}


	$scope.postAttachments = function(){
		var action = function(){
			_.forEach($scope.newItem.newAttachments, function(targetAttachment){
				var attachmentObj = {
					file: targetAttachment,
					progress: {
						total: 100,
						completion: 0
					}
				}

				if($scope.newItem.loadingAttachments)
					$scope.newItem.loadingAttachments.push(attachmentObj)
				else
					$scope.newItem.loadingAttachments = [attachmentObj]

				var formData = new FormData()
				formData.append('file', attachmentObj.file)

				$scope.newItem.postAttachment(formData, {
					xhr: function() {
				        var xhr = new window.XMLHttpRequest();

						xhr.upload.addEventListener("progress", function(e) {
							if (e.lengthComputable) {
								var percentage = Math.round((e.loaded * 100) / e.total)
								attachmentObj.progress.completion = percentage
								$scope.$apply()
							}
				       }, false);

				       return xhr;
					},
					complete: function(){
						$scope.newItem.loadingAttachments.splice($scope.newItem.loadingAttachments.indexOf(attachmentObj), 1)
						$scope.$apply()
					}
				}).done(function(result){
					attachmentObj.id = result.id
					attachmentObj.filename = attachmentObj.file.name
					attachmentObj.size = attachmentObj.file.size
					attachmentObj.contentType = attachmentObj.file.type
					if(!$scope.newItem.attachments)
						$scope.newItem.attachments = []
					$scope.newItem.attachments.push(attachmentObj)
					$scope.getQuota()
				}).e400(function(e){
					var error = JSON.parse(e.responseText);
					notify.error(error.error);
				})
			})
		}

		if(!$scope.newItem.id){
			model.folders.draft.saveDraft($scope.newItem, action);
		} else {
			action()
		}
	}

	$scope.deleteAttachment = function(event, attachment, mail){
		//Tooltip force removal
		$(event.target).trigger('mouseout')

		mail.deleteAttachment(attachment).done(function(){
			mail.attachments.splice(mail.attachments.indexOf(attachment), 1)
			$scope.getQuota()
		})
	}

	$scope.quota = {
		max: 1,
		used: 0,
		unit: 'Mo'
	};
	$scope.getQuota = function(){
		http().get('/workspace/quota/user/' + model.me.userId).done(function(data){
			
			// if the remaining storage of the user is larger than the remaining storage of the structure, then we make our calculations
			// on the structure.
			if( data.quota - data.storage > data.quotastructure - data.storagestructure ) {
				data.quota = data.quotastructure;
				data.storage = data.storagestructure;
			}

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

	$scope.sortBy = {
        name: function(mail){
            var systemFolder = $scope.getSystemFolder(mail)
            if(systemFolder === 'INBOX'){
                if(mail.fromName)
                    return mail.fromName
                else
                    return mail.sender().displayName
            }
            return _.chain(mail.displayNames)
						.filter(function(u){ return mail.to.indexOf(u[0]) >= 0 })
						.map(function(u){ return u[1] }).value().sort()
        },
        subject: function(mail){
            return mail.subject
        },
        date: function(mail){
            return mail.date
        },
        systemFolder: function(mail){
            var systemFolder = $scope.getSystemFolder(mail)
            if(systemFolder === "INBOX")
    			return 1
    		if(systemFolder === "OUTBOX")
    			return 2
    		if(systemFolder ===  "DRAFT")
    			return 3
    		return 0
        }
    }
	$scope.setSort = function(box, sortFun){
		if(box.sort === sortFun){
			box.reverse = !box.reverse
		} else {
			box.sort = sortFun
			box.reverse = false
		}
	}

	$scope.lang = lang;
	$scope.notify = notify;
	$scope.folders = model.folders;
	$scope.userFolders = model.userFolders;
	$scope.users = { list: model.users, search: '', found: [], foundCC: [] };

	$scope.getQuota();

	//Max folder depth
	http().get('max-depth').done(function(result){
		$scope.maxDepth = result['max-depth']
	})

	$scope.newItem = new Mail();

	$scope.openView('inbox', 'main');
}
