/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.mongodb;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import fr.wseduc.webutils.security.SecuredAction;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.CrudService;
import org.entcore.common.service.VisibilityFilter;
import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.common.share.ShareService;
import org.entcore.common.share.impl.MongoDbShareService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;
import static org.entcore.common.user.UserUtils.getUserInfos;

public abstract class MongoDbControllerHelper extends BaseController {

	private final Map<String, List<String>> groupedActions;
	protected MongoDb mongo;
	private final String sharedCollection;
	private ShareService shareService;
	protected TimelineHelper notification;
	private final String type;
	protected CrudService crudService;

	public MongoDbControllerHelper(String collection) {
		this(collection, null);
	}

	public MongoDbControllerHelper(String collection, Map<String, List<String>> groupedActions) {
		if (collection == null || collection.trim().isEmpty()) {
			log.error("MongoDB collection name must be not empty.");
			throw new IllegalArgumentException(
					"MongoDB collection name must be not empty.");
		}
		this.sharedCollection = collection;
		this.groupedActions = groupedActions;
		this.type = collection.toUpperCase();
	}

	@Override
	public void init(Vertx vertx, Container container, RouteMatcher rm, Map<String, SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		this.mongo = MongoDb.getInstance();
		this.notification = new TimelineHelper(vertx, eb, container);
		this.shareService = new MongoDbShareService(eb, mongo,
				sharedCollection, securedActions, groupedActions);
		this.crudService = new MongoDbCrudService(sharedCollection);
	}

