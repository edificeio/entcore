function Conversation($scope){
	Model.folders.inbox.on('mails.change', function(e){
	});

	Model.folders.outbox.on('mails.change', function(e){
	});

	$scope.resetScope = function(){
		$scope.openInbox();
	};

	$scope.viewsContainers = {};
	$scope.openView = function(view, name){
		var viewsPath = '/conversation/public/template/';
		$scope.viewsContainers[name] = viewsPath + view + '.html';
	};

	$scope.containsView = function(name, view){
		var viewsPath = '/conversation/public/template/';
		return $scope.viewsContainers[name] === viewsPath + view + '.html';
	};

	$scope.openInbox = function(){
		Model.folders.openInbox();
		$scope.openView('inbox', 'main');
	};

	$scope.openOutbox = function(){
		Model.folders.openOutbox();
		$scope.openView('outbox', 'main');
	};

	$scope.openDrafts = function(){
		Model.folders.openDrafts();
		$scope.openView('drafts', 'main');
	};

	$scope.openTrash = function(){
		Model.folders.openTrash();
		$scope.openView('trash', 'main');
	};

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

	$scope.viewMail = function(mail){
		Model.folders.current.mails.current = mail;
		$scope.mail = Model.folders.current.mails.current;
		$scope.openView('view-mail', 'main');
	};

	$scope.saveDraft = function(){

	};

	$scope.sendMail = function(){

	};

	$scope.removeSelection = function(){

	};

	$scope.inbox = Model.folders.inbox;
	$scope.outbox = Model.folders.outbox;
	$scope.drafts = Model.folders.drafts;
	$scope.trash = Model.folders.trash;

	$scope.openView('inbox', 'main');
}