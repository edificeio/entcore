function Timetable() {}

function Structure(){}

Structure.prototype.classesMapping = function(callback) {
    http().get("/directory/timetable/classes/" + this.id)
    .done(function(data) {
        if(typeof callback === 'function') {
            callback(data);
        }
    }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
};

Structure.prototype.groupsMapping = function(callback) {
    http().get("/directory/timetable/groups/" + this.id)
    .done(function(data) {
        if(typeof callback === 'function') {
            callback(data);
        }
    }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
};

Structure.prototype.init = function(callback) {
    http().putJson("/directory/timetable/init/" + this.id, { "type" : this.timetable })
    .done(function(data) {
        if(typeof callback === 'function') {
            callback(data);
        }
    }).e400(function(e){
        var error = JSON.parse(e.responseText);
        notify.error(error.error);
    });
};

Structure.prototype.updateClassesMapping = function(cm, callback) {
    http().putJson("/directory/timetable/classes/" + this.id, cm)
    .done(function(data) {
        if(typeof callback === 'function') {
            callback(data);
        } else {
            notify.info('directory.params.success');
        }
    }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
};

Structure.prototype.updateGroupsMapping = function(gm, callback) {
    http().putJson("/directory/timetable/groups/" + this.id, gm)
    .done(function(data) {
        if(typeof callback === 'function') {
            callback(data);
        } else {
            notify.info('directory.params.success');
        }
    }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
};

Structure.prototype.import = function(formData, callback) {
    http().postFile("/directory/timetable/import/" + this.id, formData)
    .done(function(data) {
        if(typeof callback === 'function') {
            callback(data);
        } else {
            notify.info('directory.params.success');
        }
    }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
}

Structure.prototype.getReport = function(reportId, callback)
{
    http().get("/directory/timetable/import/" + this.id + "/report/" + reportId)
    .done(function(data)
    {
        if(typeof callback === 'function') {
            callback(data);
        } else {
            notify.info('directory.params.success');
        }
    }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
}

model.build = function(){
    this.makeModels([Structure])

    this.collection(Structure, {
        sync: function(){
            var that = this
            http().get('/directory/structure/admin/list').done(function(data){
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
            })
        }
    })

}