function Notification(){
	this.isUnread = function(){
		return _.find(this.recipients, function(recipient){
			return recipient.userId === model.me.userId;
		}) !== undefined;
	}
}

function NotificationType(){
	this.apply = function(){
		if(model.notificationTypes.selection().length > 0){
			model.notificationTypes.noFilter = false;
		}
		model.notifications.sync();
	}
}

function Skin(data){
	this.setForUser = function(){
		http().get('/userbook/api/edit-userbook-info?prop=theme&value=' + this._id);
	}
}

model.build = function (){
	this.makeModels([Notification, NotificationType, Widget, Skin]);

	this.collection(Notification, {
		page: 0,
		lastPage: false,
		loading: false,
		sync: function(paginate){
			var that = this;

			if(that.loading || (paginate && that.lastPage))
				return

			var types = model.notificationTypes.selection();
			if(model.notificationTypes.noFilter){
				types = model.notificationTypes.all;
			}

			if(!types.length){
				return;
			}

			var params = { type: _.map(types, function(type){
				return type.data;
			})};
			params.page = that.page;

			if(paginate)
				that.loading = true;

			http().get('/timeline/lastNotifications', params).done(function(response){
				that.loading = false;
				if(paginate){
					if(response.results.length > 0){
						that.addRange(response.results);
						that.page++;
					} else {
						that.lastPage = true;
					}
				} else {
					that.page = 0;
					that.lastPage = false;
					that.load(response.results);
				}

			}).error(function(data){
				that.loading = false;
				notify.error(data);
			});

			http().putJson('/userbook/preference/timeline', params);
		}
	});

	this.collection(NotificationType, {
		sync: function(){
			http().get('/timeline/types').done(function(data){
				this.load(data);

				var that = this
				http().get('/userbook/preference/timeline').done(function(data){
					var pref = data.preference ? JSON.parse(data.preference) : null
					var myFilters = pref && pref.type && pref.type.length > 0  ? pref.type : null
					that.forEach(function(t){
						if(myFilters === null || myFilters.indexOf(t.data) >= 0){
							t.selected = true
						}
					});
					model.notifications.sync();
				})
			}.bind(this));
		},
		removeFilter: function(){
			model.notificationTypes.current = null;
			model.notifications.sync();
		},
		noFilter: false
	});

	this.collection(Skin, {
		sync: function(){
			skin.listThemes(function(themes){
				this.load(themes);
			}.bind(this));
		}
	});
};
