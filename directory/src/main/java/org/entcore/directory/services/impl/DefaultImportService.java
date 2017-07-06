/*
 * Copyright © WebServices pour l'Éducation, 2016
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
 */

package org.entcore.directory.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import org.entcore.directory.Directory;
import org.entcore.directory.pojo.ImportInfos;
import org.entcore.directory.services.ImportService;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class DefaultImportService implements ImportService {

	private static final Logger log = LoggerFactory.getLogger(DefaultImportService.class);
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
					.putString("action", "validate");
			eb.send(Directory.FEEDER, action, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> res) {
					if ("ok".equals(res.body().getString("status"))) {
						JsonObject r = res.body().getObject("result", new JsonObject());
						if (r.getObject("errors", new JsonObject()).size() > 0) {
							handler.handle(new Either.Left<JsonObject, JsonObject>(r.getObject("errors")));
						} else {
							JsonObject f = r.getObject("files");
							if(r.getObject("softErrors") != null) {
								f.putObject("softErrors", r.getObject("softErrors"));
							}
							handler.handle(new Either.Right<JsonObject, JsonObject>(f));
						}
					} else {
						handler.handle(new Either.Left<JsonObject, JsonObject>(
								new JsonObject().putArray("global",
								new JsonArray().addString(res.body().getString("message", "")))));
					}
				}
			});
		} catch (JsonProcessingException e) {
			handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject()
					.putArray("global", new JsonArray().addString("unexpected.error"))));
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void doImport(ImportInfos importInfos, final Handler<Either<JsonObject, JsonObject>> handler) {
		try {
			JsonObject action = new JsonObject(mapper.writeValueAsString(importInfos))
					.putString("action", "import");
			eb.send("entcore.feeder", action, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						JsonObject r = event.body().getObject("result", new JsonObject());
						if (r.getObject("errors", new JsonObject()).size() > 0) {
							handler.handle(new Either.Left<JsonObject, JsonObject>(r.getObject("errors")));
						} else {
							handler.handle(new Either.Right<JsonObject, JsonObject>(r.getObject("ignored")));
						}
					} else {
						handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject().putArray("global",
								new JsonArray().addString(event.body().getString("message", "")))));
					}
			}
			});
		} catch (JsonProcessingException e) {
			handler.handle(new Either.Left<JsonObject, JsonObject>(new JsonObject()
					.putArray("global", new JsonArray().addString("unexpected.error"))));
			log.error(e.getMessage(), e);
		}
	}

}
