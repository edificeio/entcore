package org.entcore.common.explorer;

import io.reactiverse.pgclient.Row;
import io.reactiverse.pgclient.Tuple;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.postgres.PostgresClient;
import org.entcore.common.postgres.PostgresClientPool;
import org.entcore.common.user.UserInfos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ExplorerResourceCrudSql implements IExplorerResourceCrud {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final PostgresClientPool pgPool;

    public ExplorerResourceCrudSql(final PostgresClientPool pool) {
        this.pgPool = pool;
    }

    @Override
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

    @Override
    public void fetchByDate(final ExplorerStream<JsonObject> stream, final Optional<Date> from, final Optional<Date> to) {
        //TODO cursor and filter by date
        int i = 1;
        final Tuple tuple = Tuple.tuple();
        final StringBuilder query = new StringBuilder();
        query.append(String.format("SELECT * FROM %s ", getTableName()));
        if (from.isPresent() || to.isPresent()) {
            query.append("WHERE ");
            if (from.isPresent()) {
                query.append(String.format("%s >= $%s", getCreatedAtColumn(), i++));
                final LocalDateTime localFrom = Instant.ofEpochMilli(from.get().getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                tuple.addLocalDateTime(localFrom);
            }
            if (to.isPresent()) {
                query.append(String.format("%s < $%s", getCreatedAtColumn(), i++));
                final LocalDateTime localTo = Instant.ofEpochMilli(to.get().getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                tuple.addLocalDateTime(localTo);
            }
        }
        pgPool.preparedQuery(query.toString(), tuple).onSuccess(rows -> {
            final List<JsonObject> jsons = new ArrayList<>();
            for (final Row row : rows) {
                jsons.add(PostgresClient.toJson(row, rows));
            }
            stream.end(jsons);
        }).onFailure(e -> {
            stream.end();
            log.error("Failed to fetch folders for index: ", e.getCause());
        });
    }


    @Override
    public Future<List<String>> createAll(final UserInfos user, final List<JsonObject> sources) {
        final Map<String, Object> map = new HashMap<>();
        map.put(getCreatorIdColumn(), user.getUserId());
        map.put(getCreatorNameColumn(), user.getUsername());
        final String inPlaceholder = PostgresClient.insertPlaceholders(sources, 1, getColumns());
        final Tuple inValues = PostgresClient.insertValuesWithDefault(sources, Tuple.tuple(), map, getMessageFields());
        final String queryTpl = "INSERT INTO %s(%s) VALUES %s returning id";
        final String columns = String.join(",", getColumns());
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
    public Future<List<Boolean>> deleteById(final List<String> ids) {
        final Set<Integer> safeIds = ids.stream().map(e->Integer.valueOf(e)).collect(Collectors.toSet());
        final String queryTpl = "DELETE FROM %s WHERE id IN (%s);";
        final String inPlaceholder = PostgresClient.inPlaceholder(ids, 1);
        final String query = String.format(queryTpl, getTableName(), inPlaceholder);
        final Tuple tuple = PostgresClient.inTuple(Tuple.tuple(), safeIds);
        return pgPool.preparedQuery(query, tuple).map(result -> {
            return ids.stream().map(e -> true).collect(Collectors.toList());
        });
    }

    @Override
    public String getIdForModel(final JsonObject json) {
        return json.getValue(getIdColumn()).toString();
    }

    @Override
    public void setIdForModel(final JsonObject json, final String id) {
        json.put(getIdColumn(), Integer.valueOf(id));
    }

    @Override
    public UserInfos getCreatorForModel(final JsonObject json) {
        final String id = json.getString(getCreatorIdColumn());
        final String name = json.getString(getCreatorNameColumn());
        final UserInfos user = new UserInfos();
        user.setUserId(id);
        user.setUsername(name);
        return user;
    }

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

    protected abstract String getTableName();

    protected abstract List<String> getColumns();

    protected List<String> getMessageFields() {
        return getColumns();
    }

    protected Object toSqlId(final String id) {
        return id;
    }

}
