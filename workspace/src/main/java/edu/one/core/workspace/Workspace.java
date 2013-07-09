package edu.one.core.workspace;

import static edu.one.core.infra.Utils.getOrElse;

import java.io.File;
import java.util.Date;
import java.util.UUID;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.Controller;
import edu.one.core.infra.FileUtils;
import edu.one.core.infra.MongoDb;
import edu.one.core.workspace.dao.DocumentDao;

public class Workspace extends Controller {

	private static final String DOCUMENTS_COLLECTION = "documents";

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

		final String filesRepository = config.getString("files-repository");

		rm.post("/document", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				final String filename = generateFilePath(filesRepository);
				FileUtils.writeUploadFile(request, filename, new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject metadata) {
						JsonObject doc = new JsonObject();
						doc.putString("name", getOrElse(request
								.params().get("name"), metadata.getString("filename")));
						final String now = MongoDb.formatDate(new Date());
						doc.putString("created", now);
						doc.putString("modified", now);
						doc.putObject("metadata", metadata);
						doc.putString("file", filename);
						mongo.save(DOCUMENTS_COLLECTION, doc, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> res) {
								if ("ok".equals(res.body().getString("status"))) {
									request.response().setStatusCode(201).end(res.body().toString());
								} else {
									request.response().setStatusCode(500).end(res.body().toString());
								}
							}
						});
					}
				});
			}
		});

		rm.get("/document/:id", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				documentDao.findById(request.params().get("id"), new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject res) {
						String status = res.getString("status");
						JsonObject result = res.getObject("result");
						if ("ok".equals(status) && result != null && result.getString("file") != null) {
							request.response().sendFile(result.getString("file"));
						} else {
							request.response().setStatusCode(404).end();
						}
					}
				});
			}
		});

		rm.put("/document/:id", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				final String filename = generateFilePath(filesRepository);
				FileUtils.writeUploadFile(request, filename, new Handler<JsonObject>() {
					@Override
					public void handle(final JsonObject metadata) {
						documentDao.findById(request.params().get("id"), new Handler<JsonObject>() {
							@Override
							public void handle(final JsonObject old) {
								if ("ok".equals(old.getString("status"))) {
									JsonObject set = new JsonObject();
									JsonObject doc = new JsonObject();
									doc.putString("name", getOrElse(request
											.params().get("name"), metadata.getString("filename")));
									final String now = MongoDb.formatDate(new Date());
									doc.putString("modified", now);
									doc.putObject("metadata", metadata);
									doc.putString("file", filename);
									String query = "{ \"_id\": \"" + request.params().get("id") + "\"}";
									set.putObject("$set", doc);
									mongo.update(DOCUMENTS_COLLECTION, new JsonObject(query), set,
											new Handler<Message<JsonObject>>() {
										@Override
										public void handle(final Message<JsonObject> res) {
											String status = res.body().getString("status");
											JsonObject result = old.getObject("result");
											if ("ok".equals(status) && result != null) {
												vertx.fileSystem().delete(result.getString("file"),
														new Handler<AsyncResult<Void>>() {
													@Override
													public void handle(AsyncResult<Void> event) {
														request.response().setStatusCode(200)
															.end(res.body().toString());
													}
												});
											} else {
												request.response().setStatusCode(500).end(res.body().toString());
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
		});

		rm.delete("/document/:id", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				final String id = request.params().get("id");
				documentDao.findById(id, new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject res) {
						String status = res.getString("status");
						JsonObject result = res.getObject("result");
						if ("ok".equals(status) && result != null && result.getString("file") != null) {
							vertx.fileSystem().delete(result.getString("file"), new Handler<AsyncResult<Void>>() {
								@Override
								public void handle(AsyncResult<Void> event) {
									documentDao.delete(id, new Handler<JsonObject>() {
										@Override
										public void handle(JsonObject result) {
											if ("ok".equals(result.getString("status"))) {
												request.response().setStatusCode(204).end(result.toString());
											} else {
												request.response().setStatusCode(500).end(result.toString());
											}
										}
									});
								}
							});
						} else {
							request.response().setStatusCode(404).end();
						}
					}
				});
			}
		});

		rm.get("/documents", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String query = "{ \"file\" : { \"$exists\" : true }}";
				mongo.find(DOCUMENTS_COLLECTION, new JsonObject(query), new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> res) {
						String status = res.body().getString("status");
						JsonArray results = res.body().getArray("results");
						if ("ok".equals(status) && results != null) {
							request.response().end(results.encode());
						} else {
							request.response().end("[]");
						}
					}
				});
			}
		});

		rm.get("/documents/:folder", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String query = "{ \"file\" : { \"$exists\" : true }, \"folder\" : \"" +
					request.params().get("folder") + "\" }";
				mongo.find(DOCUMENTS_COLLECTION, new JsonObject(query), new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> res) {
						String status = res.body().getString("status");
						JsonArray results = res.body().getArray("results");
						if ("ok".equals(status) && results != null) {
							request.response().end(results.encode());
						} else {
							request.response().end("[]");
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
							request.response().end(res.toString());
						} else {
							request.response().setStatusCode(404).end(res.toString());
						}
					}
				});
			}
		});

		rm.put("/document/copy/:id/:folder", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				documentDao.findById(request.params().get("id"), new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject src) {
						if ("ok".equals(src.getString("status")) && src.getObject("result") != null) {
							JsonObject orig = src.getObject("result");
							final JsonObject dest = orig.copy();
							String now = MongoDb.formatDate(new Date());
							dest.removeField("_id");
							dest.putString("created", now);
							dest.putString("modified", now);
							dest.putString("folder", request.params().get("folder"));
							String filePath = orig.getString("file");
							if (filePath != null) {
								String path = generateFilePath(filesRepository);
								dest.putString("file", path);
								vertx.fileSystem().copy(filePath, path, new Handler<AsyncResult<Void>>() {
									@Override
									public void handle(AsyncResult<Void> copied) {
										persist(dest);
									}
								});
							} else {
								persist(dest);
							}
						} else {
							request.response().setStatusCode(404).end(src.toString());
						}
					}

					private void persist(final JsonObject dest) {
						documentDao.save(dest, new Handler<JsonObject>() {
							@Override
							public void handle(JsonObject res) {
								if ("ok".equals(res.getString("status"))) {
									request.response().end(res.toString());
								} else {
									request.response().setStatusCode(500).end(res.toString());
								}
							}
						});
					}

				});
			}
		});

		rm.get("/folders", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				mongo.distinct(DOCUMENTS_COLLECTION, "folder", new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> res) {
						if ("ok".equals(res.body().getString("status"))) {
							JsonArray values = res.body().getArray("values", new JsonArray("[]"));
							request.response().end(values.encode());
						} else {
							request.response().end("[]");
						}
					}
				});
			}
		});

	}

	private String generateFilePath(final String filesRepository) {
		return filesRepository + File.separator + UUID.randomUUID().toString();
	}

}
