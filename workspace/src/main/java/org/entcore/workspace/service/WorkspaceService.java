package org.entcore.workspace.service;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.user.UserUtils.getUserInfos;
import static fr.wseduc.webutils.Utils.getOrElse;

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

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import org.entcore.common.http.request.ActionsUtils;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.share.ShareService;
import org.entcore.common.share.impl.MongoDbShareService;
import fr.wseduc.webutils.*;
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

import org.entcore.common.neo4j.Neo;
import fr.wseduc.webutils.http.ETag;
import org.entcore.common.user.UserUtils;
import org.entcore.common.user.UserInfos;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.dao.GenericDao;
import org.entcore.workspace.dao.RackDao;

public class WorkspaceService extends Controller {

	public static final String WORKSPACE_NAME = "WORKSPACE";
	private final String gridfsAddress;
	private final String imageResizerAddress;
	private final MongoDb mongo;
	private final DocumentDao documentDao;
	private final RackDao rackDao;
	private final Neo neo;
	private final TimelineHelper notification;
	private final TracerHelper trace;
	private final ShareService shareService;
	private final FolderService folderService;

	public WorkspaceService(Vertx vertx, Container container, RouteMatcher rm, TracerHelper trace,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		mongo = new MongoDb(Server.getEventBus(vertx),
				container.config().getString("mongo-address", "wse.mongodb.persistor"));
		gridfsAddress = container.config().getString("gridfs-address", "wse.gridfs.persistor");
		imageResizerAddress = container.config().getString("image-resizer-address", "wse.image.resizer");
		documentDao = new DocumentDao(mongo);
		rackDao = new RackDao(mongo);
		neo = new Neo(eb, log);
		notification = new TimelineHelper(vertx, eb, container);
		this.trace = trace;
		this.shareService = new MongoDbShareService(eb, mongo, "documents", securedActions, null);
		this.folderService = new DefaultFolderService(eb, mongo, gridfsAddress);
	}

	@SecuredAction("workspace.view")
	public void view(HttpServerRequest request) {
		renderView(request);
	}
	
	public void scrapbook(HttpServerRequest request) {
		renderView(request);
	}

