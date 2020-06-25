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

package org.entcore.directory.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.utils.StringUtils;
import org.entcore.directory.Directory;
import org.entcore.directory.services.TimetableService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import com.mongodb.QueryBuilder;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;
import static org.entcore.common.neo4j.Neo4jResult.*;

public class DefaultTimetableService implements TimetableService {

	private static final String COURSES = "courses";
	private final EventBus eb;
	private final Neo4j neo4j = Neo4j.getInstance();
	private final MongoDb mongo = MongoDb.getInstance();
	private static final JsonObject KEYS = new JsonObject().put("_id", 1).put("structureId", 1).put("subjectId", 1)
			.put("roomLabels", 1).put("equipmentLabels", 1).put("teacherIds", 1).put("personnelIds", 1)
			.put("classes", 1).put("groups", 1).put("dayOfWeek", 1).put("startDate", 1).put("endDate", 1)
			.put("subjectId", 1).put("roomLabels", 1);
	private static final String START_DATE_PATTERN = "T00:00Z";
	private static final String END_DATE_PATTERN = "T23.59Z";

	public DefaultTimetableService(EventBus eb) {
		this.eb = eb;
	}

	@Override
	public void listCourses(String structureId, long lastDate, Handler<Either<String, JsonArray>> handler) {
		if (Utils.validationParamsNull(handler, structureId)) return;
		final JsonObject query = new JsonObject().put("structureId", structureId);
		final JsonObject sort = new JsonObject().put("startDate", 1);
		final JsonObject keys = KEYS.copy();
		if (lastDate > 0) {
			query.put("$or", new fr.wseduc.webutils.collections.JsonArray()
					.add(new JsonObject().put("modified", new JsonObject().put("$gte", lastDate)))
					.add(new JsonObject().put("deleted", new JsonObject().put("$gte", lastDate))));
			keys.put("deleted", 1);
		} else {
			query.put("deleted", new JsonObject().put("$exists", false))
					.put("modified", new JsonObject().put("$exists", true));
		}
		MongoDb.getInstance().find(COURSES, query, sort, keys, validResultsHandler(handler));
	}

	@Override
	public void listCoursesBetweenTwoDates(String structureId, String teacherId, List<String> groupNames, String begin, String end, Handler<Either<String,JsonArray>> handler){
		if (Utils.validationParamsNull(handler, structureId, begin, end)) return;
		final JsonObject query = new JsonObject();

		query.put("structureId", structureId);

		if (teacherId != null){
			query.put("teacherIds", teacherId);
		}

		final String startDate = begin + START_DATE_PATTERN;
		final String endDate = end + END_DATE_PATTERN;

		JsonObject betweenStart = new JsonObject();
		betweenStart.put("$lte", endDate);

		JsonObject betweenEnd = new JsonObject();
		betweenEnd.put("$gte", startDate);

		if (groupNames != null) {
			JsonObject dateOperand =  new JsonObject()
					.put("$and", new fr.wseduc.webutils.collections.JsonArray()
							.add(new JsonObject().put("startDate" ,betweenStart))
							.add(new JsonObject().put("endDate" ,betweenEnd)));

			JsonObject groupOperand = new JsonObject();
			JsonArray groupsNameArray = new JsonArray();

			for (int i = 0; i < groupNames.size(); i++) {
				String groupName = groupNames.get(i);
				groupsNameArray.add(new JsonObject().put("classes", groupName));
				groupsNameArray.add(new JsonObject().put("groups", groupName));
			}

			groupOperand.put("$or", groupsNameArray);

			query.put("$and", new JsonArray().add(dateOperand).add(groupOperand));
		} else {
			query.put("$and", new fr.wseduc.webutils.collections.JsonArray()
					.add(new JsonObject().put("startDate", betweenStart))
					.add(new JsonObject().put("endDate", betweenEnd)));
		}

		final JsonObject sort = new JsonObject().put("startDate", 1);

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
		final JsonObject params = new JsonObject().put("id", structureId);
		StringBuilder query = new StringBuilder();
		query.append("MATCH (:Structure {id:{id}})<-[:SUBJECT]-(sub:Subject)");
		StringBuilder whereClause = new StringBuilder().append(" WHERE 1=1");
		if (teachers != null && !teachers.isEmpty()) {
			query.append("<-[r:TEACHES]-(u:User)");
			params.put("teacherIds", new fr.wseduc.webutils.collections.JsonArray(teachers));
			whereClause.append(" AND u.id IN  {teacherIds}");
		}
		if (!StringUtils.isEmpty(externalGroupId)) {
			params.put("externalGroupId", externalGroupId);
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
		JsonObject action = new JsonObject().put("action", "manual-init-timetable-structure")
				.put("conf", conf.put("structureId", structureId));
		eb.send(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(handler)));
	}

