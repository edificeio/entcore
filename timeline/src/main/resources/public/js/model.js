function Notification(){

}

Notification.prototype = {
	fetch: function(){
		var params = {};
		if(Model.types.current){
			params.type = Model.types.current;
		}
		http().get('/timeline/lastNotifications', params).done(function(response){
			$scope.notifications = response.results;
			$scope.$apply('notifications');
		});
	},
	isUnread: function(){
		return _.find(this.recipients, function(recipient){
				return recipient.userId === Model.state.me.userId;
			}) !== undefined;
	}
};

function NotificationType(){

}

NotificationType.prototype.rest = {
	get: '/timeline/types'
}

model.define([Notification, NotificationType]);

