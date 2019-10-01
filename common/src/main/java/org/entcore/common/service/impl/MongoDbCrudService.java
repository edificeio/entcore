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

package org.entcore.common.service.impl;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.service.CrudService;
import org.entcore.common.service.VisibilityFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.mongodb.MongoDbResult.*;


public class MongoDbCrudService implements CrudService {

	protected final MongoDb mongo;
	protected final String collection;
	protected final JsonObject defaultRetrieveProjection;
	protected final JsonObject defaultListProjection;
	protected final MongoDbConf mongoDbConf;
	private final String plainSuffixField = "Plain";
	private static final Logger log = LoggerFactory.getLogger(MongoDbCrudService.class);

	public MongoDbCrudService(String collection) {
		this(collection, null, null);
	}

	public MongoDbCrudService(String collection,
			JsonObject defaultRetrieveProjection, JsonObject defaultListProjection) {
		this.collection = collection;
		this.defaultRetrieveProjection = defaultRetrieveProjection;
		this.defaultListProjection = defaultListProjection;
		this.mongo = MongoDb.getInstance();
		this.mongoDbConf = MongoDbConf.getInstance();
	}

	@Override
	public void create(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler)
	{
		JsonObject now = MongoDb.now();
		data.put("created", now).put("modified", now);

		MongoDbCrudService.setUserMetadata(data, user.getUserId(), user.getUsername());

		addPlainField(data);
		mongo.save(collection, data, validActionResultHandler(handler));
	}

	public static void setUserMetadata(JsonObject data, String userId, String userName)
	{
		data.put(
			"owner",
			new JsonObject()
				.put("userId", userId)
				.put("displayName", userName)
		);
	}

	@Override
	public void retrieve(String id, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder builder = QueryBuilder.start("_id").is(id);
		mongo.findOne(collection, MongoQueryBuilder.build(builder),
				defaultRetrieveProjection, validResultHandler(handler));
	}

	@Override
	public void retrieve(String id, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder builder = QueryBuilder.start("_id").is(id);
		if (user == null) {
			builder.put("visibility").is(VisibilityFilter.PUBLIC.name());
		}
		mongo.findOne(collection, MongoQueryBuilder.build(builder),
				defaultRetrieveProjection,validResultHandler(handler));
	}

	@Override
	public void update(String id, JsonObject data, Handler<Either<String, JsonObject>> handler) {
		update(id, data, null, handler);
	}

	@Override
	public void update(String id, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder query = QueryBuilder.start("_id").is(id);
		addPlainField(data);
		MongoUpdateBuilder modifier = new MongoUpdateBuilder();
		for (String attr: data.fieldNames()) {
			modifier.set(attr, data.getValue(attr));
		}
		modifier.set("modified", MongoDb.now());
		mongo.update(collection, MongoQueryBuilder.build(query),
				modifier.build(), validActionResultHandler(handler));
	}

	@Override
	public void delete(String id, Handler<Either<String, JsonObject>> handler) {
		delete(id, null, handler);
	}

	@Override
	public void delete(String id, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		QueryBuilder q = QueryBuilder.start("_id").is(id);
		mongo.delete(collection, MongoQueryBuilder.build(q), validActionResultHandler(handler));
	}

	@Override
	public void list(Handler<Either<String, JsonArray>> handler) {
		JsonObject sort = new JsonObject().put("modified", -1);
		mongo.find(collection, new JsonObject(), sort, defaultListProjection, validResultsHandler(handler));
	}

	@Override
	public void list(VisibilityFilter filter, UserInfos user, Handler<Either<String, JsonArray>> handler) {
		QueryBuilder query;
		if (user != null) {
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(user.getUserId()).get());
			for (String gpId: user.getGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId).get());
			}
			switch (filter) {
				case OWNER:
					query = QueryBuilder.start("owner.userId").is(user.getUserId());
					break;
				case OWNER_AND_SHARED:
					query = new QueryBuilder().or(
							QueryBuilder.start("owner.userId").is(user.getUserId()).get(),
							QueryBuilder.start("shared").elemMatch(
									new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
							).get());
					break;
				case SHARED:
					query = QueryBuilder.start("shared").elemMatch(
									new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get());
					break;
				case PROTECTED:
					query = QueryBuilder.start("visibility").is(VisibilityFilter.PROTECTED.name());
					break;
				case PUBLIC:
					query = QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name());
					break;
				default:
					query = new QueryBuilder().or(
							QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name()).get(),
							QueryBuilder.start("visibility").is(VisibilityFilter.PROTECTED.name()).get(),
							QueryBuilder.start("owner.userId").is(user.getUserId()).get(),
							QueryBuilder.start("shared").elemMatch(
									new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
							).get());
					break;
			}
		} else {
			query = QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name());
		}
		JsonObject sort = new JsonObject().put("modified", -1);
		mongo.find(collection, MongoQueryBuilder.build(query), sort,
				defaultListProjection, validResultsHandler(handler));
	}

	public void isOwner(String id, UserInfos user, final Handler<Boolean> handler) {
		QueryBuilder query = QueryBuilder.start("_id").is(id).put("owner.userId").is(user.getUserId());
		mongo.count(collection, MongoQueryBuilder.build(query), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject res = event.body();
				handler.handle(res != null && "ok".equals(res.getString("status")) && 1 == res.getInteger("count"));
			}
		});
	}

	private void addPlainField(JsonObject data) {
		if (!this.mongoDbConf.getSearchTextFields().isEmpty()) {
			for (final String field : this.mongoDbConf.getSearchTextFields()) {
				final List<String> decomposition = StringUtils.split(field, "\\.");
				final String collection = decomposition.get(0);
				if (this.collection.equals(collection)) {
					if (decomposition.size() == 2) {
						//not an object or array
						final String label = decomposition.get(1);
						if (data.containsKey(label)) {
							data.put(label + plainSuffixField, StringUtils.stripHtmlTag(data.getString(label)));
						}
					} else if (decomposition.size() == 3) {
						final String label = decomposition.get(1);
						final String deepLabel = decomposition.get(2);
						final Object element = data.getValue(label);
						if (element instanceof JsonArray) {
							//not processed yet
							log.error("the plain duplication d'ont support Json Array");
						} else if (element instanceof JsonObject) {
							final JsonObject jo = (JsonObject) element;
							if (jo.containsKey(deepLabel)) {
								jo.put(deepLabel + plainSuffixField,
										StringUtils.stripHtmlTag(jo.getString(deepLabel)));
							}
						}
					} else {
						//object too complex : not treaty
						log.error("the plain duplication only works for a string field or top-level field of an object : collection.field | collection.object.field");
					}
				} else {
					break;
				}
			}
		}
	}

}
