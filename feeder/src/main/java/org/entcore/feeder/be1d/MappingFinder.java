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

package org.entcore.feeder.be1d;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;
import au.com.bytecode.opencsv.CSVWriteProc;
import au.com.bytecode.opencsv.CSVWriter;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.feeder.utils.CSVUtil;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.utils.TransactionManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

public class MappingFinder {

	private static final Logger log = LoggerFactory.getLogger(MappingFinder.class);
	private final Neo4j neo4j = TransactionManager.getNeo4jHelper();
	private final Vertx vertx;
	private static final Comparator<String[]> studentLineComparator = new Comparator<String[]>() {

		@Override
		public int compare(String[] line1, String[] line2) {
			return compareLine(line1, line2, 1, 3, 2);
		}

	};
	private static final Comparator<String[]> lineComparator = new Comparator<String[]>() {

		@Override
		public int compare(String[] line1, String[] line2) {
			return compareLine(line1, line2, 3, 4, 2);
		}

	};

	private static int compareLine(String[] line1, String[] line2, int lastNameIdx, int firstNameIdx, int otherNameIdx) {
		final String lastName1 = (line1[otherNameIdx] != null && !line1[otherNameIdx].isEmpty()) ? line1[otherNameIdx] : line1[lastNameIdx];
		final String lastName2 = (line2[otherNameIdx] != null && !line2[otherNameIdx].isEmpty()) ? line2[otherNameIdx] : line2[lastNameIdx];
		if ((lastName1 != null && lastName1.equalsIgnoreCase(lastName2)) || (lastName1 == null && lastName2 == null)) {
			final String firstName1 = line1[firstNameIdx];
			final String firstName2 = line2[firstNameIdx];
			if (firstName1 != null) {
				return firstName1.compareToIgnoreCase(firstName2);
			} else if (firstName2 != null) {
				return 1;
			}
			return 0;
		}
		if (lastName1 != null) {
			return lastName1.compareToIgnoreCase(lastName2);
		}
		return 1;
	}

	public MappingFinder(Vertx vertx) {
		this.vertx = vertx;
	}

