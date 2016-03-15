function Config(){}
Config.prototype.toJSON = function(){
    return {
        key: this.key,
        defaultFrequency: this.defaultFrequency,
        restriction: this.restriction
    }
}
Config.prototype.upsert = function(){
    return http().putJson('/timeline/config', this)
}

function Notification(){}

model.build = function(){
    this.makeModels([Config, Notification])

    this.collection(Config, {
        sync: function(cb){
            http().get('/timeline/config').done(function(config){
                this.load(config);
                if(typeof cb === 'function')
                    cb()
            }.bind(this))
        }
    })

    this.collection(Notification, {
        sync: function(){
            http().get('/timeline/registeredNotifications').done(function(notifs){
                this.load(notifs)
                model.configs.sync(function(){
                    model.notifications.forEach(function(notif){
                        var configObj = model.configs.findWhere({key: notif.key})
                        if(configObj){
                            _.extend(notif, configObj)
                            notif.config = configObj
                        } else {
                            notif.config = new Config({
                                key: notif.key,
                                defaultFrequency: notif.defaultFrequency,
                                restriction: notif.restriction
                            })
                        }
                    })
                })
            }.bind(this))
        }
    })
};
