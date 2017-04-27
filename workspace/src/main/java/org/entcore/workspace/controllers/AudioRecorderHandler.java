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

package org.entcore.workspace.controllers;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.collections.PersistantBuffer;
import fr.wseduc.webutils.data.ZLib;
import fr.wseduc.webutils.request.CookieHelper;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.request.filter.UserAuthFilter.SESSION_ID;

import net.sf.lamejb.*;
import net.sf.lamejb.impl.std.StreamEncoderWAVImpl;
import net.sf.lamejb.std.LameConfig;
import net.sf.lamejb.std.StreamEncoder;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.user.UserUtils;
import org.entcore.workspace.service.impl.AudioRecorderWorker;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.http.WebSocketFrame;
import org.vertx.java.core.http.impl.ws.DefaultWebSocketFrame;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.jna.Platform;

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
						new JsonObject().putString("action", "open").putString("id", id), new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> m) {
						if ("ok".equals(m.body().getString("status"))) {
							ws.frameHandler(new Handler<WebSocketFrame>() {
								@Override
								public void handle(WebSocketFrame frame) {
									if (frame.isBinary()) {
										log.debug("frame handler");
										eb.sendWithTimeout(AudioRecorderWorker.class.getSimpleName() + id,
												((DefaultWebSocketFrame) frame).getBinaryData().array(), TIMEOUT,
												new AsyncResultHandler<Message<JsonObject>>() {
													@Override
													public void handle(AsyncResult<Message<JsonObject>> ar) {
														if (ar.failed() || !"ok".equals(ar.result().body().getString("status"))) {
															ws.writeTextFrame("audio.chunk.error");
														}
													}
												});
									} else {
										final String command = frame.textData();
										if (command != null && command.startsWith("save-")) {
											save(id, command.substring(5), infos, ws);
										} else if ("cancel".equals(command)) {
											cancel(id, ws);
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
							ws.writeTextFrame(m.body().getString("message"));
						}
					}
				});
			}
		});
	}

	private void save(String id, String name, JsonObject infos, final ServerWebSocket ws) {
		JsonObject message = new JsonObject().putString("action", "save")
				.putString("id", id).putObject("session", infos);
		if (isNotEmpty(name)) {
			message.putString("name", name);
		}
		eb.send(AudioRecorderWorker.class.getSimpleName(), message,
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						if ("ok".equals(event.body().getString("status"))) {
							ws.writeTextFrame("ok");
						} else {
							ws.writeTextFrame(event.body().getString("message"));
						}
						ws.close();
					}
				});
	}

	private void cancel(String id, final ServerWebSocket ws) {
		eb.send(AudioRecorderWorker.class.getSimpleName(),
				new JsonObject().putString("action", "cancel").putString("id", id),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						if (ws != null) {
							if ("ok".equals(event.body().getString("status"))) {
								ws.writeTextFrame("ok");
							} else {
								ws.writeTextFrame(event.body().getString("message"));
							}
							ws.close();
						}
					}
				});
	}

}
