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
		container.deployModule("io.vertx~mod-mongo-persistor~2.0.0-CR1", mongodbConf, 1, new AsyncResultHandler<String>() {
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
				final String filename = filesRepository + File.separator + UUID.randomUUID().toString();
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
				final String filename = filesRepository + File.separator + UUID.randomUUID().toString();
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
									documentDao.delete(id, request.response());
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
							request.response().setStatusCode(404).end();
						}
					}
				});
			}
		});
	}

}
