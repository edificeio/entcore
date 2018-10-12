/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
