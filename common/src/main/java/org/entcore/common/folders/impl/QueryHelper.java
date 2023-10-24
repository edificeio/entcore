package org.entcore.common.folders.impl;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.mongodb.client.model.Filters;
import io.vertx.core.Promise;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.ElementQuery.ElementSort;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.impl.InheritShareComputer.InheritShareResult;
import org.entcore.common.service.impl.MongoDbSearchService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;


import fr.wseduc.mongodb.AggregationsBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class QueryHelper {
	final static int MAX_BATCH = 10;
	final static long MAX_DEPTH = 10;
	protected final MongoDb mongo = MongoDb.getInstance();
	private final String collection;
	private final boolean useOldChildrenQuery;

	public QueryHelper(String collection, boolean useOldChildrenQuery) {
		this.collection = collection;
		this.useOldChildrenQuery = useOldChildrenQuery;
	}

	static JsonObject toJson(Bson queryBuilder) {
		return MongoQueryBuilder.build(queryBuilder);
	}

	static boolean isOk(JsonObject body) {
		return "ok".equals(body.getString("status"));
	}

	static String toErrorStr(JsonObject body) {
		return body.getString("error", body.getString("message", "query helper error"));
	}

	public static class DocumentQueryBuilder {
		Bson builder = Filters.empty();
		private boolean excludeDeleted;
		private JsonObject mongoSorts;
		private JsonObject mongoProjections;
		private boolean onlyDeleted;
		private Integer skip;
		private Integer limit;
		private List<String> parentIdsFilter = new ArrayList<>();

		public DocumentQueryBuilder copy() {
			final DocumentQueryBuilder copy = new DocumentQueryBuilder();
			copy.builder = this.builder;
			copy.excludeDeleted = this.excludeDeleted;
			copy.mongoSorts = this.mongoSorts;
			copy.mongoProjections = this.mongoProjections;
			copy.onlyDeleted = this.onlyDeleted;
			copy.skip = this.skip;
			copy.limit = this.limit;
			copy.parentIdsFilter = this.parentIdsFilter;
			return copy;
		}

		public boolean hasParentIdFilter(){
			return parentIdsFilter.size() > 0;
		}

		public static DocumentQueryBuilder fromElementQuery(ElementQuery query, Optional<UserInfos> user) {
			DocumentQueryBuilder builder = new DocumentQueryBuilder();
			if (user.isPresent()) {
				// filters that belongs to user and shares
				// search by share or owner
				if(!StringUtils.isEmpty(query.getActionExistsInInheritedShares())){
					if (query.getVisibilitiesOr() != null && query.getVisibilitiesOr().size() > 0) {
						builder.withActionExistingInInheritedSharedWithOrVisiblities(user.get(), query.getActionExistsInInheritedShares(),query.getVisibilitiesOr());
					}else{
						builder.withActionExistingInInheritedShared(user.get(), query.getActionExistsInInheritedShares());
					}
				} else if (query.getVisibilitiesOr() != null && query.getVisibilitiesOr().size() > 0) {
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
					builder.withActionNotExistingInInheritedShared(user.get(), query.getActionNotExists());
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
			builder = Filters.and(builder, Filters.elemMatch("ancestors", Filters.eq("$eq", ancestorId)));
			return this;
		}

		public DocumentQueryBuilder withEparentNotIn(Set<String> eParents) {
			builder = Filters.and(builder, Filters.or(
						Filters.exists("eParent", false),
						Filters.nin("eParent", eParents)));
			return this;
		}

		public DocumentQueryBuilder withHavingParent(boolean haveParent) {
			if (haveParent) {
				builder = Filters.and(builder, Filters.and(
						Filters.exists("eParent", true),
						Filters.ne("eParent", null)));
			} else {
				builder= Filters.and(builder, Filters.or(
						Filters.exists("eParent",false),
						Filters.eq("eParent", null)));
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
			if(projection == null || projection.isEmpty()) return this;
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
				builder = Filters.and(builder, Filters.eq(v, true));
			}
			return this;
		}

		public DocumentQueryBuilder withNotVisibilities(Collection<String> visibilities) {
			if (visibilities == null) {
				return this;
			}
			for (String v : visibilities) {
				builder = Filters.and(builder, Filters.ne(v, true));
			}
			return this;
		}

		public DocumentQueryBuilder filterByOwnerVisibilities(UserInfos user, Collection<String> visibilities) {
			List<Bson> ors = new ArrayList<>();
			for (String v : visibilities) {
				ors.add(Filters.eq(v, true));
			}
			ors.add(Filters.eq("owner", user.getUserId()));
			builder = Filters.and(builder, Filters.or(ors));
			return this;
		}

		public DocumentQueryBuilder withFullTextSearch(List<String> searchWordsLst) {
			if (searchWordsLst.isEmpty()) {
				return this;
			}
			final Bson worldsQuery = Filters.text(MongoDbSearchService.textSearchedComposition(searchWordsLst));
			builder = Filters.and(builder, worldsQuery);
			return this;
		}

		public DocumentQueryBuilder filterBySharedAndOwner(UserInfos user) {
			List<Bson> groups = new ArrayList<>();
			groups.add(Filters.eq("userId", user.getUserId()));
			for (String gpId : user.getGroupsIds()) {
				groups.add(Filters.eq("groupId", gpId));
			}
			Bson subQuery = Filters.or(groups);
			builder = Filters.and(builder, Filters.or(
						Filters.eq("owner", user.getUserId()), //
						Filters.elemMatch("shared", subQuery)));
			return this;
		}

		public DocumentQueryBuilder filterByInheritShareAndOwnerOrVisibilities(UserInfos user,
																			   Collection<String> visibilities) {
			List<Bson> ors = new ArrayList<>();
			// owner
			ors.add(Filters.eq("owner", user.getUserId()));
			// shared
			List<Bson> groups = new ArrayList<>();
			groups.add(Filters.eq("userId", user.getUserId()));
			for (String gpId : user.getGroupsIds()) {
				groups.add(Filters.eq("groupId", gpId));
			}
			Bson subQuery = Filters.or(groups);
			ors.add(Filters.elemMatch("inheritedShares", subQuery));
			//
			for (String visibility : visibilities) {
				ors.add(Filters.eq(visibility, true));
			}
			//
			builder = Filters.and(builder, Filters.or(ors));
			return this;
		}

		public DocumentQueryBuilder filterBySharedAndOwnerOrVisibilities(UserInfos user,
																			   Collection<String> visibilities) {
			List<Bson> ors = new ArrayList<>();
			// owner
			ors.add(Filters.eq("owner", user.getUserId()));
			// shared
			List<Bson> groups = new ArrayList<>();
			groups.add(Filters.eq("userId", user.getUserId()));
			for (String gpId : user.getGroupsIds()) {
				groups.add(Filters.eq("groupId", gpId));
			}
			Bson subQuery = Filters.or(groups);
			ors.add(Filters.elemMatch("shared", subQuery));
			//
			for (String visibility : visibilities) {
				ors.add(Filters.eq(visibility, true));
			}
			//
			builder = Filters.and(builder, Filters.or(ors));
			return this;
		}

		public DocumentQueryBuilder filterByOwnerOrVisibilities(UserInfos user, Collection<String> visibilities) {
			List<Bson> ors = new ArrayList<>();
			// owner
			ors.add(Filters.eq("owner", user.getUserId()));
			//
			for (String visibility : visibilities) {
				ors.add(Filters.eq(visibility, true));
			}
			//
			builder = Filters.and(builder, Filters.or(ors));
			return this;
		}

		public DocumentQueryBuilder filterByInheritShareAndOwner(UserInfos user) {
			List<Bson> groups = new ArrayList<>();
			groups.add(Filters.eq("userId", user.getUserId()));
			for (String gpId : user.getGroupsIds()) {
				groups.add(Filters.eq("groupId", gpId));
			}
			Bson subQuery = Filters.or(groups);
			builder = Filters.and(builder,
					Filters.or(
							Filters.eq("owner", user.getUserId()),
							Filters.elemMatch("inheritedShares", subQuery)));
			return this;
		}

		public DocumentQueryBuilder withOnlyFavorites(UserInfos user) {
			List<Bson> groups = new ArrayList<>();
			groups.add(Filters.eq("userId", user.getUserId()));
			// if one day we have favorites groups...
			// for (String gpId : user.getGroupsIds()) {
			// groups.add(Filters.eq("groupId", gpId));
			// }
			builder = Filters.and(builder, Filters.elemMatch("favorites", Filters.or(groups)));
			return this;
		}

		public DocumentQueryBuilder filterByOwner(UserInfos user) {
			builder = Filters.and(builder, Filters.eq("owner", user.getUserId()));
			return this;
		}

		public DocumentQueryBuilder filterByInheritShareAndOwnerWithAction(UserInfos user, String action) {
			List<Bson> groups = new ArrayList<>();
			groups.add(Filters.and(Filters.eq("userId", user.getUserId()), Filters.eq(action, true)));
			for (String gpId : user.getGroupsIds()) {
				groups.add(Filters.and(Filters.eq("groupId", gpId), Filters.eq(action, true)));
			}
			builder = Filters.and(builder, Filters.or(
						Filters.eq("owner", user.getUserId()),
						Filters.elemMatch("inheritedShares", Filters.or(groups))));
			return this;
		}

		public DocumentQueryBuilder withOwnerIds(Collection<String> ids) {
			builder = Filters.and(builder, Filters.in("owner", ids));
			return this;
		}

		public DocumentQueryBuilder withTrasherId(String ids) {
			builder = Filters.and(builder, Filters.eq("trasher", ids));
			return this;
		}

		public DocumentQueryBuilder withBeingShared(Boolean isShared) {
			// if is shared does not exists => notEquals
			if (isShared) {
				builder = Filters.and(builder, Filters.eq("isShared", true));
			} else {
				builder = Filters.and(builder, Filters.ne("isShared", true));
			}
			return this;
		}

		public DocumentQueryBuilder withActionNotExistingInInheritedShared(UserInfos user, String action) {
			builder = Filters.and(builder,
					Filters.elemMatch("inheritedShares",
							Filters.and(Filters.ne("userId", user.getUserId()), Filters.eq(action, true))));
			return this;
		}

		public DocumentQueryBuilder withActionExistingInInheritedShared(UserInfos user, String action) {
			List<Bson> groups = new ArrayList<>();
			groups.add(Filters.and(Filters.eq("userId", user.getUserId()), Filters.eq(action, true)));
			for (String gpId : user.getGroupsIds()) {
				groups.add(Filters.and(Filters.eq("groupId", gpId), Filters.eq(action, true)));
			}
			Bson subQuery = Filters.or(groups);
			builder = Filters.and(builder, Filters.or(
						Filters.eq("owner", user.getUserId()),
						Filters.elemMatch("inheritedShares", subQuery)));
			return this;
		}

		public DocumentQueryBuilder withActionExistingInInheritedSharedWithOrVisiblities(UserInfos user, String action, Set<String> visibilities) {
			final List<Bson> ors = new ArrayList<>();
			final List<Bson> groups = new ArrayList<>();
			//owner
			ors.add(Filters.eq("owner", user.getUserId()));
			//inherit shares
			groups.add(Filters.and(Filters.eq("userId", user.getUserId()), Filters.eq(action, true)));
			for (String gpId : user.getGroupsIds()) {
				groups.add(Filters.and(Filters.eq("groupId", gpId), Filters.eq(action, true)));
			}
			Bson subQuery = Filters.or(groups);
			ors.add(Filters.elemMatch("inheritedShares", subQuery));
			//visibilities
			for (String visibility : visibilities) {
				ors.add(Filters.eq(visibility, true));
			}
			//
			builder = Filters.and(builder, Filters.or(ors));
			return this;
		}

		public DocumentQueryBuilder withActionExistingInShared(UserInfos user, String action) {
			List<Bson> groups = new ArrayList<>();
			groups.add(Filters.and(Filters.eq("userId", user.getUserId()), Filters.eq(action, true)));
			for (String gpId : user.getGroupsIds()) {
				groups.add(Filters.and(Filters.eq("groupId", gpId), Filters.eq(action, true)));
			}
			Bson subQuery = Filters.or(groups);
			builder = Filters.and(builder, Filters.or(
						Filters.eq("owner", user.getUserId()), //
						Filters.elemMatch("shared", subQuery)));
			return this;
		}

		public DocumentQueryBuilder withParent(String id) {
			builder = Filters.and(builder, Filters.eq("eParent", id));
			parentIdsFilter.add(id);
			return this;
		}

		public DocumentQueryBuilder withParent(Collection<String> id) {
			builder = Filters.and(builder, Filters.in("eParent", id));
			parentIdsFilter.addAll(id);
			return this;
		}

		public DocumentQueryBuilder withIdNotEq(String id) {
			builder = Filters.and(builder, Filters.ne("_id", id));
			return this;
		}

		public DocumentQueryBuilder withId(String id) {
			builder = Filters.and(builder, Filters.eq("_id", id));
			return this;
		}

		public DocumentQueryBuilder withId(Collection<String> id) {
			builder = Filters.and(builder, Filters.in("_id", id));
			return this;
		}

		public DocumentQueryBuilder withKeyValue(String key, Object value) {
			builder = Filters.and(builder, Filters.eq(key, value));
			return this;
		}

		public DocumentQueryBuilder withKeyValueNotEq(String key, Object value) {
			builder = Filters.and(builder, Filters.ne(key, value));
			return this;
		}

		public DocumentQueryBuilder withIds(Collection<String> ids) {
			builder = Filters.and(builder, Filters.in("_id", ids));
			return this;
		}

		public DocumentQueryBuilder withExcludeDeleted() {
			excludeDeleted = true;
			builder = Filters.and(builder, Filters.ne("deleted", true));
			return this;
		}

		public DocumentQueryBuilder withOnlyDeleted() {
			onlyDeleted = true;
			builder = Filters.and(builder, Filters.eq("deleted", true));
			return this;
		}

		public DocumentQueryBuilder withFileType(final String type) {
			builder = Filters.and(builder, Filters.eq("eType", type));
			return this;
		}

		public DocumentQueryBuilder withNameMatch(final String pattern) {
			builder = Filters.and(builder, new Document("name", new Document("$regex", "^" + pattern + "(_|$)")));
			return this;
		}

		public DocumentQueryBuilder withNameStarts(final String pattern) {
			builder = Filters.and(builder, new Document("name", new Document("$regex", "^" + pattern)));
			return this;
		}

		public DocumentQueryBuilder withNameEq(final String name) {
			builder = Filters.and(builder, Filters.eq("name", name));
			return this;
		}

		public boolean isExcludeDeleted() {
			return excludeDeleted;
		}

		public boolean isOnlyDeleted() {
			return onlyDeleted;
		}

		public Bson build() {
			return builder;
		}
	}

	public DocumentQueryBuilder queryBuilder() {
		return new DocumentQueryBuilder();
	}

	@Deprecated
	protected Future<List<String>> getChildrenIdsRecursively(DocumentQueryBuilder parentFilter,
			Optional<DocumentQueryBuilder> queryChildren, boolean includeParents) {
		JsonObject projection = new JsonObject().put("_id", 1).put("children", "$children._id");
		@SuppressWarnings("unchecked")
		Optional<JsonObject> query = queryChildren.map(q -> new JsonObject(q.build().toBsonDocument().toJson()));
		AggregationsBuilder agg = AggregationsBuilder.startWithCollection(this.collection)//
				.withMatch(parentFilter.build())//
				.withGraphLookup("$_id", "_id", "eParent", "children", Optional.of(MAX_DEPTH), Optional.empty(), query)//
				.withProjection(projection)//
				.withAllowDiskUse(true);//#24499 allow disk use for big folder tree
		JsonObject command = agg.getCommand();
		Promise<List<String>> future = Promise.promise();
		mongo.aggregateBatched(collection, command, MAX_BATCH, message -> {
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
		return future.future();
	}

	private class FolderSubtreeIds{
		final Set<String> rootIds = new HashSet<String>();
		final Set<String> childrenIds = new HashSet<String>();
		boolean isEmptyRoots(){ return this.rootIds.isEmpty(); }
		boolean isEmptyChildrens(){ return this.childrenIds.isEmpty(); }
		boolean isEmpty(){ return this.isEmptyRoots() && this.isEmptyChildrens(); }
		Set<String> getAllFolderIds(){
			final Set<String> all = new HashSet<String>(this.rootIds);
			all.addAll(this.childrenIds);
			return all;
		}
	}

	protected Future<FolderSubtreeIds> getChildrenFoldersIds(DocumentQueryBuilder parentFilter,
														Optional<DocumentQueryBuilder> queryChildren){
		final JsonObject projection = new JsonObject().put("_id", 1).put("children", "$children._id");
		final DocumentQueryBuilder queryChildrenBuild = queryChildren.orElse(queryBuilder()).withFileType(FolderManager.FOLDER_TYPE);
		final Optional<JsonObject> queryChildrenOpt = Optional.of(new JsonObject(queryChildrenBuild.build().toBsonDocument().toJson()));
		AggregationsBuilder agg = AggregationsBuilder.startWithCollection(this.collection)//
				.withMatch(parentFilter.build())//
				.withGraphLookup("$_id", "_id", "eParent", "children", Optional.of(MAX_DEPTH), Optional.empty(), queryChildrenOpt)//
				.withProjection(projection)//
				.withAllowDiskUse(true);//#24499 allow disk use for big folder tree
		JsonObject command = agg.getCommand();
		Promise<FolderSubtreeIds> future = Promise.promise();
		mongo.aggregateBatched(collection, command, MAX_BATCH, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				JsonArray results = (body.getJsonObject("result", new JsonObject())
						.getJsonObject("cursor", new JsonObject()).getJsonArray("firstBatch"));
				final FolderSubtreeIds returned = new FolderSubtreeIds();
				for (int i = 0; i < results.size(); i++) {
					JsonObject result = results.getJsonObject(i);
					returned.rootIds.add(result.getString("_id"));
					JsonArray children = result.getJsonArray("children", new JsonArray());
					for (int j = 0; j < children.size(); j++) {
						String child = children.getString(j);
						returned.childrenIds.add(child);
					}
				}
				future.complete(returned);
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future.future();
	}

	protected Future<List<String>> getChildrenIdsRecursively_NEW(DocumentQueryBuilder parentFilter,
												   Optional<DocumentQueryBuilder> queryChildren, boolean includeParents) {
		return getChildrenFoldersIds(parentFilter.copy(), queryChildren.map(e->e.copy())).compose(treeResult->{
			if(treeResult.isEmpty()){
				return Future.succeededFuture(new ArrayList<>());
			} else {
				final Set<String> folderIds = treeResult.getAllFolderIds();
				final DocumentQueryBuilder childQuery = queryChildren.orElse(queryBuilder()).withFileType(FolderManager.FILE_TYPE).withParent(folderIds);
				return findAllAsList(childQuery.withProjection("_id")).map(all->{
					final List<String> allIds = new ArrayList<>(folderIds);
					for(Object doc : all){
						final JsonObject jsonDoc = (JsonObject)doc;
						allIds.add(jsonDoc.getString("_id"));
					}
					if(!includeParents){
						allIds.removeAll(treeResult.rootIds);
					}
					return allIds;
				});
			}
		});
	}

	protected Future<List<JsonObject>> getChildrenRecursively_NEW(DocumentQueryBuilder parentFilter,
													Optional<DocumentQueryBuilder> queryChildren, boolean includeParents, Set<String> projection) {
		// impossible de charger les objets sous forme d arbre => stackoverflow ->
		// charger ids
		return getChildrenFoldersIds(parentFilter.copy(), queryChildren.map(e->e.copy())).compose(treeResult->{
			if(treeResult.isEmpty()){
				return Future.succeededFuture(new ArrayList<>());
			} else {
				final List<Future> futures = new ArrayList<Future>();
				{//fetch folder tree
					final Set<String> ids = includeParents? treeResult.getAllFolderIds() : treeResult.childrenIds;
					if(ids.size() > 0){
						final DocumentQueryBuilder queryParent = queryBuilder().withId(ids).withProjections(projection);
						futures.add(findAllAsList(queryParent));
					}
				}
				{//fetch document tree
					final Set<String> parentIds = treeResult.getAllFolderIds();
					if(parentIds.size() > 0){
						final DocumentQueryBuilder childQuery = queryChildren.orElse(queryBuilder()).withFileType(FolderManager.FILE_TYPE).withParent(parentIds);
						futures.add(findAllAsList(childQuery.withProjections(projection)));
					}
				}
				return CompositeFuture.all(futures).map(all->{
					final List<JsonObject> allIds = new ArrayList<>();
					for(Object ids : all.list()){
						allIds.addAll((List<JsonObject>)ids);
					}
					return allIds;
				});
			}
		});
	}

	@Deprecated
	protected Future<List<JsonObject>> getChildrenRecursively_OLD(DocumentQueryBuilder parentFilter,
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


	Future<List<JsonObject>> getChildrenRecursively(DocumentQueryBuilder parentFilter,
													Optional<DocumentQueryBuilder> queryChildren, boolean includeParents) {
		return getChildrenRecursively(parentFilter, queryChildren, includeParents, null);
	}

	Future<List<JsonObject>> getChildrenRecursively(DocumentQueryBuilder parentFilter,
				Optional<DocumentQueryBuilder> queryChildren, boolean includeParents, Set<String> projection) {
		if(useOldChildrenQuery){
			return getChildrenRecursively_OLD(parentFilter, queryChildren, includeParents);
		}else{
			return getChildrenRecursively_NEW(parentFilter, queryChildren, includeParents, projection);
		}
	}

	@Deprecated
	protected Future<JsonArray> listHierarchical_OLD(DocumentQueryBuilder query) {

		Promise<JsonArray> future = Promise.promise();
		// match all (folders and file)
		Bson match = query.build();
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
		mongo.aggregateBatched(collection, command, MAX_BATCH, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(body.getJsonObject("result", new JsonObject()).getJsonObject("cursor", new JsonObject())
						.getJsonArray("firstBatch", new JsonArray()));
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future.future();
	}

	Future<JsonArray> listHierarchical(DocumentQueryBuilder query) {
		if(query.hasParentIdFilter()){
			return listHierarchical_OLD(query);
		}
		return findAll(query);
	}

	Future<JsonObject> findById(String id) {
		Promise<JsonObject> future = Promise.promise();
		mongo.findOne(collection, toJson(Filters.eq("_id", id)), message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(body.getJsonObject("result"));
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future.future();
	}

	Future<JsonObject> findOne(DocumentQueryBuilder query) {
		Promise<JsonObject> future = Promise.promise();
		mongo.findOne(collection, toJson(query.build()), message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(body.getJsonObject("result"));
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future.future();
	}

	public Future<List<JsonObject>> findAllAsList(DocumentQueryBuilder query) {
		return findAll(query).map(s -> s.stream().map(o -> (JsonObject) o).collect(Collectors.toList()));
	}

	Future<Integer> countAll(DocumentQueryBuilder query) {
		Promise<Integer> future = Promise.promise();
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
		return future.future();
	}

	Future<JsonArray> findAll(DocumentQueryBuilder query) {
		Promise<JsonArray> future = Promise.promise();
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
		return future.future();
	}

	Future<JsonObject> insert(JsonObject file) {
		Promise<JsonObject> future = Promise.promise();
		mongo.insert(collection, file, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(file.put("_id", body.getString("_id")));
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future.future();
	}

	Future<JsonObject> upsertFolder(JsonObject folder) {
		Promise<JsonObject> future = Promise.promise();
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
		return future.future();
	}

	Future<List<JsonObject>> insertAll(List<JsonObject> files) {
		// TODO bulk insert
		List<Future<?>> futures = new ArrayList<>();
		for (JsonObject json : files) {
			futures.add(insert(json));
		}
		return Future.all(futures).map(CompositeFuture::list);
	}

	Future<JsonObject> updateInheritShares(JsonObject file) {
		Promise<JsonObject> future = Promise.promise();
		String id = file.getString("_id");
		String now = MongoDb.formatDate(new Date());
		JsonArray inheritShared = file.getJsonArray("inheritedShares");
		JsonArray ancestors = file.getJsonArray("ancestors");
		JsonObject set = new MongoUpdateBuilder().set("inheritedShares", inheritShared)
				.set("isShared", inheritShared != null && !inheritShared.isEmpty())//
				.set("ancestors", ancestors)//
				.set("modified", now).build();
		mongo.update(collection, toJson(Filters.eq("_id", id)), set, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(file);
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future.future();
	}

	Future<Void> updateMove(String sourceId, String destinationFolderId) {
		String now = MongoDb.formatDate(new Date());
		MongoUpdateBuilder set = new MongoUpdateBuilder().set("modified", now).set("eParent", destinationFolderId)
				.unset("eParentOld");
		return update(sourceId, set);
	}

	Future<Void> update(String id, JsonObject set) {
		Promise<Void> future = Promise.promise();
		mongo.update(collection, toJson(Filters.eq("_id", id)), set, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(null);
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future.future();
	}

	Future<Void> update(String id, MongoUpdateBuilder set) {
		Promise<Void> future = Promise.promise();
		String now = MongoDb.formatDate(new Date());
		set.set("modified", now);
		mongo.update(collection, toJson(Filters.eq("_id", id)), set.build(), message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(null);
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future.future();
	}

	Future<Void> updateAll(Set<String> id, MongoUpdateBuilder set) {
		return updateAll(id, set, true);
	}
	
	Future<Void> updateAll(Set<String> id, MongoUpdateBuilder set, boolean setModified) {
		if (id.isEmpty()) {
			return Future.succeededFuture();
		}
		Promise<Void> future = Promise.promise();
		String now = MongoDb.formatDate(new Date());
		if(setModified){
			set.set("modified", now);
		}
		JsonObject query = toJson(Filters.in("_id", id));
		JsonObject setJson = set.build();
		mongo.update(collection, query, setJson, false, true, message -> {
			JsonObject body = message.body();
			if (isOk(body)) {
				future.complete(null);
			} else {
				future.fail(toErrorStr(body));
			}
		});
		return future.future();
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
		res.getAll().stream().map(o -> (JsonObject) o).forEach(row -> {
			JsonObject set = new MongoUpdateBuilder()//
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
		Promise<Void> future = Promise.promise();
		mongo.bulk(collection, operations, bulkEv -> {
			if (isOk(bulkEv.body())) {
				future.complete((null));
			} else {
				future.fail(toErrorStr(bulkEv.body()));
			}
		});
		return future.future();
	}

	Future<Void> deleteByIds(Set<String> ids) {
		if (ids.isEmpty()) {
			return Future.succeededFuture();
		}
		Promise<Void> future = Promise.promise();
		mongo.delete(collection, toJson(Filters.in("_id", ids)), res -> {
			if (isOk(res.body())) {
				future.complete(null);
			} else {
				future.fail(toErrorStr(res.body()));
			}
		});
		return future.future();
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
			final List<Future> futures = new ArrayList<>();
			for (JsonObject doc : docs) {
				String id = DocumentHelper.getId(doc);
				String parent = DocumentHelper.getParent(doc);
				String oldParent = DocumentHelper.getParentOld(doc);
				final JsonArray shared = doc.getJsonArray("shared", new JsonArray());
				final JsonArray inheritedShares = doc.getJsonArray("inheritedShares", new JsonArray());
				final JsonArray mergedShares = InheritShareComputer.concatShares(inheritedShares, shared);
				// if oldparent is not in my virtual tree=> remove parent
				if (oldParent != null) {
					if (parentIdsOk.contains(oldParent) || treeIds.contains(oldParent)) {
						idsToRename.add(id);
					} else {
						idsToRemoveParent.add(id);
						// doc is moved to root => shared = inheritedshares
						if(inheritedShares.size() > 0){
							futures.add(update(id, new MongoUpdateBuilder().set("shared", mergedShares)));
						}
					}
				} else if(parent != null){
					if (parentIdsOk.contains(parent) || treeIds.contains(parent)) {
						//do nothing
					} else {
						idsToRemoveParent.add(id);
						// doc is moved to root => shared = inheritedshares
						if(inheritedShares.size() > 0){
							futures.add(update(id, new MongoUpdateBuilder().set("shared", mergedShares)));
						}
					}
				}//else do nothing
			}
			futures.add(updateAll(idsToRename,
					new MongoUpdateBuilder().rename("eParentOld", "eParent")));
			futures.add(updateAll(idsToRemoveParent,
					new MongoUpdateBuilder().unset("eParentOld").unset("eParent")));
			return CompositeFuture.all(futures).mapEmpty();
		});
	}

	public Future<Void> breakParentLink(Set<String> ids) {
		if (ids.isEmpty()) {
			return Future.succeededFuture();
		}
		return updateAll(ids, new MongoUpdateBuilder().rename("eParent", "eParentOld"), false);
	}

	public Future<Void> restoreParentLink(RestoreParentDirection dir, Collection<JsonObject> all) {
		Set<String> eParentOlds = all.stream().map(o -> {
			if(StringUtils.isEmpty(DocumentHelper.getParentOld(o))){
				return DocumentHelper.getParent(o);
			} else{
				return DocumentHelper.getParentOld(o);
			}
		}).filter(pa -> !StringUtils.isEmpty(pa)).collect(Collectors.toSet());
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
