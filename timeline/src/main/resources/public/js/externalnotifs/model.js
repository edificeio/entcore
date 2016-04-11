function Preference(){
}
function Config(){
}
function AppAction(){
}

function Appli(data){
    this.collection(AppAction)
    this.appActions.load(data.appActions)
}

Preference.prototype.getinfo = function(callback){
    http().get('/userbook/preference/timeline').done(function(data){
        this.preference = JSON.parse(data.preference)
        if(typeof callback === 'function')
            callback()
    }.bind(this))
}

Preference.prototype.putinfo = function(){
    var json = this.preference
    http().putJson('/userbook/preference/timeline', json).done(function(){
        notify.info('clef')
        window.location = "/userbook/mon-compte";
    })
}

model.build = function(){
	this.makeModels([Preference, Config, Appli, AppAction]);
    this.preference = new Preference();

    this.collection(Appli, {
        list: function(){
            http().get('/timeline/notifications-defaults').done(function(data){

                data = _.reject(data, function(notif){
                    return notif.restriction === "INTERNAL"
                })
                data = _.filter(data, function(notif){
                    return _.find(model.me.apps, function(app){
                        return (notif.type.toLowerCase() === app.name.toLowerCase() ||
                            (notif['app-name'] && notif['app-name'].toLowerCase() === app.name.toLowerCase()))
                    })
                })
                data=_.map(_.groupBy(data, 'type'), function(item){
                    return{
                        appActions: item,
                        appName: item[0]['app-name'],
                        type: item[0]['type'],
                        appAddress: item[0]['app-address'],
                        eventType: item[0]['event-type']
                    }
                })

                this.load(data);

                model.preference.getinfo(function(){
                    if(!model.preference.preference.config)
                        return
            		model.applis.each(function(appli){
                        appli.appActions.each(function(appAction){
                            if(model.preference.preference.config[appAction.key]){
                                appAction.defaultFrequency = model.preference.preference.config[appAction.key].defaultFrequency
                            }
                        })
                    })
                });
            }.bind(this))
        }
    })
};
