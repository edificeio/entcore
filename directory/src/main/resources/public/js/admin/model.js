var hookCheck = function(hook){
    if(typeof hook === 'function')
        hook()
}

function Launcher(times, execute){
    var count = times
    this.exec = typeof execute === 'function' ? function(){
        if(--count === 0)
            execute()
    } : function(){}
}

function User(){}

User.prototype.create = function(hook){
    var that = this
    $.ajax("api/user", {
        type: "POST",
        traditional: true,
        data: {
            classId:        that.classId,
            structureId:    that.structureId,
            firstname:      that.firstName,
            lastname:       that.lastName,
            type:           that.type,
            birthDate:      that.birthDate ? moment(that.birthDate).format("YYYY-MM-DD") : null,
            childrenIds:    _.map(that.children, function(child){ return child.id })
        }
    }).done(function(){
        notify.info(lang.translate("directory.notify.userCreated"))
        hookCheck(hook)
    }).fail(function(){
        notify.error(lang.translate("directory.notify.userCreationError"))
    })

}

User.prototype.get = function(hook, getQuota){
    var that = this
    http().get("user/" + that.id).done(function(data){
        for(var prop in data){
            if(prop === 'type'){
                if(data[prop] && data[prop].length > 0)
                    that[prop] = data[prop][0]
            } else
                that[prop] = data[prop]
        }
        if(getQuota && !that.code)
            that.getQuota(hook)
        else
            hookCheck(hook)
    })
}

User.prototype.update = function(hook){
    var that = this
    http().putJson("user/"+that.id, {
        firstName:      that.firstName,
        lastName:       that.lastName,
        displayName:    that.displayName,
        birthDate:      that.birthDate ? moment(that.birthDate).format("YYYY-MM-DD") : null,
        address:        that.address,
        city:           that.city,
        zipCode:        that.zipCode,
        email:          that.email,
        homePhone:      that.homePhone,
        mobile:         that.mobile
    }).done(function(){
        notify.info(lang.translate("directory.notify.userUpdate"))
        hookCheck(hook)
    })
}

User.prototype.delete = function(hook){
    var that = this
    http().delete("user?userId="+that.id).done(function(){
        hookCheck(hook)
    })
}

User.prototype.restore = function(hook){
    var that = this
    http().put("restore/user?userId="+that.id).done(function(){
        hookCheck(hook)
    })
}

User.prototype.getQuota = function(hook){
    var that = this
    var req = http().get("/workspace/quota/user/" + that.id)
    req.done(function(data){
        that.quota = data.quota
        that.storage = data.storage
        hookCheck(hook)
    })
}

User.prototype.saveQuota = function(){
    var that = this
    http().putJson("/workspace/quota", {
        users: [that.id],
        quota: that.quota
    }).done(function(){
        notify.info(lang.translate("directory.notify.quotaUpdate"))
    })
}

User.prototype.setLocalAdmin = function(structure){
        var that = this
        var structureIds = [structure.id]
        if(that.functions){
            var localAdmin = _.find(that.functions, function(f){ return f[0] === "ADMIN_LOCAL" } )
            if(localAdmin){
                for(var i = 0; i < localAdmin[1].length; i++)
                    structureIds.push(localAdmin[1][i])
            }
        }
        return http().postJson("/directory/user/function/"+that.id, {
            functionCode: "ADMIN_LOCAL",
            inherit: "s",
            scope: structureIds
        }).done(function(){
            notify.info(lang.translate("directory.notify.setLocalAdmin"))
            that.get()
        }).e504(function(){
            notify.info(lang.translate("directory.notify.setLocalAdmin.504"))
        })
}

User.prototype.removeLocalAdmin = function(){
    var that = this
    return http().delete("/directory/user/function/"+that.id+"/ADMIN_LOCAL").done(function(){
        notify.info(lang.translate("directory.notify.removeLocalAdmin"))
        that.get()
    })
}

User.prototype.setCentralAdmin = function(structure){
        var that = this
        http().postJson("/directory/user/function/"+that.id, {
            functionCode: "SUPER_ADMIN",
            scope: []
        }).done(function(){
            notify.info(lang.translate("directory.notify.setCentralAdmin"))
            that.get()
        })
}

User.prototype.removeCentralAdmin = function(){
    var that = this
    http().delete("/directory/user/function/"+that.id+"/SUPER_ADMIN").done(function(){
        notify.info(lang.translate("directory.notify.removeCentralAdmin"))
        that.get()
    })
}

