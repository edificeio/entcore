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

package org.entcore.feeder.timetable;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import org.entcore.common.neo4j.Neo4jUtils;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.dictionary.structures.Transition;
import org.entcore.feeder.dictionary.users.PersEducNat;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.exceptions.ValidationException;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.utils.Report;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.joda.time.DateTime;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public abstract class AbstractTimetableImporter implements TimetableImporter {

	protected static final Logger log = LoggerFactory.getLogger(AbstractTimetableImporter.class);
	private static final String CREATE_SUBJECT =
			"MATCH (s:Structure {externalId : {structureExternalId}}) " +
			"MERGE (sub:Subject {externalId : {externalId}}) " +
			"ON CREATE SET sub.code = {Code}, sub.label = {Libelle}, sub.id = {id} " +
			"SET sub.lastUpdated = {now}, sub.source = {source} " +
			"MERGE (sub)-[:SUBJECT]->(s) ";
	private static final String LINK_SUBJECT =
			"MATCH (s:Subject {id : {subjectId}}), (u:User) " +
			"WHERE u.id IN {teacherIds} " +
			"MERGE u-[r:TEACHES]->s " +
			"SET r.classes = FILTER(c IN coalesce(r.classes, []) where NOT(c IN r.classes)) + {classes}, " +
			"r.groups = FILTER(g IN coalesce(r.groups, []) where NOT(g IN r.groups)) + {groups}, " +
			"r.lastUpdated = {now}, r.source = {source} ";
	private static final String DELETE_SUBJECT =
			"MATCH (s:Structure {externalId : {structureExternalId}})<-[:SUBJECT]-(sub:Subject {source: {source}}) " +
			"WHERE NOT(sub.id IN {subjects}) " +
			"DETACH DELETE sub";
	private static final String UNLINK_SUBJECT =
			"MATCH (s:Structure {externalId : {structureExternalId}})<-[:SUBJECT]-(:Subject)<-[r:TEACHES {source: {source}}]-(:User) " +
			"WHERE r.lastUpdated <> {now} " +
			"DELETE r";
	protected static final String UNKNOWN_CLASSES =
			"MATCH (s:Structure {UAI : {UAI}})<-[:BELONGS]-(c:Class) " +
			"WHERE c.name = {className} " +
			"WITH count(*) AS exists " +
			"MATCH (s:Structure {UAI : {UAI}}) " +
			"WHERE exists = 0 " +
			"MERGE (cm:ClassesMapping { UAI : {UAI}}) " +
			"SET cm.unknownClasses = coalesce(FILTER(cn IN cm.unknownClasses WHERE cn <> {className}), []) + {className} " +
			"MERGE (s)<-[:MAPPING]-(cm) ";
	protected static final String CREATE_GROUPS =
			"MATCH (s:Structure {externalId : {structureExternalId}}) " +
			"MERGE (fg:FunctionalGroup:Group {externalId:{externalId}}) " +
			"ON CREATE SET fg.name = {name}, fg.id = {id}, fg.source = {source}, fg.displayNameSearchField = {displayNameSearchField} " +
			"MERGE (fg)-[:DEPENDS]->(s) ";
	private static final String PERSEDUCNAT_TO_GROUPS =
			"MATCH (u:User {id : {id}}), (fg:FunctionalGroup) " +
			"WHERE fg.externalId IN {groups} " +
			"MERGE u-[r:IN]->fg " +
			"SET r.lastUpdated = {now}, r.source = {source}, r.outDate = {outDate} ";
	private static final String PERSEDUCNAT_TO_CLASSES =
			"MATCH (u:User {id : {id}}), (:Structure {externalId : {structureExternalId}})<-[:BELONGS]-(c:Class)" +
			"<-[:DEPENDS]-(pg:ProfileGroup {name : c.name + '-' + {profile}}) " +
			"WHERE u.source = {source} AND c.name IN {classes} " +
			"MERGE u-[r:IN]->pg " +
			"SET r.lastUpdated = {now}, r.source = {source}, r.outDate = {outDate} ";
	private static final String UNLINK_GROUP =
			"MATCH (s:Structure {externalId : {structureExternalId}})<-[:DEPENDS]-(fg:FunctionalGroup)<-[r:IN]-(u:User) " +
			"WHERE r.source = {source} AND (r.outDate < {now} OR r.lastUpdated < {now}) " +
			"OPTIONAL MATCH fg-[rc:COMMUNIQUE]-u " +
			"DELETE r, rc";
	private static final String DELETE_GROUPS =
			"MATCH (:Structure {externalId : {structureExternalId}})<-[:DEPENDS]-(g:FunctionalGroup {source:{source}})<-[:IN]-(:User) " +
			"WITH COLLECT(distinct g.id) as usedFunctionalGroup " +
			"MATCH (:Structure {externalId : {structureExternalId}})<-[:DEPENDS]-(g:FunctionalGroup {source:{source}}) " +
			"WHERE NOT(g.id IN usedFunctionalGroup) " +
			"DETACH DELETE g ";
	// prevent difference between relationships and properties
	private static final String UNSET_OLD_GROUPS =
			"MATCH (:Structure {externalId : {structureExternalId}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
			"WHERE NOT(HAS(u.deleteDate)) AND has(u.groups) AND LENGTH(u.groups) > 0 " +
			"AND NOT(u-[:IN]->(:FunctionalGroup)) " +
			"SET u.groups = [];";
	private static final String SET_GROUPS =
			"MATCH (:Structure {externalId : {structureExternalId}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
			"WITH u " +
			"MATCH u-[:IN]->(g:FunctionalGroup) " +
			"WHERE has(u.groups) " +
			"WITH u, collect(g.externalId) as groups " +
			"SET u.groups = groups";
	public static final String COURSES = "courses";
	protected long importTimestamp;
	protected final String UAI;
	protected final Report report;
	protected final JsonArray structure = new fr.wseduc.webutils.collections.JsonArray();
	protected String structureExternalId;
	protected String structureId;
	protected JsonObject classesMapping;
	protected DateTime startDateWeek1;
	protected int slotDuration; // seconds
	protected Map<String, Slot> slots = new HashMap<>();
	protected final Map<String, String> rooms = new HashMap<>();
	protected final Map<String, String[]> teachersMapping = new HashMap<>();
	protected final Map<String, String[]> teachersCleanNameMapping = new HashMap<>();
	protected final Map<String, String> teachers = new HashMap<>();
	protected final Map<String, String> subjectsMapping = new HashMap<>();
	protected final Map<String, String> subjects = new HashMap<>();
	protected final Map<String, JsonObject> classes = new HashMap<>();
	protected final Map<String, JsonObject> groups = new HashMap<>();

	protected PersEducNat persEducNat;
	protected TransactionHelper txXDT;
	private final MongoDb mongoDb = MongoDb.getInstance();
	private final AtomicInteger countMongoQueries = new AtomicInteger(0);
	private Handler<AsyncResult<Report>> endHandler;
	protected final String basePath;
	private boolean txSuccess = false;
	protected Set<String> userImportedExternalId = new HashSet<>();
	private volatile JsonArray coursesBuffer = new fr.wseduc.webutils.collections.JsonArray();

	protected AbstractTimetableImporter(String uai, String path, String acceptLanguage) {
		UAI = uai;
		this.basePath = path;
		this.report = new Report(acceptLanguage);
	}

	protected void init(final Handler<AsyncResult<Void>> handler) throws TransactionException {
		importTimestamp = System.currentTimeMillis();
		final String externalIdFromUAI = "MATCH (s:Structure {UAI : {UAI}}) " +
				"return s.externalId as externalId, s.id as id, s.timetable as timetable ";
		final String tma = getTeacherMappingAttribute();
		final String getUsersByProfile =
				"MATCH (:Structure {UAI : {UAI}})<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User) " +
				"WHERE head(u.profiles) = {profile} AND NOT(u." + tma +  " IS NULL) " +
				"RETURN DISTINCT u.id as id, u." + tma + " as tma, head(u.profiles) as profile, u.source as source, " +
				"u.lastName as lastName, u.firstName as firstName";
		final String classesMappingQuery =
				"MATCH (s:Structure {UAI : {UAI}})<-[:MAPPING]-(cm:ClassesMapping) " +
				"return cm.mapping as mapping ";
		final String subjectsMappingQuery = "MATCH (s:Structure {UAI : {UAI}})<-[:SUBJECT]-(sub:Subject) return sub.code as code, sub.id as id";
		final TransactionHelper tx = TransactionManager.getTransaction();
		tx.add(getUsersByProfile, new JsonObject().put("UAI", UAI).put("profile", "Teacher"));
		tx.add(externalIdFromUAI, new JsonObject().put("UAI", UAI));
		tx.add(classesMappingQuery, new JsonObject().put("UAI", UAI));
		tx.add(subjectsMappingQuery, new JsonObject().put("UAI", UAI));
		tx.commit(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final JsonArray res = event.body().getJsonArray("results");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 4) {
					try {
						for (Object o : res.getJsonArray(0)) {
							if (o instanceof JsonObject) {
								final JsonObject j = (JsonObject) o;
								teachersMapping.put(j.getString("tma"), new String[]{j.getString("id"), j.getString("source")});
								teachersCleanNameMapping.put(Validator
										.sanitize(j.getString("firstName")+j.getString("lastName")),
										new String[]{j.getString("id"), j.getString("source")});
							}
						}
						JsonArray a = res.getJsonArray(1);
						if (a != null && a.size() == 1) {
							structureExternalId = a.getJsonObject(0).getString("externalId");
							structure.add(structureExternalId);
							structureId = a.getJsonObject(0).getString("id");
							if (!getSource().equals(a.getJsonObject(0).getString("timetable"))) {
								handler.handle(new DefaultAsyncResult<Void>(new TransactionException("different.timetable.type")));
								return;
							}
						} else {
							handler.handle(new DefaultAsyncResult<Void>(new ValidationException("invalid.uai")));
							return;
						}
						JsonArray cm = res.getJsonArray(2);
						if (cm != null && cm.size() == 1) {
							try {
								final JsonObject cmn = cm.getJsonObject(0);
								log.info(cmn.encode());
								if (isNotEmpty(cmn.getString("mapping"))) {
									classesMapping = new JsonObject(cmn.getString("mapping"));
									log.info("classMapping : " + classesMapping.encodePrettily());
								} else {
									classesMapping = new JsonObject();
								}
							} catch (Exception ecm) {
								classesMapping = new JsonObject();
								log.error(ecm.getMessage(), ecm);
							}
						}
						JsonArray subjects = res.getJsonArray(3);
						if (subjects != null && subjects.size() > 0) {
							for (Object o : subjects) {
								if (o instanceof JsonObject) {
									final  JsonObject s = (JsonObject) o;
									subjectsMapping.put(s.getString("code"), s.getString("id"));
								}
							}
						}
						txXDT = TransactionManager.getTransaction();
						persEducNat = new PersEducNat(txXDT, report, getSource());
						persEducNat.setMapping("dictionary/mapping/" + getSource().toLowerCase() + "/PersEducNat.json");
						handler.handle(new DefaultAsyncResult<>((Void) null));
					} catch (Exception e) {
						handler.handle(new DefaultAsyncResult<Void>(e));
					}
				} else {
					handler.handle(new DefaultAsyncResult<Void>(new TransactionException(event.body().getString("message"))));
				}
			}
		});
	}

	protected void addSubject(String id, JsonObject currentEntity) {
		String subjectId = subjectsMapping.get(currentEntity.getString("Code"));
		if (isEmpty(subjectId)) {
			final String externalId = structureExternalId + "$" + currentEntity.getString("Code");
			subjectId = UUID.randomUUID().toString();
			txXDT.add(CREATE_SUBJECT, currentEntity.put("structureExternalId", structureExternalId)
					.put("externalId", externalId).put("id", subjectId)
					.put("source", getSource()).put("now", importTimestamp));
		}
		subjects.put(id, subjectId);
	}

	protected void persistCourse(JsonObject object) {
		if (object == null) {
			return;
		}
		persEducNatToClasses(object);
		persEducNatToGroups(object);
		persEducNatToSubjects(object);
		object.put("pending", importTimestamp);
		final int currentCount = countMongoQueries.incrementAndGet();
		JsonObject m = new JsonObject().put("$set", object)
				.put("$setOnInsert", new JsonObject().put("created", importTimestamp));
		coursesBuffer.add(new JsonObject()
						.put("operation", "upsert")
						.put("document", m)
						.put("criteria", new JsonObject().put("_id", object.getString("_id")))
		);

		if (currentCount % 1000 == 0) {
			persistBulKCourses();
		}
	}

	private void persistBulKCourses() {
		final JsonArray cf = coursesBuffer;
		coursesBuffer = new fr.wseduc.webutils.collections.JsonArray();
		final int countCoursesBuffer = cf.size();
		if (countCoursesBuffer > 0) {
			mongoDb.bulk(COURSES, cf, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if (!"ok".equals(event.body().getString("status"))) {
						if (event.body().getString("message") == null ||
									!event.body().getString("message").contains("duplicate key error")) {
							report.addError("error.persist.course");
						} else {
							log.warn("Duplicate courses keys.");
						}
					}
					if (countMongoQueries.addAndGet(-countCoursesBuffer) == 0) {
						end();
					}
				}
			});
		}
	}

	private void persEducNatToClasses(JsonObject object) {
		final JsonArray classes = object.getJsonArray("classes");
		if (classes != null) {
			final JsonObject params = new JsonObject()
					.put("structureExternalId", structureExternalId)
					.put("classes", classes)
					.put("source", getSource())
					.put("outDate", DateTime.now().plusDays(1).getMillis())
					.put("now", importTimestamp);
			final JsonArray teacherIds = object.getJsonArray("teacherIds");
			if (teacherIds != null && teacherIds.size() > 0) {
				params.put("profile", "Teacher");
				for (Object id : teacherIds) {
					if (id != null) {
						txXDT.add(PERSEDUCNAT_TO_CLASSES, params.copy().put("id", id.toString()));
					}
				}
			}
			final JsonArray personnelIds = object.getJsonArray("personnelIds");
			if (personnelIds != null && personnelIds.size() > 0) {
				params.put("profile", "Personnel");
				for (Object id : personnelIds) {
					if (id != null) {
						txXDT.add(PERSEDUCNAT_TO_CLASSES, params.copy().put("id", id.toString()));
					}
				}
			}
		}
	}


	private void persEducNatToSubjects(JsonObject object) {
		final String subjectId = object.getString("subjectId");
		final JsonArray teacherIds = object.getJsonArray("teacherIds");
		if (isNotEmpty(subjectId) && teacherIds != null && teacherIds.size() > 0) {
			final JsonObject params = new JsonObject()
					.put("subjectId", subjectId)
					.put("teacherIds", teacherIds)
					.put("classes", object.getJsonArray("classes", new fr.wseduc.webutils.collections.JsonArray()))
					.put("groups", object.getJsonArray("groups", new fr.wseduc.webutils.collections.JsonArray()))
					.put("source", getSource()).put("now", importTimestamp);
			txXDT.add(LINK_SUBJECT, params);
		}
	}

	private void persEducNatToGroups(JsonObject object) {
		final JsonArray groups = object.getJsonArray("groups");
		if (groups != null) {
			final JsonArray teacherIds = object.getJsonArray("teacherIds");
			final List<String> ids = new ArrayList<>();
			if (teacherIds != null) {
				ids.addAll(teacherIds.getList());
			}
			final JsonArray personnelIds = object.getJsonArray("personnelIds");
			if (personnelIds != null) {
				ids.addAll(personnelIds.getList());
			}
			if (!ids.isEmpty()) {
				final JsonArray g = new fr.wseduc.webutils.collections.JsonArray();
				for (Object o : groups) {
					g.add(structureExternalId + "$" + o.toString());
				}
				for (String id : ids) {
					txXDT.add(PERSEDUCNAT_TO_GROUPS, new JsonObject()
							.put("groups", g)
							.put("id", id)
							.put("source", getSource())
							.put("outDate", DateTime.now().plusDays(1).getMillis())
							.put("now", importTimestamp));
				}
			}
		}
	}

	protected void updateUser(JsonObject user) {
		user.remove("Ident");
		user.remove("epj");
		final String attrs = Neo4jUtils.nodeSetPropertiesFromJson("u", user,
				"id", "externalId", "login", "activationCode", "displayName", "email");
		if (isNotEmpty(attrs.trim())) {
			final String updateUser =
					"MATCH (u:User {" + getTeacherMappingAttribute() + ": {" + getTeacherMappingAttribute() + "}}) " +
							"SET " + attrs;
			txXDT.add(updateUser, user);
		}
	}

	private void end() {
		if (endHandler != null && countMongoQueries.get() == 0) {
			final JsonObject baseQuery = new JsonObject().put("structureId", structureId);
			if (txSuccess) {
				mongoDb.update(COURSES, baseQuery.copy().put("pending", importTimestamp),
						new JsonObject().put("$rename", new JsonObject().put("pending", "modified")),
						false, true, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if ("ok".equals(event.body().getString("status"))) {
									mongoDb.update(COURSES, baseQuery.copy()
											.put("deleted", new JsonObject().put("$exists", false))
											.put("modified", new JsonObject().put("$ne", importTimestamp)),
									new JsonObject().put("$set", new JsonObject().put("deleted", importTimestamp)),
									false, true, new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> event) {
											if (!"ok".equals(event.body().getString("status"))) {
												report.addError("error.set.deleted.courses");
											}
											endHandler.handle(new DefaultAsyncResult<>(report));
										}
									});
								} else {
									report.addError("error.renaming.pending");
									endHandler.handle(new DefaultAsyncResult<>(report));
								}
							}
						});
			} else {
				mongoDb.delete(COURSES, baseQuery.copy()
						.put("pending", importTimestamp)
						.put("modified", new JsonObject().put("$exists", false)), new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if ("ok".equals(event.body().getString("status"))) {
							mongoDb.update(COURSES, baseQuery.copy()
											.put("pending", importTimestamp),
									new JsonObject().put("$unset", new JsonObject().put("pending", "")),
									false, true, new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> event) {
											if (!"ok".equals(event.body().getString("status"))) {
												report.addError("error.unset.pending");
											}
											endHandler.handle(new DefaultAsyncResult<>(report));
										}
									});
						} else {
							report.addError("error.removing.inconsistencies.courses");
							endHandler.handle(new DefaultAsyncResult<>(report));
						}
					}
				});
			}
		}
	}

	protected void commit(final Handler<AsyncResult<Report>> handler) {
		final JsonObject params = new JsonObject().put("structureExternalId", structureExternalId)
				.put("source", getSource()).put("now", importTimestamp);
		persistBulKCourses();
		txXDT.add(DELETE_SUBJECT, params.copy().put("subjects", new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(subjects.values()))));
		txXDT.add(UNLINK_SUBJECT, params);
		txXDT.add(UNLINK_GROUP, params);
		txXDT.add(DELETE_GROUPS, params);
		txXDT.add(UNSET_OLD_GROUPS, params);
		txXDT.add(SET_GROUPS, params);
		Importer.markMissingUsers(structureExternalId, getSource(), userImportedExternalId, txXDT, new Handler<Void>() {
			@Override
			public void handle(Void event) {
				Importer.restorePreDeletedUsers(getSource(), txXDT);
				txXDT.commit(new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if (!"ok".equals(event.body().getString("status"))) {
							report.addError("error.commit.timetable.transaction");
						} else {
							txSuccess = true;
						}
						endHandler = handler;
						end();
					}
				});
			}
		});
	}

	protected abstract String getSource();

	protected abstract String getTeacherMappingAttribute();

	public static void updateMergedUsers(JsonArray mergedUsers) {
		if (mergedUsers == null) return;
		long now = System.currentTimeMillis();
		for (int i=1; i < mergedUsers.size(); i+=2) {
			final JsonArray a = mergedUsers.getJsonArray(i);
			if (a.size() > 0) {
				updateMergedUsers(a.getJsonObject(0), now);
			}
		}
	}

	private static void updateMergedUsers(JsonObject j, long now) {
		final String oldId = j.getString("oldId");
		final String id = j.getString("id");
		final String profile = j.getString("profile");
		if (isEmpty(oldId) || isEmpty(id) || (!"Teacher".equals(profile) && !"Personnel".equals(profile))) {
			return;
		}
		final JsonObject query = new JsonObject();
		final JsonObject modifier = new JsonObject();
		final String pl = profile.toLowerCase();
		query.put(pl + "Ids", oldId);
		modifier.put("$set", new JsonObject()
				.put(pl + "Ids.$", id)
				.put("modified", now));
		MongoDb.getInstance().update(COURSES, query, modifier, false, true);
	}

	public static void transition(final String structureExternalId) {
		if (isNotEmpty(structureExternalId)) {
			final String query = "MATCH (s:Structure {externalId: {externalId}}) RETURN s.id as structureId";
			final JsonObject params = new JsonObject().put("externalId", structureExternalId);
			TransactionManager.getNeo4jHelper().execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					final JsonArray res = event.body().getJsonArray("result");
					if ("ok".equals(event.body().getString("status")) && res != null && res.size() > 0) {
						transitionDeleteCourses(res.getJsonObject(0));
						transitionDeleteSubjectsAndMapping(structureExternalId);
					}
				}
			});
		} else {
			transitionDeleteCourses(new JsonObject());
			transitionDeleteSubjectsAndMapping(structureExternalId);
		}
	}

	private static void transitionDeleteCourses(final JsonObject query) {
		MongoDb.getInstance().delete(COURSES, query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (!"ok".equals(event.body().getString("status"))) {
					log.error("Courses timetable transition error on structure " + query.encode() +
							" - message : " + event.body().getString("message"));
				}
			}
		});
	}

	private static void transitionDeleteSubjectsAndMapping(final String structureExternalId) {
		final JsonObject params = new JsonObject();
		String filter = "";
		if (isNotEmpty(structureExternalId)) {
			filter = " {externalId : {structureExternalId}}";
			params.put("structureExternalId", structureExternalId);
		}
		try {
			final TransactionHelper tx = TransactionManager.getTransaction();
			tx.add("MATCH (s:Structure" + filter + ") SET s.timetable = 'NOP'", params);
			tx.add("MATCH (:Structure" + filter + ")<-[:SUBJECT]-(sub:Subject) DETACH DELETE sub", params);
			tx.add("MATCH (:Structure" + filter + ")<-[:MAPPING]-(cm:ClassesMapping) DETACH DELETE cm", params);
			tx.commit(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if (!"ok".equals(event.body().getString("status"))) {
						log.error("Subjects timetable transition error on structure " + params.encode() +
								" - message : " + event.body().getString("message"));
					}
				}
			});
		} catch (TransactionException e) {
			log.error("Unable to acquire transaction for timetable transition", e);
		}
	}

	public static void initStructure(final EventBus eb, final Message<JsonObject> message) {
		final JsonObject conf = message.body().getJsonObject("conf");
		if (conf == null) {
			message.reply(new JsonObject().put("status", "error").put("message", "invalid.conf"));
			return;
		}
		final String query =
				"MATCH (s:Structure {id:{structureId}}) " +
				"RETURN (NOT(HAS(s.timetable)) OR s.timetable <> {type}) as update ";
		TransactionManager.getNeo4jHelper().execute(query, conf, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(final Message<JsonObject> event) {
				final JsonArray j = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && j != null && j.size() == 1 &&
						j.getJsonObject(0).getBoolean("update", false)) {
					try {
						TransactionHelper tx = TransactionManager.getTransaction();
						final String q1 =
								"MATCH (s:Structure {id : {structureId}})<-[:DEPENDS]-(fg:FunctionalGroup) " +
								"WHERE NOT(HAS(s.timetable)) OR s.timetable <> {type} " +
								"OPTIONAL MATCH fg<-[:IN]-(u:User) " +
								"RETURN fg.id as group, fg.name as groupName, collect(u.id) as users ";
						final String q2 =
								"MATCH (s:Structure {id: {structureId}}) " +
								"WHERE NOT(HAS(s.timetable)) OR s.timetable <> {type} " +
								"SET s.timetable = {type} " +
								"WITH s " +
								"MATCH s<-[:DEPENDS]-(fg:FunctionalGroup), s<-[:SUBJECT]-(sub:Subject) " +
								"DETACH DELETE fg, sub ";
						final String q3 =
								"MATCH (s:Structure {id: {structureId}})<-[:MAPPING]-(cm:ClassesMapping) " +
								"DETACH DELETE cm";
						tx.add(q1, conf);
						tx.add(q2, conf);
						tx.add(q3, conf);
						tx.commit(new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> res) {
								if ("ok".equals(res.body().getString("status"))) {
									final JsonArray r = res.body().getJsonArray("results");
									if (r != null && r.size() == 2) {
										Transition.publishDeleteGroups(eb, log, r.getJsonArray(0));
									}
									final JsonObject matcher = new JsonObject().put("structureId", conf.getString("structureId"));
									MongoDb.getInstance().delete(COURSES, matcher, new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> mongoResult) {
											if (!"ok".equals(mongoResult.body().getString("status"))) {
												log.error("Error deleting courses : " + mongoResult.body().getString("message"));
											}
											message.reply(event.body());
										}
									});
								} else {
									message.reply(res.body());
								}
							}
						});
					} catch (TransactionException e) {
						log.error("Transaction error when init timetable structure", e);
						message.reply(new JsonObject().put("status", "error").put("message", e.getMessage()));
					}
				} else {
					message.reply(event.body());
				}
			}
		});
	}

}
