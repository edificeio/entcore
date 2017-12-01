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
import org.entcore.feeder.utils.*;
import org.entcore.feeder.ImportValidator;
import org.entcore.feeder.dictionary.structures.Structure;
import org.entcore.feeder.utils.Joiner;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import static fr.wseduc.webutils.Utils.getOrElse;
import static org.entcore.feeder.csv.CsvFeeder.*;
import static org.entcore.feeder.utils.CSVUtil.emptyLine;
import static org.entcore.feeder.utils.CSVUtil.getCsvReader;

public class CsvValidator extends Report implements ImportValidator {

	private final Vertx vertx;
	private String structureId;
	private final ColumnsMapper columnsMapper;
	private final MappingFinder mappingFinder;
	private boolean findUsersEnabled = true;
	private final Map<String, String> classesNamesMapping = new HashMap<>();
	public static final Map<String, Validator> profiles;
	private final Map<String, String> studentExternalIdMapping = new HashMap<>();
	private final long defaultStudentSeed;

	static {
		Map<String, Validator> p = new HashMap<>();
		p.put("Personnel", new Validator("dictionary/schema/Personnel.json", true));
		p.put("Teacher", new Validator("dictionary/schema/Personnel.json", true));
		p.put("Student", new Validator("dictionary/schema/Student.json", true));
		p.put("Relative", new Validator("dictionary/schema/User.json", true));
		p.put("Guest", new Validator("dictionary/schema/User.json", true));
		profiles = Collections.unmodifiableMap(p);
	}

	public CsvValidator(Vertx vertx, String acceptLanguage, JsonObject additionnalsMappings) {
		super(acceptLanguage);
		this.columnsMapper = new ColumnsMapper(additionnalsMappings);
		this.mappingFinder = new MappingFinder(vertx);
		this.vertx = vertx;
		defaultStudentSeed = new Random().nextLong();
	}

	@Override
	public void validate(final String p, final Handler<JsonObject> handler) {
		vertx.fileSystem().readDir(p, new Handler<AsyncResult<List<String>>>() {
			@Override
			public void handle(AsyncResult<List<String>> event) {
				if (event.succeeded() && event.result().size() == 1) {
					final String path = event.result().get(0);
					String[] s = path.replaceAll("/$", "").substring(path.lastIndexOf("/") + 1).split("_")[0].split("@");
					if (s.length == 2) {
						structureId = s[1];
					}
					vertx.fileSystem().readDir(path, new Handler<AsyncResult<List<String>>>() {
						@Override
						public void handle(AsyncResult<List<String>> event) {
							final List<String> importFiles = event.result();
							Collections.sort(importFiles, Collections.reverseOrder());
							if (event.succeeded() && importFiles.size() > 0) {
								final Handler[] handlers = new Handler[importFiles.size() + 1];
								handlers[handlers.length -1] = new Handler<Void>() {
									@Override
									public void handle(Void v) {
										handler.handle(result);
									}
								};
								for (int i = importFiles.size() - 1; i >= 0; i--) {
									final int j = i;
									handlers[i] = new Handler<Void>() {
										@Override
										public void handle(Void v) {
											final String file = importFiles.get(j);
											log.info("Validating file : " + file);
											findUsersEnabled = true;
											final String profile = file.substring(path.length() + 1).replaceFirst(".csv", "");
											CSVUtil.getCharset(vertx, file, new Handler<String>(){

												@Override
												public void handle(String charset) {
													if (profiles.containsKey(profile)) {
														log.info("Charset : " + charset);
														checkFile(file, profile, charset, new Handler<JsonObject>() {
															@Override
															public void handle(JsonObject event) {
																handlers[j + 1].handle(null);
															}
														});
													} else {
														addError("unknown.profile");
														handler.handle(result);
													}
												}
											});
										}
									};
								}
								handlers[0].handle(null);
							} else {
								addError("error.list.files");
								handler.handle(result);
							}
						}
					});
				} else {
					addError("error.list.files");
					handler.handle(result);
				}
			}
		});
	}

