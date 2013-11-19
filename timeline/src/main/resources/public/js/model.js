function buildModel(){
	function Notification(){
		this.isUnread = function(){
			return _.find(this.recipients, function(recipient){
				return recipient.userId === Model.state.me.userId;
			}) !== undefined;
		}
	}

	Model.collection(Notification, {
		sync: function(){
			var params = {};
			if(Model.notificationTypes.current){
				params.type = Model.notificationTypes.current;
			}
			var that = this;
			http().get('/timeline/lastNotifications', params).done(function(response){
				that.load(response.results);
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

	Model.collection(NotificationType, {
		sync: function(){
			var that = this;
			http().get('/timeline/types').done(function(data){
				that.load(data);
			});
		}
	})

	function Widget(){

	}

	Model.collection(Widget);

	function Skin(){}

	Model.collection(Skin, {
		sync: function(){
			var that = this;
			http().get('/timeline/public/json/themes.json').done(function(data){
				that.load(data);
			})
		}
	});
}



