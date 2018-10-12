/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.feeder.aaf;

import org.entcore.feeder.Feed;
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

public class AafFeeder implements Feed {

	private static final Logger log = LoggerFactory.getLogger(AafFeeder.class);
	public static final String IMPORT_DIRECTORIES_JSON = "importDirectories.json";
	private final Vertx vertx;
	private final String path;

	public AafFeeder(Vertx vertx, String path) {
		this.vertx = vertx;
		if (path.endsWith(File.separator)) {
			this.path = path.substring(0, path.length() - 1);
		} else {
			this.path = path;
		}
	}

	@Override
	public void launch(final Importer importer, final Handler<Message<JsonObject>> handler) throws Exception {
		vertx.fileSystem().readFile(path + File.separator + IMPORT_DIRECTORIES_JSON, new Handler<AsyncResult<Buffer>>() {
			@Override
			public void handle(AsyncResult<Buffer> f) {
				if (f.succeeded()) {
					final JsonArray importSubDirectories;
					try {
						importSubDirectories = new fr.wseduc.webutils.collections.JsonArray(f.result().toString());
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
												new StructureImportProcessing(
														importsDirs.get(j), vertx).start(handlers[j]);
											} else {
												handler.handle(m);
											}
										}
									};
								}
								new StructureImportProcessing(
										importsDirs.get(0), vertx).start(handlers[0]);
							} else {
								handler.handle(new ResultMessage().error(event.cause().getMessage()));
							}
						}
					});
				} else {
					new StructureImportProcessing(path, vertx).start(handler);
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
		return "AAF";
	}

}
