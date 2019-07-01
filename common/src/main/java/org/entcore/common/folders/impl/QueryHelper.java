package org.entcore.common.folders.impl;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.ElementQuery.ElementSort;
import org.entcore.common.folders.impl.InheritShareComputer.InheritShareResult;
import org.entcore.common.service.impl.MongoDbSearchService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

import fr.wseduc.mongodb.AggregationsBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class QueryHelper {
	final static long MAX_DEPTH = 10;
	protected final MongoDb mongo = MongoDb.getInstance();
	private final String collection;

	public QueryHelper(String collection) {
		this.collection = collection;
	}

	static JsonObject toJson(QueryBuilder queryBuilder) {
		return MongoQueryBuilder.build(queryBuilder);
	}

	static boolean isOk(JsonObject body) {
		return "ok".equals(body.getString("status"));
	}

	static String toErrorStr(JsonObject body) {
		return body.getString("error", body.getString("message", "query helper error"));
	}

	public static class DocumentQueryBuilder {
		QueryBuilder builder = new QueryBuilder();
		private boolean excludeDeleted;
		private JsonObject mongoSorts;
		private JsonObject mongoProjections;
		private boolean onlyDeleted;
		private Integer skip;
		private Integer limit;

		public static DocumentQueryBuilder fromElementQuery(ElementQuery query, Optional<UserInfos> user) {
			DocumentQueryBuilder builder = new DocumentQueryBuilder();
			if (user.isPresent()) {
				// filters that belongs to user and shares
				// search by share or owner
				if (query.getVisibilitiesOr() != null && query.getVisibilitiesOr().size() > 0) {
					if (query.isDirectShared()){
						builder.filterBySharedAndOwnerOrVisibilities(user.get(), query.getVisibilitiesOr());
					} else if (query.getShared()) {
						builder.filterByInheritShareAndOwnerOrVisibilities(user.get(), query.getVisibilitiesOr());
					} else {
						builder.filterByOwnerOrVisibilities(user.get(), query.getVisibilitiesOr());
					}
				} else {
					if (query.isDirectShared()){
						builder.filterBySharedAndOwner(user.get());
					} else if (query.getShared()) {
						builder.filterByInheritShareAndOwner(user.get());
					} else {
						builder.filterByOwner(user.get());
					}
				}
				//
				if (query.getHasBeenShared() != null) {
					builder.withBeingShared(query.getHasBeenShared());
				}
				//
				if (query.isFavorites()) {
					builder.withOnlyFavorites(user.get());
				}
				//
				if (query.getActionNotExists() != null) {
					builder.withActionNotExistingInShared(user.get(), query.getActionNotExists());
				}
			}
			//
			if (query.getVisibilitiesIn() != null) {
				builder.withVisibilities(query.getVisibilitiesIn());
			}
			if (query.getVisibilitiesNotIn() != null) {
				builder.withNotVisibilities(query.getVisibilitiesNotIn());
			}
			//
			if (query.getId() != null) {
				builder.withId(query.getId());
			}
			if (query.getIds() != null && query.getIds().size() > 0) {
				builder.withId(query.getIds());
			}
			if (query.getType() != null) {
				builder.withFileType(query.getType());
			}
			if (query.getTrash() != null) {
				if (query.getTrash()) {
					builder.withOnlyDeleted();
				} else {
					builder.withExcludeDeleted();
				}
			}
			if (query.getParentId() != null) {
				builder.withParent(query.getParentId());
			}
			if (query.getApplication() != null) {
				builder.withKeyValue("application", query.getApplication());
			}
			if (query.getNotApplication() != null) {
				builder.withKeyValueNotEq("application", query.getApplication());
			}
			if (query.getSearchByName() != null) {
				builder.withNameMatch(query.getSearchByName());
			}
			if (query.getParams() != null && !query.getParams().isEmpty()) {
				query.getParams().forEach((key, value) -> {
					builder.withKeyValue(key, value);
				});
			}
			// advanced filters
			if (query.getFullTextSearch() != null) {
				builder.withFullTextSearch(query.getFullTextSearch());
			}
			if (query.getProjection() != null && !query.getProjection().isEmpty()) {
				builder.withProjections(query.getProjection());
			}
			if (query.getSort() != null && !query.getSort().isEmpty()) {
				builder.withSorts(query.getSort());
			}
			if (query.getLimit() != null) {
				builder.withSkipAndLimit(query.getSkip(), query.getLimit());
			}
			if (query.getOwnerIds() != null && query.getOwnerIds().size() > 0) {
				builder.withOwnerIds(query.getOwnerIds());
			}
			if (query.getTrasherId() != null) {
				builder.withTrasherId(query.getTrasherId());
			}
			//
			if(!StringUtils.isEmpty(query.getAncestorId())) {
				builder.withAncestorContains(query.getAncestorId());
			}
			// filter without parent
			if (query.getHierarchical() == null || !query.getHierarchical()) {
				if (query.getNoParent() != null && query.getNoParent()) {
					builder.withHavingParent(false);
				}
			}
			return builder;
		}

		public DocumentQueryBuilder withAncestorContains(String ancestorId) {
			DBObject sub = new BasicDBObject();
			sub.put("$eq", ancestorId);
			builder.and(QueryBuilder.start("ancestors").elemMatch(sub).get());
			return this;
		}

		public DocumentQueryBuilder withHavingParent(boolean haveParent) {
			if (haveParent) {
				builder.and(QueryBuilder.start("eParent").exists(true).and("eParent").notEquals(null).get());
			} else {
				builder.and(new QueryBuilder().or(//
						QueryBuilder.start("eParent").exists(false).get(), //
						QueryBuilder.start("eParent").is(null).get()).get()//
				);
			}
			return this;
		}

		public DocumentQueryBuilder withSkipAndLimit(Integer skip, Integer limit) {
			if (skip == null) {
				skip = -1;
			}
			this.limit = limit;
			this.skip = skip;
			return this;
		}

		public DocumentQueryBuilder withProjection(String projection) {
			mongoProjections = new JsonObject();
			mongoProjections.put(projection, 1);
			return this;
		}

		public DocumentQueryBuilder withProjections(Set<String> projection) {
			mongoProjections = new JsonObject();
			for (String p : projection) {
				mongoProjections.put(p, 1);
			}
			return this;
		}

		public DocumentQueryBuilder withSorts(List<Map.Entry<String, ElementSort>> sorts) {
			mongoSorts = new JsonObject();
			for (Map.Entry<String, ElementSort> p : sorts) {
				mongoSorts.put(p.getKey(), p.getValue().equals(ElementSort.Asc) ? 1 : -1);
			}
			return this;
		}

		public DocumentQueryBuilder withVisibilities(Collection<String> visibilities) {
			if (visibilities == null) {
				return this;
			}
			for (String v : visibilities) {
				builder.and(QueryBuilder.start(v).is(true).get());
			}
			return this;
		}

		public DocumentQueryBuilder withNotVisibilities(Collection<String> visibilities) {
			if (visibilities == null) {
				return this;
			}
			for (String v : visibilities) {
				builder.and(QueryBuilder.start(v).notEquals(true).get());
			}
			return this;
		}

		public DocumentQueryBuilder filterByOwnerVisibilities(UserInfos user, Collection<String> visibilities) {
			List<DBObject> ors = new ArrayList<>();
			for (String v : visibilities) {
				ors.add(QueryBuilder.start(v).is(true).get());
			}
			ors.add(QueryBuilder.start("owner").is(user.getUserId()).get());
			//
			builder.or(ors.toArray(new DBObject[ors.size()]));
			return this;
		}

		public DocumentQueryBuilder withFullTextSearch(List<String> searchWordsLst) {
			if (searchWordsLst.isEmpty()) {
				return this;
			}
			final QueryBuilder worldsQuery = new QueryBuilder();
			worldsQuery.text(MongoDbSearchService.textSearchedComposition(searchWordsLst));
			builder.and(worldsQuery.get());
			return this;
		}

		public DocumentQueryBuilder filterBySharedAndOwner(UserInfos user) {
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(user.getUserId()).get());
			for (String gpId : user.getGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId).get());
			}
			DBObject subQuery = new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get();
			builder.or(QueryBuilder.start("owner").is(user.getUserId()).get(), //
					QueryBuilder.start("shared").elemMatch(subQuery).get());
			return this;
		}

		public DocumentQueryBuilder filterByInheritShareAndOwnerOrVisibilities(UserInfos user,
																			   Collection<String> visibilities) {
			List<DBObject> ors = new ArrayList<>();
			// owner
			ors.add(QueryBuilder.start("owner").is(user.getUserId()).get());
			// shared
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(user.getUserId()).get());
			for (String gpId : user.getGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId).get());
			}
			DBObject subQuery = new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get();
			ors.add(QueryBuilder.start("inheritedShares").elemMatch(subQuery).get());
			//
			for (String visibility : visibilities) {
				ors.add(QueryBuilder.start(visibility).is(true).get());
			}
			//
			builder.or(ors.toArray(new DBObject[ors.size()]));
			return this;
		}

		public DocumentQueryBuilder filterBySharedAndOwnerOrVisibilities(UserInfos user,
																			   Collection<String> visibilities) {
			List<DBObject> ors = new ArrayList<>();
			// owner
			ors.add(QueryBuilder.start("owner").is(user.getUserId()).get());
			// shared
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(user.getUserId()).get());
			for (String gpId : user.getGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId).get());
			}
			DBObject subQuery = new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get();
			ors.add(QueryBuilder.start("shared").elemMatch(subQuery).get());
			//
			for (String visibility : visibilities) {
				ors.add(QueryBuilder.start(visibility).is(true).get());
			}
			//
			builder.or(ors.toArray(new DBObject[ors.size()]));
			return this;
		}

		public DocumentQueryBuilder filterByOwnerOrVisibilities(UserInfos user, Collection<String> visibilities) {
			List<DBObject> ors = new ArrayList<>();
			// owner
			ors.add(QueryBuilder.start("owner").is(user.getUserId()).get());
			//
			for (String visibility : visibilities) {
				ors.add(QueryBuilder.start(visibility).is(true).get());
			}
			//
			builder.or(ors.toArray(new DBObject[ors.size()]));
			return this;
		}

		public DocumentQueryBuilder filterByInheritShareAndOwner(UserInfos user) {
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(user.getUserId()).get());
			for (String gpId : user.getGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId).get());
			}
			DBObject subQuery = new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get();
			builder.or(//
					QueryBuilder.start("owner").is(user.getUserId()).get(), //
					QueryBuilder.start("inheritedShares").elemMatch(subQuery).get()//
			);
			return this;
		}

		public DocumentQueryBuilder withOnlyFavorites(UserInfos user) {
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(user.getUserId()).get());
			// if one day we have favorites groups...
			// for (String gpId : user.getGroupsIds()) {
			// groups.add(QueryBuilder.start("groupId").is(gpId).get());
			// }
			builder.and(QueryBuilder.start("favorites")
					.elemMatch(new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()).get());
			return this;
		}

		public DocumentQueryBuilder filterByOwner(UserInfos user) {
			builder.and("owner").is(user.getUserId());
			return this;
		}

		public DocumentQueryBuilder filterByInheritShareAndOwnerWithAction(UserInfos user, String action) {
			List<DBObject> groups = new ArrayList<>();
			groups.add(QueryBuilder.start("userId").is(user.getUserId()).put(action).is(true).get());
			for (String gpId : user.getGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId).put(action).is(true).get());
			}
			builder.or(QueryBuilder.start("owner").is(user.getUserId()).get(), QueryBuilder.start("inheritedShares")
					.elemMatch(new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()).get());
			return this;
		}

		public DocumentQueryBuilder withOwnerIds(Collection<String> ids) {
			builder.and(QueryBuilder.start("owner").in(ids).get());
			return this;
		}

		public DocumentQueryBuilder withTrasherId(String ids) {
			builder.and(QueryBuilder.start("trasher").is(ids).get());
			return this;
		}

		public DocumentQueryBuilder withBeingShared(Boolean isShared) {
			// if is shared does not exists => notEquals
			if (isShared) {
				builder.and(QueryBuilder.start("isShared").is(true).get());
			} else {
				builder.and(QueryBuilder.start("isShared").notEquals(true).get());
			}
			return this;
		}

		public DocumentQueryBuilder withActionNotExistingInShared(UserInfos user, String action) {
			builder.and(QueryBuilder.start("inheritedShares")
					.elemMatch(QueryBuilder.start("userId").notEquals(user.getUserId()).and(action).is(true).get())
					.get());
			return this;
		}

		public DocumentQueryBuilder withParent(String id) {
			builder.and("eParent").is(id);
			return this;
		}

		public DocumentQueryBuilder withParent(Collection<String> id) {
			builder.and("eParent").in(id);
			return this;
		}

		public DocumentQueryBuilder withId(String id) {
			builder.and("_id").is(id);
			return this;
		}

		public DocumentQueryBuilder withId(Collection<String> id) {
			builder.and("_id").in(id);
			return this;
		}

		public DocumentQueryBuilder withKeyValue(String key, Object value) {
			builder.and(key).is(value);
			return this;
		}

		public DocumentQueryBuilder withKeyValueNotEq(String key, Object value) {
			builder.and(key).notEquals(value);
			return this;
		}

		public DocumentQueryBuilder withIds(Collection<String> ids) {
			builder.and("_id").in(ids);
			return this;
		}

		public DocumentQueryBuilder withExcludeDeleted() {
			excludeDeleted = true;
			builder.and("deleted").notEquals(true);
			return this;
		}

		public DocumentQueryBuilder withOnlyDeleted() {
			onlyDeleted = true;
			builder.and("deleted").is(true);
			return this;
		}

		public DocumentQueryBuilder withFileType(final String type) {
			builder.and("eType").is(type);
			return this;
		}

		public DocumentQueryBuilder withNameMatch(final String pattern) {
			builder.and("name").regex(Pattern.compile("^" + pattern + "(_|$)"));
			return this;
		}

		public boolean isExcludeDeleted() {
			return excludeDeleted;
		}

		public boolean isOnlyDeleted() {
			return onlyDeleted;
		}

		public QueryBuilder build() {
			return builder;
		}
	}

	DocumentQueryBuilder queryBuilder() {
		return new DocumentQueryBuilder();
	}

	Future<List<String>> getChildrenIdsRecursively(DocumentQueryBuilder parentFilter,
			Optional<DocumentQueryBuilder> queryChildren, boolean includeParents) {
		JsonObject projection = new JsonObject().put("_id", 1).put("children", "$children._id");
		@SuppressWarnings("unchecked")
		Optional<JsonObject> query = queryChildren.map(q -> new JsonObject(q.build().get().toMap()));
		AggregationsBuilder agg = AggregationsBuilder.startWithCollection(this.collection)//
				.withMatch(parentFilter.build())//
				.withGraphLookup("$_id", "_id", "eParent", "children", Optional.of(MAX_DEPTH), Optional.empty(), query)//
				.withProjection(projection)//
				.withAllowDiskUse(true);//#24499 allow disk use for big folder tree
		JsonObject command = agg.getCommand();
		Future<List<String>> future = Future.future();
		mongo.aggregate(command, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				JsonArray results = (body.getJsonObject("result", new JsonObject())
						.getJsonObject("cursor", new JsonObject()).getJsonArray("firstBatch"));
				List<String> returned = new ArrayList<>();
				for (int i = 0; i < results.size(); i++) {
					JsonObject result = results.getJsonObject(i);
					if (includeParents) {
						returned.add(result.getString("_id"));
					}
					JsonArray children = result.getJsonArray("children", new JsonArray());
					for (int j = 0; j < children.size(); j++) {
						String child = children.getString(j);
						returned.add(child);
					}
				}
				future.complete(returned);
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<List<JsonObject>> getChildrenRecursively(DocumentQueryBuilder parentFilter,
			Optional<DocumentQueryBuilder> queryChildren, boolean includeParents) {
		// impossible de charger les objets sous forme d arbre => stackoverflow ->
		// charger ids
		return this.getChildrenIdsRecursively(parentFilter, queryChildren, includeParents).compose(ids -> {
			if (ids.isEmpty()) {
				return Future.succeededFuture(new ArrayList<>());
			} else {
				return this.findAllAsList(queryBuilder().withId(ids));
			}
		});
	}

	Future<JsonArray> listWithParents(DocumentQueryBuilder query) {

		Future<JsonArray> future = Future.future();
		// match all (folders and file)
		QueryBuilder match = query.build();
		// first : match only folder regarding criterias
		AggregationsBuilder agg = AggregationsBuilder.startWithCollection(this.collection);
		agg = agg.withMatch(match)
				// then build the graph from root folder
				.withGraphLookup("$eParent", "eParent", "_id", "tree", Optional.of(MAX_DEPTH), Optional.empty(),
						Optional.empty());
		if (query.isExcludeDeleted()) {
			// exclude deleted files => graphlookup reintroduce deleted children
			agg = agg.withMatch(queryBuilder().withExcludeDeleted().build());
		}
		if (query.isOnlyDeleted()) {
			agg = agg.withMatch(queryBuilder().withOnlyDeleted().build());
		}
		// finally project name and parent
		JsonObject projections = new JsonObject();
		if (query.mongoProjections != null) {
			projections = query.mongoProjections;
		} else {
			for (String p : ElementQuery.defaultProjection()) {
				projections.put(p, 1);
			}
		}
		projections.put("parents", "$tree._id");
		agg.withProjection(projections);
		// sort
		if (query.mongoSorts != null) {
			agg.withSort(query.mongoSorts);
		}
		// skip and limit
		if (query.skip != null) {
			agg.withSkip(query.skip);
		}
		if (query.limit != null) {
			agg.withLimit(query.limit);
		}
		//
		JsonObject command = agg.getCommand();
		mongo.aggregate(command, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(body.getJsonObject("result", new JsonObject()).getJsonObject("cursor", new JsonObject())
						.getJsonArray("firstBatch", new JsonArray()));
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<JsonObject> findById(String id) {
		Future<JsonObject> future = Future.future();
		mongo.findOne(collection, toJson(QueryBuilder.start("_id").is(id)), message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(body.getJsonObject("result"));
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<JsonObject> findOne(DocumentQueryBuilder query) {
		Future<JsonObject> future = Future.future();
		mongo.findOne(collection, toJson(query.build()), message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(body.getJsonObject("result"));
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<List<JsonObject>> findAllAsList(DocumentQueryBuilder query) {
		return findAll(query).map(s -> s.stream().map(o -> (JsonObject) o).collect(Collectors.toList()));
	}

	Future<Integer> countAll(DocumentQueryBuilder query) {
		Future<Integer> future = Future.future();
		// finally project name and parent

		JsonObject queries = toJson(query.build());
		mongo.count(collection, queries, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(body.getInteger("count"));
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<JsonArray> findAll(DocumentQueryBuilder query) {
		Future<JsonArray> future = Future.future();
		// finally project name and parent
		JsonObject projections = null;
		if (query.mongoProjections != null) {
			projections = query.mongoProjections;
		}
		// sort
		JsonObject mongoSorts = null;
		if (query.mongoSorts != null) {
			mongoSorts = (query.mongoSorts);
		}
		// limit skip
		Integer limit = -1, skip = -1;
		if (query.limit != null) {
			limit = query.limit;
		}
		if (query.skip != null) {
			skip = query.skip;
		}
		JsonObject queries = toJson(query.build());
		mongo.find(collection, queries, mongoSorts, projections, skip, limit, Integer.MAX_VALUE, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(body.getJsonArray("results"));
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<JsonObject> insert(JsonObject file) {
		Future<JsonObject> future = Future.future();
		mongo.insert(collection, file, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(file.put("_id", body.getString("_id")));
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<JsonObject> upsertFolder(JsonObject folder) {
		Future<JsonObject> future = Future.future();
		//findAndModify does not generate _id string
		String genID = UUID.randomUUID().toString();
		JsonObject matcher = new JsonObject().put("owner", DocumentHelper.getOwner(folder))
												.put("externalId",DocumentHelper.getExternalId(folder));
		JsonObject fields = new JsonObject().put("_id",1);
		JsonObject cloneFolder = new JsonObject(new HashMap<>(folder.getMap()));
		JsonObject update = new JsonObject().put("$setOnInsert", cloneFolder.put("_id",genID));
		mongo.findAndModify(collection, matcher, update, null, fields, false, true, true,  message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				JsonObject result = body.getJsonObject("result", new JsonObject());
				future.complete(folder.put("_id", result.getString("_id")));
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<List<JsonObject>> insertAll(List<JsonObject> files) {
		// TODO bulk insert
		@SuppressWarnings("rawtypes")
		List<Future> futures = new ArrayList<>();
		for (JsonObject json : files) {
			futures.add(insert(json));
		}
		return CompositeFuture.all(futures).map(results -> {
			return results.list();
		});
	}

	Future<JsonObject> updateInheritShares(JsonObject file) {
		Future<JsonObject> future = Future.future();
		String id = file.getString("_id");
		String now = MongoDb.formatDate(new Date());
		JsonArray inheritShared = file.getJsonArray("inheritedShares");
		JsonArray ancestors = file.getJsonArray("ancestors");
		JsonObject set = new MongoUpdateBuilder().set("inheritedShares", inheritShared)
				.set("isShared", inheritShared != null && inheritShared.size() > 0)//
				.set("ancestors", ancestors)//
				.set("modified", now).build();
		mongo.update(collection, toJson(QueryBuilder.start("_id").is(id)), set, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(file);
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<Void> updateMove(String sourceId, String destinationFolderId) {
		String now = MongoDb.formatDate(new Date());
		MongoUpdateBuilder set = new MongoUpdateBuilder().set("modified", now).set("eParent", destinationFolderId)
				.unset("eParentOld");
		return update(sourceId, set);
	}

	Future<Void> update(String id, JsonObject set) {
		Future<Void> future = Future.future();
		mongo.update(collection, toJson(QueryBuilder.start("_id").is(id)), set, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(null);
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<Void> update(String id, MongoUpdateBuilder set) {
		Future<Void> future = Future.future();
		String now = MongoDb.formatDate(new Date());
		set.set("modified", now);
		mongo.update(collection, toJson(QueryBuilder.start("_id").is(id)), set.build(), message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(null);
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<Void> updateAll(Set<String> id, MongoUpdateBuilder set) {
		Future<Void> future = Future.future();
		String now = MongoDb.formatDate(new Date());
		set.set("modified", now);
		JsonObject query = toJson(QueryBuilder.start("_id").in(id));
		JsonObject setJson = set.build();
		mongo.update(collection, query, setJson, false, true, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(null);
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future;
	}

	Future<Void> bulkUpdateFavorites(Collection<JsonObject> rows) {
		JsonArray operations = new JsonArray();
		rows.stream().map(o -> (JsonObject) o).forEach(row -> {
			operations.add(new JsonObject().put("operation", "update")//
					.put("document",
							new JsonObject().put("$set",
									new JsonObject().put("favorites", row.getJsonArray("favorites"))))//
					.put("criteria", new JsonObject().put("_id", row.getString("_id"))));
		});
		return bulkUpdate(operations);
	}

	Future<Void> bulkUpdateShares(InheritShareResult res) {
		JsonArray operations = new JsonArray();
		String now = MongoDb.formatDate(new Date());
		res.getAll().stream().map(o -> (JsonObject) o).forEach(row -> {
			JsonObject set = new MongoUpdateBuilder()//
					.set("modified", now)//
					.set("isShared", row.getJsonArray("inheritedShares", new JsonArray()).size() > 0)//
					.set("ancestors", row.getJsonArray("ancestors", new JsonArray()))//
					.set("inheritedShares", row.getJsonArray("inheritedShares", new JsonArray()))//
					.set("shared", row.getJsonArray("shared", new JsonArray())).build();
			operations.add(new JsonObject().put("operation", "update")//
					.put("document", set)//
					.put("criteria", new JsonObject().put("_id", row.getString("_id"))));
		});
		return bulkUpdate(operations);
	}

	Future<Void> bulkUpdate(JsonArray operations) {
		Future<Void> future = Future.future();
		mongo.bulk(collection, operations, bulkEv -> {
			if (isOk(bulkEv.body())) {
				future.complete((null));
			} else {
				future.fail(toErrorStr(bulkEv.body()));
			}
		});
		return future;
	}

	Future<Void> deleteByIds(Set<String> ids) {
		if (ids.isEmpty()) {
			return Future.succeededFuture();
		}
		Future<Void> future = Future.future();
		mongo.delete(collection, toJson(QueryBuilder.start("_id").in(ids)), res -> {
			if (isOk(res.body())) {
				future.complete(null);
			} else {
				future.fail(toErrorStr(res.body()));
			}
		});
		return future;
	}

	public static enum RestoreParentDirection {
		FromShare, FromTrash;
	}

	private Future<Void> restoreParentsMatching(Collection<JsonObject> docs, DocumentQueryBuilder query) {
		return findAllAsList(query).compose(parentDocsOk -> {
			Set<String> parentIdsOk = parentDocsOk.stream().map(o -> DocumentHelper.getId(o))
					.collect(Collectors.toSet());
			Set<String> treeIds = docs.stream().map(o -> DocumentHelper.getId(o)).collect(Collectors.toSet());
			Set<String> idsToRename = new HashSet<>();
			Set<String> idsToRemoveParent = new HashSet<>();
			for (JsonObject doc : docs) {
				String id = DocumentHelper.getId(doc);
				String oldParent = DocumentHelper.getParentOld(doc);
				// String parent = DocumentHelper.getParent(doc);
				// if oldparent is not in my virtual tree=> remove parent
				if (oldParent != null) {
					if (parentIdsOk.contains(oldParent) || treeIds.contains(oldParent)) {
						idsToRename.add(id);
					} else {
						idsToRemoveParent.add(id);
					}
				}
				// should we check parent? not needed?
			}
			Future<Void> futureRename = updateAll(idsToRename,
					new MongoUpdateBuilder().rename("eParentOld", "eParent"));
			Future<Void> futureRemove = updateAll(idsToRemoveParent,
					new MongoUpdateBuilder().unset("eParentOld").unset("eParent"));
			return CompositeFuture.all(futureRename, futureRemove).mapEmpty();
		});
	}

	public Future<Void> breakParentLink(Set<String> ids) {
		if (ids.isEmpty()) {
			return Future.succeededFuture();
		}
		return updateAll(ids, new MongoUpdateBuilder().rename("eParent", "eParentOld"));
	}

	public Future<Void> restoreParentLink(RestoreParentDirection dir, Collection<JsonObject> all) {
		Set<String> eParentOlds = all.stream().map(o -> DocumentHelper.getParentOld(o))
				.filter(pa -> !StringUtils.isEmpty(pa)).collect(Collectors.toSet());
		if (eParentOlds.isEmpty()) {
			return Future.succeededFuture();
		}
		switch (dir) {
		case FromShare: {
			return restoreParentsMatching(all,
					queryBuilder().withId(eParentOlds).withBeingShared(false).withProjection("_id"));
		}
		case FromTrash: {
			return restoreParentsMatching(all,
					queryBuilder().withId(eParentOlds).withExcludeDeleted().withProjection("_id"));
		}
		}
		return Future.failedFuture("File operation does not exists: " + dir);
	}
}
