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
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.exceptions.TransactionException;
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
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.feeder.csv.CsvFeeder.*;
import static org.entcore.feeder.utils.CSVUtil.emptyLine;
import static org.entcore.feeder.utils.CSVUtil.getCsvReader;

public class CsvValidator extends CsvReport implements ImportValidator {

	private boolean enableRelativeStudentLinkCheck = true;

	private enum CsvValidationProcessType { VALIDATE, COLUMN_MAPPING, CLASSES_MAPPING }
	private final Vertx vertx;
	private String structureId;
	private final MappingFinder mappingFinder;
	private boolean findUsersEnabled = true;
	private final Map<String, String> classesNamesMapping = new HashMap<>();
	private final Map<String, Set<String>> profilesClassesMapping = new HashMap<>();
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

	public CsvValidator(Vertx vertx, String acceptLanguage, JsonObject importInfos) {
		super(vertx, importInfos);
		this.mappingFinder = new MappingFinder(vertx);
		this.vertx = vertx;
		defaultStudentSeed = getSeed();
	}

	public void columnsMapping(final String p, final Handler<JsonObject> handler) {
		process(p, CsvValidationProcessType.COLUMN_MAPPING, null, handler);
	}

	public void classesMapping(final String p, final Handler<JsonObject> handler) {
		process(p, CsvValidationProcessType.CLASSES_MAPPING, null, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject event) {
				if (isNotEmpty(getStructureExternalId())) {
					final String query =
							"MATCH (s:Structure {externalId:{externalId}})<-[:BELONGS]-(c:Class) " +
							"RETURN COLLECT(DISTINCT c.name) as classes";
					final JsonObject params = new JsonObject().put("externalId", getStructureExternalId());
					TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							final JsonArray r = event.body().getJsonArray("result");
							if ("ok".equals(event.body().getString("status")) && r != null && r.size() > 0 && r.getJsonObject(0) != null) {
								final JsonArray classes = r.getJsonObject(0).getJsonArray("classes");
								mapClasses(classes);
							} else {
								addError("error.database.classes");
								log.error(event.body().getString("message"));
							}
							handler.handle(result);
						}
					});
				} else {
					mapClasses(null);
					handler.handle(result);
				}
			}
		});
	}

	protected void mapClasses(JsonArray classes) {
		JsonObject classesMapping = new JsonObject();
		// 1 : Maps profile's classes against StudentClasses if they are availables
		Set<String> studentClasses = profilesClassesMapping.get("Student") != null ?
				profilesClassesMapping.get("Student") : Collections.<String>emptySet();
		for (String profile : profiles.keySet()) {
			Set<String> profileClasses = profilesClassesMapping.get(profile);
			if (profileClasses == null || profileClasses.size() == 0) continue;
			JsonObject mapping = new JsonObject();
			classesMapping.put(profile, mapping);
			for (String c: profileClasses) {
				if (!"Student".equals(profile) && studentClasses.contains(c)) {
					mapping.put(c, c);
				} else {
					mapping.put(c, "");
				}
			}
		}

		// 2 : Maps profile's classes against dbClasses
		if (classes != null && classes.size() > 0) {
			for (String profile : classesMapping.fieldNames()) {
				JsonObject profileClassesMapping = classesMapping.getJsonObject(profile);
				for (String _class : profileClassesMapping.fieldNames()) {
					if (classes.contains(_class)) {
						profileClassesMapping.put(_class, _class);
					}
				}
			}
			classesMapping.put("dbClasses", classes);
		}
		setClassesMapping(classesMapping);
	}

	@Override
	public void validate(List<String> admlStructures, final Handler<JsonObject> handler) {
		exportFiles(new Handler<AsyncResult<String>>() {
			@Override
			public void handle(AsyncResult<String> event) {
				if (event.succeeded()) {
					clearBeforeValidation();
					clearBeforeValidation();
					validate(event.result(), admlStructures, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject event) {
							updateErrors(new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> event) {
									if (!"ok".equals(event.body().getString("status"))) {
										addError(event.body().getString("message"));
									}
									handler.handle(result);
								}
							});
						}
					});
				} else {
					addError(event.cause().getMessage());
					handler.handle(result);
				}
			}
		});
	}

	@Override
	public void validate(final String p, List<String> admlStructures, final Handler<JsonObject> handler) {
		process(p, CsvValidationProcessType.VALIDATE, admlStructures, handler);
	}

	@Override
	public boolean isValid() {
		return !containsErrors();
	}

	@Override
	public void exportIfValid(final Handler<JsonObject> handler) {
		if (isValid()) {
			exportFiles(new Handler<AsyncResult<String>>() {
				@Override
				public void handle(AsyncResult<String> event) {
					if (event.failed()) {
						addError(event.cause().getMessage());
					}
					handler.handle(result);
				}
			});
		} else {
			handler.handle(result);
		}
	}

	private void process(final String p, final CsvValidationProcessType processType,
			List<String> admlStructures, final Handler<JsonObject> handler) {
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
								if (processType == CsvValidationProcessType.VALIDATE && importFiles.stream()
										.anyMatch(f -> f.endsWith("Relative"))) {
									loadStudentExternalIdMapping(structureId, h -> {
										processFiles(importFiles, handler, processType, path, admlStructures);
									});
								} else {
									processFiles(importFiles, handler, processType, path, admlStructures);
								}
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

	protected void processFiles(List<String> importFiles, Handler<JsonObject> handler, CsvValidationProcessType processType, String path, List<String> admlStructures) {
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
					log.info(processType.name() + " file : " + file);
					findUsersEnabled = true;
					final String profile = file.substring(path.length() + 1).replaceFirst(".csv", "");
					CSVUtil.getCharset(vertx, file, new Handler<String>(){

						@Override
						public void handle(String charset) {
							if (profiles.containsKey(profile)) {
								log.info("Charset : " + charset);
								Handler<JsonObject> h = new Handler<JsonObject>() {
									@Override
									public void handle(JsonObject event) {
										handlers[j + 1].handle(null);
									}
								};
								switch (processType) {
									case VALIDATE:
										checkFile(file, profile, charset, admlStructures, h);
										break;
									case COLUMN_MAPPING:
										checkColumnsMapping(file, profile, charset, h);
										break;
									case CLASSES_MAPPING:
										checkClassesMapping(file, profile, charset, h);
										break;
								}
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
	}

	private void loadStudentExternalIdMapping(String structure, Handler<Void> handler) {
		final String query =
				"MATCH (s:Structure {externalId:{externalId}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
				"WHERE head(u.profiles) = 'Student' " +
				"OPTIONAL MATCH u-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
				"RETURN u.externalId as externalId, u.surname as surname, " +
				"u.lastName as lastName, u.firstName as firstName, u.email as email, u.title as title, " +
				"u.homePhone as homePhone, u.mobile as mobile, c.name as className ";
		TransactionManager.getNeo4jHelper().execute(query, new JsonObject().put("externalId", structure), r -> {
			final JsonArray res = r.body().getJsonArray("result");
			if ("ok".equals(r.body().getString("status")) && res != null) {
				final Structure struct = new Structure(new JsonObject().put("externalId", structure));
				for (Object o: res) {
					if (!(o instanceof JsonObject)) continue;
					final JsonObject user = (JsonObject) o;
					final String externalId = user.getString("externalId");
					final String ca = user.getString("className");
					for (String attr: user.copy().fieldNames()) {
						if (user.getValue(attr) == null) {
							user.remove(attr);
						}
					}
					studentExternalIdMapping.put(getHashMapping(user, ca, struct, defaultStudentSeed), externalId);
					studentExternalIdMapping.put(externalId, externalId);
					classesNamesMapping.put(externalId, ca);
				}
			}
			handler.handle(null);
		});
	}

	private void checkClassesMapping(String path, String profile, String charset, Handler<JsonObject> handler) {
		try {
			CSVReader csvParser = getCsvReader(path, charset);

			String[] strings;
			final List<String> columns = new ArrayList<>();
			final List<Integer> classesIdx = new ArrayList<>();
			final Set<String> mapping = new HashSet<>();
			profilesClassesMapping.put(profile, mapping);
			int i = 0;
			while ((strings = csvParser.readNext()) != null) {
				if (i == 0) {
					JsonArray invalidColumns = columnsMapper.getColumsNames(profile, strings, columns);
					if (invalidColumns.size() > 0 ) {
						parseErrors("invalid.column", invalidColumns, profile, handler);
						return;
					} else if (!columns.contains("classes") && !columns.contains("childClasses")) {
						handler.handle(result);
						return;
					} else {
						int j = 0;
						for (String column : columns) {
							if ("classes".equals(column) || "childClasses".equals(column)) {
								classesIdx.add(j);
							}
							j++;
						}
					}
				} else if (!emptyLine(strings)) {
					for (Integer idx : classesIdx) {
						if (idx < strings.length && isNotEmpty(strings[idx].trim())) {
							mapping.add(strings[idx].trim());
						}
					}
				}
				i++;
			}
		} catch (Exception e) {
			addError(profile, "csv.exception");
			log.error("csv.exception", e);
		} finally {
			handler.handle(result);
		}
	}

	private void checkColumnsMapping(String path, String profile, String charset, Handler<JsonObject> handler) {
		try {
			CSVReader csvParser = getCsvReader(path, charset);

			String[] strings;
			int columnsNumber = -1;
			int i = 0;
			while ((strings = csvParser.readNext()) != null) {
				if (i == 0) {
					JsonObject mapping = columnsMapper.getColumsMapping(profile, strings);
					if (mapping != null) {
						addMapping(profile, mapping);
					} else {
						addErrorByFile(profile, "invalid.column.empty");
						break;
					}
					columnsNumber = strings.length;
//				} else if (!emptyLine(strings) && columnsNumber != strings.length) {
//					addErrorByFile(profile, "bad.columns.number", "" + (i + 1));
				}
				i++;
			}
		} catch (Exception e) {
			addError(profile, "csv.exception");
			log.error("csv.exception", e);
		} finally {
			handler.handle(result);
		}
	}

	private void checkFile(final String path, final String profile, final String charset,
			List<String> admlStructures, final Handler<JsonObject> handler) {
		final List<String> columns = new ArrayList<>();
		final AtomicInteger filterExternalId = new AtomicInteger(-1);
		final Set<String> externalIds = new HashSet<>();
		try {
			CSVReader csvParser = getCsvReader(path, charset);

			String[] strings;
			int i = 0;
			while ((strings = csvParser.readNext()) != null) {
				if (i == 0) {
					List<String> stringsHeader = new ArrayList<>(Arrays.asList(strings));
					if (stringsHeader.contains("R1_NOM")) {
						stringsHeader.add("relative");
						stringsHeader.add("relative");
						setNotReverseFilesOrder(true);
					} else if (stringsHeader.contains("L_PROVINCE")) {
						enableRelativeStudentLinkCheck = false;
					}
					addHeader(profile, new JsonArray(stringsHeader));
					JsonArray invalidColumns = columnsMapper.getColumsNames(profile, strings, columns);
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
						findUsers(path, profile, admlStructures, columns, charset, handler);
					} else {
						validateFile(path, profile, columns, null, charset, handler);
					}
				} else if (filterExternalId.get() >= 0 && !emptyLine(strings)) {
					if (strings[filterExternalId.get()] != null && !strings[filterExternalId.get()].isEmpty()) {
						if (!externalIds.add(strings[filterExternalId.get()])) {
							addErrorByFile(profile, "duplicate.externalId.in.file",
									"" + (i+1), strings[filterExternalId.get()]);
						}
					} else if (findUsersEnabled) {
						findUsersEnabled = false;
						final int eii = filterExternalId.get();
						filterExternalId.set(-1);
						findUsers(path, profile, admlStructures, columns, eii, charset, handler);
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
			filterExternalIdExists(admlStructures, profile, externalIds, new Handler<JsonArray>() {
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

	private void findUsers(final String path, final String profile, List<String> admlStructures,
			List<String> columns, String charset, final Handler<JsonObject> handler) {
		findUsers(path, profile, admlStructures, columns, -1, charset, handler);
	}

	private void findUsers(final String path, final String profile, List<String> admlStructures,
			List<String> columns, int eii, final String charset, final Handler<JsonObject> handler) {
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
					checkFile(path, profile, charset, admlStructures, handler);
				}
			}
		});
	}

	private void filterExternalIdExists(List<String> admlStructures, String profile,
			Set<String> externalIds, final Handler<JsonArray> handler) {
		final List<String> cleanExtIds = externalIds.stream()
				.map(s -> s.contains(" ") ? s.replaceAll("\\s+", "") : s)
				.collect(Collectors.toList());
		final JsonObject params = new JsonObject().put("externalIds", new JsonArray(cleanExtIds));
		try {
			final TransactionHelper tx = TransactionManager.getTransaction();
			final String query =
					"MATCH (u:User) " +
					"WHERE u.externalId in {externalIds} " +
					"RETURN COLLECT(u.externalId) as ids";
			tx.add(query, params);
			if (admlStructures != null && admlStructures.size() > 0) {
				final String q =
						"MATCH (u:User)-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
						"WHERE u.externalId in {externalIds} " +
						"RETURN COLLECT(distinct s.id) as structureIds";
				tx.add(q, params);
			}
			tx.commit(event -> {
				JsonArray results = event.body().getJsonArray("results");
				if ("ok".equals(event.body().getString("status")) && results != null) {
					if (admlStructures != null && admlStructures.size() > 0) {
						final JsonArray usedStructures = results.getJsonArray(1)
								.getJsonObject(0).getJsonArray("structureIds");
						if (admlStructures.containsAll(usedStructures.getList())) {
							handler.handle(results.getJsonArray(0)
									.getJsonObject(0).getJsonArray("ids"));
						} else {
							addError(profile, "externalId.used.in.not.adml.structure");
							handler.handle(null);
						}
					} else {
						handler.handle(results.getJsonArray(0)
								.getJsonObject(0).getJsonArray("ids"));
					}
				} else {
					handler.handle(null);
				}
			});
		} catch (TransactionException e) {
			log.error("Error opening filterExternalId transaction.", e);
			handler.handle(null);
		}
	}

	private void parseErrors(String key, JsonArray invalidColumns, String profile, final Handler<JsonObject> handler) {
		for (Object o : invalidColumns) {
			if (isEmpty((String) o)) {
				addErrorByFile(profile, "invalid.column.empty");
			} else {
				addErrorByFile(profile, key, (String) o);
			}
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
				final JsonObject checkChildExists = new JsonObject();
//				setStructureExternalIdIfAbsent(structure.getExternalId());
				try {
					final JsonObject classMapping = getClassesMapping(profile);
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
						final JsonArray classesNames = new fr.wseduc.webutils.collections.JsonArray();
						JsonObject user = new JsonObject();
						JsonArray linkStudents = new fr.wseduc.webutils.collections.JsonArray();
						user.put("structures", new fr.wseduc.webutils.collections.JsonArray().add(structure.getExternalId()));
						user.put("profiles", new fr.wseduc.webutils.collections.JsonArray().add(profile));
						List<String[]> classes = new ArrayList<>();
						for (int j = 0; j < strings.length; j++) {
							if (j >= columns.size()) {
								addErrorByFile(profile, "out.columns", "" + i);
								return;
							}
							final String c = columns.get(j);
							final String v;
							if ("classes".equals(c) && classMapping != null) {
								v = getOrElse(classMapping.getString(strings[j].trim()), strings[j].trim(), false);
							} else {
								v = strings[j].trim();
							}
							//if ((v.isEmpty() && !"childUsername".equals(c)) ||
							//		(v.isEmpty() && "childUsername".equals(c) && strings[j+1].trim().isEmpty())) continue;
							if (v.isEmpty() && !c.startsWith("child")) continue;

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
									if (("classes".equals(c) || "subjectTaught".equals(c) || "functions".equals(c) || "groups".equals(c)) &&
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
						final State state;
						String externalId = user.getString("externalId");
						if (externalId == null || externalId.trim().isEmpty()) {
							if ("Relative".equals(profile)) {
								generateRelativeUserExternalId(user, structure);
							} else {
								generateUserExternalId(user, ca, structure, seed);
							}
							state = State.NEW;
						} else {
							if (externalId.contains(" ")) {
								externalId = externalId.replaceAll("\\s+", "");
								user.put("externalId", externalId);
							}
							if (existExternalId.contains(externalId)) {
								state = State.UPDATED;
								studentExternalIdMapping.put(getHashMapping(user, ca, structure, seed), externalId);
							} else {
								state = State.NEW;
							}
						}
						switch (profile) {
							case "Student":
								JsonArray relative = null;
								if (user.containsKey("r_nom") && user.containsKey("r_prenom")) {
									relative = new JsonArray();
									if (user.getValue("r_nom") instanceof JsonArray &&
											user.getValue("r_prenom") instanceof JsonArray) {
										final JsonArray rNames = user.getJsonArray("r_nom");
										final JsonArray rFirstnames = user.getJsonArray("r_prenom");
										for (int k = 0; k < rNames.size(); k++) {
											final JsonObject rUser = new JsonObject()
													.put("lastName", rNames.getString(k))
													.put("firstName", rFirstnames.getString(k));
											final String extId = getRelativeHashMapping(rUser, structure);
											relative.add(extId);
										}
									} else if (user.getValue("r_nom") instanceof String &&
											user.getValue("r_prenom") instanceof String) {
										final JsonObject rUser = new JsonObject()
												.put("lastName", user.getString("r_nom"))
												.put("firstName", user.getString("r_prenom"));
										final String extId = getRelativeHashMapping(rUser, structure);
										relative.add(extId);
									}
								}
								if (relative != null && relative.size() > 0) {
									user.put("relative", relative);
								}
								break;
							case "Relative":
								boolean checkChildMapping = true;
								linkStudents = new fr.wseduc.webutils.collections.JsonArray();
								if (("Intitulé".equals(strings[0]) && "Adresse Organisme".equals(strings[1])) ||
										("Intitulé".equals(strings[1]) && "Adresse Organisme".equals(strings[2]))) {
									break csvParserWhile;
								}
								user.put("linkStudents", linkStudents);
								for (String attr : user.fieldNames()) {
									if ("childExternalId".equals(attr)) {
										if (checkChildMapping) {
											checkChildMapping = false;
										}
										Object o = user.getValue(attr);
										if (o instanceof JsonArray) {
											for (Object c : (JsonArray) o) {
												if (!(c instanceof String)) continue;
												String childExtId = (String) c;
												if (childExtId.contains(" ")) {
													childExtId = childExtId.replaceAll("\\s+", "");
												}
												linkStudents.add(childExtId);
											}
										} else if (o instanceof String){
											String childExtId = (String) o;
											if (childExtId.contains(" ")) {
												childExtId = childExtId.replaceAll("\\s+", "");
											}
											linkStudents.add(childExtId);
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
											addErrorByFile(profile, "invalid.child.mapping", "" + (i+1) , "childLUsername");
											handler.handle(result);
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
								if (checkChildMapping || classesNamesMapping.size() > 0) {
									for (Object o : linkStudents) {
										if (!(o instanceof String)) continue;
										if (classesNamesMapping.get(o) != null) {
											classesNames.add(classesNamesMapping.get(o));
										}
									}
									if (classesNames.size() == 0 && enableRelativeStudentLinkCheck) {
										addSoftErrorByFile(profile, "missing.student.soft", "" + (i + 1),
												user.getString("firstName"), user.getString("lastName"));
									}
								} else {
									Object c = user.getValue("childExternalId");
									JsonObject u = new JsonObject()
											.put("lastName", user.getString("lastName"))
											.put("firstName", user.getString("firstName"))
											.put("line", i);
									if (c instanceof String) {
										c = new JsonArray().add(c);
									}
									if (c instanceof JsonArray) {
										for (Object ceId : ((JsonArray) c)) {
											if (isEmpty((String) ceId)) continue;
											JsonArray jr = checkChildExists.getJsonArray((String) ceId);
											if (jr == null) {
												jr = new JsonArray();
												checkChildExists.put((String) ceId, jr);
											}
											jr.add(u);
										}
									}
								}
								if (classesNames.size() == 0 && enableRelativeStudentLinkCheck) {
									// "NO_ATTR" is used because we can't map this soft error to any attribute
									addSoftErrorByFile(profile, "missing.student.soft", "" + (i+1), "NO_ATTR",
											user.getString("firstName"), user.getString("lastName"));
								}
								break;
						}
						// softerrorContext is used to collect context information usefull to display soft Error
						// it follows the shape : [{"reason":"error.key", "attribute":"lastName", "value":""}...
						// TODO : Merge error (return of Validator.validate()) and softErrorContext
						JsonArray softErrorsContext = new JsonArray();
						String error = validator.validate(user, acceptLanguage, true, softErrorsContext);
						if (error != null && softErrorsContext == null) {
							log.warn(error);
							addErrorByFile(profile, "validator.errorWithLine", "" + (i+1), error); // Note that 'error' is already translated
						} else {
							final String classesStr = Joiner.on(", ").join(classesNames);
							if (!"Relative".equals(profile)) {
								classesNamesMapping.put(user.getString("externalId"), classesStr);
							}
							addUser(profile, user.put("state", translate(state.name()))
									.put("translatedProfile", translate(profile))
									.put("classesStr", classesStr)
									.put("childExternalId", linkStudents)
									.put("line", i + 1)
							);
							for (Object sec : softErrorsContext) {
								JsonObject err = (JsonObject)sec;
									addSoftErrorByFile(profile,
											err.getString("reason"), "" + (i + 1),
											translate(err.getString("attribute")), err.getString("value"),
											"nta-" + err.getString("attribute"),
											err.getString("errorLevel")
									);
							}
						}
						i++;
					}
				} catch (Exception e) {
					addError(profile, "csv.exception");
					log.error("csv.exception", e);
				}
				if (!checkChildExists.isEmpty()) {
					final String query =
							"MATCH (u:User) " +
							"WHERE u.externalId IN {childExternalIds} " +
							"RETURN COLLECT(u.externalId) as childExternalIds ";
					final JsonObject params = new JsonObject().put("childExternalIds", new JsonArray(
							new ArrayList<>(checkChildExists.fieldNames())));
					Neo4j.getInstance().execute(query, params, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								JsonArray existsChilds = getOrElse(getOrElse(getOrElse(event.body().getJsonArray("result"), new JsonArray())
										.getJsonObject(0), new JsonObject()).getJsonArray("childExternalIds"), new JsonArray());
								for (String cexternalId : checkChildExists.fieldNames()) {
									if (!existsChilds.contains(cexternalId)) {
										for (Object o: checkChildExists.getJsonArray(cexternalId)) {
											if (!(o instanceof JsonObject)) continue;
											JsonObject user = (JsonObject) o;
											addSoftErrorByFile(profile, "missing.student.soft", "" + (user.getInteger("line") + 1),
													user.getString("firstName"), user.getString("lastName"));
										}
									}
								}
							}
							handler.handle(result);
						}
					});
				} else {
					handler.handle(result);
				}
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
					final Structure s = new Structure(result.getJsonObject(0));
					try {
						final JsonObject structure = CSVUtil.getStructure(path);
						final String overrideClass = structure.getString("overrideClass");
						if (isNotEmpty(overrideClass)) {
							s.setOverrideClass(overrideClass);
						}
					} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
						log.error("Error parsing structure path : " + path, e);
					}
					handler.handle(s);
				} else {
					try {
						final JsonObject structure = CSVUtil.getStructure(path);
						final String overrideClass = structure.getString("overrideClass");
						final Structure s = new Structure(structure);
						if (isNotEmpty(overrideClass)) {
							s.setOverrideClass(overrideClass);
						}
						handler.handle(s);
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

	public ProfileColumnsMapper getColumnsMapper() {
		return columnsMapper;
	}

}
