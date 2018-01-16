import { Behaviours } from 'entcore';

Behaviours.register('timeline', {
	rights:{
		workflow: {
			allowLanguages: "org.entcore.timeline.controllers.TimelineController|allowLanguages",
			externalNotifications: "org.entcore.timeline.controllers.TimelineController|mixinConfig",
			historyView: "org.entcore.timeline.controllers.TimelineController|historyView",
			deleteOwnNotification: "org.entcore.timeline.controllers.TimelineController|deleteNotification",
			discardNotification: "org.entcore.timeline.controllers.TimelineController|discardNotification",
			reportNotification: "org.entcore.timeline.controllers.TimelineController|reportNotification"
		}
	}
})
