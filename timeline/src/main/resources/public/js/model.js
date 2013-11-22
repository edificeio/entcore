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
				params.type = Model.notificationTypes.current.data;
			}
			var that = this;
			http().get('/timeline/lastNotifications', params).done(function(response){
				that.load(response.results);
			});
		}
	});

	function NotificationType(){
		this.apply = function(){
			Model.notificationTypes.current = this;
			Model.notifications.sync();
		}
	}
	Model.collection(NotificationType, {
		sync: function(){
			var that = this;
			http().get('/timeline/types').done(function(data){
				that.load(data);
			});
		},
		removeFilter: function(){
			Model.notificationTypes.current = null;
			Model.notifications.sync();
		}
	})

	function Widget(data){}
	Model.collection(Widget, {
		sync: function(){
			var that = this;
			http().get('/timeline/public/json/widgets.json').done(function(data){
				that.load(data, function(widget){
					loader.loadFile(widget.js);
				});
			});
		},
		findWidget: function(name){
			return _.findWhere(this.all, {name: name});
		},
		apply: function(){
			Model.trigger('widgets.change');
		}
	});

	function Skin(data){
		this.setForUser = function(){
			http().get('/userbook/api/edit-userbook-info?prop=theme&value=' + this._id);
		}
	}

	Model.collection(Skin, {
		sync: function(){
			var that = this;
			http().get('/timeline/public/json/themes.json').done(function(data){
				that.load(data);
			})
		}
	});
}



