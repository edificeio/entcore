/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.workspace.service.impl;

import static fr.wseduc.webutils.Utils.getOrElse;
import static org.entcore.workspace.dao.DocumentDao.DOCUMENTS_COLLECTION;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserInfos;
import org.entcore.workspace.service.FolderService;
import org.entcore.workspace.service.WorkspaceService;
import org.entcore.common.storage.Storage;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.Either.Right;

public class DefaultFolderService implements FolderService {

	private static final Logger log = LoggerFactory.getLogger(DefaultFolderService.class);
	private final MongoDb mongo;
	private final Storage storage;
	private final DateFormat format;

	public DefaultFolderService(MongoDb mongo, Storage storage) {
		this.mongo = mongo;
		this.storage = storage;
		this.format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	}

	public void getParentRights(final String parentName, final String parentFolder, final UserInfos owner, final Handler<Either<String, JsonArray>> result){
		getParentRights(parentName, parentFolder, owner.getUserId(), result);
	}
	public void getParentRights(final String parentName, final String parentFolder, final String owner, final Handler<Either<String, JsonArray>> result){
		QueryBuilder parentFolderQuery = QueryBuilder.start("owner").is(owner)
				.and("name").is(parentName)
				.and("folder").is(parentFolder);

		mongo.findOne(DOCUMENTS_COLLECTION,  MongoQueryBuilder.build(parentFolderQuery), new Handler<Message<JsonObject>>(){
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status")) && event.body().containsKey("result")){
					JsonObject parent = event.body().getJsonObject("result");
					JsonArray parentSharedRights = parent != null ? parent.getJsonArray("shared", null) : null;

					result.handle(new Either.Right<String, JsonArray>(parentSharedRights));
				} else {
					result.handle(new Either.Left<String, JsonArray>("workspace.folder.not.found"));
				}
			}
		});
	}

	@Override
	public void create(String name, final String path, String application,
			final UserInfos owner, final Handler<Either<String, JsonObject>> result) {
		if (owner == null) {
			result.handle(new Either.Left<String, JsonObject>("workspace.invalid.user"));
			return;
		}
		if (name == null || name.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("workspace.invalid.folder.name"));
			return;
		}
		final JsonObject doc = new JsonObject();
		String now = MongoDb.formatDate(new Date());
		doc.put("created", now);
		doc.put("modified", now);
		doc.put("owner", owner.getUserId());
		doc.put("ownerName", owner.getUsername());
		doc.put("name", name);
		String folder;
		if (path != null && !path.trim().isEmpty()) {
			folder = path + "_" + name;
		} else {
			folder = name;
		}
		doc.put("folder", folder);
		doc.put("application", getOrElse(application, WorkspaceService.WORKSPACE_NAME));
		QueryBuilder alreadyExist = QueryBuilder.start("owner").is(owner.getUserId()).put("folder").is(folder);
		mongo.count(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(alreadyExist),
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if ("ok".equals(event.body().getString("status")) &&
								event.body().getInteger("count") == 0) {

							if(path == null || path.trim().isEmpty()){
								mongo.save(DOCUMENTS_COLLECTION, doc, new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> res) {
										result.handle(Utils.validResult(res));
									}
								});
							} else {
								//If the folder has a parent folder, replicate sharing rights
								String[] splittedPath = path.split("_");
								String parentName = splittedPath[splittedPath.length - 1];
								String parentFolder = path;

								getParentRights(parentName, parentFolder, owner, new Handler<Either<String, JsonArray>>(){
									public void handle(Either<String, JsonArray> event) {
										final JsonArray parentSharedRights = event.right() == null || event.isLeft() ?
												null : event.right().getValue();

										if(parentSharedRights != null)
											doc.put("shared", parentSharedRights);

										mongo.save(DOCUMENTS_COLLECTION, doc, new Handler<Message<JsonObject>>() {
											@Override
											public void handle(Message<JsonObject> res) {
												result.handle(Utils.validResult(res));
											}
										});
									}

								});
							}
						} else {
							result.handle(new Either.Left<String, JsonObject>("workspace.folder.already.exists"));
						}
					}
				});
	}

	@Override
	public void move(String id, final String path, final UserInfos owner,
			final Handler<Either<String, JsonObject>> result) {
		if (owner == null) {
			result.handle(new Either.Left<String, JsonObject>("workspace.invalid.user"));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("workspace.folder.not.found"));
			return;
		}
		QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner").is(owner.getUserId());
		JsonObject keys = new JsonObject().put("folder", 1).put("name", 1);
		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						final String folder = event.body().getJsonObject("result", new JsonObject()).getString("folder");
						final String name = event.body().getJsonObject("result", new JsonObject()).getString("name");
						if ("ok".equals(event.body().getString("status")) &&
								folder != null && !folder.trim().isEmpty() && name != null && !name.trim().isEmpty()) {
							if (path != null && path.startsWith(folder)) {
								result.handle(new Either.Left<String, JsonObject>(
										"workspace.forbidden.move.folder.in.itself"));
							} else {
								String dest;
								if (path != null && !path.trim().isEmpty()) {
									dest = path + "_" + name;
								} else {
									dest = name;
								}
								QueryBuilder q = QueryBuilder.start("owner").is(owner.getUserId())
										.put("folder").is(dest);
								mongo.count(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(q),
										new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> event) {
										if ("ok".equals(event.body().getString("status")) &&
												event.body().getInteger("count") == 0) {
											recursiveMove(folder, name, owner.getUserId(), path, result);
										} else {
											result.handle(new Either.Left<String, JsonObject>(
													"workspace.folder.already.exists"));
										}
									}
								});
							}
						} else {
							result.handle(new Either.Left<String, JsonObject>("workspace.folder.not.found"));
						}
					}
				});
	}

	private void recursiveMove(final String folder, final String name, final String owner,
				final String path, final Handler<Either<String, JsonObject>> result) {
		final String folderAttr = "Trash".equals(path) ? "old-folder" : "folder";
		final QueryBuilder q = QueryBuilder.start("owner").is(owner).put(folderAttr)
				.regex(Pattern.compile("^" + Pattern.quote(folder) + "($|_)"));

		//If the folder has a parent folder, replicate sharing rights
		String[] splittedPath = path.split("_");
		String parentName = splittedPath[splittedPath.length - 1];
		String parentFolder = path;

		getParentRights(parentName, parentFolder, owner, new Handler<Either<String, JsonArray>>(){
			public void handle(Either<String, JsonArray> event) {
				final JsonArray parentSharedRights = event.right() == null || event.isLeft() ?
						null : event.right().getValue();

				mongo.distinct(DOCUMENTS_COLLECTION, folderAttr, MongoQueryBuilder.build(q),
					new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> d) {
							JsonArray directories = d.body().getJsonArray("values", new JsonArray());
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
									QueryBuilder qf = QueryBuilder.start("owner").is(owner)
											.put(folderAttr).is(dir);
									MongoUpdateBuilder modifier = new MongoUpdateBuilder();
									modifier.set("folder", dir.replaceFirst("^" + Pattern.quote(folder), Matcher.quoteReplacement(dest)));

									if("Trash".equals(path) || parentSharedRights == null)
										modifier.unset("shared");
									else
										modifier.set("shared", parentSharedRights);

									mongo.update(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(qf),
											modifier.build(), false, true, new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> res) {
											count.getAndAdd(res.body().getInteger("number", 0));
											if (remaining.decrementAndGet() == 0) {
												res.body().put("number", count.get());
												result.handle(Utils.validResult(res));
											}
										}
									});
								}
							} else {
								result.handle(new Either.Left<String, JsonObject>("workspace.folder.not.found"));
							}
						}
					}
				);
			}

		});


	}

	@Override
	public void copy(final String id, final String n, final String p, final UserInfos owner, final long emptySize,
			final Handler<Either<String, JsonArray>> result) {
		if (owner == null) {
			result.handle(new Either.Left<String, JsonArray>("workspace.invalid.user"));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonArray>("workspace.folder.not.found"));
			return;
		}
		final String path = getOrElse(p, "");
		//If the folder has a parent folder, replicate sharing rights
		String[] splittedPath = path.split("_");
		String parentName = splittedPath[splittedPath.length - 1];
		String parentFolder = path;

		getParentRights(parentName, parentFolder, owner, new Handler<Either<String, JsonArray>>(){
			public void handle(Either<String, JsonArray> event) {
				final JsonArray parentSharedRights = event.right() == null || event.isLeft() ?
						null : event.right().getValue();

				QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner").is(owner.getUserId());
				JsonObject keys = new JsonObject().put("folder", 1).put("name", 1);

				mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys,
						new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						final String folder = event.body().getJsonObject("result", new JsonObject()).getString("folder");
						final String n1 = event.body().getJsonObject("result", new JsonObject()).getString("name");
						if ("ok".equals(event.body().getString("status")) &&
								folder != null && !folder.trim().isEmpty() && n1 != null && !n1.trim().isEmpty()) {
							final String folderAttr = "folder";
							QueryBuilder q = QueryBuilder.start("owner").is(owner.getUserId()).put(folderAttr)
									.regex(Pattern.compile("^" + Pattern.quote(folder) + "($|_)"));
							mongo.find(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(q),
									new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> src) {
									final JsonArray origs = src.body().getJsonArray("results", new JsonArray());
									if ("ok".equals(src.body().getString("status")) && origs.size() > 0) {
										long size = 0;
										for (Object o: origs) {
											if (!(o instanceof JsonObject)) continue;
											JsonObject metadata = ((JsonObject) o).getJsonObject("metadata");
											if (metadata != null) {
												size += metadata.getLong("size", 0l);
											}
										}
										if (size > emptySize) {
											result.handle(new Either.Left<String, JsonArray>("files.too.large"));
											return;
										}
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
													dest.remove("_id");
													dest.put("created", now);
													dest.put("modified", now);
													dest.put("folder", dest.getString("folder", "")
															.replaceFirst("^" + Pattern.quote(folder),  Matcher.quoteReplacement(destFolder)));
													dest.put("shared", parentSharedRights);
													insert.add(dest);
													String filePath = orig.getString("file");
													if (filePath != null) {
														storage.copyFile(filePath,
																new Handler<JsonObject>() {
															@Override
															public void handle(JsonObject event) {
																if (event != null && "ok".equals(event.getString("status"))) {
																	dest.put("file", event.getString("_id"));
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
										result.handle(new Either.Left<String, JsonArray>("workspace.folder.not.found"));
									}
								}

								private void persist(final JsonArray insert, int remains) {
									if (remains == 0) {
										mongo.insert(DOCUMENTS_COLLECTION, insert,
												new Handler<Message<JsonObject>>() {
											@Override
											public void handle(Message<JsonObject> inserted) {
												if ("ok".equals(inserted.body().getString("status"))) {
													result.handle(new Either.Right<String, JsonArray>(insert));
												} else {
													result.handle(new Either.Left<String, JsonArray>(
															inserted.body().getString("message")));
												}
											}
										});
									}
								}
							});
						} else {
							result.handle(new Either.Left<String, JsonArray>("workspace.folder.not.found"));
						}
					}
				});
			}
		});
	}

	@Override
	public void trash(String id, final UserInfos owner, final Handler<Either<String, JsonObject>> result) {
		if (owner == null) {
			result.handle(new Either.Left<String, JsonObject>("workspace.invalid.user"));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("workspace.folder.not.found"));
			return;
		}
		final QueryBuilder resourceQuery = QueryBuilder.start("_id").is(id);

		final List<DBObject> groups = new ArrayList<>();
		groups.add(QueryBuilder.start("userId").is(owner.getUserId()).get());
		for (String gpId: owner.getGroupsIds()) {
			groups.add(QueryBuilder.start("groupId").is(gpId).get());
		}

		final QueryBuilder rightQuery = new QueryBuilder().or(
				QueryBuilder.start("owner").is(owner.getUserId()).get(),
				QueryBuilder.start("shared").elemMatch(
						new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
				).get());

		final QueryBuilder query = new QueryBuilder().and(resourceQuery.get(), rightQuery.get());

		JsonObject keys = new JsonObject().put("folder", 1).put("name", 1).put("owner", 1);
		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						final String folder = event.body().getJsonObject("result", new JsonObject()).getString("folder");
						final String name = event.body().getJsonObject("result", new JsonObject()).getString("name");
						final String owner = event.body().getJsonObject("result", new JsonObject()).getString("owner");
						if ("ok".equals(event.body().getString("status")) &&
								folder != null && !folder.trim().isEmpty()) {
							QueryBuilder q = QueryBuilder.start("owner").is(owner).put("folder")
									.regex(Pattern.compile("^" + Pattern.quote(folder) + "($|_)"));
							MongoUpdateBuilder modifier = new MongoUpdateBuilder();
							modifier.rename("folder", "old-folder");
							mongo.update(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(q),
									modifier.build(), false, true, new Handler<Message<JsonObject>>() {
								@Override
								public void handle(Message<JsonObject> e) {
									if ("ok".equals(e.body().getString("status"))) {
										recursiveMove(folder, name, owner, "Trash", result);
									} else {
										result.handle(new Either.Left<String, JsonObject>("workspace.trash.error"));
									}
								}
							});
						} else {
							result.handle(new Either.Left<String, JsonObject>("workspace.folder.not.found"));
						}
					}
				});
	}

	@Override
	public void delete(String id, final UserInfos owner, final Handler<Either<String, JsonArray>> result) {
		if (owner == null) {
			result.handle(new Either.Left<String, JsonArray>("workspace.invalid.user"));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonArray>("workspace.folder.not.found"));
			return;
		}
		QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner").is(owner.getUserId());
		JsonObject keys = new JsonObject().put("folder", 1);
		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys,
				new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				String folder = event.body().getJsonObject("result", new JsonObject()).getString("folder");
				if ("ok".equals(event.body().getString("status")) && folder != null && !folder.trim().isEmpty()) {
					QueryBuilder q = QueryBuilder.start("owner").is(owner.getUserId()).put("folder")
							.regex(Pattern.compile("^" + Pattern.quote(folder) + "($|_)"));
					JsonObject keys = new JsonObject().put("metadata", 1)
							.put("owner", 1).put("name", 1).put("file", 1);
					final JsonObject query = MongoQueryBuilder.build(q);
					mongo.find(DOCUMENTS_COLLECTION, query, null, keys, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> message) {
							final JsonArray results = message.body().getJsonArray("results");
							if ("ok".equals(message.body().getString("status")) && results != null) {
								mongo.delete(DOCUMENTS_COLLECTION, query, new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> res) {
										if ("ok".equals(res.body().getString("status"))) {
											result.handle(new Either.Right<String, JsonArray>(results));
											final JsonArray filesIds = new JsonArray();
											for (Object o : results) {
												if (!(o instanceof JsonObject)) continue;
												String file = ((JsonObject) o).getString("file");
												if (file != null && !file.trim().isEmpty()) {
													filesIds.add(file);
												}
											}
											if (filesIds.size() > 0) {
												storage.removeFiles(filesIds,
														new Handler<JsonObject>() {
													@Override
													public void handle(JsonObject jsonObject) {
														if (!"ok".equals(jsonObject.getString("status"))) {
															log.error("Error deleting gridfs files : " +
																	filesIds.encode());
															log.error(jsonObject.getString("message"));
														}
													}
												});
											}
										} else {
											result.handle(new Either.Left<String, JsonArray>(
													res.body().getString("message")));
										}
									}
								});
							} else {
								result.handle(new Either.Left<String, JsonArray>(
										message.body().getString("message")));
							}
						}
					});
				} else {
					result.handle(new Either.Left<String, JsonArray>("workspace.folder.not.found"));
				}
			}
		});
	}

	@Override
	public void list(String path, UserInfos owner, boolean hierarchical, String filter,
			final Handler<Either<String, JsonArray>> results) {
		if (owner == null) {
			results.handle(new Either.Left<String, JsonArray>("workspace.invalid.user"));
			return;
		}

		QueryBuilder q = QueryBuilder.start();

		if("shared".equals(filter)){
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(owner.getUserId()).get());
			for (String gpId: owner.getGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId).get());
			}
			q.put("shared").elemMatch(new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get());
		} else {
			q.put("owner").is(owner.getUserId());
		}

		q.and("file").exists(false)
		 .and("application").is(WorkspaceService.WORKSPACE_NAME);

		if (path != null && !path.trim().isEmpty()) {
			if (hierarchical) {
				q = q.put("folder").regex(Pattern.compile("^" + Pattern.quote(path) + "_[^_]+$"));
			} else {
				q = q.put("folder").regex(Pattern.compile("^" + Pattern.quote(path) + "_"));
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
			result.handle(new Either.Left<String, JsonObject>("workspace.invalid.user"));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("workspace.folder.not.found"));
			return;
		}
		QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner").is(owner.getUserId());
		JsonObject keys = new JsonObject().put("folder", 1);
		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys,
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						final String folder = event.body().getJsonObject("result", new JsonObject()).getString("folder");
						if ("ok".equals(event.body().getString("status")) &&
								folder != null && !folder.trim().isEmpty()) {
							QueryBuilder q = QueryBuilder.start("owner").is(owner.getUserId()).put("folder")
									.regex(Pattern.compile("^" + Pattern.quote(folder) + "($|_)")).put("old-folder").exists(true);
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
							result.handle(new Either.Left<String, JsonObject>("workspace.folder.not.found"));
						}
					}
				});
	}

	@Override
	public void shareFolderAction(final String id, final UserInfos owner, final List<String> actions, final String groupId, final String userId, final ShareService shareService, final boolean remove, final Handler<Either<String, JsonObject>> result) {

		if (owner == null) {
			result.handle(new Either.Left<String, JsonObject>("workspace.invalid.user"));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("workspace.folder.not.found"));
			return;
		}

		String sharedMethod = "org-entcore-workspace-service-WorkspaceService|shareJsonSubmit";

		List<DBObject> groups = new ArrayList<>();
		groups.add(QueryBuilder.start("userId").is(owner.getUserId())
				.put(sharedMethod).is(true).get());
		for (String gpId: owner.getGroupsIds()) {
			groups.add(QueryBuilder.start("groupId").is(gpId)
					.put(sharedMethod).is(true).get());
		}

		final DBObject managerCheck = QueryBuilder.start("shared").elemMatch(
			new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()).get();

		QueryBuilder query = QueryBuilder.start("_id").is(id).put("file").exists(false).or(
			QueryBuilder.start("owner").is(owner.getUserId()).get(),
			managerCheck
		);

		JsonObject keys = new JsonObject().put("folder", 1).put("name", 1);

		Handler<Message<JsonObject>> folderHandler = new Handler<Message<JsonObject>>(){
			@Override
			public void handle(Message<JsonObject> event) {
				final String folder = event.body().getJsonObject("result", new JsonObject()).getString("folder");

				QueryBuilder q = QueryBuilder.start().or(
						QueryBuilder.start("owner").is(owner.getUserId()).get(),
						managerCheck
					).put("folder").regex(Pattern.compile("^" + Pattern.quote(folder) + "($|_)"))
					 .put("_id").notEquals(id);

				mongo.find(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(q), new Handler<Message<JsonObject>>() {

					public void handle(Message<JsonObject> d) {

						JsonArray files = d.body().getJsonArray("results", new JsonArray());

						if ("ok".equals(d.body().getString("status"))) {

							if(files.size() == 0){
								result.handle(new Either.Right<String, JsonObject>(new JsonObject("{ \"number\": 0, \"number-errors\": 0 }")));
								return;
							}

							final AtomicInteger remaining = new AtomicInteger(files.size());
							final AtomicInteger count = new AtomicInteger(0);
							final AtomicInteger errorcount = new AtomicInteger(0);

							Handler<Either<String, JsonObject>> recursiveHandler = new Handler<Either<String, JsonObject>>() {
								@Override
								public void handle(Either<String, JsonObject> event) {
									if(event.isRight())
										count.getAndAdd(1);
									else
										errorcount.getAndAdd(1);

									if (remaining.decrementAndGet() == 0) {
										JsonObject response = new JsonObject();
										response.put("number", count);
										response.put("number-errors", errorcount);
										result.handle(new Either.Right<String, JsonObject>(response));
									}
								}
							};

							for (Object o : files) {
								if (!(o instanceof JsonObject)) continue;
								JsonObject file = (JsonObject) o;

								if (groupId != null) {
									if(remove)
										shareService.removeGroupShare(groupId, file.getString("_id"), actions, recursiveHandler);
									else
										shareService.groupShare(owner.getUserId(), groupId, file.getString("_id"), actions, recursiveHandler);
								} else if (userId != null) {
									if(remove)
										shareService.removeUserShare(userId, file.getString("_id"), actions, recursiveHandler);
									else
										shareService.userShare(owner.getUserId(), userId, file.getString("_id"), actions, recursiveHandler);
								}
							}

						} else {
							result.handle(new Either.Left<String, JsonObject>("workspace.folder.not.found"));
						}
					}
				});

			}
		};

		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys, folderHandler);
	}

	public void rename(String id, final String newName, final UserInfos owner, final Handler<Either<String, JsonObject>> result){
		if (owner == null) {
			result.handle(new Either.Left<String, JsonObject>("workspace.invalid.user"));
			return;
		}
		if (id == null || id.trim().isEmpty()) {
			result.handle(new Either.Left<String, JsonObject>("workspace.folder.not.found"));
			return;
		}

		final QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner").is(owner.getUserId()).and("file").exists(false);
		final JsonObject keys = new JsonObject().put("folder", 1).put("name", 1);

		Handler<Message<JsonObject>> folderHandler = new Handler<Message<JsonObject>>(){
			@Override
			public void handle(Message<JsonObject> event) {
				final String folder = event.body().getJsonObject("result", new JsonObject()).getString("folder");
				final String newFolderPath = folder.lastIndexOf("_") < 0 ? newName : folder.substring(0, folder.lastIndexOf("_") + 1) + newName;

				//3 - Find & rename children
				final Handler<Message<JsonObject>> renameChildren = new Handler<Message<JsonObject>>() {
					public void handle(Message<JsonObject> event) {

						if (!"ok".equals(event.body().getString("status"))){
							result.handle(Utils.validResult(event)	);
							return;
						}

						QueryBuilder targetQuery = QueryBuilder.start("owner").is(owner.getUserId());
						targetQuery.or(
								QueryBuilder.start("folder").regex(Pattern.compile("^" + Pattern.quote(folder) + "($|_)")).get(),
								QueryBuilder.start("old-folder").regex(Pattern.compile("^" + Pattern.quote(folder) + "($|_)")).get()
								);

						mongo.find(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(targetQuery), new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> d) {
								JsonArray children = d.body().getJsonArray("results", new JsonArray());
								if ("ok".equals(d.body().getString("status")) && children.size() > 0) {
									final AtomicInteger remaining = new AtomicInteger(children.size());
									final AtomicInteger count = new AtomicInteger(0);

									Handler<Message<JsonObject>> recursiveHandler = new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> res) {
											count.getAndAdd(res.body().getInteger("number", 0));
											if (remaining.decrementAndGet() == 0) {
												res.body().put("number", count.get());
												result.handle(Utils.validResult(res));
											}
										}
									};

									for (Object o : children) {
										if (!(o instanceof JsonObject)) {
											remaining.decrementAndGet();
											continue;
										}
										JsonObject child = (JsonObject) o;

										String childFolder = child.getString("folder");
										String childOldFolder = child.getString("old-folder");
										String id = child.getString("_id");

										JsonObject updateMatcher = MongoQueryBuilder.build(QueryBuilder.start("_id").is(id));
										MongoUpdateBuilder updateModifier = new MongoUpdateBuilder();
										if(childOldFolder != null || childFolder.equals("Trash")){
											String newPath = childOldFolder.lastIndexOf("_") < 0 ? newName : childOldFolder.replaceFirst(Pattern.quote(folder), Matcher.quoteReplacement(newFolderPath));
											updateModifier.set("old-folder", newPath);
										} else {
											String newPath = childFolder.lastIndexOf("_") < 0 ? newName : childFolder.replaceFirst(Pattern.quote(folder), Matcher.quoteReplacement(newFolderPath));
											updateModifier.set("folder", newPath);
										}

										mongo.update(DOCUMENTS_COLLECTION, updateMatcher, updateModifier.build(), recursiveHandler);
									}

								} else {
									result.handle(Utils.validResult(d));
								}
							}
						});
					}
				};

				//2 - Rename target folder
				mongo.update(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), new JsonObject("{ \"$set\": { \"folder\": \""+newFolderPath+"\", \"name\": \""+newName+"\" }}"), renameChildren);
			}
		};

		//1 - Find the folder
		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys, folderHandler);
	}

}
