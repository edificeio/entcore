function Conversation($scope, date){
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

	$scope.readMail = function(mail){
		$scope.openView('read-mail', 'main');
		setCurrentMail(mail);
		mail.open();
	};

	$scope.editDraft = function(draft){
		$scope.openView('write-mail', 'main');
		draft.open();
		$scope.newItem = draft;
	};

	$scope.saveDraft = function(){
		Model.folders.draft.saveDraft($scope.newItem);
	};

	$scope.sendMail = function(){
		$scope.newItem.send();
		$scope.openFolder();
	};

	$scope.removeSelection = function(){
		Model.folders.current.mails.removeMails();
	};

	$scope.clearSearch = function(){
		$scope.users.found = [];
		$scope.users.search = '';
	}

	$scope.updateFoundUsers = function(){
		$scope.users.found = Model.users.find($scope.users.search, []);
	};

	$scope.addUser = function(user){
		if(!$scope.newItem.to){
			$scope.newItem.to = [];
		}
		$scope.newItem.to.push($scope.newItem.currentReceiver);
	};

	$scope.removeUser = function(user){
		$scope.newItem.to = _.reject($scope.newItem.to, function(item){ return item === user; });
	};

	$scope.inbox = Model.folders.inbox;
	$scope.outbox = Model.folders.outbox;
	$scope.draft = Model.folders.draft;
	$scope.trash = Model.folders.trash;

	$scope.users = { list: Model.users, search: '', found: [] };

	$scope.newItem = new Mail();

	$scope.openView('inbox', 'main');
}