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
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.user.UserUtils.getUserInfos;
import static org.entcore.workspace.dao.DocumentDao.DOCUMENTS_COLLECTION;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

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
import fr.wseduc.webutils.FileUtils;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.ETag;
import fr.wseduc.webutils.request.RequestUtils;

public class WorkspaceService extends BaseController {

	public static final String WORKSPACE_NAME = "WORKSPACE";
	private String gridfsAddress;
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

	public void init(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, container, rm, securedActions);
		mongo = MongoDb.getInstance();
		gridfsAddress = container.config().getString("gridfs-address", "wse.gridfs.persistor");
		imageResizerAddress = container.config().getString("image-resizer-address", "wse.image.resizer");
		documentDao = new DocumentDao(mongo);
		notification = new TimelineHelper(vertx, eb, container);
		this.shareService = new MongoDbShareService(eb, mongo, "documents", securedActions, null);
		this.folderService = new DefaultFolderService(eb, mongo, gridfsAddress);
		this.threshold = container.config().getInteger("alertStorage", 80);
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
								for (String attr : j.getFieldNames()) {
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
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
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
					shareService.shareInfos(user.getUserId(), id, I18n.acceptLanguage(request), defaultResponseHandler(request));
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
					JsonObject n = event.right().getValue().getObject("notify-timeline");
					if (n != null) {
						notifyShare(request, id, user, new JsonArray().add(n));
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
								JsonObject n = event.right().getValue().getObject("notify-timeline");
								if (n != null) {
									notifyShare(request, id, user, new JsonArray().add(n), true);
								} else {
									n = new JsonObject();
								}
								n.putNumber("number", number+1).putNumber("number-errors", errorsNb);
								renderJson(request, n);
							} else {
								JsonObject error = new JsonObject().putString("error", event.left().getValue());
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
					JsonObject error = new JsonObject().putString("error", event.left().getValue());
					renderJson(request, error, 400);
				}
			}
		};

		folderService.shareFolderAction(id, user, actions, groupId, userId, shareService, remove, subHandler);

	}

	@Put("/share/json/:id")
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void shareJsonSubmit(final HttpServerRequest request) {
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
						mongo.findOne(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject().putString("_id", id), new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if ("ok".equals(event.body().getString("status")) && event.body().getObject("result") != null) {
									final boolean isFolder = !event.body().getObject("result").containsField("file");
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
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void removeShare(final HttpServerRequest request) {
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
				getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos user) {
						if (user != null) {
							mongo.findOne(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject().putString("_id", id), new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> event) {
									if ("ok".equals(event.body().getString("status")) && event.body().getObject("result") != null) {
										final boolean isFolder = !event.body().getObject("result").containsField("file");
										if(isFolder)
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
		.putString("uri", container.config().getString("userbook-host") +
				"/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
		.putString("username", user.getUsername())
		.putString("resourceUri", container.config().getString("host", "http://localhost:8011") +
				pathPrefix + "/document/" + resource)
		.putString("appPrefix", pathPrefix+"/workspace");

		if(isFolder){
			params.putString("resourceUri", container.config().getString("host", "http://localhost:8011") +
					pathPrefix + "/workspace#/shared/folder/" + resource);
		} else {
			params.putString("resourceUri", container.config().getString("host", "http://localhost:8011") +
					pathPrefix + "/document/" + resource);
		}

		final String template = isFolder ? "notify-share-folder.html" : "notify-share.html";

		mongo.findOne(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject().putString("_id", resource),
				new JsonObject().putNumber("name", 1), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) && event.body().getObject("result") != null) {
					params.putString("resourceName", event.body().getObject("result").getString("name", ""));
					notification.notifyTimeline(request, user, WORKSPACE_NAME, WORKSPACE_NAME + "_SHARE",
							recipients, resource, template, params);
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
					doc.putString("created", now);
					doc.putString("modified", now);
					doc.putString("owner", userInfos.getUserId());
					doc.putString("ownerName", userInfos.getUsername());
					String application = request.params().get("application");
					String protectedContent = request.params().get("protected");
					if (application != null && !application.trim().isEmpty() &&
							"true".equals(protectedContent)) {
						doc.putBoolean("protected", true);
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
							for (String attr : j.getFieldNames()) {
								UserUtils.addSessionAttribute(eb, userInfos.getUserId(), attr, j.getLong(attr), null);
							}
							emptySizeHandler.handle(quota - storage);
						}
					}
				}
			});
		}
	}

	private void add(final HttpServerRequest request, final String mongoCollection,
			final JsonObject doc, long allowedSize) {
		FileUtils.gridfsWriteUploadFile(request, eb, gridfsAddress, allowedSize, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject uploaded) {
				if ("ok".equals(uploaded.getString("status"))) {
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
				} else {
					badRequest(request, uploaded.getString("message"));
				}
			}
		});
	}

	private void addAfterUpload(final JsonObject uploaded, final JsonObject doc, String name, String application,
			final List<String> thumbs, final String mongoCollection, final Handler<Message<JsonObject>> handler) {
		doc.putString("name", getOrElse(name, uploaded.getObject("metadata")
				.getString("filename"), false));
		doc.putObject("metadata", uploaded.getObject("metadata"));
		doc.putString("file", uploaded.getString("_id"));
		doc.putString("application", getOrElse(application, WORKSPACE_NAME)); // TODO check if application name is valid
		mongo.save(mongoCollection, doc, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					incrementStorage(doc);
					createThumbnailIfNeeded(mongoCollection, uploaded,
							res.body().getString("_id"), null, thumbs);
				}
				if (handler != null) {
					handler.handle(res);
				}
			}
		});
	}

	private void incrementStorage(JsonObject added) {
		updateStorage(new JsonArray().add(added), null);
	}

	private void decrementStorage(JsonObject removed) {
		updateStorage(null, new JsonArray().add(removed));
	}

	private void incrementStorage(JsonArray added) {
		updateStorage(added, null);
	}

	private void decrementStorage(JsonArray removed) {
		updateStorage(null, removed);
	}

	private void updateStorage(JsonObject added, JsonObject removed) {
		updateStorage(new JsonArray().add(added), new JsonArray().add(removed));
	}

	private void updateStorage(JsonArray addeds, JsonArray removeds) {
		Map<String, Long> sizes = new HashMap<>();
		if (addeds != null) {
			for (Object o : addeds) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject added = (JsonObject) o;
				Long size = added.getObject("metadata", new JsonObject()).getLong("size", 0l);
				String userId = (added.containsField("to")) ? added.getString("to") : added.getString("owner");
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
				Long size = removed.getObject("metadata", new JsonObject()).getLong("size", 0l);
				String userId = (removed.containsField("to")) ? removed.getString("to") : removed.getString("owner");
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
				}
			});
		}
	}

	private void notifyEmptySpaceIsSmall(String userId) {
		List<String> recipients = new ArrayList<>();
		recipients.add(userId);
		notification.notifyTimeline(new JsonHttpServerRequest(new JsonObject()), null, WORKSPACE_NAME,
				WORKSPACE_NAME + "_STORAGE", recipients, null, "notify-storage.html", null);
	}

	@Post("/folder")
	@SecuredAction("workspace.folder.add")
	public void addFolder(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final String name = request.formAttributes().get("name");
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
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final String id = request.params().get("id");
				final String path = request.formAttributes().get("path");
				final String name = request.formAttributes().get("name");
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
																.putNumber("number", r.right().getValue().size()));
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
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final String id = request.params().get("id");
				String p;
				try {
					p = request.formAttributes().get("path");
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
								decrementStorage(r.right().getValue());
								renderJson(request, new JsonObject()
										.putNumber("number", r.right().getValue().size()), 204);
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

	private void createThumbnailIfNeeded(final String collection, JsonObject srcFile,
			final String documentId, JsonObject oldThumbnail, List<String> thumbs) {
		if (documentId != null && thumbs != null && !documentId.trim().isEmpty() && !thumbs.isEmpty() &&
				srcFile != null && isImage(srcFile) && srcFile.getString("_id") != null) {
			Pattern size = Pattern.compile("([0-9]+)x([0-9]+)");
			JsonArray outputs = new JsonArray();
			for (String thumb: thumbs) {
				Matcher m = size.matcher(thumb);
				if (m.matches()) {
					try {
						int width = Integer.parseInt(m.group(1));
						int height = Integer.parseInt(m.group(2));
						if (width == 0 && height == 0) continue;
						JsonObject j = new JsonObject().putString("dest", "gridfs://fs");
						if (width != 0) {
							j.putNumber("width", width);
						}
						if (height != 0) {
							j.putNumber("height", height);
						}
						outputs.addObject(j);
					} catch (NumberFormatException e) {
						log.error("Invalid thumbnail size.", e);
					}
				}
			}
			if (outputs.size() > 0) {
				JsonObject json = new JsonObject()
						.putString("action", "resizeMultiple")
						.putString("src", "gridfs://fs:" + srcFile.getString("_id"))
						.putArray("destinations", outputs);
				eb.send(imageResizerAddress, json, new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						JsonObject thumbnails = event.body().getObject("outputs");
						if ("ok".equals(event.body().getString("status")) && thumbnails != null) {
							mongo.update(collection, new JsonObject().putString("_id", documentId),
									new JsonObject().putObject("$set", new JsonObject()
											.putObject("thumbnails", thumbnails)));
						}
				   }
				});
			}
		}
		if (oldThumbnail != null) {
			for (String attr: oldThumbnail.getFieldNames()) {
				FileUtils.gridfsRemoveFile(oldThumbnail.getString(attr), eb, gridfsAddress, null);
			}
		}
	}

	@Put("document/:id")
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void updateDocument(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					request.pause();
					emptySize(user, new Handler<Long>() {
						@Override
						public void handle(Long emptySize) {
							request.resume();
							FileUtils.gridfsWriteUploadFile(request, eb, gridfsAddress, emptySize,
									new Handler<JsonObject>() {
								@Override
								public void handle(final JsonObject uploaded) {
									if ("ok".equals(uploaded.getString("status"))) {
										updateAfterUpload(request.params().get("id"), request.params().get("name"),
												uploaded, request.params().getAll("thumbnail"), new Handler<Message<JsonObject>>() {
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
									} else {
										badRequest(request, uploaded.getString("message"));
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

	private void updateAfterUpload(final String id, final String name, final JsonObject uploaded,
			final List<String> t, final Handler<Message<JsonObject>> handler) {
		documentDao.findById(id, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject old) {
				if ("ok".equals(old.getString("status"))) {
					JsonObject metadata = uploaded.getObject("metadata");
					JsonObject set = new JsonObject();
					final JsonObject doc = new JsonObject();
					doc.putString("name", getOrElse(name, metadata.getString("filename")));
					final String now = MongoDb.formatDate(new Date());
					doc.putString("modified", now);
					doc.putObject("metadata", metadata);
					doc.putString("file", uploaded.getString("_id"));
					final JsonObject thumbs = old.getObject("result", new JsonObject())
							.getObject("thumbnails");
					if (thumbs != null) {
						doc.putObject("thumbnails", new JsonObject());
					}
					String query = "{ \"_id\": \"" + id + "\"}";
					set.putObject("$set", doc);
					mongo.update(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(query), set,
							new Handler<Message<JsonObject>>() {
						@Override
						public void handle(final Message<JsonObject> res) {
							String status = res.body().getString("status");
							JsonObject result = old.getObject("result");
							if ("ok".equals(status) && result != null) {
								updateStorage(doc, old);
								FileUtils.gridfsRemoveFile(
										result.getString("file"), eb, gridfsAddress,
										new Handler<JsonObject>() {

											@Override
											public void handle(JsonObject event) {
												if (handler != null) {
													handler.handle(res);
												}
											}
										});
								createThumbnailIfNeeded(DocumentDao.DOCUMENTS_COLLECTION,
										uploaded, id, thumbs, t);
							} else {
								if (handler != null) {
									handler.handle(res);
								}
							}
						}
					});
				} else if (handler != null) {
					handler.handle(null);
				}
			}
		});
	}

	@Get("/document/:id")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void getDocument(HttpServerRequest request) {
		getFile(request, documentDao, null);
	}

	private void getFile(final HttpServerRequest request, GenericDao dao, String owner) {
		dao.findById(request.params().get("id"), owner, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				String status = res.getString("status");
				JsonObject result = res.getObject("result");
				String thumbSize = request.params().get("thumbnail");
				if ("ok".equals(status) && result != null) {
					String file;
					if (thumbSize != null && !thumbSize.trim().isEmpty()) {
						file = result.getObject("thumbnails", new JsonObject())
								.getString(thumbSize, result.getString("file"));
					} else {
						file = result.getString("file");
					}
					if (file != null && !file.trim().isEmpty()) {
						boolean inline = inlineDocumentResponse(result, request.params().get("application"));
						if (inline && ETag.check(request, file)) {
							notModified(request, file);
						} else {
							FileUtils.gridfsSendFile(file,
									result.getString("name"), eb, gridfsAddress, request.response(),
									inline, result.getObject("metadata"));
						}
						eventStore.createAndStoreEvent(WokspaceEvent.GET_RESOURCE.name(), request,
								new JsonObject().putString("resource", request.params().get("id")));
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
		JsonObject metadata = doc.getObject("metadata");
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
		JsonObject metadata = doc.getObject("metadata");
		return metadata != null && (
				"image/jpeg".equals(metadata.getString("content-type")) ||
						"image/gif".equals(metadata.getString("content-type")) ||
						"image/png".equals(metadata.getString("content-type")) ||
						"image/tiff".equals(metadata.getString("content-type"))
		);
	}

	@Delete("/document/:id")
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void deleteDocument(HttpServerRequest request) {
		deleteFile(request, documentDao, null);
	}

	private void deleteFile(final HttpServerRequest request, final GenericDao dao, String owner) {
		final String id = request.params().get("id");
		dao.findById(id, owner, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				String status = res.getString("status");
				final JsonObject result = res.getObject("result");
				if ("ok".equals(status) && result != null && result.getString("file") != null) {
					FileUtils.gridfsRemoveFile(result.getString("file"), eb, gridfsAddress,
							new Handler<JsonObject>() {

						@Override
						public void handle(JsonObject event) {
							if (event != null && "ok".equals(event.getString("status"))) {
								dao.delete(id, new Handler<JsonObject>() {
									@Override
									public void handle(JsonObject result2) {
										if ("ok".equals(result2.getString("status"))) {
											decrementStorage(result);
											renderJson(request, result2, 204);
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
			JsonArray idsArray = new JsonArray(ids.split(","));
			String criteria = "{ \"_id\" : { \"$in\" : " + idsArray.encode() + "}";
			if (owner != null) {
				criteria += ", \"to\" : \"" + owner + "\"";
			}
			criteria += "}";
			mongo.find(collection, new JsonObject(criteria), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> r) {
					JsonObject src = r.body();
					if ("ok".equals(src.getString("status")) && src.getArray("results") != null) {
						final JsonArray origs = src.getArray("results");
						final JsonArray insert = new JsonArray();
						final AtomicInteger number = new AtomicInteger(origs.size());
						emptySize(user, new Handler<Long>() {
							@Override
							public void handle(Long emptySize) {
								long size = 0;
								for (Object o: origs) {
									if (!(o instanceof JsonObject)) continue;
									JsonObject metadata = ((JsonObject) o).getObject("metadata");
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
									dest.removeField("_id");
									dest.removeField("protected");
									dest.removeField("comments");
									dest.putString("application", WORKSPACE_NAME);
									if (owner != null) {
										dest.putString("owner", owner);
										dest.putString("ownerName", dest.getString("toName"));
										dest.removeField("to");
										dest.removeField("from");
										dest.removeField("toName");
										dest.removeField("fromName");
									} else if (user != null) {
										dest.putString("owner", user.getUserId());
										dest.putString("ownerName", user.getUsername());
										dest.putArray("shared", new JsonArray());
									}
									dest.putString("created", now);
									dest.putString("modified", now);
									if (folder != null && !folder.trim().isEmpty()) {
										dest.putString("folder", folder);
									} else {
										dest.removeField("folder");
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

										QueryBuilder parentFolderQuery = QueryBuilder.start("owner").is(owner == null ? user.getUserId() : owner)
												.and("folder").is(parentFolder)
												.and("name").is(parentName);

										mongo.findOne(DOCUMENTS_COLLECTION,  MongoQueryBuilder.build(parentFolderQuery), new Handler<Message<JsonObject>>(){
											@Override
											public void handle(Message<JsonObject> event) {
												if ("ok".equals(event.body().getString("status"))){
													JsonObject parent = event.body().getObject("result");
													if(parent != null && parent.getArray("shared") != null && parent.getArray("shared").size() > 0)
														dest.putArray("shared", parent.getArray("shared"));

													if (filePath != null) {
														FileUtils.gridfsCopyFile(filePath, eb, gridfsAddress, new Handler<JsonObject>() {
															@Override
															public void handle(JsonObject event) {
																if (event != null && "ok".equals(event.getString("status"))) {
																	dest.putString("file", event.getString("_id"));
																	persist(insert, number.decrementAndGet());
																}
															}
														});
													} else {
														persist(insert, number.decrementAndGet());
													}
												} else {
													renderJson(request, event.body(), 404);
												}
											}
										});

									} else if (filePath != null) {
										FileUtils.gridfsCopyFile(filePath, eb, gridfsAddress, new Handler<JsonObject>() {

											@Override
											public void handle(JsonObject event) {
												if (event != null && "ok".equals(event.getString("status"))) {
													dest.putString("file", event.getString("_id"));
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
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
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
				if ("ok".equals(src.getString("status")) && src.getObject("result") != null) {
					JsonObject orig = src.getObject("result");
					if (orig.getObject("metadata", new JsonObject()).getLong("size", 0l) > emptySize) {
						badRequest(request, "file.too.large");
						return;
					}
					final JsonObject dest = orig.copy();
					String now = MongoDb.formatDate(new Date());
					dest.removeField("_id");
					dest.removeField("protected");
					dest.putString("application", WORKSPACE_NAME);
					if (owner != null) {
						dest.putString("owner", owner);
						dest.putString("ownerName", dest.getString("toName"));
						dest.removeField("to");
						dest.removeField("from");
						dest.removeField("toName");
						dest.removeField("fromName");
					}
					dest.putString("created", now);
					dest.putString("modified", now);
					String folder = getOrElse(request.params().get("folder"), "");
					try {
						folder = URLDecoder.decode(folder, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						log.warn(e.getMessage(), e);
					}
					dest.putString("folder", folder);
					String filePath = orig.getString("file");
					if (filePath != null) {
						FileUtils.gridfsCopyFile(filePath, eb, gridfsAddress, new Handler<JsonObject>() {

							@Override
							public void handle(JsonObject event) {
								if (event != null && "ok".equals(event.getString("status"))) {
									dest.putString("file", event.getString("_id"));
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
					final  String relativePath = relativePath2;
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
								JsonArray values = res.body().getArray("values", new JsonArray("[]"));
								JsonArray out = values;
								if (hierarchical != null) {
									Set<String> folders = new HashSet<String>();
									for (Object value: values) {
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
									out = new JsonArray(folders.toArray());
								}
								renderJson(request, out);
							} else {
								renderJson(request, new JsonArray());
							}
						}
					});
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@Post("/document/:id/comment")
	@SecuredAction(value = "workspace.comment", type = ActionType.RESOURCE)
	public void commentDocument(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					request.expectMultiPart(true);
					request.endHandler(new VoidHandler() {
						@Override
						protected void handle() {
							String comment = request.formAttributes().get("comment");
							if (comment != null && !comment.trim().isEmpty()) {
								String query = "{ \"$push\" : { \"comments\":" +
										" {\"author\" : \"" + user.getUserId() + "\", " +
										"\"authorName\" : \"" + user.getUsername() +
										"\", \"posted\" : \"" + MongoDb.formatDate(new Date()) +
										"\", \"comment\": \"" + comment + "\" }}}";
								documentDao.update(request.params().get("id"), new JsonObject(query),
										new Handler<JsonObject>() {
									@Override
									public void handle(JsonObject res) {
										if ("ok".equals(res.getString("status"))) {
											renderJson(request, res);
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

	@Put("/document/move/:id/:folder")
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
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
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
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
					String obj2 = "{ \"$set\" : { \"folder\": \"" + folder +
							"\", \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\" }}";
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
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void moveDocuments(final HttpServerRequest request) {
		final String ids = request.params().get("ids"); // TODO refactor with json in request body
		String tempFolder = getOrElse(request.params().get("folder"), "");
		try {
			tempFolder = URLDecoder.decode(tempFolder, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.warn(e.getMessage(), e);
		}
		final String folder = tempFolder;

		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					if (ids != null && !ids.trim().isEmpty()) {
						JsonArray idsArray = new JsonArray(ids.split(","));
						final String criteria = "{ \"_id\" : { \"$in\" : " + idsArray.encode() + "}}";

						if(folder != null && !folder.trim().isEmpty()){

							//If the document has a parent folder, replicate sharing rights
							String parentName, parentFolder;
							if(folder.lastIndexOf('_') < 0){
								parentName = folder;
								parentFolder = folder;
							} else {
								String[] splittedPath = folder.split("_");
								parentName = splittedPath[splittedPath.length - 1];
								parentFolder = folder;
							}

							QueryBuilder parentFolderQuery = QueryBuilder.start("owner").is(user.getUserId())
									.and("folder").is(parentFolder)
									.and("name").is(parentName);

							mongo.findOne(DOCUMENTS_COLLECTION,  MongoQueryBuilder.build(parentFolderQuery), new Handler<Message<JsonObject>>(){
								@Override
								public void handle(Message<JsonObject> event) {
									if ("ok".equals(event.body().getString("status"))){
										JsonObject parent = event.body().getObject("result");
										String obj = "{ \"$set\" : { \"folder\": \"" + folder +
													"\", \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\"";
										if(parent.getArray("shared") != null && parent.getArray("shared").size() > 0)
											obj += ", \"shared\" : "+parent.getArray("shared").toString()+" }}";
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
									} else {
										renderJson(request, event.body(), 404);
									}
								}
							});

						} else {
							String obj;
							if (folder != null && !folder.trim().isEmpty()) {
								obj = "{ \"$set\" : { \"folder\": \"" + folder +
										"\", \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\" }, " +
										" \"$unset\" : { \"shared\": 1 }}";
							} else {
								obj = "{ \"$set\" : { \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\" }, " +
										" \"$unset\" : { \"folder\" : 1, \"shared\": 1 }}";
							}
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
							JsonArray results = res.body().getArray("results");
							if ("ok".equals(status) && results != null) {
								renderJson(request, results);
							} else {
								renderJson(request, new JsonArray());
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
							JsonArray results = res.body().getArray("results");
							if ("ok".equals(status) && results != null) {
								renderJson(request, results);
							} else {
								renderJson(request, new JsonArray());
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
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
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
						JsonObject doc = res.getObject("result");
						if (doc.getString("old-folder") != null) {
							doc.putString("folder", doc.getString("old-folder"));
						} else {
							doc.removeField("folder");
						}
						doc.removeField("old-folder");
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
			default:
				message.reply(new JsonObject().putString("status", "error")
						.putString("message", "invalid.action"));
		}
	}

	private void addDocument(final Message<JsonObject> message) {
		JsonObject uploaded = message.body().getObject("uploaded");
		JsonObject doc = message.body().getObject("document");
		if (doc == null || uploaded == null) {
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "missing.attribute"));
			return;
		}
		String name = message.body().getString("name");
		String application = message.body().getString("application");
		JsonArray t = message.body().getArray("thumbs", new JsonArray());
		List<String> thumbs = new ArrayList<>();
		for (int i = 0; i < t.size(); i++) {
			thumbs.add((String) t.get(i));
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
		JsonObject uploaded = message.body().getObject("uploaded");
		String id = message.body().getString("id");
		if (uploaded == null || id == null || id.trim().isEmpty()) {
			message.reply(new JsonObject().putString("status", "error")
					.putString("message", "missing.attribute"));
			return;
		}
		String name = message.body().getString("name");
		JsonArray t = message.body().getArray("thumbs", new JsonArray());
		List<String> thumbs = new ArrayList<>();
		for (int i = 0; i < t.size(); i++) {
			thumbs.add((String) t.get(i));
		}
		updateAfterUpload(id, name, uploaded, thumbs, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				if (m != null) {
					message.reply(m.body());
				}
			}
		});
	}

	public void setQuotaService(QuotaService quotaService) {
		this.quotaService = quotaService;
	}

	@Put("/folder/rename")
	@SecuredAction(value = "workspace.folder.rename")
	public void renameFolder(final HttpServerRequest request){
		RequestUtils.bodyToJson(request, pathPrefix + "rename", new Handler<JsonObject>() {
			public void handle(final JsonObject body) {
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					public void handle(UserInfos userInfos) {
						if(userInfos != null){
							String id = body.getString("id");
							String name = body.getString("name");
							folderService.rename(id, name, userInfos, defaultResponseHandler(request));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});

	}

	@Put("/rename/document")
	@SecuredAction(value = "workspace.document.rename")
	public void renameDocument(final HttpServerRequest request){
		RequestUtils.bodyToJson(request, pathPrefix + "rename", new Handler<JsonObject>() {
			public void handle(final JsonObject body) {
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					public void handle(UserInfos userInfos) {
						if(userInfos != null){
							String id = body.getString("id");
							String name = body.getString("name");

							final QueryBuilder matcher = QueryBuilder.start("_id").is(id).put("owner").is(userInfos.getUserId()).and("file").exists(true);
							MongoUpdateBuilder modifier = new MongoUpdateBuilder();
							modifier.set("name", name);

							mongo.update(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(matcher), modifier.build(), MongoDbResult.validResultHandler(defaultResponseHandler(request)));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

}
