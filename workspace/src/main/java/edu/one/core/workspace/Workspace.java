package edu.one.core.workspace;

import static edu.one.core.infra.Utils.getOrElse;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.Controller;
import edu.one.core.infra.MongoDb;
import edu.one.core.infra.request.filter.ActionFilter;
import edu.one.core.infra.request.filter.SecurityHandler;
import edu.one.core.workspace.dao.DocumentDao;
import edu.one.core.workspace.dao.RackDao;
import edu.one.core.workspace.security.WorkspaceResourcesProvider;
import edu.one.core.workspace.service.WorkspaceService;

public class Workspace extends Controller {

	@Override
	public void start(final Future<Void> result) {
		super.start();

		// Mongodb config
		JsonObject mongodbConf = container.config().getObject("mongodb-config");
		container.deployModule("io.vertx~mod-mongo-persistor~2.0.0-CR3-SNAPSHOT-WSE", mongodbConf, 1, new AsyncResultHandler<String>() {
			public void handle(AsyncResult<String> ar) {
				if (ar.succeeded()) {
					result.setResult(null);
				} else {
					log.error(ar.cause().getMessage());
				}
			}
		});
		final MongoDb mongo = new MongoDb(vertx.eventBus(), mongodbConf.getString("address"));
		final DocumentDao documentDao = new DocumentDao(mongo);
		final RackDao rackDao = new RackDao(mongo);

		JsonObject gridfsConf = container.config().getObject("gridfs-config");
		container.deployModule("com.wse~gridfs-persistor~0.1.0-SNAPSHOT", gridfsConf);

		WorkspaceService service = new WorkspaceService(vertx, container, rm, securedActions);

		rm.get("/workspace", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request);
			}
		});

		service.post("/document", "addDocument");

		service.get("/document/:id", "getDocument");

		service.put("document/:id", "updateDocument");

		service.delete("/document/:id", "deleteDocument");

		rm.get("/documents", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String query = null;
				String forApplication = getOrElse(request.params()
						.get("application"), WorkspaceService.WORKSPACE_NAME);
				if (request.params().get("hierarchical") != null) {
					query = "{ \"file\" : { \"$exists\" : true }, \"application\": \"" +
							forApplication + "\", \"folder\" : { \"$exists\" : false }}";
				} else {
					query = "{ \"file\" : { \"$exists\" : true }, \"application\": \"" +
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
			}
		});

		rm.get("/documents/:folder", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String query = null;
				String expectedFolder = request.params().get("folder");
				String forApplication = getOrElse(request.params()
						.get("application"), WorkspaceService.WORKSPACE_NAME);
				if (request.params().get("hierarchical") != null) {
					query = "{ \"file\" : { \"$exists\" : true }, \"application\": \"" +
							forApplication + "\", \"folder\" : \"" + expectedFolder + "\" }";
				} else {
					query = "{ \"file\" : { \"$exists\" : true }, \"application\": \"" +
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
			}
		});

		rm.put("/document/move/:id/:folder", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String obj = "{ \"$set\" : { \"folder\": \"" + request.params().get("folder") +
						"\", \"modified\" : \""+ MongoDb.formatDate(new Date()) + "\" }}";
				documentDao.update(request.params().get("id"), new JsonObject(obj), new Handler<JsonObject>() {
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
		});

		rm.put("/documents/move/:ids/:folder", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
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
		});

		rm.put("/document/trash/:id", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String obj = "{ \"$set\" : { \"folder\": \"Trash\", \"modified\" : \"" +
						MongoDb.formatDate(new Date()) + "\" }}";
				documentDao.update(request.params().get("id"), new JsonObject(obj), new Handler<JsonObject>() {
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
		});


		service.post("/documents/copy/:ids/:folder", "copyDocuments");

		service.post("/document/copy/:id/:folder", "copyDocument");

		rm.post("/document/:id/comment", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
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
		});

		rm.get("/folders", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				final String hierarchical = request.params().get("hierarchical");
				final String relativePath = request.params().get("relativePath");
				String query = null;
				if (relativePath != null) {
					query = "{ \"folder\" : { \"$regex\" : \"^" + relativePath + "(_|$)\" }}";
				} else {
					query = "{}";
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
			}
		});

		service.post("/rack/:to", "addRackDocument");

		rm.get("/rack/documents", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String query = "{ \"file\" : { \"$exists\" : true }, \"folder\" : { \"$ne\" : \"Trash\" }}"; // TODO add check on "to" attribute
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
			}
		});

		service.get("/rack/:id", "getRackDocument");

		service.delete("/rack/:id", "deleteRackDocument");

		service.post("/rack/documents/copy/:ids/:folder", "copyRackDocuments");

		service.post("/rack/document/copy/:id/:folder", "copyRackDocument");

		rm.put("/rack/trash/:id", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String obj = "{ \"$set\" : { \"folder\": \"Trash\", \"modified\" : \""+
						MongoDb.formatDate(new Date()) + "\" }}";
				rackDao.update(request.params().get("id"), new JsonObject(obj), new Handler<JsonObject>() {
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
		});

		rm.get("/rack/documents/Trash", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String	query = "{ \"file\" : { \"$exists\" : true }, \"folder\" : \"Trash\" }";
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
			}
		});

		SecurityHandler.addFilter(new ActionFilter(service.securedUriBinding(),
				vertx.eventBus(), new WorkspaceResourcesProvider(mongo)));

	}

}
