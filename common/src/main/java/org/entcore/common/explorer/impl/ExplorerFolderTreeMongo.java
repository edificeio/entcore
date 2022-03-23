package org.entcore.common.explorer.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ExplorerFolderTreeMongo extends ExplorerFolderTree{
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final MongoClient mongoClient;

    protected ExplorerFolderTreeMongo(final ExplorerPlugin parent, final MongoClient mongoClient) {
        super(parent);
        this.mongoClient = mongoClient;
    }

    @Override
    protected String getFolderId(final JsonObject source) {
        return source.getString("_id");
    }

    @Override
    protected String getName(final JsonObject source) {
        return source.getString("name");
    }

    @Override
    protected Optional<String> getParentId(final JsonObject source) {
        final String parentId = source.getString("parentId");
        return Optional.ofNullable(parentId);
    }

    @Override
    protected boolean isTrashed(final JsonObject source) {
        return source.getBoolean("trashed", false);
    }

    @Override
    protected Set<String> getResourceIds(final JsonObject source) {
        final JsonArray ressourceIds = source.getJsonArray("ressourceIds", new JsonArray());
        return ressourceIds.stream().map(e-> e.toString()).collect(Collectors.toSet());
    }

    @Override
    protected UserInfos getCreatorForModel(final JsonObject json) {
        final JsonObject owner = json.getJsonObject("owner", new JsonObject());
        final String id = owner.getString("userId");
        final String name = owner.getString("displayName");
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

    protected String getCreatedAtColumn() { return "created"; }

    protected Object toMongoDate(final LocalDateTime date) {
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    //abstract
    protected abstract String getCollectionName();

}