	private void checkFile(final String path, final String profile, final String charset, final Handler<JsonObject> handler) {
		final List<String> columns = new ArrayList<>();
		final AtomicInteger filterExternalId = new AtomicInteger(-1);
		final JsonArray externalIds = new JsonArray();
		try {
			CSVReader csvParser = getCsvReader(path, charset);

			String[] strings;
			int i = 0;
			while ((strings = csvParser.readNext()) != null) {
				if (i == 0) {
					JsonArray invalidColumns = columnsMapper.getColumsNames(strings, columns);
					if (invalidColumns.size() > 0) {
						parseErrors("invalid.column", invalidColumns, profile, handler);
					} else if (columns.contains("externalId")) {
						int j = 0;
						for (String c : columns) {
							if ("externalId".equals(c)) {
								filterExternalId.set(j);
							}
							j++;
						}
					} else if (structureId != null && !structureId.trim().isEmpty()) {
						findUsersEnabled = false;
						findUsers(path, profile, columns, charset, handler);
					} else {
						validateFile(path, profile, columns, null, charset, handler);
					}
				} else if (filterExternalId.get() >= 0 && !emptyLine(strings)) {
					if (strings[filterExternalId.get()] != null && !strings[filterExternalId.get()].isEmpty()) {
						externalIds.add(strings[filterExternalId.get()]);
					} else if (findUsersEnabled) { // TODO add check to empty lines
						findUsersEnabled = false;
						final int eii = filterExternalId.get();
						filterExternalId.set(-1);
						findUsers(path, profile, columns, eii, charset, handler);
					}
				}
				i++;
			}
		} catch (Exception e) {
			addError(profile, "csv.exception");
			log.error("csv.exception", e);
			handler.handle(result);
			return;
		}
		if (filterExternalId.get() >= 0) {
			filterExternalIdExists(externalIds, new Handler<JsonArray>() {
				@Override
				public void handle(JsonArray externalIdsExists) {
					if (externalIdsExists != null) {
						validateFile(path, profile, columns, externalIdsExists, charset, handler);
					} else {
						addError(profile, "error.find.externalIds");
						handler.handle(result);
					}
				}
			});
		}
	}

	private void findUsers(final String path, final String profile, List<String> columns, String charset, final Handler<JsonObject> handler) {
		findUsers(path, profile, columns, -1, charset, handler);
	}