	protected void shareJson(final HttpServerRequest request) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					isOwner(sharedCollection, id, user, new Handler<Boolean>() {
						@Override
						public void handle(Boolean event) {
							if (Boolean.TRUE.equals(event)) {
								shareService.shareInfos(user.getUserId(), id,
										I18n.acceptLanguage(request), defaultResponseHandler(request));
							} else {
								unauthorized(request);
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	protected void shareJsonSubmit(final HttpServerRequest request, final String notifyShareTemplate) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject object) {
				final JsonArray a = object.getArray("actions");
				final String groupId = object.getString("groupId");
				final String userId = object.getString("userId");
				if (a == null || a.size() == 0) {
					badRequest(request);
					return;
				}
				final List<String> actions = new ArrayList<>();
				for (Object o: a) {
					if (o != null && o instanceof String) {
						actions.add(o.toString());
					}
				}
				getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							isOwner(sharedCollection, id, user, new Handler<Boolean>() {
								@Override
								public void handle(Boolean event) {
									if (Boolean.TRUE.equals(event)) {
										Handler<Either<String, JsonObject>> r = new Handler<Either<String, JsonObject>>() {
											@Override
											public void handle(Either<String, JsonObject> event) {
												if (event.isRight()) {
													JsonObject n = event.right().getValue()
															.getObject("notify-timeline");
													if (n != null && notifyShareTemplate != null) {
														notifyShare(request, id, user, new JsonArray().add(n),
																notifyShareTemplate);
													}
													renderJson(request, event.right().getValue());
												} else {
													JsonObject error = new JsonObject()
															.putString("error", event.left().getValue());
													renderJson(request, error, 400);
												}
											}
										};
										if (groupId != null) {
											shareService.groupShare(user.getUserId(), groupId, id, actions, r);
										} else if (userId != null) {
											shareService.userShare(user.getUserId(), userId, id, actions, r);
										} else {
											badRequest(request);
										}
									} else {
										unauthorized(request);
									}
								}
							});
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	protected void removeShare(final HttpServerRequest request) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}

		RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject object) {
				final JsonArray a = object.getArray("actions");
				final String groupId = object.getString("groupId");
				final String userId = object.getString("userId");
				if (a == null || a.size() == 0) {
					badRequest(request);
					return;
				}
				final List<String> actions = new ArrayList<>();
				for (Object o: a) {
					if (o != null && o instanceof String) {
						actions.add(o.toString());
					}
				}
				getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							isOwner(sharedCollection, id, user, new Handler<Boolean>() {
								@Override
								public void handle(Boolean event) {
									if (Boolean.TRUE.equals(event)) {
										if (groupId != null) {
											shareService.removeGroupShare(groupId, id, actions,
													defaultResponseHandler(request));
										} else if (userId != null) {
											shareService.removeUserShare(userId, id, actions,
													defaultResponseHandler(request));
										} else {
											badRequest(request);
										}
									} else {
										unauthorized(request);
									}
								}
							});
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	private void isOwner(String collection, String documentId, UserInfos user,
						 final Handler<Boolean> handler) {
		QueryBuilder query = QueryBuilder.start("_id").is(documentId).put("owner.userId").is(user.getUserId());
		mongo.count(collection, MongoQueryBuilder.build(query), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject res = event.body();
				handler.handle(res != null && "ok".equals(res.getString("status")) && 1 == res.getInteger("count"));
			}
		});
	}

	private void notifyShare(final HttpServerRequest request, final String resource,
			final UserInfos user, JsonArray sharedArray, final String notifyShareTemplate) {
		final List<String> recipients = new ArrayList<>();
		final AtomicInteger remaining = new AtomicInteger(sharedArray.size());
		for (Object j : sharedArray) {
			JsonObject json = (JsonObject) j;
			String userId = json.getString("userId");
			if (userId != null) {
				recipients.add(userId);
				remaining.getAndDecrement();
			} else {
				String groupId = json.getString("groupId");
				if (groupId != null) {
					UserUtils.findUsersInProfilsGroups(groupId, eb, user.getUserId(), false, new Handler<JsonArray>() {
						@Override
						public void handle(JsonArray event) {
							if (event != null) {
								for (Object o : event) {
									if (!(o instanceof JsonObject)) continue;
									JsonObject j = (JsonObject) o;
									String id = j.getString("id");
									log.debug(id);
									recipients.add(id);
								}
							}
							if (remaining.decrementAndGet() < 1) {
								sendNotify(request, resource, user, recipients, notifyShareTemplate);
							}
						}
					});
				}
			}
		}
		if (remaining.get() < 1) {
			sendNotify(request, resource, user, recipients, notifyShareTemplate);
		}
	}

	private void sendNotify(final HttpServerRequest request, final String resource,
			final UserInfos user, final List<String> recipients, final String notifyShareTemplate) {
		final JsonObject params = new JsonObject()
				.putString("uri", container.config().getString("userbook-host") +
						"/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
				.putString("username", user.getUsername())
				.putString("resourceUri", container.config().getString("host", "http://localhost:8011") +
						pathPrefix + "/document/" + resource);
		mongo.findOne(sharedCollection, new JsonObject().putString("_id", resource),
				new JsonObject().putNumber("name", 1), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) && event.body().getObject("result") != null) {
					params.putString("resourceName", event.body().getObject("result").getString("name", ""));
					notification.notifyTimeline(request, user, type, type + "_SHARE",
							recipients, resource, notifyShareTemplate, params);
				} else {
					log.error("Unable to send timeline notification : missing name on resource " + resource);
				}
			}
		});
	}

	protected void create(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject object) {
							crudService.create(object, user, notEmptyResponseHandler(request));
						}
					});
				} else {
					log.debug("User not found in session.");
					Renders.unauthorized(request);
				}
			}
		});
	}

	protected void retrieve(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				String id = request.params().get("id");
				crudService.retrieve(id, user, notEmptyResponseHandler(request));
			}
		});
	}

	protected void update(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject object) {
							String id = request.params().get("id");
							crudService.update(id, object, user, notEmptyResponseHandler(request));
						}
					});
				} else {
					log.debug("User not found in session.");
					Renders.unauthorized(request);
				}
			}
		});
	}

	protected void delete(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					String id = request.params().get("id");
					crudService.delete(id, user, notEmptyResponseHandler(request));
				} else {
					log.debug("User not found in session.");
					Renders.unauthorized(request);
				}
			}
		});
	}

	protected void list(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				String filter = request.params().get("filter");
				VisibilityFilter v = VisibilityFilter.ALL;
				if (filter != null) {
					try {
						v = VisibilityFilter.valueOf(filter.toUpperCase());
					} catch (IllegalArgumentException | NullPointerException e) {
						v = VisibilityFilter.ALL;
						if (log.isDebugEnabled()) {
							log.debug("Invalid filter " + filter);
						}
					}
				}
				crudService.list(v, user, arrayResponseHandler(request));
			}
		});
	}

}
