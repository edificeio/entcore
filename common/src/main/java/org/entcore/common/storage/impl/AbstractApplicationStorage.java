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

package org.entcore.common.storage.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import org.entcore.common.storage.ApplicationStorage;
import org.entcore.common.storage.FileInfos;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.Utils.isNotEmpty;


public abstract class AbstractApplicationStorage implements ApplicationStorage {

	private static final Logger log = LoggerFactory.getLogger(ApplicationStorage.class);
	protected Vertx vertx;

	@Override
	public void handle(final Message<JsonObject> event) {
		switch (event.body().getString("action", "")) {
			case "getInfos" :
				getInfo(event.body().getString("id", ""), new Handler<AsyncResult<FileInfos>>() {
					@Override
					public void handle(AsyncResult<FileInfos> infos) {
						JsonObject response;
						if (infos.succeeded()) {
							if (infos.result() == null) return;
							response = infos.result().toJsonExcludeEmpty();
							response.put("status", "ok");
						} else {
							response = new JsonObject().put("status", "error")
									.put("message", infos.cause().getMessage());
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
						response.put("status", "error")
								.put("message", "missing.file.id");
						log.error("Missing file id");
						reply(event, response);
						return;
					}
					fi.setId(null);
					updateInfo(fileId, fi, new Handler<AsyncResult<Integer>>() {
						@Override
						public void handle(AsyncResult<Integer> updated) {
							if (updated.succeeded()) {
								response.put("count", updated.result()).put("status", "ok");
							} else {
								response.put("status", "error")
										.put("message", updated.cause().getMessage());
								log.error("Error updating file infos.", updated.cause());
							}
							reply(event, response);
						}
					});
				} catch (IOException e) {
					response.put("status", "error")
							.put("message", e.getMessage());
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
				response.put("action", replyAction);
			}
			vertx.eventBus().send(replyTo, response, handlerToAsyncHandler(this));
		} else {
			event.reply(response, handlerToAsyncHandler(this));
		}
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

}
