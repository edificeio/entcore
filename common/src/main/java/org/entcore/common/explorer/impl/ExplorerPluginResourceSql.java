package org.entcore.common.explorer.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import static java.lang.Long.parseLong;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import io.vertx.core.CompositeFuture;
import org.apache.commons.collections4.CollectionUtils;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.explorer.IExplorerPluginCommunication;
import org.entcore.common.explorer.IngestJobState;
import org.entcore.common.explorer.IngestJobStateUpdateMessage;
import org.entcore.common.explorer.to.ExplorerReindexResourcesRequest;
import org.entcore.common.postgres.IPostgresClient;
import org.entcore.common.postgres.PostgresClient;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Assuming that {@code T} is the name of the class of your plugin and {@code TXXX} the name of the classes depending on
 * type T, here is the archetypal way of implementing this abstract class.
 * <pre>{@code public class PostExplorerPlugin extends ExplorerSubResourceMongo {
 *     public static final String TYPE = Blog.BLOG_TYPE;
 *     public static final String COLLECTION = Blog.POSTS_COLLECTION;
 *     static Logger log = LoggerFactory.getLogger(PostExplorerPlugin.class);
 *         // Implement here the logic of how to get a UserInfos out of a record of T from the database
 *         // Implement here the logic of how to get a UserInfos out of a record of T from the database
 *
 *     public PostExplorerPlugin(final BlogExplorerPlugin plugin) {
 *         super(plugin, plugin.getMongoClient());
 *     }
 *
 *     @Override
 *     protected Optional<UserInfos> getCreatorForModel(final JsonObject json) {
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
 *         final MongoClient mongo = ((BlogExplorerPlugin)super.parent).getMongoClient();
 *         final JsonObject filter = MongoQueryBuilder.build(QueryBuilder.start(getParentColumn()).in(ids));
 *         final Promise<MongoClientDeleteResult> promise = Promise.promise();
 *         log.info("Deleting post related to deleted blog. Number of blogs="+ids.size());
 *         mongo.removeDocuments(COLLECTION, filter, promise);
 *         return promise.future().map(e->{
 *             log.info("Deleted post related to deleted blog. Number of posts="+e.getRemovedCount());
 *             return null;
 *         });
 *     }
 *
 *     @Override
 *     public String getEntityType() {
 *         return "t_entity_type"; // TODO Change this to reflect the name of your resource, it should be
 *                                 // the name of TSub in lowercase;
 *     }
 *
 *     @Override
 *     protected String getParentId(JsonObject jsonObject) {
 *         final JsonObject blogRef = jsonObject.getJsonObject("t"); // TODO change to reflect T's schema
 *         return blogRef.getString("$id");
 *     }
 *
 *
 *     @Override
 *     protected Future<ExplorerMessage> doToMessage(final ExplorerMessage message, final JsonObject source) {
 *         final String id = source.getString("_id");
 *         // Implement here the way you want to put data in ExplorerMessage from your database object
 *         // with successive calls like message.withXXX(source.getString("XXX", ""));
 *         return Future.succeededFuture(message);
 *     }
 *
 *     @Override
 *     protected String getCollectionName() { return COLLECTION; }
 *
 *     protected String getParentColumn() {
 *         return "T.$id"; // TODO change this to reflect T's schema
 *     }
 *
 * }}</pre>
 */
public abstract class ExplorerPluginResourceSql extends ExplorerPluginResource {
    protected final IPostgresClient pgPool;
    protected List<String> defaultColumns = Arrays.asList("version", INGEST_JOB_STATE);

    protected ExplorerPluginResourceSql(final IExplorerPluginCommunication communication,
                                        final IPostgresClient pool) {
        super(communication);
        this.pgPool = pool;
    }

    @Override
    protected String getIdForModel(final JsonObject json) {
        return json.getValue(getIdColumn()).toString();
    }

    @Override
    protected JsonObject setIdForModel(final JsonObject json, final String id) {
        json.put(getIdColumn(), Integer.valueOf(id));
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
        if(value != null && value instanceof String){
            final LocalDateTime localDate = LocalDateTime.parse((String) value);
            final Date date = Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
            return date;
        }
        // return a default value => application should override it if createdAt field is specific
        return new Date();
    }

