package com.wse.neo4j.exception;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class ExceptionUtils {

	public static JsonObject exceptionToJson(Throwable e) {
		JsonArray stacktrace = new JsonArray();
		for (StackTraceElement s: e.getStackTrace()) {
			stacktrace.add(s.toString());
		}
		return new JsonObject()
			.putString("message", e.getMessage())
			.putString("exception", e.getClass().getSimpleName())
			.putString("fullname", e.getClass().getName())
			.putArray("stacktrace", stacktrace);
	}

}
