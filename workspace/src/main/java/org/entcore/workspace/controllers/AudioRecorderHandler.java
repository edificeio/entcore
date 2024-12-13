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

import org.entcore.common.events.EventHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.workspace.Workspace;
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AudioRecorderHandler implements Handler<ServerWebSocket> {
	static final String RESOURCE_NAME = "sound";
	private static final Logger log = LoggerFactory.getLogger(AudioRecorderHandler.class);
	private static final long TIMEOUT = 5000l;
	private final Vertx vertx;
	private final EventBus eb;
	private final EventHelper eventHelper;

	public AudioRecorderHandler(Vertx vertx) {
		this.vertx = vertx;
		this.eb = Server.getEventBus(vertx);
		final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Workspace.class.getSimpleName());
		this.eventHelper = new EventHelper(eventStore);
	}


	private Map<String, String> parseQueries(final String query) {
		final Map<String,String> queries = new HashMap<>();
		if(StringUtils.isEmpty(query)){
			return queries;
		}
		final String[] parts = query.split("&");
		for(final String p : parts){
			final String[] s = p.split("=");
			queries.put(decode(s[0]), decode(s[1]));
		}
		return queries;
	}

	private static String decode(final String encoded) {
		try {
			return encoded == null ? null : URLDecoder.decode(encoded, "UTF-8");
		} catch(final UnsupportedEncodingException e) {
			throw new RuntimeException("Impossible: UTF-8 is a required encoding", e);
		}
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
				final Map<String, String> queries = parseQueries(ws.query());
				final String path = ws.path();
				final String id = path.replaceFirst("/audio/", "");
				log.info("[Dictaphone] - Pausing n°: " + id+" / "+queries.getOrDefault("sampleRate", "44100"));
				eb.request(AudioRecorderWorker.class.getSimpleName(),
						new JsonObject().put("action", "open").put("id", id).put("sampleRate", queries.getOrDefault("sampleRate", "44100")), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> m) {
						if ("ok".equals(m.body().getString("status"))) {
							ws.frameHandler(new Handler<WebSocketFrame>() {
								@Override
								public void handle(WebSocketFrame frame) {
									if (frame.isBinary()) {
										log.debug("frame handler");
										eb.request(AudioRecorderWorker.class.getSimpleName() + id,
												frame.binaryData().getBytes(),
												new DeliveryOptions().setSendTimeout(TIMEOUT),
												new Handler<AsyncResult<Message<JsonObject>>>() {
													@Override
													public void handle(AsyncResult<Message<JsonObject>> ar) {
														if (ar.failed() || !"ok".equals(ar.result().body().getString("status"))) {
															ws.writeTextMessage("audio.chunk.error");
															log.error("[Dictaphone] - Error: " + ar.result().body().getString("message"));
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
							log.info("[Dictaphone] - Resuming n°: " + id);
						} else {
							ws.writeTextMessage(m.body().getString("message"));
							log.error("[Dictaphone] - Error: " + m.body().getString("message"));
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
		eb.request(AudioRecorderWorker.class.getSimpleName(), message,
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						if ("ok".equals(event.body().getString("status"))) {
							final String docId = event.body().getString("_id");
							final JsonObject jo = new JsonObject().put("status", "ok").put("docId", docId);
							ws.write(jo.toBuffer());
							eventHelper.onCreateResource(UserUtils.sessionToUserInfos(infos), RESOURCE_NAME, ws.headers());
						} else {
							ws.writeTextMessage(event.body().getString("message"));
							log.error("[Dictaphone] - Error: " + event.body().getString("message"));
						}
						ws.close();
						log.info("[Dictaphone] - Closing n°: " + id);
					}
				}));
	}

	private void cancel(String id, final ServerWebSocket ws) {
		eb.request(AudioRecorderWorker.class.getSimpleName(),
				new JsonObject().put("action", "cancel").put("id", id),
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						if (ws != null) {
							if ("ok".equals(event.body().getString("status"))) {
								ws.writeTextMessage("ok");
							} else {
								ws.writeTextMessage(event.body().getString("message"));
								log.error("[Dictaphone] - Error: " + event.body().getString("message"));
							}
							ws.close();
							log.info("[Dictaphone] - Closing n°: " + id);
						}
					}
				}));
	}

	private void disableCompression(String id, final ServerWebSocket ws) {
		eb.request(AudioRecorderWorker.class.getSimpleName(),
				new JsonObject().put("action", "rawdata").put("id", id),
				handlerToAsyncHandler(new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						if (ws != null) {
							if ("ok".equals(event.body().getString("status"))) {
								ws.writeTextMessage("ok");
							} else {
								ws.writeTextMessage(event.body().getString("message"));
								log.error("[Dictaphone] - Error: " + event.body().getString("message"));
							}
						}
					}
				}));
	}

}
