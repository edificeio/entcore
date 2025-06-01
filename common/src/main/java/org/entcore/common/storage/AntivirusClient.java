/*
 * Copyright © "Open Digital Education", 2017
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

package org.entcore.common.storage;


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;

import org.entcore.common.storage.impl.HttpAntivirusClient;

import fr.wseduc.webutils.Utils;

import java.util.Optional;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public interface AntivirusClient {

	void scan(String path);

	void scan(String path, Handler<AsyncResult<Void>> handler);

	void scanS3(String id, String bucket);

	void scanS3(String id, String bucket, Handler<AsyncResult<Void>> handler);

	static Future<Optional<AntivirusClient>> create(Vertx vertx){
		final Promise<Optional<AntivirusClient>> promise = Promise.promise();
		vertx.sharedData().<String, String>getAsyncMap("server")
			.compose(serverMap -> serverMap.get("file-system"))
			.onSuccess(s ->
				promise.complete(create(vertx, Utils.isNotEmpty(s) ?  new JsonObject(s) : new JsonObject()))
			).onFailure(promise::fail);
        return promise.future();
	}

	static Optional<AntivirusClient> create(Vertx vertx, JsonObject fs){
		try{
			if (fs != null && !fs.isEmpty()) {
				final JsonObject antivirus = fs.getJsonObject("antivirus");
				if (antivirus != null) {
					final String h = antivirus.getString("host");
					final String c = antivirus.getString("credential");
					if (isNotEmpty(h) && isNotEmpty(c)) {
						final AntivirusClient av = new HttpAntivirusClient(vertx, h, c);
						return Optional.ofNullable(av);
					}
				}
			}
		} catch(Exception e){
			LoggerFactory.getLogger(AntivirusClient.class).warn("Could not create antivirus client: ", e);
		}
		return Optional.empty();
	}
}
