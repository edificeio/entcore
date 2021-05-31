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

import com.opencsv.CSVReader;
import org.entcore.feeder.Feed;
import org.entcore.feeder.ManualFeeder;
import org.entcore.feeder.dictionary.structures.DefaultFunctions;
import org.entcore.feeder.dictionary.structures.DefaultProfiles;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.dictionary.structures.Structure;
import org.entcore.feeder.utils.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.Utils.getOrElse;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.*;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.GUEST_PROFILE;
import static org.entcore.feeder.utils.CSVUtil.emptyLine;
import static org.entcore.feeder.utils.CSVUtil.getCsvReader;

public class CsvFeeder implements Feed {

	public static final Pattern frenchDatePatter = Pattern.compile("^([0-9]{2})/([0-9]{2})/([0-9]{4})$");
	private static final Logger log = LoggerFactory.getLogger(CsvFeeder.class);
	private long defaultStudentSeed = 0l;
	private ProfileColumnsMapper columnsMapper;
	private final Vertx vertx;
	private final Map<String, String> studentExternalIdMapping = new HashMap<>();
	private Map<String, String> reverseRelativeExternalIds = new HashMap<>();

	public CsvFeeder(Vertx vertx) {
		this.vertx = vertx;
		this.columnsMapper = new ProfileColumnsMapper();
	}

	@Override
	public void launch(Importer importer, Handler<Message<JsonObject>> handler) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void launch(Importer importer, String path, Handler<Message<JsonObject>> handler) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void launch(final Importer importer, final String path, final JsonObject mappings,
			final Handler<Message<JsonObject>> handler) throws Exception {
		columnsMapper = new ProfileColumnsMapper(mappings);
		studentExternalIdMapping.clear();
		reverseRelativeExternalIds.clear();
		defaultStudentSeed = new Random().nextLong();
		importer.createOrUpdateProfile(STUDENT_PROFILE);
		importer.createOrUpdateProfile(RELATIVE_PROFILE);
		importer.createOrUpdateProfile(PERSONNEL_PROFILE);
		importer.createOrUpdateProfile(TEACHER_PROFILE);
		importer.createOrUpdateProfile(GUEST_PROFILE);
		DefaultFunctions.createOrUpdateFunctions(importer);
		parse(importer, path, handler);
	}

