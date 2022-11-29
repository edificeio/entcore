package org.entcore.common.explorer.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.postgres.IPostgresClient;
import org.entcore.common.postgres.PostgresClient;
import org.entcore.common.postgres.PostgresClientPool;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

public abstract class ExplorerSubResourceSql extends ExplorerSubResource{
    protected final IPostgresClient postgresClient;

    protected ExplorerSubResourceSql(final ExplorerPlugin parent, final IPostgresClient postgresClient) {
        super(parent);
        this.postgresClient = postgresClient;
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
        if (from.isPresent() && to.isPresent()) {
            final LocalDateTime localFrom = Instant.ofEpochMilli(from.get().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            final LocalDateTime localTo = Instant.ofEpochMilli(to.get().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            tuple.addValue(localFrom);
            tuple.addValue(localTo);
            query.append(String.format("WHERE %s >= $1 AND %s < $2 ",getCreatedAtColumn(),getCreatedAtColumn()));
        } else if (from.isPresent()) {
            final LocalDateTime localFrom = Instant.ofEpochMilli(from.get().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            tuple.addValue(localFrom);
            query.append(String.format("WHERE %s >= $1 ",getCreatedAtColumn()));
        } else if (to.isPresent()) {
            final LocalDateTime localTo = Instant.ofEpochMilli(to.get().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            tuple.addValue(localTo);
            query.append(String.format("WHERE %s < $1 ",getCreatedAtColumn()));
        }
        if(getShareTableName().isPresent()){
            query.append(" GROUP BY t.id ");
        }
        postgresClient.queryStream(query.toString(),tuple, getBatchSize()).onSuccess(result -> {
            result.handler(row -> {
                final JsonObject json = PostgresClient.toJson(row);
                if(getShareTableName().isPresent()) {
                    SqlResult.parseSharedFromArray(json);
                }
                stream.add(json);
            }).endHandler(finish -> {
                stream.end();
            }).exceptionHandler(e->{
                log.error("Failed to sqlSelect subresources "+getTableName()+ "for reindex : ", e);
            });
        }).onFailure(e->{
            log.error("Failed to create sqlCursor subresources "+getTableName()+ "for reindex : ", e);
        });
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

    protected int getBatchSize() { return 50; }

    protected Object toPgDate(final LocalDateTime date) {
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    protected Optional<String> getShareTableName(){
        return Optional.empty();
    }

    //abstract
    protected abstract String getTableName();
}
