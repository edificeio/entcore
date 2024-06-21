package org.entcore.workspace.service.impl;

import com.google.thirdparty.publicsuffix.PublicSuffixType;
import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.bson.conversions.Bson;
import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.impl.DocumentHelper;
import org.entcore.common.folders.impl.FolderManagerWithQuota;
import org.entcore.common.folders.impl.QueryHelper.DocumentQueryBuilder;
import org.entcore.common.folders.impl.StorageHelper;
import org.entcore.common.folders.QuotaService;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.share.ShareService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.workspace.service.WorkspaceService;
import org.entcore.workspace.controllers.WorkspaceController;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.dao.RevisionDao;

import java.util.*;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultWorkspaceService extends FolderManagerWithQuota implements WorkspaceService {

	private final Storage storage;
	private ShareService shareService;
	private static final JsonObject PROPERTIES_KEYS = new JsonObject().put("name", 1).put("alt", 1).put("legend", 1);
	private static final Logger log = LoggerFactory.getLogger(DefaultWorkspaceService.class);
	private int threshold;
	private String imageResizerAddress;
	private EventBus eb;
	private TimelineHelper notification;
	private DocumentDao dao;
	private MongoDb mongo;
	private final RevisionDao revisionDao;
	private final DocumentDao documentDao;

	public DefaultWorkspaceService(Storage storage, MongoDb mongo, int threshold, String imageResizerAddress,
								   QuotaService quotaService, FolderManager folderManager, Vertx vertx, ShareService share, boolean useOldQueryChildren) {
		super(DocumentDao.DOCUMENTS_COLLECTION, threshold, quotaService, folderManager, vertx, useOldQueryChildren);
		this.dao = new DocumentDao(mongo);
		this.storage = storage;
		this.mongo = mongo;
		this.threshold = threshold;
		this.shareService = share;
		this.imageResizerAddress = imageResizerAddress;
		this.eb = vertx.eventBus();
		this.revisionDao = new RevisionDao(mongo);
		this.documentDao = new DocumentDao(mongo);
	}

	@Override
	public void hasRightsOn(final Collection<String> elementIds, Boolean onEmpty, UserInfos user, final Handler<AsyncResult<Boolean>> handler){
		if(elementIds.size()>0){
			final ElementQuery query = new ElementQuery(true);
			query.setIds(elementIds);
			this.countByQuery(query, user, res -> {
				handler.handle(new DefaultAsyncResult<>(res.succeeded() && res.result()==elementIds.size()));
			});
		}else{
			handler.handle(new DefaultAsyncResult<>(onEmpty));
		}
	}

	@Override
	public void canWriteOn(final Optional<String> elementId, UserInfos user, final Handler<AsyncResult<Optional<JsonObject>>> handler){
		if(elementId.isPresent()){
			final ElementQuery query = new ElementQuery(true);
			query.setId(elementId.get());
			query.setActionExistsInInheritedShares(WorkspaceController.WRITE_ACTION);
			query.setLimit(1);
			this.findByQuery(query, user, res -> {
				final Optional<JsonObject> resOpt = res.succeeded() && res.result().size() == 1?
						Optional.ofNullable(res.result().getJsonObject(0)): Optional.empty();
				handler.handle(new DefaultAsyncResult<>(resOpt));
			});
		}else{
			handler.handle(new DefaultAsyncResult<>(Optional.empty()));
		}
	}

	@Override
	public void findById(String id, Handler<JsonObject> handler) {
		dao.findById(id, handler);
	}

	@Override
	public void findById(String id, String onwer, Handler<JsonObject> handler) {
		dao.findById(id, onwer, handler);
	}

	@Override
	public void findById(String id, JsonObject keys, Handler<JsonObject> handler) {
		dao.findById(id, keys, handler);
	}

	@Override
	public void findById(String id, String onwer, boolean publicOnly, Handler<JsonObject> handler) {
		dao.findById(id, onwer, publicOnly, handler);
	}

	@Override
	public void getQuotaAndUsage(String userId, Handler<Either<String, JsonObject>> handler) {
		this.quotaService.quotaAndUsage(userId, handler);
	}

	@Override
	public void getShareInfos(String userId, String resourceId, String acceptLanguage, String search,
			Handler<Either<String, JsonObject>> handler) {
		this.shareService.inheritShareInfos(userId, resourceId, acceptLanguage, search, handler);
	}

	public void addDocument(final UserInfos user, final float quality, final String name, final String application,
							final JsonObject doc, final JsonObject uploaded, final Handler<AsyncResult<JsonObject>> handler) {
		compressImage(uploaded, quality, new Handler<Integer>() {
			@Override
			public void handle(Integer size) {
				JsonObject meta = uploaded.getJsonObject("metadata");
				if (size != null && meta != null) {
					meta.put("size", size);
				}
				addAfterUpload(uploaded, doc, name, application, user.getUserId(), user.getUsername(),
						handler);
			}
		});

	}

	public void addDocumentWithParent(Optional<JsonObject> parent, final UserInfos user, final float quality, final String name, final String application,
							final JsonObject doc, final JsonObject uploaded, final Handler<AsyncResult<JsonObject>> handler) {
		compressImage(uploaded, quality, new Handler<Integer>() {
			@Override
			public void handle(Integer size) {
				JsonObject meta = uploaded.getJsonObject("metadata");
				if (size != null && meta != null) {
					meta.put("size", size);
				}
				addAfterUploadWithParent(parent, uploaded, doc, name, application, user.getUserId(), user.getUsername(),
						handler);
			}
		});

	}

	public void updateDocument(final String id, final float quality, final String name,
			final JsonObject uploaded, UserInfos user, final Handler<Message<JsonObject>> handler) {
		compressImage(uploaded, quality, new Handler<Integer>() {
			@Override
			public void handle(Integer size) {
				JsonObject meta = uploaded.getJsonObject("metadata");
				if (size != null && meta != null) {
					meta.put("size", size);
				}
				updateAfterUpload(id, name, uploaded, user, handler);
			}
		});
	}

	public void documentProperties(final String id, final Handler<JsonObject> handler) {
		this.findById(id, PROPERTIES_KEYS, handler);
	}

	public String addComment(final String id, final String commentId, final String comment, final UserInfos user,
			final Handler<JsonObject> handler) {
		JsonObject query = new JsonObject().put("$push",
				new JsonObject().put("comments",
						new JsonObject().put("id", commentId).put("author", user.getUserId())
								.put("authorName", user.getUsername()).put("posted", MongoDb.formatDate(new Date()))
								.put("comment", comment)));
		dao.update(id, query, handler);
		return commentId;
	}

	public void deleteComment(final String id, final String commentId, final Handler<JsonObject> handler) {
		JsonObject query = new JsonObject().put("$pull",
				new JsonObject().put("comments", new JsonObject().put("id", commentId)));

		dao.update(id, query, handler);
	}

	private void compressImage(JsonObject srcFile, float quality, final Handler<Integer> handler) {
		if (DocumentHelper.isImage(srcFile) == false) {
			handler.handle(null);
			return;
		}

		JsonObject json = new JsonObject().put("action", "compress").put("quality", quality)
				.put("src", storage.getProtocol() + "://" + storage.getBucket() + ":" + srcFile.getString("_id"))
				.put("dest", storage.getProtocol() + "://" + storage.getBucket() + ":" + srcFile.getString("_id"));
		eb.request(imageResizerAddress, json, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				Integer size = event.body().getInteger("size");
				handler.handle(size);
			}
		}));
	}

	public void addAfterUploadWithParent(final Optional<JsonObject> parent, final JsonObject uploaded, final JsonObject doc, String name, String application,
										 final String ownerId, final String ownerName, final Handler<AsyncResult<JsonObject>> handler){
		doc.put("name", getOrElse(name, uploaded.getJsonObject("metadata").getString("filename"), false));
		doc.put("metadata", uploaded.getJsonObject("metadata"));
		doc.put("file", uploaded.getString("_id"));
		doc.put("fileDate", MongoDb.formatDate(new Date()));
		doc.put("application", getOrElse(application, WorkspaceController.MEDIALIB_APP));
		addFileWithParent(parent, doc, ownerId, ownerName, res ->
		{
			if (res.succeeded()) {
				incrementStorage(doc);

				if (handler != null) {
					handler.handle(res);
				}

				revisionDao.create(res.result().getString("_id"), uploaded.getString("_id"),
						doc.getString("name"), doc.getString("owner"), doc.getString("owner"),
						doc.getString("ownerName"), doc.getJsonObject("metadata"), new JsonObject());
			}
			else if (handler != null)
			{
				handler.handle(res);
			}
		});
	}

	public void addAfterUpload(final JsonObject uploaded, final JsonObject doc, String name, String application,
			final String ownerId, final String ownerName, final Handler<AsyncResult<JsonObject>> handler) {
		doc.put("name", getOrElse(name, uploaded.getJsonObject("metadata").getString("filename"), false));
		doc.put("metadata", uploaded.getJsonObject("metadata"));
		doc.put("file", uploaded.getString("_id"));
		doc.put("fileDate", MongoDb.formatDate(new Date()));
		doc.put("application", getOrElse(application, WorkspaceController.MEDIALIB_APP));

		addFile(Optional.ofNullable(doc.getString("eParent")), doc, ownerId, ownerName, res ->
		{
			if (res.succeeded()) {
				incrementStorage(doc);

				if (handler != null) {
					handler.handle(res);
				}

				revisionDao.create(res.result().getString("_id"), uploaded.getString("_id"),
					doc.getString("name"), doc.getString("owner"), doc.getString("owner"),
					doc.getString("ownerName"), doc.getJsonObject("metadata"), new JsonObject());
			}
			else if (handler != null)
			{
				handler.handle(res);
			}
		});
	}

	@Override
	public Future<JsonObject> getRevision(String revisionId) {
		Promise<JsonObject> future = Promise.promise();
		revisionDao.findById(revisionId, ev -> {
			if ("ok".equals(ev.getString("status"))) {
				future.complete(ev.getJsonObject("result", new JsonObject()));
			} else {
				future.fail("COuld not found revision : " + revisionId);
			}
		});
		return future.future();
	}

	public void updateAfterUpload(final String id, final String name, final JsonObject uploaded,
			final UserInfos user, final Handler<Message<JsonObject>> handler) {
		findById(id, new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject old) {
				if ("ok".equals(old.getString("status"))) {
					final JsonObject metadata = uploaded.getJsonObject("metadata");
					JsonObject set = new JsonObject();
					final JsonObject doc = new JsonObject();
					doc.put("name", getOrElse(name, metadata.getString("filename")));
					final String now = MongoDb.formatDate(new Date());
					doc.put("modified", now);
					doc.put("metadata", metadata);
					doc.put("file", uploaded.getString("_id"));
					doc.put("fileDate", MongoDb.formatDate(new Date()));
					doc.put("alt", uploaded.getString("alt"));
					doc.put("legend", uploaded.getString("legend"));

					final JsonObject compositeDoc = DocumentHelper.setId(new JsonObject(), id);

					String query = "{ \"_id\": \"" + id + "\"}";
					set.put("$set", doc).put("$unset", new JsonObject().put("thumbnails", ""));
					mongo.update(DocumentDao.DOCUMENTS_COLLECTION, new JsonObject(query), set,
							new Handler<Message<JsonObject>>() {
								@Override
								public void handle(final Message<JsonObject> res) {
									String status = res.body().getString("status");
									JsonObject result = old.getJsonObject("result");
									if ("ok".equals(status) && result != null) {
										String userId = user != null ? user.getUserId() : result.getString("owner");
										String userName = user != null ? user.getUsername()
												: result.getString("ownerName");
										doc.put("owner", result.getString("owner"));
										incrementStorage(doc);

										Future<JsonObject> daoPromise = revisionDao.create(id, doc.getString("file"), doc.getString("name"),
											result.getString("owner"), userId, userName, metadata, new JsonObject());

										if (handler != null)
											daoPromise.onComplete(new Handler<AsyncResult<JsonObject>>()
											{
												@Override
												public void handle(AsyncResult<JsonObject> daoResult)
												{
													handler.handle(res);
												}
											});

									} else if (handler != null) {
										handler.handle(res);
									}
								}
							});
				} else if (handler != null) {
					handler.handle(null);
				}
			}
		});
	}

	public void listRevisions(final String id, final Handler<Either<String, JsonArray>> handler) {
		final Bson builder = Filters.eq("documentId", id);
		mongo.find(RevisionDao.DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder),
				MongoDbResult.validResultsHandler(handler));
	}

	public void getRevision(final String documentId, final String revisionId,
			final Handler<Either<String, JsonObject>> handler) {
		final Bson builder = Filters.and(Filters.eq("_id", revisionId), Filters.eq("documentId", documentId));
		mongo.findOne(RevisionDao.DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder),
				MongoDbResult.validResultHandler(handler));
	}

	@Override
	public void delete(String id, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		super.delete(id, user, afterDelete(handler));
	}

	@Override
	public void deleteAll(Set<String> ids, UserInfos user, Handler<AsyncResult<JsonArray>> handler) {
		super.deleteAll(ids, user, afterDelete(handler));
	}

	@Override
	public void deleteByQuery(ElementQuery query, Optional<UserInfos> user, Handler<AsyncResult<JsonArray>> handler) {
		super.deleteByQuery(query, user, afterDelete(handler));
	}

	private Handler<AsyncResult<JsonArray>> afterDelete(final Handler<AsyncResult<JsonArray>> resOrig) {
		return (res) -> {
			if (res.succeeded()) {
				JsonArray results = res.result();
				// Delete revisions for each sub-document
				for (Object obj : results) {
					JsonObject item = (JsonObject) obj;
					if (DocumentHelper.isFile(item) && !StringUtils.isEmpty(DocumentHelper.getFileId(item))) {
						deleteAllRevisions(DocumentHelper.getId(item),
								new JsonArray().add(DocumentHelper.getFileId(item)));
					}
				}
			}
			//
			resOrig.handle(res);
		};
	}

	private Future<JsonArray> deleteRevisionsOnDisk(JsonArray revisions, JsonArray alreadyDeleted) {
		@SuppressWarnings("unchecked")
		List<JsonObject> revisionsList = revisions.getList();
		Set<String> fileIds = new HashSet<String>(StorageHelper.getListOfFileIds(revisionsList));
		Promise<JsonArray> future = Promise.promise();
		JsonArray fileIdsJson = new JsonArray(new ArrayList<String>(fileIds));
		storage.removeFiles(fileIdsJson, event -> {
			future.complete(revisions);
		});
		return future.future();
	}

	private Future<JsonArray> deleteAllRevisions(final String documentId, final JsonArray alreadyDeleted) {
		return revisionDao.findByDoc(documentId).compose(revisions -> {
			Future<JsonArray> f = deleteRevisionsOnDisk(revisions, alreadyDeleted).map(revisions);
			return f;
		}).compose(revisions -> {
			return this.revisionDao.deleteByDoc(documentId).map(revisions);
		}).compose(revisions -> {
			for (Object obj : revisions) {
				JsonObject result = (JsonObject) obj;
				if (!alreadyDeleted.contains(result.getString("file")))
					decrementStorage(result);
			}
			Future<JsonArray> a = Future.succeededFuture(revisions);
			return a;
		});
	}

	public void deleteRevision(final String documentId, final String revisionId,
			final Handler<Either<String, JsonObject>> handler) {
		final Future<JsonArray> lastTwoFuture = revisionDao.getLastRevision(documentId, 2);
		final Future<Boolean> isLastFuture = lastTwoFuture
				.map(arr -> arr.size() > 0 && arr.getJsonObject(0).getString("_id", "").equals(revisionId));
		isLastFuture.compose(last -> {
			if (lastTwoFuture.result().size() <= 1) {
				return Future.failedFuture("Cannot delete the only 1 revision");
			}
			return revisionDao.findByDocAndId(documentId, revisionId).compose(revision -> {
				Future<JsonArray> future = deleteRevisionsOnDisk(new JsonArray().add(revision), new JsonArray());
				return future.map(revision);
			});
		}).compose(revision -> {
			return revisionDao.deleteByDocAndId(documentId, revisionId).map(revision);
		}).compose(revision -> {
			decrementStorage(revision);
			// restore document it was last
			if (isLastFuture.result()) {
				JsonObject previous = lastTwoFuture.result().getJsonObject(1);
				return documentDao.restaureFromRevision(documentId, previous).compose(doc->
				{
					return Future.succeededFuture(null);
				}).map(revision);
			} else {
				return Future.succeededFuture(revision);
			}
		}).onComplete(revision -> {
			if (revision.succeeded()) {
				handler.handle(new Either.Right<String, JsonObject>(revision.result()));
			} else {
				handler.handle(new Either.Left<String, JsonObject>(revision.cause().getMessage()));
			}
		});
	}

	@Override
	public void copy(String sourceId, Optional<String> destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		super.copy(sourceId, destinationFolderId, user, afterCopy(user.getUserId(), user.getUsername(), handler));
	}

	@Override
	public void copyAll(Collection<String> sourceIds, Optional<String> destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		super.copyAll(sourceIds, destinationFolderId, user, afterCopy(user.getUserId(), user.getUsername(), handler));
	}

	@Override
	public void copyUnsafe(String sourceId, Optional<String> destinationFolderId, UserInfos user,
			Handler<AsyncResult<JsonArray>> handler) {
		super.copyUnsafe(sourceId, destinationFolderId, user, afterCopy(user.getUserId(), user.getUsername(), handler));
	}

	private Handler<AsyncResult<JsonArray>> afterCopy(String userId, String userName,
			Handler<AsyncResult<JsonArray>> handler) {
		return (res) -> {
			if (res.succeeded()) {
				List<JsonObject> copied = res.result().stream()
						.filter(c -> c instanceof JsonObject && DocumentHelper.isFile((JsonObject) c))
						.map(c -> (JsonObject) c).collect(Collectors.toList());
				for (JsonObject c : copied) {
					revisionDao.create(DocumentHelper.getId(c), DocumentHelper.getFileId(c), DocumentHelper.getName(c),
							DocumentHelper.getOwner(c), DocumentHelper.getOwner(c), //
							DocumentHelper.getOwnerName(c), c.getJsonObject("metadata", new JsonObject()), //
							c.getJsonObject("thumbnails", new JsonObject()));
				}
			}
			handler.handle(res);
		};
	}

	public void emptySize(final String userId, final Handler<Long> emptySizeHandler) {
		quotaService.quotaAndUsage(userId, new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> r) {
				if (r.isRight()) {
					JsonObject j = r.right().getValue();
					if (j != null) {
						long quota = j.getLong("quota", 0l);
						long storage = j.getLong("storage", 0l);
						for (String attr : j.fieldNames()) {
							UserUtils.addSessionAttribute(eb, userId, attr, j.getLong(attr), null);
						}
						emptySizeHandler.handle(quota - storage);
					}
				}
			}
		});
	}

	public void emptySize(final UserInfos userInfos, final Handler<Long> emptySizeHandler) {
		try {
			long quota = Long.valueOf(userInfos.getAttribute("quota").toString());
			long storage = Long.valueOf(userInfos.getAttribute("storage").toString());
			emptySizeHandler.handle(quota - storage);
		} catch (Exception e) {
			quotaService.quotaAndUsage(userInfos.getUserId(), new Handler<Either<String, JsonObject>>() {
				@Override
				public void handle(Either<String, JsonObject> r) {
					if (r.isRight()) {
						JsonObject j = r.right().getValue();
						if (j != null) {
							long quota = j.getLong("quota", 0l);
							long storage = j.getLong("storage", 0l);
							for (String attr : j.fieldNames()) {
								UserUtils.addSessionAttribute(eb, userInfos.getUserId(), attr, j.getLong(attr), null);
							}
							emptySizeHandler.handle(quota - storage);
						}
					}
				}
			});
		}
	}

	public void incrementStorage(JsonObject added) {
		updateStorage(new JsonArray().add(added), null);
	}

	public void decrementStorage(JsonObject removed) {
		updateStorage(null, new JsonArray().add(removed));
	}

	public void decrementStorage(JsonObject removed, Handler<Either<String, JsonObject>> handler) {
		updateStorage(null, new JsonArray().add(removed), handler);
	}

	public void incrementStorage(JsonArray added) {
		updateStorage(added, null);
	}

	public void decrementStorage(JsonArray removed) {
		updateStorage(null, removed);
	}

	public void updateStorage(JsonObject added, JsonObject removed) {
		updateStorage(new JsonArray().add(added), new JsonArray().add(removed));
	}

	public void updateStorage(JsonArray addeds, JsonArray removeds) {
		updateStorage(addeds, removeds, null);
	}

	public void updateStorage(JsonArray addeds, JsonArray removeds, final Handler<Either<String, JsonObject>> handler) {
		Map<String, Long> sizes = new HashMap<>();
		if (addeds != null) {
			for (Object o : addeds) {
				if (!(o instanceof JsonObject))
					continue;
				JsonObject added = (JsonObject) o;
				Long size = added.getJsonObject("metadata", new JsonObject()).getLong("size", 0l);
				String userId = (added.containsKey("to")) ? added.getString("to") : added.getString("owner");
				if (userId == null) {
					log.info("UserId is null when update storage size");
					log.info(added.encode());
					continue;
				}
				Long old = sizes.get(userId);
				if (old != null) {
					size += old;
				}
				sizes.put(userId, size);
			}
		}

		if (removeds != null) {
			for (Object o : removeds) {
				if (!(o instanceof JsonObject))
					continue;
				JsonObject removed = (JsonObject) o;
				Long size = removed.getJsonObject("metadata", new JsonObject()).getLong("size", 0l);
				String userId = (removed.containsKey("to")) ? removed.getString("to") : removed.getString("owner");
				if (userId == null) {
					log.info("UserId is null when update storage size");
					log.info(removed.encode());
					continue;
				}
				Long old = sizes.get(userId);
				if (old != null) {
					old -= size;
				} else {
					old = -1l * size;
				}
				sizes.put(userId, old);
			}
		}

		for (final Map.Entry<String, Long> e : sizes.entrySet()) {
			quotaService.incrementStorage(e.getKey(), e.getValue(), threshold,
					new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> r) {
							if (r.isRight()) {
								JsonObject j = r.right().getValue();
								UserUtils.addSessionAttribute(eb, e.getKey(), "storage", j.getLong("storage"), null);
								if (j.getBoolean("notify", false)) {
									notifyEmptySpaceIsSmall(e.getKey());
								}
							} else {
								log.error(r.left().getValue());
							}
							if (handler != null) {
								handler.handle(r);
							}
						}
					});
		}
	}

	private void notifyEmptySpaceIsSmall(String userId) {
		List<String> recipients = new ArrayList<>();
		recipients.add(userId);
		notification.notifyTimeline(new JsonHttpServerRequest(new JsonObject()),
				WORKSPACE_NAME.toLowerCase() + ".storage", null, recipients, null, new JsonObject());
	}

	public void setNotification(TimelineHelper notification) {
		this.notification = notification;
	}

	public Future<Set<String>> getNotifyContributorDest(Optional<String> id, UserInfos user, Set<String> docIds) {
		Set<String> actions = new HashSet<>();
		actions.add(WorkspaceController.SHARED_ACTION);
		final Set<String> recipientId = new HashSet<>();
		Promise<Void> futureSHared = Promise.promise();
		if(id.isPresent()) {
			shareService.findUserIdsForInheritShare(id.get(), user.getUserId(), Optional.of(actions), ev -> {
				// get list of managers
				if (ev.succeeded()) {
					recipientId.addAll(ev.result());
					futureSHared.complete();
				} else {
					futureSHared.fail(ev.cause());
				}
			});
		}else {
			//no folder => no need to get folder's contributors
			futureSHared.complete();
		}
		return futureSHared.future().compose(resShared -> {
			// get list of historic owner
			return revisionDao.findByDocs(docIds).compose(versions -> {
				for (Object version : versions) {
					JsonObject vJson = (JsonObject) version;
					String userId = vJson.getString("userId");
					if (userId != null) {
						recipientId.add(userId);
					}
				}
				return Future.succeededFuture(null);
			});
		}).map(ev -> {
			// remove current user
			recipientId.remove(user.getUserId());
			return recipientId;
		});
	}


	@Override
	public void changeVisibility(final JsonArray documentIds, String visibility, final Handler<Message<JsonObject>> handler) {
		JsonObject jo = new JsonObject();
		Visibility v = Visibility.fromString(visibility.toLowerCase());
		switch (v) {
			case PROTECTED:
			case OWNER:
				jo.put("$set", new JsonObject().put("protected", true))
					.put("$unset", new JsonObject().put("public", ""));
				break;
			case PUBLIC:
				jo.put("$set", new JsonObject().put("public", true))
					.put("$unset", new JsonObject().put("protected", ""));
				break;
			default:
				handler.handle(null);
		}
		Bson qb = Filters.and(
                Filters.in("_id", documentIds),
                Filters.or(
                    Filters.eq("protected", true),
                    Filters.eq("public", true))
        );
		JsonObject query = MongoQueryBuilder.build(qb);
		mongo.update(DocumentDao.DOCUMENTS_COLLECTION, query, jo,false, true, (MongoDb.WriteConcern)null, res -> handler.handle(res));
	}

	@Override
	public Future<JsonArray> transferAll(
			final List<String> sourceIds,
			Optional<String> application,
			Visibility visibility,
			final UserInfos user
		) {
		if (sourceIds.isEmpty()) {
			return Future.succeededFuture(new JsonArray());
		}
		if( visibility!=Visibility.PROTECTED && visibility!=Visibility.PUBLIC ) {
			return Future.failedFuture("invalid.parameters");
		}

		final DocumentQueryBuilder querySourceDocuments = queryHelper.queryBuilder()
			.filterByInheritShareAndOwner(user)
			.withExcludeDeleted()
			.withFileType(FILE_TYPE)
			.withId(sourceIds);

		// Find matching documents for queried ids.
		final Future<JsonArray> results = queryHelper.findAllAsList(querySourceDocuments)
		.map(docs -> {
			// Index documents by id, and determine any needed visibility change.
			final Map<String, JsonObject> index = new HashMap<String, JsonObject>(docs.size());
			docs.stream().forEach( doc -> {
				// Check if this doc must me copied, changed or preserved.
				final boolean isProtected = Boolean.TRUE.equals(doc.getBoolean("protected"));
				final boolean isPublic = Boolean.TRUE.equals(doc.getBoolean("public"));
				if( !isProtected && !isPublic ) {
					// We must copy it
					doc.put("application", application.isPresent() ? application.get() : WorkspaceController.MEDIALIB_APP);
					// => set a temporary action
					doc.put("visibility_action", "copy");
					// Set the desired visibility for the copy.
					if( visibility==Visibility.PUBLIC ) {
						doc.put("public", true);
					} else {
						DocumentHelper.setProtected(doc, true);
					}
				} else if( (isProtected && visibility==Visibility.PUBLIC)
						|| (isPublic && visibility==Visibility.PROTECTED)) {
					// We must change its visibility => set a temporary action.
					doc.put("visibility_action", "change");
					// Set the desired visibility.
					if( visibility==Visibility.PUBLIC ) {
						doc.put("public", true);
						doc.remove("protected");
					} else {
						DocumentHelper.setProtected(doc, true);
						doc.remove("public");
					}
				} else {
					// We must do nothing => set a temporary action.
					doc.put("visibility_action", "preserve");
				}
				index.put(DocumentHelper.getId(doc), doc);
			});
			return index;
		})
		.compose( docsMap -> {
			// Split docs in two lists, depending on the required action.
			final List<JsonObject> docsToCopy = new ArrayList<JsonObject>();
			final JsonArray docIdsToChange = new JsonArray();
			// Preserve ids order. Any doc not found will be null in `docs` list.
			final List<JsonObject> docs = sourceIds.stream().map( (id) -> {
				final JsonObject doc = docsMap.get(id);
				if( doc != null ) {
					// Remove the temporary action.
					final String action = (String) doc.remove("visibility_action");
					switch( action ) {
						case "copy": docsToCopy.add(doc); break;
						case "change": docIdsToChange.add(DocumentHelper.getId(doc)); break;
						default: break;
					}
				}
				return doc;
			}).collect(Collectors.toList());

			// Copy private documents
			final Future<List<JsonObject>> copyFuture = (docsToCopy.size() > 0)
				? folderManager.copyAllNoFail(Optional.of(user), docsToCopy, true)
				: Future.succeededFuture(Collections.emptyList());

			// Change visibility
			final Promise<Message<JsonObject>> changePromise = Promise.promise();
			if( docIdsToChange.size() > 0 ) {
				changeVisibility( docIdsToChange, visibility.getCode(), new Handler<Message<JsonObject>>() {
					public void handle(Message<JsonObject> event) {
						changePromise.complete();
					}
				});
			} else {
				changePromise.complete();
			}

			// Resolve actions.
			return CompositeFuture.join(copyFuture, changePromise.future())
			.map( unused -> copyFuture.result() )
			.map( copies -> {
				// Replace original docs by their copy (maybe null).
				if( copies.size() == docsToCopy.size() ) {
					for( int i=0; i<docsToCopy.size(); i++) {
						final JsonObject source = docsToCopy.get(i);
						final JsonObject copy = copies.get(i);
						for( int j=0; j<docs.size(); j++) {
							final JsonObject original = docs.get(j);
							if( original!=null
								&& source!=null
								&& DocumentHelper.getId(original).equals(DocumentHelper.getId(source))
								) {
									docs.set(j, copy);
									break;
							}
						}
					}
				}
				return copies;
			})
			.recover( t -> null )
			.map( unused -> docs );
		})
		.map( docs -> new JsonArray(docs) );

		return results;
	}

	@Override
	public void getParentInfos(String childId, UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
		final ElementQuery queryChild = new ElementQuery(true);
		queryChild.setId(childId);
		super.findByQuery(queryChild, user, resChild -> {
			final JsonArray resultChild = resChild.result();
			if (resChild.succeeded()) {
				if (resultChild.size() != 1 || !(resultChild.getValue(0) instanceof  JsonObject)) {
					handler.handle(new DefaultAsyncResult<>(new Exception("workspace.getparent.childnotfound")));
					return;
				}
				final String eParent = DocumentHelper.getParent(resultChild.getJsonObject(0));
				if(StringUtils.isEmpty(eParent)){
					handler.handle(new DefaultAsyncResult<>(new Exception("workspace.getparent.notfound")));
					return;
				}
				final ElementQuery queryParent = new ElementQuery(false);
				queryParent.setId(eParent);
				queryParent.setProjection(ElementQuery.defaultProjection());
				super.findByQuery(queryParent, null, resParent ->{
					if (resParent.succeeded()) {
						final JsonArray resultParent = resParent.result();
						if (resultParent.size() != 1 || !(resultParent.getValue(0) instanceof  JsonObject)) {
							handler.handle(new DefaultAsyncResult<>(new Exception("workspace.getparent.notfound")));
							return;
						}
						final JsonObject parent = resultParent.getJsonObject(0);
						shareService.inheritShareInfos(user.getUserId(), eParent,null,null,resShare->{
							if(resShare.isLeft()){
								handler.handle(new DefaultAsyncResult<>(new Exception(resShare.left().getValue())));
								return;
							}
							final JsonObject shares = resShare.right().getValue();
							final JsonArray visiblesGroups = shares.getJsonObject("groups", new JsonObject()).getJsonArray("visibles");
							final JsonArray visiblesUsers = shares.getJsonObject("users", new JsonObject()).getJsonArray("visibles");
							parent.put("visibleGroups", visiblesGroups);
							parent.put("visibleUsers", visiblesUsers);
							handler.handle(new DefaultAsyncResult<>(parent));
						});
					} else {
						handler.handle(new DefaultAsyncResult<>(resChild.cause()));
					}
				});
			} else {
				handler.handle(new DefaultAsyncResult<>(resChild.cause()));
			}
		});
	}
}
