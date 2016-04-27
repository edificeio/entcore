console.log('timeline behaviours loaded');

Behaviours.register('timeline', {
	rights:{
		workflow: {
			externalNotifications: "org.entcore.timeline.controllers.TimelineController|mixinConfig"
		}
	}
});
