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

package org.entcore.infra.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.infra.services.AntivirusService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public abstract class AbstractAntivirusService implements AntivirusService, Handler<Message<JsonObject>> {

	protected static final Logger log = LoggerFactory.getLogger(AntivirusService.class);
	protected Vertx vertx;
	private Map<String, InfectedFile> queue;
	private Renders render;
	private Storage storage;
	private TimelineHelper timeline;

	public void init() {
		this.queue = new HashMap<>();
		this.storage = new StorageFactory(vertx).getStorage();
		vertx.eventBus().localConsumer("antivirus", this);
	}

	protected abstract void parseScanReport(String path, Handler<AsyncResult<List<InfectedFile>>> handler);


	@Override
	public void replaceInfectedFiles(String path, final Handler<Either<String, JsonObject>> handler) {
		parseScanReport(path, new Handler<AsyncResult<List<InfectedFile>>>() {
			@Override
			public void handle(AsyncResult<List<InfectedFile>> event) {
				if (event.succeeded()) {
					final JsonObject j = launchReplace(event.result());
					handler.handle(new Either.Right<String, JsonObject>(j));
				} else {
					log.error("Error parsing scan report.", event.cause());
					handler.handle(new Either.Left<String, JsonObject>(event.cause().getMessage()));
				}
			}
		});
	}

	protected JsonObject launchReplace(List<InfectedFile> infectedFiles) {
		final JsonObject j = new JsonObject();
		for (final InfectedFile i : infectedFiles) {
			final JsonObject message = new JsonObject()
					.put("action", "getInfos")
					.put("id", i.getId())
					.put("replyTo", "antivirus")
					.put("replyAction", "rmInfected");
			final long timerId = vertx.setTimer(30000l, new Handler<Long>() {
				@Override
				public void handle(Long event) {
					removeInfectedFile(i, null);
				}
			});
			i.setTimerId(timerId);
			queue.put(i.getId(), i);
			vertx.eventBus().publish("storage", message);
			j.put(i.getPath(), i.getVirus());
		}
		return j;
	}

	@Override
	public void handle(Message<JsonObject> event) {
		switch (event.body().getString("action", "")) {
			case "rmInfected" :
				InfectedFile i = queue.remove(event.body().getString("id", ""));
				if (i != null) {
					vertx.cancelTimer(i.getTimerId());
					i.setApplication(event.body().getString("application"));
					i.setName(event.body().getString("name"));
					i.setOwner(event.body().getString("owner"));
					removeInfectedFile(i, event);
				}
				break;
		}
	}

	private void removeInfectedFile(final InfectedFile i, final Message<JsonObject> message) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			final JsonObject params = new JsonObject(mapper.writeValueAsString(i));
			log.info("Remove infected file : " + params.encode());
			final HttpServerRequest request = new JsonHttpServerRequest(new JsonObject());
			render.processTemplate(request, "text/infectedFile.txt", params, new Handler<String>() {
				@Override
				public void handle(String content) {
					storage.writeBuffer(i.getPath(), i.getId(), Buffer.buffer(content), "text/plain", i.getName() + ".txt", new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject event) {
							if (timeline != null && i.getOwner() != null) {
								final List<String> recipients = new ArrayList<>();
								recipients.add(i.getOwner());
								timeline.notifyTimeline(request,
										"workspace.delete-virus", null, recipients, null, params);
							}
							if (message != null) {
								JsonObject m = new JsonObject()
										.put("id", i.getId())
										.put("name", i.getName() + ".txt")
										.put("contentType", "text/plain")
										.put("action", "updateInfos");
								message.reply(m, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> r) {
										if ("ok".equals(r.body().getString("status")) && r.body().getInteger("count", -1) > 0) {
											log.info("File info " + i.getId() + " updated.");
										} else {
											log.error("Error updating file info " + i.getId());
										}
									}
								}));
							}
						}
					});
				}
			});
		} catch (IOException | DecodeException e) {
			log.error("Error serializing infected file : " + i.getId(), e);
		}
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	public void setTimeline(TimelineHelper timeline) {
		this.timeline = timeline;
	}

	public void setRender(Renders render) {
		this.render = render;
	}

}
