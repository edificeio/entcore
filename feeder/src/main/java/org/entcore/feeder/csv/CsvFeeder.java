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

package org.entcore.feeder.csv;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;
import org.entcore.feeder.Feed;
import org.entcore.feeder.ManualFeeder;
import org.entcore.feeder.dictionary.structures.DefaultFunctions;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.dictionary.structures.Structure;
import org.entcore.feeder.utils.*;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import static org.entcore.feeder.be1d.Be1dFeeder.generateUserExternalId;
import static org.entcore.feeder.be1d.Be1dFeeder.frenchDatePatter;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.*;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.GUEST_PROFILE;

public class CsvFeeder implements Feed {

	private static final Logger log = LoggerFactory.getLogger(CsvFeeder.class);
	public static final long DEFAULT_STUDENT_SEED = 0l;
	private final ColumnsMapper columnsMapper;
	private final Vertx vertx;

	public CsvFeeder(Vertx vertx, JsonObject additionnalsMappings) {
		this.vertx = vertx;
		this.columnsMapper = new ColumnsMapper(additionnalsMappings);
	}

	@Override
	public void launch(Importer importer, Handler<Message<JsonObject>> handler) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void launch(final Importer importer, final String path, final Handler<Message<JsonObject>> handler) throws Exception {
		parse(importer, path, handler);
	}