	@Override
	public void classesMapping(String structureId, final Handler<Either<String, JsonObject>> handler) {
		final String query =
				"MATCH (s:Structure {id:{id}})<-[:MAPPING]-(cm:ClassesMapping) " +
				"OPTIONAL MATCH s<-[:BELONGS]-(c:Class) " +
				"return cm.mapping as mapping, cm.unknownClasses as unknownClasses, collect(c.name) as classNames ";
		final JsonObject params = new JsonObject().put("id", structureId);
		neo4j.execute(query, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight() && event.right().getValue() != null &&
						event.right().getValue().getString("mapping") != null) {
					try {
						event.right().getValue().put("mapping",
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
					if (cm == null || cm.getJsonArray("unknownClasses") == null) {
						handler.handle(new Either.Left<String, JsonObject>("missing.classes.mapping"));
						return;
					}
					final JsonArray uc = cm.getJsonArray("unknownClasses");
					final JsonObject m = mapping.getJsonObject("mapping");
					for (String attr : m.copy().fieldNames()) {
						if (!uc.contains(attr)) {
							m.remove(attr);
						}
					}
					mapping.put("mapping", m.encode());
					final String query =
							"MATCH (:Structure {id:{id}})<-[:MAPPING]-(cm:ClassesMapping) " +
									"SET cm.mapping = {mapping} ";
					neo4j.execute(query, mapping.put("id", structureId), validEmptyHandler(handler));
				} else {
					handler.handle(event);
				}
			}
		});
	}

	private void getStructureUAI(String structureId, Handler<Either<String, String>> handler)
	{
		final JsonObject params = new JsonObject().put("id", structureId);
		StringBuilder query = new StringBuilder();
		query.append("MATCH (s:Structure {id:{id}}) RETURN s.UAI as UAI");

		neo4j.execute(query.toString(), params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>()
		{
			@Override
			public void handle(Either<String, JsonObject> event)
			{
				if(event.isLeft() == true)
					handler.handle(new Either.Left<String, String>(event.left().getValue()));
				else
					handler.handle(new Either.Right<String, String>(event.right().getValue().getString("UAI")));
			}
		}));
	}

	@Override
	public void listReports(String structureId, Handler<Either<String, JsonArray>> handler)
	{
		this.getStructureUAI(structureId, new Handler<Either<String, String>>()
		{
			@Override
			public void handle(Either<String, String> either)
			{
				if(either.isLeft() == true)
					handler.handle(new Either.Left<String,JsonArray>(either.left().getValue()));
				else
				{
					String UAI = either.right().getValue();

					QueryBuilder query = QueryBuilder.start("UAI").is(UAI);
					JsonObject sort = new JsonObject()
						.put("created", -1);
					JsonObject projection = new JsonObject()
						.put("_id", 1)
						.put("created", 1)
						.put("source", 1);

					mongo.find("timetableImports", MongoQueryBuilder.build(query), sort, projection, new Handler<Message<JsonObject>>()
					{
						@Override
						public void handle(Message<JsonObject> event)
						{
							handler.handle(Utils.validResults(event));
						}
					});
				}
			}
		});
	}

	@Override
	public void getReport(String structureId, String reportId, Handler<Either<String, JsonObject>> handler)
	{
		this.getStructureUAI(structureId, new Handler<Either<String, String>>()
		{
			@Override
			public void handle(Either<String, String> either)
			{
				if(either.isLeft() == true)
					handler.handle(new Either.Left<String,JsonObject>(either.left().getValue()));
				else
				{
					String UAI = either.right().getValue();

					QueryBuilder query = QueryBuilder.start().and(
						QueryBuilder.start("UAI").is(UAI).get(),
						QueryBuilder.start("_id").is(reportId).get()
					);
					JsonObject projection = new JsonObject()
						.put("_id", 1)
						.put("created", 1)
						.put("source", 1)
						.put("report", 1);

					mongo.findOne("timetableImports", MongoQueryBuilder.build(query), projection, new Handler<Message<JsonObject>>()
					{
						@Override
						public void handle(Message<JsonObject> event)
						{
							handler.handle(Utils.validResult(event));
						}
					});
				}
			}
		});
		
	}

	@Override
	public void importTimetable(String structureId, final String path, final String domain,
			final String acceptLanguage, boolean uai, final Handler<Either<JsonObject, JsonObject>> handler) {
		final String  structureAttr = uai ? "UAI" : "id";
		final String query = "MATCH (s:Structure {" + structureAttr + ":{id}}) RETURN s.UAI as UAI, s.timetable as timetable";
		neo4j.execute(query, new JsonObject().put("id", structureId),
				validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				final JsonArray errors = new fr.wseduc.webutils.collections.JsonArray();
				final JsonObject ge = new JsonObject().put("error.global", errors);
				if (event.isRight() && isNotEmpty(event.right().getValue().getString("UAI")) &&
						TIMETABLE_TYPES.contains(event.right().getValue().getString("timetable"))) {
					if (!("EDT".equals(event.right().getValue().getString("timetable")) && !path.endsWith("\\.xml")) &&
							!("UDT".equals(event.right().getValue().getString("timetable")) && !path.endsWith("\\.zip"))) {
						errors.add(I18n.getInstance().translate("invalid.import.format", domain, acceptLanguage));
						handler.handle(new Either.Left<JsonObject, JsonObject>(ge));
						return;
					}
					JsonObject action = new JsonObject().put("action", "manual-" +
							event.right().getValue().getString("timetable").toLowerCase())
							.put("path", path)
							.put("UAI", event.right().getValue().getString("UAI"))
							.put("language", acceptLanguage);
					eb.send(Directory.FEEDER, action, new DeliveryOptions().setSendTimeout(600000l), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								JsonObject r = event.body().getJsonObject("result", new JsonObject());
								if (r.getJsonObject("errors", new JsonObject()).size() > 0) {
									handler.handle(new Either.Left<JsonObject, JsonObject>(r.getJsonObject("errors")));
								} else {
									handler.handle(new Either.Right<JsonObject, JsonObject>(r));
								}
							} else {
								errors.add(event.body().getString("message", ""));
								handler.handle(new Either.Left<JsonObject, JsonObject>(ge));
							}
						}
					}));
				} else {
					errors.add(I18n.getInstance().translate("invalid.structure", domain, acceptLanguage));
					handler.handle(new Either.Left<JsonObject, JsonObject>(ge));
				}
			}
		}));
	}

}
