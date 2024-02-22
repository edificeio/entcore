/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.common.controller;

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray;
import org.apache.commons.lang3.NotImplementedException;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.CrudService;
import org.entcore.common.service.VisibilityFilter;
import org.entcore.common.share.ShareModel;
import org.entcore.common.share.ShareNormalizer;
import org.entcore.common.share.ShareService;
import org.entcore.common.share.Shareable;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.user.UserUtils.getUserInfos;

public abstract class ControllerHelper extends BaseController implements Shareable {

	protected ShareService shareService;
	protected TimelineHelper notification;
	protected CrudService crudService;

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		this.notification = new TimelineHelper(vertx, eb, config);
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
											I18n.acceptLanguage(request), request.params().get("search"), addNormalizedRights(defaultResponseHandler(request)));
								} else {
									unauthorized(request);
								}
							}
						});
					} else {
						shareService.shareInfos(user.getUserId(), id,
								I18n.acceptLanguage(request), request.params().get("search"), addNormalizedRights(defaultResponseHandler(request)));
					}
				} else {
					unauthorized(request);
				}
			}
		});
	}

	/**
	 * Add normalized rights to JsonObject
	 *
	 * @param json a JsonObject containings "shared" or detailed shared {users:{checked:{}}, groups:{checked:{}}}
	 * @return the same JsonArray or JsonObject with the new field "rights" added to each object
	 */
	protected JsonObject addNormalizedRights(final JsonObject json) {
		if(!shouldNormalizedRights()){
			return json;
		}
		return new ShareNormalizer(this.securedActions).addNormalizedRights(json, jsonToOwnerId());
	}

	/**
	 * Add normalized rights to JsonArray
	 *
	 * @param jsonArray an array of JsonObject
	 * @return the same JsonArray or JsonObject with the new field "rights" added to each object
	 */
	protected JsonArray addNormalizedRights(final JsonArray jsonArray){
		if(!shouldNormalizedRights()){
			return jsonArray;
		}
		return new ShareNormalizer(this.securedActions).addNormalizedRights(jsonArray, jsonToOwnerId());
	}

	/**
	 * Add normalized rights to JsonArray/JsonObject
	 *
	 * @param handler Handler<Either<String,JsonArray | JsonObject>> OR Handler<AsyncResult<JsonArray | JsonObject>>
	 * @return the same JsonArray or JsonObject with the new field "rights" added to each object
	 */
	protected <T> Handler<T>addNormalizedRights(final Handler<T> handler){
		if(!shouldNormalizedRights()){
			return handler;
		}
		return new ShareNormalizer(this.securedActions).addNormalizedRights(handler, jsonToOwnerId());
	}
	/**
	 * Add normalized rights to JsonArray/JsonObject
	 *
	 * @param either Either of a JsonArray OR JsonObject
	 * @return the same JsonArray or JsonObject with the new field "rights" added to each object
	 */
	protected <T> Either<String, T> addNormalizedRights(final Either<String, T> either){
		if(!shouldNormalizedRights()){
			return either;
		}
		return new ShareNormalizer(this.securedActions).addNormalizedRights(either, jsonToOwnerId());
	}

	/**
	 * Add normalized rights to JsonArray/JsonObject
	 *
	 * @param asyncResult AsyncResult of a JsonArray OR JsonObject
	 * @return the same JsonArray or JsonObject with the new field "rights" added to each object
	 */
	protected <T> AsyncResult<T> addNormalizedRights(final AsyncResult<T> asyncResult){
		if(!shouldNormalizedRights()){
			return asyncResult;
		}
		return new ShareNormalizer(this.securedActions).addNormalizedRights(asyncResult, jsonToOwnerId());
	}

	/**
	 * Wether to enable or not normalized rights. If enabled, the controller will add a field "rights" containing normalized rights into json responses
	 *
	 * @return true if normalized rights is enabled
	 */
	protected boolean shouldNormalizedRights(){
		return false;
	}

	/**
	 * This method is used by ShareNormalizer to extract the ownerId of a JsonObject.
	 * Normalized rights contains the special rights: "creator:USER_ID"
	 *
	 * <b>If the controller enable normalized rights, this method should be overriden!</b>
	 *
	 * @return a Function that take the jsonObject model and return the ownerId as Optional. If the ownerId could not be extracted it returns Optional.empty
	 */
	protected Function<JsonObject, Optional<String>> jsonToOwnerId(){
		throw new NotImplementedException("implement.this.function");
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
		request.setExpectMultipart(true);
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
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
									.getJsonObject("notify-timeline");
							if (n != null && notificationName != null) {
								notifyShare(request, eb, id, user, new fr.wseduc.webutils.collections.JsonArray().add(n),
										notificationName, params, resourceNameAttribute);
							}
							renderJson(request, addNormalizedRights(event.right().getValue()));
						} else {
							JsonObject error = new JsonObject()
									.put("error", event.left().getValue());
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

		request.setExpectMultipart(true);
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
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

	@Override
	public void sendNotify(final HttpServerRequest request, final String resource,
			final UserInfos user, final List<String> recipients, final String notificationName,
			JsonObject p, final String resourceNameAttribute) {
		if (p == null) {
			p = new JsonObject()
				.put("uri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
				.put("username", user.getUsername())
				.put("resourceUri", pathPrefix + "/" + resource);
		}
		final JsonObject params = p;
		crudService.retrieve(resource, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					String attr = (resourceNameAttribute != null && !resourceNameAttribute.trim().isEmpty()) ?
							resourceNameAttribute : "name";
					params.put("resourceName", r.right().getValue().getString(attr, ""));
					final Object preview = params.remove("preview");
					if (preview instanceof JsonObject) {
						notification.notifyTimeline(request, notificationName, user, recipients, null, null, params, false, (JsonObject) preview);
					} else {
						notification.notifyTimeline(request, notificationName, user, recipients, params);
					}
				} else {
					log.error("Unable to send timeline notification : missing name on resource " + resource);
				}
			}
		});
	}

	protected void shareResource(final HttpServerRequest request, final String notificationName,
			final boolean checkIsOwner, final JsonObject params, final String resourceNameAttribute) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, user -> {
			if (user != null) {
				if (checkIsOwner) {
					crudService.isOwner(id, user, event -> {
						if (Boolean.TRUE.equals(event)) {
							doShare(request, eb, id, user, notificationName, params, resourceNameAttribute);
						} else {
							unauthorized(request, "not.owner");
						}
					});
				} else {
					doShare(request, eb, id, user, notificationName, params, resourceNameAttribute);
				}
			} else {
				unauthorized(request, "invalid.user");
			}
		});
	}

	protected void create(final HttpServerRequest request) {
		create(request, e-> {});
	}

	protected void create(final HttpServerRequest request, final Handler<AsyncResult<JsonObject>> handler) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject object) {
							crudService.create(object, user, r -> {
								notEmptyResponseHandler(request).handle(addNormalizedRights(r));
								if (r.isLeft()) {
									handler.handle(new DefaultAsyncResult<>(new Exception(r.left().getValue())));
								} else {
									handler.handle(new DefaultAsyncResult<>(r.right().getValue()));
								}
							});
						}
					});
				} else {
					log.debug("User not found in session.");
					Renders.unauthorized(request);
					handler.handle(new DefaultAsyncResult<>(new Exception("unauthorized")));
				}
			}
		});
	}

	protected void retrieve(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				String id = request.params().get("id");
				crudService.retrieve(id, user, addNormalizedRights(notEmptyResponseHandler(request)));
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
							crudService.update(id, object, user, addNormalizedRights(notEmptyResponseHandler(request)));
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
				crudService.list(v, user, addNormalizedRights(arrayResponseHandler(request)));
			}
		});
	}

	public void setCrudService(CrudService crudService) {
		this.crudService = crudService;
	}

	public void setShareService(ShareService shareService) {
		this.shareService = shareService;
	}

	@Override
	public ShareService getShareService() {
		return shareService;
	}

	@Override
	public TimelineHelper getNotification() {
		return notification;
	}

}
