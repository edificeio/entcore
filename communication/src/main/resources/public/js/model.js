// Copyright © WebServices pour l'Éducation, 2014
//
// This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation (version 3 of the License).
//
// For the sake of explanation, any module that communicate over native
// Web protocols, such as HTTP, with ENT Core is outside the scope of this
// license and could be license under its own terms. This is merely considered
// normal use of ENT Core, and does not fall under the heading of "covered work".
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
        group.data.users = data.users
        delete data.users
        group.updateData(data)
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
            http().get('/directory/group/admin/list', { structureId: that.model.id }).done(function(data){
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
                if(model.scope) model.scope.$apply()
            })
        }
    })

}
