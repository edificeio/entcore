function Notification(){
	this.isUnread = function(){
		return _.find(this.recipients, function(recipient){
			return recipient.userId === Model.state.me.userId;
		}) !== undefined;
	}
}

function NotificationType(){
	this.apply = function(){
		Model.notifications.sync();
	}
}

function Widget(data){}

function Skin(data){
	this.setForUser = function(){
		http().get('/userbook/api/edit-userbook-info?prop=theme&value=' + this._id);
	}
}

Model.build = function(){
	this.collection(Notification, {
		sync: function(){
			var that = this;
			that.all = [];
			var types = Model.notificationTypes.selection();
			if(Model.notificationTypes.noFilter){
				types = Model.notificationTypes.all;
			}
			types.forEach(function(type){
				var params = { type: type.data };
				http().get('/timeline/lastNotifications', params).done(function(response){
					that.addRange(response.results);
				});
			});
		}
	});

	this.collection(NotificationType, {
		sync: function(){
			var that = this;
			http().get('/timeline/types').done(function(data){
				that.load(data);
				Model.notifications.sync();
			});
		},
		removeFilter: function(){
			Model.notificationTypes.current = null;
			Model.notifications.sync();
		},
		noFilter: true
	});

	this.collection(Widget, {
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

	this.collection(Skin, {
		sync: function(){
			var that = this;
			http().get('/timeline/public/json/themes.json').done(function(data){
				that.load(data);
			})
		}
	});
}