	public void structureExists(String externalId, final Handler<Boolean> handler) {
		String query = "MATCH (s:Structure {externalId : {externalId}}) RETURN count(*) = 1 as exists";
		neo4j.execute(query, new JsonObject().putString("externalId", externalId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray result = event.body().getArray("result");
				handler.handle("ok".equals(event.body().getString("status")) && result != null && result.size() == 1 &&
						result.<JsonObject>get(0).getBoolean("exists", false));
			}
		});
	}

	public void generateFilesWithExternalIds(final String p, final String structureExternalId, final Handler<String> handler) {
		final AtomicInteger count = new AtomicInteger(Be1dValidator.fileNames.length);
		final AtomicBoolean error = new AtomicBoolean(false);
		for (final String filename : Be1dValidator.fileNames) {
			final String charset =  CSVUtil.getCharsetSync(p + File.separator + filename);
			final CSV csvParser = CSV
					.ignoreLeadingWhiteSpace()
					.separator(';')
							//.skipLines(1)
					.charset(charset)
					.create();
			generateFile(p, structureExternalId, csvParser, filename, error, new Handler<String>() {
				@Override
				public void handle(String event) {
					if (event != null || count.decrementAndGet() == 0) {
						if (event  != null) {
							log.error(event);
							error.set(true);
						}
						handler.handle(event);
					}
				}
			});
		}
	}

	private void generateFile(final String p, final String structureExternalId,
							  final CSV csvParser, final String filename, final AtomicBoolean error, final Handler<String> handler) {
		final List<String[]> lines = new ArrayList<>();
		final AtomicInteger count = new AtomicInteger();
		try {
			log.debug(p + File.separator + filename);
			csvParser.read(p + File.separator + filename, new CSVReadProc() {

				@Override
				public void procRow(int rowIdx, final String... values) {
					if (error.get()) return;
					final List<String> line = new LinkedList<>(Arrays.asList(values));
					if (rowIdx == 0) {
						line.add(0, "externalId");
						lines.add(line.toArray(new String[line.size()]));
						return;
					}
					JsonObject params = new JsonObject().putString("externalId", structureExternalId);
					String filter = "";
					try {
						String lastName;
						String firstName;
						if ("CSVExtraction-eleves.csv".equals(filename)) {
							filter = "AND u.birthDate = {birthDate} ";
							Matcher m;

							if (values[3] != null &&
									(m = Be1dValidator.frenchDatePatter.matcher(values[3])).find()) {
								params.putString("birthDate", m.group(3) + "-" + m.group(2) + "-" + m.group(1));
							} else {
								params.putString("birthDate", values[3]);
							}
							lastName = values[0];
							firstName = values[2];
							params.putString("profile", "Student");
						} else {
							lastName = values[2];
							firstName = values[3];
							if ("CSVExtraction-responsables.csv".equals(filename)) {
								params.putString("profile", "Relative");
							} else {
								params.putString("profile", "Teacher");
							}
						}
						params.putString("firstName", firstName.trim().toLowerCase())
								.putString("lastName", lastName.trim().toLowerCase());
//						params.putString("firstName", StringValidation.removeAccents(firstName.trim().toLowerCase()))
//								.putString("lastName", StringValidation.removeAccents(lastName.trim().toLowerCase()));
					} catch (Exception e) {
						log.error("Error parsing line " + rowIdx, e);
						line.add(0, "");
						lines.add(line.toArray(new String[line.size()]));
						handler.handle(e.getMessage());
						return;
					}
					String query =
							"MATCH (s:Structure {externalId : {externalId}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
									"WHERE lower(u.firstName) = {firstName} AND lower(u.lastName) = {lastName} " +
									"AND head(u.profiles) = {profile} " +
									//"WHERE u.firstNameSearchField = {firstName} AND u.lastNameSearchField = {lastName} " +
									filter +
									"RETURN DISTINCT u.externalId as externalId";
					count.incrementAndGet();
					neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							JsonArray result = event.body().getArray("result");
							String eId = "";
							if ("ok".equals(event.body().getString("status")) && result != null &&
									result.size() == 1) {
								eId = result.<JsonObject>get(0).getString("externalId", "");
							}
							line.add(0, eId);
							if ("CSVExtraction-responsables.csv".equals(filename)) {
								String q =
										"MATCH (s:Structure {externalId : {externalId}})" +
												"<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
												"WHERE lower(u.firstName) = {firstName} AND HEAD(u.profiles) = 'Student' AND " +
												"lower(u.lastName) = {lastName} " +
												"RETURN DISTINCT u.externalId as externalId";
								JsonObject par = new JsonObject().putString("externalId", structureExternalId);
								StatementsBuilder statementsBuilder = new StatementsBuilder();
								for (int i = 12; i < values.length; i += 4) {
									try {
										JsonObject pa = par.copy()
												.putString("className", values[i + 3].trim().toLowerCase())
												.putString("firstName", values[i + 2].trim().toLowerCase())
												.putString("lastName", values[i + 1].trim().toLowerCase());
										statementsBuilder.add(q, pa);
									} catch (ArrayIndexOutOfBoundsException e) {
										handler.handle("error.matching.studentId");
										return;
									}
								}
								neo4j.executeTransaction(statementsBuilder.build(), null, true,
										new Handler<Message<JsonObject>>() {
											@Override
											public void handle(Message<JsonObject> event) {
												JsonArray results = event.body().getArray("results");
												if ("ok".equals(event.body().getString("status")) && results != null) {
													int i = 13;
													for (Object o : results) {
														JsonArray ja = (JsonArray) o;
														if (ja.size() > 0) {
															JsonObject j = ja.get(0);
															String exId = j.getString("externalId");
															if (exId != null) {
																line.remove(i);
																line.add(i, exId);
																line.remove(i + 1);
																line.add(i + 1, "");
																line.remove(i + 2);
																line.add(i + 2, "");
																line.remove(i + 3);
																line.add(i + 3, "");
															}
														}
														i += 4;
													}
													lines.add(line.toArray(new String[line.size()]));
													if (count.decrementAndGet() == 0) {
														vertx.fileSystem().deleteSync(p + File.separator + filename);
														final String[] line0 = lines.remove(0);
														Collections.sort(lines, lineComparator);
														lines.add(0, line0);
														csvParser.write(p + File.separator + filename, new CSVWriteProc() {
															@Override
															public void process(CSVWriter out) {
																out.writeAll(lines);
																try {
																	out.flush();
																	out.close();
																} catch (IOException e) {
																	log.error("Error writing file.", e);
																}
																handler.handle(null);
															}
														});
													}
												}
											}
										});
							} else {
								lines.add(line.toArray(new String[line.size()]));
								if (count.decrementAndGet() == 0) {
									vertx.fileSystem().deleteSync(p + File.separator + filename);
									final String[] line0 = lines.remove(0);
									if ("CSVExtraction-eleves.csv".equals(filename)) {
										Collections.sort(lines, studentLineComparator);
									} else  {
										Collections.sort(lines, lineComparator);
									}
									lines.add(0, line0);
									csvParser.write(p + File.separator + filename, new CSVWriteProc() {
										@Override
										public void process(CSVWriter out) {
											out.writeAll(lines);
											try {
												out.flush();
												out.close();
											} catch (IOException e) {
												log.error("Error writing file.", e);
											}
											handler.handle(null);
										}
									});
								}
							}
						}
					});
				}
			});
		} catch (Exception e) {
			log.error("Error with file : " + filename, e);
			handler.handle(e.getMessage());
		}
	}

}
