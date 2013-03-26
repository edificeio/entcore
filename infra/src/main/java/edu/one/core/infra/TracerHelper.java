package edu.one.core.infra;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

public class TracerHelper {

	private String address;
	private EventBus eb;
	private String appName;

	TracerHelper(EventBus eb, String address, String name) {
		this.address = address;
		this.eb = eb;
		this.appName = name.toLowerCase();
	}
	
	public void info(String logMessage){
		JsonObject tracerMessage = new JsonObject()
				.putString("app", appName)
				.putString("message", logMessage);
		eb.publish(address, tracerMessage);
	}

	public void error(String logMessage){
		JsonObject tracerMessage = new JsonObject()
				.putString("app", appName)
				.putString("message", logMessage);
		eb.publish(address, tracerMessage);
	}
}
