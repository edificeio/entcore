function Notification(){
	this.isUnread = function(){
		return _.find(this.recipients, function(recipient){
			return recipient.userId === model.me.userId;
		}) !== undefined;
	}
}

function NotificationType(){
	this.apply = function(){
		model.notifications.all = [];
        model.notifications.lastPage = false;
		model.notifications.page = 0;
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

function Preferences(){}
Preferences.prototype.get = function(cb){
    return http().get('/userbook/preference/timeline').done(function(data){
        this.prefs = JSON.parse(data.preference)
        if(typeof cb === 'function')
            cb()
    }.bind(this))
}

function RegisteredNotification(){}

model.build = function (){
	this.makeModels([Notification, NotificationType, Widget, Skin, RegisteredNotification]);

    this.preferences = new Preferences()

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
				if(response.results.length > 0){
					that.addRange(response.results);
					that.page++;
				} else {
					that.lastPage = true;
				}

			}).error(function(data){
				that.loading = false;
				notify.error(data);
			});

			http().putJson('/userbook/preference/timeline', _.extend(model.preferences.prefs || {}, params));
		}
	});

	this.collection(NotificationType, {
		sync: function(){
			http().get('/timeline/types').done(function(data){
				this.load(data);

				var that = this
                model.registeredNotifications.get(function(){
                    that.all = that.filter(function(typeObj){
                        var type = typeObj.data
                        var matchingNotifs = model.registeredNotifications.filter(function(notif){ return notif.type === type })
                        if(matchingNotifs.length < 1)
                            return false
                        var access = model.me.apps.some(function(app){
                            return _.some(matchingNotifs, function(n){
                                return app.name.toLowerCase() === n.type.toLowerCase() || (n["app-name"] && app.name.toLowerCase() === n["app-name"].toLowerCase())
                            })
                        })
                        return access
                    })

				    model.preferences.get(function(){
                        var pref = model.preferences.prefs
                        var myFilters = pref && pref.type && pref.type.length > 0  ? pref.type : null
                        that.forEach(function(t){
                            if(myFilters === null || myFilters.indexOf(t.data) >= 0){
                                t.selected = true
                            }
                        });
                        model.notifications.sync();
                    })
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

    this.collection(RegisteredNotification, {
        get: function(cb){
            http().get('/timeline/registeredNotifications').done(function(data){
                this.load(data)
                if(typeof cb === 'function')
                    cb()
            }.bind(this));
        }
    });
};
