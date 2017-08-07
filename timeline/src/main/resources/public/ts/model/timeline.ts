import { http, BaseModel, model as entcoreModel, skin, notify } from 'entcore';
import { _ } from 'entcore/libs/underscore/underscore';

interface TimelineModel extends BaseModel{
	notifications: any;
	notificationTypes: any;
	preferences: any;
	registeredNotifications: any;
}

const model = entcoreModel as TimelineModel;

export let Timeline = {
	Notification: function(){
		this.isUnread = function() {
			return _.find(this.recipients, function(recipient){
				return recipient.userId === model.me.userId;
			}) !== undefined;
		}

		this.reported = this.reporters && this.reporters.length > 0
	},
	NotificationType: function(){
		this.apply = function(){
			model.notifications.all = [];
			model.notifications.lastPage = false;
			model.notifications.page = 0;
			if(model.notificationTypes.selection().length > 0){
				model.notificationTypes.noFilter = false;
			}
			model.notifications.sync();
		}
	},
	Skin: function(){
		this.setForUser = function(){
			http().get('/userbook/api/edit-userbook-info?prop=theme-' + this.path.slice(this.path.indexOf("themes/") + 7, this.path.indexOf('/skins')) + '&value=' + this._id);
		}
	},
	Preferences: function(){},
	RegisteredNotification: function(){},
	FlashMessage: function(){}
};

Timeline.Notification.prototype.delete = function() {
	return http().delete('/timeline/' + this._id)
}
Timeline.Notification.prototype.discard = function() {
	return http().put('/timeline/' + this._id)
}
Timeline.Notification.prototype.report = function() {
	return http().put('/timeline/' + this._id + '/report')
}

Timeline.Preferences.prototype.get = function(cb){
    return http().get('/userbook/preference/timeline').done(function(data){
        try {
			this.prefs = JSON.parse(data.preference)
		} catch(e) {
			this.prefs = {}
		}
        if(typeof cb === 'function')
            cb()
    }.bind(this))
}

Timeline.FlashMessage.prototype.markAsRead = function(){
    return http().put("/timeline/flashmsg/" + this.id + "/markasread")
}

export const build = function (){
	model.me.workflow.load(['directory']);
	this.makeModels(Timeline);

    this.preferences = new Timeline.Preferences()

	this.collection(Timeline.Notification, {
		page: 0,
		lastPage: false,
		loading: false,
		mine: model.notifications && model.notifications.mine,
		sync: function(paginate){
			var that = this;

			if(that.loading || (paginate && that.lastPage))
				return;

			var types = model.notificationTypes.selection();
			if(model.notificationTypes.noFilter){
				types = model.notificationTypes.all;
			}

			if(model.notificationTypes.all.length === 0) {
				that.lastPage = true;
				return;
			}
			if(!types.length){
				return;
			}

			var params = {
                page: this.page,
				type: _.map(types, function(type){
					return type.data;
				}),
                mine: 1
			};

			if(!this.mine){
				delete params.mine;
			}

			if(paginate)
				that.loading = true;

			http().get('/timeline/lastNotifications', params).done(function(response){
				that.loading = false;
				if(response.results.length > 0){
					that.addRange(response.results);
					that.page++;
				} else {
					that.lastPage = true;
					model.trigger('notifications.change')
				}

			}).error(function(data){
				that.loading = false;
				notify.error(data);
			});

			if(!this.mine)
				http().putJson('/userbook/preference/timeline', _.extend(model.preferences.prefs || {}, params));
		}
	});

	this.collection(Timeline.NotificationType, {
		mine: model.notificationTypes && model.notificationTypes.mine,
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
                    });
					model.notifications.lastPage = false;

					if(that.mine) {
						that.forEach(function(t){ t.selected = true })
						model.notifications.sync()
					} else {
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
					}
				})
			}.bind(this));
		},
		removeFilter: function(){
			model.notificationTypes.current = null;
			model.notifications.sync();
		},
		noFilter: false
	});

	this.collection(Timeline.Skin, {
		sync: function(){
			skin.listThemes(function(themes){
				this.load(themes);
			}.bind(this));
		}
	});

    this.collection(Timeline.RegisteredNotification, {
        get: function(cb){
            http().get('/timeline/registeredNotifications').done(function(data){
                this.load(data)
                if(typeof cb === 'function')
                    cb()
            }.bind(this));
        }
    });

    this.collection(Timeline.FlashMessage, {
        sync: "/timeline/flashmsg/listuser"
    })
};