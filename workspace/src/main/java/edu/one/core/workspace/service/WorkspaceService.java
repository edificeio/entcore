package edu.one.core.workspace.service;

import static edu.one.core.infra.Utils.getOrElse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.infra.Controller;
import edu.one.core.infra.FileUtils;
import edu.one.core.infra.MongoDb;
import edu.one.core.common.neo4j.Neo;
import edu.one.core.infra.NotificationHelper;
import edu.one.core.infra.Server;
import edu.one.core.infra.TracerHelper;
import edu.one.core.infra.http.ETag;
import edu.one.core.common.user.UserUtils;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.security.ActionType;
import edu.one.core.security.SecuredAction;
import edu.one.core.workspace.dao.DocumentDao;
import edu.one.core.workspace.dao.GenericDao;
import edu.one.core.workspace.dao.RackDao;

public class WorkspaceService extends Controller {

	public static final String WORKSPACE_NAME = "WORKSPACE";
	private final String gridfsAddress;
	private final MongoDb mongo;
	private final DocumentDao documentDao;
	private final RackDao rackDao;
	private final Neo neo;
	private final NotificationHelper notification;
	private final TracerHelper trace;

	public WorkspaceService(Vertx vertx, Container container, RouteMatcher rm, TracerHelper trace,
			Map<String, edu.one.core.infra.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		mongo = new MongoDb(Server.getEventBus(vertx),
				container.config().getString("mongo-address", "wse.mongodb.persistor"));
		gridfsAddress = container.config().getString("gridfs-address", "wse.gridfs.persistor");
		documentDao = new DocumentDao(mongo);
		rackDao = new RackDao(mongo);
		neo = new Neo(eb, log);
		notification = new NotificationHelper(eb, container);
		this.trace = trace;
	}

	@SecuredAction("workspace.view")
	public void view(HttpServerRequest request) {
		renderView(request);
	}

