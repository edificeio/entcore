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
import io.vertx.core.Promise;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DeleteOrphan implements Handler<Long> {

	private static final Logger log = LoggerFactory.getLogger(DeleteOrphan.class);

	private static final String STATUS_OK = "ok";
	private static final String STATUS_FIELD = "status";
	private static final String MESSAGE_FIELD = "message";
	private static final String UNKNOWN_ERROR = "Unknown error";

	private static final String SELECT_ORPHAN_ATTACHMENT =
		"SELECT a.id AS orphanid FROM conversation.attachments a " +
		"WHERE NOT EXISTS (SELECT 1 FROM conversation.usermessagesattachments uma WHERE uma.attachment_id = a.id) " +
		"LIMIT ?;";

	private static final String DELETE_ORPHAN_MESSAGE =
		"DELETE FROM conversation.messages WHERE id IN " +
		"(SELECT id FROM conversation.messages m " +
		"WHERE NOT EXISTS (SELECT 1 FROM conversation.usermessages um WHERE um.message_id = m.id) " +
		"LIMIT ?);";

	private static final String DELETE_ORPHAN_THREAD =
		"WITH to_delete AS " +
		"(SELECT t.id FROM conversation.threads t " +
		"WHERE NOT EXISTS (SELECT 1 FROM conversation.userthreads ut WHERE ut.thread_id = t.id) " +
		"ORDER BY t.date LIMIT ?) " +
		"DELETE FROM conversation.threads th USING to_delete WHERE th.id = to_delete.id;";

	private static final String DELETE_ORPHAN_ATTACHMENT_BATCH =
		"DELETE FROM conversation.attachments WHERE id = ANY(?::text[]);";

	private final long sqlTimeout;
	private final int batchSize;
	private final int maxBatches;

	private final Storage storage;

	public DeleteOrphan(Storage storage) {
		this.storage = storage;

		JsonObject orphanConfig = Vertx.currentContext().config().getJsonObject("orphans-killer", new JsonObject());
		sqlTimeout = orphanConfig.getLong("sql-timeout", 600000L);
		batchSize = orphanConfig.getInteger("batch-size", 100);
		maxBatches = orphanConfig.getInteger("max-batches", 10);
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
		return deleteInBatchesSequential(DELETE_ORPHAN_MESSAGE, "messages", 0, 0);
	}

	private Future<Void> deleteOrphanThreads() {
		log.info("Starting orphan threads cleanup");
		return deleteInBatchesSequential(DELETE_ORPHAN_THREAD, "threads", 0, 0);
	}

	private Future<Void> deleteInBatchesSequential(String query, String entityType, int batchNumber, int totalDeleted) {
		if (batchNumber >= maxBatches) {
			if (totalDeleted > 0) {
				log.info("Successfully deleted " + totalDeleted + " orphan " + entityType);
			}
			return Future.succeededFuture();
		}

		final Sql sql = Sql.getInstance();
		Promise<Void> promise = Promise.promise();

		sql.prepared(query, new JsonArray().add(batchSize),
			new DeliveryOptions().setSendTimeout(sqlTimeout),
			result -> {
				if (STATUS_OK.equals(result.body().getString(STATUS_FIELD))) {
					int deletedCount = result.body().getInteger("rows", 0);
					if (deletedCount > 0) {
						log.info("Batch " + batchNumber + " deleted " + deletedCount + " orphan " + entityType);
					}

					// Stop if no more orphans found
					if (deletedCount == 0) {
						promise.complete();
					} else {
						// Continue with next batch
						deleteInBatchesSequential(query, entityType, batchNumber + 1, totalDeleted + deletedCount)
							.onComplete(promise);
					}
				} else {
					String error = result.body().getString(MESSAGE_FIELD, UNKNOWN_ERROR);
					log.error("Batch " + batchNumber + " failed for orphan " + entityType + ": " + error);

					// Continue with next batch despite error
					deleteInBatchesSequential(query, entityType, batchNumber + 1, totalDeleted)
						.onComplete(promise);
				}
			});

		return promise.future();
	}

	private Future<Void> deleteOrphanAttachments() {
		log.info("Starting orphan attachments cleanup");
		return processOrphanAttachmentsSequential(0, 0);
	}

	private Future<Void> processOrphanAttachmentsSequential(int batchNumber, int totalProcessed) {
		if (batchNumber >= maxBatches) {
			if (totalProcessed > 0) {
				log.info("Successfully processed " + totalProcessed + " orphan attachments");
			}
			return Future.succeededFuture();
		}

		final Sql sql = Sql.getInstance();
		Promise<Void> promise = Promise.promise();

		sql.prepared(SELECT_ORPHAN_ATTACHMENT, new JsonArray().add(batchSize),
			new DeliveryOptions().setSendTimeout(sqlTimeout),
			result -> {
				if (!STATUS_OK.equals(result.body().getString(STATUS_FIELD))) {
					String error = result.body().getString(MESSAGE_FIELD, UNKNOWN_ERROR);
					log.error("Batch " + batchNumber + " failed to select orphan attachments: " + error);

					// Continue with next batch despite error
					processOrphanAttachmentsSequential(batchNumber + 1, totalProcessed)
						.onComplete(promise);
					return;
				}

				JsonArray attachments = result.body().getJsonArray("results", new JsonArray());
				if (attachments.size() == 0) {
					log.info("No more orphan attachments found");
					promise.complete();
					return;
				}

				log.info("Processing " + attachments.size() + " orphan attachments in batch " + batchNumber);
				processAttachmentBatch(attachments)
					.onSuccess(v -> processOrphanAttachmentsSequential(batchNumber + 1, totalProcessed + attachments.size())
						.onComplete(promise)
					)
					.onFailure(err -> {
						log.error("Batch " + batchNumber + " failed to process attachments: " + err.getMessage());

						// Continue with next batch despite error
						processOrphanAttachmentsSequential(batchNumber + 1, totalProcessed)
							.onComplete(promise);
					});
			});

		return promise.future();
	}

	private Future<Void> processAttachmentBatch(JsonArray attachments) {
		List<String> attachmentIds = new ArrayList<>();
		List<Future<StorageResult>> storageDeletions = new ArrayList<>();

		for (Object attObj : attachments) {
			if (!(attObj instanceof JsonObject)) continue;
			JsonObject attachment = (JsonObject) attObj;
			String attachmentId = attachment.getString("orphanid");
			if (attachmentId != null) {
				attachmentIds.add(attachmentId);

				// Schedule storage deletion with result tracking
				Promise<StorageResult> storagePromise = Promise.promise();
				Future<StorageResult> storageDeletion = storagePromise.future();
				storage.removeFile(attachmentId, event -> {
					boolean success = STATUS_OK.equals(event.getString(STATUS_FIELD));
					if (!success) {
						log.error("Error deleting attachment file " + attachmentId + ": " + event.getString(MESSAGE_FIELD, UNKNOWN_ERROR));
					}
					storagePromise.complete(new StorageResult(success));
				});
				storageDeletions.add(storageDeletion);
			}
		}

		if (attachmentIds.isEmpty()) {
			return Future.succeededFuture();
		}

		// Wait for all storage deletions to complete and collect results
		Promise<List<StorageResult>> allResultsPromise = Promise.promise();
		collectAllStorageResults(storageDeletions, new ArrayList<>(), 0, allResultsPromise);

		// Process results and delete from database
		return allResultsPromise.future().compose(results -> {
			int successCount = 0;
			int failureCount = 0;

			for (StorageResult result : results) {
				if (result.success) {
					successCount++;
				} else {
					failureCount++;
				}
			}

			if (successCount > 0) {
				log.info("Successfully deleted " + successCount + " attachment files from storage");
			}
			if (failureCount > 0) {
				log.warn(failureCount + " attachment files failed to delete from storage");
			}

			// Delete from database regardless of storage results (orphan records are worse than orphan files)
			return deleteAttachmentRowsBatch(attachmentIds);
		});
	}

	private Future<Void> deleteAttachmentRowsBatch(List<String> attachmentIds) {
		if (attachmentIds.isEmpty()) {
			return Future.succeededFuture();
		}

		final Sql sql = Sql.getInstance();
		JsonArray idsArray = new JsonArray();
		attachmentIds.forEach(idsArray::add);

		Promise<Void> promise = Promise.promise();
		sql.prepared(DELETE_ORPHAN_ATTACHMENT_BATCH, new JsonArray().add(idsArray),
			new DeliveryOptions().setSendTimeout(sqlTimeout),
			result -> {
				if (STATUS_OK.equals(result.body().getString(STATUS_FIELD))) {
					int deleted = result.body().getInteger("rows", 0);
					log.info("Deleted " + deleted + " orphan attachment records");
					promise.complete();
				} else {
					String error = result.body().getString(MESSAGE_FIELD, UNKNOWN_ERROR);
					log.error("Error deleting orphan attachment records: " + error);
					promise.fail(error);
				}
			});
		return promise.future();
	}

	private void collectAllStorageResults(List<Future<StorageResult>> futures, List<StorageResult> results, int index, Promise<List<StorageResult>> promise) {
		if (index >= futures.size()) {
			promise.complete(results);
			return;
		}

		futures.get(index).onComplete(ar -> {
			if (ar.succeeded()) {
				results.add(ar.result());
			} else {
				results.add(new StorageResult(false));
			}
			collectAllStorageResults(futures, results, index + 1, promise);
		});
	}

	private Future<Void> logRemainingOrphans() {
		final Sql sql = Sql.getInstance();

		String countMessages =
			"SELECT COUNT(*) AS count FROM (SELECT 1 FROM conversation.messages m " +
			"WHERE NOT EXISTS (SELECT 1 FROM conversation.usermessages um WHERE um.message_id = m.id) " +
			"LIMIT 100000) subquery";

		String countThreads =
			"SELECT COUNT(*) AS count FROM (SELECT 1 FROM conversation.threads t " +
			"WHERE NOT EXISTS (SELECT 1 FROM conversation.userthreads ut WHERE ut.thread_id = t.id) " +
			"LIMIT 100000) subquery";

		String countAttachments =
			"SELECT COUNT(*) AS count FROM (SELECT 1 FROM conversation.attachments a " +
			"WHERE NOT EXISTS (SELECT 1 FROM conversation.usermessagesattachments uma WHERE uma.attachment_id = a.id) " +
			"LIMIT 100000) subquery";

		AtomicInteger remainingMessages = new AtomicInteger(0);
		AtomicInteger remainingThreads = new AtomicInteger(0);
		AtomicInteger remainingAttachments = new AtomicInteger(0);

		Promise<Void> promise = Promise.promise();
		sql.prepared(countMessages, new JsonArray(), result1 -> {
			if (!STATUS_OK.equals(result1.body().getString(STATUS_FIELD))) {
				// Don't fail for logging issues
				promise.complete();
				return;
			}

			JsonArray results1 = result1.body().getJsonArray("results", new JsonArray());
			if (results1.size() > 0 && results1.getValue(0) instanceof JsonObject) {
				remainingMessages.set(results1.getJsonObject(0).getInteger("count", 0));
			}

			sql.prepared(countThreads, new JsonArray(), result2 -> {
				if (!STATUS_OK.equals(result2.body().getString(STATUS_FIELD))) {
					promise.complete();
					return;
				}

				JsonArray results2 = result2.body().getJsonArray("results", new JsonArray());
				if (results2.size() > 0 && results2.getValue(0) instanceof JsonObject) {
					remainingThreads.set(results2.getJsonObject(0).getInteger("count", 0));
				}

				sql.prepared(countAttachments, new JsonArray(), result3 -> {
					if (!STATUS_OK.equals(result3.body().getString(STATUS_FIELD))) {
						promise.complete();
						return;
					}

					JsonArray results3 = result3.body().getJsonArray("results", new JsonArray());
					if (results3.size() > 0 && results3.getValue(0) instanceof JsonObject) {
						remainingAttachments.set(results3.getJsonObject(0).getInteger("count", 0));
					}
					int totalRemaining = remainingMessages.get() + remainingThreads.get() + remainingAttachments.get();
					if (totalRemaining > 0) {
						log.info("Orphan cleanup completed. Remaining orphans: " + remainingMessages.get() + " messages, " + remainingThreads.get() + " threads, " + remainingAttachments.get() + " attachments (total: " + totalRemaining + ")");
					} else {
						log.info("Orphan cleanup completed. No orphans remaining.");
					}

					promise.complete();
				});
			});
		});
		return promise.future();
	}

	private static class StorageResult {
		final boolean success;

		StorageResult(boolean success) {
			this.success = success;
		}
	}

}
