Behaviours.register('conversation', {
	rights: {
		workflow: {
			create: 'org.entcore.conversation.controllers.ConversationController|send'
		}
	},
	sniplets: {
		ml: {
			title: 'sniplet.ml.title',
			description: 'sniplet.ml.description',
			controller: {
				init: function(){
					this.message = {}
				},
				initSource: function(){
					this.setSnipletSource({});
				},
				send: function(){
					this.message.to = _.map(this.snipletResource.shared, function(shared){ return shared.userId || shared.groupId });
					this.message.to.push(this.snipletResource.owner.userId);
					http().postJson('/conversation/send', this.message).done(function(){
						notify.info('ml.sent');
					}).e401(function(){});
					this.message = {}
				}
			}
		}
	}
});