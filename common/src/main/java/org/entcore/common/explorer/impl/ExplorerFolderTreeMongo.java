package org.entcore.common.explorer.impl;

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import org.bson.conversions.Bson;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.user.UserInfos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
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
    protected Date getCreatedAtForModel(final JsonObject json) {
        final Object value = json.getValue(getCreatedAtColumn());
        if(value != null && value instanceof JsonObject){
            return MongoDb.parseIsoDate((JsonObject) value);
        }
        // return a default value => application should override it if createdAt field is specific
        return new Date();
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
    protected void doFetchForIndex(final ExplorerStream<JsonObject> stream, final Date from, final Date to) {
        final Set<Bson> indexFilters = new HashSet<>();
        if (from != null || to != null) {
            if (from != null) {
                final LocalDateTime localFrom = Instant.ofEpochMilli(from.getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                indexFilters.add(Filters.gte(getCreatedAtColumn(), toMongoDate(localFrom)));
            }
            if (to != null) {
                final LocalDateTime localTo = Instant.ofEpochMilli(to.getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                indexFilters.add(Filters.lt(getCreatedAtColumn(), toMongoDate(localTo)));
            }
        }
        final JsonObject queryJson = MongoQueryBuilder.build(Filters.and(indexFilters));
        mongoClient.findBatch(getCollectionName(),queryJson).handler(result -> {
            stream.add(result);
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