	@SecuredAction("workspace.share.json")
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
					isOwner(DocumentDao.DOCUMENTS_COLLECTION, id, user, new Handler<Boolean>() {
						@Override
						public void handle(Boolean event) {
							if (Boolean.TRUE.equals(event)) {
								shareService.shareInfos(user.getUserId(), id, defaultResponseHandler(request));
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

	@SecuredAction("workspace.share.json")
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
						if (user != null) {
							isOwner(DocumentDao.DOCUMENTS_COLLECTION, id, user, new Handler<Boolean>() {
								@Override
								public void handle(Boolean event) {
									if (Boolean.TRUE.equals(event)) {
										Handler<Either<String, JsonObject>> r = new Handler<Either<String, JsonObject>>() {
											@Override
											public void handle(Either<String, JsonObject> event) {
												if (event.isRight()) {
													JsonObject n = event.right().getValue()
															.getObject("notify-timeline");
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

	@SecuredAction("workspace.share.json")
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
							isOwner(DocumentDao.DOCUMENTS_COLLECTION, id, user, new Handler<Boolean>() {
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
		QueryBuilder query = QueryBuilder.start("_id").is(documentId).put("owner").is(user.getUserId());
		mongo.count(collection, MongoQueryBuilder.build(query), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject res = event.body();
				handler.handle(res != null && "ok".equals(res.getString("status")) && 1 == res.getInteger("count"));
			}
		});
	}

	private void notifyShare(final HttpServerRequest request, final String resource,
				final UserInfos user, JsonArray sharedArray) {
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
								sendNotify(request, resource, user, recipients);
							}
						}
					});
				}
			}
		}
		if (remaining.get() < 1) {
			sendNotify(request, resource, user, recipients);
		}
	}

	private void sendNotify(final HttpServerRequest request, final String resource,
			final UserInfos user, final List<String> recipients) {
		final JsonObject params = new JsonObject()
		.putString("uri", container.config().getString("userbook-host") +
				"/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
		.putString("username", user.getUsername())
		.putString("resourceUri", container.config().getString("host", "http://localhost:8011") +
				pathPrefix + "/document/" + resource);
		mongo.findOne(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject().putString("_id", resource),
				new JsonObject().putNumber("name", 1), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) && event.body().getObject("result") != null) {
					params.putString("resourceName", event.body().getObject("result").getString("name", ""));
					notification.notifyTimeline(request, user, WORKSPACE_NAME, WORKSPACE_NAME + "_SHARE",
							recipients, resource, "notify-share.html", params);
				} else {
					log.error("Unable to send timeline notification : missing name on resource " + resource);
				}
			}
		});
	}

	@SecuredAction("workspace.document.add")
	public void addDocument(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(final UserInfos userInfos) {
				if (userInfos != null) {
					JsonObject doc = new JsonObject();
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
					add(request, DocumentDao.DOCUMENTS_COLLECTION, doc);
				} else {
					request.response().setStatusCode(401).end();
				}
			}
		});
	}

	@SecuredAction("workspace.rack.document.add")
	public void addRackDocument(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(final UserInfos userInfos) {
				if (userInfos != null) {
					final String to = request.params().get("to");
					if (to != null && !to.trim().isEmpty()) {
						String query =
								"MATCH (n:User) " +
								"WHERE n.id = {id} " +
								"RETURN count(n) as nb, n.displayName as username";
						Map<String, Object> params = new HashMap<>();
						params.put("id", to);
						request.pause();
						neo.send(query, params, new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> res) {
								request.resume();
								if ("ok".equals(res.body().getString("status")) &&
										"1".equals(res.body().getObject("result")
												.getObject("0").getString("nb"))) {
									JsonObject doc = new JsonObject();
									doc.putString("to", to);
									doc.putString("toName", res.body().getObject("result")
											.getObject("0").getString("username"));
									doc.putString("from", userInfos.getUserId());
									doc.putString("fromName", userInfos.getUsername());
									String now = MongoDb.formatDate(new Date());
									doc.putString("sent", now);
									add(request, RackDao.RACKS_COLLECTION, doc);
								} else {
									badRequest(request);
								}
							}
						});
					} else {
						badRequest(request);
					}
				} else {
					request.response().setStatusCode(401).end();
				}
			}
		});
	}

	private void add(final HttpServerRequest request, final String mongoCollection,
			final JsonObject doc) {
		FileUtils.gridfsWriteUploadFile(request, eb, gridfsAddress, new Handler<JsonObject>() {
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
				}
			}
		});
	}

	private void addAfterUpload(final JsonObject uploaded, JsonObject doc, String name, String application,
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
					createThumbnailIfNeeded(mongoCollection, uploaded,
							res.body().getString("_id"), null, thumbs);
				}
				if (handler != null) {
					handler.handle(res);
				}
			}
		});
	}

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
							folderService.copy(id, name, path, userInfos,
									defaultResponseHandler(request));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	@SecuredAction(value = "workspace.folder.move", type = ActionType.AUTHENTICATED)
	public void moveFolder(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				final String id = request.params().get("id");
				final String path = request.formAttributes().get("path");
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
					folderService.delete(id, userInfos, defaultResponseHandler(request, 204));
				} else {
					unauthorized(request);
				}
	  		 }
		});
	}

	@SecuredAction(value = "workspace.folders.list", type = ActionType.AUTHENTICATED)
	public void folders(final HttpServerRequest request) {
		final String path = request.params().get("path");
		final boolean hierarchical = request.params().get("hierarchical") != null;
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos userInfos) {
				if (userInfos != null) {
					folderService.list(path, userInfos, hierarchical, arrayResponseHandler(request));
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

	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void updateDocument(final HttpServerRequest request) {
		FileUtils.gridfsWriteUploadFile(request, eb, gridfsAddress, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject uploaded) {
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
					JsonObject doc = new JsonObject();
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

	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void getDocument(HttpServerRequest request) {
		getFile(request, documentDao, null);
	}

	@SecuredAction("workspace.rack.document.get")
	public void getRackDocument(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					getFile(request, rackDao, user.getUserId());
				} else {
					unauthorized(request);
				}
			}
		});
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

	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void deleteDocument(HttpServerRequest request) {
		deleteFile(request, documentDao, null);
	}

	@SecuredAction("workspace.rack.document.delete")
	public void deleteRackDocument(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					deleteFile(request, rackDao, user.getUserId());
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void deleteFile(final HttpServerRequest request, final GenericDao dao, String owner) {
		final String id = request.params().get("id");
		dao.findById(id, owner, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				String status = res.getString("status");
				JsonObject result = res.getObject("result");
				if ("ok".equals(status) && result != null && result.getString("file") != null) {
					FileUtils.gridfsRemoveFile(result.getString("file"), eb, gridfsAddress,
							new Handler<JsonObject>() {

						@Override
						public void handle(JsonObject event) {
							if (event != null && "ok".equals(event.getString("status"))) {
								dao.delete(id, new Handler<JsonObject>() {
									@Override
									public void handle(JsonObject result) {
										if ("ok".equals(result.getString("status"))) {
											renderJson(request, result, 204);
										} else {
											renderError(request, result);
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

	@SecuredAction("workspace.rack.documents.copy")
	public void copyRackDocuments(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					copyFiles(request, RackDao.RACKS_COLLECTION, user.getUserId(), user);
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void copyFiles(final HttpServerRequest request, final String collection,
				final String owner, final UserInfos user) {
		String ids = request.params().get("ids"); // TODO refactor with json in request body
		final String folder = request.params().get("folder");
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
						JsonArray origs = src.getArray("results");
						final JsonArray insert = new JsonArray();
						final AtomicInteger number = new AtomicInteger(origs.size());
						for (Object o: origs) {
							JsonObject orig = (JsonObject) o;
							final JsonObject dest = orig.copy();
							String now = MongoDb.formatDate(new Date());
							dest.removeField("_id");
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
							String filePath = orig.getString("file");
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
						}
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

	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void copyDocument(HttpServerRequest request) {
		copyFile(request, documentDao, null);
	}

	@SecuredAction("workspace.document.copy")
	public void copyRackDocument(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					copyFile(request, rackDao, user.getUserId());
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void copyFile(final HttpServerRequest request, final GenericDao dao, final String owner) {
		dao.findById(request.params().get("id"), owner, new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject src) {
				if ("ok".equals(src.getString("status")) && src.getObject("result") != null) {
					JsonObject orig = src.getObject("result");
					final JsonObject dest = orig.copy();
					String now = MongoDb.formatDate(new Date());
					dest.removeField("_id");
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
					dest.putString("folder", request.params().get("folder"));
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
		if (user.getProfilGroupsIds() != null) {
			for (String groupId: user.getProfilGroupsIds()) {
				sb.append(", { \"groupId\": \"" + groupId + "\" }");
			}
		}
		return "{ \"$or\" : [{ \"userId\": \"" + user.getUserId() + "\" }" + sb.toString() + "]}";
	}

	@SecuredAction("workspace.document.list.folders")
	public void listFolders(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					final String hierarchical = request.params().get("hierarchical");
					final String relativePath = request.params().get("relativePath");
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

	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void moveDocument(final HttpServerRequest request) {
		moveOne(request, request.params().get("folder"), documentDao, null);
	}

	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void moveTrash(final HttpServerRequest request) {
		moveOne(request, "Trash", documentDao, null);
	}

	@SecuredAction("workspace.rack.document.move.trash")
	public void moveTrashRack(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					moveOne(request, "Trash", rackDao, user.getUserId());
				} else {
					unauthorized(request);
				}
			}
		});
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

	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void moveDocuments(final HttpServerRequest request) {
		String ids = request.params().get("ids"); // TODO refactor with json in request body
		String folder = request.params().get("folder");
		if (ids != null && !ids.trim().isEmpty()) {
			JsonArray idsArray = new JsonArray(ids.split(","));
			String criteria = "{ \"_id\" : { \"$in\" : " + idsArray.encode() + "}}";
			String obj;
			if (folder != null && !folder.trim().isEmpty()) {
				obj = "{ \"$set\" : { \"folder\": \"" + folder +
						"\", \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\" }}";
			} else {
				obj = "{ \"$set\" : { \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\" }, " +
						" \"$unset\" : { \"folder\" : 1 }}";
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
		} else {
			badRequest(request);
		}
	}

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
						query += "\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user) + "}";
					} else {
						query += "\"$or\" : [{ \"owner\": \"" + user.getUserId() +
								"\"}, {\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user) + "}}]";
					}
					String expectedFolder = request.params().get("folder");
					String forApplication = getOrElse(request.params()
							.get("application"), WorkspaceService.WORKSPACE_NAME);
					if (request.params().get("hierarchical") != null) {
						query += ", \"file\" : { \"$exists\" : true }, \"application\": \"" +
								forApplication + "\", \"folder\" : \"" + expectedFolder + "\" }";
					} else {
						query += ", \"file\" : { \"$exists\" : true }, \"application\": \"" +
								forApplication + "\", \"folder\" : { \"$regex\" : \"^" +
								expectedFolder + "(_|$)\" }}";
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

	@SecuredAction("workspace.rack.list.documents")
	public void listRackDocuments(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					String query = "{ \"to\": \"" + user.getUserId() + "\", "
							+ "\"file\" : { \"$exists\" : true }, "
							+ "\"folder\" : { \"$ne\" : \"Trash\" }}";
					mongo.find(RackDao.RACKS_COLLECTION, new JsonObject(query),
							new Handler<Message<JsonObject>>() {
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

	@SecuredAction("workspace.rack.list.trash.documents")
	public void listRackTrashDocuments(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					String query = "{ \"to\": \"" + user.getUserId() + "\", "
							+ "\"file\" : { \"$exists\" : true }, \"folder\" : \"Trash\" }";
					mongo.find(RackDao.RACKS_COLLECTION, new JsonObject(query), new Handler<Message<JsonObject>>() {
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

	@SecuredAction("workspace.rack.available.users")
	public void rackAvailableUsers(final HttpServerRequest request) {
		String customReturn =
				"MATCH visibles-[:IN]->g-[:AUTHORIZED]->r-[:AUTHORIZE]->a " +
				"WHERE has(a.name) AND a.name={action} " +
				"RETURN distinct visibles.id as id, visibles.displayName as username " +
				"ORDER BY username ";
		JsonObject params = new JsonObject()
		.putString("action", "org.entcore.workspace.service.WorkspaceService|listRackDocuments");
		UserUtils.findVisibleUsers(eb, request, false, customReturn, params,
				new Handler<JsonArray>() {

			@Override
			public void handle(JsonArray users) {
				renderJson(request, users);
			}
		});
	}

	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void restoreTrash(HttpServerRequest request) {
		restore(request, documentDao, null);
	}

	@SecuredAction("workspace.rack.restore")
	public void restoreTrashRack(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					restore(request, rackDao, user.getUserId());
				} else {
					unauthorized(request);
				}
			}
		});
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

	@SecuredAction(value = "workspace.habilitation", type = ActionType.AUTHENTICATED)
	public void getActionsInfos(final HttpServerRequest request) {
		ActionsUtils.findWorkflowSecureActions(eb, request, this);
	}

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

}
