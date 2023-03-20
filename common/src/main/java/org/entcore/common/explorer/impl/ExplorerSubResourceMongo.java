package org.entcore.common.explorer.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.MongoClient;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.explorer.IngestJobStateUpdateMessage;
import org.entcore.common.user.UserInfos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class ExplorerSubResourceMongo extends ExplorerSubResource {
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
    protected Date getCreatedAtForModel(final JsonObject json) {
        final Object value = json.getValue(getCreatedAtColumn());
        if(value != null && value instanceof JsonObject){
            return MongoDb.parseIsoDate((JsonObject) value);
        }
        // return a default value => application should override it if createdAt field is specific
        return new Date();
    }

    @Override
    protected Optional<UserInfos> getCreatorForModel(final JsonObject json) {
        if(!json.containsKey(getCreatorIdColumn())){
            return Optional.empty();
        }
        final String id = json.getString(getCreatorIdColumn());
        final String name = json.getString(getCreatorNameColumn());
        final UserInfos user = new UserInfos();
        user.setUserId(id);
        user.setUsername(name);
        return Optional.ofNullable(user);
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

    @Override
    public Future<Void> onJobStateUpdatedMessageReceived(final List<IngestJobStateUpdateMessage> messages) {
        final List<BulkOperation> operations = messages.stream().map(message -> {
            final JsonObject filter = new JsonObject()
                    .put("_id", message.getEntityId())
                    .put("version", new JsonObject().put("$lte", message.getVersion()));
            final JsonObject update = new JsonObject()
                    .put("$set", new JsonObject()
                            .put("ingest_job_state", message.getState().name())
                            .put("version", message.getVersion())
                    );
            return BulkOperation.createUpdate(filter, update);
        }).collect(Collectors.toList());
        final Promise<Void> promise = Promise.promise();
        mongoClient.bulkWrite(getCollectionName(), operations, asyncResult -> {
            if(asyncResult.succeeded()) {
                log.debug("Update successul of " + messages.size() + " messages") ;
                promise.complete();
            } else {
                log.error("Update error of " + messages +" : \n" + asyncResult.cause());
                promise.fail(asyncResult.cause());
            }
        });
        return promise.future();
    }
}
