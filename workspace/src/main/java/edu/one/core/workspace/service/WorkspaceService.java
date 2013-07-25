package edu.one.core.workspace.service;

import static edu.one.core.infra.Controller.badRequest;
import static edu.one.core.infra.Controller.renderError;
import static edu.one.core.infra.Controller.renderJson;
import static edu.one.core.infra.Utils.getOrElse;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.infra.AbstractService;
import edu.one.core.infra.Controller;
import edu.one.core.infra.FileUtils;
import edu.one.core.infra.MongoDb;
import edu.one.core.security.ActionType;
import edu.one.core.security.SecuredAction;
import edu.one.core.workspace.dao.DocumentDao;
import edu.one.core.workspace.dao.GenericDao;
import edu.one.core.workspace.dao.RackDao;

public class WorkspaceService extends AbstractService {

	public static final String WORKSPACE_NAME = "WORKSPACE";
	private final String gridfsAddress;
	private final MongoDb mongo;
	private final DocumentDao documentDao;
	private final RackDao rackDao;

	public WorkspaceService(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, edu.one.core.infra.security.SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
		mongo = new MongoDb(vertx.eventBus(),
				container.config().getObject("mongodb-config").getString("address"));
		gridfsAddress = container.config().getObject("gridfs-config").getString("address");
		documentDao = new DocumentDao(mongo);
		rackDao = new RackDao(mongo);
	}

	@SecuredAction("workspace.view")
	public void view(HttpServerRequest request, Controller controller) {
		controller.renderView(request);
	}

	//@SecuredAction("workspace.document.add")
	public void addDocument(HttpServerRequest request) {
		JsonObject doc = new JsonObject();
		String now = MongoDb.formatDate(new Date());
		doc.putString("created", now);
		doc.putString("modified", now);
		add(request, DocumentDao.DOCUMENTS_COLLECTION, doc);
	}

	@SecuredAction("workspace.rack.document.add")
	public void addRackDocument(HttpServerRequest request) {
		JsonObject doc = new JsonObject();
		doc.putString("to", request.params().get("to")); // TODO check existance and validity (neo4j)
		doc.putString("from", UUID.randomUUID().toString()); // TODO replace by user (session)
		String now = MongoDb.formatDate(new Date());
		doc.putString("sent", now);
		add(request, RackDao.RACKS_COLLECTION, doc);
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

	@SecuredAction(value = "workspace.document.get", type = ActionType.RESOURCE)
	public void getDocument(HttpServerRequest request) {
		getFile(request, documentDao);
	}

	@SecuredAction(value = "workspace.rack.document.get", type = ActionType.RESOURCE)
	public void getRackDocument(HttpServerRequest request) {
		getFile(request, rackDao);
	}

	private void getFile(final HttpServerRequest request, GenericDao dao) {
		dao.findById(request.params().get("id"), new Handler<JsonObject>() {
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

	@SecuredAction(value = "workspace.document.delete", type = ActionType.RESOURCE)
	public void deleteDocument(HttpServerRequest request) {
		deleteFile(request, documentDao);
	}

	@SecuredAction(value = "workspace.rack.document.delete", type = ActionType.RESOURCE)
	public void deleteRackDocument(HttpServerRequest request) {
		deleteFile(request, rackDao);
	}

	private void deleteFile(final HttpServerRequest request, final GenericDao dao) {
		final String id = request.params().get("id");
		dao.findById(id, new Handler<JsonObject>() {
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

	public void copyDocuments(HttpServerRequest request) {
		copyFiles(request, DocumentDao.DOCUMENTS_COLLECTION);
	}

	public void copyRackDocuments(HttpServerRequest request) {
		copyFiles(request, RackDao.RACKS_COLLECTION);
	}

	private void copyFiles(final HttpServerRequest request, final String collection) {
		String ids = request.params().get("ids"); // TODO refactor with json in request body
		String folder = request.params().get("folder");
		if (ids != null && folder != null &&
				!ids.trim().isEmpty() && !folder.trim().isEmpty()) {
			JsonArray idsArray = new JsonArray(ids.split(","));
			String criteria = "{ \"_id\" : { \"$in\" : " + idsArray.encode() + "}}";
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

	public void copyDocument(HttpServerRequest request) {
		copyFile(request, documentDao);
	}

	public void copyRackDocument(HttpServerRequest request) {
		copyFile(request, rackDao);
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

}