	private void parse(final Importer importer, final String p, final Handler<Message<JsonObject>> handler) {
		vertx.fileSystem().readDir(p, new Handler<AsyncResult<List<String>>>() {
			@Override
			public void handle(AsyncResult<List<String>> event) {
				if (event.succeeded() && event.result().size() == 1) {
					final String path = event.result().get(0);
					final Structure s;
					try {
						JsonObject structure = CSVUtil.getStructure(path);
						final String overrideClass = structure.getString("overrideClass");
			//			final boolean isUpdate = importer.getStructure(structure.getString("externalId")) != null;
						s = importer.createOrUpdateStructure(structure);
						if (s == null) {
							log.error("Structure error with directory " + path + ".");
							handler.handle(new ResultMessage().error("structure.error"));
							return;
						}
						if (isNotEmpty(overrideClass)) {
							s.setOverrideClass(overrideClass);
						}
					} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
						log.error("Structure error with directory " + path + ".");
						handler.handle(new ResultMessage().error("structure.error"));
						return;
					}
					vertx.fileSystem().readDir(path, new Handler<AsyncResult<List<String>>>() {
						@Override
						public void handle(final AsyncResult<List<String>> event) {
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

	private void launchFiles(final String path, final List<String> files, final Structure structure,
			final Importer importer, final Handler<Message<JsonObject>> handler) {
		if (importer.getReport() != null && importer.getReport().isNotReverseFilesOrder()) { // pronote case
			Collections.sort(files);
		} else {
			Collections.sort(files, Collections.reverseOrder());
		}
		final Set<String> parsedFiles = new HashSet<>();
		final Handler[] handlers = new Handler[files.size() + 1];
		handlers[handlers.length -1] = new Handler<Void>() {
			@Override
			public void handle(Void v) {
				importer.restorePreDeletedUsers();
				importer.addStructureNameInGroups(structure.getExternalId(), null);
				importer.persist(handler);
			}
		};
		for (int i = files.size() - 1; i >= 0; i--) {
			final int j = i;
			handlers[i] = new Handler<Void>() {
				@Override
				public void handle(Void v) {
					final String file = files.get(j);
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
	public String getFeederSource() {
		return "CSV";
	}

	private void checkNotModifiableExternalId(List<String> files, final Handler<Message<JsonObject>> handler) {
		final List<String> columns = new ArrayList<>();
		final AtomicInteger externalIdIdx = new AtomicInteger(-1);
		final JsonArray externalIds = new fr.wseduc.webutils.collections.JsonArray();
		final AtomicInteger count = new AtomicInteger(files.size());
		for (final String file: files) {
			CSVUtil.getCharset(vertx, file, new Handler<String>() {
				@Override
				public void handle(String charset) {
					try {
						final String profile = file.substring(file.lastIndexOf(File.separator) + 1).replaceFirst(".csv", "");
						CSVReader csvReader = getCsvReader(file, charset);
						String[] strings;
						int i = 0;
						while ((strings = csvReader.readNext()) != null) {
							if (i == 0) {
								columnsMapper.getColumsNames(profile, strings, columns, handler);
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
							} else if (externalIdIdx.get() >= 0 && !emptyLine(strings)) {
								externalIds.add(strings[externalIdIdx.get()]);
							}
							i++;
						}
						if (count.decrementAndGet() == 0) {
							queryNotModifiableIds(handler, externalIds);
						}
					} catch (Exception e) {
						handler.handle(new ResultMessage().error("csv.exception"));
						log.error("csv.exception", e);
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
			TransactionManager.getNeo4jHelper().execute(query, new JsonObject().put("ids", externalIds),
					new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						JsonArray res = event.body().getJsonArray("result");
						JsonArray ids;
						if (res != null && res.size() > 0 && res.getJsonObject(0) != null &&
								(ids = res.getJsonObject(0).getJsonArray("ids")) != null && ids.size() > 0) {
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
		importer.getReport().addProfile(profile);
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

		try {
			CSVReader csvParser = getCsvReader(file, charset);
			final List<String> columns = new ArrayList<>();
			int nbColumns = 0;
			String[] strings;
			int i = 0;
			csvParserWhile : while ((strings = csvParser.readNext()) != null) {
				if (i == 0) {
					columnsMapper.getColumsNames(profile, strings, columns, handler);
					nbColumns = columns.size();
				} else if (!columns.isEmpty()) {
					if (emptyLine(strings)) {
						i++;
						continue;
					}
					if (strings.length > nbColumns) {
						strings = Arrays.asList(strings).subList(0, nbColumns).toArray(new String[nbColumns]);
					}
					JsonObject user = new JsonObject();
					user.put("structures", new fr.wseduc.webutils.collections.JsonArray().add(structure.getExternalId()));
					user.put("profiles", new fr.wseduc.webutils.collections.JsonArray().add(profile));
					List<String[]> classes = new ArrayList<>();
					List<String[]> groups = new ArrayList<>();

					// Class Admin
					if (isNotEmpty(structure.getOverrideClass())) {
						String eId = structure.getOverrideClass(); // structure.getExternalId() + '$' + structure.getOverrideClass();
						//structure.createClassIfAbsent(eId, structure.getOverrideClass());
						final String[] classId = new String[3];
						classId[0] = structure.getExternalId();
						classId[1] = eId;
						classId[2] = "";
						classes.add(classId);
					}

					for (int j = 0; j < strings.length; j++) {
						final String c = columns.get(j);
						final String v = strings[j].trim();
						if ((v.isEmpty() && !c.startsWith("child")) ||
								("classes".equals(c) && isNotEmpty(structure.getOverrideClass()))) continue;
						switch (validator.getType(c)) {
							case "login-alias":
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
									a = new fr.wseduc.webutils.collections.JsonArray();
									user.put(c, a);
								}
								if (("classes".equals(c) || "subjectTaught".equals(c) || "functions".equals(c) ||
										"groups".equals(c)) &&
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
										JsonArray array = new fr.wseduc.webutils.collections.JsonArray();
										array.add(o).add(v2);
										user.put(c, array);
									}
								} else {
									user.put(c, v2);
								}
						}
						if ("classes".equals(c) || "groups".equals(c)) {
							String[] cc = v.split("\\$");
							if (cc.length == 2 && !cc[1].matches("[0-9]+")) {
								final String fosEId = importer.getFieldOfStudy().get(cc[1]);
								if (fosEId != null) {
									cc[1] = fosEId;
								}
							}
							String eId = structure.getExternalId() + '$' + cc[0];
							final String[] classId = new String[3];
							classId[0] = structure.getExternalId();
							classId[1] = eId;
							classId[2] = (cc.length == 2) ? cc[1] : "";
							if ("classes".equals(c)) {
								structure.createClassIfAbsent(eId, cc[0]);
								classes.add(classId);
							} else {
								structure.createFunctionalGroupIfAbsent(eId, cc[0]);
								groups.add(classId);
							}
						}
					}
					String ca;
					long seed;
					JsonArray classesA;
					Object co = user.getValue("classes");
					if (co != null && co instanceof JsonArray) {
						classesA = (JsonArray) co;
					} else if (co instanceof String) {
						classesA = new fr.wseduc.webutils.collections.JsonArray().add(co);
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
					if ("Relative".equals(profile)) {
						generateRelativeUserExternalId(user, structure);
						if (importer.getReport() != null && importer.getReport().isNotReverseFilesOrder()) { // pronote case
							String hash = getRelativeHashMapping(user, structure);
							if (hash != null) {
								reverseRelativeExternalIds.put(hash, user.getString("externalId"));
							}
						}
					} else {
						generateUserExternalId(user, ca, structure, seed);
					}
					if ("Student".equals(profile)) {
						studentExternalIdMapping.put(getHashMapping(user, ca, structure, seed), user.getString("externalId"));
					}
					switch (profile) {
						case "Teacher":
							createFunctionDisciplineGroup(profile, structure, user, groups);
							importer.createOrUpdatePersonnel(user, TEACHER_PROFILE_EXTERNAL_ID,
									user.getJsonArray("structures"), classes.toArray(new String[classes.size()][2]),
									groups.toArray(new String[groups.size()][3]), true, true);
							break;
						case "Personnel":
							createFunctionDisciplineGroup(profile, structure, user, groups);
							importer.createOrUpdatePersonnel(user, PERSONNEL_PROFILE_EXTERNAL_ID,
									user.getJsonArray("structures"), classes.toArray(new String[classes.size()][2]),
									groups.toArray(new String[groups.size()][3]), true, true);
							break;
						case "Student":
							createFunctionDisciplineGroup(profile, structure, user, groups);
							JsonArray relative = user.getJsonArray("relative");
							if (relative == null && user.containsKey("r_nom") && user.containsKey("r_prenom")) {
								relative = new JsonArray();
								if (user.getValue("r_nom") instanceof JsonArray) {
									final JsonArray rNames = user.getJsonArray("r_nom");
									final JsonArray rFirstnames = user.getJsonArray("r_prenom");
									for (int k = 0; k < rNames.size(); k++) {
										final JsonObject rUser = new JsonObject()
												.put("lastName", rNames.getString(k))
												.put("firstName", rFirstnames.getString(k));
										String relativeHash = getRelativeHashMapping(rUser, structure);
										relative.add(relativeHash);
										String relativeHashOld = reverseRelativeExternalIds.get(relativeHash);
										if (relativeHashOld != null) {
											relative.add(relativeHashOld);
										}
									}
								} else if (user.getValue("r_nom") instanceof String) {
									final JsonObject rUser = new JsonObject()
											.put("lastName", user.getString("r_nom"))
											.put("firstName", user.getString("r_prenom"));
									String relativeHash = getRelativeHashMapping(rUser, structure);
									relative.add(relativeHash);
									String relativeHashOld = reverseRelativeExternalIds.get(relativeHash);
									if (relativeHashOld != null) {
										relative.add(relativeHashOld);
									}
								}
							}
							importer.createOrUpdateStudent(user, STUDENT_PROFILE_EXTERNAL_ID, null, null,
									classes.toArray(new String[classes.size()][2]),
									groups.toArray(new String[groups.size()][3]),
									relative, true, true);
							break;
						case "Relative":
							createFunctionDisciplineGroup(profile, structure, user, groups);
							if (("Intitulé".equals(strings[0]) && "Adresse Organisme".equals(strings[1])) ||
									("".equals(strings[0]) && "Intitulé".equals(strings[1]) &&
											"Adresse Organisme".equals(strings[2]))) {
								break csvParserWhile;
							}
							JsonArray linkStudents = new fr.wseduc.webutils.collections.JsonArray();
							for (String attr : user.fieldNames()) {
								if ("childExternalId".equals(attr)) {
									Object o = user.getValue(attr);
									if (o instanceof JsonArray) {
										for (Object c : (JsonArray) o) {
											if (c instanceof String && !((String) c).trim().isEmpty()) {
												linkStudents.add(c);
											}
										}
									} else if (o instanceof String && !((String) o).trim().isEmpty()) {
										linkStudents.add(o);
									}
								} else if ("childUsername".equals(attr)) {
									Object childUsername = user.getValue(attr);
									Object childLastName = user.getValue("childLastName");
									Object childFirstName = user.getValue("childFirstName");
									Object childClasses;
									if (isNotEmpty(structure.getOverrideClass())) {
										//in case of classAdmin=> OverrideClass is always a string
										//in case of ONDE import => childFirstName is always a JSONArray
										if (childFirstName instanceof JsonArray) {
											childClasses = new JsonArray();
											for (int j = 0; j < ((JsonArray) childFirstName).size(); j++) {
												((JsonArray)childClasses).add(structure.getOverrideClass());
											}
										} else {
											childClasses = structure.getOverrideClass();
										}
									} else {
										childClasses = user.getValue("childClasses");
									}
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
										handler.handle(new ResultMessage().error("invalid.child.mapping"));
										return;
									}
								} else if ("childLastName".equals(attr) && !user.fieldNames().contains("childUsername")) {
									Object childLastName = user.getValue(attr);
									Object childFirstName = user.getValue("childFirstName");
									Object childClasses;
									if (isNotEmpty(structure.getOverrideClass())) {
										//in case of classAdmin=> OverrideClass is always a string
										//in case of ONDE import => childFirstName is always a JSONArray
										if (childFirstName instanceof JsonArray) {
											childClasses = new JsonArray();
											for (int j = 0; j < ((JsonArray) childFirstName).size(); j++) {
												((JsonArray)childClasses).add(structure.getOverrideClass());
											}
										} else {
											childClasses = structure.getOverrideClass();
										}
									} else {
										childClasses = user.getValue("childClasses");
									}
									if (childLastName instanceof JsonArray && childFirstName instanceof JsonArray &&
											childClasses instanceof JsonArray &&
											((JsonArray) childClasses).size() == ((JsonArray) childLastName).size() &&
											((JsonArray) childFirstName).size() == ((JsonArray) childLastName).size()) {
										for (int j = 0; j < ((JsonArray) childLastName).size(); j++) {
											String mapping = structure.getExternalId() +
													((JsonArray) childLastName).getString(j).trim() +
													((JsonArray) childFirstName).getString(j).trim() +
													((JsonArray) childClasses).getString(j).trim() + defaultStudentSeed;
											relativeStudentMapping(linkStudents, mapping);
										}
									} else if (childLastName instanceof String && childFirstName instanceof String &&
											childClasses instanceof String) {
											String mapping = structure.getExternalId() +
													childLastName.toString().trim() +
													childFirstName.toString().trim() +
													childClasses.toString().trim() + defaultStudentSeed;
											relativeStudentMapping(linkStudents, mapping);
									} else {
										handler.handle(new ResultMessage().error("invalid.child.mapping"));
										return;
									}
								}
							}
							importer.createOrUpdateUser(user, linkStudents, true, groups.toArray(new String[groups.size()][3]));
							break;
						case "Guest":
							importer.createOrUpdateGuest(user, classes.toArray(new String[classes.size()][2]));
							break;
					}
				}
				i++;
			}

			switch (profile) {
				case "Teacher":
					importer.getPersEducNat().createAndLinkSubjects(structure.getExternalId());
					break;
				case "Relative":
					importer.linkRelativeToClass(RELATIVE_PROFILE_EXTERNAL_ID, null, structure.getExternalId());
					importer.linkRelativeToStructure(RELATIVE_PROFILE_EXTERNAL_ID, null, structure.getExternalId());
					importer.addRelativeProperties(getFeederSource());
					break;
				case "Student":
					if (importer.getReport() != null && importer.getReport().isNotReverseFilesOrder()) {
						importer.linkRelativeToClass(RELATIVE_PROFILE_EXTERNAL_ID, null, structure.getExternalId());
					}
					break;
			}
		} catch (Exception e) {
			handler.handle(new ResultMessage().error("csv.exception"));
			log.error("csv.exception", e);
		}
//		importer.markMissingUsers(structure.getExternalId(), new Handler<Void>() {
//			@Override
//			public void handle(Void event) {
//				importer.persist(handler);
//			}
//		});
	//	importer.persist(handler);
	}

	private void createFunctionDisciplineGroup(String profile, Structure structure, JsonObject user, List<String[]> groups) {
		if (user.getJsonArray("functions") != null && user.getJsonArray("functions").size() > 0) {
			for (Object o: user.getJsonArray("functions")) {
				if (!(o instanceof String)) continue;
				String groupExternalId = null;
				String name = null;

				String [] g = ((String) o).split("\\$");
				if (g.length == 5) {
					if ("ENS".equals(g[1])) {
						groupExternalId = structure.getExternalId() + "$" + g[3];
						name = g[4];
					} else if (!g[1].isEmpty() && !"-".equals(g[1])) {
						groupExternalId = structure.getExternalId() + "$" + g[1];
						name = g[2];
					}
				}

				if (groupExternalId == null) {
					groupExternalId = ((String) o).replaceFirst("\\${4}", "\\$");
				}
				if (name == null) {
					name = groupExternalId.substring(structure.getExternalId().length() + 1);
				}
				if ("Teacher".equals(profile)) {
					structure.createFunctionGroupIfAbsent(groupExternalId, name, "Discipline");
				} else {
					structure.createFunctionGroupIfAbsent(groupExternalId, name, "Func");
				}
				final String[] groupId = new String[3];
				groupId[0] = structure.getExternalId();
				groupId[1] = groupExternalId;
				groups.add(groupId);
			}
		}
	}

	private void relativeStudentMapping(JsonArray linkStudents, String mapping) {
		if (mapping.trim().isEmpty()) return;
		try {
			String hash = Hash.sha1(mapping.getBytes("UTF-8"));
			String childId = studentExternalIdMapping.get(hash);
			if (childId != null) {
				linkStudents.add(childId);
			}
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
	}

	public static void generateUserExternalId(JsonObject props, String c, Structure structure, long seed) {
		String externalId = props.getString("externalId");
		if (externalId != null && !externalId.trim().isEmpty()) {
			return;
		}
		String hash = getHashMapping(props, c, structure, seed);
		if (hash != null) {
			props.put("externalId", hash);
		}
	}

	protected static String getHashMapping(JsonObject props, String c, Structure structure, long seed) {
		String mapping = structure.getExternalId()+props.getString("surname", "")+
				props.getString("lastName", "")+props.getString("firstName", "")+
				props.getString("email","")+props.getString("title","")+
				props.getString("homePhone","")+props.getString("mobile","")+
				c+seed;
		try {
			return Hash.sha1(mapping.getBytes("UTF-8"));
		} catch (NoSuchAlgorithmException |UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public static void generateRelativeUserExternalId(JsonObject props, Structure structure) {
		String externalId = props.getString("externalId");
		if (externalId != null && !externalId.trim().isEmpty()) {
			return;
		}
		String hash = getRelativeHashMapping(props, structure);
		if (hash != null) {
			props.put("externalId", hash);
		}
	}

	protected static String getRelativeHashMapping(JsonObject props, Structure structure) {
		String mapping = structure.getExternalId()+
				props.getString("lastName", "")+props.getString("firstName", "");
		try {
			return Hash.sha1(mapping.getBytes("UTF-8"));
		} catch (NoSuchAlgorithmException |UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

}
