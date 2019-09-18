/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.feeder.timetable;

import org.entcore.feeder.dictionary.structures.PostImport;
import org.entcore.feeder.timetable.edt.EDTImporter;
import org.entcore.feeder.timetable.edt.EDTUtils;
import org.entcore.feeder.timetable.udt.UDTImporter;
import org.entcore.feeder.utils.ResultMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportsLauncher implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(ImportsLauncher.class);
	private static final Pattern UAI_PATTERN = Pattern.compile(".*([0-9]{7}[A-Z]).*");
	private final Vertx vertx;
	private final String path;
	private final PostImport postImport;
	private EDTUtils edtUtils;
	private final boolean timetableUserCreation;

	public ImportsLauncher(Vertx vertx, String path, PostImport postImport, boolean timetableUserCreation) {
		this.vertx = vertx;
		this.path = path;
		this.postImport = postImport;
		this.timetableUserCreation = timetableUserCreation;
	}

	public ImportsLauncher(Vertx vertx, String path, PostImport postImport, EDTUtils edtUtils,
			boolean timetableUserCreation) {
		this(vertx, path, postImport, timetableUserCreation);
		this.edtUtils = edtUtils;
	}

	@Override
	public void handle(Long event) {
		vertx.fileSystem().readDir(path, (edtUtils != null ? ".*.xml": ".*.zip"), new Handler<AsyncResult<List<String>>>() {
			@Override
			public void handle(final AsyncResult<List<String>> event) {
				if (event.succeeded()) {
					final Handler[] handlers = new Handler[event.result().size() + 1];
					handlers[handlers.length -1] = new Handler<Void>() {
						@Override
						public void handle(Void v) {
							postImport.execute();
						}
					};
					Collections.sort(event.result());
					for (int i = event.result().size() - 1; i >= 0; i--) {
						final int j = i;
						handlers[i] = new Handler<Void>() {
							@Override
							public void handle(Void v) {
								final String file = event.result().get(j);
								log.info("Parsing file : " + file);
								Matcher matcher;
								if (file != null && (matcher = UAI_PATTERN.matcher(file)).find()) {

									ResultMessage m = new ResultMessage(new Handler<JsonObject>() {
										@Override
										public void handle(JsonObject event) {
											if (!"ok".equals(event.getString("status"))) {
												log.error("Error in import : " + file + " - " + event.getString("message"));
											}
											handlers[j + 1].handle(null);
										}
									})
											.put("path", file)
											.put("UAI", matcher.group(1))
											.put("language", "fr");
									if (edtUtils != null) {
										EDTImporter.launchImport(edtUtils, m, timetableUserCreation);
									} else {
										UDTImporter.launchImport(vertx, m, timetableUserCreation);
									}
								} else {
									log.error("UAI not found in filename : " + file);
								}
							}
						};
					}
					handlers[0].handle(null);
				} else {
					log.error("Error reading directory.");
				}
			}
		});
	}

}
