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

package org.entcore.workspace.controllers;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.request.filter.UserAuthFilter.SESSION_ID;

import org.entcore.common.user.UserUtils;
import org.entcore.workspace.service.impl.AudioRecorderWorker;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.request.CookieHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AudioRecorderHandler implements Handler<ServerWebSocket> {

	private static final Logger log = LoggerFactory.getLogger(AudioRecorderHandler.class);
	private static final long TIMEOUT = 5000l;
	private final Vertx vertx;
	private final EventBus eb;

	public AudioRecorderHandler(Vertx vertx) {
		this.vertx = vertx;
		this.eb = Server.getEventBus(vertx);
	}

	@Override
	public void handle(final ServerWebSocket ws) {
		ws.pause();
		String sessionId = CookieHelper.getInstance().getSigned(SESSION_ID, ws);
		UserUtils.getSession(Server.getEventBus(vertx), sessionId, new Handler<JsonObject>() {
			public void handle(final JsonObject infos) {
				if (infos == null) {
					ws.reject();
					return;
				}
				final String id = ws.path().replaceFirst("/audio/", "");
				eb.send(AudioRecorderWorker.class.getSimpleName(),
						new JsonObject().put("action", "open").put("id", id), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> m) {
						if ("ok".equals(m.body().getString("status"))) {
							ws.frameHandler(new Handler<WebSocketFrame>() {
								@Override
								public void handle(WebSocketFrame frame) {
									if (frame.isBinary()) {
										log.debug("frame handler");
										eb.send(AudioRecorderWorker.class.getSimpleName() + id,
												frame.binaryData().getBytes(),
												new DeliveryOptions().setSendTimeout(TIMEOUT),
												new Handler<AsyncResult<Message<JsonObject>>>() {
													@Override
													public void handle(AsyncResult<Message<JsonObject>> ar) {
														if (ar.failed() || !"ok".equals(ar.result().body().getString("status"))) {
															ws.writeTextMessage("audio.chunk.error");
														}
													}
												});
									} else {
										final String command = frame.textData();
										if (command != null && command.startsWith("save-")) {
											save(id, command.substring(5), infos, ws);
										} else if ("cancel".equals(command)) {
											cancel(id, ws);
										} else if ("rawdata".equals(command)) {
											disableCompression(id, ws);
										}
									}
								}
							});
							ws.closeHandler(new Handler<Void>() {
								@Override
								public void handle(Void event) {
									cancel(id, null);
								}
							});
							ws.resume();
						} else {
							ws.writeTextMessage(m.body().getString("message"));
						}
					}
				}));
			}
		});
	}

	private void save(String id, String name, JsonObject infos, final ServerWebSocket ws) {
		JsonObject message = new JsonObject().put("action", "save")
				.put("id", id).put("session", infos);
		if (isNotEmpty(name)) {
			message.put("name", name);
		}
		eb.send(AudioRecorderWorker.class.getSimpleName(), message,
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						if ("ok".equals(event.body().getString("status"))) {
							ws.writeTextMessage("ok");
						} else {
							ws.writeTextMessage(event.body().getString("message"));
						}
						ws.close();
					}
				}));
	}

	private void cancel(String id, final ServerWebSocket ws) {
		eb.send(AudioRecorderWorker.class.getSimpleName(),
				new JsonObject().put("action", "cancel").put("id", id),
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						if (ws != null) {
							if ("ok".equals(event.body().getString("status"))) {
								ws.writeTextMessage("ok");
							} else {
								ws.writeTextMessage(event.body().getString("message"));
							}
							ws.close();
						}
					}
				}));
	}

	private void disableCompression(String id, final ServerWebSocket ws) {
		eb.send(AudioRecorderWorker.class.getSimpleName(),
				new JsonObject().put("action", "rawdata").put("id", id),
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						if (ws != null) {
							if ("ok".equals(event.body().getString("status"))) {
								ws.writeTextMessage("ok");
							} else {
								ws.writeTextMessage(event.body().getString("message"));
							}
						}
					}
				}));
	}

}
