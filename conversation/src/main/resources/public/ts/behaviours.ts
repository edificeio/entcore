import { Behaviours, http, notify, _ } from 'entcore';

Behaviours.register('conversation', {
    rights: {
        workflow: {
            draft: 'org.entcore.conversation.controllers.ConversationController|createDraft',
            read: 'org.entcore.conversation.controllers.ConversationController|view'
        }
    },
    sniplets: {
        ml: {
            title: 'sniplet.ml.title',
            description: 'sniplet.ml.description',
            controller: {
                init: function () {
                    this.message = {}
                },
                initSource: function () {
                    this.setSnipletSource({});
                },
                send: function () {
                    this.message.to = _.map(this.snipletResource.shared, function (shared) { return shared.userId || shared.groupId });
                    this.message.to.push(this.snipletResource.owner.userId);
                    http().postJson('/conversation/send', this.message).done(function () {
                        notify.info('ml.sent');
                    }).e401(function () { });
                    this.message = {}
                }
            }
        }
    }
});