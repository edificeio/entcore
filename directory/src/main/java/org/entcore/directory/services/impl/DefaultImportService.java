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

package org.entcore.directory.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.directory.Directory;
import org.entcore.directory.pojo.ImportInfos;
import org.entcore.directory.services.ImportService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultImportService implements ImportService {

	private static final Logger log = LoggerFactory.getLogger(DefaultImportService.class);
	private static final long TIMEOUT = 10 * 60 * 1000l;
	private final EventBus eb;
	private final Vertx vertx;
	private static final ObjectMapper mapper = new ObjectMapper();

	public DefaultImportService(Vertx vertx, EventBus eb) {
		this.eb = eb;
		this.vertx = vertx;
	}

	@Override
	public void validate(ImportInfos importInfos, final Handler<Either<JsonObject, JsonObject>> handler) {
		try {
			JsonObject action = new JsonObject(mapper.writeValueAsString(importInfos))
					.put("action", "validate");
			eb.send(Directory.FEEDER, action, new DeliveryOptions().setSendTimeout(TIMEOUT), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> res) {
					if ("ok".equals(res.body().getString("status"))) {
						JsonObject r = res.body().getJsonObject("result", new JsonObject());
						if (r.getJsonObject("errors", new JsonObject()).size() > 0) {
							handler.handle(new Either.Left<JsonObject, JsonObject>(r.getJsonObject("errors")));
						} else {
							JsonObject f = r.getJsonObject("files");
							if(r.getJsonObject("softErrors") != null) {
								f.put("softErrors", r.getJsonObject("softErrors"));
							}
							handler.handle(new Either.Right<JsonObject, JsonObject>(f));
						}
					} else {
						handler.handle(new Either.Left<JsonObject, JsonObject>(
								new JsonObject().put("global",
								new fr.wseduc.webutils.collections.JsonArray().add(res.body().getString("message", "")))));
					}
				}
			}));
		} catch (JsonProcessingException e) {
			handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject()
					.put("global", new fr.wseduc.webutils.collections.JsonArray().add("unexpected.error"))));
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void doImport(ImportInfos importInfos, final Handler<Either<JsonObject, JsonObject>> handler) {
		try {
			JsonObject action = new JsonObject(mapper.writeValueAsString(importInfos))
					.put("action", "import");
			eb.send("entcore.feeder", action, new DeliveryOptions().setSendTimeout(TIMEOUT), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						JsonObject r = event.body().getJsonObject("result", new JsonObject());
						if (r.getJsonObject("errors", new JsonObject()).size() > 0) {
							handler.handle(new Either.Left<JsonObject, JsonObject>(r.getJsonObject("errors")));
						} else {
							handler.handle(new Either.Right<JsonObject, JsonObject>(r.getJsonObject("ignored")));
						}
					} else {
						handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject().put("global",
								new fr.wseduc.webutils.collections.JsonArray().add(event.body().getString("message", "")))));
					}
			}
			}));
		} catch (JsonProcessingException e) {
			handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject()
					.put("global", new fr.wseduc.webutils.collections.JsonArray().add("unexpected.error"))));
			log.error(e.getMessage(), e);
		}
	}

}
