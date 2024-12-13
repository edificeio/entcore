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

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.bson.conversions.Bson;
import org.entcore.common.folders.impl.DocumentHelper;
import org.entcore.common.mongodb.MongoDbResult;

import java.util.Date;

public class DocumentDao extends GenericDao {

	public static final String DOCUMENTS_COLLECTION = "documents";

	public DocumentDao(MongoDb mongo) {
		super(mongo, DOCUMENTS_COLLECTION);
	}

	static JsonObject toJson(Bson queryBuilder) {
		return MongoQueryBuilder.build(queryBuilder);
	}

	static boolean isOk(JsonObject body) {
		return "ok".equals(body.getString("status"));
	}

	static String toErrorStr(JsonObject body) {
		return body.getString("error", body.getString("message", "documentdao error"));
	}

	public Future<JsonObject> findById(String id) {
		final Bson builder = Filters.eq("_id", id);
		Promise<JsonObject> future = Promise.promise();
		mongo.findOne(DOCUMENTS_COLLECTION, MongoQueryBuilder.build(builder), MongoDbResult.validResultHandler(res -> {
			if (res.isLeft()) {
				future.fail(res.left().getValue());
			} else {
				future.complete(res.right().getValue());
			}
		}));
		return future.future();
	}

	public Future<JsonObject> restaureFromRevision(String docId, JsonObject revision) {
		Promise<JsonObject> future = Promise.promise();
		String now = MongoDb.formatDate(new Date());
		String name = revision.getString("name", "");
		MongoUpdateBuilder set = new MongoUpdateBuilder().set("modified", now)//
				.set("name", name)//
				.set("owner", revision.getString("userId"))//
				.set("ownerName", revision.getString("userName"))//
				.set("file", revision.getString("file"))//
				.set("thumbnails", revision.getJsonObject("thumbnails"))//
				.set("metadata", revision.getJsonObject("metadata"))//
				.set("nameSearch", name != null ? DocumentHelper.prepareNameForSearch(name) : "").unset("previewDate");
		mongo.update(collection, toJson(Filters.eq("_id", docId)), set.build(), message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				JsonObject doc = new JsonObject()
				.put("_id", docId)//
				.put("name", name)//
				.put("owner", revision.getString("userId"))//
				.put("ownerName", revision.getString("userName"))//
				.put("file", revision.getString("file"))//
				.put("thumbnails", revision.getJsonObject("thumbnails"))//
				.put("metadata", revision.getJsonObject("metadata"))//
				.put("nameSearch", name != null ? DocumentHelper.prepareNameForSearch(name) : "");
				future.complete(doc);
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future.future();
	}
}
