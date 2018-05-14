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

package org.entcore.workspace.service;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.user.UserUtils.getUserInfos;
import static org.entcore.workspace.dao.DocumentDao.DOCUMENTS_COLLECTION;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.request.ActionsUtils;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.share.ShareService;
import org.entcore.common.share.impl.MongoDbShareService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.workspace.Workspace;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.dao.GenericDao;
import org.entcore.workspace.service.impl.DefaultFolderService;
import org.entcore.common.storage.Storage;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.mongodb.QueryBuilder;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.ETag;
import fr.wseduc.webutils.request.RequestUtils;
import org.vertx.java.core.http.RouteMatcher;

public class WorkspaceService extends BaseController {

	public static final String WORKSPACE_NAME = "WORKSPACE";
	public static final String DOCUMENT_REVISION_COLLECTION = "documentsRevisions";
	private static final JsonObject PROPERTIES_KEYS = new JsonObject().put("name", 1).put("alt", 1).put("legend", 1);
	private String imageResizerAddress;
	private MongoDb mongo;
	private DocumentDao documentDao;
	private TimelineHelper notification;
	private ShareService shareService;
	private FolderService folderService;
	private QuotaService quotaService;
	private int threshold;
	private EventStore eventStore;
	private enum WokspaceEvent { ACCESS, GET_RESOURCE }
	private Storage storage;

	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		mongo = MongoDb.getInstance();
		String node = (String) vertx.sharedData().getLocalMap("server").get("node");
		if (node == null) {
			node = "";
		}
		imageResizerAddress = node + config.getString("image-resizer-address", "wse.image.resizer");
		documentDao = new DocumentDao(mongo);
		notification = new TimelineHelper(vertx, eb, config);
		this.shareService = new MongoDbShareService(eb, mongo, "documents", securedActions, null);
		this.folderService = new DefaultFolderService(mongo, storage);
		this.threshold = config.getInteger("alertStorage", 80);
		eventStore = EventStoreFactory.getFactory().getEventStore(Workspace.class.getSimpleName());
		post("/documents/copy/:ids", "copyDocuments");
		put("/documents/move/:ids", "moveDocuments");
	}

	@Get("/workspace")
	@SecuredAction("workspace.view")
	public void view(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					if (user.getAttribute("storage") != null && user.getAttribute("quota") != null) {
						renderView(request);
						eventStore.createAndStoreEvent(WokspaceEvent.ACCESS.name(), request);
						return;
					}
					quotaService.quotaAndUsage(user.getUserId(), new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> r) {
							if (r.isRight()) {
								JsonObject j = r.right().getValue();
								for (String attr : j.fieldNames()) {
									UserUtils.addSessionAttribute(eb, user.getUserId(), attr, j.getLong(attr), null);
								}
							}
							renderView(request);
							eventStore.createAndStoreEvent(WokspaceEvent.ACCESS.name(), request);
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Get("/share/json/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void shareJson(final HttpServerRequest request) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					shareService.shareInfos(user.getUserId(), id, I18n.acceptLanguage(request),
							request.params().get("search"), defaultResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void shareFileAction(final HttpServerRequest request, final String id, final UserInfos user, final List<String> actions, final String groupId, final String userId, final boolean remove){
		Handler<Either<String, JsonObject>> r = new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					JsonObject n = event.right().getValue().getJsonObject("notify-timeline");
					if (n != null) {
						notifyShare(request, id, user, new fr.wseduc.webutils.collections.JsonArray().add(n));
					}
					renderJson(request, event.right().getValue());
				} else {
					JsonObject error = new JsonObject()
							.put("error", event.left().getValue());
					renderJson(request, error, 400);
				}
			}
		};

		if (groupId != null) {
			if(remove)
				shareService.removeGroupShare(groupId, id, actions, defaultResponseHandler(request));
			else
				shareService.groupShare(user.getUserId(), groupId, id, actions, r);
		} else if (userId != null) {
			if(remove)
				shareService.removeUserShare(userId, id, actions, defaultResponseHandler(request));
			else
				shareService.userShare(user.getUserId(), userId, id, actions, r);
		} else {
			badRequest(request);
		}
	}

	private void shareFolderAction(final HttpServerRequest request, final String id, final UserInfos user, final List<String> actions, final String groupId, final String userId, final boolean remove){

		Handler<Either<String, JsonObject>> subHandler = new Handler<Either<String, JsonObject>>(){
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {

					final int number = event.right().getValue().getInteger("number");
					final int errorsNb = event.right().getValue().getInteger("number-errors");

					Handler<Either<String, JsonObject>> finalHandler = new Handler<Either<String, JsonObject>>() {
						public void handle(Either<String, JsonObject> event) {
							if (event.isRight()) {
								JsonObject n = event.right().getValue().getJsonObject("notify-timeline");
								if (n != null) {
									notifyShare(request, id, user, new fr.wseduc.webutils.collections.JsonArray().add(n), true);
								} else {
									n = new JsonObject();
								}
								n.put("number", number+1).put("number-errors", errorsNb);
								renderJson(request, n);
							} else {
								JsonObject error = new JsonObject().put("error", event.left().getValue());
								renderJson(request, error, 400);
							}
						}
					};

					if (groupId != null) {
						if(remove)
							shareService.removeGroupShare(groupId, id, actions, finalHandler);
						else
							shareService.groupShare(user.getUserId(), groupId, id, actions, finalHandler);
					} else if (userId != null) {
						if(remove)
							shareService.removeUserShare(userId, id, actions, finalHandler);
						else
							shareService.userShare(user.getUserId(), userId, id, actions, finalHandler);
					} else {
						badRequest(request);
					}

				} else {
					JsonObject error = new JsonObject().put("error", event.left().getValue());
					renderJson(request, error, 400);
				}
			}
		};

		folderService.shareFolderAction(id, user, actions, groupId, userId, shareService, remove, subHandler);

	}

	@Put("/share/json/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void shareJsonSubmit(final HttpServerRequest request) {
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
						mongo.findOne(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject().put("_id", id), new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if ("ok".equals(event.body().getString("status")) && event.body().getJsonObject("result") != null) {
									final boolean isFolder = !event.body().getJsonObject("result").containsKey("file");
									if(isFolder)
										shareFolderAction(request, id, user, actions, groupId, userId, false);
									else
										shareFileAction(request, id, user, actions, groupId, userId, false);
								} else {
									unauthorized(request);
								}
							}
						});
					}
				});
			}
		});
	}

	@Put("/share/remove/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void removeShare(final HttpServerRequest request) {
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
				getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							mongo.findOne(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject().put("_id", id), new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> event) {
									if ("ok".equals(event.body().getString("status")) && event.body().getJsonObject("result") != null) {
										final boolean isFolder = !event.body().getJsonObject("result").containsKey("file");
										if (isFolder)
											shareFolderAction(request, id, user, actions, groupId, userId, true);
										else
											shareFileAction(request, id, user, actions, groupId, userId, true);
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

	@Put("/share/resource/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void shareResource(final HttpServerRequest request) {
		getUserInfos(eb, request, user -> {
			if (user != null) {
				RequestUtils.bodyToJson(request, share -> {
					shareService.share(user.getUserId(), request.params().get("id"), share, r ->  {
						if (r.isRight()) {
							renderJson(request, r.right().getValue());
						} else {
							JsonObject error = new JsonObject().put("error", r.left().getValue());
							renderJson(request, error, 400);
						}
					});
				});
			} else {
				badRequest(request, "invalid.user");
			}
		});
	}

	private void notifyShare(final HttpServerRequest request, final String resource, final UserInfos user, JsonArray sharedArray){
		notifyShare(request, resource, user, sharedArray, false);
	}
	private void notifyShare(final HttpServerRequest request, final String resource, final UserInfos user, JsonArray sharedArray, final boolean isFolder) {
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
								for(Object o: event) {
									if (!(o instanceof JsonObject)) continue;
									JsonObject j = (JsonObject) o;
									String id = j.getString("id");
									log.debug(id);
									recipients.add(id);
								}
							}
							if (remaining.decrementAndGet() < 1) {
								sendNotify(request, resource, user, recipients, isFolder);
							}
						}
					});
				}
			}
		}
		if (remaining.get() < 1) {
			sendNotify(request, resource, user, recipients, isFolder);
		}
	}

	private void sendNotify(final HttpServerRequest request, final String resource, final UserInfos user, final List<String> recipients, final boolean isFolder) {
		final JsonObject params = new JsonObject()
				.put("uri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
				.put("username", user.getUsername())
				.put("appPrefix", pathPrefix + "/workspace")
				.put("doc", "share");

		final JsonObject pushNotif = new JsonObject();
		final String i18nPushNotifBody;

		if (isFolder) {
			params.put("resourceUri", pathPrefix + "/workspace#/shared/folder/" + resource);
			pushNotif.put("title", "push.notif.folder.share");
			i18nPushNotifBody = user.getUsername() + " " + I18n.getInstance().translate("workspace.shared.folder",
					getHost(request), I18n.acceptLanguage(request)) + " : ";
		} else {
			params.put("resourceUri", pathPrefix + "/document/" + resource);
			pushNotif.put("title", "push.notif.file.share");
			i18nPushNotifBody = user.getUsername() + " " + I18n.getInstance().translate("workspace.shared.document",
					getHost(request), I18n.acceptLanguage(request)) + " : ";
		}

		final String notificationName = WORKSPACE_NAME.toLowerCase() + "." + (isFolder ? "share-folder" : "share");

		mongo.findOne(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject().put("_id", resource),
				new JsonObject().put("name", 1), new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if ("ok".equals(event.body().getString("status")) && event.body().getJsonObject("result") != null) {
							String resourceName = event.body().getJsonObject("result").getString("name", "");
							params.put("resourceName", resourceName);
							params.put("pushNotif", pushNotif.put("body", i18nPushNotifBody + resourceName));
							notification.notifyTimeline(request, notificationName, user, recipients, resource, params);
						} else {
							log.error("Unable to send timeline notification : missing name on resource " + resource);
						}
					}
				});
	}

	@Post("/document")
	@SecuredAction("workspace.document.add")
	public void addDocument(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(final UserInfos userInfos) {
				if (userInfos != null) {
					final JsonObject doc = new JsonObject();
					String now = MongoDb.formatDate(new Date());
					doc.put("created", now);
					doc.put("modified", now);
					doc.put("owner", userInfos.getUserId());
					doc.put("ownerName", userInfos.getUsername());
					String application = request.params().get("application");
					String protectedContent = request.params().get("protected");
					String publicContent = request.params().get("public");
					if (application != null && !application.trim().isEmpty() &&
							"true".equals(protectedContent)) {
						doc.put("protected", true);
					} else if (application != null && !application.trim().isEmpty() &&
							"true".equals(publicContent)) {
						doc.put("public", true);
					}
					request.pause();
					emptySize(userInfos, new Handler<Long>() {
						@Override
						public void handle(Long emptySize) {
							request.resume();
							add(request, DocumentDao.DOCUMENTS_COLLECTION, doc, emptySize);
						}
					});
				} else {
					request.response().setStatusCode(401).end();
				}
			}
		});
	}

	private void emptySize(final UserInfos userInfos, final Handler<Long> emptySizeHandler) {
		try {
			long quota = Long.valueOf(userInfos.getAttribute("quota").toString());
			long storage = Long.valueOf(userInfos.getAttribute("storage").toString());
			emptySizeHandler.handle(quota - storage);
		} catch (Exception e) {
			quotaService.quotaAndUsage(userInfos.getUserId(), new Handler<Either<String, JsonObject>>() {
				@Override
				public void handle(Either<String, JsonObject> r) {
					if (r.isRight()) {
						JsonObject j = r.right().getValue();
						if (j != null) {
							long quota = j.getLong("quota", 0l);
							long storage = j.getLong("storage", 0l);
							for (String attr : j.fieldNames()) {
								UserUtils.addSessionAttribute(eb, userInfos.getUserId(), attr, j.getLong(attr), null);
							}
							emptySizeHandler.handle(quota - storage);
						}
					}
				}
			});
		}
	}

	private void emptySize(final String userId, final Handler<Long> emptySizeHandler) {
		quotaService.quotaAndUsage(userId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					JsonObject j = r.right().getValue();
					if (j != null) {
						long quota = j.getLong("quota", 0l);
						long storage = j.getLong("storage", 0l);
						for (String attr : j.fieldNames()) {
							UserUtils.addSessionAttribute(eb, userId, attr, j.getLong(attr), null);
						}
						emptySizeHandler.handle(quota - storage);
					}
				}
			}
		});
	}

	private void add(final HttpServerRequest request, final String mongoCollection,
			final JsonObject doc, long allowedSize) {
		storage.writeUploadFile(request, allowedSize, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject uploaded) {
				if ("ok".equals(uploaded.getString("status"))) {
					compressImage(uploaded, request.params().get("quality"), new Handler<Integer>() {
						@Override
						public void handle(Integer size) {
							JsonObject meta = uploaded.getJsonObject("metadata");
							if (size != null && meta != null) {
								meta.put("size", size);
							}
							addAfterUpload(uploaded, doc, request
											.params().get("name"), request.params().get("application"),
									request.params().getAll("thumbnail"),
									mongoCollection, new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> res) {
											if ("ok".equals(res.body().getString("status"))) {
												renderJson(request, res.body(), 201);
											} else {
												renderError(request, res.body());
											}
										}
									});
						}
					});
				} else {
					badRequest(request, uploaded.getString("message"));
				}
			}
		});
	}

	private void addAfterUpload(final JsonObject uploaded, final JsonObject doc, String name, String application,
			final List<String> thumbs, final String mongoCollection,
			final Handler<Message<JsonObject>> handler) {
		doc.put("name", getOrElse(name, uploaded.getJsonObject("metadata")
				.getString("filename"), false));
		doc.put("metadata", uploaded.getJsonObject("metadata"));
		doc.put("file", uploaded.getString("_id"));
		doc.put("application", getOrElse(application, WORKSPACE_NAME)); // TODO check if application name is valid
		log.debug(doc.encodePrettily());
		mongo.save(mongoCollection, doc, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(final Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					incrementStorage(doc);
					createRevision(res.body().getString("_id"), uploaded.getString("_id"), doc.getString("name"), doc.getString("owner"), doc.getString("owner"), doc.getString("ownerName"), doc.getJsonObject("metadata"));
					createThumbnailIfNeeded(mongoCollection, uploaded,
							res.body().getString("_id"), null, thumbs, new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> event) {
									if (handler != null) {
										handler.handle(res);
									}
								}
							});
				} else if (handler != null) {
					handler.handle(res);
				}
			}
		});
	}

	private void incrementStorage(JsonObject added) {
		updateStorage(new fr.wseduc.webutils.collections.JsonArray().add(added), null);
	}

	private void decrementStorage(JsonObject removed) {
		updateStorage(null, new fr.wseduc.webutils.collections.JsonArray().add(removed));
	}

	private void decrementStorage(JsonObject removed, Handler<Either<String, JsonObject>> handler) {
		updateStorage(null, new fr.wseduc.webutils.collections.JsonArray().add(removed), handler);
	}

	private void incrementStorage(JsonArray added) {
		updateStorage(added, null);
	}

	private void decrementStorage(JsonArray removed) {
		updateStorage(null, removed);
	}

	private void updateStorage(JsonObject added, JsonObject removed) {
		updateStorage(new fr.wseduc.webutils.collections.JsonArray().add(added), new JsonArray().add(removed));
	}

	private void updateStorage(JsonArray addeds, JsonArray removeds) {
		updateStorage(addeds, removeds, null);
	}

	private void updateStorage(JsonArray addeds, JsonArray removeds, final Handler<Either<String, JsonObject>> handler) {
		Map<String, Long> sizes = new HashMap<>();
		if (addeds != null) {
			for (Object o : addeds) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject added = (JsonObject) o;
				Long size = added.getJsonObject("metadata", new JsonObject()).getLong("size", 0l);
				String userId = (added.containsKey("to")) ? added.getString("to") : added.getString("owner");
				if (userId == null) {
					log.info("UserId is null when update storage size");
					log.info(added.encode());
					continue;
				}
				Long old = sizes.get(userId);
				if (old != null) {
					size += old;
				}
				sizes.put(userId, size);
			}
		}

		if (removeds != null) {
			for (Object o : removeds) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject removed = (JsonObject) o;
				Long size = removed.getJsonObject("metadata", new JsonObject()).getLong("size", 0l);
				String userId = (removed.containsKey("to")) ? removed.getString("to") : removed.getString("owner");
				if (userId == null) {
					log.info("UserId is null when update storage size");
					log.info(removed.encode());
					continue;
				}
				Long old = sizes.get(userId);
				if (old != null) {
					old -= size;
				} else {
					old = -1l * size;
				}
				sizes.put(userId, old);
			}
		}

		for (final Map.Entry<String, Long> e : sizes.entrySet()) {
			quotaService.incrementStorage(e.getKey(), e.getValue(), threshold, new Handler<Either<String, JsonObject>>() {
				@Override
				public void handle(Either<String, JsonObject> r) {
					if (r.isRight()) {
						JsonObject j = r.right().getValue();
						UserUtils.addSessionAttribute(eb, e.getKey(), "storage", j.getLong("storage"), null);
						if (j.getBoolean("notify", false)) {
							notifyEmptySpaceIsSmall(e.getKey());
						}
					} else {
						log.error(r.left().getValue());
					}
					if (handler != null) {
						handler.handle(r);
					}
				}
			});
		}
	}

	private void notifyEmptySpaceIsSmall(String userId) {
		List<String> recipients = new ArrayList<>();
		recipients.add(userId);
		notification.notifyTimeline(new JsonHttpServerRequest(new JsonObject()), WORKSPACE_NAME.toLowerCase() + ".storage",
				null, recipients, null, new JsonObject());
	}

	@Post("/folder")
	@SecuredAction("workspace.folder.add")
	public void addFolder(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
				final String name = replaceUnderscore(request.formAttributes().get("name"));
				final String path = request.formAttributes().get("path");
				if (name == null || name.trim().isEmpty()) {
					badRequest(request);
					return;
				}
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos userInfos) {
						if (userInfos != null) {
							folderService.create(name, path, request.params().get("application"), userInfos,
									defaultResponseHandler(request, 201));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	@Put("/folder/copy/:id")
	@SecuredAction(value = "workspace.folder.copy", type = ActionType.AUTHENTICATED)
	public void copyFolder(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
				final String id = request.params().get("id");
				final String path = request.formAttributes().get("path");
				final String name = replaceUnderscore(request.formAttributes().get("name"));
				if (id == null || id.trim().isEmpty()) {
					badRequest(request);
					return;
				}
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos userInfos) {
						if (userInfos != null) {
							emptySize(userInfos, new Handler<Long>() {
								@Override
								public void handle(Long emptySize) {
									folderService.copy(id, name, path, userInfos, emptySize,
											new Handler<Either<String, JsonArray>>() {
												@Override
												public void handle(Either<String, JsonArray> r) {
													if (r.isRight()) {
														incrementStorage(r.right().getValue());
														renderJson(request, new JsonObject()
																.put("number", r.right().getValue().size()));
													} else {
														badRequest(request, r.left().getValue());
													}
												}
											});

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

	@Put("/folder/move/:id")
	@SecuredAction(value = "workspace.folder.move", type = ActionType.AUTHENTICATED)
	public void moveFolder(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
				final String id = request.params().get("id");
				String p;
				try {
					p = getOrElse(request.formAttributes().get("path"), "");
				} catch (IllegalStateException e) {
					p = "";
				}
				final String path = p;
				if (id == null || id.trim().isEmpty()) {
					badRequest(request);
					return;
				}
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos userInfos) {
						if (userInfos != null) {
							folderService.move(id, path, userInfos, defaultResponseHandler(request));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	@Put("/folder/trash/:id")
	@SecuredAction(value = "workspace.folder.trash", type = ActionType.AUTHENTICATED)
	public void moveTrashFolder(final HttpServerRequest request) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos userInfos) {
				if (userInfos != null) {
					folderService.trash(id, userInfos, defaultResponseHandler(request));
				} else {
					unauthorized(request);
				}
		   }
		});
	}

	@Put("/folder/restore/:id")
	@SecuredAction(value = "workspace.folder.trash", type = ActionType.AUTHENTICATED)
	public void restoreFolder(final HttpServerRequest request) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos userInfos) {
				if (userInfos != null) {
					folderService.restore(id, userInfos, defaultResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Delete("/folder/:id")
	@SecuredAction(value = "workspace.folder.delete", type = ActionType.AUTHENTICATED)
	public void deleteFolder(final HttpServerRequest request) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos userInfos) {
				if (userInfos != null) {
					folderService.delete(id, userInfos, new Handler<Either<String, JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> r) {
							if (r.isRight()) {
								//Delete revisions for each sub-document
								for(Object obj : r.right().getValue()){
									JsonObject item = (JsonObject) obj;
									if(item.containsKey("file"))
										deleteAllRevisions(item.getString("_id"), new fr.wseduc.webutils.collections.JsonArray().add(item.getString("file")));
								}
								//Decrement storage
								decrementStorage(r.right().getValue());
								renderJson(request, new JsonObject()
										.put("number", r.right().getValue().size()), 204);
							} else {
								badRequest(request, r.left().getValue());
							}
						}
					});
				} else {
					unauthorized(request);
				}
	  		 }
		});
	}

	@Get("/folders/list")
	@SecuredAction(value = "workspace.folders.list", type = ActionType.AUTHENTICATED)
	public void folders(final HttpServerRequest request) {
		final String path = request.params().get("path");
		final boolean hierarchical = request.params().get("hierarchical") != null;
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos userInfos) {
				if (userInfos != null) {
					String filter = request.params().get("filter");
					folderService.list(path, userInfos, hierarchical, filter, arrayResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void createThumbnailIfNeeded(final String collection, final JsonObject srcFile,
			final String documentId, final JsonObject oldThumbnail, final List<String> thumbs,
			final Handler<Message<JsonObject>> callback) {
		if (documentId != null && thumbs != null && !documentId.trim().isEmpty() && !thumbs.isEmpty() &&
				srcFile != null && isImage(srcFile) && srcFile.getString("_id") != null) {
			createThumbnails(thumbs, srcFile, collection, documentId, callback);
		} else {
			callback.handle(null);
		}
		if (oldThumbnail != null) {
			for (final String attr: oldThumbnail.fieldNames()) {
				storage.removeFile(oldThumbnail.getString(attr), new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject event) {
						if (!"ok".equals(event.getString("status"))) {
							log.error("Error removing thumbnail " + oldThumbnail.getString(attr) + " : "
									+ event.getString("message"));
						}
					}
				});
			}
		}
	}

	private void createThumbnails(List<String> thumbs, JsonObject srcFile, final String collection,
			final String documentId, final Handler<Message<JsonObject>> callback) {
		Pattern size = Pattern.compile("([0-9]+)x([0-9]+)");
		JsonArray outputs = new fr.wseduc.webutils.collections.JsonArray();
		for (String thumb: thumbs) {
			Matcher m = size.matcher(thumb);
			if (m.matches()) {
				try {
					int width = Integer.parseInt(m.group(1));
					int height = Integer.parseInt(m.group(2));
					if (width == 0 && height == 0) continue;
					JsonObject j = new JsonObject().put("dest",
							storage.getProtocol() + "://" + storage.getBucket());
					if (width != 0) {
						j.put("width", width);
					}
					if (height != 0) {
						j.put("height", height);
					}
					outputs.add(j);
				} catch (NumberFormatException e) {
					log.error("Invalid thumbnail size.", e);
				}
			}
		}
		if (outputs.size() > 0) {
			JsonObject json = new JsonObject()
					.put("action", "resizeMultiple")
					.put("src", storage.getProtocol() + "://" + storage.getBucket() + ":"
							+ srcFile.getString("_id"))
					.put("destinations", outputs);
			eb.send(imageResizerAddress, json, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonObject thumbnails = event.body().getJsonObject("outputs");
					if ("ok".equals(event.body().getString("status")) && thumbnails != null) {
						mongo.update(collection, new JsonObject().put("_id", documentId),
								new JsonObject().put("$set", new JsonObject()
										.put("thumbnails", thumbnails)), callback);
					}
				}
			}));
		} else if (callback != null) {
			callback.handle(null);
		}
	}

	private void compressImage(JsonObject srcFile, String quality, final Handler<Integer> handler) {
		if (!isImage(srcFile)) {
			handler.handle(null);
			return;
		}
		float q;
		if (quality != null) {
			try {
				q = Float.parseFloat(quality);
			} catch (NumberFormatException e) {
				log.warn(e.getMessage(), e);
				q = 0.8f;
			}
		} else {
			q = 0.8f;
		}
		JsonObject json = new JsonObject()
				.put("action", "compress")
				.put("quality", q)
				.put("src", storage.getProtocol() + "://" + storage.getBucket() + ":" + srcFile.getString("_id"))
				.put("dest", storage.getProtocol() + "://" + storage.getBucket() + ":" + srcFile.getString("_id"));
		eb.send(imageResizerAddress, json, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				Integer size = event.body().getInteger("size");
				handler.handle(size);
			}
		}));
	}

	@Put("/document/:id")
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void updateDocument(final HttpServerRequest request) {
		final String documentId = request.params().get("id");

		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					request.pause();
					documentDao.findById(documentId, new Handler<JsonObject>() {
						public void handle(JsonObject event) {
							if (!"ok".equals(event.getString("status"))) {
								notFound(request);
								return;
							}

							final String userId = event.getJsonObject("result").getString("owner");

							emptySize(userId, new Handler<Long>() {
								@Override
								public void handle(Long emptySize) {
									request.resume();
									storage.writeUploadFile(request, emptySize,
										new Handler<JsonObject>() {
											@Override
											public void handle(final JsonObject uploaded) {
												if ("ok".equals(uploaded.getString("status"))) {
													compressImage(uploaded, request.params().get("quality"), new Handler<Integer>() {
														@Override
														public void handle(Integer size) {
															JsonObject meta = uploaded.getJsonObject("metadata");
															if (size != null && meta != null) {
																meta.put("size", size);
															}
															updateAfterUpload(documentId, request.params().get("name"),
																	uploaded, request.params().getAll("thumbnail"), user,
																	new Handler<Message<JsonObject>>() {
																		@Override
																		public void handle(Message<JsonObject> res) {
																			if (res == null) {
																				request.response().setStatusCode(404).end();
																			} else if ("ok".equals(res.body().getString("status"))) {
																				renderJson(request, res.body());
																			} else {
																				renderError(request, res.body());
																			}
																		}
																	});
														}
													});
												} else {
													badRequest(request, uploaded.getString("message"));
												}
											}
									});
								}
							});
						}
					});
				}
			}
		});
	}

	private void updateAfterUpload(final String id, final String name, final JsonObject uploaded,
			final List<String> t, final UserInfos user, final Handler<Message<JsonObject>> handler) {
		documentDao.findById(id, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject old) {
				if ("ok".equals(old.getString("status"))) {
					final JsonObject metadata = uploaded.getJsonObject("metadata");
					JsonObject set = new JsonObject();
					final JsonObject doc = new JsonObject();
					doc.put("name", getOrElse(name, metadata.getString("filename")));
					final String now = MongoDb.formatDate(new Date());
					doc.put("modified", now);
					doc.put("metadata", metadata);
					doc.put("file", uploaded.getString("_id"));
					final JsonObject thumbs = old.getJsonObject("result", new JsonObject())
							.getJsonObject("thumbnails");

					String query = "{ \"_id\": \"" + id + "\"}";
					set.put("$set", doc).put("$unset", new JsonObject().put("thumbnails", ""));
					mongo.update(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(query), set,
							new Handler<Message<JsonObject>>() {
								@Override
								public void handle(final Message<JsonObject> res) {
									String status = res.body().getString("status");
									JsonObject result = old.getJsonObject("result");
									if ("ok".equals(status) && result != null) {
										String userId = user != null ? user.getUserId() : result.getString("owner");
										String userName = user != null ? user.getUsername() : result.getString("ownerName");
										doc.put("owner", result.getString("owner"));
										incrementStorage(doc);
										createRevision(id, doc.getString("file"), doc.getString("name"), result.getString("owner"), userId, userName, metadata);
										createThumbnailIfNeeded(DocumentDao.DOCUMENTS_COLLECTION,
												uploaded, id, thumbs, t, new Handler<Message<JsonObject>>() {
													@Override
													public void handle(Message<JsonObject> event) {
														if (handler != null) {
															handler.handle(res);
														}
													}
												});

									} else if (handler != null) {
										handler.handle(res);
									}
								}
							});
				} else if (handler != null) {
					handler.handle(null);
				}
			}
		});
	}

	@Get("/document/properties/:id")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void getDocumentProperties(final HttpServerRequest request) {
		documentDao.findById(request.params().get("id"), PROPERTIES_KEYS, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				JsonObject result = res.getJsonObject("result");
				if ("ok".equals(res.getString("status")) && result != null) {
					renderJson(request, result);
				} else {
					notFound(request);
				}
			}
		});
	}

	@Get("/document/:id")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void getDocument(HttpServerRequest request) {
		getFile(request, documentDao, null, false);
	}

	@Get("/pub/document/:id")
	public void getPublicDocument(HttpServerRequest request) {
		getFile(request, documentDao, null, true);
	}

	private void getFile(final HttpServerRequest request, GenericDao dao, String owner, boolean publicOnly) {
		dao.findById(request.params().get("id"), owner, publicOnly, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				String status = res.getString("status");
				JsonObject result = res.getJsonObject("result");
				String thumbSize = request.params().get("thumbnail");
				if ("ok".equals(status) && result != null) {
					String file;
					if (thumbSize != null && !thumbSize.trim().isEmpty()) {
						file = result.getJsonObject("thumbnails", new JsonObject())
								.getString(thumbSize, result.getString("file"));
					} else {
						file = result.getString("file");
					}
					if (file != null && !file.trim().isEmpty()) {
						boolean inline = inlineDocumentResponse(result, request.params().get("application"));
						if (inline && ETag.check(request, file)) {
							notModified(request, file);
						} else {
							storage.sendFile(file, result.getString("name"), request,
									inline, result.getJsonObject("metadata"));
						}
						eventStore.createAndStoreEvent(WokspaceEvent.GET_RESOURCE.name(), request,
								new JsonObject().put("resource", request.params().get("id")));
					} else {
						request.response().setStatusCode(404).end();
					}
				} else {
					request.response().setStatusCode(404).end();
				}
			}
		});
	}

	private boolean inlineDocumentResponse(JsonObject doc, String application) {
		JsonObject metadata = doc.getJsonObject("metadata");
		String storeApplication = doc.getString("application");
		return metadata != null && !"WORKSPACE".equals(storeApplication) && (
				"image/jpeg".equals(metadata.getString("content-type")) ||
				"image/gif".equals(metadata.getString("content-type")) ||
				"image/png".equals(metadata.getString("content-type")) ||
				"image/tiff".equals(metadata.getString("content-type")) ||
				"image/vnd.microsoft.icon".equals(metadata.getString("content-type")) ||
				"image/svg+xml".equals(metadata.getString("content-type")) ||
				("application/octet-stream".equals(metadata.getString("content-type")) && application != null)
			);
	}

	private boolean isImage(JsonObject doc) {
		if (doc == null) {
			return false;
		}
		JsonObject metadata = doc.getJsonObject("metadata");
		return metadata != null && (
				"image/jpeg".equals(metadata.getString("content-type")) ||
						"image/gif".equals(metadata.getString("content-type")) ||
						"image/png".equals(metadata.getString("content-type")) ||
						"image/tiff".equals(metadata.getString("content-type"))
		);
	}

	@Delete("/document/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void deleteDocument(HttpServerRequest request) {
		deleteFile(request, documentDao, null);
	}

	private void deleteFile(final HttpServerRequest request, final GenericDao dao, String owner) {
		final String id = request.params().get("id");
		dao.findById(id, owner, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				String status = res.getString("status");
				final JsonObject result = res.getJsonObject("result");
				if ("ok".equals(status) && result != null && result.getString("file") != null) {

					final String file = result.getString("file");
					Set<Entry<String, Object>> thumbnails = new HashSet<Entry<String, Object>>();
					if(result.containsKey("thumbnails")){
						thumbnails = result.getJsonObject("thumbnails").getMap().entrySet();
					}

					storage.removeFile(file,
							new Handler<JsonObject>() {
								@Override
								public void handle(JsonObject event) {
									if (event != null && "ok".equals(event.getString("status"))) {
										dao.delete(id, new Handler<JsonObject>() {
											@Override
											public void handle(final JsonObject result2) {
												if ("ok".equals(result2.getString("status"))) {
													deleteAllRevisions(id, new fr.wseduc.webutils.collections.JsonArray().add(file));
													decrementStorage(result, new Handler<Either<String, JsonObject>>() {
														@Override
														public void handle(Either<String, JsonObject> event) {
															renderJson(request, result2, 204);
														}
													});
												} else {
													renderError(request, result2);
												}
											}
										});
									} else {
										renderError(request, event);
									}
								}
							});

					//Delete thumbnails
					for(final Entry<String, Object> thumbnail : thumbnails){
						storage.removeFile(thumbnail.getValue().toString(), new Handler<JsonObject>(){
							public void handle(JsonObject event) {
								if (event == null || !"ok".equals(event.getString("status"))) {
									log.error("Error while deleting thumbnail "+thumbnail);
								}
							}
						});
					}
				} else {
					request.response().setStatusCode(404).end();
				}
			}
		});
	}

	@Post("/documents/copy/:ids/:folder")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void copyDocuments(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					copyFiles(request, DocumentDao.DOCUMENTS_COLLECTION, null, user);
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void copyFiles(final HttpServerRequest request, final String collection,
				final String owner, final UserInfos user) {
		String ids = request.params().get("ids"); // TODO refactor with json in request body
		String folder2 = getOrElse(request.params().get("folder"), "");
		try {
			folder2 = URLDecoder.decode(folder2, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.warn(e.getMessage(), e);
		}
		final String folder = folder2;
		if (ids != null && !ids.trim().isEmpty()) {
			JsonArray idsArray = new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(ids.split(",")));
			String criteria = "{ \"_id\" : { \"$in\" : " + idsArray.encode() + "}";
			if (owner != null) {
				criteria += ", \"to\" : \"" + owner + "\"";
			}
			criteria += "}";
			mongo.find(collection, new JsonObject(criteria), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> r) {
					JsonObject src = r.body();
					if ("ok".equals(src.getString("status")) && src.getJsonArray("results") != null) {
						final JsonArray origs = src.getJsonArray("results");
						final JsonArray insert = new fr.wseduc.webutils.collections.JsonArray();
						final AtomicInteger number = new AtomicInteger(origs.size());
						emptySize(user, new Handler<Long>() {
							@Override
							public void handle(Long emptySize) {
								long size = 0;
								for (Object o: origs) {
									if (!(o instanceof JsonObject)) continue;
									JsonObject metadata = ((JsonObject) o).getJsonObject("metadata");
									if (metadata != null) {
										size += metadata.getLong("size", 0l);
									}
								}
								if (size > emptySize) {
									badRequest(request, "files.too.large");
									return;
								}
								for (Object o: origs) {
									JsonObject orig = (JsonObject) o;
									final JsonObject dest = orig.copy();
									String now = MongoDb.formatDate(new Date());
									dest.remove("_id");
									dest.remove("protected");
									dest.remove("comments");
									dest.put("application", WORKSPACE_NAME);
									if (owner != null) {
										dest.put("owner", owner);
										dest.put("ownerName", dest.getString("toName"));
										dest.remove("to");
										dest.remove("from");
										dest.remove("toName");
										dest.remove("fromName");
									} else if (user != null) {
										dest.put("owner", user.getUserId());
										dest.put("ownerName", user.getUsername());
										dest.put("shared", new fr.wseduc.webutils.collections.JsonArray());
									}
									dest.put("_id", UUID.randomUUID().toString());
									dest.put("created", now);
									dest.put("modified", now);
									if (folder != null && !folder.trim().isEmpty()) {
										dest.put("folder", folder);
									} else {
										dest.remove("folder");
									}
									insert.add(dest);
									final String filePath = orig.getString("file");

									if((owner != null || user != null) && folder != null && !folder.trim().isEmpty()){

										//If the document has a new parent folder, replicate sharing rights
										String parentName, parentFolder;
										if(folder.lastIndexOf('_') < 0){
											parentName = folder;
											parentFolder = folder;
										} else if(filePath != null){
											String[] splittedPath = folder.split("_");
											parentName = splittedPath[splittedPath.length - 1];
											parentFolder = folder;
										} else {
											String[] splittedPath = folder.split("_");
											parentName = splittedPath[splittedPath.length - 2];
											parentFolder = folder.substring(0, folder.lastIndexOf("_"));
										}

										folderService.getParentRights(parentName, parentFolder, owner, new Handler<Either<String, JsonArray>>(){
											public void handle(Either<String, JsonArray> event) {
												final JsonArray parentSharedRights = event.right() == null || event.isLeft() ?
														null : event.right().getValue();

												if(parentSharedRights != null && parentSharedRights.size() > 0)
													dest.put("shared", parentSharedRights);
												if (filePath != null) {
													storage.copyFile(filePath, new Handler<JsonObject>() {
														@Override
														public void handle(JsonObject event) {
															if (event != null && "ok".equals(event.getString("status"))) {
																dest.put("file", event.getString("_id"));
																persist(insert, number.decrementAndGet());
															}
														}
													});
												} else {
													persist(insert, number.decrementAndGet());
												}
											}
										});
									} else if (filePath != null) {
										storage.copyFile(filePath, new Handler<JsonObject>() {

											@Override
											public void handle(JsonObject event) {
												if (event != null && "ok".equals(event.getString("status"))) {
													dest.put("file", event.getString("_id"));
													persist(insert, number.decrementAndGet());
												}
											}
										});
									} else {
										persist(insert, number.decrementAndGet());
									}
								}
							}
						});
					} else {
						renderJson(request, src, 404);
					}
				}

				private void persist(final JsonArray insert, int remains) {
					if (remains == 0) {
						mongo.insert(DocumentDao.DOCUMENTS_COLLECTION, insert, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> inserted) {
								if ("ok".equals(inserted.body().getString("status"))){
									incrementStorage(insert);
									for(Object obj : insert){
										JsonObject json = (JsonObject) obj;
										createRevision(
												json.getString("_id"),
												json.getString("file"),
												json.getString("name"),
												json.getString("owner"),
												json.getString("owner"),
												json.getString("ownerName"),
												json.getJsonObject("metadata"));
									}
									renderJson(request, inserted.body());
								} else {
									renderError(request, inserted.body());
								}
							}
						});
					}
				}

			});
		} else {
			badRequest(request);
		}
	}

	@Post("/document/copy/:id/:folder")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void copyDocument(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					emptySize(user, new Handler<Long>() {
						@Override
						public void handle(Long emptySize) {
							copyFile(request, documentDao, null, emptySize);
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void copyFile(final HttpServerRequest request, final GenericDao dao, final String owner,
			final long emptySize) {
		dao.findById(request.params().get("id"), owner, new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject src) {
				if ("ok".equals(src.getString("status")) && src.getJsonObject("result") != null) {
					JsonObject orig = src.getJsonObject("result");
					if (orig.getJsonObject("metadata", new JsonObject()).getLong("size", 0l) > emptySize) {
						badRequest(request, "file.too.large");
						return;
					}
					final JsonObject dest = orig.copy();
					String now = MongoDb.formatDate(new Date());
					dest.remove("_id");
					dest.remove("protected");
					dest.put("application", WORKSPACE_NAME);
					if (owner != null) {
						dest.put("owner", owner);
						dest.put("ownerName", dest.getString("toName"));
						dest.remove("to");
						dest.remove("from");
						dest.remove("toName");
						dest.remove("fromName");
					}
					dest.put("created", now);
					dest.put("modified", now);
					String folder = getOrElse(request.params().get("folder"), "");
					try {
						folder = URLDecoder.decode(folder, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						log.warn(e.getMessage(), e);
					}
					dest.put("folder", folder);
					String filePath = orig.getString("file");
					if (filePath != null) {
						storage.copyFile(filePath, new Handler<JsonObject>() {

							@Override
							public void handle(JsonObject event) {
								if (event != null && "ok".equals(event.getString("status"))) {
									dest.put("file", event.getString("_id"));
									persist(dest);
								}
							}
						});
					} else {
						persist(dest);
					}
				} else {
					renderJson(request, src, 404);
				}
			}

			private void persist(final JsonObject dest) {
				documentDao.save(dest, new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject res) {
						if ("ok".equals(res.getString("status"))) {
							incrementStorage(dest);
							renderJson(request, res);
						} else {
							renderError(request, res);
						}
					}
				});
			}

		});
	}

	private String orSharedElementMatch(UserInfos user) {
		StringBuilder sb = new StringBuilder();
		if (user.getGroupsIds() != null) {
			for (String groupId: user.getGroupsIds()) {
				sb.append(", { \"groupId\": \"" + groupId + "\" }");
			}
		}
		return "{ \"$or\" : [{ \"userId\": \"" + user.getUserId() + "\" }" + sb.toString() + "]}";
	}

	@Get("/folders")
	@SecuredAction("workspace.document.list.folders")
	public void listFolders(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					final String hierarchical = request.params().get("hierarchical");
					String relativePath2;
					try {
						relativePath2 = URLDecoder.decode(request.params().get("folder"), "UTF-8");
					} catch (UnsupportedEncodingException | NullPointerException e) {
						relativePath2 = request.params().get("folder");
					}
					final String relativePath = relativePath2;
					String filter = request.params().get("filter");
					String query = "{ ";
					if ("owner".equals(filter)) {
						query += "\"owner\": \"" + user.getUserId() + "\"";
					} else if ("shared".equals(filter)) {
						query += "\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user) + "}";
					} else {
						query += "\"$or\" : [{ \"owner\": \"" + user.getUserId() +
								"\"}, {\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user) + "}}]";
					}
					if (relativePath != null) {
						query += ", \"folder\" : { \"$regex\" : \"^" + relativePath + "(_|$)\" }}";
					} else {
						query += "}";
					}
					mongo.distinct(DocumentDao.DOCUMENTS_COLLECTION, "folder", new JsonObject(query),
							new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> res) {
									if ("ok".equals(res.body().getString("status"))) {
										JsonArray values = res.body().getJsonArray("values", new fr.wseduc.webutils.collections.JsonArray("[]"));
										JsonArray out = values;
										if (hierarchical != null) {
											Set<String> folders = new HashSet<String>();
											for (Object value : values) {
												String v = (String) value;
												if (relativePath != null) {
													if (v != null && v.contains("_") &&
															v.indexOf("_", relativePath.length() + 1) != -1 &&
															v.substring(v.indexOf("_", relativePath.length() + 1)).contains("_")) {
														folders.add(v.substring(0, v.indexOf("_", relativePath.length() + 1)));
													} else {
														folders.add(v);
													}
												} else {
													if (v != null && v.contains("_")) {
														folders.add(v.substring(0, v.indexOf("_")));
													} else {
														folders.add(v);
													}
												}
											}
											out = new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(folders));
										}
										renderJson(request, out);
									} else {
										renderJson(request, new fr.wseduc.webutils.collections.JsonArray());
									}
								}
							});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void notifyComment(final HttpServerRequest request, final String id, final UserInfos user, final boolean isFolder) {
		final JsonObject params = new JsonObject()
			.put("userUri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
			.put("userName", user.getUsername())
			.put("appPrefix", pathPrefix+"/workspace");

		final String notifyName = WORKSPACE_NAME.toLowerCase() + "." + (isFolder ? "comment-folder" : "comment");

		//Retrieve the document from DB
		mongo.findOne(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject().put("_id", id), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) && event.body().getJsonObject("result") != null) {
					final JsonObject document = event.body().getJsonObject("result");
					params.put("resourceName", document.getString("name", ""));

					//Handle the parent folder id (null if document is at root level)
					final Handler<String> folderIdHandler = new Handler<String>() {
						public void handle(final String folderId) {

							//Send the notification to the shared network
							Handler<List<String>> shareNotificationHandler = new Handler<List<String>>() {
								public void handle(List<String> recipients) {
									JsonObject sharedNotifParams = params.copy();

									if(folderId != null){
										sharedNotifParams.put("resourceUri", pathPrefix + "/workspace#/shared/folder/" + folderId);
									} else {
										sharedNotifParams.put("resourceUri", pathPrefix + "/workspace#/shared");
									}

									// don't send comment with share uri at owner
									final String o = document.getString("owner");
									if(o != null && recipients.contains(o)) {
										recipients.remove(o);
									}
									notification.notifyTimeline(request, notifyName, user, recipients, id, sharedNotifParams);
								}
							};

							//'Flatten' the share users & group into a user id list (excluding the current user)
							flattenShareIds(document.getJsonArray("shared", new fr.wseduc.webutils.collections.JsonArray()), user, shareNotificationHandler);

							//If the user commenting is not the owner, send a notification to the owner
							if(!document.getString("owner").equals(user.getUserId())){
								JsonObject ownerNotif = params.copy();
								ArrayList<String> ownerList = new ArrayList<>();
								ownerList.add(document.getString("owner"));

								if(folderId != null){
									ownerNotif.put("resourceUri", pathPrefix + "/workspace#/folder/" + folderId);
								} else {
									ownerNotif.put("resourceUri", pathPrefix + "/workspace");
								}
								notification.notifyTimeline(request, notifyName, user, ownerList, id, null, ownerNotif, true);
							}

						}
					};

					//Handle the parent folder result from DB
					Handler<Message<JsonObject>> folderHandler = new Handler<Message<JsonObject>>() {
						public void handle(Message<JsonObject> event) {
							if(!"ok".equals(event.body().getString("status")) || event.body().getJsonObject("result") == null){
								log.error("Unable to send timeline notification : invalid parent folder on resource " + id);
								return;
							}

							final JsonObject folder = event.body().getJsonObject("result");
							final String folderId = folder.getString("_id");

							folderIdHandler.handle(folderId);
						}
					};

					//If the document does not have a parent folder
					if(!isFolder && !document.containsKey("folder") || isFolder && document.getString("folder").equals(document.getString("name"))){
						folderIdHandler.handle(null);
					} else {
						//If the document has a parent folder
						String parentFolderPath = document.getString("folder");
						if(isFolder){
							parentFolderPath = parentFolderPath.substring(0, parentFolderPath.lastIndexOf("_"));
						}

						QueryBuilder query = QueryBuilder
								.start("folder").is(parentFolderPath)
								.and("file").exists(false)
								.and("owner").is(document.getString("owner"));

						//Retrieve the parent folder from DB
						mongo.findOne(DocumentDao.DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), folderHandler);
					}


				} else {
					log.error("Unable to send timeline notification : missing name on resource " + id);
				}
			}
		});
	}

	private void flattenShareIds(final JsonArray sharedArray, final UserInfos user, final Handler<List<String>> resultHandler){
		final List<String> recipients = new ArrayList<>();
		final AtomicInteger remaining = new AtomicInteger(sharedArray.size());
		for (Object j : sharedArray) {
			JsonObject json = (JsonObject) j;
			String userId = json.getString("userId");
			if (userId != null) {
				if(!userId.equals(user.getUserId()))
					recipients.add(userId);
				remaining.getAndDecrement();
			} else {
				String groupId = json.getString("groupId");
				if (groupId != null) {
					UserUtils.findUsersInProfilsGroups(groupId, eb, user.getUserId(), false, new Handler<JsonArray>() {
						public void handle(JsonArray event) {
							if (event != null) {
								for(Object o: event) {
									if (!(o instanceof JsonObject)) continue;
									JsonObject j = (JsonObject) o;
									String id = j.getString("id");
									if(!id.equals(user.getUserId()))
										recipients.add(id);
								}
							}
							if (remaining.decrementAndGet() < 1) {
								resultHandler.handle(recipients);
							}
						}
					});
				}
			}
		}
		if (remaining.get() < 1) {
			resultHandler.handle(recipients);
		}
	}

	@Post("/document/:id/comment")
	@SecuredAction(value = "workspace.comment", type = ActionType.RESOURCE)
	public void commentDocument(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					request.setExpectMultipart(true);
					request.endHandler(new Handler<Void>() {
						@Override
						public void handle(Void v) {
							String comment = request.formAttributes().get("comment");
							if (comment != null && !comment.trim().isEmpty()) {
								final String id = UUID.randomUUID().toString();
								JsonObject query = new JsonObject()
									.put("$push", new JsonObject()
										.put("comments", new JsonObject()
											.put("id", id)
											.put("author", user.getUserId())
											.put("authorName", user.getUsername())
											.put("posted", MongoDb.formatDate(new Date()))
											.put("comment", comment)));
								documentDao.update(request.params().get("id"), query,
										new Handler<JsonObject>() {
									@Override
									public void handle(JsonObject res) {
										if ("ok".equals(res.getString("status"))) {
											notifyComment(request, request.params().get("id"), user, false);
											renderJson(request, res.put("id", id));
										} else {
											renderError(request, res);
										}
									}
								});
							} else {
								badRequest(request);
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Post("/folder/:id/comment")
	@SecuredAction(value = "workspace.comment", type = ActionType.RESOURCE)
	public void commentFolder(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					request.setExpectMultipart(true);
					request.endHandler(new Handler<Void>() {
						@Override
						public void handle(Void v) {
							String comment = request.formAttributes().get("comment");
							if (comment != null && !comment.trim().isEmpty()) {
								final String id = UUID.randomUUID().toString();
								JsonObject query = new JsonObject()
									.put("$push", new JsonObject()
										.put("comments", new JsonObject()
											.put("id", id)
											.put("author", user.getUserId())
											.put("authorName", user.getUsername())
											.put("posted", MongoDb.formatDate(new Date()))
											.put("comment", comment)));
								documentDao.update(request.params().get("id"), query,
										new Handler<JsonObject>() {
									@Override
									public void handle(JsonObject res) {
										if ("ok".equals(res.getString("status"))) {
											notifyComment(request, request.params().get("id"), user, true);
											renderJson(request, res.put("id", id));
										} else {
											renderError(request, res);
										}
									}
								});
							} else {
								badRequest(request);
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Delete("/document/:id/comment/:commentId")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void deleteComment(final HttpServerRequest request) {
		final String id = request.params().get("id");
		final String commentId = request.params().get("commentId");

		QueryBuilder query = QueryBuilder.start("_id").is(id);
		MongoUpdateBuilder queryUpdate = new MongoUpdateBuilder().pull("comments", new JsonObject().put("id", commentId));

		mongo.update(DocumentDao.DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), queryUpdate.build(), MongoDbResult.validActionResultHandler(defaultResponseHandler(request)));
	}

	@Put("/document/move/:id/:folder")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void moveDocument(final HttpServerRequest request) {
		String folder = getOrElse(request.params().get("folder"), "");
		try {
			folder = URLDecoder.decode(folder, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.warn(e.getMessage(), e);
		}
		moveOne(request, folder, documentDao, null);
	}

	@Put("/document/trash/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void moveTrash(final HttpServerRequest request) {
		moveOne(request, "Trash", documentDao, null);
	}

	private void moveOne(final HttpServerRequest request, final String folder,
			final GenericDao dao, final String owner) {
		String obj = "{ \"$rename\" : { \"folder\" : \"old-folder\"}}";
		dao.update(request.params().get("id"), new JsonObject(obj), owner, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				if ("ok".equals(res.getString("status"))) {
					String obj2 = "";
					if("Trash".equals(folder)){
						obj2 = "{ \"$set\" : { \"folder\": \"" + folder +"\", "
								+ "\"modified\" : \""+ MongoDb.formatDate(new Date()) + "\"}, "
								+ "\"$unset\": { \"shared\": true }}";
					} else {
						obj2 = "{ \"$set\" : { \"folder\": \"" + folder +
								"\", \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\" }}";
					}
					dao.update(request.params().get("id"), new JsonObject(obj2), owner, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject res) {
							if ("ok".equals(res.getString("status"))) {
								renderJson(request, res);
							} else {
								renderJson(request, res, 404);
							}
						}
					});
				} else {
					renderJson(request, res, 404);
				}
			}
		});
	}

	@Put("/documents/move/:ids/:folder")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void moveDocuments(final HttpServerRequest request) {
		final String ids = request.params().get("ids"); // TODO refactor with json in request body
		String tempFolder = getOrElse(request.params().get("folder"), "");
		try {
			tempFolder = URLDecoder.decode(tempFolder, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.warn(e.getMessage(), e);
		}
		final String folder = tempFolder;
		final String cleanedFolder = folder.replaceAll(Pattern.quote("\\"), Matcher.quoteReplacement ("\\\\")).replaceAll(Pattern.quote("\""), Matcher.quoteReplacement ("\\\""));

		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					if (ids != null && !ids.trim().isEmpty()) {
						JsonArray idsArray = new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(ids.split(",")));
						final String criteria = "{ \"_id\" : { \"$in\" : " + idsArray.encode() + "}}";

						if (folder != null && !folder.trim().isEmpty()) {

							//If the document has a parent folder, replicate sharing rights
							String parentName, parentFolder;
							if (folder.lastIndexOf('_') < 0) {
								parentName = folder;
								parentFolder = folder;
							} else {
								String[] splittedPath = folder.split("_");
								parentName = splittedPath[splittedPath.length - 1];
								parentFolder = folder;
							}

							folderService.getParentRights(parentName, parentFolder, user, new Handler<Either<String, JsonArray>>(){
								public void handle(Either<String, JsonArray> event) {
									final JsonArray parentSharedRights = event.right() == null || event.isLeft() ?
											null : event.right().getValue();

									String obj = "{ \"$set\" : { \"folder\": \"" + cleanedFolder +
												"\", \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\"";
									if(parentSharedRights != null && parentSharedRights.size() > 0)
										obj += ", \"shared\" : "+parentSharedRights.toString()+" }}";
									else
										obj += "}, \"$unset\" : { \"shared\": 1 }}";

									mongo.update(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(criteria),
											new JsonObject(obj), false, true, new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> r) {
											JsonObject res = r.body();
											if ("ok".equals(res.getString("status"))) {
												renderJson(request, res);
											} else {
												renderJson(request, res, 404);
											}
										}
									});
								}
							});
						} else {
							String obj = "{ \"$set\" : { \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\" }, " +
									" \"$unset\" : { \"folder\" : 1, \"shared\": 1 }}";

							mongo.update(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(criteria),
									new JsonObject(obj), false, true, new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> r) {
											JsonObject res = r.body();
											if ("ok".equals(res.getString("status"))) {
												renderJson(request, res);
											} else {
												renderJson(request, res, 404);
											}
										}
									});
						}
					} else {
						badRequest(request);
					}
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Get("/documents")
	@SecuredAction("workspace.documents.list")
	public void listDocuments(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					String filter = request.params().get("filter");
					String query = "{ ";
					String forApplication = ", \"application\": \"" + getOrElse(request.params()
							.get("application"), WorkspaceService.WORKSPACE_NAME) + "\"";
					if ("owner".equals(filter)) {
						query += "\"owner\": \"" + user.getUserId() + "\"";
					} else if ("protected".equals(filter)) {
						query += "\"owner\": \"" + user.getUserId() + "\", \"protected\":true";
						forApplication = "";
					} else if ("public".equals(filter)) {
						query += "\"owner\": \"" + user.getUserId() + "\", \"public\":true";
						forApplication = "";
					} else if ("shared".equals(filter)) {
						query += "\"owner\": { \"$ne\":\"" + user.getUserId() +
								"\"},\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user) + "}";
					} else {
						query += "\"$or\" : [{ \"owner\": \"" + user.getUserId() +
								"\"}, {\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user) + "}}]";
					}

					if (request.params().get("hierarchical") != null) {
						query += ", \"file\" : { \"$exists\" : true }" +
								forApplication + ", \"folder\" : { \"$exists\" : false }}";
					} else {
						query += ", \"file\" : { \"$exists\" : true }" + forApplication + "}";
					}
					mongo.find(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(query), new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> res) {
							String status = res.body().getString("status");
							JsonArray results = res.body().getJsonArray("results");
							if ("ok".equals(status) && results != null) {
								renderJson(request, results);
							} else {
								renderJson(request, new fr.wseduc.webutils.collections.JsonArray());
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Get("/documents/:folder")
	@SecuredAction("workspace.documents.list.by.folder")
	public void listDocumentsByFolder(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					String filter = request.params().get("filter");
					String query = "{ ";
					if ("owner".equals(filter)) {
						query += "\"owner\": \"" + user.getUserId() + "\"";
					} else if ("shared".equals(filter)) {
						String ownerId = request.params().get("ownerId");
						query += "\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user) + "}";
						if(ownerId != null){
							query += ", \"owner\": \"" + ownerId + "\"";
						}
					} else {
						query += "\"$or\" : [{ \"owner\": \"" + user.getUserId() +
								"\"}, {\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user) + "}}]";
					}
					String folder = getOrElse(request.params().get("folder"), "");
					try {
						folder = URLDecoder.decode(folder, "UTF-8");
						folder = folder.replaceAll(Pattern.quote("\\"), Matcher.quoteReplacement ("\\\\")).replaceAll(Pattern.quote("\""), Matcher.quoteReplacement ("\\\""));
					} catch (UnsupportedEncodingException e) {
						log.warn(e.getMessage(), e);
					}
					String forApplication = getOrElse(request.params()
							.get("application"), WorkspaceService.WORKSPACE_NAME);
					if (request.params().get("hierarchical") != null) {
						query += ", \"file\" : { \"$exists\" : true }, \"application\": \"" +
								forApplication + "\", \"folder\" : \"" + folder + "\" }";
					} else {
						query += ", \"file\" : { \"$exists\" : true }, \"application\": \"" +
								forApplication + "\", \"folder\" : { \"$regex\" : \"^" +
								folder + "(_|$)\" }}";
					}
					mongo.find(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(query), new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> res) {
							String status = res.body().getString("status");
							JsonArray results = res.body().getJsonArray("results");
							if ("ok".equals(status) && results != null) {
								renderJson(request, results);
							} else {
								renderJson(request, new fr.wseduc.webutils.collections.JsonArray());
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Put("/restore/document/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void restoreTrash(HttpServerRequest request) {
		restore(request, documentDao, null);
	}

	private void restore(final HttpServerRequest request, final GenericDao dao, final String to) {
		final String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			dao.findById(id, to, new Handler<JsonObject>() {

				@Override
				public void handle(JsonObject res) {
					if ("ok".equals(res.getString("status"))) {
						JsonObject doc = res.getJsonObject("result");
						if (doc.getString("old-folder") != null) {
							doc.put("folder", doc.getString("old-folder"));
						} else {
							doc.remove("folder");
						}
						doc.remove("old-folder");
						dao.update(id, doc, to, new Handler<JsonObject>() {
							@Override
							public void handle(JsonObject res) {
								if ("ok".equals(res.getString("status"))) {
									renderJson(request, res);
								} else {
									renderJson(request, res, 404);
								}
							}
						});
					} else {
						renderJson(request, res, 404);
					}
				}
			});
		} else {
			badRequest(request);
		}
	}

	@Get("/workspace/availables-workflow-actions")
	@SecuredAction(value = "workspace.habilitation", type = ActionType.AUTHENTICATED)
	public void getActionsInfos(final HttpServerRequest request) {
		ActionsUtils.findWorkflowSecureActions(eb, request, this);
	}

	@BusAddress("org.entcore.workspace")
	public void workspaceEventBusHandler(final Message<JsonObject> message) {
		switch (message.body().getString("action", "")) {
			case "addDocument" : addDocument(message);
				break;
			case "updateDocument" : updateDocument(message);
				break;
			case "getDocument" : getDocument(message);
				break;
			case "copyDocument" : copyDocument(message);
				break;
			default:
				message.reply(new JsonObject().put("status", "error")
						.put("message", "invalid.action"));
		}
	}

	private void getDocument(final Message<JsonObject> message) {
		documentDao.findById(message.body().getString("id"), new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				message.reply(res);
			}
		});
	}

	private void addDocument(final Message<JsonObject> message) {
		JsonObject uploaded = message.body().getJsonObject("uploaded");
		JsonObject doc = message.body().getJsonObject("document");
		if (doc == null || uploaded == null) {
			message.reply(new JsonObject().put("status", "error")
					.put("message", "missing.attribute"));
			return;
		}
		String name = message.body().getString("name");
		String application = message.body().getString("application");
		JsonArray t = message.body().getJsonArray("thumbs", new fr.wseduc.webutils.collections.JsonArray());
		List<String> thumbs = new ArrayList<>();
		for (int i = 0; i < t.size(); i++) {
			thumbs.add(t.getString(i));
		}
		addAfterUpload(uploaded, doc,  name, application, thumbs, DocumentDao.DOCUMENTS_COLLECTION,
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				if (m != null) {
					message.reply(m.body());
				}
			}
		});
	}

	private void updateDocument(final Message<JsonObject> message) {
		JsonObject uploaded = message.body().getJsonObject("uploaded");
		String id = message.body().getString("id");
		if (uploaded == null || id == null || id.trim().isEmpty()) {
			message.reply(new JsonObject().put("status", "error")
					.put("message", "missing.attribute"));
			return;
		}
		String name = message.body().getString("name");
		JsonArray t = message.body().getJsonArray("thumbs", new fr.wseduc.webutils.collections.JsonArray());
		List<String> thumbs = new ArrayList<>();
		for (int i = 0; i < t.size(); i++) {
			thumbs.add(t.getString(i));
		}
		updateAfterUpload(id, name, uploaded, thumbs, null, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				if (m != null) {
					message.reply(m.body());
				}
			}
		});
	}

	public void copyDocument(final Message<JsonObject> message) {
		emptySize(message.body().getJsonObject("user").getString("userId"), new Handler<Long>() {
			@Override
			public void handle(Long emptySize) {
				copyFile(message.body(), documentDao, emptySize, new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject res) {
						if ("ok".equals(res.getString("status")))
							message.reply(res);
						else
							message.fail(500, res.getString("status"));
					}
				});
			}
		});
	}

	private void copyFile(JsonObject message, final GenericDao dao, final long emptySize, Handler<JsonObject> handler) {
		try {
			dao.findById(message.getString("documentId"), message.getString("ownerId"), new Handler<JsonObject>() {
				@Override
				public void handle(JsonObject src) {
					if ("ok".equals(src.getString("status")) && src.getJsonObject("result") != null) {
						JsonObject orig = src.getJsonObject("result");
						if (orig.getJsonObject("metadata", new JsonObject()).getLong("size", 0l) > emptySize) {
							handler.handle(new JsonObject().put("status", "ko").put("error", "file.too.large"));
							return;
						}
						final JsonObject dest = orig.copy();
						String now = MongoDb.formatDate(new Date());
						dest.remove("_id");
						dest.remove("protected");
						if (message.getBoolean("protected") != null)
							dest.put("protected", message.getBoolean("protected"));
						dest.put("application", WORKSPACE_NAME);
						if (message.getJsonObject("user") != null) {
							dest.put("owner", message.getJsonObject("user").getString("userId"));
							dest.put("ownerName", message.getJsonObject("user").getString("userName"));
							dest.remove("to");
							dest.remove("from");
							dest.remove("toName");
							dest.remove("fromName");
						}
						dest.put("created", now);
						dest.put("modified", now);
						String folder = getOrElse(message.getString("folder"), "");
						try {
							folder = URLDecoder.decode(folder, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							log.warn(e.getMessage(), e);
						}
						dest.put("folder", folder);
						String filePath = orig.getString("file");
						if (filePath != null) {
							storage.copyFile(filePath, new Handler<JsonObject>() {

								@Override
								public void handle(JsonObject event) {
									if (event != null && "ok".equals(event.getString("status"))) {
										dest.put("file", event.getString("_id"));
										persist(dest);
									}
								}
							});
						} else {
							persist(dest);
						}
					}
					else {
						handler.handle(new JsonObject().put("status", "ko").put("error", "not.found"));
					}
				}

				private void persist(final JsonObject dest) {
					documentDao.save(dest, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject res) {
							if ("ok".equals(res.getString("status"))) {
								incrementStorage(dest);
								handler.handle(res);
							} else {
								handler.handle(res);
							}
						}
					});
				}

			});
		}
		catch (Exception e){ log.error(e); }
	}

	public void setQuotaService(QuotaService quotaService) {
		this.quotaService = quotaService;
	}

	@Put("/folder/rename/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void renameFolder(final HttpServerRequest request){
		RequestUtils.bodyToJson(request, pathPrefix + "rename", new Handler<JsonObject>() {
			public void handle(final JsonObject body) {
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					public void handle(UserInfos userInfos) {
						if(userInfos != null){
							final String name = replaceUnderscore(body.getString("name"));
							String id = request.params().get("id");
							folderService.rename(id, name, userInfos, defaultResponseHandler(request));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});

	}

	@Put("/rename/document/:id")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void renameDocument(final HttpServerRequest request){
		RequestUtils.bodyToJson(request, pathPrefix + "rename", new Handler<JsonObject>() {
			public void handle(final JsonObject body) {
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					public void handle(UserInfos userInfos) {
						if(userInfos != null){
							String id = request.params().get("id");

							final QueryBuilder matcher = QueryBuilder.start("_id").is(id).put("owner").is(userInfos.getUserId()).and("file").exists(true);
							MongoUpdateBuilder modifier = new MongoUpdateBuilder();
							if (body.getString("name") != null) modifier.set("name", body.getString("name"));
							if (body.getString("alt") != null) modifier.set("alt", body.getString("alt"));
							if (body.getString("legend") != null) modifier.set("legend", body.getString("legend"));

							mongo.update(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(matcher), modifier.build(), MongoDbResult.validResultHandler(defaultResponseHandler(request)));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	public void setStorage(Storage storage) {
		this.storage = storage;
	}

	@Get("/document/:id/revisions")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void listRevisions(HttpServerRequest request) {
		String id = request.params().get("id");
		final QueryBuilder builder = QueryBuilder.start("documentId").is(id);
		mongo.find(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder), MongoDbResult.validResultsHandler(arrayResponseHandler(request)));
	}

	@Get("/document/:id/revision/:revisionId")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void getRevision(HttpServerRequest request) {
		getRevisionFile(request, request.params().get("id"), request.params().get("revisionId"));
	}

	private void getRevisionFile(final HttpServerRequest request, final String documentId, final String revisionId) {
		final QueryBuilder builder = QueryBuilder.start("_id").is(revisionId).and("documentId").is(documentId);

		//Find revision
		mongo.findOne(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder), MongoDbResult.validResultHandler(new Handler<Either<String,JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if(event.isLeft()){
					notFound(request);
					return;
				}
				JsonObject result = event.right().getValue();
				String file = result.getString("file");
				if (file != null && !file.trim().isEmpty()) {
					if (ETag.check(request, file)) {
						notModified(request, file);
					} else {
						storage.sendFile(file, result.getString("name"), request, false, result.getJsonObject("metadata"));
					}
					eventStore.createAndStoreEvent(WokspaceEvent.GET_RESOURCE.name(), request,
							new JsonObject().put("resource", documentId));
				} else {
					notFound(request);
				}
			}
		}));
	}

	private void createRevision(final String id, final String file, final String name, String ownerId, String userId, String userName, JsonObject metadata){
		JsonObject document = new JsonObject();
		document
			.put("documentId", id)
			.put("file", file)
			.put("name", name)
			.put("owner", ownerId)
			.put("userId", userId)
			.put("userName", userName)
			.put("date", MongoDb.now())
			.put("metadata", metadata);

		mongo.save(DOCUMENT_REVISION_COLLECTION, document, MongoDbResult.validResultHandler(new Handler<Either<String,JsonObject>>() {
			public void handle(Either<String, JsonObject> event) {
				if (event.isLeft()) {
					log.error("[Workspace] Error creating revision " + id + "/" + file + " - " + event.left().getValue());
				}
			}
		}));
	}

	@Delete("/document/:id/revision/:revisionId")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void deleteRevision(final HttpServerRequest request){
		final String id = request.params().get("id");
		final String revisionId = request.params().get("revisionId");
		final QueryBuilder builder = QueryBuilder.start("_id").is(revisionId).and("documentId").is(id);

		//Find revision
		mongo.findOne(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder), MongoDbResult.validResultHandler(new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					final JsonObject result = event.right().getValue();
					final String file = result.getString("file");
					//Delete file in storage
					storage.removeFile(file, new Handler<JsonObject>(){
						public void handle(JsonObject event) {
							if(event != null && "ok".equals(event.getString("status"))){
								//Delete revision
								mongo.delete(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder), MongoDbResult.validResultHandler(new Handler<Either<String,JsonObject>>() {
									public void handle(Either<String, JsonObject> event) {
										if (event.isLeft()) {
											log.error("[Workspace] Error deleting revision " + revisionId + " - " + event.left().getValue());
											badRequest(request, event.left().getValue());
										} else {
											decrementStorage(result);
											renderJson(request, event.right().getValue());
										}
									}
								}));
							} else {
								log.error("[Workspace] Error deleting revision storage file " + revisionId + " ["+file+"] - " + event.getString("message"));
								badRequest(request, event.getString("message"));
							}
						}
					});
				} else {
					log.error("[Workspace] Error finding revision storage file " + revisionId + " - " + event.left().getValue());
					notFound(request, event.left().getValue());
				}
			}
		}));
	}

	private void deleteAllRevisions(final String documentId, final JsonArray alreadyDeleted){
		final QueryBuilder builder = QueryBuilder.start("documentId").is(documentId);
		JsonObject keys = new JsonObject();

		mongo.find(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder), new JsonObject(), keys, MongoDbResult.validResultsHandler(new Handler<Either<String,JsonArray>>() {
			public void handle(Either<String, JsonArray> event) {
				if (event.isRight()) {
					final JsonArray results = event.right().getValue();
					final JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
					for(Object obj : results){
						JsonObject json = (JsonObject) obj;
						String id = json.getString("file");
						if (id != null && !alreadyDeleted.contains(id)) {
							ids.add(id);
						}
					}
					storage.removeFiles(ids, new Handler<JsonObject>() {
						public void handle(JsonObject event) {
							if(event != null && "ok".equals(event.getString("status"))){
								//Delete revisions
								mongo.delete(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder), MongoDbResult.validResultHandler(new Handler<Either<String,JsonObject>>() {
									public void handle(Either<String, JsonObject> event) {
										if (event.isLeft()) {
											log.error("[Workspace] Error deleting revisions for document " + documentId + " - " + event.left().getValue());
										} else {
											for(Object obj : results){
												JsonObject result = (JsonObject) obj;
												if(!alreadyDeleted.contains(result.getString("file")))
													decrementStorage(result);
											}
										}
									}
								}));
							} else {
								log.error("[Workspace] Error deleting revision storage files for document " + documentId + " "+ids+" - " + event.getString("message"));
							}
						}
					});
				} else {
					log.error("[Workspace] Error finding revision for document " + documentId + " - " + event.left().getValue());
				}
			}
		}));
	}


	private String replaceUnderscore(String value) {
		String n = value;
		if (n != null) {
			n = n.replaceAll("_", "＿"); // "&#95;"
		}
		return n;
	}

}
