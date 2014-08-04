/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.feeder.export.eliot;

import org.entcore.feeder.export.Exporter;
import org.entcore.feeder.utils.ResultMessage;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.io.File;

public class EliotExporter implements Exporter {

	private final String exportBasePath;
	private final Vertx vertx;

	public EliotExporter(String exportPath, Vertx vertx) {
		this.exportBasePath = exportPath;
		this.vertx = vertx;
	}

	@Override
	public void export(final Handler<Message<JsonObject>> handler) throws Exception {
		final String path = exportBasePath + File.separator + System.currentTimeMillis();
		vertx.fileSystem().mkdir(path, true, new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> ar) {
				if (ar.succeeded()) {
					new EleveExportProcessing(path).start(new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> message) {
							if ("ok".equals(message.body().getString("status"))) {
								message.body().putString("exportPath", path);
							}
							handler.handle(message);
						}
					});
				} else {
					handler.handle(new ResultMessage().error(ar.cause().getMessage()));
				}
			}
		});
	}

}
