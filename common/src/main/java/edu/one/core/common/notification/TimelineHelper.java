package edu.one.core.common.notification;

import edu.one.core.infra.http.Renders;
import edu.one.core.common.user.UserInfos;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.io.IOException;
import java.util.List;

public class TimelineHelper {

		private static final String TIMELINE_ADDRESS = "wse.timeline";
		private final EventBus eb;
		private final Renders render;

		public TimelineHelper(EventBus eb, Container container) {
			this.eb = eb;
			this.render = new Renders(container);
		}

		public void notifyTimeline(HttpServerRequest request, UserInfos sender, String type, String eventType,
								   List<String> recipients, String resource, String template, JsonObject params)
				throws IOException {
			notifyTimeline(request, sender, type, eventType, recipients, resource, null, template, params);
		}

		public void notifyTimeline(HttpServerRequest request, UserInfos sender, String type, String eventType,
				List<String> recipients, String resource, String subResource, String template, JsonObject params)
				throws IOException {
			JsonArray r = new JsonArray();
			for (String userId: recipients) {
				r.addObject(new JsonObject().putString("userId", userId).putNumber("unread", 1));
			}
			JsonObject event = new JsonObject()
					.putString("action", "add")
					.putString("resource", resource)
					.putString("type", type)
					.putString("event-type", eventType)
					.putString("sender", sender.getUserId())
					.putString("message", render.processTemplate(request, template, params))
					.putArray("recipients", r);
			if (subResource != null && !subResource.trim().isEmpty()) {
				event.putString("sub-resource", subResource);
			}
			eb.send(TIMELINE_ADDRESS, event);
		}

	}
