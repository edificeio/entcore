package edu.one.core.workspace;

import static edu.one.core.infra.Utils.getOrElse;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
import edu.one.core.infra.FileUtils;
import edu.one.core.infra.MongoDb;
import edu.one.core.workspace.dao.DocumentDao;
import edu.one.core.workspace.dao.RackDao;

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

		final String filesRepository = config.getString("files-repository");

		rm.get("/workspace", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request);
			}
		});

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
						mongo.save(DocumentDao.DOCUMENTS_COLLECTION, doc, new Handler<Message<JsonObject>>() {
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
							request.response().putHeader("Content-Disposition" , "attachment; filename=" +
									result.getString("name"))
								.sendFile(result.getString("file"));
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
									mongo.update(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(query), set,
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
												renderJson(request, result, 204);
											} else {
												renderError(request, result);
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
				String query = null;
				if (request.params().get("hierarchical") != null) {
					query = "{ \"file\" : { \"$exists\" : true }, \"folder\" : { \"$exists\" : false }}";
				} else {
					query = "{ \"file\" : { \"$exists\" : true }}";
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
				if (request.params().get("hierarchical") != null) {
					query = "{ \"file\" : { \"$exists\" : true }, \"folder\" : \"" + expectedFolder + "\" }";
				} else {
					query = "{ \"file\" : { \"$exists\" : true }, \"folder\" : { \"$regex\" : \"^" +
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

		rm.post("/documents/copy/:ids/:folder", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String ids = request.params().get("ids"); // TODO refactor with json in request body
				String folder = request.params().get("folder");
				if (ids != null && folder != null &&
						!ids.trim().isEmpty() && !folder.trim().isEmpty()) {
					JsonArray idsArray = new JsonArray(ids.split(","));
					String criteria = "{ \"_id\" : { \"$in\" : " + idsArray.encode() + "}}";
					mongo.find(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(criteria), new Handler<Message<JsonObject>>() {
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
										String path = generateFilePath(filesRepository);
										dest.putString("file", path);
										vertx.fileSystem().copy(filePath, path, new Handler<AsyncResult<Void>>() {
											@Override
											public void handle(AsyncResult<Void> copied) {
												persist(insert, number.decrementAndGet());
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
		});

		rm.post("/document/:id/comment", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
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

		rm.post("/rack/:to", new Handler<HttpServerRequest>() {
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
						doc.putString("to", request.params().get("to")); // TODO check existance and validity (neo4j)
						doc.putString("from", UUID.randomUUID().toString()); // TODO replace by user (session)
						doc.putString("sent", now);
						doc.putObject("metadata", metadata);
						doc.putString("file", filename);
						mongo.save(RackDao.RACKS_COLLECTION, doc, new Handler<Message<JsonObject>>() {
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
			}
		});


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

		rm.get("/rack/:id", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				rackDao.findById(request.params().get("id"), new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject res) {
						String status = res.getString("status");
						JsonObject result = res.getObject("result");
						if ("ok".equals(status) && result != null && result.getString("file") != null) {
							request.response().putHeader("Content-Disposition" , "attachment; filename=" +
									result.getString("name")).sendFile(result.getString("file"));
						} else {
							request.response().setStatusCode(404).end();
						}
					}
				});
			}
		});

		rm.delete("/rack/:id", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				final String id = request.params().get("id");
				rackDao.findById(id, new Handler<JsonObject>() {
					@Override
					public void handle(JsonObject res) {
						String status = res.getString("status");
						JsonObject result = res.getObject("result");
						if ("ok".equals(status) && result != null && result.getString("file") != null) {
							vertx.fileSystem().delete(result.getString("file"), new Handler<AsyncResult<Void>>() {
								@Override
								public void handle(AsyncResult<Void> event) {
									rackDao.delete(id, new Handler<JsonObject>() {
										@Override
										public void handle(JsonObject result) {
											if ("ok".equals(result.getString("status"))) {
												renderJson(request, result, 204);
											} else {
												renderError(request, result);
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

		rm.post("/rack/documents/copy/:ids/:folder", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String ids = request.params().get("ids"); // TODO refactor with json in request body
				String folder = request.params().get("folder");
				if (ids != null && folder != null &&
						!ids.trim().isEmpty() && !folder.trim().isEmpty()) {
					JsonArray idsArray = new JsonArray(ids.split(","));
					String criteria = "{ \"_id\" : { \"$in\" : " + idsArray.encode() + "}}";
					mongo.find(RackDao.RACKS_COLLECTION, new JsonObject(criteria), new Handler<Message<JsonObject>>() {
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
										String path = generateFilePath(filesRepository);
										dest.putString("file", path);
										vertx.fileSystem().copy(filePath, path, new Handler<AsyncResult<Void>>() {
											@Override
											public void handle(AsyncResult<Void> copied) {
												persist(insert, number.decrementAndGet());
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
		});

		rm.put("/rack/copy/:id/:folder", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				rackDao.findById(request.params().get("id"), new Handler<JsonObject>() {
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
		});

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
	}

	private String generateFilePath(final String filesRepository) {
		return filesRepository + File.separator + UUID.randomUUID().toString();
	}

}
