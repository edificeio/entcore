function User(){}

function Group(){
    this.collection(User, {
        sync: function(){
            var that = this
            http().get('/directory/user/admin/list', { groupId: that.model.id }).done(function(data){
                that.load(data)
                model.scope.$apply()
            })
        }
    })
}

Group.prototype.getCommunication = function() {
    var group = this
    http().get('group/' + this.id).done(function(data) {
        var backupUsers = data.users
        delete data.users
        group.updateData(data)
        group.communiqueUsers = backupUsers
        model.scope.$apply()
    })
}

Group.prototype.addGroupLink = function(otherGroupId) {
    var group = this
    http().post('group/' + this.id + '/communique/' + otherGroupId).done(function(){
        //notify.info(lang.translate("communication.notify.groupLinkAdded"))
        group.getCommunication()
    })
}

Group.prototype.removeGroupLink = function(otherGroupId) {
    var group = this
    http().delete('group/' + this.id + '/communique/' + otherGroupId).done(function() {
        //notify.info(lang.translate("communication.notify.groupLinkRemoved"))
        group.getCommunication()
    })
}

Group.prototype.addLinksWithUsers = function(direction) {
    var group = this
    http().post('group/' + this.id + "?direction="+direction).done(function() {
        notify.info(lang.translate("communication.notify.groupUserLinksAdded"))
        group.getCommunication()
    })
}

Group.prototype.removeLinksWithUsers = function(direction) {
    var group = this
    http().delete('group/' + this.id + "?direction="+direction).done(function() {
        notify.info(lang.translate("communication.notify.groupUserLinksDeleted"))
        group.getCommunication()
    })
}

Group.prototype.addLinkBetweenRelativeAndStudent = function(direction) {
    var group = this
    http().post('relative/' + this.id + "?direction="+direction).done(function() {
        notify.info(lang.translate("communication.notify.groupRelativeLinksAdded"))
        group.getCommunication()
    })
}

Group.prototype.removeLinkBetweenRelativeAndStudent = function(direction) {
    var group = this
    http().delete('relative/' + this.id + "?direction="+direction).done(function() {
        notify.info(lang.translate("communication.notify.groupRelativeLinksDeleted"))
        group.getCommunication()
    })
}


function Structure(){
    this.collection(Group, {
        sync: function(){
            var that = this
            http().get('/directory/group/admin/list', { structureId: that.model.id }, { requestName: 'load-group-comm' }).done(function(data){
                that.load(data)
                that.forEach(function(group){
                    group.getCommunication()
                })
            })
        }
    })
}

model.build = function(){
    this.makeModels([Structure, Group, User])

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
                if(model.scope)
                    model.scope.$apply()
            })
        }
    })

}
