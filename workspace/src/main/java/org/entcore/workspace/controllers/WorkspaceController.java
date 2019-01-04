package org.entcore.workspace.controllers;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.asyncArrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.asyncDefaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.user.UserUtils.getUserInfos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.ElementQuery.ElementSort;
import org.entcore.common.folders.ElementShareOperations;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.impl.DocumentHelper;
import org.entcore.common.http.request.ActionsUtils;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.share.impl.GenericShareService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.workspace.Workspace;
import org.entcore.workspace.service.WorkspaceService;
import org.vertx.java.core.http.RouteMatcher;

import fr.wseduc.bus.BusAddress;
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
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class WorkspaceController extends BaseController {
	private enum WokspaceEvent {
		ACCESS, GET_RESOURCE
	}

	public static final String GET_ACTION = "org-entcore-workspace-controllers-WorkspaceController|getDocument";
	public static final String COPY_ACTION = "org-entcore-workspace-controllers-WorkspaceController|copyDocuments";
	public static final String SHARED_ACTION = "org-entcore-workspace-controllers-WorkspaceController|shareResource";
	public static final String MEDIALIB_APP = "media-library";
	private EventStore eventStore;
	private WorkspaceService workspaceService;
	private TimelineHelper notification;
	private GenericShareService shareService;

	private Storage storage;

	public WorkspaceController(Storage storage, WorkspaceService workspaceService, GenericShareService shareService) {
		this.storage = storage;
		this.workspaceService = workspaceService;
		this.shareService = shareService;
	}

	@Post("/document")
	@SecuredAction("workspace.document.add")
	public void addDocument(final HttpServerRequest request) {

		UserUtils.getUserInfos(eb, request, userInfos -> {
			if (userInfos != null) {

				final JsonObject doc = new JsonObject();
				float quality = checkQuality(request.params().get("quality"));
				String name = request.params().get("name");
				List<String> thumbnail = request.params().getAll("thumbnail");
				String application = request.params().get("application");
				String protectedContent = request.params().get("protected");
				String publicContent = request.params().get("public");
				String parentId = request.params().get("parentId");
				if (application != null && !application.trim().isEmpty() && "true".equals(protectedContent)) {
					doc.put("protected", true);
				} else if (application != null && !application.trim().isEmpty() && "true".equals(publicContent)) {
					doc.put("public", true);
				}
				doc.put("eParent", parentId);
				request.pause();
				workspaceService.emptySize(userInfos, emptySize -> {
					request.resume();
					storage.writeUploadFile(request, emptySize, uploaded -> {
						if ("ok".equals(uploaded.getString("status"))) {
							workspaceService.addDocument(userInfos, quality, name, application, thumbnail, doc,
									uploaded, asyncDefaultResponseHandler(request, 201));
						} else {
							badRequest(request, uploaded.getString("message"));
						}
					});
				});
			} else {
				request.response().setStatusCode(401).end();
			}
		});
	}

	private void addDocument(final Message<JsonObject> message) {
		JsonObject uploaded = message.body().getJsonObject("uploaded");
		JsonObject doc = message.body().getJsonObject("document");
		String ownerId = doc.getString("owner");
		String ownerName = doc.getString("ownerName");
		if (doc == null || uploaded == null || ownerId == null || ownerName == null) {
			message.reply(new JsonObject().put("status", "error").put("message", "missing.attribute"));
			return;
		}
		String name = message.body().getString("name");
		String application = message.body().getString("application");
		JsonArray t = message.body().getJsonArray("thumbs", new fr.wseduc.webutils.collections.JsonArray());
		List<String> thumbs = new ArrayList<>();
		for (int i = 0; i < t.size(); i++) {
			thumbs.add(t.getString(i));
		}
		// TODO workspaceService.addDocument?
		workspaceService.addAfterUpload(uploaded, doc, name, application, thumbs, ownerId, ownerName, m -> {
			if (m.succeeded()) {
				message.reply(m.result().put("status", "ok"));
			} else {
				message.reply(new JsonObject().put("status", "error").put("message", m.cause().getMessage()));
			}
		});
	}

	@Post("/folder")
	@SecuredAction("workspace.folder.add")
	public void addFolder(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.endHandler(v -> {
			final String name = request.formAttributes().get("name");
			final String parentFolderId = request.formAttributes().get("parentFolderId");
			if (name == null || name.trim().isEmpty()) {
				badRequest(request);
				return;
			}
			UserUtils.getUserInfos(eb, request, userInfos -> {

				if (userInfos != null) {
					JsonObject folder = new JsonObject().put("name", name).put("application", MEDIALIB_APP);
					if (parentFolderId == null || parentFolderId.trim().isEmpty())
						workspaceService.createFolder(folder, userInfos, asyncDefaultResponseHandler(request, 201));
					else
						workspaceService.createFolder(parentFolderId, userInfos, folder,
								asyncDefaultResponseHandler(request, 201));
				} else {
					unauthorized(request);
				}
			});
		});
	}

	private float checkQuality(String quality) {
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

		return q;
	}

	private void comment(final HttpServerRequest request, boolean isFolder) {
		request.setExpectMultipart(true);
		request.endHandler(v -> {
			UserUtils.getUserInfos(eb, request, user -> {
				if (user != null) {
					String comment = request.formAttributes().get("comment");
					if (comment != null && !comment.trim().isEmpty()) {
						final String id = UUID.randomUUID().toString();
						workspaceService.addComment(request.params().get("id"), comment, user, res -> {
							if ("ok".equals(res.getString("status"))) {
								notifyComment(request, request.params().get("id"), user, isFolder);
								renderJson(request, res.put("id", id));
							} else {
								renderError(request, res);
							}
						});
					} else {
						badRequest(request);
					}
				} else {
					unauthorized(request);
				}
			});
		});
	}

	@Post("/document/:id/comment")
	@SecuredAction(value = "workspace.comment", type = ActionType.RESOURCE)
	public void commentDocument(final HttpServerRequest request) {
		comment(request, false);
	}

	@Post("/folder/:id/comment")
	@SecuredAction(value = "workspace.comment", type = ActionType.RESOURCE)
	public void commentFolder(final HttpServerRequest request) {
		comment(request, true);
	}

	@Post("/document/copy/:id/:folder")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void copyDocument(final HttpServerRequest request) {
		String id = request.params().get("id");
		String folder = request.params().get("folder");
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && user.getUserId() != null) {
				workspaceService.copy(id, Optional.ofNullable(folder), user, event -> {
					if (event.succeeded()) {
						renderJson(request, event.result());
					} else {
						badRequest(request, event.cause().getMessage());
					}
				});
			} else {
				unauthorized(request);
			}
		});
	}

	@Post("/folder/notify/contrib/:id")
	@SecuredAction(value = "workspace.contrib", type = ActionType.AUTHENTICATED)
	public void notifyContrib(final HttpServerRequest request) {
		String id = request.params().get("id");
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && user.getUserId() != null) {
				bodyToJson(request, body -> {
					Set<String> ids = body.getJsonArray("ids").stream().map(a -> (String) a)
							.collect(Collectors.toSet());
					Boolean addVersion = body.getBoolean("addVersion", false);
					// find receivers
					Future<Set<String>> futureRecipientIds = workspaceService.getNotifyContributorDest(id, user, ids);
					futureRecipientIds.compose(recipientIds -> {
						// find element to put in message
						if (recipientIds.isEmpty()) {
							return Future.succeededFuture(new JsonObject());
						}
						Future<JsonObject> futureFindResource = Future.future();
						String elementId = id;
						if (addVersion) {
							elementId = ids.iterator().next();
						}
						workspaceService.findById(elementId,
								new JsonObject().put("_id", 1).put("name", 1).put("eType", 1), event -> {
									if ("ok".equals(event.getString("status"))
											&& event.getJsonObject("result") != null) {
										futureFindResource.complete(event.getJsonObject("result"));
									} else {
										log.error("Unable to send timeline notification : missing name on resource "
												+ id);
										futureFindResource.fail("missing name or resource" + id);
									}
								});
						return futureFindResource;
					}).setHandler(ev -> {
						if (ev.succeeded()) {
							Set<String> recipientId = futureRecipientIds.result();
							JsonObject result = ev.result();
							// if no receivers return
							if (recipientId.isEmpty()) {
								created(request);
							} else {
								// if some receivers send and return
								String resourceName = result.getString("name", "");
								String resourceId = result.getString("_id");
								boolean isFolder = DocumentHelper.isFolder(result);
								final JsonObject params = new JsonObject()
										.put("userUri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
										.put("userName", user.getUsername())
										.put("appPrefix", pathPrefix + "/workspace");
								params.put("resourceUri", pathPrefix + "/workspace#/folder/" + id);
								params.put("resourceName", resourceName);
								if (addVersion) {
									final String notificationName = WorkspaceService.WORKSPACE_NAME.toLowerCase()
											+ ".contrib-version";
									notification.notifyTimeline(request, notificationName, user,
											new ArrayList<>(recipientId), resourceId, params);
									created(request);
								} else if (isFolder) {
									final String notificationName = WorkspaceService.WORKSPACE_NAME.toLowerCase()
											+ ".contrib-folder";
									notification.notifyTimeline(request, notificationName, user,
											new ArrayList<>(recipientId), resourceId, params);
									created(request);
								} else {
									badRequest(request, "id is not a folder" + id);
								}
							}
						} else {
							badRequest(request, ev.cause().getMessage());
						}
					});
				});
			} else {
				unauthorized(request);
			}
		});
	}

	private void copyDocumentFromBus(final Message<JsonObject> message) {
		String userId = message.body().getJsonObject("user").getString("userId");
		String fileId = message.body().getString("documentId");
		UserUtils.getSessionByUserId(eb, userId, session -> {
			workspaceService.copyUnsafe(fileId, Optional.empty(), UserUtils.sessionToUserInfos(session), res -> {
				if (res.succeeded())
					message.reply(res.result().getJsonObject(0));
				else
					message.fail(500, res.cause().getMessage());
			});
		});
	}

	@SuppressWarnings("unchecked")
	@Post("/documents/copy/:folder")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void copyDocuments(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && user.getUserId() != null) {
				bodyToJson(request, body -> {
					JsonArray ids = body.getJsonArray("ids");
					String folder = request.params().get("folder");
					if ("root".equals(folder)) {
						folder = null;
					}
					workspaceService.copyAll(ids.getList(), Optional.ofNullable(folder), user, event -> {
						if (event.succeeded()) {
							renderJson(request, event.result());
						} else {
							badRequest(request, event.cause().getMessage());
						}
					});
				});
			} else {
				unauthorized(request);
			}
		});
	}

	@Put("/folder/copy/:id")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void copyFolder(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.endHandler(v -> {
			final String id = request.params().get("id");
			final String parentFolderId = request.formAttributes().get("parentFolderId");
			if (StringUtils.isEmpty(id)) {
				badRequest(request);
				return;
			}
			UserUtils.getUserInfos(eb, request, userInfos -> {
				if (userInfos != null) {
					workspaceService.copy(id, Optional.ofNullable(parentFolderId), userInfos, event -> {
						if (event.succeeded()) {
							renderJson(request, event.result());
						} else {
							badRequest(request, event.cause().getMessage());
						}
					});
				} else {
					unauthorized(request);
				}
			});
		});
	}

	@Delete("/document/:id/comment/:commentId")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void deleteComment(final HttpServerRequest request) {
		final String id = request.params().get("id");
		final String commentId = request.params().get("commentId");

		workspaceService.deleteComment(id, commentId, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				if ("ok".equals(res.getString("status"))) {
					noContent(request);
				} else {
					renderError(request, res);
				}
			}
		});
	}

	@Delete("/document/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void deleteDocument(HttpServerRequest request) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		UserUtils.getUserInfos(eb, request, userInfos -> {
			if (userInfos != null) {
				workspaceService.delete(id, userInfos, event -> {
					if (event.succeeded()) {
						renderJson(request, event.result());
					} else {
						badRequest(request, event.cause().getMessage());
					}
				});
			} else {
				unauthorized(request);
			}
		});
	}

	@Delete("/folder/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void deleteFolder(final HttpServerRequest request) {
		this.deleteDocument(request);
	}

	@Delete("/documents")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void bulkDelete(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && user.getUserId() != null) {
				bodyToJson(request, body -> {
					Set<String> ids = body.getJsonArray("ids").stream().map(a -> (String) a)
							.collect(Collectors.toSet());
					workspaceService.deleteAll(ids, user, event -> {
						if (event.succeeded()) {
							renderJson(request, new JsonObject().put("number", event.result().size()));
						} else {
							badRequest(request, event.cause().getMessage());
						}
					});
				});
			} else {
				unauthorized(request);
			}
		});
	}

	@Delete("/document/:id/revision/:revisionId")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void deleteRevision(final HttpServerRequest request) {
		final String id = request.params().get("id");
		final String revisionId = request.params().get("revisionId");
		workspaceService.deleteRevision(id, revisionId, defaultResponseHandler(request));
	}

	@Get("/document/archive/:ids")
	@SecuredAction(value = "workspace.read", type = ActionType.AUTHENTICATED)
	public void download(HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && user.getUserId() != null) {
				String ids = request.params().get("ids");
				List<String> idLists = StringUtils.split(ids, ",");
				workspaceService.downloadFiles(idLists, user, request);
			} else {
				unauthorized(request);
			}
		});
	}

	@Delete("/trash")
	@SecuredAction(value = "workspace.manager", type = ActionType.AUTHENTICATED)
	public void emptyTrash(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				ElementQuery query = new ElementQuery(true);
				query.setApplication(null);
				query.setShared(true);
				query.setTrash(true);
				query.setTrasherId(user.getUserId());
				workspaceService.deleteByQuery(query, Optional.of(user), asyncArrayResponseHandler(request));
			} else {
				unauthorized(request);
			}
		});
	}

	private ElementQuery queryFromRequest(HttpServerRequest request, UserInfos user) {
		final String parentId = getOrElse(request.params().get("parentId"), request.params().get("folder"), false);
		final String ancestorId = request.params().get("ancestorId");
		final String hierarchical = request.params().get("hierarchical");
		final String filter = getOrElse(request.params().get("filter"), "owner", false);
		final String application = getOrElse(request.params().get("application"), null, false);
		final String search = request.params().get("search");
		//
		ElementQuery query = new ElementQuery(false);
		query.setHierarchical(hierarchical != null && hierarchical.equals("true"));
		query.setApplication(application);
		query.setTrash(false);
		// search
		if (!StringUtils.isEmpty(search)) {
			final List<String> searchs = StringUtils.split(search, "\\s+");
			query.setFullTextSearch(searchs);
			query.addSort("modified", ElementSort.Desc);
		}
		// parent
		query.setParentId(parentId);
		if (StringUtils.isEmpty(parentId)) {
			query.setNoParent(true);
		}
		if (!StringUtils.isEmpty(ancestorId)) {
			query.setAncestorId(ancestorId);
		}
		//
		switch (filter) {
		case "all":
			query.setTrash(null);
			query.setShared(true);
			query.setApplication(null);
			query.setNoParent(null);
			break;
		case "owner":
			query.setHasBeenShared(false);
			query.setVisibilitiesNotIn(new HashSet<>());
			query.getVisibilitiesNotIn().add("protected");
			query.getVisibilitiesNotIn().add("public");
			break;
		case "shared":
			query.setShared(true);
			query.setHasBeenShared(true);
			break;
		case "protected":
			query.setApplication(null);
			query.setVisibilitiesIn(new HashSet<>());
			query.getVisibilitiesIn().add("protected");
			break;
		case "public":
			query.setApplication(null);
			query.setVisibilitiesIn(new HashSet<>());
			query.getVisibilitiesIn().add("public");
			break;
		case "trash":
			query.setApplication(null);
			query.setShared(true);
			query.setTrash(true);
			query.setTrasherId(user.getUserId());
			break;
		}
		return query;
	}

	@Get("/folders/list")
	@SecuredAction(value = "workspace.folders.list", type = ActionType.AUTHENTICATED)
	public void folders(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, userInfos -> {
			if (userInfos != null) {
				ElementQuery query = queryFromRequest(request, userInfos);
				query.setType(FolderManager.FOLDER_TYPE);
				query.setProjection(ElementQuery.defaultProjection());
				query.getProjection().add("comments");
				query.getProjection().add("application");
				query.getProjection().add("trasher");
				workspaceService.findByQuery(query, userInfos, asyncArrayResponseHandler(request));
			} else {
				unauthorized(request);
			}
		});
	}

	@Get("/workspace/availables-workflow-actions")
	@SecuredAction(value = "workspace.habilitation", type = ActionType.AUTHENTICATED)
	public void getActionsInfos(final HttpServerRequest request) {
		ActionsUtils.findWorkflowSecureActions(eb, request, this);
	}

	@Get("/document/:id")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void getDocument(HttpServerRequest request) {
		getFile(request, null, false);
	}

	private void getDocument(final Message<JsonObject> message) {
		workspaceService.findById(message.body().getString("id"), new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				message.reply(res);
			}
		});
	}

	@Get("/document/properties/:id")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void getDocumentProperties(final HttpServerRequest request) {
		workspaceService.documentProperties(request.params().get("id"), new Handler<JsonObject>() {
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

	private void getFile(final HttpServerRequest request, String owner, boolean publicOnly) {
		workspaceService.findById(request.params().get("id"), owner, publicOnly, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				String status = res.getString("status");
				JsonObject result = res.getJsonObject("result");
				String thumbSize = request.params().get("thumbnail");
				if ("ok".equals(status) && result != null) {
					String file;
					if (thumbSize != null && !thumbSize.trim().isEmpty()) {
						file = result.getJsonObject("thumbnails", new JsonObject()).getString(thumbSize,
								result.getString("file"));
					} else {
						file = result.getString("file");
					}
					if (file != null && !file.trim().isEmpty()) {
						boolean inline = inlineDocumentResponse(result, request.params().get("application"));
						if (inline && ETag.check(request, file)) {
							notModified(request, file);
						} else {
							storage.sendFile(file, result.getString("name"), request, inline,
									result.getJsonObject("metadata"));
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

	@Get("/pub/document/:id")
	public void getPublicDocument(HttpServerRequest request) {
		getFile(request, null, true);
	}

	@Get("/document/:id/revision/:revisionId")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void getRevision(HttpServerRequest request) {
		String documentId = request.params().get("id");
		String revisionId = request.params().get("revisionId");
		if (revisionId == null || revisionId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		workspaceService.getRevision(documentId, revisionId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isLeft()) {
					notFound(request);
					return;
				}
				JsonObject result = event.right().getValue();
				String file = result.getString("file");
				if (file != null && !file.trim().isEmpty()) {
					if (ETag.check(request, file)) {
						notModified(request, file);
					} else {
						storage.sendFile(file, result.getString("name"), request, false,
								result.getJsonObject("metadata"));
					}
					eventStore.createAndStoreEvent(WokspaceEvent.GET_RESOURCE.name(), request,
							new JsonObject().put("resource", documentId));
				} else {
					notFound(request);
				}
			}
		});
	}

	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);

		notification = new TimelineHelper(vertx, eb, config);
		eventStore = EventStoreFactory.getFactory().getEventStore(Workspace.class.getSimpleName());
		post("/documents/copy/:ids", "copyDocuments");
		put("/documents/move/:ids", "moveDocuments");
	}

	private boolean inlineDocumentResponse(JsonObject doc, String application) {
		JsonObject metadata = doc.getJsonObject("metadata");
		String storeApplication = doc.getString("application");
		return metadata != null && !"WORKSPACE".equals(storeApplication) && ("image/jpeg"
				.equals(metadata.getString("content-type")) || "image/gif".equals(metadata.getString("content-type"))
				|| "image/png".equals(metadata.getString("content-type"))
				|| "image/tiff".equals(metadata.getString("content-type"))
				|| "image/vnd.microsoft.icon".equals(metadata.getString("content-type"))
				|| "image/svg+xml".equals(metadata.getString("content-type"))
				|| ("application/octet-stream".equals(metadata.getString("content-type")) && application != null));
	}

	@Get("/documents")
	@SecuredAction("workspace.documents.list")
	public void listDocuments(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && user.getUserId() != null) {
				ElementQuery query = queryFromRequest(request, user);
				query.setType(FolderManager.FILE_TYPE);
				query.setProjection(ElementQuery.defaultProjection());
				query.getProjection().add("comments");
				query.getProjection().add("application");
				query.getProjection().add("trasher");
				query.getProjection().add("protected");
				final String includeall = request.params().get("includeall");
				if (includeall != null && "true".equals(includeall)) {
					query.setType(null);
				}
				workspaceService.findByQuery(query, user, asyncArrayResponseHandler(request));
			} else {
				unauthorized(request);
			}
		});
	}

	@Get("/documents/:folder")
	@SecuredAction("workspace.documents.list.by.folder")
	public void listDocumentsByFolder(final HttpServerRequest request) {
		String folderId = request.params().get("folder");
		if (folderId == null || folderId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && user.getUserId() != null) {
				ElementQuery query = queryFromRequest(request, user);
				query.setType(FolderManager.FILE_TYPE);
				workspaceService.findByQuery(query, user, asyncArrayResponseHandler(request));
			} else {
				unauthorized(request);
			}
		});
	}

	@Get("/folders")
	@SecuredAction("workspace.document.list.folders")
	public void listFolders(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && user.getUserId() != null) {
				ElementQuery query = queryFromRequest(request, user);
				query.setType(FolderManager.FOLDER_TYPE);

				workspaceService.findByQuery(query, user, event -> {
					if (event.succeeded()) {
						Set<String> folders = new HashSet<String>();
						for (Object value : event.result()) {
							JsonObject v = (JsonObject) value;
							folders.add(v.getString("name"));
						}
						renderJson(request, new JsonArray(new ArrayList<>(folders)));
					} else {
						renderError(request, new JsonObject().put("errors", event.cause().getMessage()));
					}

				});
			} else {
				unauthorized(request);
			}
		});
	}

	@Get("/document/:id/revisions")
	@SecuredAction(value = "workspace.read", type = ActionType.RESOURCE)
	public void listRevisions(HttpServerRequest request) {
		String id = request.params().get("id");
		workspaceService.listRevisions(id, arrayResponseHandler(request));
	}

	@Get("/trash")
	@SecuredAction("workspace.documents.list")
	public void listTrashDocuments(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && user.getUserId() != null) {
				ElementQuery query = new ElementQuery(false);
				query.setTrash(true);
				workspaceService.findByQuery(query, user, asyncArrayResponseHandler(request));
			} else {
				unauthorized(request);
			}
		});
	}

	@Put("/document/move/:id/:folder")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void moveDocument(final HttpServerRequest request) {
		String folderId = request.params().get("folder");
		if (folderId == null || folderId.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					workspaceService.move(request.params().get("id"), folderId, user,
							asyncDefaultResponseHandler(request));
				} else {
					unauthorized(request);
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Put("/documents/move/:folder")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void moveDocuments(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null && user.getUserId() != null) {
				bodyToJson(request, body -> {
					JsonArray ids = body.getJsonArray("ids");
					String folder = request.params().get("folder");
					if ("root".equals(folder)) {
						folder = null;
					}
					workspaceService.moveAll(ids.getList(), folder, user, event -> {
						if (event.succeeded()) {
							renderJson(request, new JsonObject().put("number", event.result().size()));
						} else {
							badRequest(request, event.cause().getMessage());
						}
					});
				});
			} else {
				unauthorized(request);
			}
		});
	}

	@Put("/folder/move/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void moveFolder(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void v) {
				final String id = request.params().get("id");
				final String parentFolderId = request.formAttributes().get("parentFolderId");

				if (id == null || id.trim().isEmpty()) {
					badRequest(request);
					return;
				}
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					@Override
					public void handle(final UserInfos userInfos) {
						if (userInfos != null) {
							workspaceService.move(id, parentFolderId, userInfos, asyncDefaultResponseHandler(request));
						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	@Put("/documents/trash")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void moveTrash(final HttpServerRequest request) {
		this.moveTrashFolder(request);
	}

	@Put("/folders/trash")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void moveTrashFolder(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, userInfos -> {
			if (userInfos != null) {
				bodyToJson(request, body -> {
					Set<String> ids = body.getJsonArray("ids").stream().map(a -> (String) a)
							.collect(Collectors.toSet());
					workspaceService.trashAll(ids, userInfos, asyncArrayResponseHandler(request));
				});
			} else {
				unauthorized(request);
			}
		});
	}

	private void notifyComment(final HttpServerRequest request, final String id, final UserInfos user,
			final boolean isFolder) {
		final JsonObject params = new JsonObject()
				.put("userUri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
				.put("userName", user.getUsername()).put("appPrefix", pathPrefix + "/workspace");

		final String notifyName = WorkspaceService.WORKSPACE_NAME.toLowerCase() + "."
				+ (isFolder ? "comment-folder" : "comment");

		workspaceService.findById(id, event -> {
			if ("ok".equals(event.getString("status")) && event.getJsonObject("result") != null) {
				final JsonObject document = event.getJsonObject("result");
				params.put("resourceName", document.getString("name", ""));
				String parentId = document.getString("eParent");
				// Send the notification to the shared network
				shareService.findUserIdsForInheritShare(id, user.getUserId(), Optional.empty(), evRecipients -> {
					if (evRecipients.succeeded()) {
						Set<String> recipients = evRecipients.result();
						JsonObject sharedNotifParams = params.copy();
						if (parentId != null) {
							sharedNotifParams.put("resourceUri", pathPrefix + "/workspace#/shared/folder/" + parentId);
						} else {
							sharedNotifParams.put("resourceUri", pathPrefix + "/workspace#/shared");
						}
						// don't send comment with share uri at owner
						final String o = document.getString("owner");
						if (o != null && recipients.contains(o)) {
							recipients.remove(o);
						}
						notification.notifyTimeline(request, notifyName, user, new ArrayList<>(recipients), id,
								sharedNotifParams);
					}
				});

				// If the user commenting is not the owner, send a notification to the owner
				if (!document.getString("owner").equals(user.getUserId())) {
					JsonObject ownerNotif = params.copy();
					ArrayList<String> ownerList = new ArrayList<>();
					ownerList.add(document.getString("owner"));
					if (parentId != null) {
						ownerNotif.put("resourceUri", pathPrefix + "/workspace#/folder/" + parentId);
					} else {
						ownerNotif.put("resourceUri", pathPrefix + "/workspace");
					}
					notification.notifyTimeline(request, notifyName, user, ownerList, id, null, ownerNotif, true);
				}

			} else {
				log.error("Unable to send timeline notification : missing name on resource " + id);
			}
		});
	}

	@Put("/rename/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void renameDocument(final HttpServerRequest request) {
		RequestUtils.bodyToJson(request, pathPrefix + "rename", new Handler<JsonObject>() {
			public void handle(final JsonObject body) {
				UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
					public void handle(UserInfos userInfos) {
						if (userInfos != null) {
							String id = request.params().get("id");
							final String name = body.getString("name");
							workspaceService.rename(id, name, userInfos, asyncDefaultResponseHandler(request));

						} else {
							unauthorized(request);
						}
					}
				});
			}
		});
	}

	@Put("/folder/rename/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void renameFolder(final HttpServerRequest request) {
		this.renameDocument(request);
	}

	@Put("/folders/restore")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void restoreFolder(final HttpServerRequest request) {
		this.restoreTrash(request);
	}

	@Put("/documents/restore")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void restoreTrash(HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, userInfos -> {
			if (userInfos != null) {
				bodyToJson(request, body -> {
					Set<String> ids = body.getJsonArray("ids").stream().map(a -> (String) a)
							.collect(Collectors.toSet());
					workspaceService.restoreAll(ids, userInfos, asyncArrayResponseHandler(request));
				});
			} else {
				unauthorized(request);
			}
		});
	}

	private void sendNotify(final HttpServerRequest request, final String resource, final UserInfos user,
			final Collection<String> recipients) {
		final JsonObject params = new JsonObject()
				.put("uri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
				.put("username", user.getUsername()).put("appPrefix", pathPrefix + "/workspace").put("doc", "share");
		JsonObject keys = new JsonObject().put("name", 1).put("eType", 1).put("eParent", 1).put("isShared", 1);
		Future<JsonObject> futureFindById = Future.future();
		workspaceService.findById(resource, keys, event -> {
			if ("ok".equals(event.getString("status")) && event.getJsonObject("result") != null) {
				futureFindById.complete(event.getJsonObject("result"));
			} else {
				log.error("Unable to send timeline notification : missing name on resource " + resource);
				futureFindById.fail("Unable to send timeline notification : missing name on resource " + resource);
			}
		});
		futureFindById.compose(result -> {
			boolean isFolder = DocumentHelper.isFolder(result);
			String parentId = DocumentHelper.getParent(result);
			if (isFolder) {
				return Future.succeededFuture(new JsonObject());
			} else if (parentId == null) {
				return Future.succeededFuture(new JsonObject());
			} else {
				Future<JsonObject> futureParent = Future.future();
				workspaceService.findById(parentId, keys, event -> {
					if ("ok".equals(event.getString("status")) && event.getJsonObject("result") != null) {
						futureParent.complete(event.getJsonObject("result"));
					} else {
						log.error("Unable to send timeline notification : missing name on resource " + resource);
						futureParent.complete(new JsonObject());
					}
				});
				return futureParent;
			}
		}).setHandler(evtParent -> {
			if (evtParent.succeeded()) {
				JsonObject parent = evtParent.result();
				JsonObject result = futureFindById.result();
				String resourceName = result.getString("name", "");
				boolean isFolder = DocumentHelper.isFolder(result);
				final JsonObject pushNotif = new JsonObject();
				final String i18nPushNotifBody;
				final String notificationName;
				if (isFolder) {
					notificationName = WorkspaceService.WORKSPACE_NAME.toLowerCase() + ".share-folder";
					params.put("resourceUri", pathPrefix + "/workspace#/shared/folder/" + resource);
					pushNotif.put("title", "push.notif.folder.share");
					i18nPushNotifBody = user.getUsername() + " " + I18n.getInstance().translate(
							"workspace.shared.folder", getHost(request), I18n.acceptLanguage(request)) + " : ";
				} else {
					notificationName = WorkspaceService.WORKSPACE_NAME.toLowerCase() + ".share";
					String parentId = DocumentHelper.getParent(result);
					boolean isShared = DocumentHelper.isShared(result);
					if (parentId != null) {
						params.put("resourceFolderUri", pathPrefix + "/workspace#/folder/" + parentId);
						params.put("resourceFolderName", DocumentHelper.getName(parent));
					} else if (isShared) {
						params.put("resourceFolderUri", pathPrefix + "/workspace#shared");
						params.put("resourceFolderName", I18n.getInstance().translate("shared_tree", getHost(request),
								I18n.acceptLanguage(request)));
					} else {
						params.put("resourceFolderUri", pathPrefix + "/workspace");
						params.put("resourceFolderName", I18n.getInstance().translate("documents", getHost(request),
								I18n.acceptLanguage(request)));
					}
					params.put("resourceUri", pathPrefix + "/document/" + resource);
					pushNotif.put("title", "push.notif.file.share");
					i18nPushNotifBody = user.getUsername() + " " + I18n.getInstance().translate(
							"workspace.shared.document", getHost(request), I18n.acceptLanguage(request)) + " : ";
				}
				//
				params.put("resourceName", resourceName);
				params.put("pushNotif", pushNotif.put("body", i18nPushNotifBody + resourceName));
				notification.notifyTimeline(request, notificationName, user, new ArrayList<>(recipients), resource,
						params);
			}
		});
	}

	public void setStorage(Storage storage) {
		this.storage = storage;
	}

	@Get("/share/json/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void shareJson(final HttpServerRequest request) {
		final String id = request.params().get("id");
		if (id == null || id.trim().isEmpty()) {
			badRequest(request);
			return;
		}
		getUserInfos(eb, request, user -> {
			if (user != null) {
				workspaceService.getShareInfos(user.getUserId(), id, I18n.acceptLanguage(request),
						request.params().get("search"), defaultResponseHandler(request));
			} else {
				unauthorized(request);
			}
		});
	}

	@Put("/share/resource/:id")
	@SecuredAction(value = "workspace.manager", type = ActionType.RESOURCE)
	public void shareResource(final HttpServerRequest request) {
		final String id = request.params().get("id");
		getUserInfos(eb, request, user -> {
			if (user != null) {
				RequestUtils.bodyToJson(request, body -> {
					workspaceService.share(id, ElementShareOperations.addShareObject(SHARED_ACTION, user, body),
							event -> {
								if (event.succeeded()) {
									JsonArray n = event.result().getJsonArray("notify-timeline-array");
									if (n != null) {
										shareService.findUserIdsForInheritShare(id, user.getUserId(), Optional.empty(),
												evRecipients -> {
													if (evRecipients.succeeded()) {
														sendNotify(request, id, user, evRecipients.result());
													}
												});
									}
									renderJson(request, event.result());
								} else {
									JsonObject error = new JsonObject().put("error", event.cause().getMessage());
									renderJson(request, error, 400);
								}
							});
				});
			} else {
				unauthorized(request);
			}
		});

	}

	@Put("/document/:id")
	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void updateDocument(final HttpServerRequest request) {
		final String documentId = request.params().get("id");

		UserUtils.getUserInfos(eb, request, user -> {
			if (user != null) {
				float quality = checkQuality(request.params().get("quality"));
				String name = request.params().get("name");
				List<String> thumbnail = request.params().getAll("thumbnail");
				request.pause();
				workspaceService.findById(documentId, event -> {
					if (!"ok".equals(event.getString("status"))) {
						notFound(request);
						return;
					}
					workspaceService.emptySize(user, emptySize -> {
						request.resume();
						storage.writeUploadFile(request, emptySize, uploaded -> {
							if ("ok".equals(uploaded.getString("status"))) {
								uploaded.put("alt", request.params().get("alt"));
								uploaded.put("legend", request.params().get("legend"));
								workspaceService.updateDocument(documentId, quality, name, thumbnail, uploaded, user,
										res -> {
											if (res == null) {
												request.response().setStatusCode(404).end();
											} else if ("ok".equals(res.body().getString("status"))) {
												renderJson(request, res.body());
											} else {
												renderError(request, res.body());
											}
										});
							} else {
								badRequest(request, uploaded.getString("message"));
							}
						});
					});
				});
			} else {
				unauthorized(request);
			}
		});
	}

	private void updateDocument(final Message<JsonObject> message) {
		JsonObject uploaded = message.body().getJsonObject("uploaded");
		String id = message.body().getString("id");
		if (uploaded == null || id == null || id.trim().isEmpty()) {
			message.reply(new JsonObject().put("status", "error").put("message", "missing.attribute"));
			return;
		}
		String name = message.body().getString("name");
		JsonArray t = message.body().getJsonArray("thumbs", new fr.wseduc.webutils.collections.JsonArray());
		List<String> thumbs = new ArrayList<>();
		for (int i = 0; i < t.size(); i++) {
			thumbs.add(t.getString(i));
		}
		workspaceService.updateAfterUpload(id, name, uploaded, thumbs, null, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				if (m != null) {
					message.reply(m.body());
				}
			}
		});
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
					workspaceService.getQuotaAndUsage(user.getUserId(), new Handler<Either<String, JsonObject>>() {
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

	@BusAddress("org.entcore.workspace")
	public void workspaceEventBusHandler(final Message<JsonObject> message) {
		switch (message.body().getString("action", "")) {
		case "addDocument":
			addDocument(message);
			break;
		case "updateDocument":
			updateDocument(message);
			break;
		case "getDocument":
			getDocument(message);
			break;
		case "copyDocument":
			copyDocumentFromBus(message);
			break;
		default:
			message.reply(new JsonObject().put("status", "error").put("message", "invalid.action"));
		}
	}
}
