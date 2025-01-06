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

package org.entcore.common.events.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class BusEventStore extends GenericEventStore {

	@Override
	protected void storeEvent(final JsonObject event, final Handler<Either<String, Void>> handler) {
		eventBus.request("event.store", event, (Handler<AsyncResult<Message<JsonObject>>>) res -> {
      if (res.succeeded()) {
        handler.handle(new Either.Right<>(null));
      } else {
        handler.handle(new Either.Left<>(
            "Error : " + res.cause().getMessage() + ", Event : " + event.encode()));
      }
    });
	}

	@Override
	public void storeCustomEvent(String baseEventType, JsonObject payload) {
		eventBus.request("event.store.custom", new JsonObject()
				.put("base-event-type", baseEventType).put("payload", payload));
	}

}
