function Notification(){
	this.isUnread = function(){
		return _.find(this.recipients, function(recipient){
			return recipient.userId === model.me.userId;
		}) !== undefined;
	}
}

function NotificationType(){
	this.apply = function(){
		model.notifications.sync();
	}
}

function Widget(data){}

function Skin(data){
	this.setForUser = function(){
		http().get('/userbook/api/edit-userbook-info?prop=theme&value=' + this._id);
	}
}

model.build = function (){
	this.makeModels([Notification, NotificationType, Widget, Skin]);

	this.collection(Notification, {
		sync: function(){
			var that = this;
			that.all = [];
			var types = model.notificationTypes.selection();
			if(model.notificationTypes.noFilter){
				types = model.notificationTypes.all;
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
				model.notifications.sync();
			});
		},
		removeFilter: function(){
			model.notificationTypes.current = null;
			model.notifications.sync();
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
			return this.findWhere({name: name});
		},
		apply: function(){
			model.trigger('widgets.change');
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



