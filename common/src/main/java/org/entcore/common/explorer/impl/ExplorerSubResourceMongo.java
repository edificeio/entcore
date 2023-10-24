package org.entcore.common.explorer.impl;

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.MongoClient;
import org.bson.conversions.Bson;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.explorer.IngestJobStateUpdateMessage;
import org.entcore.common.explorer.to.ExplorerReindexSubResourcesRequest;
import org.entcore.common.user.UserInfos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Assuming that {@code TSub} is the name of the class of your plugin and {@code TXXX} the name of the classes depending on
 * type TSub (like {@code TExplorerPlugin} who would be the name of the parent plugin handling resources of T), here is
 * the archetypal way of implementing this abstract class.
 * <pre>{@code public class TSubExplorerPlugin extends ExplorerSubResourceMongo {
 *     public static final String COLLECTION = "name_of_the_sub_resource_collection";
 *     private static final Logger log = LoggerFactory.getLogger(TSubExplorerPlugin.class);
 *
 *     public TSubExplorerPlugin(final TExplorerPlugin plugin) {
 *         super(plugin, plugin.getMongoClient());
 *     }
 *
 *     @Override
 *     protected Optional<UserInfos> getCreatorForModel(final JsonObject json) {
 *         // Implement here the logic of how to get a UserInfos out of a record of T from the database
 *         if(!json.containsKey("author") || !json.getJsonObject("author").containsKey("userId")){
 *             return Optional.empty();
 *         }
 *         final JsonObject author = json.getJsonObject("author");
 *         final UserInfos user = new UserInfos();
 *         user.setUserId( author.getString("userId"));
 *         user.setUsername(author.getString("username"));
 *         user.setLogin(author.getString("login"));
 *         return Optional.of(user);
 *     }
 *
 *     @Override
 *     public Future<Void> onDeleteParent(final Collection<String> ids) {
 *         if(ids.isEmpty()) {
 *             return Future.succeededFuture();
 *         }
 *         final MongoClient mongo = ((TExplorerPlugin)super.parent).getMongoClient();
 *         final JsonObject filter = MongoQueryBuilder.build(QueryBuilder.start(getParentColumn()).in(ids));
 *         final Promise<MongoClientDeleteResult> promise = Promise.promise();
 *         log.info("Deleting TSubs related to deleted blog. Number of TSub="+ids.size());
 *         mongo.removeDocuments(COLLECTION, filter, promise);
 *         return promise.future().map(e->{
 *             log.info("Deleted TSubs related to deleted T. Number of TSub="+e.getRemovedCount());
 *             return null;
 *         });
 *     }
 *
 *     @Override
 *     public String getEntityType() {
 *         return "tsub_entity_type"; // TODO Change this to reflect the name of your subresource, it should be
 *                                    // the name of TSub in lowercase
 *     }
 *
 *     @Override
 *     protected String getParentId(JsonObject jsonObject) {
 *         final JsonObject parentRef = jsonObject.getJsonObject("T"); // TODO change this to reflect TSub's schema
 *         return parentRef.getString("$id");
 *     }
 *
 *
 *     @Override
 *     protected Future<ExplorerMessage> doToMessage(final ExplorerMessage message, final JsonObject source) {
 *         final String id = source.getString("_id");
 *         // TODO Implement here the way you want to put data in ExplorerMessage from your database object
 *         // with successive calls like message.withSubResourceHtml(id, source.getString("XXX", ""), source.getLong("version", 0L));
 *         message.withSubResourceHtml(id, source.getString("content",""), source.getLong("version", 0L)); // TODO change
 *         return Future.succeededFuture(message);
 *     }
 *
 *     @Override
 *     protected String getCollectionName() { return COLLECTION; }
 *
 *     protected String getParentColumn() {
 *         return "blog.$id"; // TODO change to reflect TSub's schema
 *     }
 *
 * }
 * }</pre>
 */
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
    protected void doFetchForIndex(final ExplorerStream<JsonObject> stream, final ExplorerReindexSubResourcesRequest request) {
        final Set<Bson> indexFilters = new HashSet<>();
        final Date from = request.getFrom();
        final Date to = request.getTo();
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
        if(request.getIds() != null && !request.getIds().isEmpty()) {
            indexFilters.add(Filters.in(getIdColumn(), request.getIds()));
        }
        if(request.getParentIds() != null && !request.getParentIds().isEmpty()) {
            indexFilters.add(Filters.in(getParentColumn(), request.getIds()));
        }
        final JsonObject queryJson = MongoQueryBuilder.build(Filters.and(indexFilters));
        mongoClient.findBatch(getCollectionName(),queryJson).handler(result -> {
            stream.add(Arrays.asList(result));
        }).endHandler(e-> stream.end());
    }

    protected String getIdColumn() { return "_id"; }
    protected abstract String getParentColumn();

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
