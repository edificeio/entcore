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

package org.entcore.common.bus;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class ErrorMessage implements Message<JsonObject> {

	private final JsonObject body = new JsonObject();

	public ErrorMessage(String error) {
		body.putString("status", "error").putString("message", error);
	}

	@Override
	public String address() {
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
	public <T> void replyWithTimeout(long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Object message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Object message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(JsonObject message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(JsonObject message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(JsonArray message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(JsonArray message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(String message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(String message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Buffer message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Buffer message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(byte[] message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(byte[] message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Integer message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Integer message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Long message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Long message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Short message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Short message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Character message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Character message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Boolean message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Boolean message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Float message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Float message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Double message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Double message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public void fail(int failureCode, String message) {

	}

}