	private void parse(final Importer importer, final String p, final Handler<Message<JsonObject>> handler) {
		vertx.fileSystem().readDir(p, new Handler<AsyncResult<String[]>>() {
			@Override
			public void handle(AsyncResult<String[]> event) {
				if (event.succeeded() && event.result().length == 1) {
					final String path = event.result()[0];
					final Structure s;
					try {
						JsonObject structure = CSVUtil.getStructure(path);
			//			final boolean isUpdate = importer.getStructure(structure.getString("externalId")) != null;
						s = importer.createOrUpdateStructure(structure);
						if (s == null) {
							log.error("Structure error with directory " + path + ".");
							handler.handle(new ResultMessage().error("structure.error"));
							return;
						}
					} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
						log.error("Structure error with directory " + path + ".");
						handler.handle(new ResultMessage().error("structure.error"));
						return;
					}
					vertx.fileSystem().readDir(path, new Handler<AsyncResult<String[]>>() {
						@Override
						public void handle(final AsyncResult<String[]> event) {
							if (event.succeeded()) {
								checkNotModifiableExternalId(event.result(), new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> m) {
										if ("ok".equals(m.body().getString("status"))) {
											launchFiles(path, event.result(), s, importer, handler);
										} else {
											handler.handle(m);
										}
									}
								});
							} else {
								handler.handle(new ResultMessage().error("error.list.files"));
							}
						}
					});
				} else {
					handler.handle(new ResultMessage().error("error.list.files"));
				}
			}
		});
	}

	private void launchFiles(final String path, final String[] files, final Structure structure,
			final Importer importer, final Handler<Message<JsonObject>> handler) {
		Arrays.sort(files, Collections.reverseOrder());
		final Set<String> parsedFiles = new HashSet<>();
		final VoidHandler[] handlers = new VoidHandler[files.length + 1];
		handlers[handlers.length -1] = new VoidHandler() {
			@Override
			protected void handle() {
				importer.restorePreDeletedUsers();
				importer.persist(handler);
			}
		};
		for (int i = files.length - 1; i >= 0; i--) {
			final int j = i;
			handlers[i] = new VoidHandler() {
				@Override
				protected void handle() {
					final String file = files[j];
					if (!parsedFiles.add(file)) {
						return;
					}
					try {
						log.info("Parsing file : " + file);
						final String profile = file.substring(path.length() + 1).replaceFirst(".csv", "");
						CSVUtil.getCharset(vertx, file, new Handler<String>(){

							@Override
							public void handle(String charset) {
								start(profile, structure, file, charset, importer, handler);
								importer.flush(new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> message) {
										if ("ok".equals(message.body().getString("status"))) {
											handlers[j + 1].handle(null);
										} else {
											importer.getReport().addErrorWithParams("file.error", file);
											handler.handle(null);
										}
									}
								});
							}
						});
					} catch (Exception e) {
						importer.getReport().addErrorWithParams("file.error", file);
						handler.handle(null);
						log.error("file.error", e);
					}
				}
			};
		}
		handlers[0].handle(null);
	}

	@Override
	public String getSource() {
		return "CSV";
	}

	private void checkNotModifiableExternalId(String[] files, final Handler<Message<JsonObject>> handler) {
		final List<String> columns = new ArrayList<>();
		final AtomicInteger externalIdIdx = new AtomicInteger(-1);
		final JsonArray externalIds = new JsonArray();
		final CSVReadProc proc = new CSVReadProc() {
			@Override
			public void procRow(int i, String... strings) {
				if (i == 0) {
					columnsMapper.getColumsNames(strings, columns, handler);
					if (columns.isEmpty()) {
						handler.handle(new ResultMessage().error("invalid.columns"));
						return;
					}
					for (int j = 0; j < columns.size(); j++) {
						if ("externalId".equals(columns.get(j))) {
							externalIdIdx.set(j);
							break;
						}
					}
				} else if (externalIdIdx.get() >= 0) {
					externalIds.add(strings[externalIdIdx.get()]);
				}
			}
		};
		final AtomicInteger count = new AtomicInteger(files.length);
		for (final String file: files) {
			CSVUtil.getCharset(vertx, file, new Handler<String>() {
				@Override
				public void handle(String charset) {
					CSV csvParser = CSV
							.ignoreLeadingWhiteSpace()
							.separator(';')
							.skipLines(0)
							.charset(charset)
							.create();
					csvParser.read(file, proc);
					if (count.decrementAndGet() == 0) {
						queryNotModifiableIds(handler, externalIds);
					}
				}
			});

		}
	}

	private void queryNotModifiableIds(final Handler<Message<JsonObject>> handler, JsonArray externalIds) {
		if (externalIds.size() > 0) {
			String query =
					"MATCH (u:User) where u.externalId IN {ids} AND u.source IN ['AAF', 'AAF1D'] " +
					"AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) " +
					"RETURN COLLECT(u.externalId) as ids";
			TransactionManager.getNeo4jHelper().execute(query, new JsonObject().putArray("ids", externalIds),
					new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						JsonArray res = event.body().getArray("result");
						JsonArray ids;
						if (res != null && res.size() > 0 && res.<JsonObject>get(0) != null &&
								(ids = res.<JsonObject>get(0).getArray("ids")) != null && ids.size() > 0) {
							handler.handle(new ResultMessage().error("unmodifiable.externalId-" + ids.encode()));
						} else {
							handler.handle(new ResultMessage());
						}
					} else {
						handler.handle(event);
					}
				}
			});
		} else {
			handler.handle(new ResultMessage());
		}
	}

	public void start(final String profile, final Structure structure, String file, String charset,
			final Importer importer, final Handler<Message<JsonObject>> handler) {
		importer.createOrUpdateProfile(STUDENT_PROFILE);
		importer.createOrUpdateProfile(RELATIVE_PROFILE);
		importer.createOrUpdateProfile(PERSONNEL_PROFILE);
		importer.createOrUpdateProfile(TEACHER_PROFILE);
		importer.createOrUpdateProfile(GUEST_PROFILE);
		DefaultFunctions.createOrUpdateFunctions(importer);

		final Validator validator = ManualFeeder.profiles.get(profile);
//		final Structure structure = importer.getStructure(structureExternalId);
//		if (structure == null) {
//			handler.handle(new ResultMessage().error("invalid.structure"));
//			return;
//		}

		CSV csvParser = CSV
				.ignoreLeadingWhiteSpace()
				.separator(';')
				.skipLines(0)
				.charset(charset)
				.create();
		final List<String> columns = new ArrayList<>();
		csvParser.read(file, new CSVReadProc() {
			@Override
			public void procRow(int i, String... strings) {
				if (i == 0) {
					columnsMapper.getColumsNames(strings, columns, handler);
				} else if (!columns.isEmpty()) {
					JsonObject user = new JsonObject();
					user.putArray("structures", new JsonArray().add(structure.getExternalId()));
					user.putArray("profiles", new JsonArray().add(profile));
					List<String[]> classes = new ArrayList<>();
					for (int j = 0; j < strings.length; j++) {
						final String c = columns.get(j);
						final String v = strings[j].trim();
						if (v.isEmpty()) continue;
						switch (validator.getType(c)) {
							case "string":
								if ("birthDate".equals(c)) {
									Matcher m = frenchDatePatter.matcher(v);
									if (m.find()) {
										user.putString(c, m.group(3) + "-" + m.group(2) + "-" + m.group(1));
									} else {
										user.putString(c, v);
									}
								} else {
									user.putString(c, v);
								}
								break;
							case "array-string":
								JsonArray a = user.getArray(c);
								if (a == null) {
									a = new JsonArray();
									user.putArray(c, a);
								}
								if (("classes".equals(c) || "subjectTaught".equals(c) || "functions".equals(c)) &&
										!v.startsWith(structure.getExternalId() + "$")) {
									a.add(structure.getExternalId() + "$" + v);
								} else {
									a.add(v);
								}
								break;
							case "boolean":
								user.putBoolean(c, "true".equals(v.toLowerCase()));
								break;
							default:
								Object o = user.getValue(c);
								final String v2;
								if ("childClasses".equals(c) && !v.startsWith(structure.getExternalId() + "$")) {
									v2 = structure.getExternalId() + "$" + v;
								} else {
									v2 = v;
								}
								if (o != null) {
									if (o instanceof JsonArray) {
										((JsonArray) o).add(v2);
									} else {
										JsonArray array = new JsonArray();
										array.add(o).add(v2);
										user.putArray(c, array);
									}
								} else {
									user.putString(c, v2);
								}
						}
						if ("classes".equals(c)) {
							String [] cc = v.split("\\$");
							if (cc.length == 2 && !cc[1].matches("[0-9]+")) {
								final String fosEId = importer.getFieldOfStudy().get(cc[1]);
								if (fosEId != null) {
									cc[1] = fosEId;
								}
							}
							String eId = structure.getExternalId() + '$' + cc[0];
							structure.createClassIfAbsent(eId, v);
							final String[] classId = new String[3];
							classId[0] = structure.getExternalId();
							classId[1] = eId;
							classId[2] = (cc.length == 2) ? cc[1] : "";
							classes.add(classId);
						}
					}
					String ca;
					long seed;
					JsonArray classesA;
					Object co = user.getValue("classes");
					if (co != null && co instanceof JsonArray) {
						classesA = (JsonArray) co;
					} else if (co instanceof String) {
						classesA = new JsonArray().add(co);
					} else {
						classesA = null;
					}
					if ("Student".equals(profile) && classesA != null && classesA.size() == 1) {
						seed = DEFAULT_STUDENT_SEED;
						ca = classesA.get(0);
					} else {
						ca = String.valueOf(i);
						seed = System.currentTimeMillis();
					}
					generateUserExternalId(user, ca, structure, seed);
					switch (profile) {
						case "Teacher":
							importer.createOrUpdatePersonnel(user, TEACHER_PROFILE_EXTERNAL_ID,
									user.getArray("structures"), classes.toArray(new String[classes.size()][2]),
									null, true, true);
							break;
						case "Personnel":
							importer.createOrUpdatePersonnel(user, PERSONNEL_PROFILE_EXTERNAL_ID,
									user.getArray("structures"), classes.toArray(new String[classes.size()][2]),
									null, true, true);
							break;
						case "Student":
							importer.createOrUpdateStudent(user, STUDENT_PROFILE_EXTERNAL_ID, null, null,
									classes.toArray(new String[classes.size()][2]), null, null, true, true);
							break;
						case "Relative":
							JsonArray linkStudents = new JsonArray();
							for (String attr : user.getFieldNames()) {
								if ("childExternalId".equals(attr)) {
									Object o = user.getValue(attr);
									if (o instanceof JsonArray) {
										for (Object c : (JsonArray) o) {
											linkStudents.add(c);
										}
									} else {
										linkStudents.add(o);
									}
								} else if ("childLastName".equals(attr)) {
									Object childLastName = user.getValue(attr);
									Object childFirstName = user.getValue("childFirstName");
									Object childClasses = user.getValue("childClasses");
									if (childLastName instanceof JsonArray && childFirstName instanceof JsonArray &&
											childClasses instanceof JsonArray &&
											((JsonArray) childClasses).size() == ((JsonArray) childLastName).size() &&
											((JsonArray) childFirstName).size() == ((JsonArray) childLastName).size()) {
										for (int j = 0; j < ((JsonArray) childLastName).size(); j++) {
											String mapping = structure.getExternalId() +
													((JsonArray) childLastName).<String>get(i).trim() +
													((JsonArray) childFirstName).<String>get(i).trim() +
													((JsonArray) childClasses).<String>get(i).trim() + DEFAULT_STUDENT_SEED;
											relativeStudentMapping(linkStudents, mapping);
										}
									} else if (childLastName instanceof String && childFirstName instanceof String &&
											childClasses instanceof String) {
										if (childLastName != null && childFirstName != null && childClasses != null) {
											String mapping = structure.getExternalId() +
													childLastName.toString().trim() +
													childFirstName.toString().trim() +
													childClasses.toString().trim() + DEFAULT_STUDENT_SEED;
											relativeStudentMapping(linkStudents, mapping);
										}
									} else {
										handler.handle(new ResultMessage().error("invalid.child.mapping"));
										return;
									}
								}
							}
							importer.createOrUpdateUser(user, linkStudents);
							break;
						case "Guest":
							importer.createOrUpdateGuest(user, classes.toArray(new String[classes.size()][2]));
							break;
					}
				}
			}

			private void relativeStudentMapping(JsonArray linkStudents, String mapping) {
				if (mapping.trim().isEmpty()) return;
				try {
					linkStudents.add(Hash.sha1(mapping.getBytes("UTF-8")));
				} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
					log.error(e.getMessage(), e);
				}
			}

		});
		switch (profile) {
			case "Teacher":
				importer.getPersEducNat().createAndLinkSubjects(structure.getExternalId());
				break;
			case "Relative":
				importer.linkRelativeToClass(RELATIVE_PROFILE_EXTERNAL_ID);
				importer.linkRelativeToStructure(RELATIVE_PROFILE_EXTERNAL_ID);
				importer.addRelativeProperties(getSource());
				break;
		}
//		importer.markMissingUsers(structure.getExternalId(), new Handler<Void>() {
//			@Override
//			public void handle(Void event) {
//				importer.persist(handler);
//			}
//		});
	//	importer.persist(handler);
	}

}