	private void findUsers(final String path, final String profile, List<String> columns, int eii, final String charset, final Handler<JsonObject> handler) {
		mappingFinder.findExternalIds(structureId, path, profile, columns, eii, charset, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray errors) {
				if (errors.size() > 0) {
					for (Object o: errors) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject j = (JsonObject) o;
						JsonArray p = j.getJsonArray("params");
						log.info(j.encode());
						if (p != null && p.size() > 0) {
							addErrorByFile(profile, j.getString("key"), p.encode().substring(1, p.encode().length() - 1)
									.replaceAll("\"", "").split(","));
						} else if (j.getString("key") != null) {
							addError(profile, j.getString("key"));
						} else {
							addError(profile, "mapping.unknown.error");
						}
					}
					handler.handle(result);
				} else {
					//validateFile(path, profile, columns, null, handler);
					checkFile(path, profile, charset, handler);
				}
			}
		});
	}

	private void filterExternalIdExists(JsonArray externalIds, final Handler<JsonArray> handler) {
		String query = "MATCH (u:User) where u.externalId in {externalIds} return collect(u.externalId) as ids";
		TransactionManager.getNeo4jHelper().execute(query, new JsonObject().put("externalIds", externalIds), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray result = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && result.size() == 1) {
					handler.handle(result.getJsonObject(0).getJsonArray("ids"));
				} else {
					handler.handle(null);
				}
			}
		});
	}

	private void parseErrors(String key, JsonArray invalidColumns, String profile, final Handler<JsonObject> handler) {
		for (Object o : invalidColumns) {
			addErrorByFile(profile, key, (String) o);
		}
		handler.handle(result);
	}

	private void validateFile(final String path, final String profile, final List<String> columns, final JsonArray existExternalId, final String charset, final Handler<JsonObject> handler) {
		addProfile(profile);
		final Validator validator = profiles.get(profile);
		getStructure(path.substring(0, path.lastIndexOf(File.separator)), new Handler<Structure>() {
			@Override
			public void handle(final Structure structure) {
				if (structure == null) {
					addError(profile, "invalid.structure");
					handler.handle(result);
					return;
				}
				try {
					CSVReader csvParser = getCsvReader(path, charset, 1);
					final int nbColumns = columns.size();
					String[] strings;
					int i = 1;
					csvParserWhile : while ((strings = csvParser.readNext()) != null) {
						if (emptyLine(strings)) {
							i++;
							continue;
						}
						if (strings.length > nbColumns) {
							strings = Arrays.asList(strings).subList(0, nbColumns).toArray(new String[nbColumns]);
						}
//						if (strings.length != nbColumns) {
//							addErrorByFile(profile, "bad.columns.number", "" + ++i);
//							continue;
//						}
						final JsonArray classesNames = new JsonArray();
						JsonObject user = new JsonObject();
						user.put("structures", new JsonArray().add(structure.getExternalId()));
						user.put("profiles", new JsonArray().add(profile));
						List<String[]> classes = new ArrayList<>();
						for (int j = 0; j < strings.length; j++) {
							if (j >= columns.size()) {
								addErrorByFile(profile, "out.columns", "" + i);
								return;
							}
							final String c = columns.get(j);
							final String v = strings[j].trim();
							if (v.isEmpty() && !c.startsWith("child")) continue;
							switch (validator.getType(c)) {
								case "string":
									if ("birthDate".equals(c)) {
										Matcher m = frenchDatePatter.matcher(v);
										if (m.find()) {
											user.put(c, m.group(3) + "-" + m.group(2) + "-" + m.group(1));
										} else {
											user.put(c, v);
										}
									} else {
										user.put(c, v);
									}
									break;
								case "array-string":
									JsonArray a = user.getJsonArray(c);
									if (a == null) {
										a = new JsonArray();
										user.put(c, a);
									}
									if (("classes".equals(c) || "subjectTaught".equals(c) || "functions".equals(c)) &&
											!v.startsWith(structure.getExternalId() + "$")) {
										a.add(structure.getExternalId() + "$" + v);
									} else {
										a.add(v);
									}
									break;
								case "boolean":
									user.put(c, "true".equals(v.toLowerCase()));
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
											user.put(c, array);
										}
									} else {
										user.put(c, v2);
									}
							}
							if ("classes".equals(c)) {
								String eId = structure.getExternalId() + '$' + v;
								String[] classId = new String[2];
								classId[0] = structure.getExternalId();
								classId[1] = eId;
								classes.add(classId);
								classesNames.add(v);
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
							seed = defaultStudentSeed;
							ca = classesA.getString(0);
						} else {
							ca = String.valueOf(i);
							seed = System.currentTimeMillis();
						}
						final State state;
						final String externalId = user.getString("externalId");
						if (externalId == null || externalId.trim().isEmpty()) {
							generateUserExternalId(user, ca, structure, seed);
							state = State.NEW;
						} else {
							if (existExternalId.contains(externalId)) {
								state = State.UPDATED;
								studentExternalIdMapping.put(getHashMapping(user, ca, structure, seed), externalId);
							} else {
								state = State.NEW;
							}
						}
						switch (profile) {
							case "Relative":
								JsonArray linkStudents = new JsonArray();
								if ("Intitulé".equals(strings[0]) && "Adresse Organisme".equals(strings[1])) {
									break csvParserWhile;
								}
								user.put("linkStudents", linkStudents);
								for (String attr : user.fieldNames()) {
									if ("childExternalId".equals(attr)) {
										Object o = user.getValue(attr);
										if (o instanceof JsonArray) {
											for (Object c : (JsonArray) o) {
												linkStudents.add(c);
											}
										} else {
											linkStudents.add(o);
										}
									} else if ("childUsername".equals(attr)) {
										Object childUsername = user.getValue(attr);
										Object childLastName = user.getValue("childLastName");
										Object childFirstName = user.getValue("childFirstName");
										Object childClasses = user.getValue("childClasses");
										if (childUsername instanceof JsonArray && childLastName instanceof JsonArray &&
												childFirstName instanceof JsonArray && childClasses instanceof JsonArray &&
												((JsonArray) childClasses).size() == ((JsonArray) childLastName).size() &&
												((JsonArray) childFirstName).size() == ((JsonArray) childLastName).size()) {
											for (int j = 0; j < ((JsonArray) childUsername).size(); j++) {
												String mapping = structure.getExternalId() +
														((JsonArray) childUsername).getString(j).trim() +
														((JsonArray) childLastName).getString(j).trim() +
														((JsonArray) childFirstName).getString(j).trim() +
														((JsonArray) childClasses).getString(j).trim() +
														defaultStudentSeed;
												relativeStudentMapping(linkStudents, mapping);
											}
										} else if (childUsername instanceof String && childLastName instanceof String &&
												childFirstName instanceof String && childClasses instanceof String) {
											String mapping = structure.getExternalId() +
													childLastName.toString().trim() +
													childFirstName.toString().trim() +
													childClasses.toString().trim() +
													defaultStudentSeed;
											relativeStudentMapping(linkStudents, mapping);
										} else {
											addErrorByFile(profile, "invalid.child.mapping", "" + (i+1) , "childLUsername");
											handler.handle(result);
											return;
										}
									} else if ("childLastName".equals(attr) && !user.fieldNames().contains("childUsername")) {
										Object childLastName = user.getValue(attr);
										Object childFirstName = user.getValue("childFirstName");
										Object childClasses = user.getValue("childClasses");
										if (childLastName instanceof JsonArray && childFirstName instanceof JsonArray &&
												childClasses instanceof JsonArray &&
												((JsonArray) childClasses).size() == ((JsonArray) childLastName).size() &&
												((JsonArray) childFirstName).size() == ((JsonArray) childLastName).size()) {
											for (int j = 0; j < ((JsonArray) childLastName).size(); j++) {
												String mapping = structure.getExternalId() +
														((JsonArray) childLastName).getString(j) +
														((JsonArray) childFirstName).getString(j) +
														((JsonArray) childClasses).getString(j) +
														defaultStudentSeed;
												relativeStudentMapping(linkStudents, mapping);
											}
										} else if (childLastName instanceof String && childFirstName instanceof String &&
												childClasses instanceof String) {
											String mapping = structure.getExternalId() +
													childLastName.toString().trim() +
													childFirstName.toString().trim() +
													childClasses.toString().trim() +
													defaultStudentSeed;
											relativeStudentMapping(linkStudents, mapping);
										} else {
											addErrorByFile(profile, "invalid.child.mapping", "" + (i+1) , "childLastName & childFirstName");
											handler.handle(result);
											return;
										}
									}
								}
								for (Object o : linkStudents) {
									if (!(o instanceof String)) continue;
									if (classesNamesMapping.get(o) != null) {
										classesNames.add(classesNamesMapping.get(o));
									}
								}
								if (classesNames.size() == 0) {
									addSoftErrorByFile(profile, "missing.student.soft", "" + (i+1),
											user.getString("firstName"), user.getString("lastName"));
								}
								break;
						}
						String error = validator.validate(user, acceptLanguage);
						if (error != null) {
							log.warn(error);
							addErrorByFile(profile, "validator.errorWithLine", "" + (i+1), error); // Note that 'error' is already translated
						} else {
							final String classesStr = Joiner.on(", ").join(classesNames);
							classesNamesMapping.put(user.getString("externalId"), classesStr);
							addUser(profile, user.put("state", translate(state.name()))
									.put("translatedProfile", translate(profile))
									.put("classesStr", classesStr));
						}
						i++;
					}
				} catch (Exception e) {
					addError(profile, "csv.exception");
					log.error("csv.exception", e);
				}
				handler.handle(result);
			}
		});

	}

	private void getStructure(final String path, final Handler<Structure> handler) {
		String query = "MATCH (s:Structure {externalId:{id}})" +
				"return s.id as id, s.externalId as externalId, s.UAI as UAI, s.name as name";
		TransactionManager.getNeo4jHelper().execute(query, new JsonObject().put("id", structureId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray result = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && result != null && result.size() == 1) {
					handler.handle(new Structure(result.getJsonObject(0)));
				} else {
					try {
						handler.handle(new Structure(CSVUtil.getStructure(path)));
					} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
						handler.handle(null);
					}
				}
			}
		});
	}

	private void relativeStudentMapping(JsonArray linkStudents, String mapping) {
		if (mapping.trim().isEmpty()) return;
		try {
			String hash = Hash.sha1(mapping.getBytes("UTF-8"));
			linkStudents.add(getOrElse(studentExternalIdMapping.get(hash), hash));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
	}

}