    @Override
    protected void doFetchForIndex(final ExplorerStream<JsonObject> stream, final ExplorerReindexResourcesRequest request) {
        final Tuple tuple = Tuple.tuple();
        final StringBuilder query = new StringBuilder();
        if(getShareTableName().isPresent()){
            final String schema = getTableName().split("\\.")[0];
            final String shareTable = getShareTableName().get();
            query.append(" SELECT t.*, ");
            query.append(String.format(" JSON_AGG(ROW_TO_JSON(ROW(member_id,action)::%s.share_tuple)) AS shared, ", schema));
            query.append(" ARRAY_TO_JSON(ARRAY_AGG(group_id)) AS groups ");
            query.append(String.format(" FROM %s AS t ", getTableName()));
            query.append(String.format(" LEFT JOIN %s s ON t.id = s.resource_id ", shareTable));
            query.append(String.format(" LEFT JOIN %s.members ON (member_id = %s.members.id AND group_id IS NOT NULL) ",schema, schema));
        }else{
            query.append(String.format("SELECT * FROM %s ", getTableName()));
        }
        final Date from = request.getFrom();
        final Date to = request.getTo();
        final List<String> filters = new ArrayList<>();
        if (from != null && to != null) {
            final LocalDateTime localFrom = Instant.ofEpochMilli(from.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            final LocalDateTime localTo = Instant.ofEpochMilli(to.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            tuple.addValue(localFrom);
            tuple.addValue(localTo);
            filters.add(String.format(" %s >= $1 AND %s < $2 ",getCreatedAtColumn(),getCreatedAtColumn()));
        } else if (from != null) {
            final LocalDateTime localFrom = Instant.ofEpochMilli(from.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            tuple.addValue(localFrom);
            filters.add(String.format(" %s >= $1 ",getCreatedAtColumn()));
        } else if (to != null) {
            final LocalDateTime localTo = Instant.ofEpochMilli(to.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            tuple.addValue(localTo);
            filters.add(String.format(" %s < $1 ",getCreatedAtColumn()));
        }
        if(isNotEmpty(request.getIds())) {
            filters.add("t." + getIdColumn() + " IN (" + String.join(",", request.getIds()) + ") ");
        }
        if(isNotEmpty(request.getStates())) {
            filters.add("t." + INGEST_JOB_STATE + " IN (" + String.join(",", request.getStates()) + ") ");
        }
        if(!filters.isEmpty()) {
            query.append(" WHERE ");
            query.append(String.join(" AND ", filters));
        }
        if(getShareTableName().isPresent()){
            query.append(" GROUP BY t.id ");
        }
        pgPool.queryStream(query.toString(),tuple, getBatchSize()).onSuccess(result -> {
            result.handler(row -> {
                final JsonObject json = PostgresClient.toJson(row);
                if(getShareTableName().isPresent()) {
                    SqlResult.parseSharedFromArray(json);
                }
                stream.add(json);
            })
            .endHandler(finish -> stream.end())
            .exceptionHandler(e->
                log.error("Failed to sqlSelect resources "+getTableName()+ "for reindex : ", e)
            );
        }).onFailure(e->{
            log.error("Failed to create sqlCursor resources "+getTableName()+ "for reindex : ", e);
        });
    }

    @Override
    protected Future<List<String>> doCreate(final UserInfos user, final List<JsonObject> sources, final boolean isCopy) {
        final Map<String, Object> map = new HashMap<>();
        for(final JsonObject source : sources){
            setCreatorForModel(user, source);
            setCreatedAtForModel(user, source);
            source.put(INGEST_JOB_STATE, IngestJobState.TO_BE_SENT);
        }
        final List<String> columnNames = new ArrayList<>(getColumns());
        columnNames.addAll(defaultColumns);
        final String inPlaceholder = PostgresClient.insertPlaceholders(sources, 1, columnNames);
        final Tuple inValues = PostgresClient.insertValuesWithDefault(sources, Tuple.tuple(), map, getMessageFields());
        final String queryTpl = "INSERT INTO %s(%s) VALUES %s returning id";
        final String columns = String.join(",", columnNames);
        final String query = String.format(queryTpl, getTableName(), columns, inPlaceholder);
        return pgPool.preparedQuery(query, inValues).map(result -> {
            final List<String> ids = new ArrayList<>();
            for (final Row row : result) {
                ids.add(row.getInteger(0) + "");
            }
            return ids;
        });
    }

    @Override
    protected Future<List<Boolean>> doDelete(final UserInfos user, final List<String> ids) {
        if(ids.isEmpty()){
            return Future.succeededFuture(new ArrayList<>());
        }
        final Set<Integer> safeIds = ids.stream().map(e->Integer.valueOf(e)).collect(Collectors.toSet());
        final String queryTpl = "DELETE FROM %s WHERE id IN (%s);";
        final String inPlaceholder = PostgresClient.inPlaceholder(ids, 1);
        final String query = String.format(queryTpl, getTableName(), inPlaceholder);
        final Tuple tuple = PostgresClient.inTuple(Tuple.tuple(), safeIds);
        return pgPool.preparedQuery(query, tuple).map(result -> {
            return ids.stream().map(e -> true).collect(Collectors.toList());
        });
    }

    public Future<List<JsonObject>> getByIds(final Set<String> ids) {
        if (ids.isEmpty()) {
            return Future.succeededFuture(new ArrayList<>());
        }
        final Set<Object> idParsed = ids.stream().map(e-> toSqlId(e)).collect(Collectors.toSet());
        final Tuple tuple = PostgresClient.inTuple(Tuple.tuple(), idParsed);
        final String queryTpl = "SELECT * FROM %s  WHERE id IN (%s) ";
        final String inPlaceholder = PostgresClient.inPlaceholder(idParsed, 1);
        final String query = String.format(queryTpl, getTableName(), inPlaceholder);
        return pgPool.preparedQuery(query, tuple).map(rows -> {
            final List<JsonObject> jsons = new ArrayList<>();
            for (final Row row : rows) {
                jsons.add(PostgresClient.toJson(row, rows));
            }
            return jsons;
        });
    }

    //overridable
    protected void setCreatorForModel(final UserInfos user, final JsonObject json){
        json.put(getCreatorIdColumn(), user.getUserId());
        json.put(getCreatorNameColumn(), user.getUsername());
    }
    protected void setCreatedAtForModel(final UserInfos user, final JsonObject json){
        json.put(getCreatedAtColumn(), new Date().getTime());
    }

    protected int getBatchSize() { return 50; }

    protected String getCreatedAtColumn() {
        return "created_at";
    }

    protected String getCreatorIdColumn() {
        return "creator_id";
    }

    protected String getCreatorNameColumn() {
        return "creator_name";
    }

    protected String getIdColumn() {
        return "id";
    }

    protected List<String> getMessageFields() {
        final List<String> columnNames = new ArrayList<>(getColumns());
        columnNames.addAll(defaultColumns);
        return columnNames;
    }

    protected Object toSqlId(final String id) {
        return id;
    }

    protected Optional<String> getShareTableName(){
        return Optional.of(getTableName()+"_shares");
    }
    //abstract
    protected abstract String getTableName();

    protected abstract List<String> getColumns();


    @Override
    public void setIngestJobState(final JsonObject source, final IngestJobState state) {
        super.setIngestJobState(source, state);
    }

    @Override
    public void setIngestJobStateAndVersion(final JsonObject source, final IngestJobState state, long version) {
        super.setIngestJobStateAndVersion(source, state, version);
    }

    @Override
    public Future<Void> onJobStateUpdatedMessageReceived(final List<IngestJobStateUpdateMessage> messages) {
        if(messages.isEmpty()){
            return Future.succeededFuture();
        }
        final String schema = getTableName();
        final List<Future> futures = new ArrayList<Future>();
        for(IngestJobStateUpdateMessage message : messages) {
            final String query = new StringBuilder()
                    .append(" UPDATE ").append(schema)
                    .append(" SET ingest_job_state = $1, version = $2 WHERE id = $3 AND version <= $2")
                    .toString();
            final Tuple tuple = Tuple.tuple();
            tuple.addValue(message.getState().name())
                    .addValue(message.getVersion())
                    .addValue(parseLong(message.getEntityId()));
            futures.add(pgPool.preparedQuery(query, tuple));
        }
        return CompositeFuture.all(futures).mapEmpty();
    }
}
