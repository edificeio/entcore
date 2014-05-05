/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.utils;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class ResultMessage implements Message<JsonObject> {

	private final JsonObject body = new JsonObject().putString("status", "ok");

	public ResultMessage put(String attr, Object o) {
		body.putValue(attr, o);
		return this;
	}

	public ResultMessage error(String message) {
		body.putString("status", "error");
		body.putString("message", message);
		return this;
	}

	@Override
	public JsonObject body() {
		return body;
	}

	@Override
	public String replyAddress() {
		return null;
	}

	@Override
	public void reply() {

	}

	@Override
	public void reply(Object message) {

	}

	@Override
	public void reply(JsonObject message) {

	}

	@Override
	public void reply(JsonArray message) {

	}

	@Override
	public void reply(String message) {

	}

	@Override
	public void reply(Buffer message) {

	}

	@Override
	public void reply(byte[] message) {

	}

	@Override
	public void reply(Integer message) {

	}

	@Override
	public void reply(Long message) {

	}

	@Override
	public void reply(Short message) {

	}

	@Override
	public void reply(Character message) {

	}

	@Override
	public void reply(Boolean message) {

	}

	@Override
	public void reply(Float message) {

	}

	@Override
	public void reply(Double message) {

	}

	@Override
	public <T1> void reply(Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(Object message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(JsonObject message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(JsonArray message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(String message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(Buffer message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(byte[] message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(Integer message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(Long message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(Short message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(Character message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(Boolean message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(Float message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T1> void reply(Double message, Handler<Message<T1>> replyHandler) {

	}

}
