function Conversation($scope, date, notify){
	Model.folders.systemFolders.forEach(function(folderName){
		Model.folders[folderName].on('mails.change', function(e){
			$scope.$apply(folderName);
			$scope.$apply('newItem');
			$scope.$apply('mail');
		});
	});

	$scope.resetScope = function(){
		$scope.openInbox();
	};

	$scope.viewsContainers = {};
	$scope.openView = function(view, name){
		$scope.newItem = new Mail();
		$scope.selection.selectAll = false;
		var viewsPath = '/conversation/public/template/';
		$scope.viewsContainers[name] = viewsPath + view + '.html';
	};

	$scope.containsView = function(name, view){
		var viewsPath = '/conversation/public/template/';
		return $scope.viewsContainers[name] === viewsPath + view + '.html';
	};

	$scope.openFolder = function(folderName){
		if(!folderName){
			folderName = Model.folders.current.folderName;
		}
		$scope.openView('', 'reply');
		$scope.mail = undefined;
		Model.folders.openFolder(folderName);
		$scope.openView(folderName, 'main');
	};

	$scope.nextPage = function(){
		Model.folders.current.nextPage();
	}

	$scope.selection = {
		selectAll: false
	};

	$scope.switchSelectAll = function(){
		if($scope.selection.selectAll){
			Model.folders.current.mails.selectAll();
		}
		else{
			Model.folders.current.mails.deselectAll();
		}
	};

	function setCurrentMail(mail){
		Model.folders.current.mails.current = mail;
		Model.folders.current.mails.deselectAll();
		Model.folders.current.mails.current.selected = true;
		$scope.mail = mail;
	}

	$scope.viewMail = function(mail){
		$scope.openView('view-mail', 'main');
		setCurrentMail(mail);
		mail.open();
	};

	$scope.refresh = function(){
		notify.info('Mise à jour...');
		Model.folders.current.mails.refresh();
	}

	$scope.readMail = function(mail){
		$scope.openView('read-mail', 'main');
		setCurrentMail(mail);
		mail.open();
	};

	var format = {
		reply: {
			prefix: 'Re : '
		},
		transfer: {
			prefix: 'Tr : '
		}
	};

	http().get('/conversation/public/template/mail-content/transfer.html').done(function(content){
		format.transfer.content = content;
	});

	http().get('/conversation/public/template/mail-content/reply.html').done(function(content){
		format.reply.content = content;
	});

	function setMailContent(mailType){
		if($scope.mail.subject.indexOf(format[mailType].prefix) === -1){
			$scope.newItem.subject = format[mailType].prefix + $scope.mail.subject;
		}
		else{
			$scope.newItem.subject = $scope.mail.subject;
		}

		$scope.newItem.body = format[mailType].content + '<blockquote>' + $scope.mail.body + '</blockquote>';
	}

	$scope.transfer = function(){
		$scope.openView('write-mail', 'reply');
		setMailContent('transfer');
	};

	$scope.reply = function(){
		$scope.openView('write-mail', 'reply');
		$scope.newItem.parentConversation = $scope.mail;
		$scope.addUser($scope.mail.sender());
		setMailContent('reply');
	};

	$scope.replyAll = function(){
		$scope.openView('write-mail', 'reply');
		$scope.newItem.parentConversation = $scope.mail;
		setMailContent('reply');
		$scope.mail.displayNames.forEach(function(user){
			if(user[0] === Model.me.userId){
				return;
			}
			$scope.addUser(new User({ id: user[0], displayName: user[1] }));
		});
	};

	$scope.editDraft = function(draft){
		$scope.openView('write-mail', 'main');
		draft.open();
		$scope.newItem = draft;
	};

	$scope.saveDraft = function(){
		notify.info('Brouillon enregistré');
		Model.folders.draft.saveDraft($scope.newItem);
	};

	$scope.sendMail = function(){
		$scope.newItem.send(function(result){
			if(parseInt(result.sent) > 0){
				notify.info('Message envoyé');
			}
			var inactives = '';
			result.inactive.forEach(function(name){
				inactives += name + lang.translate('invalid') + '<br />';
			});
			if(result.inactive.length > 0){
				notify.info(inactives);
			}

			result.undelivered.forEach(function(name){
				notify.error(name + lang.translate('undelivered'));
			});
		});
		$scope.openFolder();
	};

	$scope.removeSelection = function(){
		Model.folders.current.mails.removeMails();
	};

	$scope.clearSearch = function(){
		$scope.users.found = [];
		$scope.users.foundCC = [];
		$scope.users.search = '';
		$scope.users.searchCC = '';
	}

	$scope.updateFoundCCUsers = function(){
		var include = [];
		var exclude = $scope.newItem.cc || [];
		if($scope.mail){
			include = _.map($scope.mail.displayNames, function(item){
				return new User({ id: item[0], displayName: item[1] });
			});
		}
		$scope.users.foundCC = Model.users.find($scope.users.searchCC, include, exclude);
	};

	$scope.updateFoundUsers = function(){
		var include = [];
		var exclude = $scope.newItem.to || [];
		if($scope.mail){
			include = _.map($scope.mail.displayNames, function(item){
				return new User({ id: item[0], displayName: item[1] });
			});
		}
		$scope.users.found = Model.users.find($scope.users.search, include, exclude);
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

	$scope.lang = lang;
	$scope.notify = notify;
	$scope.folders = Model.folders;
	$scope.users = { list: Model.users, search: '', found: [], foundCC: [] };

	$scope.newItem = new Mail();

	$scope.openView('inbox', 'main');
}