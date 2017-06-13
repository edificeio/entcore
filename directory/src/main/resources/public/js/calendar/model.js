function Timetable() {}

function Structure() {}

function Component() {
  this.profilesList;
}

var hookCheck = function(hook){
    if(typeof hook === 'function') {
        hook()
    }
}

Component.prototype.slotprofiles = function(id, callback) {
    var that = this
    http().get("/directory/slotprofiles/schools/" + id)
    .done(function(data) {
        that.profilesList = data;
        returnData(callback, [data])
    }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
};


Component.prototype.updateSlotProfile = function(slotprofile, callback) {
    http().putJson("/directory/slotprofiles/" + slotprofile._id, {
        name : slotprofile.name,
        schoolId : slotprofile.schoolId,
        slots : slotprofile.slots
    }).done(function(data) {
        hookCheck(callback);
        notify.info(lang.translate("directory.notify.slotProfileUpdated"));
    }).error(function(e){
        var error = JSON.parse(e.responseText);
        notify.error(error.error);
    });
};

var returnData = function(hook, params){
    if(typeof hook === 'function')
        hook.apply(this, params)
};

Component.prototype.slots = function(slotProfileId, callback) {
    return http().get("/directory/slotprofiles/" + slotProfileId + "/slots")
        .done(function(data) {
            returnData (callback, [data]);
        }).e400(function(e){
            var error = JSON.parse(e.responseText);
            if(typeof callback === 'function') {
                callback(error);
            } else {
                notify.error(error.error);
            }
        });
};

Component.prototype.save = function(slotprofile, callback) {
    http().postJson("/directory/slotprofiles" , slotprofile)
    .done(function(data) {
        hookCheck(callback);
        notify.info(lang.translate("directory.notify.slotProfileCreated"));
    }).error(function(e){
        var error = JSON.parse(e.responseText);
        notify.error(error.error);
    });
};

Component.prototype.saveSlot = function(slotProfileId, slot, callback) {
   http().postJson("/directory/slotprofiles/" + slotProfileId + "/slots", slot)
    .done(function(data) {
        hookCheck(callback);
        notify.info(lang.translate("directory.notify.slotCreated"))
    }).error(function(e){
        var error = JSON.parse(e.responseText);
        notify.error(error.error);
    });
};

Component.prototype.deleteSlot = function(slotprofileId, slotId, callback) {
    http().delete("/directory/slotprofiles/" + slotprofileId + "/slots/" + slotId)
        .done(function(data) {
            hookCheck(callback);
            notify.info(lang.translate("directory.notify.slotDeleted"))
        }).error(function(e){
        var error = JSON.parse(e.responseText);
        notify.error(error.error);
    });
};

Component.prototype.updateSlot = function(slotprofileId, slot, callback) {
    http().putJson("/directory/slotprofiles/" + slotprofileId + "/slots/" + slot.id, {
        name : slot.name,
        startHour : slot.startHour,
        endHour : slot.endHour
        }).done(function(data) {
            hookCheck(callback);
            notify.info(lang.translate("directory.notify.slotUpdated"))
        }).error(function(e){
        var error = JSON.parse(e.responseText);
        notify.error(error.error);
    });
};

model.build = function(){
    this.makeModels([Structure])

    this.collection(Structure, {
        sync: function(hook){
            var that = this
            http().get('structure/admin/list').done(function(data){
                that.load(data)

                _.forEach(that.all, function(struct){
                    struct.parents = _.filter(struct.parents, function(parent){
                        var parentMatch = _.findWhere(that.all, {id: parent.id})
                        if(parentMatch){
                            parentMatch.children = parentMatch.children ? parentMatch.children : []
                            parentMatch.children.push(struct)
                            return true
                        } else
                            return false
                    })
                    if(struct.parents.length === 0)
                        delete struct.parents
                })

                hookCheck(hook)
            })
        }
    })

}
