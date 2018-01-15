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

package org.entcore.directory.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.utils.StringUtils;
import org.entcore.directory.Directory;
import org.entcore.directory.services.TimetableService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;
import static org.entcore.common.neo4j.Neo4jResult.*;

public class DefaultTimetableService implements TimetableService {

	private static final String COURSES = "courses";
	private final EventBus eb;
	private final Neo4j neo4j = Neo4j.getInstance();
	private static final JsonObject KEYS = new JsonObject().putNumber("_id", 1).putNumber("structureId", 1).putNumber("subjectId", 1)
			.putNumber("roomLabels", 1).putNumber("equipmentLabels", 1).putNumber("teacherIds", 1).putNumber("personnelIds", 1)
			.putNumber("classes", 1).putNumber("groups", 1).putNumber("dayOfWeek", 1).putNumber("startDate", 1).putNumber("endDate", 1)
			.putNumber("subjectId", 1).putNumber("roomLabels", 1);
	private static final String START_DATE_PATTERN = "T00:00Z";
	private static final String END_DATE_PATTERN = "T23.59Z";

	public DefaultTimetableService(EventBus eb) {
		this.eb = eb;
	}

	@Override
	public void listCourses(String structureId, long lastDate, Handler<Either<String, JsonArray>> handler) {
		if (Utils.validationParamsNull(handler, structureId)) return;
		final JsonObject query = new JsonObject().putString("structureId", structureId);
		final JsonObject sort = new JsonObject().putNumber("startDate", 1);
		final JsonObject keys = KEYS.copy();
		if (lastDate > 0) {
			query.putArray("$or", new JsonArray()
					.addObject(new JsonObject().putObject("modified", new JsonObject().putNumber("$gte", lastDate)))
					.addObject(new JsonObject().putObject("deleted", new JsonObject().putNumber("$gte", lastDate))));
			keys.putNumber("deleted", 1);
		} else {
			query.putObject("deleted", new JsonObject().putBoolean("$exists", false))
					.putObject("modified", new JsonObject().putBoolean("$exists", true));
		}
		MongoDb.getInstance().find(COURSES, query, sort, keys, validResultsHandler(handler));
	}

	@Override
	public void listCoursesBetweenTwoDates(String structureId, String teacherId, List<String> groupNames, String begin, String end, Handler<Either<String,JsonArray>> handler){
		if (Utils.validationParamsNull(handler, structureId, begin, end)) return;
		final JsonObject query = new JsonObject();

		query.putString("structureId", structureId);

		if (teacherId != null){
			query.putString("teacherIds", teacherId);
		}

		final String startDate = begin + START_DATE_PATTERN;
		final String endDate = end + END_DATE_PATTERN;

		JsonObject betweenStart = new JsonObject();
		betweenStart.putString("$lte", endDate);

		JsonObject betweenEnd = new JsonObject();
		betweenEnd.putString("$gte", startDate);

		if (groupNames != null) {
			JsonObject dateOperand =  new JsonObject()
					.putArray("$and", new JsonArray()
							.addObject(new JsonObject().putObject("startDate" ,betweenStart))
							.addObject(new JsonObject().putObject("endDate" ,betweenEnd)));

			JsonObject groupOperand = new JsonObject();
			JsonArray groupsNameArray = new JsonArray();

			for (int i = 0; i < groupNames.size(); i++) {
				String groupName = groupNames.get(i);
				groupsNameArray.addObject(new JsonObject().putString("classes", groupName));
				groupsNameArray.addObject(new JsonObject().putString("groups", groupName));
			}

			groupOperand.putArray("$or", groupsNameArray);

			query.putArray("$and", new JsonArray().addObject(dateOperand).add(groupOperand));
		} else {
			query.putArray("$and", new JsonArray()
					.addObject(new JsonObject().putObject("startDate", betweenStart))
					.addObject(new JsonObject().putObject("endDate", betweenEnd)));
		}

		final JsonObject sort = new JsonObject().putNumber("startDate", 1);

		MongoDb.getInstance().find(COURSES, query, sort, KEYS, validResultsHandler(handler));
	}

	@Override
	public void listSubjects(String structureId, List<String> teachers, boolean classes, boolean groups,
	                         Handler<Either<String, JsonArray>> handler) {
		if (Utils.validationParamsNull(handler, structureId)) return;
		listSubjects(structureId, teachers, null, classes, groups, handler);
	}

	@Override
	public void listSubjectsByGroup(String structureId, String externalGroupId,
	                         Handler<Either<String, JsonArray>> handler) {
		if (Utils.validationParamsNull(handler, structureId, externalGroupId)) return;
		listSubjects(structureId, null, externalGroupId, true, true, handler);
	}

