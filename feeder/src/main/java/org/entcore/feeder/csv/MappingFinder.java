/*
 * Copyright © WebServices pour l'Éducation, 2016
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

package org.entcore.feeder.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import static org.entcore.feeder.utils.CSVUtil.emptyLine;
import static org.entcore.feeder.utils.CSVUtil.getCsvReader;
import static org.entcore.feeder.utils.CSVUtil.getCsvWriter;
import static org.entcore.feeder.utils.Validator.sanitize;

public class MappingFinder {

	private static final Logger log = LoggerFactory.getLogger(MappingFinder.class);
	private final Vertx vertx;
	private static final String NOP_QUERY = "MATCH (u:User {externalId:{externalId}}) return u.externalId as externalId";

	public MappingFinder(Vertx vertx) {
		this.vertx = vertx;
	}

	public void findExternalIds(final String structureId, final String path, final String profile, final List<String> columns,
								String charset, final Handler<JsonArray> handler) {
		findExternalIds(structureId, path, profile, columns, -1, charset, handler);
	}

	public void findExternalIds(final String structureId, final String path, final String profile, final List<String> columns,
			final int eII, final String charset, final Handler<JsonArray> handler) {
		final boolean additionalColumn;
		final int externalIdIdx;
		if (eII >= 0) {
			additionalColumn = false;
			externalIdIdx = eII;
		} else {
			additionalColumn = true;
			externalIdIdx = 0;
		}
		final JsonArray errors = new JsonArray();
		String filter = "";
		if ("Student".equals(profile)) {
			filter = "AND u.birthDate = {birthDate} ";
		}
		final String query =
				"MATCH (s:Structure {externalId : {id}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
				"WHERE u.firstNameSearchField = {firstName} AND u.lastNameSearchField = {lastName} AND head(u.profiles) = {profile} " +
				filter +
				"RETURN DISTINCT u.externalId as externalId";
		final List<String[]> lines = new ArrayList<>();
		final TransactionHelper tx;
		try {
			tx = TransactionManager.getTransaction();
		} catch (TransactionException e) {
			addError(errors, "transaction.error");
			handler.handle(errors);
			return;
		}

		try {
			CSVReader csvReader = getCsvReader(path, charset);
			String[] values;
			int rowIdx = 0;
			while ((values = csvReader.readNext()) != null) {
				if (emptyLine(values)) {
					rowIdx++;
					continue;
				}
				final List<String> line = new LinkedList<>(Arrays.asList(values));
				if (additionalColumn) {
					line.add(0, "");
				}
				lines.add(line.toArray(new String[line.size()]));
				if (rowIdx == 0) {
					if (additionalColumn) {
						lines.get(0)[externalIdIdx] = "externalId";
					}
					rowIdx++;
					continue;
				}

				final JsonObject params = new JsonObject();
				if (!additionalColumn && values[externalIdIdx] != null && !values[externalIdIdx].isEmpty()) {
					tx.add(NOP_QUERY, params.putString("externalId", values[externalIdIdx]));
				} else {
					params.putString("id", structureId).putString("profile", profile);
					try {
						int i = 0;
						for (String c : columns) {
							switch (c) {
								case "lastName":
									params.putString("lastName", sanitize(values[i]));
									break;
								case "firstName":
									params.putString("firstName", sanitize(values[i]));
									break;
								case "birthDate":
									if ("Student".equals(profile)) {
										Matcher m;

										if (values[i] != null &&
												(m = CsvFeeder.frenchDatePatter.matcher(values[i])).find()) {
											params.putString("birthDate", m.group(3) + "-" + m.group(2) + "-" + m.group(1));
										} else {
											params.putString("birthDate", values[i]);
										}
									}
									break;
							}
							i++;
						}
					} catch (Exception e) {
						errors.add(new JsonObject().putString("key", "parse.line.error").putArray("params",
								new JsonArray().addString(Integer.toString(rowIdx))));
					}

					tx.add(query, params);
				}
				rowIdx++;
			}
		} catch (Exception e) {
			addError(errors, "error.read.file", path);
			handler.handle(errors);
		}
		tx.commit(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray results = event.body().getArray("results");
				if ("ok".equals(event.body().getString("status")) && results != null && results.size() + 1 == lines.size()) {
					for (int i = 0; i < results.size(); i++) {
						JsonArray line = results.get(i);
						if (line.size() == 1) { // Si 0 ou plusieurs utilisateurs, on laisse la ligne d'origine
							String eId = line.<JsonObject>get(0).getString("externalId", "");
							lines.get(i + 1)[externalIdIdx] = eId;
						}
					}
					vertx.fileSystem().deleteSync(path);

					try {
						CSVWriter writer = getCsvWriter(path, charset);
						writer.writeAll(lines);
						writer.close();
					} catch (IOException e) {
						log.error("Error writing file.", e);
						addError(errors, "error.write.file", path);
					}
					handler.handle(errors);
				} else {
					addError(errors, "error.find.ids");
					handler.handle(errors);
				}
			}
		});
	}

	private String lower(String value) {
		if (value != null) {
			return value.toLowerCase();
		}
		return null;
	}

	private void addError(JsonArray errors, String s, String... params) {
		JsonObject o = new JsonObject().putString("key", s);
		errors.addObject(o);
		if (params.length > 0) {
			o.putArray("params", new JsonArray(params));
		}
	}

}
