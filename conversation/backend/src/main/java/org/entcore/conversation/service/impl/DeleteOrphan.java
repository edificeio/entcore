/*
 * Copyright © "Open Digital Education", 2017
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.conversation.service.impl;

import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.common.sql.Sql;
import org.entcore.common.storage.Storage;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.CompositeFuture;
import java.util.ArrayList;
import java.util.List;

public class DeleteOrphan implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(DeleteOrphan.class);

	private final int batchSize;
	private final int maxConcurrentOperations;
	private final int maxBatches;
	
	private static final String STATUS_OK = "ok";
	private static final String STATUS_FIELD = "status";
	private static final String MESSAGE_FIELD = "message";
	private static final String UNKNOWN_ERROR = "Unknown error";

	private static final String SELECT_ORPHAN_ATTACHMENT =
			"select a.id as orphanid from conversation.attachments a " +
			"left join conversation.usermessagesattachments uma on uma.attachment_id = a.id " +
			"where uma.message_id is NULL LIMIT ?;";

	private static final String DELETE_ORPHAN_MESSAGE =
			"delete from conversation.messages where id IN " +
			"(select m.id from conversation.messages m " +
			"left join conversation.usermessages um on um.message_id = m.id " +
			"where um.user_id is NULL LIMIT ?);";

	private static final String DELETE_ORPHAN_THREAD =
			"delete from conversation.threads where id IN " +
			"(select t.id from conversation.threads t " +
			"left join conversation.userthreads ut on ut.thread_id = t.id " +
			"where ut.user_id is NULL LIMIT ?);";

	private static final String DELETE_ORPHAN_ATTACHMENT_BATCH =
			"delete from conversation.attachments where id = ANY(?::uuid[]);";

	private final long timeout;

	private final Storage storage;

	public DeleteOrphan(Storage storage) {
		this.storage = storage;

		JsonObject orphanConfig = Vertx.currentContext().config().getJsonObject("orphans-cleaner", new JsonObject());
		timeout = orphanConfig.getLong("timeout", 600000L);
		batchSize = orphanConfig.getInteger("batch-size", 100);
		maxBatches = orphanConfig.getInteger("max-batches", 10);
		maxConcurrentOperations = orphanConfig.getInteger("max-concurrent-operations", 5);
	}

	@Override
	public void handle(Long event) {
		log.info("Starting orphan cleanup process with batch processing");
		deleteOrphanEntitiesInBatches()
			.onSuccess(v -> log.info("Orphan cleanup process completed successfully"))
			.onFailure(err -> log.error("Orphan cleanup process failed: " + err.getMessage(), err));
	}

	private Future<Void> deleteOrphanEntitiesInBatches() {
		return deleteOrphanMessages()
			.compose(v -> deleteOrphanThreads())
			.compose(v -> deleteOrphanAttachments())
			.compose(v -> logRemainingOrphans());
	}

	

	private Future<Void> deleteOrphanMessages() {
		log.info("Starting orphan messages cleanup");
		return deleteInBatches(DELETE_ORPHAN_MESSAGE, "messages");
	}

	private Future<Void> deleteOrphanThreads() {
		log.info("Starting orphan threads cleanup");
		return deleteInBatches(DELETE_ORPHAN_THREAD, "threads");
	}

	private Future<Void> deleteInBatches(String query, String entityType) {
		final Sql sql = Sql.getInstance();

		return Future.succeededFuture()
			.compose(v -> {
				List<Future<Integer>> batchFutures = new ArrayList<>();

				for (int i = 0; i < maxBatches; i++) {
					Future<Integer> batchFuture = Future.future(promise -> {
						sql.prepared(query, new JsonArray().add(batchSize),
							new DeliveryOptions().setSendTimeout(timeout / 4),
							result -> {
								if (STATUS_OK.equals(result.body().getString(STATUS_FIELD))) {
									int deletedCount = result.body().getInteger("rows", 0);
									if (deletedCount > 0) {
										log.info("Deleted {} orphan {}", deletedCount, entityType);
									}
									promise.complete(deletedCount);
								} else {
									String error = result.body().getString(MESSAGE_FIELD, UNKNOWN_ERROR);
									log.error("Error deleting orphan {}: {}", entityType, error);
									promise.fail(error);
								}
							});
					});
					batchFutures.add(batchFuture);

					// Stop if previous batch deleted nothing
					if (i > 0) {
						Future<Integer> currentBatchFuture = batchFuture;
						batchFuture = batchFutures.get(i-1).compose(prevCount -> {
							if (prevCount == 0) {
								return Future.succeededFuture(0);
							}
							return currentBatchFuture;
						});
						batchFutures.set(i, batchFuture);
					}
				}

				return CompositeFuture.all(new ArrayList<>(batchFutures))
					.map(cf -> {
						int totalDeleted = cf.list().stream()
							.mapToInt(count -> (Integer) count)
							.sum();
						if (totalDeleted > 0) {
							log.info("Successfully deleted {} orphan {}", totalDeleted, entityType);
						}
						return null;
					});
			});
	}

	private Future<Void> deleteOrphanAttachments() {
		log.info("Starting orphan attachments cleanup");
		return processOrphanAttachmentsBatch(0);
	}

	private Future<Void> processOrphanAttachmentsBatch(int batchNumber) {
		if (batchNumber >= maxBatches) {
			return Future.succeededFuture();
		}

		final Sql sql = Sql.getInstance();

		return Future.future(promise -> {
			sql.prepared(SELECT_ORPHAN_ATTACHMENT, new JsonArray().add(batchSize),
				new DeliveryOptions().setSendTimeout(timeout / 4),
				result -> {
					if (!STATUS_OK.equals(result.body().getString(STATUS_FIELD))) {
						String error = result.body().getString(MESSAGE_FIELD, UNKNOWN_ERROR);
						log.error("Error selecting orphan attachments: {}", error);
						promise.fail(error);
						return;
					}

					JsonArray attachments = result.body().getJsonArray("results", new JsonArray());
					if (attachments.size() == 0) {
						log.info("No more orphan attachments found");
						promise.complete();
						return;
					}

					log.info("Processing {} orphan attachments in batch {}", attachments.size(), batchNumber);
					processAttachmentBatch(attachments)
						.onSuccess(v -> processOrphanAttachmentsBatch(batchNumber + 1)
							.onComplete(promise)
						)
						.onFailure(promise::fail);
				});
		});
	}

	private Future<Void> processAttachmentBatch(JsonArray attachments) {
		List<String> attachmentIds = new ArrayList<>();
		List<Future<Void>> storageDeletions = new ArrayList<>();

		for (Object attObj : attachments) {
			if (!(attObj instanceof JsonObject)) continue;
			JsonObject attachment = (JsonObject) attObj;
			String attachmentId = attachment.getString("orphanid");
			if (attachmentId != null) {
				attachmentIds.add(attachmentId);

				// Schedule storage deletion
				Future<Void> storageDeletion = Future.future(promise ->
					storage.removeFile(attachmentId, event -> {
						if (!STATUS_OK.equals(event.getString(STATUS_FIELD))) {
							log.error("Error deleting attachment file {}: {}", attachmentId, event.getString(MESSAGE_FIELD, UNKNOWN_ERROR));
						}

						// Don't fail the whole batch for storage errors
						promise.complete();
					}));
				storageDeletions.add(storageDeletion);
			}
		}

		if (attachmentIds.isEmpty()) {
			return Future.succeededFuture();
		}

		// Wait for storage deletions to complete (with limited concurrency)
		List<List<Future<Void>>> batches = partition(storageDeletions, maxConcurrentOperations);
		Future<Void> allStorageDeletions = Future.succeededFuture();

		for (List<Future<Void>> batch : batches) {
			allStorageDeletions = allStorageDeletions.compose(v -> CompositeFuture.join(new ArrayList<>(batch)).mapEmpty());
		}

		// Delete from database after storage cleanup attempts
		return allStorageDeletions.compose(v -> deleteAttachmentRowsBatch(attachmentIds));
	}

	private Future<Void> deleteAttachmentRowsBatch(List<String> attachmentIds) {
		if (attachmentIds.isEmpty()) {
			return Future.succeededFuture();
		}

		final Sql sql = Sql.getInstance();
		
		JsonArray idsArray = new JsonArray();
		attachmentIds.forEach(idsArray::add);

		return Future.future(promise ->
			sql.prepared(DELETE_ORPHAN_ATTACHMENT_BATCH, new JsonArray().add(idsArray),
				new DeliveryOptions().setSendTimeout(timeout / 4),
				result -> {
					if (STATUS_OK.equals(result.body().getString(STATUS_FIELD))) {
						int deleted = result.body().getInteger("rows", 0);
						log.info("Deleted {} orphan attachment records", deleted);
						promise.complete();
					} else {
						String error = result.body().getString(MESSAGE_FIELD, UNKNOWN_ERROR);
						log.error("Error deleting orphan attachment records: {}", error);
						promise.fail(error);
					}
				}));
	}

	private <T> List<List<T>> partition(List<T> list, int size) {
		List<List<T>> partitions = new ArrayList<>();
		for (int i = 0; i < list.size(); i += size) {
			partitions.add(list.subList(i, Math.min(i + size, list.size())));
		}
		return partitions;
	}

	private Future<Void> logRemainingOrphans() {
		final Sql sql = Sql.getInstance();

		String countMessages = "select count(*) as count from conversation.messages m " +
			"left join conversation.usermessages um on um.message_id = m.id " +
			"where um.user_id is NULL";

		String countThreads = "select count(*) as count from conversation.threads t " +
			"left join conversation.userthreads ut on ut.thread_id = t.id " +
			"where ut.user_id is NULL";

		String countAttachments = "select count(*) as count from conversation.attachments a " +
			"left join conversation.usermessagesattachments uma on uma.attachment_id = a.id " +
			"where uma.message_id is NULL";

		return Future.future(promise -> {
			sql.prepared(countMessages, new JsonArray(), result1 -> {
				if (!STATUS_OK.equals(result1.body().getString(STATUS_FIELD))) {
					// Don't fail for logging issues
					promise.complete();
					return;
				}

				JsonArray results1 = result1.body().getJsonArray("results", new JsonArray());
				int remainingMessages = results1.size() > 0 ? results1.getJsonObject(0).getInteger("count", 0) : 0;

				sql.prepared(countThreads, new JsonArray(), result2 -> {
					if (!STATUS_OK.equals(result2.body().getString(STATUS_FIELD))) {
						promise.complete();
						return;
					}

					JsonArray results2 = result2.body().getJsonArray("results", new JsonArray());
					int remainingThreads = results2.size() > 0 ? results2.getJsonObject(0).getInteger("count", 0) : 0;

					sql.prepared(countAttachments, new JsonArray(), result3 -> {
						if (!STATUS_OK.equals(result3.body().getString(STATUS_FIELD))) {
							promise.complete();
							return;
						}

						JsonArray results3 = result3.body().getJsonArray("results", new JsonArray());
						int remainingAttachments = results3.size() > 0 ? results3.getJsonObject(0).getInteger("count", 0) : 0;
						int totalRemaining = remainingMessages + remainingThreads + remainingAttachments;
						if (totalRemaining > 0) {
							log.info("Orphan cleanup completed. Remaining orphans: {} messages, {} threads, {} attachments (total: {})",
								remainingMessages, remainingThreads, remainingAttachments, totalRemaining);
						} else {
							log.info("Orphan cleanup completed. No orphans remaining.");
						}

						promise.complete();
					});
				});
			});
		});
	}

}
