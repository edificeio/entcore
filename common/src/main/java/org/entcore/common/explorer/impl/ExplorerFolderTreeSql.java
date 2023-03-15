package org.entcore.common.explorer.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Tuple;
import org.entcore.common.explorer.ExplorerStream;
import org.entcore.common.postgres.IPostgresClient;
import org.entcore.common.postgres.PostgresClient;
import org.entcore.common.postgres.PostgresClientPool;
import org.entcore.common.user.UserInfos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ExplorerFolderTreeSql extends ExplorerFolderTree{
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final IPostgresClient postgresClient;

    protected ExplorerFolderTreeSql(final ExplorerPlugin parent, final IPostgresClient postgresClient) {
        super(parent);
        this.postgresClient = postgresClient;
    }

    @Override
    protected String getFolderId(final JsonObject source) {
        return source.getValue("id").toString();
    }

    @Override
    protected String getName(final JsonObject source) {
        return source.getString("name");
    }

    @Override
    protected Optional<String> getParentId(final JsonObject source) {
        final Object parentId = source.getValue("parent_id");
        return Optional.ofNullable(parentId).map(e-> e.toString());
    }

    @Override
    protected boolean isTrashed(final JsonObject source) {
        return source.getBoolean("trashed", false);
    }

    @Override
    protected Set<String> getResourceIds(final JsonObject source) {
        final JsonArray ressourceIds = source.getJsonArray("ressource_ids", new JsonArray());
        return ressourceIds.stream().map(e-> e.toString()).collect(Collectors.toSet());
    }

    @Override
    protected UserInfos getCreatorForModel(final JsonObject json) {
        final String id = json.getString("owner");
        final String name = json.getString("owner_username");
        final UserInfos user = new UserInfos();
        user.setUserId(id);
        user.setUsername(name);
        return user;
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
    protected void doFetchForIndex(final ExplorerStream<JsonObject> stream, final Optional<Date> from, final Optional<Date> to) {
        final Tuple tuple = Tuple.tuple();
        final StringBuilder query = new StringBuilder();
        if(getUserTableName().isPresent()){
            if(getResourceTableName().isPresent()){
                query.append(String.format("SELECT f.*, u.username as owner_username, COALESCE(json_agg(s.id) FILTER (WHERE s.id IS NOT NULL), '[]') as ressource_ids FROM %s AS f ", getTableName()));
                query.append(String.format("LEFT JOIN %s u ON f.owner = u.id ", getUserTableName().get()));
                query.append(String.format("LEFT JOIN %s s ON f.id = s.folder_id ", getResourceTableName().get()));
            }else{
                query.append(String.format("SELECT f.*, u.username as owner_username FROM %s AS f ", getTableName()));
                query.append(String.format("LEFT JOIN %s u ON f.owner = u.id ", getUserTableName().get()));
            }
        }else{
            if(getResourceTableName().isPresent()){
                query.append(String.format("SELECT f.*, COALESCE(json_agg(s.id) FILTER (WHERE s.id IS NOT NULL), '[]') as ressource_ids FROM %s AS f ", getTableName()));
                query.append(String.format("LEFT JOIN %s s ON f.id = s.folder_id ", getResourceTableName().get()));
            }else{
                query.append(String.format("SELECT f.* FROM %s AS f ", getTableName()));
            }
        }
        //WHERE
        if (from.isPresent() && to.isPresent()) {
            final LocalDateTime localFrom = Instant.ofEpochMilli(from.get().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            final LocalDateTime localTo = Instant.ofEpochMilli(to.get().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            tuple.addValue(localFrom);
            tuple.addValue(localTo);
            query.append(String.format("WHERE f.%s >= $1 AND f.%s < $2 ",getCreatedAtColumn(),getCreatedAtColumn()));
        } else if (from.isPresent()) {
            final LocalDateTime localFrom = Instant.ofEpochMilli(from.get().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            tuple.addValue(localFrom);
            query.append(String.format("WHERE f.%s >= $1 ",getCreatedAtColumn()));
        } else if (to.isPresent()) {
            final LocalDateTime localTo = Instant.ofEpochMilli(to.get().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            tuple.addValue(localTo);
            query.append(String.format("WHERE f.%s < $1 ",getCreatedAtColumn()));
        }
        //GROUP BY
        if(getResourceTableName().isPresent()){
            if(getUserTableName().isPresent()){
                query.append(" GROUP BY f.id, u.username ");
            }else{
                query.append(" GROUP BY f.id ");
            }
        }
        postgresClient.queryStream(query.toString(),tuple, getBatchSize()).onSuccess(result -> {
            result.handler(row -> {
                stream.add(PostgresClient.toJson(row));
            }).endHandler(finish -> {
                stream.end();
            }).exceptionHandler(e->{
                log.error("Failed to sqlSelect folders "+getTableName()+ "for reindex : ", e);
            });
        }).onFailure(e->{
            log.error("Failed to create sqlCursor folders "+getTableName()+ "for reindex : ", e);
        });
    }

    protected int getBatchSize() { return 50; }

    protected String getCreatedAtColumn() { return "created"; }

    protected Object toPgDate(final LocalDateTime date) {
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }

    //abstract
    protected abstract String getTableName();
    protected abstract Optional<String> getUserTableName();
    protected abstract Optional<String> getResourceTableName();

}
