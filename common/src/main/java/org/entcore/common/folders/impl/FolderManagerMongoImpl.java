package org.entcore.common.folders.impl;

import static org.entcore.common.folders.impl.QueryHelper.isOk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.ElementShareOperations;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.impl.QueryHelper.DocumentQueryBuilder;
import org.entcore.common.folders.impl.QueryHelper.RestoreParentDirection;
import org.entcore.common.share.ShareService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.swift.storage.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.ETag;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FolderManagerMongoImpl implements FolderManager {
	private static final Logger log = LoggerFactory.getLogger(FolderManagerMongoImpl.class);

	private static void notModified(HttpServerRequest request, String fileId) {
		if (fileId != null && !fileId.trim().isEmpty()) {
			request.response().headers().add("ETag", fileId);
		}
		request.response().setStatusCode(304).setStatusMessage("Not Modified").end();
	}

	private static <T> AsyncResult<T> toError(String msg) {
		return new DefaultAsyncResult<>(new Exception(msg));
	}

	private static <T> AsyncResult<T> toError(Throwable msg) {
		return new DefaultAsyncResult<>((msg));
	}

	protected final Storage storage;

	protected final FileSystem fileSystem;

	protected final QueryHelper queryHelper;

	protected final ShareService shareService;
	protected final InheritShareComputer inheritShareComputer;

	public FolderManagerMongoImpl(String collection, Storage sto, FileSystem fs, ShareService shareService) {
		this.storage = sto;
		this.fileSystem = fs;
		this.shareService = shareService;
		this.queryHelper = new QueryHelper(collection);
		this.inheritShareComputer = new InheritShareComputer(queryHelper);
	}

	@Override
	public void addFile(Optional<String> parentId, JsonObject doc, String ower, String ownerName,
			Handler<AsyncResult<JsonObject>> handler) {
		this.inheritShareComputer.computeFromParentId(doc, false, parentId).compose(parent -> {
			String now = MongoDb.formatDate(new Date());
			if (parentId.isPresent()) {
				doc.put("eParent", parentId.get());
			}
			doc.put("eType", FILE_TYPE);
			doc.put("created", now);
			doc.put("modified", now);
			doc.put("owner", ower);
			doc.put("ownerName", ownerName);
			//
			return queryHelper.insert(doc);
		}).setHandler(handler);
	}

	@Override
	public void copy(String sourceId, Optional<String> destFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		queryHelper.findAllAsList(
				queryHelper.queryBuilder().filterByInheritShareAndOwner(user).withExcludeDeleted().withId(sourceId))
				.compose(docs -> {
					return copyRecursivelyFromParentId(Optional.of(user), docs, destFolderId);
				}).setHandler(handler);
	}

	@Override
	public void copyAll(Collection<String> sourceIds, Optional<String> destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		if (sourceIds.isEmpty()) {
			handler.handle(new DefaultAsyncResult<JsonArray>(new JsonArray()));
			return;
		}
		queryHelper.findAllAsList(
				queryHelper.queryBuilder().filterByInheritShareAndOwner(user).withExcludeDeleted().withId(sourceIds))
				.compose(docs -> {
					return copyRecursivelyFromParentId(Optional.of(user), docs, destinationFolderId);
				}).setHandler(handler);
	}

	private Future<JsonArray> copyFile(Optional<UserInfos> userOpt, Collection<JsonObject> originals,
			Optional<JsonObject> parent) {
		return StorageHelper.copyFileInStorage(storage, originals).compose(oldFileIdForNewFileId -> {
			// set newFileIds and parent
			List<JsonObject> copies = originals.stream().map(o -> {
				JsonObject copy = o.copy();
				copy.put("copyFromId", o.getString("_id"));
				copy.remove("_id");
				// parent
				if (parent.isPresent()) {
					copy.put("eParent", DocumentHelper.getId(parent.get()));
				} else {
					copy.remove("eParent");
				}
				copy.remove("eParentOld");
				copy.remove("protected");
				copy.remove("public");
				//
				if (userOpt.isPresent()) {
					UserInfos user = userOpt.get();
					copy.put("owner", user.getUserId());
					copy.put("ownerName", user.getUsername());
				}
				// dates
				String now = MongoDb.formatDate(new Date());
				copy.put("created", now);
				copy.put("modified", now);
				// remove shares and favorites
				copy.put("favorites", new JsonArray());
				copy.put("shared", new JsonArray());
				// merge shared after reset shared
				InheritShareComputer.mergeShared(parent, copy);
				// copy file from storage
				StorageHelper.replaceAll(copy, oldFileIdForNewFileId);

				return copy;
			}).collect(Collectors.toList());
			// save copies in database using bulk (new fileid)
			return queryHelper.insertAll(copies)
					.map(c -> c.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
		});
	}

	private Future<JsonArray> copyRecursivelyFromParentId(Optional<UserInfos> userOpt, Collection<JsonObject> docs,
			Optional<String> newParentIdOpt) {
		if (newParentIdOpt.isPresent()) {
			return queryHelper.findById(newParentIdOpt.get())
					.compose(newParent -> copyRecursivelyFromParent(userOpt, docs, Optional.ofNullable(newParent)));
		} else {
			return copyRecursivelyFromParent(userOpt, docs, Optional.empty());
		}
	}

	private Future<JsonArray> copyRecursivelyFromParent(Optional<UserInfos> userOpt, Collection<JsonObject> docs,
			Optional<JsonObject> newParentOpt) {
		JsonArray allCopies = new JsonArray();
		return this.copyFile(userOpt, docs, newParentOpt).compose(copies -> {
			allCopies.addAll(copies);
			List<String> folderIds = docs.stream().filter(doc -> DocumentHelper.isFolder(doc))
					.map(doc -> DocumentHelper.getId(doc)).collect(Collectors.toList());
			if (folderIds.isEmpty()) {
				return Future.succeededFuture(new ArrayList<>());
			}
			return queryHelper.findAllAsList(queryHelper.queryBuilder().withParent(folderIds).withExcludeDeleted());
		}).compose(children -> {
			// sort children by old parent (in order to set the new one)
			Map<String, Collection<JsonObject>> childrenByParents = new HashMap<>();
			for (JsonObject json : children) {
				String parent = DocumentHelper.getParent(json);
				childrenByParents.putIfAbsent(parent, new ArrayList<>());
				childrenByParents.get(parent).add(json);
			}
			// for each oldparent, get child his children and set new parent
			@SuppressWarnings("rawtypes")
			List<Future> futures = new ArrayList<>();
			for (String oldParent : childrenByParents.keySet()) {
				// find the new parent (copy of the old)
				Optional<JsonObject> nextParentRecursion = allCopies.stream().map(o -> (JsonObject) o)
						.filter(o -> o.getString("copyFromId", "").equals(oldParent)).findFirst();
				futures.add(copyRecursivelyFromParent(userOpt, childrenByParents.get(oldParent), nextParentRecursion));
			}
			return CompositeFuture.all(futures);
		}).map(results -> {
			List<JsonArray> copies = results.list();
			for (JsonArray a : copies) {
				allCopies.addAll(a);
			}
			return allCopies;
		});
	}

	@Override
	public void copyUnsafe(String sourceId, Optional<String> destFolderId, String userId,
			Handler<AsyncResult<JsonArray>> handler) {
		queryHelper.findAllAsList(queryHelper.queryBuilder().withId(sourceId).withExcludeDeleted()).compose(docs -> {
			return copyRecursivelyFromParentId(Optional.empty(), docs, destFolderId);
		}).setHandler(handler);
	}

	@Override
	public void countByQuery(ElementQuery query, UserInfos user, Handler<AsyncResult<Integer>> handler) {
		// TODO hierarchical count?
		queryHelper.countAll(DocumentQueryBuilder.fromElementQuery(query, Optional.ofNullable(user)))
				.setHandler(handler);
	}

	@Override
	public void createFolder(JsonObject folder, UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
		this.inheritShareComputer.compute(folder, false).compose(res -> {
			folder.put("eType", FOLDER_TYPE);
			String now = MongoDb.formatDate(new Date());
			folder.put("created", now);
			folder.put("modified", now);
			folder.put("owner", user.getUserId());
			folder.put("ownerName", user.getUsername());
			return queryHelper.insert(folder);
		}).setHandler(handler);
	}

	@Override
	public void createFolder(String destinationFolderId, UserInfos user, JsonObject folder,
			Handler<AsyncResult<JsonObject>> handler) {
		folder.put("eParent", destinationFolderId);
		this.createFolder(folder, user, handler);
	}

	@Override
	public void delete(String id, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		List<String> idFolders = new ArrayList<>();
		idFolders.add(id);
		deleteFolderRecursively(idFolders, Optional.ofNullable(user))
				.map(v -> v.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll)).setHandler(handler);
	}

	@Override
	public void deleteAll(Set<String> ids, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		deleteFolderRecursively(ids, Optional.ofNullable(user))
				.map(v -> v.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll)).setHandler(handler);
	}

	@Override
	public void deleteByQuery(ElementQuery query, Optional<UserInfos> user, Handler<AsyncResult<JsonArray>> handler) {
		// first delete folders matching recursively
		DocumentQueryBuilder builderFolder = DocumentQueryBuilder.fromElementQuery(query, user)
				.withFileType(FOLDER_TYPE);
		if (user.isPresent()) {
			builderFolder.filterByInheritShareAndOwner(user.get());
		}
		Future<JsonArray> futureDelFolder = queryHelper.findAllAsList(builderFolder).compose(res -> {
			List<String> ids = res.stream().map(o -> DocumentHelper.getId(o)).collect(Collectors.toList());
			return deleteFolderRecursively(ids, user);
		}).map(v -> v.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
		// delete files
		futureDelFolder.compose(deleted -> {
			DocumentQueryBuilder builder = DocumentQueryBuilder.fromElementQuery(query, user).withFileType(FILE_TYPE);
			if (user.isPresent()) {
				builder.filterByInheritShareAndOwner(user.get());
			}
			return queryHelper.findAllAsList(builder);
		}).compose(res -> {
			return deleteFiles(res);
		}).map(v -> {
			// merge deleted files with deleted folder recursive
			JsonArray all = v.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
			all.addAll(futureDelFolder.result());
			return all;
		}).setHandler(handler);
	}

	private Future<List<JsonObject>> deleteFiles(List<JsonObject> files) {
		if (files.isEmpty()) {
			return Future.succeededFuture(files);
		}
		// Set to avoid duplicate
		Set<String> ids = files.stream().map(o -> o.getString("_id")).collect(Collectors.toSet());
		return queryHelper.deleteByIds((ids)).compose(res -> {
			Future<List<JsonObject>> future = Future.future();
			List<String> listOfFilesIds = StorageHelper.getListOfFileIds(files);
			this.storage.removeFiles(new JsonArray(listOfFilesIds), resDelete -> {
				if (isOk(resDelete)) {
					future.complete(files);
				} else {
					// dont throw error
					// future.fail(toErrorStr(resDelete));
					JsonArray errors = resDelete.getJsonArray("errors", new JsonArray());
					for (Object o : errors) {
						if (o instanceof JsonObject) {
							String docId = ((JsonObject) o).getString("id");
							String message = ((JsonObject) o).getString("message");
							log.error("Failed to remove file with id: " + docId + "/" + message);
						}
					}
					// delete document even if file does not exists
					future.complete(files);
				}
			});
			return future;
		});
	}

	private Future<List<JsonObject>> deleteFolderRecursively(Collection<String> foldersIds, Optional<UserInfos> user) {
		if (foldersIds.isEmpty()) {
			return Future.succeededFuture(new ArrayList<>());
		}
		DocumentQueryBuilder builder = queryHelper.queryBuilder().withId(foldersIds);
		if (user.isPresent()) {
			builder.filterByInheritShareAndOwner(user.get());
		}
		return queryHelper.getChildrenRecursively(builder, Optional.empty(), true).compose(rows -> {
			if (rows.isEmpty()) {
				return Future.succeededFuture();
			}
			List<JsonObject> files = rows.stream().map(o -> (JsonObject) o).filter(o -> DocumentHelper.isFile(o))
					.collect(Collectors.toList());
			List<JsonObject> folders = rows.stream().map(o -> (JsonObject) o).filter(o -> DocumentHelper.isFolder(o))
					.collect(Collectors.toList());

			return CompositeFuture.all(deleteFolders(folders), deleteFiles(files));
		}).map(result -> {
			List<JsonObject> array = new ArrayList<>();
			for (int i = 0; i < result.result().size(); i++) {
				array.addAll(result.result().resultAt(i));
			}
			return array;
		});
	}

	private Future<List<JsonObject>> deleteFolders(List<JsonObject> files) {
		if (files.isEmpty()) {
			return Future.succeededFuture(files);
		}
		// Set to avoid duplicate
		Set<String> ids = files.stream().map(o -> o.getString("_id")).collect(Collectors.toSet());
		return queryHelper.deleteByIds((ids)).map(files);
	}

	@Override
	public void downloadFile(String id, UserInfos user, HttpServerRequest request) {
		this.info(id, user, msg -> {
			if (msg.succeeded()) {
				JsonObject bodyRoot = msg.result();
				switch (DocumentHelper.getType(bodyRoot)) {
				case FOLDER_TYPE:
					String idFolder = bodyRoot.getString("_id");
					DocumentQueryBuilder parentFilter = queryHelper.queryBuilder().filterByInheritShareAndOwner(user)
							.withExcludeDeleted().withId(idFolder);
					DocumentQueryBuilder childFilter = queryHelper.queryBuilder().withExcludeDeleted();
					Future<List<JsonObject>> future = queryHelper.getChildrenRecursively(parentFilter,
							Optional.ofNullable(childFilter), true);

					future.setHandler(result -> {
						if (result.succeeded()) {
							List<JsonObject> rows = result.result();
							FolderExporterZip zipBuilder = new FolderExporterZip(storage, fileSystem);
							zipBuilder.exportAndSendZip(bodyRoot, rows, request).setHandler(zipEvent -> {
								if (zipEvent.failed()) {
									request.response().setStatusCode(500).end();
								}
							});
						} else {
							request.response().setStatusCode(404).end();
						}
					});
					return;
				case FILE_TYPE:
					downloadOneFile(bodyRoot, request);
					return;
				default:
					request.response().setStatusCode(400)
							.setStatusMessage("Could not determine the type (file or folder) for id:" + id).end();
					return;
				}
			} else {
				request.response().setStatusCode(404).end();
			}
		});
	}

	@Override
	public void downloadFiles(Collection<String> ids, UserInfos user, HttpServerRequest request) {
		DocumentQueryBuilder parentFilter = queryHelper.queryBuilder().filterByInheritShareAndOwner(user)
				.withExcludeDeleted().withIds(ids);
		DocumentQueryBuilder childFilter = queryHelper.queryBuilder().withExcludeDeleted();
		queryHelper.getChildrenRecursively(parentFilter, Optional.ofNullable(childFilter), true).setHandler(msg -> {
			if (msg.succeeded() && msg.result().size() > 0) {
				// download ONE file
				List<JsonObject> all = msg.result();
				if (all.size() == 1 //
						&& DocumentHelper.isFile(all.get(0))) {
					downloadOneFile(all.get(0), request);
					return;
				}
				// download multiple files
				FolderExporterZip zipBuilder = new FolderExporterZip(storage, fileSystem);
				zipBuilder.exportAndSendZip(all, request).setHandler(zipEvent -> {
					if (zipEvent.failed()) {
						request.response().setStatusCode(500).end();
					}
				});

			} else {
				request.response().setStatusCode(404).end();
			}

		});
	}

	private void downloadOneFile(JsonObject bodyRoot, HttpServerRequest request) {
		String name = bodyRoot.getString("name");
		String file = bodyRoot.getString("file");
		JsonObject metadata = bodyRoot.getJsonObject("metadata");
		boolean inline = inlineDocumentResponse(bodyRoot, request.params().get("application"));
		if (inline && ETag.check(request, file)) {
			notModified(request, file);
		} else {
			storage.sendFile(file, name, request, inline, metadata);
		}
		return;
	}

	@Override
	public void findByQuery(ElementQuery query, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		DocumentQueryBuilder builder = DocumentQueryBuilder.fromElementQuery(query, Optional.ofNullable(user));
		// query
		if (query.getHierarchical() != null && query.getHierarchical()) {
			queryHelper.listWithParents(builder).setHandler(handler);
		} else {
			queryHelper.findAll(builder).setHandler(handler);
		}
	}

	@Override
	public void info(String id, UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
		// find only non deleted file/folder that i can see
		Future<JsonObject> future = queryHelper
				.findOne(queryHelper.queryBuilder().withId(id).filterByInheritShareAndOwner(user).withExcludeDeleted());
		future.setHandler(handler);
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

	@Override
	public void list(String idFolder, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		Future<JsonArray> future = queryHelper.findAll(queryHelper.queryBuilder().filterByInheritShareAndOwner(user)
				.withParent(idFolder).withExcludeDeleted());
		future.setHandler(handler);
	}

	@Override
	public void listFoldersRecursively(UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		Future<JsonArray> future = queryHelper.listWithParents(//
				queryHelper.queryBuilder().filterBySharedAndOwner(user).withExcludeDeleted());
		future.setHandler(handler);
	}

	@Override
	public void listFoldersRecursivelyFromFolder(String idFolder, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		Future<JsonArray> future = queryHelper.getChildrenRecursively(
				queryHelper.queryBuilder().filterByInheritShareAndOwner(user).withExcludeDeleted().withId(idFolder), //
				Optional.ofNullable(queryHelper.queryBuilder().withExcludeDeleted().withId(idFolder)), //
				true).map(f -> new JsonArray(f));
		future.setHandler(handler);
	}

	@Override
	public void markFavorites(Collection<String> ids, Boolean markOrUnmark, UserInfos user,
			Handler<AsyncResult<Collection<JsonObject>>> h) {
		queryHelper.findAllAsList(queryHelper.queryBuilder().filterByInheritShareAndOwner(user).withIds(ids))
				.compose(all -> {
					//
					List<JsonObject> updated = all.stream().filter(doc -> {
						JsonArray favorites = DocumentHelper.getOrCreateFavorties(doc);
						if (markOrUnmark) {
							// add to favorites
							Optional<JsonObject> first = favorites.stream().map(o -> (JsonObject) o).filter(o -> {
								String userId = o.getString("userId");
								return userId != null && user.getUserId().equals(userId);
							}).findFirst();
							if (!first.isPresent()) {
								JsonObject entry = new JsonObject();
								entry.put("userId", user.getUserId());
								favorites.add(entry);
								return true;
							}
						} else {
							// remove from favorites
							List<JsonObject> founded = favorites.stream().map(o -> (JsonObject) o).filter(o -> {
								String userId = o.getString("userId");
								return userId != null && user.getUserId().equals(userId);
							}).collect(Collectors.toList());
							if (founded.size() > 0) {
								for (JsonObject o : founded) {
									favorites.remove(o);
								}
								return true;
							}
						}
						return false;
					}).collect(Collectors.toList());
					return queryHelper.bulkUpdateFavorites(updated).map((Collection<JsonObject>) all);
				}).setHandler(h);
	}

	@Override
	public void move(String sourceId, String destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonObject>> handler) {
		this.info(sourceId, user, msg -> {
			if (msg.succeeded()) {
				JsonObject previous = msg.result();
				if (previous != null) {
					queryHelper.updateMove(sourceId, destinationFolderId).compose(s -> {
						DocumentHelper.setParent(previous, destinationFolderId);
						return this.inheritShareComputer.compute(previous, true)
								.compose(res -> queryHelper.bulkUpdateShares(res));
					}).map(previous).setHandler(handler);
				} else {
					handler.handle(toError("not.found"));
				}
			} else {
				handler.handle(toError(msg.cause()));
			}
		});

	}

	@Override
	public void moveAll(Collection<String> sourceIds, String destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		@SuppressWarnings("rawtypes")
		List<Future> futures = sourceIds.stream().map(sourceId -> {
			Future<JsonObject> future = Future.future();
			this.move(sourceId, destinationFolderId, user, future.completer());
			return future;
		}).collect(Collectors.toList());
		CompositeFuture.all(futures).map(results -> {
			return results.list().stream().map(o -> (JsonObject) o).collect(JsonArray::new, JsonArray::add,
					JsonArray::addAll);
		}).setHandler(handler);
	}

	@Override
	public void rename(String id, String newName, UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
		this.info(id, user, msg -> {
			if (msg.succeeded()) {
				JsonObject doc = msg.result();
				doc.put("name", newName);// need for result
				MongoUpdateBuilder set = new MongoUpdateBuilder().set("name", newName);
				queryHelper.update(id, set).map(doc).setHandler(handler);
			} else {
				handler.handle(toError(msg.cause()));
			}
		});
	}

	@Override
	public void restore(String id, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		Set<String> roots = new HashSet<>();
		roots.add(id);
		setDeleteFlag(roots, user, handler, false);
	}

	private void setDeleteFlag(Set<String> roots, UserInfos user, Handler<AsyncResult<JsonArray>> handler,
			boolean deleted) {
		if (roots.isEmpty()) {
			handler.handle(new DefaultAsyncResult<JsonArray>(new JsonArray()));
			return;
		}
		DocumentQueryBuilder parentFilter = queryHelper.queryBuilder().filterByInheritShareAndOwner(user).withId(roots);
		queryHelper.getChildrenRecursively(parentFilter, Optional.empty(), true).compose(rows -> {
			if (rows.isEmpty()) {
				return Future.succeededFuture();
			}
			List<JsonObject> all = rows.stream().map(o -> (JsonObject) o).collect(Collectors.toList());
			return setDeleteFlag(user, roots, all, deleted);
		}).setHandler(handler);
	}

	private Future<JsonArray> setDeleteFlag(UserInfos user, Set<String> roots, List<JsonObject> files,
			boolean deleted) {
		if (files.isEmpty()) {
			return Future.succeededFuture(new JsonArray());
		}
		// Set to avoid duplicate
		Set<String> ids = files.stream().map(o -> o.getString("_id")).collect(Collectors.toSet());
		MongoUpdateBuilder mongo = new MongoUpdateBuilder().set("deleted", deleted);
		if (deleted) {
			mongo.set("trasher", user.getUserId());
		} else {
			mongo.unset("trasher");
		}
		return queryHelper.updateAll(ids, mongo).compose(e -> {
			if (deleted) {
				return queryHelper.breakParentLink(roots);
			} else {
				return queryHelper.restoreParentLink(RestoreParentDirection.FromTrash, files);
			}
		}).map(new JsonArray(new ArrayList<>(ids)));
	}

	public void share(String id, ElementShareOperations shareOperations, Handler<AsyncResult<JsonObject>> hh) {
		UserInfos user = shareOperations.getUser();
		// is owner or has shared right
		queryHelper.findOne(queryHelper.queryBuilder().withId(id).filterByInheritShareAndOwnerWithAction(user,
				shareOperations.getShareAction())).compose(founded -> {
					Future<JsonObject> futureShared = Future.future();
					// compute shared after sharing
					final Handler<Either<String, JsonObject>> handler = (event) -> {
						if (event.isRight()) {
							futureShared.complete(event.right().getValue());
						} else {
							futureShared.fail(event.left().getValue());
						}
					};
					//
					switch (shareOperations.getKind()) {
					case GROUP_SHARE:
						this.shareService.groupShare(user.getUserId(), shareOperations.getGroupId(), id,
								shareOperations.getActions(), handler);
						break;
					case GROUP_SHARE_REMOVE:
						this.shareService.removeGroupShare(shareOperations.getGroupId(), id,
								shareOperations.getActions(), handler);
						break;
					case USER_SHARE:
						this.shareService.userShare(user.getUserId(), shareOperations.getUserId(), id,
								shareOperations.getActions(), handler);
						break;
					case USER_SHARE_REMOVE:
						this.shareService.removeUserShare(shareOperations.getUserId(), id, shareOperations.getActions(),
								handler);
						break;
					case SHARE_OBJECT:
						this.shareService.share(user.getUserId(), id, shareOperations.getShare(), handler);
						break;
					}
					return futureShared;
				}).compose(ev -> {
					// recompute from id (to refresh shared array)
					return this.inheritShareComputer.compute(id, true).compose(res -> queryHelper.bulkUpdateShares(res))
							.map(ev);
				}).setHandler(hh);
	}

	@Override
	public void shareAll(Collection<String> ids, ElementShareOperations shareOperations,
			Handler<AsyncResult<Collection<JsonObject>>> h) {
		@SuppressWarnings("rawtypes")
		List<Future> futures = ids.stream().map(id -> {
			Future<JsonObject> future = Future.future();
			share(id, shareOperations, future.completer());
			return future;
		}).collect(Collectors.toList());
		CompositeFuture.all(futures).map(res -> {
			Collection<JsonObject> temp = res.list();
			return temp;
		}).setHandler(h);
	}

	@Override
	public void trash(String id, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		Set<String> roots = new HashSet<>();
		roots.add(id);
		setDeleteFlag(roots, user, handler, true);
	}

	@Override
	public void restoreAll(Set<String> ids, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		setDeleteFlag(ids, user, handler, false);
	}

	@Override
	public void trashAll(Set<String> ids, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		setDeleteFlag(ids, user, handler, true);
	}

	@Override
	public void updateFile(String id, Optional<String> parentId, JsonObject doc,
			Handler<AsyncResult<JsonObject>> handler) {
		this.inheritShareComputer.computeFromParentId(doc, false, parentId).compose(parent -> {
			String now = MongoDb.formatDate(new Date());
			if (parentId.isPresent()) {
				doc.put("eParent", parentId.get());
			}
			doc.put("modified", now);
			//
			return queryHelper.update(id, doc).map(doc);
		}).setHandler(handler);
	}

	@Override
	public void updateShared(String id, UserInfos user, Handler<AsyncResult<Void>> handler) {
		this.info(id, user, msg -> {
			if (msg.succeeded()) {
				Future<Void> future = this.inheritShareComputer.compute(msg.result(), true)
						.compose(res -> queryHelper.bulkUpdateShares(res)).map(e -> null);
				future.setHandler(handler);
			} else {
				handler.handle(toError(msg.cause()));
			}
		});

	}

}
