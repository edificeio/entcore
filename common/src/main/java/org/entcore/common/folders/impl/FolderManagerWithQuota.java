package org.entcore.common.folders.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.ElementShareOperations;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.QuotaService;
import org.entcore.common.folders.impl.QueryHelper.DocumentQueryBuilder;
import org.entcore.common.storage.FileStats;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class FolderManagerWithQuota implements FolderManager {
	protected final QuotaService quotaService;
	protected final FolderManager folderManager;
	protected final EventBus eventBus;
	protected final QueryHelper queryHelper;
	protected final int quotaThreshold;

	public FolderManagerWithQuota(String collection, int quotaTreshold, QuotaService quotaService,
			FolderManager folderManager, EventBus bus) {
		super();
		this.eventBus = bus;
		this.quotaThreshold = quotaTreshold;
		this.quotaService = quotaService;
		this.folderManager = folderManager;
		this.queryHelper = new QueryHelper(collection);
	}

	@Override
	public void countByQuery(ElementQuery query, UserInfos user, Handler<AsyncResult<Integer>> handler) {
		this.folderManager.countByQuery(query, user, handler);
	}

	@Override
	public void addFile(Optional<String> parentId, JsonObject doc, String ownerId, String ownerName,
			Handler<AsyncResult<JsonObject>> handler) {
		this.folderManager.addFile(parentId, doc, ownerId, ownerName, handler);
	}

	public Future<Long> computFreeSpace(final UserInfos userInfos) {
		Future<Long> future = Future.future();
//		try {
//			long quota = Long.valueOf(userInfos.getAttribute("quota").toString());
//			long storage = Long.valueOf(userInfos.getAttribute("storage").toString());
//			future.complete(quota - storage);
//		} catch (Exception e) {
		quotaService.quotaAndUsage(userInfos.getUserId(), new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					JsonObject j = r.right().getValue();
					if (j != null) {
						long quota = j.getLong("quota", 0l);
						long storage = j.getLong("storage", 0l);
						for (String attr : j.fieldNames()) {
							UserUtils.addSessionAttribute(eventBus, userInfos.getUserId(), attr, j.getLong(attr), null);
						}
						future.complete(quota - storage);
					} else {
						future.fail("not.found");
					}
				} else {
					future.fail(r.left().getValue());
				}
			}
		});
		// }
		return future;
	}

	private Future<Long> computFreeSpace(final String userId) {
		Future<Long> future = Future.future();
		quotaService.quotaAndUsage(userId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					JsonObject j = r.right().getValue();
					if (j != null) {
						long quota = j.getLong("quota", 0l);
						long storage = j.getLong("storage", 0l);
						for (String attr : j.fieldNames()) {
							UserUtils.addSessionAttribute(eventBus, userId, attr, j.getLong(attr), null);
						}
						future.complete(quota - storage);
					} else {
						future.fail("not.found");
					}
				} else {
					future.fail(r.left().getValue());
				}
			}
		});
		return future;
	}

	private Future<Void> canCopy(Collection<String> sourceId, Either<String, UserInfos> user,
			Future<List<JsonObject>> filesAndFolders) {
		Future<Long> futureFreeSpace = user.isLeft() ? computFreeSpace(user.left().getValue())
				: computFreeSpace(user.right().getValue());
		return CompositeFuture.all(filesAndFolders, futureFreeSpace).compose(results -> {
			@SuppressWarnings("unchecked")
			final List<JsonObject> filesOrFolders = (List<JsonObject>) results.resultAt(0);
			Set<String> ids = filesOrFolders.stream().map(file -> DocumentHelper.getId(file))
					.collect(Collectors.toSet());
			DocumentQueryBuilder parentFilter = queryHelper.queryBuilder().withId(ids);
			DocumentQueryBuilder childrenFilter = queryHelper.queryBuilder().withFileType(FILE_TYPE)
					.withExcludeDeleted();
			return queryHelper.getChildrenRecursively(parentFilter, Optional.ofNullable(childrenFilter), true)
					.map(founded -> {
						return DocumentHelper.getFileSize(founded);
					});
		}).compose(size -> {
			Long freeSpace = futureFreeSpace.result();
			if (freeSpace >= size) {
				return Future.succeededFuture(null);
			} else {
				return Future.failedFuture("files.too.large");
			}
		});
	}

	@Override
	public void copy(String sourceId, Optional<String> destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		// fetch only if i have right on it (inherit share and owner)
		Future<List<JsonObject>> filesAndFolders = queryHelper
				.findOne(queryHelper.queryBuilder().filterByInheritShareAndOwner(user).withId(sourceId)).map(e -> {
					List<JsonObject> list = new ArrayList<>();
					list.add(e);
					return list;
				});
		List<String> ids = new ArrayList<>();
		ids.add(sourceId);
		this.canCopy(ids, new Either.Right<String, UserInfos>(user), filesAndFolders).compose(e -> {
			Future<JsonArray> innerFuture = Future.future();
			this.folderManager.copy(sourceId, destinationFolderId, user, innerFuture.completer());
			return innerFuture;
		}).compose(copies -> {
			// update quota before return
			final long size = DocumentHelper.getFileSize(copies);
			return decrementFreeSpace(user.getUserId(), size).map(copies);
		}).setHandler(handler);
	}

	@Override
	public void copyAll(Collection<String> sourceIds, Optional<String> destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		// fetch only if i have right on it (inherit share and owner)
		Future<List<JsonObject>> filesAndFolders = queryHelper
				.findAllAsList(queryHelper.queryBuilder().filterByInheritShareAndOwner(user).withId(sourceIds));
		this.canCopy(sourceIds, new Either.Right<String, UserInfos>(user), filesAndFolders).compose(e -> {
			Future<JsonArray> innerFuture = Future.future();
			this.folderManager.copyAll(sourceIds, destinationFolderId, user, innerFuture.completer());
			return innerFuture;
		}).compose(copies -> {
			// update quota before return
			final long size = DocumentHelper.getFileSize(copies);
			return decrementFreeSpace(user.getUserId(), size).map(copies);
		}).setHandler(handler);
	}

	@Override
	public void copyUnsafe(String sourceId, Optional<String> destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		// fetch only if i have right on it (inherit share and owner)
		Future<List<JsonObject>> filesAndFolders = queryHelper.findOne(queryHelper.queryBuilder().withId(sourceId))
				.map(e -> {
					List<JsonObject> list = new ArrayList<>();
					list.add(e);
					return list;
				});
		List<String> ids = new ArrayList<>();
		ids.add(sourceId);
		this.canCopy(ids, new Either.Right<String, UserInfos>(user), filesAndFolders).compose(e -> {
			Future<JsonArray> innerFuture = Future.future();
			this.folderManager.copyUnsafe(sourceId, destinationFolderId, user, innerFuture.completer());
			return innerFuture;
		}).compose(copies -> {
			// update quota before return
			final long size = DocumentHelper.getFileSize(copies);
			return decrementFreeSpace(user.getUserId(), size).map(copies);
		}).setHandler(handler);
	}

	@Override
	public void createExternalFolder(JsonObject folder, UserInfos user, String externalId, Handler<AsyncResult<JsonObject>> handler) {
		// dont need to check quota
		this.folderManager.createExternalFolder(folder, user, externalId, handler);
	}

	@Override
	public void createFolder(JsonObject folder, UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
		// dont need to check quota
		this.folderManager.createFolder(folder, user, handler);
	}

	@Override
	public void createFolder(String destinationFolderId, UserInfos user, JsonObject folder,
			Handler<AsyncResult<JsonObject>> handler) {
		// dont need to check quota
		this.folderManager.createFolder(destinationFolderId, user, folder, handler);
	}

	private Future<Void> decrementFreeSpace(String userId, final long amount) {
		if (amount == 0) {
			return Future.succeededFuture();
		}
		Future<Void> future = Future.future();
		quotaService.incrementStorage(userId, amount, this.quotaThreshold, ev -> {
			if (ev.isRight()) {
				future.complete(null);
			} else {
				future.fail(ev.left().getValue());
			}
		});
		return future;
	}

	private Future<JsonArray> updateQuotaForDelete(Optional<String> currentUserId, JsonArray deleted) {
		Map<String, Long> sizeByOwner = new HashMap<>();
		for (Object d : deleted) {
			if (d instanceof JsonObject) {
				JsonObject dJson = (JsonObject) d;
				String owner = DocumentHelper.getOwner(dJson);
				long size = DocumentHelper.getFileSize(dJson);
				size += sizeByOwner.getOrDefault(owner, 0l);
				sizeByOwner.put(owner, size);
			}
		}
		//
		@SuppressWarnings("rawtypes")
		List<Future> futures = sizeByOwner.entrySet().stream()
				.map(entry -> incrementFreeSpace(entry.getKey(), entry.getValue())).collect(Collectors.toList());
		return CompositeFuture.all(futures).map(e -> deleted);
	}

	@Override
	public void delete(String id, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		Future<JsonArray> future = Future.future();
		this.folderManager.delete(id, user, future.completer());
		future.compose(deleted -> { 
			return updateQuotaForDelete(Optional.ofNullable(user.getUserId()), deleted);
		}).setHandler(handler);
	}

	@Override
	public void deleteAll(Set<String> ids, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		Future<JsonArray> future = Future.future();
		this.folderManager.deleteAll(ids, user, future.completer());
		future.compose(deleted -> {
			return updateQuotaForDelete(Optional.ofNullable(user.getUserId()), deleted);
		}).setHandler(handler);
	}

	@Override
	public void deleteByQuery(ElementQuery query, Optional<UserInfos> user, Handler<AsyncResult<JsonArray>> handler) {
		Future<JsonArray> future = Future.future();
		this.folderManager.deleteByQuery(query, user, future.completer());
		future.compose(deleted -> {
			return updateQuotaForDelete(user.map(u->u.getUserId()), deleted);
		}).setHandler(handler);
	}

	@Override
	public void downloadFile(String id, UserInfos user, HttpServerRequest request) {
		// dont need to check
		this.folderManager.downloadFile(id, user, request);
	}

	@Override
	public void downloadFiles(Collection<String> ids, UserInfos user, boolean includeDeleted, HttpServerRequest request) {
		// dont need to check
		this.folderManager.downloadFiles(ids, user, includeDeleted, request);
	}

	@Override
	public void findByQuery(ElementQuery query, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		this.folderManager.findByQuery(query, user, handler);
	}

	private Future<Void> incrementFreeSpace(String userId, final long amount) {
		if (amount == 0) {
			return Future.succeededFuture();
		}
		Future<Void> future = Future.future();
		quotaService.decrementStorage(userId, amount, this.quotaThreshold, ev -> {
			if (ev.isRight()) {
				JsonObject j = ev.right().getValue();
				UserUtils.addSessionAttribute(eventBus, userId, "storage", j.getLong("storage"), null);
				if (j.getBoolean("notify", false)) {
					quotaService.notifySmallAmountOfFreeSpace(userId);
				}
				future.complete(null);
			} else {
				future.fail(ev.left().getValue());
			}
		});
		return future;
	}

	@Override
	public void info(String id, UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
		// dont need to check
		this.folderManager.info(id, user, handler);
	}

	@Override
	public void list(String idFolder, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		// dont need to check
		this.folderManager.list(idFolder, user, handler);
	}

	@Override
	public void listFoldersRecursively(UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		// dont need to check
		this.folderManager.listFoldersRecursively(user, handler);
	}

	@Override
	public void listFoldersRecursivelyFromFolder(String idFolder, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		// dont need to check
		this.folderManager.listFoldersRecursivelyFromFolder(idFolder, user, handler);
	}

	@Override
	public void markFavorites(Collection<String> ids, Boolean markOrUnmark, UserInfos user,
			Handler<AsyncResult<Collection<JsonObject>>> h) {
		this.folderManager.markFavorites(ids, markOrUnmark, user, h);
	}

	@Override
	public void move(String sourceId, String destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonObject>> handler) {
		// dont need to check
		this.folderManager.move(sourceId, destinationFolderId, user, handler);
	}

	@Override
	public void moveAll(Collection<String> sourceId, String destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		this.folderManager.moveAll(sourceId, destinationFolderId, user, handler);
	}

	@Override
	public void rename(String id, String newName, UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
		// Dont need to check
		this.folderManager.rename(id, newName, user, handler);
	}

	@Override
	public void restore(String id, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		// dont need to change quota
		this.folderManager.restore(id, user, handler);
	}

	@Override
	public void share(String id, ElementShareOperations shareOperations, Handler<AsyncResult<JsonObject>> h) {
		// dont need to change
		this.folderManager.share(id, shareOperations, h);
	}

	@Override
	public void shareAll(Collection<String> ids, ElementShareOperations shareOperations,
			Handler<AsyncResult<Collection<JsonObject>>> h) {
		// dont need to change
		this.folderManager.shareAll(ids, shareOperations, h);
	}

	@Override
	public void trash(String id, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		// dont need to change quota
		this.folderManager.trash(id, user, handler);
	}

	@Override
	public void updateFile(String id, Optional<String> parentId, JsonObject doc,
			Handler<AsyncResult<JsonObject>> handler) {
		this.folderManager.updateFile(id, parentId, doc, handler);
	}

	@Override
	public void importFile(String filePath, String oldFileId, String userId, Handler<JsonObject> handler)
	{
		FolderManagerWithQuota self = this;
		this.folderManager.importFile(filePath, oldFileId, userId, new Handler<JsonObject>()
		{
			@Override
			public void handle(JsonObject writtenFile)
			{
				quotaService.incrementStorage(userId, DocumentHelper.getFileSize(writtenFile), self.quotaThreshold, new Handler<Either<String, JsonObject>>()
				{
					@Override
					public void handle(Either<String, JsonObject> incRes)
					{
						handler.handle(writtenFile);
					}
				});
			}
		});
	}

	@Override
	public void createThumbnailIfNeeded(JsonObject uploadedDoc, JsonObject mongoDoc, Handler<AsyncResult<JsonObject>> handler)
	{
		this.folderManager.createThumbnailIfNeeded(uploadedDoc, mongoDoc, handler);
	}

	@Override
	public void updateShared(String id, UserInfos user, Handler<AsyncResult<Void>> handler) {
		// do not need to check
		this.folderManager.updateShared(id, user, handler);
	}

	@Override
	public void restoreAll(Set<String> ids, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		this.folderManager.restoreAll(ids, user, handler);
	}

	@Override
	public void trashAll(Set<String> ids, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		this.folderManager.trashAll(ids, user, handler);
	}

}
