/*
 * Copyright © "Open Digital Education", 2014
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

 */

package org.entcore.common.bus;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BusResponseHandler {

	private BusResponseHandler() {}

	public static Handler<Either<String, JsonObject>> busResponseHandler(final Message<JsonObject> message) {
		return new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					message.reply(new JsonObject().put("status", "ok")
							.put("result", event.right().getValue()));
				} else {
					JsonObject error = new JsonObject()
							.put("status", "error")
							.put("message", event.left().getValue());
					message.reply(error);
				}
			}
		};
	}

	public static Handler<Either<String, JsonArray>> busArrayHandler(final Message<JsonObject> message) {
		return new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> event) {
				if (event.isRight()) {
					message.reply(new JsonObject().put("status", "ok")
							.put("result", event.right().getValue()));
				} else {
					JsonObject error = new JsonObject()
							.put("status", "error")
							.put("message", event.left().getValue());
					message.reply(error);
				}
			}
		};
	}

}
