function Conversation($scope, date){


	Model.folders.inbox.on('mails.change', function(e){
		console.log('inbox update');
	});

	Model.folders.outbox.on('mails.change', function(e){
		console.log('outbox update');
	});

	$scope.resetScope = function(){
		$scope.openInbox();
	};

	$scope.viewsContainers = {};
	$scope.openView = function(view, name){
		var viewsPath = '/conversation/public/template/';
		$scope.viewsContainers[name] = viewsPath + view + '.html';
	};

	$scope.openInbox = function(){
		Model.folders.openInbox();
		$scope.openView('inbox', 'main');
	};

	$scope.openOutbox = function(){
		Model.folders.openOutbox();
		$scope.openView('outbox', 'main');
	};

	$scope.inbox = Model.folders.inbox;
	$scope.outbox = Model.folders.outbox;

	$scope.openView('inbox', 'main');
}