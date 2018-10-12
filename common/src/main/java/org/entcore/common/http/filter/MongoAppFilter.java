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

package org.entcore.common.http.filter;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class MongoAppFilter extends BaseResourceProvider {

	protected String collection;
	protected String resourceIdLabel;
	protected final MongoDb mongo;

	public MongoAppFilter(String collection) {
		this(collection, "id");
	}

	public MongoAppFilter(String collection, String resourceIdLabel) {
		super();
		this.collection = collection;
		this.resourceIdLabel = resourceIdLabel;
		this.mongo = MongoDb.getInstance();
	}

	@Override
	protected String defaultFilter() {
		return "sharedAndOwner";
	}

	public void sharedAndOwner(HttpServerRequest request, String sharedMethod,
			UserInfos user, Handler<Boolean> handler) {
		String id = request.params().get(resourceIdLabel);
		if (id != null && !id.trim().isEmpty()) {
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(user.getUserId())
					.put(sharedMethod).is(true).get());
			for (String gpId: user.getGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId)
						.put(sharedMethod).is(true).get());
			}
			QueryBuilder query = QueryBuilder.start("_id").is(id).or(
					QueryBuilder.start("owner.userId").is(user.getUserId()).get(),
					QueryBuilder.start("shared").elemMatch(
							new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()).get()
			);
			executeCountQuery(request, collection, MongoQueryBuilder.build(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

	public void ownerOnly(HttpServerRequest request, String sharedMethod,
			UserInfos user, Handler<Boolean> handler) {
		String id = request.params().get(resourceIdLabel);
		if (id != null && !id.trim().isEmpty()) {
			QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner.userId").is(user.getUserId());
			executeCountQuery(request, collection, MongoQueryBuilder.build(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

	public static void executeCountQuery(final HttpServerRequest request, String collection,
			JsonObject query, final int expectedCountResult, final Handler<Boolean> handler) {
		request.pause();
		MongoDb mongo = MongoDb.getInstance();
		mongo.count(collection, query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				request.resume();
				JsonObject res = event.body();
				handler.handle(
						res != null &&
								"ok".equals(res.getString("status")) &&
								expectedCountResult == res.getInteger("count")
				);
			}
		});
	}

}
