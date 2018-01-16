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

package org.entcore.timeline.events;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;

import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class DefaultTimelineEventStore implements TimelineEventStore {

	private static final String TIMELINE_COLLECTION = "timeline";

	private MongoDb mongo = MongoDb.getInstance();

	private final DateFormat mongoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX", Locale.getDefault());

	@Override
	public void add(JsonObject event, final Handler<JsonObject> result) {
		JsonObject doc = validAndGet(event);
		if (doc != null) {
			if (!doc.containsField("date")) {
				doc.putObject("date", MongoDb.now());
			}
			doc.putObject("created", doc.getObject("date"));
			mongo.save(TIMELINE_COLLECTION, doc, resultHandler(result));
		} else {
			result.handle(invalidArguments());
		}
	}

	@Override
	public void delete(String resource, Handler<JsonObject> result) {
		if (resource != null && !resource.trim().isEmpty()) {
			JsonObject query = new JsonObject()
					.putString("resource", resource);
			mongo.delete(TIMELINE_COLLECTION, query, resultHandler(result));
		} else {
			result.handle(invalidArguments());
		}
	}

	@Override
	public void get(final UserInfos user, List<String> types, int offset, int limit, JsonObject restrictionFilter,
			boolean mine, final Handler<JsonObject> result) {
		final String recipient = user.getUserId();
		final String externalId = user.getExternalId();
		if (recipient != null && !recipient.trim().isEmpty()) {
			final JsonObject query = new JsonObject()
					.putObject("deleted", new JsonObject()
						.putBoolean("$exists", false))
					.putObject("date", new JsonObject().putObject("$lt", MongoDb.now()));
			if (externalId == null || externalId.trim().isEmpty()) {
				query.putString(mine ? "sender" : "recipients.userId", recipient);
			} else {
				query.putObject(mine ? "sender" : "recipients.userId", new JsonObject()
						.putArray("$in", new JsonArray().add(recipient).add(externalId)));
			}
			query.putObject("reportAction.action", new JsonObject().putString("$ne", "DELETE"));
			if (types != null && !types.isEmpty()) {
				if (types.size() == 1) {
					query.putString("type", types.get(0));
				} else {
					JsonArray typesFilter = new JsonArray();
					for (String t: types) {
						typesFilter.addObject(new JsonObject().putString("type", t));
					}
					query.putArray("$or", typesFilter);
				}
			}
			if(restrictionFilter != null && restrictionFilter.size() > 0){
				JsonArray nor = new JsonArray();
				for(String type : restrictionFilter.toMap().keySet()){
					for(Object eventType : restrictionFilter.getArray(type, new JsonArray())){
						nor.add(new JsonObject()
							.putString("type", type)
							.putString("event-type", eventType.toString()));
					}
					query.putArray("$nor", nor);
				}
			}
			JsonObject sort = new JsonObject().putNumber("created", -1);
			JsonObject keys = new JsonObject()
				.putNumber("message", 1)
				.putNumber("params", 1)
				.putNumber("date", 1)
				.putNumber("sender", 1)
				.putNumber("comments", 1)
				.putNumber("type", 1)
				.putNumber("event-type", 1)
				.putNumber("resource", 1)
				.putNumber("sub-resource", 1)
				.putNumber("add-comment", 1);
			if(!mine){
				keys.putObject("recipients", new JsonObject()
						.putObject("$elemMatch", new JsonObject()
							.putString("userId", user.getUserId())));
				keys.putObject("reporters", new JsonObject()
						.putObject("$elemMatch", new JsonObject()
							.putString("userId", user.getUserId())));
			}

			mongo.find(TIMELINE_COLLECTION, query, sort, keys,
					offset, limit, 100, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					result.handle(message.body());
				}
			});
		} else {
			result.handle(invalidArguments());
		}
	}

	@Override
	public void deleteSubResource(String resource, Handler<JsonObject> result) {
		if (resource != null && !resource.trim().isEmpty()) {
			JsonObject query = new JsonObject()
					.putString("sub-resource", resource);
			mongo.delete(TIMELINE_COLLECTION, query, resultHandler(result));
		} else {
			result.handle(invalidArguments());
		}
	}

	@Override
	public void listTypes(final Handler<JsonArray> result) {
		mongo.distinct(TIMELINE_COLLECTION, "type", new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					result.handle(event.body().getArray("values", new JsonArray()));
				} else {
					result.handle(new JsonArray());
				}
			}
		});
	}

	private JsonObject validAndGet(JsonObject json) {
		if (json != null) {
			JsonObject e = json.copy();
			for (String attr: json.getFieldNames()) {
				if (!FIELDS.contains(attr) || e.getValue(attr) == null) {
					e.removeField(attr);
				}
			}
			if (e.toMap().keySet().containsAll(REQUIRED_FIELDS)) {
				return e;
			}
		}
		return null;
	}

	private JsonObject invalidArguments() {
		return new JsonObject().putString("status", "error")
				.putString("message", "Invalid arguments.");
	}


	private Handler<Message<JsonObject>> resultHandler(final Handler<JsonObject> result) {
		return new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				result.handle(message.body());
			}
		};
	}

	private void markEventsAsRead(Message<JsonObject> message, String recipient) {
		JsonArray events = message.body().getArray("results");
		if (events != null && "ok".equals(message.body().getString("status"))) {
			JsonArray ids = new JsonArray();
			for (Object o : events) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject json = (JsonObject) o;
				ids.addString(json.getString("_id"));
			}
			JsonObject q = new JsonObject()
					.putObject("_id", new JsonObject().putArray("$in", ids))
					.putObject("recipients", new JsonObject().putObject("$elemMatch",
							new JsonObject().putString("userId", recipient).putNumber("unread", 1)
					));
			mongo.update(TIMELINE_COLLECTION, q, new JsonObject().putObject("$set",
					new JsonObject().putNumber("recipients.$.unread", 0)), false, true);
		}
	}

	@Override
	public void delete(String id, String sender, Handler<Either<String, JsonObject>> result) {
		JsonObject matcher = new JsonObject()
			.putString("_id", id)
			.putString("sender", sender);

		JsonObject objNew = new JsonObject().putObject("$set", new JsonObject()
			.putArray("recipients", new JsonArray())
			.putNumber("deleted", 1));

		mongo.update(TIMELINE_COLLECTION, matcher, objNew, MongoDbResult.validActionResultHandler(result));
	}

	@Override
	public void discard(String id, String recipient, Handler<Either<String, JsonObject>> result) {
		JsonObject criteria = new JsonObject()
			.putString("_id", id);

		JsonObject objNew = new JsonObject()
			.putObject("$pull", new JsonObject()
					.putObject("recipients", new JsonObject()
						.putString("userId", recipient)));

		mongo.update(TIMELINE_COLLECTION, criteria, objNew, MongoDbResult.validActionResultHandler(result));
	}

	@Override
	public void report(String id, UserInfos user, Handler<Either<String, JsonObject>> result) {
		String now = mongoFormat.format(Calendar.getInstance().getTime());

		JsonObject criteria = new JsonObject()
			.putString("_id", id)
			.putObject("reporters.userId", new JsonObject()
				.putString("$ne", user.getUserId()));

		JsonObject objNew = new JsonObject()
			.putObject("$addToSet", new JsonObject()
				.putObject("reportedStructures", new JsonObject()
					.putArray("$each", new JsonArray(user.getStructures())))
				.putObject("reporters", new JsonObject()
					.putString("userId", user.getUserId())
					.putString("firstName", user.getFirstName())
					.putString("lastName", user.getLastName())
					.putString("date", now)));

		mongo.update(TIMELINE_COLLECTION, criteria, objNew, MongoDbResult.validActionResultHandler(result));
	}

	@Override
	public void listReported(String structure, boolean pending, int offset, int limit, Handler<Either<String, JsonArray>> result) {
		JsonObject matcher = new JsonObject()
				.putString("reportedStructures", structure);
		JsonObject sort = new JsonObject();
		JsonObject keys = new JsonObject().putNumber("recipients", 0);

		if(pending){
			matcher.putObject("reportAction", new JsonObject()
				.putBoolean("$exists", false));
			sort.putNumber("reporters.date", -1);
		} else {
			matcher.putObject("reportAction", new JsonObject()
				.putBoolean("$exists", true));
			sort.putNumber("reportAction.date", -1);
		}

		mongo.find(TIMELINE_COLLECTION, matcher, sort, keys, offset, limit, 100, MongoDbResult.validResultsHandler(result));
	}

	@Override
	public void performAdminAction(String id, String structureId, UserInfos user, AdminAction action, Handler<Either<String, JsonObject>> result) {
		String now = mongoFormat.format(Calendar.getInstance().getTime());

		JsonObject criteria = new JsonObject()
			.putString("_id", id)
			.putString("reportedStructures", structureId)
			.putObject("reportAction", new JsonObject()
				.putString("$ne", AdminAction.DELETE.name()));

		JsonObject objSet = new JsonObject()
			.putObject("reportAction", new JsonObject()
					.putString("action", action.name())
					.putString("userId", user.getUserId())
					.putString("firstName", user.getFirstName())
					.putString("lastName", user.getLastName())
					.putString("date", now));
		JsonObject objNew = new JsonObject().putObject("$set", objSet);

		if(action == AdminAction.DELETE) {
			objSet.putArray("recipients", new JsonArray());
		}

		mongo.update(TIMELINE_COLLECTION, criteria, objNew, MongoDbResult.validActionResultHandler(result));
	}

	@Override
	public void deleteReportNotification(String resourceId, Handler<Either<String, JsonObject>> result) {
		JsonObject matcher = new JsonObject()
			.putString("type", "TIMELINE")
			.putString("event-type", "NOTIFY-REPORT")
			.putString("resource", resourceId);

		mongo.delete(TIMELINE_COLLECTION, matcher, MongoDbResult.validActionResultHandler(result));
	}

}
