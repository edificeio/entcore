package org.entcore.common.email.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PostgresEmailHelperDefault implements PostgresEmailHelper {
    static Logger log = LoggerFactory.getLogger(org.entcore.common.email.impl.PostgresEmailHelperDefault.class);
    final PgPool pool;
    final String tableName;
    final String readTableName;
    final String attachementTableName;

    public PostgresEmailHelperDefault(Vertx vertx, JsonObject pgConfig) {
        final SslMode sslMode = SslMode.valueOf(pgConfig.getString("ssl-mode", "DISABLE"));
        final PgPoolOptions options = new PgPoolOptions()
                .setPort(pgConfig.getInteger("port", 5432))
                .setHost(pgConfig.getString("host"))
                .setDatabase(pgConfig.getString("database"))
                .setUser(pgConfig.getString("user"))
                .setPassword(pgConfig.getString("password"));
        final PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(pgConfig.getInteger("pool-size", 5));
        if (!SslMode.DISABLE.equals(sslMode)) {
            options
                .setSslMode(sslMode)
                .setTrustAll(SslMode.ALLOW.equals(sslMode) || SslMode.PREFER.equals(sslMode) || SslMode.REQUIRE.equals(sslMode));
        }
        this.pool = PgClient.pool(vertx, options);
        this.tableName = pgConfig.getString("tablename", "mail.mail_events");
        this.attachementTableName = pgConfig.getString("attachment-tablename", "mail.attachments_events");
        this.readTableName = pgConfig.getString("read-tablename", "mail.read_events");
    }

    private static LocalDateTime getDate(final JsonObject extraParams){
        try{
            if(extraParams.containsKey("date")){
                return LocalDateTime.parse(extraParams.getString("date"));
            } else {
                return LocalDateTime.now();
            }
        }catch(Exception e){
            return LocalDateTime.now();
        }
    }

    @Override
    public Future<Void> setRead(UUID mailId, final JsonObject extraParams) {
        final Future promise = Future.future();
        final StringBuilder query = new StringBuilder();
        final List<String> placeholders = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        final LocalDateTime date = getDate(extraParams);
        final LocalDateTime readAt = LocalDateTime.now();
        final Tuple tuple = Tuple.tuple().addValue(mailId).addValue(date).addValue(readAt);
        final String[] extraCols = {"user_id","ua","device_type"};
        for(final String col : extraCols){
            if(extraParams.containsKey(col)){
                names.add(col);
                placeholders.add("$"+(tuple.size()+1));
                tuple.addValue(extraParams.getValue(col));
            }
        }
        final String separator = placeholders.isEmpty()?" ":", ";
        query.append("INSERT INTO ").append(readTableName);
        query.append("(id, date, read_at").append(separator).append(String.join(",",names)).append(") ");
        query.append("VALUES ($1, $2, $3").append(separator).append(String.join(",",placeholders)).append(") RETURNING id");
        this.pool.preparedQuery(query.toString()).execute(tuple, r -> {
            if (r.succeeded()) {
                promise.complete();
            } else {
                promise.fail(r.cause());
            }
        });
        return promise;
    }

    public Future<Void> createWithAttachments(PostgresEmailBuilder.EmailBuilder mailB, List<PostgresEmailBuilder.AttachmentBuilder> attachmentsB) {
        final Future<Void> promise = Future.future();
        final Map<String, Object> mail = mailB.getMail();
        pool.begin(resTx -> {
            if (resTx.succeeded()) {
                final List<Future> futures = new ArrayList<>();
                futures.add(insert(resTx.result(), tableName, mail).setHandler(r -> {
                    if (r.failed()) {
                        log.error("Failed to create email: ", r.cause().getMessage());
                    }
                }));
                for (final PostgresEmailBuilder.AttachmentBuilder attB : attachmentsB) {
                    final Map<String, Object> att = attB.getAttachment();
                    att.put("mail_id", mail.get("id"));
                    futures.add(insert(resTx.result(), attachementTableName, att).setHandler(r -> {
                        if (r.failed()) {
                            log.error("Failed to create attachment: ", r.cause().getMessage());
                        }
                    }));
                }
                CompositeFuture.all(futures).setHandler(res -> {
                    if (res.succeeded()) {
                        resTx.result().commit(promise);
                    } else {
                        resTx.result().rollback();
                        promise.fail(res.cause());
                    }
                });
            } else {
                promise.fail(resTx.cause());
            }
        });
        return promise;
    }

    protected Future<Void> insert(Transaction transaction, String tableName, final Map<String, Object> params) {
        final Future<Void> future = Future.future();
        final StringBuilder query = new StringBuilder();
        final StringBuilder values = new StringBuilder();
        final Tuple tuple = Tuple.tuple();
        query.append("INSERT INTO ").append(tableName).append("(");
        //iterate fields
        String separator = "";
        int index = 1;
        if (params.keySet().isEmpty()) {
            future.fail("Should not be empty");
            return future;
        }
        for (final String key : params.keySet()) {
            query.append(separator).append(key);
            values.append(separator).append("$").append(index++);
            final Object value = params.get(key);
            if (value instanceof JsonObject || value instanceof JsonArray) {
                tuple.addValue((value));
            } else {
                tuple.addValue(value);
            }
            separator = ", ";
        }
        query.append(") VALUES (").append(values).append(") RETURNING id");
        transaction.preparedQuery(query.toString()).execute(tuple, r -> {
            if (r.succeeded()) {
                future.complete();
            } else {
                future.fail(r.cause());
            }
        });
        return future;
    }
}
