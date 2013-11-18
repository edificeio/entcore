function Notification(){
	this.isUnread = function(){
		return _.find(this.recipients, function(recipient){
			return recipient.userId === Model.state.me.userId;
		}) !== undefined;
	}
}

collection(Notification, {
	sync: function(){
		var params = {};
		if(Model.types.current){
			params.type = Model.types.current;
		}
		http().get('/timeline/lastNotifications', params).done(function(response){
			this.all = response.results;
		});
	}
});

function Resource(){
	this.actions = {
		writeComment: {
			apply: function(){
				http().post(this.commentPath, this.comment)
			}
		},
		comments: {

		}
	}
}

function NotificationType(){}

collection(NotificationType, {
	sync: function(){
		http().get('/timeline/types').done(function(data){
			this.all = data;
		});
	}
})

function Widget(){

}

function Theme(){

}

