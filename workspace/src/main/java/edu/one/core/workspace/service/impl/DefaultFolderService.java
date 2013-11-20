package edu.one.core.workspace.service.impl;

import static edu.one.core.workspace.dao.DocumentDao.DOCUMENTS_COLLECTION;

import com.mongodb.QueryBuilder;
import edu.one.core.infra.*;
import edu.one.core.infra.security.resources.UserInfos;
import edu.one.core.workspace.service.FolderService;
import edu.one.core.workspace.service.WorkspaceService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static edu.one.core.infra.Utils.getOrElse;

public class DefaultFolderService implements FolderService {

	private final MongoDb mongo;
	private final EventBus eb;
	private final String gridfsAddress;
	private final DateFormat format;

	public DefaultFolderService(EventBus eb, MongoDb mongo, String gridfsAddress) {
		this.mongo = mongo;
		this.eb = eb;
		this.gridfsAddress = gridfsAddress;
		this.format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	}

	@Override
	public void create(String name, String path, String application,
			UserInfos owner, final Handler<Either<String, JsonObject>> result) {
		if (owner == null) {
			result.handle(new Either.Left<String, JsonObject>("Invalid user."));
			return;
		}
		if (name == null || name.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("Invalid parameter."));
			return;
		}
		final JsonObject doc = new JsonObject();
		String now = MongoDb.formatDate(new Date());
		doc.putString("created", now);
		doc.putString("modified", now);
		doc.putString("owner", owner.getUserId());
		doc.putString("ownerName", owner.getUsername());
		doc.putString("name", name);
		String folder;
		if (path != null && !path.trim().isEmpty()) {
			folder = path + "_" + name;
		} else {
			folder = name;
		}
		doc.putString("folder", folder);
		doc.putString("application", getOrElse(application, WorkspaceService.WORKSPACE_NAME));
		QueryBuilder alreadyExist = QueryBuilder.start("owner").is(owner.getUserId()).put("folder").is(folder);
		mongo.count(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(alreadyExist),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) &&
						event.body().getInteger("count") == 0) {
					mongo.save(DOCUMENTS_COLLECTION, doc, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> res) {
							result.handle(Utils.validResult(res));
						}
					});
				} else {
					result.handle(new Either.Left<String, JsonObject>("Folder already exists."));
				}
			}
		});
	}

	@Override
	public void move(String id, final String path, final UserInfos owner,
			final Handler<Either<String, JsonObject>> result) {
		if (owner == null) {
			result.handle(new Either.Left<String, JsonObject>("Invalid user."));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("Invalid parameter."));
			return;
		}
		QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner").is(owner.getUserId());
		JsonObject keys = new JsonObject().putNumber("folder", 1).putNumber("name", 1);
		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys,
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final String folder = event.body().getObject("result", new JsonObject()).getString("folder");
				final String name = event.body().getObject("result", new JsonObject()).getString("name");
				if ("ok".equals(event.body().getString("status")) &&
						folder != null && !folder.trim().isEmpty() && name != null && !name.trim().isEmpty()) {
					if (path != null && path.startsWith(folder)) {
						result.handle(new Either.Left<String, JsonObject>("Forbidden to move the folder in itself."));
					} else {
						recursiveMove(folder, name, owner, path, result);
					}
				} else {
					result.handle(new Either.Left<String, JsonObject>("Folder not found."));
				}
			}
		});
	}

	private void recursiveMove(final String folder, final String name, final UserInfos owner,
				final String path, final Handler<Either<String, JsonObject>> result) {
		final String folderAttr = "Trash".equals(path) ? "old-folder" : "folder";
		QueryBuilder q = QueryBuilder.start("owner").is(owner.getUserId()).put(folderAttr)
				.regex(Pattern.compile("^" + folder + "($|_)"));
		mongo.distinct(DOCUMENTS_COLLECTION, folderAttr, MongoQueryBuilder.build(q),
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> d) {
				JsonArray directories = d.body().getArray("values", new JsonArray());
				if ("ok".equals(d.body().getString("status")) && directories.size() > 0) {
					final AtomicInteger remaining = new AtomicInteger(directories.size());
					final AtomicInteger count = new AtomicInteger(0);
					String dest;
					if (path != null && !path.trim().isEmpty()) {
						dest = path + "_" + name;
					} else {
						dest = name;
					}
					for (Object o : directories) {
						if (!(o instanceof String)) continue;
						String dir = (String) o;
						QueryBuilder qf = QueryBuilder.start("owner").is(owner.getUserId())
								.put(folderAttr).is(dir);
						MongoUpdateBuilder modifier = new MongoUpdateBuilder();
						modifier.set("folder", dir.replaceFirst("^" + folder, dest));
						mongo.update(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(qf),
								modifier.build(), false, true, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> res) {
								count.getAndAdd(res.body().getInteger("number", 0));
								if (remaining.decrementAndGet() == 0) {
									res.body().putNumber("number", count.get());
									result.handle(Utils.validResult(res));
								}
							}
						});
					}
				} else {
					result.handle(new Either.Left<String, JsonObject>("Folder not found."));
				}
			}
		});
	}

	@Override
	public void copy(String id, final String n, final String path, final UserInfos owner,
			final Handler<Either<String, JsonObject>> result) {
		if (owner == null) {
			result.handle(new Either.Left<String, JsonObject>("Invalid user."));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("Invalid parameter."));
			return;
		}
		QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner").is(owner.getUserId());
		JsonObject keys = new JsonObject().putNumber("folder", 1).putNumber("name", 1);
		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys,
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final String folder = event.body().getObject("result", new JsonObject()).getString("folder");
				final String n1 = event.body().getObject("result", new JsonObject()).getString("name");
				if ("ok".equals(event.body().getString("status")) &&
						folder != null && !folder.trim().isEmpty() && n1 != null && !n1.trim().isEmpty()) {
					final String folderAttr = "folder";
					QueryBuilder q = QueryBuilder.start("owner").is(owner.getUserId()).put(folderAttr)
							.regex(Pattern.compile("^" + folder + "($|_)"));
					mongo.find(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(q),
							new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> src) {
							final JsonArray origs = src.body().getArray("results", new JsonArray());
							if ("ok".equals(src.body().getString("status")) && origs.size() > 0) {
								final AtomicInteger number = new AtomicInteger(origs.size());
								final JsonArray insert = new JsonArray();
								String name = (n != null && !n.trim().isEmpty()) ? n : n1;
								final String destFolderName = (path != null && !path.trim().isEmpty()) ?
									path + "_" + name : name;
								QueryBuilder alreadyExist = QueryBuilder.start("owner").is(owner.getUserId())
										.put("folder").is(destFolderName);
								mongo.count(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(alreadyExist),
										new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> event) {
										String destFolder;
										if ("ok".equals(event.body().getString("status")) &&
												event.body().getInteger("count") == 0) {
											destFolder = destFolderName;
										} else {
											destFolder = destFolderName + "-" + format.format(new Date());
										}
										for (Object o: origs) {
											if (!(o instanceof JsonObject)) continue;
											JsonObject orig = (JsonObject) o;
											final JsonObject dest = orig.copy();
											String now = MongoDb.formatDate(new Date());
											dest.removeField("_id");
											dest.putString("created", now);
											dest.putString("modified", now);
											dest.putString("folder", dest.getString("folder", "")
													.replaceFirst("^" + folder, destFolder));
											insert.add(dest);
											String filePath = orig.getString("file");
											if (filePath != null) {
												FileUtils.gridfsCopyFile(filePath, eb, gridfsAddress,
														new Handler<JsonObject>() {
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
								result.handle(new Either.Left<String, JsonObject>("Folder not found."));
							}
						}

						private void persist(final JsonArray insert, int remains) {
							if (remains == 0) {
								mongo.insert(DOCUMENTS_COLLECTION, insert,
										new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> inserted) {
										result.handle(Utils.validResult(inserted));
									}
								});
							}
						}
					});
				} else {
					result.handle(new Either.Left<String, JsonObject>("Folder not found."));
				}
			}
		});
	}

	@Override
	public void trash(String id, final UserInfos owner, final Handler<Either<String, JsonObject>> result) {
		if (owner == null) {
			result.handle(new Either.Left<String, JsonObject>("Invalid user."));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("Invalid parameter."));
			return;
		}
		QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner").is(owner.getUserId());
		JsonObject keys = new JsonObject().putNumber("folder", 1).putNumber("name", 1);
		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						final String folder = event.body().getObject("result", new JsonObject()).getString("folder");
						final String name = event.body().getObject("result", new JsonObject()).getString("name");
						if ("ok".equals(event.body().getString("status")) &&
								folder != null && !folder.trim().isEmpty()) {
							QueryBuilder q = QueryBuilder.start("owner").is(owner.getUserId()).put("folder")
									.regex(Pattern.compile("^" + folder + "($|_)"));
							MongoUpdateBuilder modifier = new MongoUpdateBuilder();
							modifier.rename("folder", "old-folder");
							mongo.update(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(q),
									modifier.build(), false, true, new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> e) {
									if ("ok".equals(e.body().getString("status"))) {
										recursiveMove(folder, name, owner, "Trash", result);
									} else {
										result.handle(new Either.Left<String, JsonObject>("Update error."));
									}
								}
							});
						} else {
							result.handle(new Either.Left<String, JsonObject>("Folder not found."));
						}
					}
				});
	}

	@Override
	public void delete(String id, final UserInfos owner, final Handler<Either<String, JsonObject>> result) {
		if (owner == null) {
			result.handle(new Either.Left<String, JsonObject>("Invalid user."));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("Invalid parameter."));
			return;
		}
		QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner").is(owner.getUserId());
		JsonObject keys = new JsonObject().putNumber("folder", 1);
		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys,
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				String folder = event.body().getObject("result", new JsonObject()).getString("folder");
				if ("ok".equals(event.body().getString("status")) && folder != null && !folder.trim().isEmpty()) {
					QueryBuilder q = QueryBuilder.start("owner").is(owner.getUserId()).put("folder")
							.regex(Pattern.compile("^" + folder + "($|_)"));
					mongo.delete(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(q),
							new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> res) {
									result.handle(Utils.validResult(res));
								}
							});
				} else {
					result.handle(new Either.Left<String, JsonObject>("Folder not found."));
				}
			}
		});
	}

	@Override
	public void list(String path, UserInfos owner, boolean hierarchical,
			final Handler<Either<String, JsonArray>> results) {
		if (owner == null) {
			results.handle(new Either.Left<String, JsonArray>("Invalid user."));
			return;
		}
		QueryBuilder q = QueryBuilder.start("owner").is(owner.getUserId()).put("file").exists(false)
				.put("application").is(WorkspaceService.WORKSPACE_NAME);
		if (path != null && !path.trim().isEmpty()) {
			if (hierarchical) {
				q = q.put("folder").regex(Pattern.compile("^" + path + "_[^_]+$"));
			} else {
				q = q.put("folder").regex(Pattern.compile("^" + path + "_"));
			}
		} else if (hierarchical) {
			q = q.put("folder").regex(Pattern.compile("^[^_]+$"));
		}
		mongo.find(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(q), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				results.handle(Utils.validResults(res));
			}
		});
	}

	@Override
	public void restore(String id, final UserInfos owner, final Handler<Either<String, JsonObject>> result) {
		if (owner == null) {
			result.handle(new Either.Left<String, JsonObject>("Invalid user."));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("Invalid parameter."));
			return;
		}
		QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner").is(owner.getUserId());
		JsonObject keys = new JsonObject().putNumber("folder", 1);
		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						final String folder = event.body().getObject("result", new JsonObject()).getString("folder");
						if ("ok".equals(event.body().getString("status")) &&
								folder != null && !folder.trim().isEmpty()) {
							QueryBuilder q = QueryBuilder.start("owner").is(owner.getUserId()).put("folder")
									.regex(Pattern.compile("^" + folder + "($|_)")).put("old-folder").exists(true);
							MongoUpdateBuilder modifier = new MongoUpdateBuilder();
							modifier.rename("old-folder", "folder");
							mongo.update(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(q),
									modifier.build(), false, true, new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> res) {
									result.handle(Utils.validResult(res));
								}
							});
						} else {
							result.handle(new Either.Left<String, JsonObject>("Folder not found."));
						}
					}
				});
	}

}
