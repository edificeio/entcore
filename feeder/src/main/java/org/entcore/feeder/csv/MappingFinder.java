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

package org.entcore.feeder.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
		final JsonArray errors = new fr.wseduc.webutils.collections.JsonArray();
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
			final int nbColumns = columns.size();
			String[] values;
			int rowIdx = 0;
			while ((values = csvReader.readNext()) != null) {
				if (emptyLine(values)) {
					continue;
				}
				if (values.length > nbColumns) {
					values = Arrays.asList(values).subList(0, nbColumns).toArray(new String[nbColumns]);
				} else if (values.length < nbColumns) {
					values = Arrays.copyOf(values, nbColumns);
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
					tx.add(NOP_QUERY, params.put("externalId", values[externalIdIdx]));
				} else {
					params.put("id", structureId).put("profile", profile);
					try {
						int i = 0;
						for (String c : columns) {
						//	if (i >=  values.length) break;
							switch (c) {
								case "lastName":
									params.put("lastName", sanitize(values[i]));
									break;
								case "firstName":
									params.put("firstName", sanitize(values[i]));
									break;
								case "birthDate":
									if ("Student".equals(profile)) {
										Matcher m;

										if (values[i] != null &&
												(m = CsvFeeder.frenchDatePatter.matcher(values[i])).find()) {
											params.put("birthDate", m.group(3) + "-" + m.group(2) + "-" + m.group(1));
										} else {
											params.put("birthDate", values[i]);
										}
									}
									break;
							}
							i++;
						}
					} catch (Exception e) {
						errors.add(new JsonObject().put("key", "parse.line.error").put("params",
								new fr.wseduc.webutils.collections.JsonArray().add(Integer.toString(rowIdx))));
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
				JsonArray results = event.body().getJsonArray("results");
				if ("ok".equals(event.body().getString("status")) && results != null && results.size() + 1 == lines.size()) {
					for (int i = 0; i < results.size(); i++) {
						JsonArray line = results.getJsonArray(i);
						if (line.size() == 1) { // Si 0 ou plusieurs utilisateurs, on laisse la ligne d'origine
							String eId = line.getJsonObject(0).getString("externalId", "");
							lines.get(i + 1)[externalIdIdx] = eId;
						}
					}
					vertx.fileSystem().deleteBlocking(path);

					try {
						CSVWriter writer = getCsvWriter(path, charset);
						writer.writeAll(lines);
						writer.close();
					} catch (IOException e) {
						log.error("Error writing file.", e);
						addError(errors, "error.write.file", path);
					}
					if ("Relative".equals(profile) && columns.contains("childLastName") && !columns.contains("childExternalId")) {
						if (additionalColumn) {
							columns.add(0, "externalId");
						}
						findChildExternalIds(structureId, path, charset, columns, errors, handler);
					} else {
						handler.handle(errors);
					}
				} else {
					addError(errors, "error.find.ids");
					handler.handle(errors);
				}
			}
		});
	}

	private void findChildExternalIds(final String structureId, final String path, final String charset, final List<String> columns,
			final JsonArray errors, final Handler<JsonArray> handler) {
		final List<String[]> lines = new ArrayList<>();

		final JsonArray childLastNameIndex = new fr.wseduc.webutils.collections.JsonArray();
		final JsonArray childUsernameIndex = new fr.wseduc.webutils.collections.JsonArray();
		int idx = 0;
		for (String c : columns) {
			if ("childLastName".equals(c)) {
				childLastNameIndex.add(idx);
			} else if ("childUsername".equals(c)) {
				childUsernameIndex.add(idx);
			}
			idx++;
		}
		if (childLastNameIndex.size() == 0) {
			addError(errors, "missing.childLastName");
			handler.handle(errors);
			return;
		} else if (childUsernameIndex.size() != 0 && childLastNameIndex.size() != childUsernameIndex.size()) {
			addError(errors, "mismatch.childLastName.childUsername");
			handler.handle(errors);
			return;
		}

		final int maxNbChild = childLastNameIndex.size();
		final int appendIdx;
		if (childUsernameIndex.size() > 0) {
			appendIdx = childLastNameIndex.getInteger(0) > childUsernameIndex.getInteger(0) ?
					childUsernameIndex.getInteger(0) : childLastNameIndex.getInteger(0);
		} else {
			appendIdx =  childLastNameIndex.getInteger(0);
		}
		final String query =
				"MATCH (s:Structure {externalId : {id}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
				"WHERE u.firstNameSearchField = {firstName} AND u.lastNameSearchField = {lastName} AND head(u.profiles) = 'Student' " +
				"RETURN DISTINCT u.externalId as externalId, {rowIdx} as line, {itemIdx} as item ";
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
					continue;
				}

				final List<String> line = new LinkedList<>(Arrays.asList(values));
				for (int i = 0; i < maxNbChild; i++) {
					if (rowIdx == 0) {
						line.add(appendIdx, "childExternalId");
					} else {
						line.add(appendIdx, "");
					}
				}
				lines.add(line.toArray(new String[line.size()]));
				if (rowIdx == 0) {
					rowIdx++;
					continue;
				}

				final JsonArray firstNames = new fr.wseduc.webutils.collections.JsonArray();
				final JsonArray lastNames = new fr.wseduc.webutils.collections.JsonArray();
				try {
					int i = 0;
					for (String c : columns) {
						if (i >=  values.length) break;
						switch (c) {
							case "childLastName":
								lastNames.add(sanitize(values[i]));
								break;
							case "childFirstName":
								firstNames.add(sanitize(values[i]));
								break;
						}
						i++;
					}
				} catch (Exception e) {
					errors.add(new JsonObject().put("key", "parse.line.error").put("params",
							new fr.wseduc.webutils.collections.JsonArray().add(Integer.toString(rowIdx))));
				}
				final int fns = firstNames.size();
				if (fns != lastNames.size()) {
					errors.add(new JsonObject().put("key", "child.lastName.firstName.mismatch").put("params",
							new fr.wseduc.webutils.collections.JsonArray().add(Integer.toString(rowIdx))));
				} else if (fns > 0) {
//					if (fns > maxNbChild) {
//						maxNbChild = fns;
//					}
					for (int i = 0; i < fns; i++) {
						JsonObject params = new JsonObject()
								.put("id", structureId)
								.put("firstName", firstNames.getString(i))
								.put("lastName", lastNames.getString(i))
								.put("rowIdx", rowIdx)
								.put("itemIdx", i);
						tx.add(query, params);
					}
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
				JsonArray results = event.body().getJsonArray("results");
				if ("ok".equals(event.body().getString("status")) && results != null) {
					for (int i = 0; i < results.size(); i++) {
						JsonArray item = results.getJsonArray(i);
						if (item.size() == 1) { // Si 0 ou plusieurs utilisateurs, on laisse la ligne d'origine
							String eId = item.getJsonObject(0).getString("externalId", "");
							int lineIdx =  item.getJsonObject(0).getInteger("line", -1);
							int itemIdx =  item.getJsonObject(0).getInteger("item", -1);
							if (lineIdx > 0 && itemIdx >= 0) {
								String [] line = lines.get(lineIdx);
								line[itemIdx + appendIdx] = eId;
								line[childLastNameIndex.getInteger(itemIdx) + maxNbChild] = "";
								if (childUsernameIndex.size() > 0) {
									line[childUsernameIndex.getInteger(itemIdx) + maxNbChild] = "";
								}
							}
						}
					}
					vertx.fileSystem().deleteBlocking(path);

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

	private void addError(JsonArray errors, String s, String... params) {
		JsonObject o = new JsonObject().put("key", s);
		errors.add(o);
		if (params.length > 0) {
			o.put("params", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(params)));
		}
	}

}
