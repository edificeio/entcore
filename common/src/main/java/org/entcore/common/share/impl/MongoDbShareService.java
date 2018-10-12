/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.common.share.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.*;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static fr.wseduc.webutils.Utils.getOrElse;

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
		JsonObject keys = new JsonObject().put("shared", 1);
		mongo.findOne(collection, MongoQueryBuilder.build(query), keys,
			new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if ("ok".equals(event.body().getString("status"))) {
						JsonArray shared = event.body().getJsonObject("result", new JsonObject())
								.getJsonArray("shared", new fr.wseduc.webutils.collections.JsonArray());
						JsonObject gs = new JsonObject();
						JsonObject us = new JsonObject();
						for (Object o : shared) {
							if (!(o instanceof JsonObject)) continue;
							JsonObject userShared = (JsonObject) o;
							JsonArray a = new fr.wseduc.webutils.collections.JsonArray();
							for (String attrName : userShared.fieldNames()) {
								if ("userId".equals(attrName) || "groupId".equals(attrName)) {
									continue;
								}
								if (groupedActions != null && groupedActions.containsKey(attrName)) {
									for (String action: groupedActions.get(attrName)) {
										a.add(action.replaceAll("\\.", "-"));
									}
								} else {
									a.add(attrName);
								}
							}
							final String g = userShared.getString("groupId");
							String u;
							if (g != null) {
								gs.put(g, a);
							} else if ((u = userShared.getString("userId")) != null && !u.equals(userId)){
								us.put(u, a);
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
		JsonObject keys = new JsonObject().put("shared", 1);
		final JsonObject q = MongoQueryBuilder.build(query);
		mongo.findOne(collection, q, keys, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) &&
						event.body().getJsonObject("result") != null) {
					JsonArray actual = event.body().getJsonObject("result")
							.getJsonArray("shared", new fr.wseduc.webutils.collections.JsonArray());
					boolean exist = false;
					for (int i = 0; i < actual.size(); i++) {
						JsonObject s = actual.getJsonObject(i);
						String id = s.getString(shareIdAttr);
						if (groupShareId.equals(id)) {
							for (String action : actions) {
								s.put(action, true);
							}
							if (groupedActions != null) {
								for (Map.Entry<String, List<String>> ga : groupedActions.entrySet()) {
									if (actions.containsAll(ga.getValue())) {
										s.put(ga.getKey(), true);
									}
								}
							}
							exist = true;
							break;
						}
					}
					final AtomicBoolean notifyTimeline = new AtomicBoolean(false);
					if (!exist) {
						JsonObject t = new JsonObject().put(shareIdAttr, groupShareId);
						actual.add(t);
						for (String action : actions) {
							t.put(action, true);
						}
						if (groupedActions != null) {
							for (Map.Entry<String, List<String>> ga : groupedActions.entrySet()) {
								if (actions.containsAll(ga.getValue())) {
									t.put(ga.getKey(), true);
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
								notify.put(shareIdAttr, groupShareId);
								res.body().put("notify-timeline", notify);
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

	@Override
	public void share(String userId, String resourceId, JsonObject share, Handler<Either<String, JsonObject>> handler) {
		shareValidation(resourceId, userId, share, res -> {
			if (res.isRight()) {
				final JsonObject query = new JsonObject().put("_id", resourceId);
				final JsonObject update = new JsonObject().put("$set", new JsonObject()
						.put("shared", res.right().getValue().getJsonArray("shared")));
				final JsonObject keys = new JsonObject().put("shared", 1);
				mongo.findAndModify(collection, query, update, null, keys, mongoRes -> {
					if ("ok".equals(mongoRes.body().getString("status"))) {
						JsonArray oldShared = getOrElse(mongoRes.body().getJsonObject("result"), new JsonObject()).getJsonArray("shared");
						JsonArray members = res.right().getValue().getJsonArray("notify-members");
						getNotifyMembers(handler, oldShared, members, (m -> getOrElse(((JsonObject) m).getString("groupId"), ((JsonObject) m).getString("userId"))));
					} else {
						handler.handle(new Either.Left<>(mongoRes.body().getString("message")));
					}
				});
			} else {
				handler.handle(res);
			}
		});
	}

	private void removeShare(String resourceId, final String shareId, List<String> removeActions,
			boolean isGroup, final Handler<Either<String, JsonObject>> handler) {
		final String shareIdAttr = isGroup ? "groupId" : "userId";
		final List<String> actions = findRemoveActions(removeActions);
		QueryBuilder query = QueryBuilder.start("_id").is(resourceId);
		JsonObject keys = new JsonObject().put("shared", 1);
		final JsonObject q = MongoQueryBuilder.build(query);
		mongo.findOne(collection, q, keys, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) &&
						event.body().getJsonObject("result") != null) {
					JsonArray actual = event.body().getJsonObject("result")
							.getJsonArray("shared", new fr.wseduc.webutils.collections.JsonArray());
					JsonArray shared = new fr.wseduc.webutils.collections.JsonArray();
					for (int i = 0; i < actual.size(); i++) {
						JsonObject s = actual.getJsonObject(i);
						String id = s.getString(shareIdAttr);
						if (shareId.equals(id)) {
							if (actions != null) {
								for (String action: actions) {
									s.remove(action);
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

	@Override
	protected void prepareSharedArray(String resourceId, String type, JsonArray shared, String attr, Set<String> actions) {
		JsonObject el = new JsonObject().put(type, attr);
		for (String action : actions) {
			el.put(action, true);
		}
		shared.add(el);
	}

}
