/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.auth.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import org.entcore.auth.services.ConfigurationService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.mongodb.MongoDbResult.validActionResultHandler;

public class DefaultConfigurationService implements ConfigurationService {

	private final MongoDb mongoDb = MongoDb.getInstance();
	private static final String WELCOME_MESSAGE_TYPE = "WELCOME_MESSAGE";
	public static final String PLATEFORM_COLLECTION = "platform";

	@Override
	public void editWelcomeMessage(String domain, JsonObject messages, Handler<Either<String, JsonObject>> handler) {
		if (Utils.defaultValidationParamsNull(handler, domain, messages)) return;
		final JsonObject q = new JsonObject().put("type", WELCOME_MESSAGE_TYPE);
		final JsonObject modifier = new JsonObject().put("$set", new JsonObject().put(domain.replaceAll("\\.", "_"), messages));
		mongoDb.update(PLATEFORM_COLLECTION, q, modifier, true, false, validActionResultHandler(handler));
	}

	@Override
	public void getWelcomeMessage(String domain, String language, final Handler<Either<String, JsonObject>> handler) {
		final JsonObject q = new JsonObject().put("type", WELCOME_MESSAGE_TYPE);
		JsonObject keys = null;
		if (isNotEmpty(domain) && isNotEmpty(language)) {
			keys = new JsonObject();
			keys.put("_id", 0);
			keys.put(domain.replaceAll("\\.", "_") + "." + language, 1);
			keys.put(domain.replaceAll("\\.", "_") + ".enabled", 1);
		} else if (isNotEmpty(domain)) {
			keys = new JsonObject();
			keys.put("_id", 0);
			keys.put(domain.replaceAll("\\.", "_"), 1);
		}
		mongoDb.findOne(PLATEFORM_COLLECTION, q, keys, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					JsonObject r = res.body().getJsonObject("result", new JsonObject());
					JsonObject j = new JsonObject();
					for (String attr : r.fieldNames()) {
						j.put(attr.replaceAll("_", "."), r.getValue(attr));
					}
					handler.handle(new Either.Right<String, JsonObject>(j));
				} else {
					handler.handle(new Either.Left<String, JsonObject>(res.body().getString("message", "")));
				}
			}
		});
	}

}
