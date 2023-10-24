/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.workspace.dao;

import java.util.Optional;
import java.util.Set;

import com.mongodb.client.model.Filters;
import io.vertx.core.Promise;
import org.bson.conversions.Bson;
import org.entcore.common.mongodb.MongoDbResult;


import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class RevisionDao extends GenericDao {
	private static final Logger log = LoggerFactory.getLogger(RevisionDao.class);

	public static final String DOCUMENT_REVISION_COLLECTION = "documentsRevisions";

	public RevisionDao(MongoDb mongo) {
		super(mongo, DOCUMENT_REVISION_COLLECTION);
	}

	public Future<Boolean> isLastRevision(String documentId, String revisionId) {
		return this.getLastRevision(documentId).map(res -> {
			return res.isPresent() && res.get().getString("_id", "").equals(revisionId);
		});
	}

	public Future<Optional<JsonObject>> getLastRevision(String documentId) {
		Promise<Optional<JsonObject>> future = Promise.promise();
		JsonObject mongoSorts = new JsonObject().put("date", -1);
		final Bson builder = Filters.eq("documentId", documentId);
		mongo.find(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder), mongoSorts, null, 0, 2,
				Integer.MAX_VALUE, MongoDbResult.validResultsHandler(res -> {
					if (res.isLeft()) {
						log.error("[Workspace] Error finding last revision for doc " + documentId + " - "
								+ res.left().getValue());
						future.fail(res.left().getValue());
					} else {
						JsonArray all = res.right().getValue();
						JsonObject first = !all.isEmpty() ? all.getJsonObject(0) : null;
						future.complete(Optional.ofNullable(first));
					}
				}));
		return future.future();
	}

	public Future<JsonArray> getLastRevision(String documentId, int count) {
		Promise<JsonArray> future = Promise.promise();
		JsonObject mongoSorts = new JsonObject().put("date", -1);
		final Bson builder = Filters.eq("documentId", documentId);
		mongo.find(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder), mongoSorts, null, 0, count,
				Integer.MAX_VALUE, MongoDbResult.validResultsHandler(res -> {
					if (res.isLeft()) {
						log.error("[Workspace] Error finding last revision for doc " + documentId + " - "
								+ res.left().getValue());
						future.fail(res.left().getValue());
					} else {
						JsonArray all = res.right().getValue();
						future.complete(all);
					}
				}));
		return future.future();
	}

	public Future<JsonObject> findByDocAndId(String documentId, String revisionId) {
		final Bson builder = Filters.and(Filters.eq("_id", revisionId), Filters.eq("documentId", documentId));
		Promise<JsonObject> future = Promise.promise();
		mongo.findOne(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder),
				MongoDbResult.validResultHandler(res -> {
					if (res.isLeft()) {
						log.error("[Workspace] Error finding revision storage file " + documentId + "/" + revisionId
								+ " - " + res.left().getValue());
						future.fail(res.left().getValue());
					} else {
						future.complete(res.right().getValue());
					}
				}));
		return future.future();
	}

	public Future<JsonObject> create(final String id, final String file, final String name, String ownerId,
			String userId, String userName, JsonObject metadata, JsonObject thumbnails) {
		JsonObject document = new JsonObject();
		document.put("documentId", id).put("file", file).put("name", name).put("owner", ownerId).put("userId", userId)
				.put("userName", userName).put("date", MongoDb.now()).put("metadata", metadata)
				.put("thumbnails", thumbnails);
		Promise<JsonObject> future = Promise.promise();
		mongo.save(DOCUMENT_REVISION_COLLECTION, document, MongoDbResult.validResultHandler(res -> {
			if (res.isLeft()) {
				future.fail(res.left().getValue());
			} else {
				future.complete(res.right().getValue());
			}
		}));
		return future.future();
	}

	public Future<JsonObject> deleteByDocAndId(String documentId, String revisionId) {
		Promise<JsonObject> future = Promise.promise();
		final Bson builder = Filters.and(Filters.eq("_id", revisionId), Filters.eq("documentId", documentId));
		mongo.delete(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder),
				MongoDbResult.validResultHandler(new Handler<Either<String, JsonObject>>() {
					public void handle(Either<String, JsonObject> event) {
						if (event.isLeft()) {
							log.error("[Workspace] Error deleting revision " + revisionId + " - "
									+ event.left().getValue());
							future.fail(event.left().getValue());
						} else {
							future.complete(event.right().getValue());
						}
					}
				}));
		return future.future();
	}

	public Future<JsonArray> findByDoc(String documentId) {
		final Bson builder = Filters.eq("documentId", documentId);
		Promise<JsonArray> future = Promise.promise();
		mongo.find(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder),
				MongoDbResult.validResultsHandler(res -> {
					if (res.isLeft()) {
						log.error("[Workspace] Error finding revision for doc " + documentId + " - "
								+ res.left().getValue());
						future.fail(res.left().getValue());
					} else {
						future.complete(res.right().getValue());
					}
				}));
		return future.future();
	}

	public Future<JsonArray> findByDocs(Set<String> documentId) {
		final Bson builder = Filters.in("documentId", documentId);
		Promise<JsonArray> future = Promise.promise();
		mongo.find(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder),
				MongoDbResult.validResultsHandler(res -> {
					if (res.isLeft()) {
						log.error("[Workspace] Error finding revision for doc " + documentId + " - "
								+ res.left().getValue());
						future.fail(res.left().getValue());
					} else {
						future.complete(res.right().getValue());
					}
				}));
		return future.future();
	}

	public Future<JsonObject> deleteByDoc(String documentId) {
		Promise<JsonObject> future = Promise.promise();
		final Bson builder = Filters.eq("documentId", documentId);
		mongo.delete(DOCUMENT_REVISION_COLLECTION, MongoQueryBuilder.build(builder),
				MongoDbResult.validResultHandler(new Handler<Either<String, JsonObject>>() {
					public void handle(Either<String, JsonObject> event) {
						if (event.isLeft()) {
							log.error("[Workspace] Error deleting revision for doc" + documentId + " - "
									+ event.left().getValue());
							future.fail(event.left().getValue());
						} else {
							future.complete(event.right().getValue());
						}
					}
				}));
		return future.future();
	}
}
