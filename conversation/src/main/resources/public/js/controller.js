function Conversation($scope, date){
	$scope.containers = {};

	Model.folders.inbox.on('mails.change', function(e){
		console.log('inbox update');
	});

	Model.folders.outbox.on('mails.change', function(e){
		console.log('outbox update');
	});

	$scope.resetScope = function(){
		$scope.openInbox();
	};

	$scope.openView = function(view, container){
		var viewsPath = '/conversation/public/template/';
		$scope.containers[container] = viewsPath + view + '.html';
	};

	$scope.openInbox = function(){
		Model.folders.openInbox();
		$scope.openView('mails-list', 'main');
	};

	$scope.openOutbox = function(){
		Model.folders.openOutbox();
		$scope.openView('mails-list', 'main');
	};

	$scope.longDate = function(dateString){
		return date.calendar(dateString);
	};

	$scope.mails = Model.folders.current.mails;

	$scope.resetScope();
}