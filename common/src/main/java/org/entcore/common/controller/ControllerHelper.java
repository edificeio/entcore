/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.common.controller;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import fr.wseduc.webutils.security.SecuredAction;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.CrudService;
import org.entcore.common.service.VisibilityFilter;
import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.user.UserUtils.getUserInfos;

public abstract class ControllerHelper extends BaseController {

	protected ShareService shareService;
	protected TimelineHelper notification;
	protected CrudService crudService;

	@Override
	public void init(Vertx vertx, Container container, RouteMatcher rm, Map<String, SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		this.notification = new TimelineHelper(vertx, eb, container);
	}

	protected void shareJson(final HttpServerRequest request) {
		shareJson(request, true);
	}

	protected void shareJson(final HttpServerRequest request, final boolean checkIsOwner) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					if (checkIsOwner) {
						crudService.isOwner(id, user, new Handler<Boolean>() {
							@Override
							public void handle(Boolean event) {
								if (Boolean.TRUE.equals(event)) {
									shareService.shareInfos(user.getUserId(), id,
											I18n.acceptLanguage(request), request.params().get("search"), defaultResponseHandler(request));
								} else {
									unauthorized(request);
								}
							}
						});
					} else {
						shareService.shareInfos(user.getUserId(), id,
								I18n.acceptLanguage(request), request.params().get("search"), defaultResponseHandler(request));
					}
				} else {
					unauthorized(request);
				}
			}
		});
	}

	protected void shareJsonSubmit(final HttpServerRequest request, final String notificationName) {
		shareJsonSubmit(request, notificationName, true, null, null);
	}

	protected void shareJsonSubmit(final HttpServerRequest request, final String notificationName,
			final boolean checkIsOwner) {
		shareJsonSubmit(request, notificationName, checkIsOwner, null, null);
	}

	protected void shareJsonSubmit(final HttpServerRequest request, final String notificationName,
			final boolean checkIsOwner, final JsonObject params, final String resourceNameAttribute) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final List<String> actions = request.formAttributes().getAll("actions");
				final String groupId = request.formAttributes().get("groupId");
				final String userId = request.formAttributes().get("userId");
				if (actions == null || actions.isEmpty()) {
					badRequest(request);
					return;
				}
				getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							if (checkIsOwner) {
								crudService.isOwner(id, user, new Handler<Boolean>() {
									@Override
									public void handle(Boolean event) {
										if (Boolean.TRUE.equals(event)) {
											response(user, groupId, actions, userId);
										} else {
											unauthorized(request);
										}
									}
								});
							} else {
								response(user, groupId, actions, userId);
							}
						} else {
							unauthorized(request);
						}
					}
				});
			}

			private void response(final UserInfos user, String groupId, List<String> actions, String userId) {
				Handler<Either<String, JsonObject>> r = new Handler<Either<String, JsonObject>>() {
					@Override
					public void handle(Either<String, JsonObject> event) {
						if (event.isRight()) {
							JsonObject n = event.right().getValue()
									.getObject("notify-timeline");
							if (n != null && notificationName != null) {
								notifyShare(request, id, user, new JsonArray().add(n),
										notificationName, params, resourceNameAttribute);
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
			}
		});
	}

	protected void removeShare(final HttpServerRequest request) {
		removeShare(request, true);
	}

	protected void removeShare(final HttpServerRequest request, final boolean checkIsOwner) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}

		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final List<String> actions = request.formAttributes().getAll("actions");
				final String groupId = request.formAttributes().get("groupId");
				final String userId = request.formAttributes().get("userId");
				if (checkIsOwner) {
					getUserInfos(eb, request, new Handler<UserInfos>() {
						@Override
						public void handle(final UserInfos user) {
							if (user != null) {
								crudService.isOwner(id, user, new Handler<Boolean>() {
									@Override
									public void handle(Boolean event) {
										if (Boolean.TRUE.equals(event)) {
											response(groupId, actions, userId);
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
				} else {
					response(groupId, actions, userId);
				}
			}

			private void response(String groupId, List<String> actions, String userId) {
				if (groupId != null) {
					shareService.removeGroupShare(groupId, id, actions,
							defaultResponseHandler(request));
				} else if (userId != null) {
					shareService.removeUserShare(userId, id, actions,
							defaultResponseHandler(request));
				} else {
					badRequest(request);
				}
			}

		});
	}

	private void notifyShare(final HttpServerRequest request, final String resource,
			final UserInfos user, JsonArray sharedArray, final String notificationName,
			final JsonObject params, final String resourceNameAttribute) {
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
								sendNotify(request, resource, user, recipients, notificationName,
										params, resourceNameAttribute);
							}
						}
					});
				}
			}
		}
		if (remaining.get() < 1) {
			sendNotify(request, resource, user, recipients, notificationName, params, resourceNameAttribute);
		}
	}

	private void sendNotify(final HttpServerRequest request, final String resource,
			final UserInfos user, final List<String> recipients, final String notificationName,
			JsonObject p, final String resourceNameAttribute) {
		if (p == null) {
			p = new JsonObject()
				.putString("uri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
				.putString("username", user.getUsername())
				.putString("resourceUri", pathPrefix + "/" + resource);
		}
		final JsonObject params = p;
		crudService.retrieve(resource, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					String attr = (resourceNameAttribute != null && !resourceNameAttribute.trim().isEmpty()) ?
							resourceNameAttribute : "name";
					params.putString("resourceName", r.right().getValue().getString(attr, ""));
					notification.notifyTimeline(request, notificationName, user, recipients, params);
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

	public void setCrudService(CrudService crudService) {
		this.crudService = crudService;
	}

	public void setShareService(ShareService shareService) {
		this.shareService = shareService;
	}

}
