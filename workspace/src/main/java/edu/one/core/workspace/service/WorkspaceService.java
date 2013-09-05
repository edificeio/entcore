package edu.one.core.workspace.service;

import static edu.one.core.infra.Utils.getOrElse;

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

import com.google.common.base.Joiner;

import edu.one.core.infra.Controller;
import edu.one.core.infra.FileUtils;
import edu.one.core.infra.MongoDb;
import edu.one.core.infra.Neo;
import edu.one.core.infra.Server;
import edu.one.core.infra.security.UserUtils;
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

	public WorkspaceService(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, edu.one.core.infra.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		mongo = new MongoDb(Server.getEventBus(vertx),
				container.config().getObject("mongodb-config").getString("address"));
		gridfsAddress = container.config().getObject("gridfs-config").getString("address");
		documentDao = new DocumentDao(mongo);
		rackDao = new RackDao(mongo);
		neo = new Neo(eb, log);
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
											String userId = userShared.getString("userId");
											for (String attrName : userShared.getFieldNames()) {
												if ("userId".equals(attrName)) continue;
												if (userShared.getBoolean(attrName, false)) {
													checked.add(attrName + "_" + userId);
												}
											}
										}
									}
									shareResource(request, checked);
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
				if (id != null && shares != null && !id.trim().isEmpty()) {
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
								final JsonArray sharedArray = new JsonArray();
								for (JsonObject jo: sharesMap.values()) {
									sharedArray.add(jo);
								}
								String criteria = "{\"_id\" : \"" + id + "\", "
										+ "\"owner\" : \"" + user.getUserId() + "\"}";
								String query = "{ \"$set\" : { \"shared\" : " + sharedArray.encode() + "}}";
								mongo.update(DocumentDao.DOCUMENTS_COLLECTION,
										new JsonObject(criteria), new JsonObject(query),
										new Handler<Message<JsonObject>>() {

											@Override
											public void handle(Message<JsonObject> res) {
												if ("ok".equals(res.body().getString("status"))) {
													notifyShare(id, user, sharedArray);
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

	// TODO extract in external helper class
	private void notifyShare(String resource, UserInfos user, JsonArray sharedArray) {
		JsonArray recipients = new JsonArray();
		for (Object j : sharedArray) {
			JsonObject json = (JsonObject) j;
			recipients.addString(json.getString("userId"));
		}
		JsonObject event = new JsonObject()
		.putString("action", "add")
		.putString("resource", resource)
		.putString("sender", user.getUserId())
		.putString("message", user.getUsername() +
				" a partagé avec vous <a href=\"" +
				container.config().getString("host", "http://localhost:8011") +
				pathPrefix + "/document/" + resource + "\">un document</a>.")
		.putArray("recipients", recipients);
		eb.send("wse.timeline", event);
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
								"RETURN count(n) as nb";
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
									doc.putString("from", userInfos.getUserId());
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
							.getString("filename")));
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
					FileUtils.gridfsSendFile(result.getString("file"),
							result.getString("name"), eb, gridfsAddress, request.response(),
							inlineDocumentResponse(result, request.params().get("application")));
				} else {
					request.response().setStatusCode(404).end();
				}
			}
		});
	}

	private boolean inlineDocumentResponse(JsonObject doc, String application) {
		JsonObject metadata = doc.getObject("metadata");
		return metadata != null && (
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
											notifyDelete(id);
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

	// TODO extract in external helper class
	private void notifyDelete(String resource) {
		JsonObject json = new JsonObject()
		.putString("action", "delete")
		.putString("resource", resource);
		eb.send("wse.timeline", json);
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

	private void copyFiles(final HttpServerRequest request, final String collection, String owner) {
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
		copyFile(request, documentDao);
	}

	@SecuredAction("workspace.document.copy")
	public void copyRackDocument(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					copyFile(request, rackDao);
				} else {
					unauthorized(request);
				}
			}
		});
	}

	private void copyFile(final HttpServerRequest request, final GenericDao dao) {
		dao.findById(request.params().get("id"), new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject src) {
				if ("ok".equals(src.getString("status")) && src.getObject("result") != null) {
					JsonObject orig = src.getObject("result");
					final JsonObject dest = orig.copy();
					String now = MongoDb.formatDate(new Date());
					dest.removeField("_id");
					dest.removeField("to"); // TODO add owner
					dest.removeField("from");
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

	@SecuredAction("workspace.document.list.folders")
	public void listFolders(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null && user.getUserId() != null) {
					final String hierarchical = request.params().get("hierarchical");
					final String relativePath = request.params().get("relativePath");
					String query = "{ \"$or\" : [{ \"owner\": \"" + user.getUserId() +
							"\"}, {\"shared\" : { \"$elemMatch\" : { \"userId\": \""
							+ user.getUserId()+ "\"}}}]";
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
		request.expectMultiPart(true);
		request.endHandler(new VoidHandler() {
			@Override
			protected void handle() {
				String comment = request.formAttributes().get("comment");
				if (comment != null && !comment.trim().isEmpty()) {
					String query = "{ \"$push\" : { \"comments\":" + // TODO get author from session
							" {\"author\" : \"\", \"posted\" : \"" + MongoDb.formatDate(new Date()) +
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

	private void moveOne(final HttpServerRequest request, String folder, GenericDao dao, String owner) {
		String obj = "{ \"$set\" : { \"folder\": \"" + folder +
				"\", \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\" }}";
		dao.update(request.params().get("id"), new JsonObject(obj), owner, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				if ("ok".equals(res.getString("status"))) {
					renderJson(request, res);
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
					String query = "{ \"$or\" : [{ \"owner\": \"" + user.getUserId() +
							"\"}, {\"shared\" : { \"$elemMatch\" : { \"userId\": \""
							+ user.getUserId()+ "\"}}}]";
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
					String query = "{ \"$or\" : [{ \"owner\": \"" + user.getUserId() +
							"\"}, {\"shared\" : { \"$elemMatch\" : { \"userId\": \""
							+ user.getUserId()+ "\"}}}]";
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
		UserUtils.findVisibleUsers(eb, request, new Handler<JsonArray>() {

			@Override
			public void handle(JsonArray users) {
				List<String> ids = new ArrayList<>();
				for (Object o: users) {
					JsonObject user = (JsonObject) o;
					String id = user.getString("id");
					if (id != null && !id.trim().isEmpty()) {
						ids.add(id);
					}
				}
				if (ids.size() > 0) {
					String query =
							"START n=node:node_auto_index({ids}) " +
							"MATCH n-[:APPARTIENT]->g-[:AUTHORIZED]->r-[:AUTHORIZE]->a " +
							"WHERE has(a.name) AND a.name={action}" +
							"RETURN distinct n.id as id, n.ENTPersonNomAffichage as username";
					Map<String, Object> params = new HashMap<>();
					params.put("ids", "id:" + Joiner.on(" OR id:").join(ids));
					params.put("action", "edu.one.core.workspace.service.WorkspaceService|listRackDocuments");
					neo.send(query, params, request.response());
				} else {
					renderJson(request, new JsonObject()
					.putString("status", "ok").putObject("result", new JsonObject()));
				}
			}
		});
	}

}