	private void listSubjects(String structureId, List<String> teachers, String externalGroupId, boolean classes, boolean groups,
	                          Handler<Either<String, JsonArray>> handler) {
		final JsonObject params = new JsonObject().putString("id", structureId);
		StringBuilder query = new StringBuilder();
		query.append("MATCH (:Structure {id:{id}})<-[:SUBJECT]-(sub:Subject)");
		StringBuilder whereClause = new StringBuilder().append(" WHERE 1=1");
		if (teachers != null && !teachers.isEmpty()) {
			query.append("<-[r:TEACHES]-(u:User)");
			params.putArray("teacherIds", new JsonArray(teachers));
			whereClause.append(" AND u.id IN  {teacherIds}");
		}
		if (!StringUtils.isEmpty(externalGroupId)) {
			params.putString("externalGroupId", externalGroupId);
			whereClause.append(" AND ({externalGroupId} IN r.classes OR {externalGroupId} IN r.groups)");
		}
		query.append(whereClause.toString());
		query.append(" RETURN sub.id as subjectId, sub.code as subjectCode, sub.label as subjectLabel");

		if (teachers != null && !teachers.isEmpty()) {
			query.append(", u.id as teacherId");
		}

		if (classes) {
			query.append(", r.classes as classes");
		}
		if (groups) {
			query.append(", r.groups as groups");
		}

		neo4j.execute(query.toString(), params, validResultHandler(handler));
	}

	@Override
	public void initStructure(String structureId, JsonObject conf, Handler<Either<String, JsonObject>> handler) {
		JsonObject action = new JsonObject().putString("action", "manual-init-timetable-structure")
				.putObject("conf", conf.putString("structureId", structureId));
		eb.send(Directory.FEEDER, action, validUniqueResultHandler(handler));
	}

	@Override
	public void classesMapping(String structureId, final Handler<Either<String, JsonObject>> handler) {
		final String query =
				"MATCH (s:Structure {id:{id}})<-[:MAPPING]-(cm:ClassesMapping) " +
				"OPTIONAL MATCH s<-[:BELONGS]-(c:Class) " +
				"return cm.mapping as mapping, cm.unknownClasses as unknownClasses, collect(c.name) as classNames ";
		final JsonObject params = new JsonObject().putString("id", structureId);
		neo4j.execute(query, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight() && event.right().getValue() != null &&
						event.right().getValue().getString("mapping") != null) {
					try {
						event.right().getValue().putObject("mapping",
								new JsonObject(event.right().getValue().getString("mapping")));
					} catch (Exception e) {
						handler.handle(new Either.Left<String, JsonObject>(e.getMessage()));
					}
				}
				handler.handle(event);
			}
		}));
	}

	@Override
	public void updateClassesMapping(final String structureId, final JsonObject mapping,
			final Handler<Either<String, JsonObject>> handler) {

		classesMapping(structureId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					final JsonObject cm = event.right().getValue();
					if (cm == null || cm.getArray("unknownClasses") == null) {
						handler.handle(new Either.Left<String, JsonObject>("missing.classes.mapping"));
						return;
					}
					final JsonArray uc = cm.getArray("unknownClasses");
					final JsonObject m = mapping.getObject("mapping");
					for (String attr : m.copy().getFieldNames()) {
						if (!uc.contains(attr)) {
							m.removeField(attr);
						}
					}
					mapping.putString("mapping", m.encode());
					final String query =
							"MATCH (:Structure {id:{id}})<-[:MAPPING]-(cm:ClassesMapping) " +
									"SET cm.mapping = {mapping} ";
					neo4j.execute(query, mapping.putString("id", structureId), validEmptyHandler(handler));
				} else {
					handler.handle(event);
				}
			}
		});
	}

	@Override
	public void importTimetable(String structureId, final String path, final String domain, final String acceptLanguage, final Handler<Either<JsonObject, JsonObject>> handler) {
		final String query = "MATCH (s:Structure {id:{id}}) RETURN s.UAI as UAI, s.timetable as timetable";
		neo4j.execute(query, new JsonObject().putString("id", structureId),
				validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				final JsonArray errors = new JsonArray();
				final JsonObject ge = new JsonObject().putArray("error.global", errors);
				if (event.isRight() && isNotEmpty(event.right().getValue().getString("UAI")) &&
						TIMETABLE_TYPES.contains(event.right().getValue().getString("timetable"))) {
					if (!("EDT".equals(event.right().getValue().getString("timetable")) && !path.endsWith("\\.xml")) &&
							!("UDT".equals(event.right().getValue().getString("timetable")) && !path.endsWith("\\.zip"))) {
						errors.addString(I18n.getInstance().translate("invalid.import.format", domain, acceptLanguage));
						handler.handle(new Either.Left<JsonObject, JsonObject>(ge));
						return;
					}
					JsonObject action = new JsonObject().putString("action", "manual-" +
							event.right().getValue().getString("timetable").toLowerCase())
							.putString("path", path)
							.putString("UAI", event.right().getValue().getString("UAI"))
							.putString("language", acceptLanguage);
					eb.send(Directory.FEEDER, action, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								JsonObject r = event.body().getObject("result", new JsonObject());
								if (r.getObject("errors", new JsonObject()).size() > 0) {
									handler.handle(new Either.Left<JsonObject, JsonObject>(r.getObject("errors")));
								} else {
									handler.handle(new Either.Right<JsonObject, JsonObject>(r.getObject("ignored")));
								}
							} else {
								errors.addString(event.body().getString("message", ""));
								handler.handle(new Either.Left<JsonObject, JsonObject>(ge));
							}
						}
					});
				} else {
					errors.addString(I18n.getInstance().translate("invalid.structure", domain, acceptLanguage));
					handler.handle(new Either.Left<JsonObject, JsonObject>(ge));
				}
			}
		}));
	}

}
