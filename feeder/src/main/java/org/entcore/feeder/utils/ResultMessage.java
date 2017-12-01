/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.feeder.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class ResultMessage implements Message<JsonObject> {

	private final JsonObject body = new JsonObject().put("status", "ok");
	private final Handler<JsonObject> handler;

	public ResultMessage() {
		handler = null;
	}

	public ResultMessage(Handler<JsonObject> handler) {
		this.handler = handler;
	}

	public ResultMessage put(String attr, Object o) {
		body.put(attr, o);
		return this;
	}

	public ResultMessage error(String message) {
		body.put("status", "error");
		body.put("message", message);
		return this;
	}

	@Override
	public String address() {
		return null;
	}

	@Override
	public MultiMap headers() {
		return null;
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
	public boolean isSend() {
		return false;
	}

	@Override
	public void reply(Object message) {
		if (handler != null) {
			handler.handle((JsonObject) message);
		}
	}

	@Override
	public <R> void reply(Object message, Handler<AsyncResult<Message<R>>> replyHandler) {

	}

	@Override
	public void reply(Object message, DeliveryOptions options) {

	}

	@Override
	public <R> void reply(Object message, DeliveryOptions options, Handler<AsyncResult<Message<R>>> replyHandler) {

	}

	@Override
	public void fail(int failureCode, String message) {

	}

}
