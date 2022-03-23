package org.entcore.common.explorer.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.explorer.IExplorerPluginCommunication;
import org.entcore.common.user.UserInfos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public abstract class ExplorerPluginResourceMongo extends ExplorerPluginResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final MongoClient mongoClient;

    protected ExplorerPluginResourceMongo(final IExplorerPluginCommunication communication, final MongoClient mongoClient) {
        super(communication);
        this.mongoClient = mongoClient;
    }

    @Override
    protected String getIdForModel(final JsonObject json) {
        return json.getValue(getIdColumn()).toString();
    }

    @Override
    protected JsonObject setIdForModel(final JsonObject json, final String id) {
        json.put(getIdColumn(), id);
        return json;
    }

    @Override
    protected UserInfos getCreatorForModel(final JsonObject json) {
        final String id = json.getString(getCreatorIdColumn());
        final String name = json.getString(getCreatorNameColumn());
        final UserInfos user = new UserInfos();
        user.setUserId(id);
        user.setUsername(name);
        return user;
    }

    @Override
    protected void doFetchForIndex(final ExplorerStream<JsonObject> stream, final Optional<Date> from, final Optional<Date> to) {
        final QueryBuilder query = QueryBuilder.start();
        if (from.isPresent() || to.isPresent()) {
            if (from.isPresent()) {
                final LocalDateTime localFrom = Instant.ofEpochMilli(from.get().getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                query.and(getCreatedAtColumn()).greaterThanEquals(toMongoDate(localFrom));
            }
            if (to.isPresent()) {
                final LocalDateTime localTo = Instant.ofEpochMilli(to.get().getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                query.and(getCreatedAtColumn()).lessThan(toMongoDate(localTo));
            }
        }
        final JsonObject queryJson = MongoQueryBuilder.build(query);
        mongoClient.findBatch(getCollectionName(),queryJson).handler(result -> {
            stream.add(Arrays.asList(result));
        }).endHandler(e->{
            stream.end();
        });
    }

    @Override
    protected Future<List<String>> doCreate(final UserInfos user, final List<JsonObject> sources, final boolean isCopy) {
        final List<Future> futures = new ArrayList<>();
        final List<String> ids = new ArrayList<>();
        for(final JsonObject json : sources){
            final String id = UUID.randomUUID().toString();
            ids.add(id);
            json.put(getIdColumn(), id);
            setCreatorForModel(user, json);
            final Promise<String> promise = Promise.promise();
            futures.add(promise.future());
            mongoClient.insert(getCollectionName(), json, promise);
        }
        return CompositeFuture.all(futures).map(ids);
    }

    @Override
    protected Future<List<Boolean>> doDelete(final UserInfos user, final List<String> ids) {
        final JsonObject query = MongoQueryBuilder.build(QueryBuilder.start(getIdColumn()).in(ids));
        final Promise<MongoClientDeleteResult> promise = Promise.promise();
        mongoClient.removeDocuments(getCollectionName(), query , promise);
        return promise.future().map(e->{
            final List<Boolean> all = new ArrayList<>();
            for(final String id : ids){
                all.add(true);
            }
            return all;
        });
    }

    public Future<List<JsonObject>> getByIds(final Set<String> ids) {
        if (ids.isEmpty()) {
            return Future.succeededFuture(new ArrayList<>());
        }
        final Promise<List<JsonObject>> future = Promise.promise();
        final JsonObject query = MongoQueryBuilder.build(QueryBuilder.start(getIdColumn()).in(ids));
        mongoClient.find(getCollectionName(),query, future);
        return future.future();
    }

    //overridable
    protected void setCreatorForModel(final UserInfos user, final JsonObject json){
        json.put(getCreatorIdColumn(), user.getUserId());
        json.put(getCreatorNameColumn(), user.getUsername());
    }

    protected String getCreatedAtColumn() { return "createdAt"; }

    protected String getCreatorIdColumn() { return "creatorId"; }

    protected String getCreatorNameColumn() {
        return "creatorName";
    }

    protected String getIdColumn() { return "_id"; }

    protected Object toMongoId(final String id) { return id; }

    protected Object toMongoDate(final LocalDateTime date) {
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    //abstract
    protected abstract String getCollectionName();
}
