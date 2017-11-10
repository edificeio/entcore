/*
 * Copyright © WebServices pour l'Éducation, 2017
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

package org.entcore.common.storage.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.entcore.common.storage.ApplicationStorage;
import org.entcore.common.storage.FileInfos;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.IOException;

import static fr.wseduc.webutils.Utils.isNotEmpty;


public abstract class AbstractApplicationStorage implements ApplicationStorage {

	private static final Logger log = LoggerFactory.getLogger(ApplicationStorage.class);
	protected Vertx vertx;

	@Override
	public void handle(final Message<JsonObject> event) {
		switch (event.body().getString("action", "")) {
			case "getInfos" :
				getInfo(event.body().getString("id", ""), new AsyncResultHandler<FileInfos>() {
					@Override
					public void handle(AsyncResult<FileInfos> infos) {
						JsonObject response;
						if (infos.succeeded()) {
							if (infos.result() == null) return;
							response = infos.result().toJsonExcludeEmpty();
							response.putString("status", "ok");
						} else {
							response = new JsonObject().putString("status", "error")
									.putString("message", infos.cause().getMessage());
							log.error("Error retrieving file infos.", infos.cause());
						}
						reply(event, response);
					}
				});
				break;
			case "updateInfos":
				ObjectMapper mapper = new ObjectMapper();
				final JsonObject response = new JsonObject();
				try {
					final FileInfos fi = mapper.readValue(event.body().encode(), FileInfos.class);
					final String fileId = fi.getId();
					if (fileId == null) {
						response.putString("status", "error")
								.putString("message", "missing.file.id");
						log.error("Missing file id");
						reply(event, response);
						return;
					}
					fi.setId(null);
					updateInfo(fileId, fi, new AsyncResultHandler<Integer>() {
						@Override
						public void handle(AsyncResult<Integer> updated) {
							if (updated.succeeded()) {
								response.putNumber("count", updated.result()).putString("status", "ok");
							} else {
								response.putString("status", "error")
										.putString("message", updated.cause().getMessage());
								log.error("Error updating file infos.", updated.cause());
							}
							reply(event, response);
						}
					});
				} catch (IOException e) {
					response.putString("status", "error")
							.putString("message", e.getMessage());
					log.error("Error  deserializing file infos.", e);
					reply(event, response);
				}
				break;
		}
	}

	private void reply(Message<JsonObject> event, JsonObject response) {
		final String replyTo = event.body().getString("replyTo");
		if (isNotEmpty(replyTo)) {
			final String replyAction = event.body().getString("replyAction");
			if (isNotEmpty(replyAction)) {
				response.putString("action", replyAction);
			}
			vertx.eventBus().send(replyTo, response, this);
		} else {
			event.reply(response, this);
		}
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

}
