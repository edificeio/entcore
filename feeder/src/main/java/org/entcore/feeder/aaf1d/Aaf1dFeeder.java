/*
 * Copyright © WebServices pour l'Éducation, 2015
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

package org.entcore.feeder.aaf1d;

import org.entcore.feeder.Feed;
import org.entcore.feeder.aaf.AafFeeder;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.utils.ResultMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Aaf1dFeeder implements Feed {

	private static final Logger log = LoggerFactory.getLogger(Aaf1dFeeder.class);
	private final Vertx vertx;
	private final String path;

	public Aaf1dFeeder(Vertx vertx, String path) {
		this.vertx = vertx;
		if (path.endsWith(File.separator)) {
			this.path = path.substring(0, path.length() - 1);
		} else {
			this.path = path;
		}
	}

	@Override
	public void launch(Importer importer, final Handler<Message<JsonObject>> handler) throws Exception {
		vertx.fileSystem().readFile(path + File.separator + AafFeeder.IMPORT_DIRECTORIES_JSON,
				new Handler<AsyncResult<Buffer>>() {
			@Override
			public void handle(AsyncResult<Buffer> f) {
				if (f.succeeded()) {
					final JsonArray importSubDirectories;
					try {
						importSubDirectories = new JsonArray(f.result().toString());
					} catch (RuntimeException e) {
						handler.handle(new ResultMessage().error("invalid.importDirectories.file"));
						log.error("Invalid importDirectories file.", e);
						return;
					}
					vertx.fileSystem().readDir(path, new Handler<AsyncResult<List<String>>>() {
						@Override
						public void handle(AsyncResult<List<String>> event) {
							if (event.succeeded()) {
								final List<String> importsDirs = new ArrayList<>();
								for (String dir : event.result()) {
									final int idx = dir.lastIndexOf(File.separator);
									if (idx >= 0 && dir.length() > idx && importSubDirectories.contains(dir.substring(idx + 1))) {
										importsDirs.add(dir);
									}
								}
								if (importsDirs.size() < 1) {
									handler.handle(new ResultMessage().error("missing.subdirectories"));
									return;
								}
								final Handler<Message<JsonObject>>[] handlers = new Handler[importsDirs.size()];
								handlers[handlers.length - 1] = new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> m) {
										handler.handle(m);
									}
								};
								for (int i = importsDirs.size() - 2; i >= 0; i--) {
									final int j = i + 1;
									handlers[i] = new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> m) {
											if (m != null && "ok".equals(m.body().getString("status"))) {
												new StructureImportProcessing1d(
														importsDirs.get(j), vertx).start(handlers[j]);
											} else {
												handler.handle(m);
											}
										}
									};
								}
								new StructureImportProcessing1d(
										importsDirs.get(0), vertx).start(handlers[0]);
							} else {
								handler.handle(new ResultMessage().error(event.cause().getMessage()));
							}
						}
					});
				} else {
					new StructureImportProcessing1d(path, vertx).start(handler);
				}
			}
		});
	}

	@Override
	public void launch(Importer importer, String path, Handler<Message<JsonObject>> handler) throws Exception {
		launch(importer, handler);
	}

	@Override
	public String getSource() {
		return "AAF1D";
	}

}