//Send an email containing a new activation code.
//An error will be thrown server side if the code is not empty (neo4j side)
User.prototype.sendResetPassword = function(mail){
    var that = this
    $.post("/auth/sendResetPassword", {
        login: that.login,
        email: mail
    }).success(function(){
        notify.info(lang.translate("directory.notify.mailSent"))
    }).error(function(){
        notify.error(lang.translate("directory.notify.mailError"))
    })
}

User.prototype.block = function(hook){
    var user = this
    http().putJson('/auth/block/' + user.id, { block: true }).done(function(){
        user.blocked = true
        hookCheck(hook)
    })
}

User.prototype.unblock = function(hook){
    var user = this
    http().putJson('/auth/block/' + user.id, { block: false }).done(function(){
        user.blocked = false
        hookCheck(hook)
    })
}

User.prototype.linkChild = function(child, hook){
    var parent = this
    return http().put("user/"+child.id+"/related/"+parent.id).done(function(){
        hookCheck(hook)
    })
}

User.prototype.unlinkChild = function (child, hook){
    var parent = this
    return http().delete("user/"+child.id+"/related/"+parent.id).done(function(){
        hookCheck(hook)
    })
}

User.prototype.isRemovable = function (){
    var user = this
    return (user.disappearanceDate || (user.source !== 'AAF' && user.source !== "AAF1D" && user.source !== "BE1D"));
}

function Classe(){
    this.sync = function(hook){
        var that = this
        return http().get('class/admin/list', { classId: that.id }).done(function(classes){
            that.load(classes)
            hookCheck(hook)
        })
    }

    this.linkUser = function(user, hook){
        http().put("class/"+this.id+"/link/"+user.id).done(function(){
            hookCheck(hook)
        })
    }

    this.unlinkUser = function(user, hook){
        http().delete("class/"+this.id+"/unlink/"+user.id).done(function(){
            hookCheck(hook)
        })
    }

    this.update = function(hook){
        var that = this
        http().putJson("class/"+that.id,{
            name: that.name
        }).done(function(){
            notify.info(lang.translate("directory.notify.classUpdate"))
            hookCheck(hook)
        })
    }
}

Classe.prototype.toString = function(){
    return this.name
}

//Manual groups
function ManualGroup(){}
ManualGroup.prototype = {
    save: function(structure, hook){
        var that = this

        var postData = {
            name: that.name
        }
        if(that.classId)
            postData.classId = that.classId.id
        else
            postData.structureId = structure.id

        http().postJson("group", postData).done(function(){
            notify.info(lang.translate("directory.notify.groupUpdate"))
            hookCheck(hook)
        })
    },
    update: function(hook){
        var that = this
        http().putJson("group/"+that.id, {
            name: that.name,
        }).done(function(){
            notify.info(lang.translate("directory.notify.groupUpdate"))
            hookCheck(hook)
        })
    },
    delete: function(hook){
        var that = this
        http().delete("group/"+that.id).done(function(){
            notify.info(lang.translate("directory.notify.groupDeleted"))
            hookCheck(hook)
        })
    },
    getUsers: function(hook){
        var that = this
        return http().get("user/group/"+that.id).done(function(data){
            that.data.users = data
            hookCheck(hook)
        })
    },
    addUser: function(user, hook){
        var that = this
        return http().post("user/group/"+user.id+"/"+that.id).done(function(){
            hookCheck(hook)
        })
    },
    removeUser: function(user, hook){
        var that = this
        return http().delete("user/group/"+user.id+"/"+that.id).done(function(){
            hookCheck(hook)
        })
    }
}

//Duplicates
function Duplicate(){}
Duplicate.prototype = {
    merge: function(hook){
        var that = this
        return http().put("duplicate/merge/" + that.user1.id + "/" + that.user2.id).done(function(){
            hookCheck(hook)
        })
    },
    dissociate: function(hook){
        var that = this
        return http().delete("duplicate/ignore/" + that.user1.id + "/" + that.user2.id).done(function(){
            hookCheck(hook)
        })
    }
}

function Structure(){

    this.collection(Classe, {
        sync: function(hook){
            var that = this
            return http().get('class/admin/list', { structureId: that.model.id }, { requestName: 'classes-request' }).done(function(classes){
                that.load(classes)
                that.selectAll()
                hookCheck(hook)
            })
        }
    })

    this.collection(User, {})

    this.collection(ManualGroup, {
        sync: function(hook){
            var that = this
            return http().get('group/admin/list', { type: 'ManualGroup', structureId: that.model.id }, { requestName: 'groups-request' }).done(function(groups){
                that.load(groups)
                that.forEach(function(group){ group.getUsers() })
                hookCheck(hook)
            })
        }
    })

    this.collection(Duplicate, {
        sync: function(hook){
            var that = this
            return http().get('duplicates', { inherit: "true", structure: that.model.id}, { requestName: 'duplicates-request'}).done(function(duplicates){
                that.load(duplicates)
                hookCheck(hook)
            })
        }
    })

    this.collection(Level, {
        sync: function(hook){
            var that = this
            return http().get('structure/'+that.model.id+'/levels').done(function(levels){
                that.load(levels)
                hookCheck(hook)
            })
        }
    })
}

