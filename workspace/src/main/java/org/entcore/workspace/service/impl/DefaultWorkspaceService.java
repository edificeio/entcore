package org.entcore.workspace.service.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.impl.DocumentHelper;
import org.entcore.common.folders.impl.FolderManagerWithQuota;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
			QuotaService quotaService, FolderManager folderManager, EventBus eb, ShareService share) {
		super(DocumentDao.DOCUMENTS_COLLECTION, threshold, quotaService, folderManager, eb);
		this.dao = new DocumentDao(mongo);
		this.storage = storage;
		this.mongo = mongo;
		this.threshold = threshold;
		this.shareService = share;
		this.imageResizerAddress = imageResizerAddress;
		this.eb = eb;
		this.revisionDao = new RevisionDao(mongo);
		this.documentDao = new DocumentDao(mongo);
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
			final List<String> thumbnail, final JsonObject doc, final JsonObject uploaded,
			final Handler<AsyncResult<JsonObject>> handler) {
		compressImage(uploaded, quality, new Handler<Integer>() {
			@Override
			public void handle(Integer size) {
				JsonObject meta = uploaded.getJsonObject("metadata");
				if (size != null && meta != null) {
					meta.put("size", size);
				}
				addAfterUpload(uploaded, doc, name, application, thumbnail, user.getUserId(), user.getUsername(),
						handler);
			}
		});

	}

	public void updateDocument(final String id, final float quality, final String name, final List<String> thumbnail,
			final JsonObject uploaded, UserInfos user, final Handler<Message<JsonObject>> handler) {
		compressImage(uploaded, quality, new Handler<Integer>() {
			@Override
			public void handle(Integer size) {
				JsonObject meta = uploaded.getJsonObject("metadata");
				if (size != null && meta != null) {
					meta.put("size", size);
				}
				updateAfterUpload(id, name, uploaded, thumbnail, user, handler);
			}
		});
	}

	public void documentProperties(final String id, final Handler<JsonObject> handler) {
		this.findById(id, PROPERTIES_KEYS, handler);
	}

	public void addComment(final String id, final String comment, final UserInfos user,
			final Handler<JsonObject> handler) {
		final String commentId = UUID.randomUUID().toString();
		JsonObject query = new JsonObject().put("$push",
				new JsonObject().put("comments",
						new JsonObject().put("id", commentId).put("author", user.getUserId())
								.put("authorName", user.getUsername()).put("posted", MongoDb.formatDate(new Date()))
								.put("comment", comment)));
		dao.update(id, query, handler);
	}

	public void deleteComment(final String id, final String commentId, final Handler<JsonObject> handler) {
		JsonObject query = new JsonObject().put("$pull",
				new JsonObject().put("comments", new JsonObject().put("id", commentId)));

		dao.update(id, query, handler);
	}

	private void compressImage(JsonObject srcFile, float quality, final Handler<Integer> handler) {
		if (!isImage(srcFile)) {
			handler.handle(null);
			return;
		}
		JsonObject json = new JsonObject().put("action", "compress").put("quality", quality)
				.put("src", storage.getProtocol() + "://" + storage.getBucket() + ":" + srcFile.getString("_id"))
				.put("dest", storage.getProtocol() + "://" + storage.getBucket() + ":" + srcFile.getString("_id"));
		eb.send(imageResizerAddress, json, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				Integer size = event.body().getInteger("size");
				handler.handle(size);
			}
		}));
	}

	private boolean isImage(JsonObject doc) {
		if (doc == null) {
			return false;
		}
		JsonObject metadata = doc.getJsonObject("metadata");
		return metadata != null && ("image/jpeg".equals(metadata.getString("content-type"))
				|| "image/gif".equals(metadata.getString("content-type"))
				|| "image/png".equals(metadata.getString("content-type"))
				|| "image/tiff".equals(metadata.getString("content-type")));
	}

	public void addAfterUpload(final JsonObject uploaded, final JsonObject doc, String name, String application,
			final List<String> thumbs, final String ownerId, final String ownerName,
			final Handler<AsyncResult<JsonObject>> handler) {
		doc.put("name", getOrElse(name, uploaded.getJsonObject("metadata").getString("filename"), false));
		doc.put("metadata", uploaded.getJsonObject("metadata"));
		doc.put("file", uploaded.getString("_id"));
		doc.put("application", getOrElse(application, WorkspaceController.MEDIALIB_APP));
		log.debug(doc.encodePrettily());
		addFile(Optional.ofNullable(doc.getString("eParent")), doc, ownerId, ownerName, res -> {
			if (res.succeeded()) {
				incrementStorage(doc);

				createThumbnailIfNeeded(DocumentDao.DOCUMENTS_COLLECTION, uploaded, res.result().getString("_id"), null,
						thumbs).map(resThumb -> {
							if (handler != null) {
								handler.handle(res);
							}
							JsonObject thumbnails = resThumb != null ? resThumb : new JsonObject();
							revisionDao.create(res.result().getString("_id"), uploaded.getString("_id"),
									doc.getString("name"), doc.getString("owner"), doc.getString("owner"),
									doc.getString("ownerName"), doc.getJsonObject("metadata"), thumbnails);
							return resThumb;
						});
			} else if (handler != null) {
				handler.handle(res);
			}
		});
	}

	public void updateAfterUpload(final String id, final String name, final JsonObject uploaded, final List<String> t,
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
					doc.put("alt", uploaded.getString("alt"));
					doc.put("legend", uploaded.getString("legend"));
					final JsonObject thumbs = old.getJsonObject("result", new JsonObject()).getJsonObject("thumbnails");

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
										createThumbnailIfNeeded(DocumentDao.DOCUMENTS_COLLECTION, uploaded, id, thumbs,
												t).map(resThumb -> {
													JsonObject thumbs = resThumb != null ? resThumb : new JsonObject();
													revisionDao.create(id, doc.getString("file"), doc.getString("name"),
															result.getString("owner"), userId, userName, metadata,
															thumbs);
													if (handler != null) {
														handler.handle(res);
													}
													return resThumb;
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
		final QueryBuilder builder = QueryBuilder.start("documentId").is(id);
		mongo.find(RevisionDao.DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder),
				MongoDbResult.validResultsHandler(handler));
	}

	public void getRevision(final String documentId, final String revisionId,
			final Handler<Either<String, JsonObject>> handler) {
		final QueryBuilder builder = QueryBuilder.start("_id").is(revisionId).and("documentId").is(documentId);
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
								new fr.wseduc.webutils.collections.JsonArray().add(DocumentHelper.getFileId(item)));
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
		Future<JsonArray> future = Future.future();
		JsonArray fileIdsJson = new JsonArray(new ArrayList<String>(fileIds));
		storage.removeFiles(fileIdsJson, event -> {
			future.complete(revisions);
		});
		return future;
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
				return documentDao.restaureFromRevision(documentId, previous).map(revision);
			} else {
				return Future.succeededFuture(revision);
			}
		}).setHandler(revision -> {
			if (revision.succeeded()) {
				handler.handle(new Either.Right<String, JsonObject>(revision.result()));
			} else {
				handler.handle(new Either.Left<String, JsonObject>(revision.cause().getMessage()));
			}
		});
	}

	private Future<JsonObject> createThumbnailIfNeeded(final String collection, final JsonObject srcFile,
			final String documentId, final JsonObject oldThumbnail, final List<String> thumbs) {
		Future<JsonObject> future = null;
		if (documentId != null && thumbs != null && !documentId.trim().isEmpty() && !thumbs.isEmpty() && srcFile != null
				&& isImage(srcFile) && srcFile.getString("_id") != null) {
			future = createThumbnails(thumbs, srcFile, collection, documentId);
		} else {
			future = Future.succeededFuture(null);
		}
		return future;
	}

	private Future<JsonObject> createThumbnails(List<String> thumbs, JsonObject srcFile, final String collection,
			final String documentId) {
		Future<JsonObject> future = Future.future();
		Pattern size = Pattern.compile("([0-9]+)x([0-9]+)");
		JsonArray outputs = new fr.wseduc.webutils.collections.JsonArray();
		for (String thumb : thumbs) {
			Matcher m = size.matcher(thumb);
			if (m.matches()) {
				try {
					int width = Integer.parseInt(m.group(1));
					int height = Integer.parseInt(m.group(2));
					if (width == 0 && height == 0)
						continue;
					JsonObject j = new JsonObject().put("dest", storage.getProtocol() + "://" + storage.getBucket());
					if (width != 0) {
						j.put("width", width);
					}
					if (height != 0) {
						j.put("height", height);
					}
					outputs.add(j);
				} catch (NumberFormatException e) {
					log.error("Invalid thumbnail size.", e);
				}
			}
		}
		if (outputs.size() > 0) {
			JsonObject json = new JsonObject().put("action", "resizeMultiple")
					.put("src", storage.getProtocol() + "://" + storage.getBucket() + ":" + srcFile.getString("_id"))
					.put("destinations", outputs);
			eb.send(imageResizerAddress, json, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonObject thumbnails = event.body().getJsonObject("outputs");
					if ("ok".equals(event.body().getString("status")) && thumbnails != null) {
						mongo.update(collection, new JsonObject().put("_id", documentId),
								new JsonObject().put("$set", new JsonObject().put("thumbnails", thumbnails)),
								MongoDbResult.validActionResultHandler(res -> {
									if (res.isLeft()) {
										future.fail(res.left().getValue());
									} else {
										future.complete(thumbnails);
									}
								}));
					}
				}
			}));
		} else {
			future.complete(null);
		}
		return future;
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
	public void copyUnsafe(String sourceId, Optional<String> destinationFolderId, String userId,
			Handler<AsyncResult<JsonArray>> handler) {
		super.copyUnsafe(sourceId, destinationFolderId, userId, afterCopy(userId, "", handler));
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
		updateStorage(new fr.wseduc.webutils.collections.JsonArray().add(added), null);
	}

	public void decrementStorage(JsonObject removed) {
		updateStorage(null, new fr.wseduc.webutils.collections.JsonArray().add(removed));
	}

	public void decrementStorage(JsonObject removed, Handler<Either<String, JsonObject>> handler) {
		updateStorage(null, new fr.wseduc.webutils.collections.JsonArray().add(removed), handler);
	}

	public void incrementStorage(JsonArray added) {
		updateStorage(added, null);
	}

	public void decrementStorage(JsonArray removed) {
		updateStorage(null, removed);
	}

	public void updateStorage(JsonObject added, JsonObject removed) {
		updateStorage(new fr.wseduc.webutils.collections.JsonArray().add(added), new JsonArray().add(removed));
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

}
