package org.entcore.common.folders.impl;

import static org.entcore.common.folders.impl.QueryHelper.isOk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.ElementShareOperations;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.impl.QueryHelper.DocumentQueryBuilder;
import org.entcore.common.folders.impl.QueryHelper.RestoreParentDirection;
import org.entcore.common.share.ShareService;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.FileStats;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.FileUtils;
import org.entcore.common.utils.StopWatch;
import org.entcore.common.utils.StringUtils;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.ETag;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
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

	private static void logTime(StopWatch watch, String prefix){
		try{
			final long second = watch.elapsedTimeSeconds();
			if(second > 1){
				log.warn(prefix+ " : "+ second + " seconds");
			}
		} catch(Exception e){}
	}

	protected final Vertx				vertx;
	protected final FileSystem	fileSystem;
	protected final EventBus		eb;

	protected final QueryHelper queryHelper;

	protected final ShareService shareService;
	protected final InheritShareComputer inheritShareComputer;

	protected final String imageResizerAddress;
	protected boolean allowDuplicate = false;
	protected final FolderImporterZip zipImporter;
	public FolderManagerMongoImpl(String collection, Storage sto, Vertx vertx, FileSystem fs, EventBus eb, ShareService shareService, String imageResizerAddress, boolean useOldChildrenQuery)
	{
		this.storage = sto;
		this.vertx = vertx;
		this.fileSystem = fs;
		this.eb = eb;
		this.shareService = shareService;
		this.imageResizerAddress = imageResizerAddress;
		this.queryHelper = new QueryHelper(collection, useOldChildrenQuery);
		this.inheritShareComputer = new InheritShareComputer(queryHelper);
		this.zipImporter = new FolderImporterZip(vertx, this);
	}

	public void setAllowDuplicate(boolean allowDuplicate) {
		this.allowDuplicate = allowDuplicate;
	}

	protected Future<Boolean> isDuplicate(Optional<String> parentId, JsonObject file, String type){
		final String name = DocumentHelper.getName(file);
		final String id = DocumentHelper.getId(file);
		return isDuplicate(parentId, id,name, type);
	}
	
	protected Future<Boolean> isDuplicate(Optional<String> parentId, String id, String name, String type){
		if(allowDuplicate) return Future.succeededFuture(false);
		if(parentId.isPresent()) {
			DocumentQueryBuilder builder = queryHelper.queryBuilder().withParent(parentId.get()).withNameEq(name).withFileType(type);
			if(id != null){
				builder.withIdNotEq(id);
			}
			return queryHelper.countAll(builder).map(e -> e > 0);
		} else {
			return Future.succeededFuture(false);
		}
	}

	protected Future<JsonObject> renameFileOnDuplicate(Optional<String> parentId, JsonObject file){
		final String name = DocumentHelper.getName(file);
		return isDuplicate(parentId, file, FILE_TYPE).compose(isDuplicate->{
			if(isDuplicate){
				final Optional<String> extension = FileUtils.getFileExtension(name);
				final String withoutExt = FileUtils.stripExtension(name);
				return queryHelper.countAll(queryHelper.queryBuilder().withParent(parentId.get()).withNameStarts(withoutExt).withFileType(FILE_TYPE)).map(count->{
					final String ext = extension.map(e -> "."+e).orElse("");
					DocumentHelper.setName(file, withoutExt + "_" + count+ext);
					return file;
				});
			} else {
				return Future.succeededFuture(file);
			}
		});
	}

	@Override
	public void addFile(Optional<String> parentId, JsonObject doc, String ower, String ownerName,
			Handler<AsyncResult<JsonObject>> handler) {
		this.inheritShareComputer.computeFromParentId(doc, false, parentId).compose(parent -> {
			if (parentId.isPresent()) {
				doc.put("eParent", parentId.get());
			}
			DocumentHelper.initFile(doc, ower, ownerName);
			return renameFileOnDuplicate(parentId, doc);
		}).compose(aDoc->{ 
			return queryHelper.insert(aDoc);
		}).onComplete(handler);
	}

	@Override
	public void addFileWithParent(Optional<JsonObject> parentOpt, JsonObject doc, String owner, String ownerName,
								  Handler<AsyncResult<JsonObject>> handler) {
		this.inheritShareComputer.computeFromParent(doc, false, parentOpt).compose(parent -> {
			if (parentOpt.isPresent()) {
				doc.put("eParent", DocumentHelper.getId(parentOpt.get()));
			}
			DocumentHelper.initFile(doc, owner, ownerName);
			//
			return renameFileOnDuplicate(parentOpt.map(e -> DocumentHelper.getId(e)), doc).compose(aDoc->{
				return queryHelper.insert(aDoc);
			});
		}).onComplete(handler);
	}

	@Override
	public void copy(String sourceId, Optional<String> destFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		assertDestinationOnMoveOrCopy(sourceId, destFolderId).compose(ok -> {
			return queryHelper.findAllAsList(
					queryHelper.queryBuilder().filterByInheritShareAndOwner(user).withExcludeDeleted().withId(sourceId))
					.compose(docs -> {
						return copyRecursivelyFromParentId(Optional.of(user), docs, destFolderId, false);
					});
		}).onComplete(handler);
	}

	private Future<Void> assertDestinationOnMoveOrCopy(String sourceId, Optional<String> destinationFolderId) {
		List<String> sourceIds = new ArrayList<>();
		sourceIds.add(sourceId);
		return assertDestinationOnMoveOrCopy(sourceIds, destinationFolderId);
	}

	private Future<Void> assertDestinationOnMoveOrCopy(Collection<String> sourceIds,
			Optional<String> destinationFolderId) {
		if (destinationFolderId.isPresent()) {
			final String destinationId = destinationFolderId.get();
			return queryHelper.findById(destinationId).compose(destination -> {
				// destination should be a folder
				if (!DocumentHelper.isFolder(destination)) {
					return Future.failedFuture("Destination is not a folder: " + DocumentHelper.getType(destination));
				}
				// source should not be equals to dest
				if (sourceIds.contains(destinationId)) {
					return Future.failedFuture("Source should cant also be destination: " + destinationId);
				}
				// destination cannot be a descendant of source
				List<String> ancestors = DocumentHelper.getAncestorsAsList(destination);
				for (String a : ancestors) {
					if (sourceIds.contains(a)) {
						return Future.failedFuture("Source cannot be an ancestor of destination: " + a);
					}
				}
				return Future.succeededFuture();
			});
		} else {
			// if copying to root => no matters
			return Future.succeededFuture();
		}
	}

	@Override
	public void copyAll(Collection<String> sourceIds, Optional<String> destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		if (sourceIds.isEmpty()) {
			handler.handle(new DefaultAsyncResult<JsonArray>(new JsonArray()));
			return;
		}
		assertDestinationOnMoveOrCopy(sourceIds, destinationFolderId).compose(ok -> {
			return queryHelper.findAllAsList(queryHelper.queryBuilder().filterByInheritShareAndOwner(user)
					.withExcludeDeleted().withId(sourceIds)).compose(docs -> {
						return copyRecursivelyFromParentId(Optional.of(user), docs, destinationFolderId, false);
					});
		}).onComplete(handler);
	}

	private Future<JsonArray> copyFile(Optional<UserInfos> userOpt, Collection<JsonObject> originals,
			Optional<JsonObject> parent, boolean keepVisibility) {
		return StorageHelper.copyFileInStorage(storage, originals, false).compose(oldFileIdForNewFileId -> {
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
				if (!keepVisibility) {
					copy.remove("protected");
					copy.remove("public");
				}
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
				copy.put("deleted", false);
				// merge shared after reset shared
				InheritShareComputer.mergeShared(parent, copy, true);
				// copy file from storage
				StorageHelper.replaceAll(copy, oldFileIdForNewFileId);

				return copy;
			}).collect(Collectors.toList());
			// save copies in database using bulk (new fileid)
			return queryHelper.insertAll(copies)
					.map(c -> c.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
		});
	}

	/** 
	 * Duplicate the files of a list of documents, in storage,
	 * and return new documents indexing the duplicated files.
	 * 
	 * @param userOpt optional user.
	 * @param documents list of original documents.
	 * @param keepVisibility keep the `protected` or `public` visibility of original documents.
	 * 
	 * @return ordered list of optional documents, indexing the duplicated files in storage.
	 * The order of new documents is the same as the originals.
	 * Any empty document means its files were not found in storage or the copy failed.
	 */
	private Future<List<Optional<JsonObject>>> duplicateFilesNoFail(
			Optional<UserInfos> userOpt,
			List<JsonObject> documents,
			boolean keepVisibility
	) {
		// Duplicate every file documents, including attached files.
		List<Future> copyFutures = documents.stream()
		.map( doc -> {
			// Only *files* documents are duplicated in storage
			if( !DocumentHelper.isFile(doc) ) {
				return Future.succeededFuture(Optional.empty());
			}

			// Copy document files
			final List<String> fileIds = StorageHelper.getListOfFileIds(doc);
			return StorageHelper.copyFiles(storage, fileIds)
			.recover( t -> {
				 // Never fails this copy
				log.warn("Unexpected failure from StorageHelper.copyFiles() -> skipping those files.", t);
				return Future.succeededFuture((Map<String, String>) Collections.EMPTY_MAP);
			})
			.map( copiedFilesMap -> {
				// If files were not copied, empty the resulting doc.
				if( copiedFilesMap == null || copiedFilesMap.isEmpty() ) {
					return Optional.empty();
				}

				JsonObject copy = doc.copy();
				copy.put("copyFromId", doc.getString("_id"));
				copy.remove("_id");
				copy.remove("eParent");
				copy.remove("eParentOld");
				if (!keepVisibility) {
					copy.remove("protected");
					copy.remove("public");
				}
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
				copy.put("deleted", false);
				// merge shared after reset shared
				InheritShareComputer.mergeShared(Optional.empty(), copy, true);
				// copy file from storage
				StorageHelper.replaceAll(copy, copiedFilesMap, true);

				return Optional.of(copy);
			});
		}).collect(Collectors.toList());

		return CompositeFuture.all(copyFutures).map(all -> all.list());
	}

	/**
	 * Store documents in MongoDb. They are optional and may be empty.
	 * 
	 * @param optionalDocuments list of documents to store.
	 * 
	 * @return ordered list of json documents, where entrant empty documents are converted to null,
	 * and non-empty are updated with their new _id.
	 */
	private Future<List<JsonObject>> storeOptionalDocumentsNoFail(List<Optional<JsonObject>> optionalDocuments) {
		final List<Future> futures = optionalDocuments.stream()
			.map(jsonOpt -> jsonOpt.isPresent() ? queryHelper.insert(jsonOpt.get()) : Future.succeededFuture(null))
			.collect(Collectors.toList());
		
		return CompositeFuture.join(futures)
			.map(unused -> futures)
			.recover(unused -> Future.succeededFuture(futures))
			.map(inserted -> inserted.stream()
				.map(future -> future.succeeded() ? (JsonObject) future.result() : null)
				.collect(Collectors.toList()));	
	}

	public Future<List<JsonObject>> copyAllNoFail(
			Optional<UserInfos> userOpt,
			List<JsonObject> originals,
			boolean keepVisibility
	) {
		// Duplicate every file documents, including attached files
		return duplicateFilesNoFail(userOpt, originals, keepVisibility)
		// Persist the copied documents in DB
		.compose( newDocuments -> storeOptionalDocumentsNoFail(newDocuments) )
		.onFailure( t -> {
			log.error("FolderManagerMongoImpl.copyAllNoFail() should never fail", t);
		});
	}

	private Future<JsonArray> copyRecursivelyFromParentId(Optional<UserInfos> userOpt, Collection<JsonObject> docs,
			Optional<String> newParentIdOpt, boolean keepVisibility) {
		if (newParentIdOpt.isPresent()) {
			return queryHelper.findById(newParentIdOpt.get()).compose(newParent -> copyRecursivelyFromParent(userOpt,
					docs, Optional.ofNullable(newParent), keepVisibility));
		} else {
			return copyRecursivelyFromParent(userOpt, docs, Optional.empty(), keepVisibility);
		}
	}

	private Future<JsonArray> copyRecursivelyFromParent(Optional<UserInfos> userOpt, Collection<JsonObject> docs,
			Optional<JsonObject> newParentOpt, boolean keepVisibility) {
		JsonArray allCopies = new JsonArray();
		return this.copyFile(userOpt, docs, newParentOpt, keepVisibility).compose(copies -> {
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
				futures.add(copyRecursivelyFromParent(userOpt, childrenByParents.get(oldParent), nextParentRecursion,
						keepVisibility));
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
	public void copyUnsafe(String sourceId, Optional<String> destFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		assertDestinationOnMoveOrCopy(sourceId, destFolderId).compose(ok -> {
			return queryHelper.findAllAsList(queryHelper.queryBuilder().withId(sourceId).withExcludeDeleted())
					.compose(docs -> {
						return copyRecursivelyFromParentId(Optional.of(user), docs, destFolderId, true);
					});
		}).onComplete(handler);
	}

	@Override
	public void countByQuery(ElementQuery query, UserInfos user, Handler<AsyncResult<Integer>> handler) {
		queryHelper.countAll(DocumentQueryBuilder.fromElementQuery(query, Optional.ofNullable(user)))
				.onComplete(handler);
	}

	@Override
	public void createExternalFolder(JsonObject folder, UserInfos user, String externalId, Handler<AsyncResult<JsonObject>> handler) {
			DocumentHelper.initFolder(folder, user.getUserId(), user.getUsername());
			folder.put("externalId", externalId);
			queryHelper.upsertFolder(folder).onComplete(handler);
	}

	@Override
	public void createFolder(JsonObject folder, UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
		this.inheritShareComputer.compute(folder, false).compose(res -> {
			DocumentHelper.initFolder(folder, user.getUserId(), user.getUsername());
			return queryHelper.insert(folder);
		}).onComplete(handler);
	}

	@Override
	public void createFolder(String destinationFolderId, UserInfos user, JsonObject folder,
			Handler<AsyncResult<JsonObject>> handler) {
		folder.put("eParent", destinationFolderId);
		isDuplicate(Optional.ofNullable(destinationFolderId), folder, FOLDER_TYPE).compose(isDuplicate->{
			final Promise<JsonObject> future = Promise.promise();
			if(isDuplicate){
				future.fail("folders.errors.duplicate.folder");
			}else{
				this.createFolder(folder, user, future);
			}
			return future.future();
		}).onComplete(handler);
	}

	@Override
	public void delete(String id, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		List<String> idFolders = new ArrayList<>();
		idFolders.add(id);
		deleteFolderRecursively(idFolders, Optional.ofNullable(user))
				.map(v -> v.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll)).onComplete(handler);
	}

	@Override
	public void deleteAll(Set<String> ids, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		deleteFolderRecursively(ids, Optional.ofNullable(user))
				.map(v -> v.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll)).onComplete(handler);
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
		}).onComplete(handler);
	}

	private Future<List<JsonObject>> deleteFiles(List<JsonObject> files) {
		if (files.isEmpty()) {
			return Future.succeededFuture(files);
		}
		// Set to avoid duplicate
		Set<String> ids = files.stream().map(o -> o.getString("_id")).collect(Collectors.toSet());
		return queryHelper.deleteByIds((ids)).compose(res -> {
			Promise<List<JsonObject>> future = Promise.promise();
			List<String> listOfFilesIds = StorageHelper.getListOfFileIds(files);
			final StopWatch timeToDel = new StopWatch();
			this.storage.removeFiles(new JsonArray(listOfFilesIds), resDelete -> {
				logTime(timeToDel, "Delete files");
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
			return future.future();
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

					future.onComplete(result -> {
						if (result.succeeded()) {
							List<JsonObject> rows = result.result();
							FolderExporterZip zipBuilder = new FolderExporterZip(storage, fileSystem, false);
							zipBuilder.exportAndSendZip(bodyRoot, rows, request, true).onComplete(zipEvent -> {
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
					downloadOneFile(bodyRoot, request, false);
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
	public void downloadFiles(Collection<String> ids, UserInfos user, boolean includeDeleted, HttpServerRequest request) {
		DocumentQueryBuilder parentFilter = queryHelper.queryBuilder().filterByInheritShareAndOwner(user)
				.withExcludeDeleted().withIds(ids);
		DocumentQueryBuilder childFilter = queryHelper.queryBuilder().withExcludeDeleted();
		if(includeDeleted){
			parentFilter = queryHelper.queryBuilder().filterByInheritShareAndOwner(user).withIds(ids);
			childFilter = queryHelper.queryBuilder();
		}
		queryHelper.getChildrenRecursively(parentFilter, Optional.ofNullable(childFilter), true).onComplete(msg -> {
			if (msg.succeeded() && msg.result().size() > 0) {
				// download ONE file
				List<JsonObject> all = msg.result();
				if (all.size() == 1 //
						&& DocumentHelper.isFile(all.get(0))) {
					downloadOneFile(all.get(0), request,false);
					return;
				}
				// download multiple files
				FolderExporterZip zipBuilder = new FolderExporterZip(storage, fileSystem, false);
				zipBuilder.exportAndSendZip(all, request, true).onComplete(zipEvent -> {
					if (zipEvent.failed()) {
						request.response().setStatusCode(500).end();
					}
				});

			} else {
				request.response().setStatusCode(404).end();
			}

		});
	}

	private void downloadOneFile(JsonObject bodyRoot, HttpServerRequest request, boolean inline) {
		String name = bodyRoot.getString("name");
		String file = bodyRoot.getString("file");
		JsonObject metadata = bodyRoot.getJsonObject("metadata");
		if (inline && ETag.check(request, file)) {
			notModified(request, file);
		} else {
			storage.sendFile(file, name, request, inline, metadata);
		}
		return;
	}

	@Override
	public void findByQuery(ElementQuery query, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		final DocumentQueryBuilder builder = DocumentQueryBuilder.fromElementQuery(query, Optional.ofNullable(user));
		// query
		if (query.getHierarchical() != null && query.getHierarchical()) {
			queryHelper.listHierarchical(builder).onComplete(handler);
		} else {
			if(query.isDirectShared()){
				queryHelper.findAllAsList(builder).compose(elementShared->{
					final Set<String> parentIdsOfShared = elementShared.stream().map(f -> (JsonObject) f)
							.map(f -> DocumentHelper.getParent(f)).filter(f -> !StringUtils.isEmpty(f))
							.collect(Collectors.toSet());
					if(parentIdsOfShared.isEmpty()){
						return Future.succeededFuture(new JsonArray(elementShared));
					}
					final DocumentQueryBuilder queryParents = queryHelper.queryBuilder().filterByInheritShareAndOwner(user).withFileType(FolderManager.FOLDER_TYPE).withId(parentIdsOfShared).withProjection("_id");
					return queryHelper.findAllAsList(queryParents).map(visibleFolders ->{
						final JsonArray rootShared = new JsonArray();
						final Set<String> folderIdsSet = visibleFolders.stream().map(s-> DocumentHelper.getId(s)).collect(Collectors.toSet());
						for(final JsonObject doc : elementShared){
							if(!folderIdsSet.contains(DocumentHelper.getParent(doc))){
								rootShared.add(doc);
							}
						}
						return rootShared;
					});
				}).onComplete(handler);
			}else{
				queryHelper.findAll(builder).onComplete(handler);
			}
		}
	}

	@Override
	public void info(String id, UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
		// find only non deleted file/folder that i can see
		Future<JsonObject> future = queryHelper
				.findOne(queryHelper.queryBuilder().withId(id).filterByInheritShareAndOwner(user).withExcludeDeleted());
		future.onComplete(handler);
	}

	@Override
	public void list(String idFolder, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		Future<JsonArray> future = queryHelper.findAll(queryHelper.queryBuilder().filterByInheritShareAndOwner(user)
				.withParent(idFolder).withExcludeDeleted());
		future.onComplete(handler);
	}

	@Override
	public void listFoldersRecursively(UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		Future<JsonArray> future = queryHelper.listHierarchical(//
				queryHelper.queryBuilder().filterBySharedAndOwner(user).withExcludeDeleted());
		future.onComplete(handler);
	}

	@Override
	public void listFoldersRecursivelyFromFolder(String idFolder, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		Future<JsonArray> future = queryHelper.getChildrenRecursively(
				queryHelper.queryBuilder().filterByInheritShareAndOwner(user).withExcludeDeleted().withId(idFolder), //
				Optional.ofNullable(queryHelper.queryBuilder().withExcludeDeleted().withId(idFolder)), //
				true).map(f -> new JsonArray(f));
		future.onComplete(handler);
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
				}).onComplete(h);
	}

	@Override
	public void move(String sourceId, String destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonObject>> handler) {
		this.info(sourceId, user, msg -> {
			if (msg.succeeded()) {
				JsonObject previous = msg.result();
				if (previous != null) {
					assertDestinationOnMoveOrCopy(sourceId, Optional.ofNullable(destinationFolderId)).compose(ok -> {
						return queryHelper.updateMove(sourceId, destinationFolderId).compose(s -> {
							DocumentHelper.setParent(previous, destinationFolderId);
							return this.inheritShareComputer.compute(previous, true)
									.compose(res -> queryHelper.bulkUpdateShares(res));
						}).map(previous);
					}).onComplete(handler);
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
		assertDestinationOnMoveOrCopy(sourceIds, Optional.ofNullable(destinationFolderId)).compose(ok -> {
			final List<Future<?>> futures = sourceIds.stream().map(sourceId -> {
				Promise<JsonObject> future = Promise.promise();
				this.move(sourceId, destinationFolderId, user, future);
				return future.future();
			}).collect(Collectors.toList());
			return Future.all(futures).map(results -> {
				return results.list().stream().map(o -> (JsonObject) o).collect(JsonArray::new, JsonArray::add,
						JsonArray::addAll);
			});
		}).onComplete(handler);
	}

	@Override
	public void rename(String id, String newName, UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
		this.info(id, user, msg -> {
			if (msg.succeeded()) {
				final String type = DocumentHelper.getType(msg.result());
				final String parent = DocumentHelper.getParent(msg.result());
				isDuplicate(Optional.ofNullable(parent),id, newName, type).compose(isDuplicate->{
					if(isDuplicate){
						return Future.failedFuture("folders.errors.duplicate");
					}
					String nameSearch = newName != null ? DocumentHelper.prepareNameForSearch(newName) : "";
					JsonObject doc = msg.result();
					doc.put("name", newName);// need for result
					doc.put("nameSearch", nameSearch);
					MongoUpdateBuilder set = new MongoUpdateBuilder().set("name", newName).set("nameSearch", nameSearch);
					return queryHelper.update(id, set).map(doc);
				}).onComplete(handler);
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
				return Future.succeededFuture(new JsonArray());
			}
			List<JsonObject> all = rows.stream().map(o -> (JsonObject) o).collect(Collectors.toList());
			return setDeleteFlag(user, roots, all, deleted);
		}).onComplete(handler);
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
					final List<Future> futures = new ArrayList<Future>();
					final Promise<JsonObject> futureShared = Promise.promise();
					futures.add(futureShared.future());
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
							this.shareService.removeUserShare(shareOperations.getUserId(), id,
									shareOperations.getActions(), handler);
							break;
						case SHARE_OBJECT:
							futures.add(
									this.shareService.share(user.getUserId(), id, shareOperations.getShare(), handler));
							break;
					}
					return CompositeFuture.all(futures).compose(ev -> {
						final JsonObject resultShare = (JsonObject) ev.list().get(0);
						Future<InheritShareComputer.InheritShareResult> futureInheritCompute = null;
						if (ev.list().size() == 2) {
							final JsonObject validatedShares = (JsonObject) ev.list().get(1);
							founded.put("shared", validatedShares.getJsonArray("shared", new JsonArray()));
							// avoid fetch shared => in case of mongo lag replication it could make shared
							// empty #34806
							futureInheritCompute = this.inheritShareComputer.compute(founded, true);
						} else {
							// recompute from id (to refresh shared array)
							futureInheritCompute = this.inheritShareComputer.compute(id, true);
						}
						return futureInheritCompute.compose(res -> queryHelper.bulkUpdateShares(res).map(res))
								// break parent link if needed
								.compose(res -> {
									Promise<Void> future = Promise.promise();
									if (res.parentRoot.isPresent()) {
										JsonObject parentRoot = res.parentRoot.get();
										Boolean isShared = DocumentHelper.isShared(res.root);
										Boolean parentIsShared = DocumentHelper.isShared(parentRoot);
										// if my parent is shared and i m not (vice versa)...break the link
										if (!isShared.equals(parentIsShared)) {
											Set<String> ids = new HashSet<String>();
											ids.add(DocumentHelper.getId(res.root));
											return queryHelper.breakParentLink(ids);
										}
									}
									future.complete(null);
									return future.future();
								}).map(resultShare);
					});
				}).onComplete(hh);
	}

	@Override
	public void shareAll(Collection<String> ids, ElementShareOperations shareOperations,
			Handler<AsyncResult<Collection<JsonObject>>> h) {
		List<Future<?>> futures = ids.stream().map(id -> {
			Promise<JsonObject> future = Promise.promise();
			share(id, shareOperations, future);
			return future.future();
		}).collect(Collectors.toList());
		Future.all(futures).map(res -> {
			Collection<JsonObject> temp = res.list();
			return temp;
		}).onComplete(h);
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
		}).onComplete(handler);
	}

	@Override
	public void importFile(String filePath, String oldFileId, String userId, Handler<JsonObject> handler)
	{
		final StopWatch writeFile = new StopWatch();
		Handler<JsonObject> addFileSize = json -> {
			logTime(writeFile , "Write file");
			fileSystem.props(filePath, props -> {
				if (props.succeeded()) {
					json.put("metadata", new JsonObject().put("size", props.result().size()));
				}
				handler.handle(json);
			});
		};

		if(oldFileId == null || oldFileId.trim().isEmpty() == true)
			this.storage.writeFsFile(filePath, addFileSize);
		else
		{
			final StopWatch fileStat = new StopWatch();
			// Check whether the file already exists
			this.storage.fileStats(oldFileId, new Handler<AsyncResult<FileStats>>()
			{
				@Override
				public void handle(AsyncResult<FileStats> res)
				{
					logTime(fileStat, "File stats");
					// If the file already exists, duplicate it with a new id, else keep the old id
					if(res.succeeded() == true)
						storage.writeFsFile(filePath, addFileSize);
					else
						storage.writeFsFile(oldFileId, filePath, addFileSize);
				}
			});
		}
	}

	@Override
	public void importFileZip(FolderImporterZip.FolderImporterZipContext context, Handler<AsyncResult<JsonObject>> handler) {
		zipImporter.doFinalize(context).map(r->{
			return context.getResult();
		}).onComplete(handler);
	}

	@Override
	public void createThumbnailIfNeeded(JsonObject document, JsonObject thumbs, Handler<AsyncResult<JsonObject>> handler)
	{
		Promise<JsonObject> future = Promise.promise();

		if(this.imageResizerAddress == null || this.imageResizerAddress.trim().isEmpty())
		{
			future.fail(new RuntimeException("No image resizer"));
			handler.handle(future.future());

			return;
		}

		String fileId = DocumentHelper.getFileId(document);
		String documentId = DocumentHelper.getId(document);

		if (fileId != null && thumbs != null && !fileId.trim().isEmpty() && !thumbs.isEmpty() && DocumentHelper.isImage(document))
		{
			Pattern size = Pattern.compile("([0-9]+)x([0-9]+)");
			JsonArray outputs = new JsonArray();

			for (String thumb : thumbs.getMap().keySet())
			{
				Matcher m = size.matcher(thumb);
				if (m.matches())
				{
					try
					{
						int width = Integer.parseInt(m.group(1));
						int height = Integer.parseInt(m.group(2));

						if (width == 0 && height == 0)
							continue;

						JsonObject j = new JsonObject().put("dest", this.storage.getProtocol() + "://" + this.storage.getBucket());

						if (width != 0)
							j.put("width", width);
						if (height != 0)
							j.put("height", height);

						outputs.add(j);
					}
					catch (NumberFormatException e)
					{
						log.error("Invalid thumbnail size.", e);
					}
				}
			}

			if (outputs.size() > 0)
			{
				JsonObject json = new JsonObject()
					.put("action", "resizeMultiple")
					.put("src", this.storage.getProtocol() + "://" + this.storage.getBucket() + ":" + fileId)
					.put("destinations", outputs);

				this.eb.request(this.imageResizerAddress, json, new Handler<AsyncResult<Message<JsonObject>>>()
				{
					@Override
					public void handle(AsyncResult<Message<JsonObject>> result)
					{
						if(result.succeeded()) {
							Message<JsonObject> event = result.result();
							JsonObject thumbnails = event.body().getJsonObject("outputs");

							if ("ok".equals(event.body().getString("status")) && thumbnails != null)
							{
								addThumbnails( document, thumbnails, handler );
								return;
							}
						}

						future.fail(new RuntimeException("Failed to send a request to the image resizer", result.cause()));
						handler.handle(future.future());
					}
				});
			}
			else
			{
				future.complete(null);
				handler.handle(future.future());
			}
		}
		else
		{
			future.complete(document);
			handler.handle(future.future());
		}
	}

	@Override
	public void addThumbnails(JsonObject document, JsonObject thumbnails, Handler<AsyncResult<JsonObject>> handler)
	{
		final Promise<JsonObject> promise = Promise.promise();
		final String documentId = DocumentHelper.getId(document);
		final JsonObject mergedThumbnails = document.getJsonObject("thumbnails", new JsonObject()).mergeIn(thumbnails, true);
		if(documentId != null)
		{
			// Document is already stored in Mongo, so we update it.
			JsonObject mongoUpdate = new JsonObject().put("$set", new JsonObject().put("thumbnails", mergedThumbnails));

			queryHelper.update(documentId, mongoUpdate)
			.onComplete( result -> {
				if (result.succeeded() == false) {
					promise.fail(result.cause());
				} else {
					promise.complete(DocumentHelper.setThumbnails(document, thumbnails));
				}
				handler.handle( promise.future() );
			});
		}
		else
		{
			// Document does not exists in Mongo and will be stored later, so we just attach its thumbnails.
			promise.complete(DocumentHelper.setThumbnails(document, mergedThumbnails));
			handler.handle( promise.future() );
		}
	}

	@Override
	public void updateShared(String id, UserInfos user, Handler<AsyncResult<Void>> handler) {
		this.info(id, user, msg -> {
			if (msg.succeeded()) {
				Future<Void> future = this.inheritShareComputer.compute(msg.result(), true)
						.compose(res -> queryHelper.bulkUpdateShares(res)).map(e -> null);
				future.onComplete(handler);
			} else {
				handler.handle(toError(msg.cause()));
			}
		});

	}

}
