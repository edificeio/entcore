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

User.prototype.update = function(hook){
    var that = this
    http().putJson("user/"+that.id, {
        firstName:      that.firstName,
        lastName:       that.lastName,
        displayName:    that.firstName+" "+that.lastName,
        birthDate:      that.birthDate ? moment(that.birthDate).format("YYYY-MM-DD") : null,
        address:        that.address,
        city:           that.city,
        zipCode:        that.zipCode
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

User.prototype.get = function(hook, getQuota){
    var that = this
    http().get("user/" + that.id).done(function(data){
        for(var prop in data){
            if(prop === 'type'){
                if(data[prop].length > 0)
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
        http().postJson("/directory/user/function/"+that.id, {
            functionCode: "ADMIN_LOCAL",
            scope: [structure.id]
        }).done(function(){
            notify.info(lang.translate("directory.notify.setLocalAdmin"))
        })
}

User.prototype.removeLocalAdmin = function(){
    var that = this
    http().delete("/directory/user/function/"+that.id+"/ADMIN_LOCAL").done(function(){
        notify.info(lang.translate("directory.notify.removeLocalAdmin"))
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

//Manual groups
function ManualGroup(){}
ManualGroup.prototype = {
    save: function(structure, hook){
        var that = this
        http().postJson("group", {
            name: that.name,
            structureId: structure.id
        }).done(function(){
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

function Structure(){

    this.collection(Classe, {
        sync: function(hook){
            var that = this
            return http().get('class/admin/list', { structureId: that.model.id }).done(function(classes){
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
            return http().get('group/admin/list', { type: 'ManualGroup', structureId: that.model.id }, { requestName: 'groups-requests' }).done(function(groups){
                that.load(groups)
                that.forEach(function(group){ group.getUsers() })
                hookCheck(hook)
            })
        }
    })

    this.linkUser = function(user, hook){
        http().put("structure/"+this.id+"/link/"+user.id).done(function(){
            hookCheck(hook)
        })
    }

    this.unlinkUser = function(user, hook){
        http().delete("structure/"+this.id+"/unlink/"+user.id).done(function(){
            hookCheck(hook)
        })
    }

    this.loadStructure = function(periodicHook, endHook){
        var structure = this

        structure.classes.sync(function(){
            structure.users.removeAll()

            //Add isolated users
            http().get("list/isolated", { structureId: structure.id }, { requestName: 'user-requests' }).done(function(data){
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
    this.addClassUsers = function(classe, hookArray, initClasses){
        var structure = this
        http().get("user/admin/list", { classId: classe.id }, { requestName: 'user-requests' }).done(function(data){
            _.forEach(data, function(u){
                var existing = _.findWhere(structure.users.all, {id: u.id})
                if(existing){
                    existing.classesList    = !existing.classesList  ? [] : existing.classesList
                    existing.totalClasses   = !existing.totalClasses ? [] : existing.totalClasses
                    existing.classesList.push(classe)
                    if(initClasses)
                        existing.totalClasses.push(classe)
                } else {
                    u.classesList = [ classe ]
                    if(initClasses)
                        u.totalClasses = [ classe ]
                    structure.users.all.push(new User(u))
                }
            })
            _.forEach(hookArray, function(h){
                hookCheck(h)
            })
        })
    }

    //Used in conjunction with addClassUsers when filtering the user list, removes from the array the class associated with the users.
    this.removeClassUsers = function(classe, hook){
        var structure = this
        http().get("user/admin/list", { classId: classe.id }, { requestName: 'user-requests' }).done(function(data){
            _.forEach(data, function(u){
                var existingUser = _.findWhere(structure.users.all, { id: u.id })
                if(existingUser){
                    existingUser.classesList = _.reject(existingUser.classesList, function(c){ return classe.id === c.id })
                }
            })
            hookCheck(hook)
        })
    }
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

model.build = function(){
    this.makeModels([User, IsolatedUsers, Structure, Classe, ManualGroup])

    this.collection(Structure, {
        sync: function(hook){
            var that = this
            http().get('structure/admin/list').done(function(data){
                that.load(data)
                hookCheck(hook)
            })
        }
    })

    this.isolatedUsers = new IsolatedUsers()

}
