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

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;

public class MongoDbEventStore extends GenericEventStore {

	private final MongoDb mongoDb = MongoDb.getInstance();
	private static final String COLLECTION = "events";

	@Override
	protected void storeEvent(final JsonObject event, final Handler<Either<String, Void>> handler) {
		mongoDb.insert(COLLECTION, event, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					handler.handle(new Either.Right<String, Void>(null));
				} else {
					handler.handle(new Either.Left<String, Void>(
							"Error : " + res.body().getString("message") + ", Event : " + event.encode()));
				}
			}
		});
	}

}
