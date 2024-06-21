package org.entcore.common.explorer.impl;

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import org.bson.conversions.Bson;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.explorer.IExplorerPluginCommunication;
import org.entcore.common.explorer.IngestJobState;
import org.entcore.common.explorer.IngestJobStateUpdateMessage;
import org.entcore.common.explorer.to.ExplorerReindexResourcesRequest;
import org.entcore.common.user.UserInfos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * Assuming that {@code T} is the name of the class of your plugin and {@code TXXX} the name of the classes depending on
 * type T (like {@code TSubExplorerPlugin} who would be the name of the plugin handling a sub resource of T), here is
 * the archetypal way of implementing this abstract class.
 * <pre>{@code
 * public class TExplorerPlugin extends ExplorerPluginResourceMongo {
 *     public static final String APPLICATION = T.APPLICATION;
 *     public static final String TYPE = T.TYPE;
 *     public static final String COLLECTION = T.T_COLLECTION;
 *     static Logger log = LoggerFactory.getLogger(TExplorerPlugin.class);
 *     private final MongoClient mongoClient;
 *     private final TFoldersExplorerPlugin folderPlugin;
 *     private final TSubExplorerPlugin tSubPlugin;
 *     private ShareService shareService;
 *     private final Map<String, SecuredAction> securedActions;
 *
 *     public static TExplorerPlugin create(final Map<String, SecuredAction> securedActions) throws Exception {
 *         final IExplorerPlugin plugin = ExplorerPluginFactory.createMongoPlugin((params)->{
 *             return new TExplorerPlugin(params.getCommunication(), params.getDb(), securedActions);
 *         });
 *         return (TExplorerPlugin) plugin;
 *     }
 *
 *     public TExplorerPlugin(final IExplorerPluginCommunication communication, final MongoClient mongoClient, final Map<String, SecuredAction> securedActions) {
 *         super(communication, mongoClient);
 *         this.mongoClient = mongoClient;
 *         this.securedActions = securedActions;
 *         //init folder plugin
 *         this.folderPlugin = new TFoldersExplorerPlugin(this);
 *         //init subresource plugin
 *         this.postPlugin = new TSubExplorerPlugin(this);
 *     }
 *
 *     public TSubExplorerPlugin tSubPlugin(){ return tSubPlugin; }
 *
 *     public TFoldersExplorerPlugin folderPlugin(){ return folderPlugin; }
 *
 *     public MongoClient getMongoClient() {return mongoClient;}
 *
 *     public ShareService createShareService(final Map<String, List<String>> groupedActions) {
 *         this.shareService = createMongoShareService(T.T_COLLECTION, securedActions, groupedActions);
 *         return this.shareService;
 *     }
 *
 *     @Override
 *     protected Optional<ShareService> getShareService() {
 *         return Optional.ofNullable(shareService);
 *     }
 *
 *     @Override
 *     protected String getApplication() { return APPLICATION; }
 *
 *     @Override
 *     protected String getResourceType() { return TYPE; }
 *
 *     @Override
 *     protected Future<ExplorerMessage> doToMessage(final ExplorerMessage message, final JsonObject source) {
 *         // Implement here the way you want to put data in ExplorerMessage from your database object
 *         // with successive calls like message.withXXX(source.getString("XXX", ""));
 *         return Future.succeededFuture(message);
 *     }
 *
 *     @Override
 *     public Map<String, SecuredAction> getSecuredActions() {
 *         return securedActions;
 *     }
 *
 *     @Override
 *     protected String getCollectionName() { return COLLECTION; }
 *
 *     @Override
 *     protected String getCreatedAtColumn() {
 *         return "created"; // Or whatever the name of the column is for this resource
 *     }
 *
 *     @Override
 *     public Optional<UserInfos> getCreatorForModel(final JsonObject json) {
 *         // Implement here the logic of how to get a UserInfos out of a record of T from the database
 *         if(!json.containsKey("author") || !json.getJsonObject("author").containsKey("userId")){
 *             return Optional.empty();
 *         }
 *         final JsonObject author = json.getJsonObject("author");
 *         final UserInfos user = new UserInfos();
 *         user.setUserId( author.getString("userId"));
 *         user.setUsername(author.getString("username"));
 *         user.setLogin(author.getString("login"));
 *         return Optional.ofNullable(user);
 *     }
 *
 *     @Override
 *     protected void setCreatorForModel(UserInfos user, JsonObject json) {
 *         // Implement here the logic of how to put the data of a UserInfos into record of T to be stored in the database
 *         final JsonObject author = new JsonObject();
 *         author.put("userId", user.getUserId());
 *         author.put("username", user.getUsername());
 *         author.put("login", user.getLogin());
 *         json.put("author", author);
 *     }
 *
 *     @Override
 *     protected List<ExplorerSubResource> getSubResourcesPlugin() {
 *         return Collections.singletonList(tSubPlugin);
 *     }
 *
 * }
 * }</pre>
 */
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
    protected Date getCreatedAtForModel(final JsonObject json) {
        final Object value = json.getValue(getCreatedAtColumn());
        if(value != null && value instanceof JsonObject){
            return MongoDb.parseIsoDate((JsonObject) value);
        }
        // return a default value => application should override it if createdAt field is specific
        return new Date();
    }

    @Override
    protected void doFetchForIndex(final ExplorerStream<JsonObject> stream, final ExplorerReindexResourcesRequest request) {
        final List<Bson> queryFilters = new ArrayList<>();
        final Date from = request.getFrom();
        final Date to = request.getTo();
        final Set<String> states = request.getStates();
        if(isNotEmpty(request.getIds())) {
            queryFilters.add(Filters.eq(getIdColumn(), request.getIds()));
        }
        if (from != null || to != null) {
            if (from != null) {
                final LocalDateTime localFrom = Instant.ofEpochMilli(from.getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                queryFilters.add(Filters.gte(getCreatedAtColumn(), toMongoDate(localFrom)));
            }
            if (to != null) {
                final LocalDateTime localTo = Instant.ofEpochMilli(to.getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                queryFilters.add(Filters.lt(getCreatedAtColumn(), toMongoDate(localTo)));
            }
        }
        if(isNotEmpty(states)) {
            queryFilters.add(Filters.in(INGEST_JOB_STATE, states));
        }
        final JsonObject queryJson = MongoQueryBuilder.build(Filters.and(queryFilters));
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
            json.put(INGEST_JOB_STATE, IngestJobState.TO_BE_SENT);
            setCreatorForModel(user, json);
            setCreatedAtForModel(user, json);
            final Promise<String> promise = Promise.promise();
            futures.add(promise.future());
	        mongoClient.insert(getCollectionName(), json, promise);
        }
        return CompositeFuture.all(futures).map(ids);
    }

    @Override
    protected Future<List<Boolean>> doDelete(final UserInfos user, final List<String> ids) {
        if(ids.isEmpty()){
            return Future.succeededFuture(new ArrayList<>());
        }
        final JsonObject query = MongoQueryBuilder.build(Filters.in(getIdColumn(), ids));
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
        final JsonObject query = MongoQueryBuilder.build(Filters.in(getIdColumn(), ids));
        mongoClient.find(getCollectionName(),query, future);
        return future.future();
    }

    //overridable
    protected void setCreatorForModel(final UserInfos user, final JsonObject json){
        json.put(getCreatorIdColumn(), user.getUserId());
        json.put(getCreatorNameColumn(), user.getUsername());
    }
    protected void setCreatedAtForModel(final UserInfos user, final JsonObject json){
        if(json.containsKey(getCreatedAtColumn()) && json.getValue(getCreatedAtColumn()) != null){
            return;
        }
        json.put(getCreatedAtColumn(), MongoDb.nowISO());
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

    @Override
    public Future<Void> onJobStateUpdatedMessageReceived(final List<IngestJobStateUpdateMessage> messages) {
        final List<BulkOperation> operations = messages.stream().map(message -> {
            final JsonObject filter = new JsonObject()
                    .put("_id", message.getEntityId())
                    .put("version", new JsonObject().put("$lte", message.getVersion()));
            final JsonObject update = new JsonObject()
                    .put("$set", new JsonObject()
                            .put(INGEST_JOB_STATE, message.getState().name())
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
    public void setIngestJobStateAndVersion(final MongoUpdateBuilder modifier, IngestJobState state, long version) {
        modifier.set(INGEST_JOB_STATE, state.name());
        modifier.set("version", version);
    }
}