	@SecuredAction("workspace.share")
	public void share(final HttpServerRequest request) {
		final String id  = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
				@Override
				public void handle(UserInfos user) {
					String matcher = "{\"_id\" : \"" + id + "\", "
							+ "\"owner\" : \"" + user.getUserId() + "\"}";
					mongo.findOne(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(matcher),
						new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> res) {
								if (res  != null && res.body() != null &&
										"ok".equals(res.body().getString("status"))) {
									if (res.body().getObject("result") == null) {
										notFound(request);
										return;
									}
									JsonArray shared = res.body().getObject("result").getArray("shared");
									List<String> checked = new ArrayList<>();
									if (shared != null && shared.size() > 0) {
										for (Object o : shared) {
											JsonObject userShared = (JsonObject) o;
											String userOrGroupId = userShared.getString("groupId",
													userShared.getString("userId"));
											if (userOrGroupId == null) continue;
											for (String attrName : userShared.getFieldNames()) {
												if ("userId".equals(attrName) || "groupId".equals(attrName)) {
													continue;
												}
												if (userShared.getBoolean(attrName, false)) {
													checked.add(attrName + "_" + userOrGroupId);
												}
											}
										}
									}
									shareUserAndGroupResource(request, id, checked);
								} else {
									badRequest(request);
								}
							}
						});
				}
			});
		} else {
			unauthorized(request);
		}
	}

	@SecuredAction("workspace.share")
	public void shareDocument(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {

			@Override
			protected void handle() {
				final String id = request.formAttributes().get("resourceId");
				final List<String> shares = request.formAttributes().getAll("shares");
				final List<String> shareGroups = request.formAttributes().getAll("shareGroups");
				if (id != null && shares != null && shareGroups != null && !id.trim().isEmpty()) {
					UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

						@Override
						public void handle(final UserInfos user) {
							if (user != null) {
								Map<String, JsonObject> sharesMap = new HashMap<>();
								for (String share : shares) {
									String [] s = share.split("_");
									if (s.length != 2) continue;
									String [] actions = s[0].split(",");
									if (actions.length < 1) continue;
									for (int i = 0; i < actions.length; i++) {
										JsonObject j = sharesMap.get(s[1]);
										if (j == null) {
											j = new JsonObject().putString("userId", s[1]);
											sharesMap.put(s[1], j);
										}
										j.putBoolean(actions[i].replaceAll("\\.", "-"), true);
									}
								}
								for (String share : shareGroups) {
									String [] s = share.split("_");
									if (s.length != 2) continue;
									String [] actions = s[0].split(",");
									if (actions.length < 1) continue;
									for (int i = 0; i < actions.length; i++) {
										JsonObject j = sharesMap.get(s[1]);
										if (j == null) {
											j = new JsonObject().putString("groupId", s[1]);
											sharesMap.put(s[1], j);
										}
										j.putBoolean(actions[i].replaceAll("\\.", "-"), true);
									}
								}
								final JsonArray sharedArray = new JsonArray();
								for (JsonObject jo: sharesMap.values()) {
									sharedArray.add(jo);
								}
								String criteria = "{\"_id\" : \"" + id + "\", "
										+ "\"owner\" : \"" + user.getUserId() + "\"}";
								String query = "{ \"$set\" : { \"shared\" : " + sharedArray.encode() + "}}";
								trace.info(user.getLogin() + " a partagé un document (id=" + id + ").");
								mongo.update(DocumentDao.DOCUMENTS_COLLECTION,
										new JsonObject(criteria), new JsonObject(query),
										new Handler<Message<JsonObject>>() {

											@Override
											public void handle(Message<JsonObject> res) {
												if ("ok".equals(res.body().getString("status"))) {
													notifyShare(request, id, user, sharedArray);
													redirect(request, "/workspace/workspace");
												} else {
													renderError(request, res.body());
												}
											}
										});
							} else {
								badRequest(request);
							}
						}
					});
				} else {
					badRequest(request);
				}
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
					UserUtils.findUsersInProfilsGroups(groupId, eb, new Handler<JsonArray>() {
						@Override
						public void handle(JsonArray event) {
							log.debug(event.encode());
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

	private void sendNotify(HttpServerRequest request, String resource, UserInfos user, List<String> recipients) {
		JsonObject params = new JsonObject()
		.putString("uri", container.config().getString("userbook-host") +
				"/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
		.putString("username", user.getUsername())
		.putString("resourceUri", container.config().getString("host", "http://localhost:8011") +
				pathPrefix + "/document/" + resource);
		try {
			notification.notifyTimeline(request, user, recipients, resource,
					"notify-share.html", params);
		} catch (IOException e) {
			log.error("Unable to send timeline notification", e);
		}
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
								"START n=node:node_auto_index(id={id}) " +
								"RETURN count(n) as nb, n.ENTPersonNomAffichage as username";
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
			public void handle(JsonObject uploaded) {
				if ("ok".equals(uploaded.getString("status"))) {
					doc.putString("name", getOrElse(request
							.params().get("name"), uploaded.getObject("metadata")
							.getString("filename"), false));
					doc.putObject("metadata", uploaded.getObject("metadata"));
					doc.putString("file", uploaded.getString("_id"));
					doc.putString("application", getOrElse(request.params()
							.get("application"), WORKSPACE_NAME)); // TODO check if application name is valid
					mongo.save(mongoCollection, doc, new Handler<Message<JsonObject>>() {
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

	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void updateDocument(final HttpServerRequest request) {
		FileUtils.gridfsWriteUploadFile(request, eb, gridfsAddress, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject uploaded) {
				documentDao.findById(request.params().get("id"), new Handler<JsonObject>() {
					@Override
					public void handle(final JsonObject old) {
						if ("ok".equals(old.getString("status"))) {
							JsonObject metadata = uploaded.getObject("metadata");
							JsonObject set = new JsonObject();
							JsonObject doc = new JsonObject();
							doc.putString("name", getOrElse(request
									.params().get("name"), metadata.getString("filename")));
							final String now = MongoDb.formatDate(new Date());
							doc.putString("modified", now);
							doc.putObject("metadata", metadata);
							doc.putString("file", uploaded.getString("_id"));
							String query = "{ \"_id\": \"" + request.params().get("id") + "\"}";
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
														renderJson(request, res.body());
													}
												});
									} else {
										renderError(request, res.body());
									}
								}
							});
						} else {
							request.response().setStatusCode(404).end();
						}
					}
				});
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
				if ("ok".equals(status) && result != null && result.getString("file") != null) {
					boolean inline = inlineDocumentResponse(result, request.params().get("application"));
					if (inline && ETag.check(request, result.getString("file"))) {
						notModified(request, result.getString("file"));
					} else {
						FileUtils.gridfsSendFile(result.getString("file"),
								result.getString("name"), eb, gridfsAddress, request.response(),
								inline, result.getObject("metadata"));
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
											notification.deleteFromTimeline(id);
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

	@SecuredAction(value = "workspace.contrib", type = ActionType.RESOURCE)
	public void copyDocuments(HttpServerRequest request) {
		copyFiles(request, DocumentDao.DOCUMENTS_COLLECTION, null);
	}

	@SecuredAction("workspace.rack.documents.copy")
	public void copyRackDocuments(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					copyFiles(request, RackDao.RACKS_COLLECTION, user.getUserId());
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void copyFiles(final HttpServerRequest request, final String collection, final String owner) {
		String ids = request.params().get("ids"); // TODO refactor with json in request body
		String folder = request.params().get("folder");
		if (ids != null && folder != null &&
				!ids.trim().isEmpty() && !folder.trim().isEmpty()) {
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
							}
							dest.putString("created", now);
							dest.putString("modified", now);
							dest.putString("folder", request.params().get("folder"));
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
		if (ids != null && folder != null &&
				!ids.trim().isEmpty() && !folder.trim().isEmpty()) {
			JsonArray idsArray = new JsonArray(ids.split(","));
			String criteria = "{ \"_id\" : { \"$in\" : " + idsArray.encode() + "}}";
			String obj = "{ \"$set\" : { \"folder\": \"" + folder +
					"\", \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\" }}";
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
					if ("owner".equals(filter)) {
						query += "\"owner\": \"" + user.getUserId() + "\"";
					} else if ("shared".equals(filter)) {
						query += "\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user) + "}";
					} else {
						query += "\"$or\" : [{ \"owner\": \"" + user.getUserId() +
								"\"}, {\"shared\" : { \"$elemMatch\" : " + orSharedElementMatch(user) + "}}]";
					}
					String forApplication = getOrElse(request.params()
							.get("application"), WorkspaceService.WORKSPACE_NAME);
					if (request.params().get("hierarchical") != null) {
						query += ", \"file\" : { \"$exists\" : true }, \"application\": \"" +
								forApplication + "\", \"folder\" : { \"$exists\" : false }}";
					} else {
						query += ", \"file\" : { \"$exists\" : true }, \"application\": \"" +
								forApplication + "\" }";
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
				"MATCH visibles-[:APPARTIENT]->g-[:AUTHORIZED]->r-[:AUTHORIZE]->a " +
				"WHERE has(a.name) AND a.name={action} " +
				"RETURN distinct visibles.id as id, visibles.ENTPersonNomAffichage as username " +
				"ORDER BY username ";
		JsonObject params = new JsonObject()
		.putString("action", "edu.one.core.workspace.service.WorkspaceService|listRackDocuments");
		UserUtils.findVisibleUsers(eb, request, customReturn, params, new Handler<JsonArray>() {

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

}
