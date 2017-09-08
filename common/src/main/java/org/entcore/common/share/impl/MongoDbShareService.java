/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.common.share.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.*;
import fr.wseduc.webutils.security.SecuredAction;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MongoDbShareService extends GenericShareService {

	private final String collection;
	private final MongoDb mongo;

	public MongoDbShareService(EventBus eb, MongoDb mongo, String collection,
			Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions) {
		super(eb, securedActions, groupedActions);
		this.mongo = mongo;
		this.collection = collection;
	}

	@Override
	public void shareInfos(final String userId, String resourceId, final String acceptLanguage, final String search,
			final Handler<Either<String, JsonObject>> handler) {
		if (userId == null || userId.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("Invalid userId."));
			return;
		}
		if (resourceId == null || resourceId.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("Invalid resourceId."));
			return;
		}
		final JsonArray actions = getResoureActions(securedActions);
		QueryBuilder query = QueryBuilder.start("_id").is(resourceId);
		JsonObject keys = new JsonObject().putNumber("shared", 1);
		mongo.findOne(collection, MongoQueryBuilder.build(query), keys,
			new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						JsonArray shared = event.body().getObject("result", new JsonObject())
								.getArray("shared", new JsonArray());
						JsonObject gs = new JsonObject();
						JsonObject us = new JsonObject();
						for (Object o : shared) {
							if (!(o instanceof JsonObject)) continue;
							JsonObject userShared = (JsonObject) o;
							JsonArray a = new JsonArray();
							for (String attrName : userShared.getFieldNames()) {
								if ("userId".equals(attrName) || "groupId".equals(attrName)) {
									continue;
								}
								if (groupedActions != null && groupedActions.containsKey(attrName)) {
									for (String action: groupedActions.get(attrName)) {
										a.addString(action.replaceAll("\\.", "-"));
									}
								} else {
									a.addString(attrName);
								}
							}
							final String g = userShared.getString("groupId");
							String u;
							if (g != null) {
								gs.putArray(g, a);
							} else if ((u = userShared.getString("userId")) != null && !u.equals(userId)){
								us.putArray(u, a);
							}
						}
						getShareInfos(userId, actions, gs, us, acceptLanguage, search, new Handler<JsonObject>() {
							@Override
							public void handle(JsonObject event) {
								if (event != null && event.size() == 3) {
									handler.handle(new Either.Right<String, JsonObject>(event));
								} else {
									handler.handle(new Either.Left<String, JsonObject>(
											"Error finding shared resource."));
								}
							}
						});
					} else {
						handler.handle(new Either.Left<String, JsonObject>(
								event.body().getString("error", "Error finding shared resource.")));
					}
				}
		});
	}

	@Override
	public void groupShare(final String userId, final String groupShareId, final String resourceId,
			final List<String> actions, final Handler<Either<String, JsonObject>> handler) {
		inShare(resourceId, groupShareId, true, new Handler<Boolean>() {

			@Override
			public void handle(Boolean event) {
				if (Boolean.TRUE.equals(event)) {
					share(resourceId, groupShareId, actions, true, handler);
				} else {
					groupShareValidation(userId, groupShareId, actions, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> event) {
							if (event.isRight()) {
								share(resourceId, groupShareId, actions, true, handler);
							} else {
								handler.handle(event);
							}
						}
					});
				}
			}
		});
	}

	private void inShare(String resourceId, String shareId, boolean group, final Handler<Boolean> handler) {
		QueryBuilder query = QueryBuilder.start("_id").is(resourceId)
				.put("shared").elemMatch(QueryBuilder.start(group ? "groupId" : "userId").is(shareId).get());
		mongo.count(collection, MongoQueryBuilder.build(query), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject res = event.body();
				handler.handle(
						res != null &&
						"ok".equals(res.getString("status")) &&
						1 == res.getInteger("count")
				);
			}
		});
	}

	private void share(String resourceId, final String groupShareId, final List<String> actions,
			boolean isGroup, final Handler<Either<String, JsonObject>> handler) {
		final String shareIdAttr = isGroup ? "groupId" : "userId";
		QueryBuilder query = QueryBuilder.start("_id").is(resourceId);
		JsonObject keys = new JsonObject().putNumber("shared", 1);
		final JsonObject q = MongoQueryBuilder.build(query);
		mongo.findOne(collection, q, keys, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) &&
						event.body().getObject("result") != null) {
					JsonArray actual = event.body().getObject("result")
							.getArray("shared", new JsonArray());
					boolean exist = false;
					for (int i = 0; i < actual.size(); i++) {
						JsonObject s = actual.get(i);
						String id = s.getString(shareIdAttr);
						if (groupShareId.equals(id)) {
							for (String action: actions) {
								s.putBoolean(action, true);
							}
							if (groupedActions != null) {
								for (Map.Entry<String, List<String>> ga: groupedActions.entrySet()) {
									if (actions.containsAll(ga.getValue())) {
										s.putBoolean(ga.getKey(), true);
									}
								}
							}
							exist = true;
							break;
						}
					}
					final AtomicBoolean notifyTimeline = new AtomicBoolean(false);
					if (!exist) {
						JsonObject t = new JsonObject().putString(shareIdAttr, groupShareId);
						actual.add(t);
						for (String action: actions) {
							t.putBoolean(action, true);
						}
						if (groupedActions != null) {
							for (Map.Entry<String, List<String>> ga: groupedActions.entrySet()) {
								if (actions.containsAll(ga.getValue())) {
									t.putBoolean(ga.getKey(), true);
								}
							}
						}
						notifyTimeline.set(true);
					}
					MongoUpdateBuilder updateQuery = new MongoUpdateBuilder().set("shared", actual);
					mongo.update(collection, q, updateQuery.build(), new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> res) {
							if (notifyTimeline.get()) {
								JsonObject notify = new JsonObject();
								notify.putString(shareIdAttr, groupShareId);
								res.body().putObject("notify-timeline", notify);
							}
							handler.handle(Utils.validResult(res));
						}
					});
				} else {
					handler.handle(new Either.Left<String, JsonObject>("Resource not found."));
				}
			}
		});
	}

	@Override
	public void userShare(final String userId, final String userShareId, final String resourceId,
			final List<String> actions, final Handler<Either<String, JsonObject>> handler) {
		inShare(resourceId, userShareId, false, new Handler<Boolean>() {

			@Override
			public void handle(Boolean event) {
				if (Boolean.TRUE.equals(event)) {
					share(resourceId, userShareId, actions, false, handler);
				} else {
					userShareValidation(userId, userShareId, actions, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> event) {
							if (event.isRight()) {
								share(resourceId, userShareId, actions, false, handler);
							} else {
								handler.handle(event);
							}
						}
					});
				}
			}
		});
	}

	@Override
	public void removeGroupShare(String groupId, String resourceId, List<String> actions,
			Handler<Either<String, JsonObject>> handler) {
		removeShare(resourceId, groupId, actions, true, handler);
	}

	@Override
	public void removeUserShare(String userId, String resourceId, List<String> actions,
			Handler<Either<String, JsonObject>> handler) {
		removeShare(resourceId, userId, actions, false, handler);
	}

	private void removeShare(String resourceId, final String shareId, List<String> removeActions,
			boolean isGroup, final Handler<Either<String, JsonObject>> handler) {
		final String shareIdAttr = isGroup ? "groupId" : "userId";
		final List<String> actions = findRemoveActions(removeActions);
		QueryBuilder query = QueryBuilder.start("_id").is(resourceId);
		JsonObject keys = new JsonObject().putNumber("shared", 1);
		final JsonObject q = MongoQueryBuilder.build(query);
		mongo.findOne(collection, q, keys, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) &&
						event.body().getObject("result") != null) {
					JsonArray actual = event.body().getObject("result")
							.getArray("shared", new JsonArray());
					JsonArray shared = new JsonArray();
					for (int i = 0; i < actual.size(); i++) {
						JsonObject s = actual.get(i);
						String id = s.getString(shareIdAttr);
						if (shareId.equals(id)) {
							if (actions != null) {
								for (String action: actions) {
									s.removeField(action);
								}
								if (s.size() > 1) {
									shared.add(s);
								}
							}
						} else {
							shared.add(s);
						}
					}
					MongoUpdateBuilder updateQuery = new MongoUpdateBuilder().set("shared", shared);
					mongo.update(collection, q, updateQuery.build(), new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> res) {
							handler.handle(Utils.validResult(res));
						}
					});
				} else {
					handler.handle(new Either.Left<String, JsonObject>("Resource not found."));
				}
			}
		});
	}

}
