package org.entcore.common.explorer.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.user.UserInfos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

public abstract class ExplorerSubResourceMongo extends ExplorerSubResource{
    protected final MongoClient mongoClient;

    protected ExplorerSubResourceMongo(final ExplorerPlugin parent, final MongoClient mongoClient) {
        super(parent);
        this.mongoClient = mongoClient;
    }

    @Override
    protected String getChildId(final JsonObject source) {
        return source.getValue(getIdColumn()).toString();
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
        int i = 1;
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

    protected String getIdColumn() { return "_id"; }

    protected String getCreatedAtColumn() { return "createdAt"; }

    protected String getCreatorIdColumn() { return "creatorId"; }

    protected String getCreatorNameColumn() {
        return "creatorName";
    }

    protected Object toMongoDate(final LocalDateTime date) {
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }
    //abstract
    protected abstract String getCollectionName();
}
