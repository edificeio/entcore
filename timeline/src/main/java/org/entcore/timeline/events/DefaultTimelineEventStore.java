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
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
			if (!doc.containsKey("date")) {
				doc.put("date", MongoDb.now());
			}
			doc.put("created", doc.getJsonObject("date"));
			mongo.save(TIMELINE_COLLECTION, doc, resultHandler(result));
		} else {
			result.handle(invalidArguments());
		}
	}

	@Override
	public void delete(String resource, Handler<JsonObject> result) {
		if (resource != null && !resource.trim().isEmpty()) {
			JsonObject query = new JsonObject()
					.put("resource", resource);
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
					.put("deleted", new JsonObject()
						.put("$exists", false))
					.put("date", new JsonObject().put("$lt", MongoDb.now()));
			if (externalId == null || externalId.trim().isEmpty()) {
				query.put(mine ? "sender" : "recipients.userId", recipient);
			} else {
				query.put(mine ? "sender" : "recipients.userId", new JsonObject()
						.put("$in", new JsonArray().add(recipient).add(externalId)));
			}
			query.put("reportAction.action", new JsonObject().put("$ne", "DELETE"));
			if (types != null && !types.isEmpty()) {
				if (types.size() == 1) {
					query.put("type", types.get(0));
				} else {
					JsonArray typesFilter = new JsonArray();
					for (String t: types) {
						typesFilter.add(new JsonObject().put("type", t));
					}
					query.put("$or", typesFilter);
				}
			}
			if(restrictionFilter != null && restrictionFilter.size() > 0){
				JsonArray nor = new JsonArray();
				for(String type : restrictionFilter.getMap().keySet()){
					for(Object eventType : restrictionFilter.getJsonArray(type, new JsonArray())){
						nor.add(new JsonObject()
							.put("type", type)
							.put("event-type", eventType.toString()));
					}
					query.put("$nor", nor);
				}
			}
			JsonObject sort = new JsonObject().put("created", -1);
			JsonObject keys = new JsonObject()
				.put("message", 1)
				.put("params", 1)
				.put("date", 1)
				.put("sender", 1)
				.put("comments", 1)
				.put("type", 1)
				.put("event-type", 1)
				.put("resource", 1)
				.put("sub-resource", 1)
				.put("add-comment", 1);
			if(!mine){
				keys.put("recipients", new JsonObject()
						.put("$elemMatch", new JsonObject()
							.put("userId", user.getUserId())));
				keys.put("reporters", new JsonObject()
						.put("$elemMatch", new JsonObject()
							.put("userId", user.getUserId())));
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
					.put("sub-resource", resource);
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
					result.handle(event.body().getJsonArray("values", new JsonArray()));
				} else {
					result.handle(new JsonArray());
				}
			}
		});
	}

	private JsonObject validAndGet(JsonObject json) {
		if (json != null) {
			JsonObject e = json.copy();
			for (String attr: json.fieldNames()) {
				if (!FIELDS.contains(attr) || e.getValue(attr) == null) {
					e.remove(attr);
				}
			}
			if (e.getMap().keySet().containsAll(REQUIRED_FIELDS)) {
				return e;
			}
		}
		return null;
	}

	private JsonObject invalidArguments() {
		return new JsonObject().put("status", "error")
				.put("message", "Invalid arguments.");
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
		JsonArray events = message.body().getJsonArray("results");
		if (events != null && "ok".equals(message.body().getString("status"))) {
			JsonArray ids = new JsonArray();
			for (Object o : events) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject json = (JsonObject) o;
				ids.add(json.getString("_id"));
			}
			JsonObject q = new JsonObject()
					.put("_id", new JsonObject().put("$in", ids))
					.put("recipients", new JsonObject().put("$elemMatch",
							new JsonObject().put("userId", recipient).put("unread", 1)
					));
			mongo.update(TIMELINE_COLLECTION, q, new JsonObject().put("$set",
					new JsonObject().put("recipients.$.unread", 0)), false, true);
		}
	}

	@Override
	public void delete(String id, String sender, Handler<Either<String, JsonObject>> result) {
		JsonObject matcher = new JsonObject()
			.put("_id", id)
			.put("sender", sender);

		JsonObject objNew = new JsonObject().put("$set", new JsonObject()
			.put("recipients", new JsonArray())
			.put("deleted", 1));

		mongo.update(TIMELINE_COLLECTION, matcher, objNew, MongoDbResult.validActionResultHandler(result));
	}

	@Override
	public void discard(String id, String recipient, Handler<Either<String, JsonObject>> result) {
		JsonObject criteria = new JsonObject()
			.put("_id", id);

		JsonObject objNew = new JsonObject()
			.put("$pull", new JsonObject()
					.put("recipients", new JsonObject()
						.put("userId", recipient)));

		mongo.update(TIMELINE_COLLECTION, criteria, objNew, MongoDbResult.validActionResultHandler(result));
	}

	@Override
	public void report(String id, UserInfos user, Handler<Either<String, JsonObject>> result) {
		String now = mongoFormat.format(Calendar.getInstance().getTime());

		JsonObject criteria = new JsonObject()
			.put("_id", id)
			.put("reporters.userId", new JsonObject()
				.put("$ne", user.getUserId()));

		JsonObject objNew = new JsonObject()
			.put("$addToSet", new JsonObject()
				.put("reportedStructures", new JsonObject()
					.put("$each", new JsonArray(user.getStructures())))
				.put("reporters", new JsonObject()
					.put("userId", user.getUserId())
					.put("firstName", user.getFirstName())
					.put("lastName", user.getLastName())
					.put("date", now)));

		mongo.update(TIMELINE_COLLECTION, criteria, objNew, MongoDbResult.validActionResultHandler(result));
	}

	@Override
	public void listReported(String structure, boolean pending, int offset, int limit, Handler<Either<String, JsonArray>> result) {
		JsonObject matcher = new JsonObject()
				.put("reportedStructures", structure);
		JsonObject sort = new JsonObject();
		JsonObject keys = new JsonObject().put("recipients", 0);

		if(pending){
			matcher.put("reportAction", new JsonObject()
				.put("$exists", false));
			sort.put("reporters.date", -1);
		} else {
			matcher.put("reportAction", new JsonObject()
				.put("$exists", true));
			sort.put("reportAction.date", -1);
		}

		mongo.find(TIMELINE_COLLECTION, matcher, sort, keys, offset, limit, 100, MongoDbResult.validResultsHandler(result));
	}

	@Override
	public void performAdminAction(String id, String structureId, UserInfos user, AdminAction action, Handler<Either<String, JsonObject>> result) {
		String now = mongoFormat.format(Calendar.getInstance().getTime());

		JsonObject criteria = new JsonObject()
			.put("_id", id)
			.put("reportedStructures", structureId)
			.put("reportAction", new JsonObject()
				.put("$ne", AdminAction.DELETE.name()));

		JsonObject objSet = new JsonObject()
			.put("reportAction", new JsonObject()
					.put("action", action.name())
					.put("userId", user.getUserId())
					.put("firstName", user.getFirstName())
					.put("lastName", user.getLastName())
					.put("date", now));
		JsonObject objNew = new JsonObject().put("$set", objSet);

		if(action == AdminAction.DELETE) {
			objSet.put("recipients", new JsonArray());
		}

		mongo.update(TIMELINE_COLLECTION, criteria, objNew, MongoDbResult.validActionResultHandler(result));
	}

	@Override
	public void deleteReportNotification(String resourceId, Handler<Either<String, JsonObject>> result) {
		JsonObject matcher = new JsonObject()
			.put("type", "TIMELINE")
			.put("event-type", "NOTIFY-REPORT")
			.put("resource", resourceId);

		mongo.delete(TIMELINE_COLLECTION, matcher, MongoDbResult.validActionResultHandler(result));
	}

}
