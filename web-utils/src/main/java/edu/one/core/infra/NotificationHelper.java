package edu.one.core.infra;

import java.io.IOException;
import java.util.List;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.infra.http.Renders;
import edu.one.core.infra.security.resources.UserInfos;

public class NotificationHelper {

	private static final String TIMELINE_ADDRESS = "wse.timeline";
	private final EventBus eb;
	private final Renders render;

	public NotificationHelper(EventBus eb, Container container) {
		this.eb = eb;
		this.render = new Renders(container);
	}

	public void notifyTimeline(HttpServerRequest request, UserInfos sender,
			List<String> recipients, String resource, String template, JsonObject params)
					throws IOException {
		JsonObject event = new JsonObject()
		.putString("action", "add")
		.putString("resource", resource)
		.putString("sender", sender.getUserId())
		.putString("message", render.processTemplate(request, template, params))
		.putArray("recipients", new JsonArray(recipients.toArray()));
		eb.send(TIMELINE_ADDRESS, event);
	}

	public void deleteFromTimeline(String resource) {
		JsonObject json = new JsonObject()
		.putString("action", "delete")
		.putString("resource", resource);
		eb.send(TIMELINE_ADDRESS, json);
	}

}