Structure.prototype.create = function(hook){
    var that = this
    http().postJson("school", {
        name: that.name,
    }).done(function(){
        notify.info(lang.translate("directory.notify.structureCreated"))
        hookCheck(hook)
    })
}

Structure.prototype.update = function(hook){
    var that = this
    http().putJson("structure/"+that.id, {
        name: that.name,
    }).done(function(){
        notify.info(lang.translate("directory.notify.structureUpdate"))
        hookCheck(hook)
    })
}

Structure.prototype.linkUser = function(user, hook){
    http().put("structure/"+this.id+"/link/"+user.id).done(function(){
        hookCheck(hook)
    })
}

Structure.prototype.unlinkUser = function(user, hook){
    http().delete("structure/"+this.id+"/unlink/"+user.id).done(function(){
        hookCheck(hook)
    })
}

//Save the quota for profiles
Structure.prototype.saveStructureQProfile = function(structure) {
    http().putJson("/workspace/structure/admin/quota/saveProfile", structure.pgroup)
        .done(function() {
            notify.info('directory.params.success');
        }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
    notify.info(lang.translate("directory.notify.quotaUpdate"));
}

//Save the quota for structure
Structure.prototype.saveStructureQStructure = function(structure) {
    http().putJson("/workspace/structure/admin/quota/saveStructure", structure)
        .done(function() {
            notify.info('directory.params.success');
        }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
    notify.info(lang.translate("directory.notify.quotaUpdate"));
}

//Save the quota for activity
Structure.prototype.saveStructureQActivity = function(structure) {
    http().putJson("/workspace/structure/admin/quota/saveActivity", structure.quotaActivity)
        .done(function() {
            notify.info('directory.params.success');
        }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
    notify.info(lang.translate("directory.notify.quotaUpdate"));
}

Profiles.prototype.save = function(block, callback) {
    http().putJson("/directory/profiles", block)
        .done(function(data) {
            notify.info('directory.params.success');
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


Structure.prototype.getUsersQuotaActivity = function(structureid, quotaFilterNbusers, quotaFilterSortBy, quotaFilterOrderBy, quotaFilterProfile, quotaFilterPercentageLimit, callback) {
    var structure = this;
    http().get('/workspace/structure/admin/quota/getUsersQuotaActivity',
                    {structureid : structureid,
                    quotaFilterNbusers : quotaFilterNbusers,
                    quotaFilterSortBy : quotaFilterSortBy,
                    quotaFilterOrderBy : quotaFilterOrderBy,
                    quotaFilterProfile : quotaFilterProfile,
                    quotaFilterPercentageLimit: quotaFilterPercentageLimit
                    })
        .done(function(data) {
                _.forEach(data, function(u){
                   var DEFAULT_QUOTA_UNIT = 1073741824;
                    u.quotaOri = u.quota;
                    u.quota = Math.round(u.quota * 100 / DEFAULT_QUOTA_UNIT) / 100;
                    u.maxquota = Math.round(u.maxquota * 100 / DEFAULT_QUOTA_UNIT) / 100;
                    u.storageOri = u.storage;
                    u.storage = Math.round(u.storage * 100 / DEFAULT_QUOTA_UNIT) / 100;
                    u.unit = DEFAULT_QUOTA_UNIT;
                    structure.quotaActivity.push(u);
                });
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
}


Structure.prototype.loadStructure = function(periodicHook, endHook){
    var structure = this

    structure.classes.sync(function(){
        structure.users.removeAll()

        //Add all users
        http().get("user/admin/list", { structureId: structure.id }, { requestName: 'user-requests' }).done(function(data){
            _.forEach(data, function(u){
                var user = new User(u)
                u.isolated = true
                structure.users.all.push(new User(u))
            })

            var endHookLauncher = new Launcher(structure.classes.length() + 1, endHook)
            endHookLauncher.exec()

            //For each class, flag users.
            structure.classes.forEach(function(classe){
                structure.addClassUsers(classe, [periodicHook, endHookLauncher.exec], true)
            })
        })
    })
}

//Used when filtering the user list, adds in arrays which classes a user belongs to.
Structure.prototype.addClassUsers = function(classe, hookArray, initClasses){
    var structure = this
    var classUsers = _.filter(structure.users.all, function(user){
        return _.findWhere(user.allClasses, {id: classe.id})
    })
    _.forEach(classUsers, function(u){
        u.classesList    = !u.classesList  ? [] : u.classesList
        u.totalClasses   = !u.totalClasses ? [] : u.totalClasses
        u.isolated = false
        u.classesList.push(classe)
        if(initClasses)
            u.totalClasses.push(classe)
    })
    _.forEach(hookArray, function(h){
        hookCheck(h)
    })
}

//Used in conjunction with addClassUsers when filtering the user list, removes from the array the class associated with the users.
Structure.prototype.removeClassUsers = function(classe, hook){
    var structure = this
    var classUsers = _.filter(structure.users.all, function(user){
        return _.findWhere(user.allClasses, {id: classe.id})
    })

    _.forEach(classUsers, function(u){
        u.classesList = _.reject(u.classesList, function(c){ return classe.id === c.id })
    })

    hookCheck(hook)
}

Structure.prototype.attachParent = function(parent, hook){
    var structure = this
    http().put("structure/"+structure.id+"/parent/"+parent.id).done(function(){
        if(!structure.parents)
            structure.parents = []
        structure.parents.push({id: parent.id, name: parent.name})

        if(!parent.children)
            parent.children = []
        parent.children.push(structure)

        hookCheck(hook)
    })
}

Structure.prototype.detachParent = function(parent, hook){
    var structure = this
    http().delete("structure/"+structure.id+"/parent/"+parent.id).done(function(){
        structure.parents = _.filter(structure.parents, function(p){ return p.id !== parent.id })
        parent.children = _.filter(parent.children, function(c){ return c.id !== structure.id })

        hookCheck(hook)
    })
}

Structure.prototype.toString = function(){
    return this.name;
}

//Levels
function Level(){}
Level.prototype.toString = function(){ return this.name }

//Collection of structures
function Structures(){
    this.collection(Structure, {
        sync: function(hook){
            var that = this
            http().get('structure/admin/list').done(function(data){
                that.load(data)

                _.forEach(that.all, function(struct){
                    var DEFAULT_QUOTA_UNIT = 1073741824;
                    // update quota with unit
                    _.forEach(struct.pgroup, function(pgroup){
                        pgroup.quotaOri = pgroup.quota;
                        pgroup.maxquotaOri = pgroup.maxquota;
                        pgroup.quota = Math.round(pgroup.quota * 100 / DEFAULT_QUOTA_UNIT) / 100;
                        pgroup.maxquota = Math.round(pgroup.maxquota * 100 / DEFAULT_QUOTA_UNIT) / 100;
                        pgroup.unit = DEFAULT_QUOTA_UNIT;
                    })
                    struct.quotaActivity = [];
                    struct.quotaOri = struct.quota;
                    struct.quota = Math.round(struct.quota * 100 / DEFAULT_QUOTA_UNIT) / 100;
                    struct.unit = DEFAULT_QUOTA_UNIT;
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

//Collection of users with no relation to classes or structures
function IsolatedUsers(){
    this.collection(User, {
        sync: function(){
            var that = this
            return http().get('list/isolated', {}, { requestName: 'isolated-request' }).done(function(users){
                that.load(users);
            })
        }
    })
}

function CrossUsers(){
    this.collection(User, {
        sync: function(filter){
            var that = this
            return http().get('user/admin/list', { name: filter}, { requestName: 'cross-search-request' }).done(function(users){
                that.load(users);
            })
        }
    })
}

function Profile(){}
function Profiles() {
    this.collection(Profile, {
        sync: function(hook) {
            var that = this
            return http().get('profiles', {}, { requestName: 'profiles-request' }).done(function(profiles){
                that.load(profiles);
                hookCheck(hook);
            })
        }
    });
}

Profiles.prototype.save = function(block, callback) {
    http().putJson("/directory/profiles", block)
    .done(function(data) {
        notify.info('directory.params.success');
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

model.build = function(){
    this.makeModels([User, IsolatedUsers, CrossUsers, Structure, Structures, Classe, ManualGroup, Duplicate, Level, Profile, Profiles])

    this.structures = new Structures()
    this.isolatedUsers = new IsolatedUsers()
    this.crossUsers = new CrossUsers()
    this.profiles = new Profiles();
}
