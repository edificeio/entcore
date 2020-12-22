package org.entcore.common.email.impl;

import io.reactiverse.pgclient.*;
import io.reactiverse.pgclient.data.Json;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PostgresEmailHelperDefault implements PostgresEmailHelper {
    static Logger log = LoggerFactory.getLogger(org.entcore.common.email.impl.PostgresEmailHelperDefault.class);
    final PgPool pool;
    final String tableName;
    final String attachementTableName;

    public PostgresEmailHelperDefault(Vertx vertx, JsonObject pgConfig) {
        final PgPoolOptions options = new PgPoolOptions()
                .setPort(pgConfig.getInteger("port", 5432))
                .setHost(pgConfig.getString("host"))
                .setDatabase(pgConfig.getString("database"))
                .setUser(pgConfig.getString("user"))
                .setPassword(pgConfig.getString("password"))
                .setMaxSize(pgConfig.getInteger("pool-size", 5));
        this.pool = PgClient.pool(vertx, options);
        this.tableName = pgConfig.getString("tablename", "mail.mail_events");
        this.attachementTableName = pgConfig.getString("attachment-tablename", "mail.attachments_events");
    }

    @Override
    public Future<Void> setRead(boolean read, UUID mailId) {
        final Future promise = Future.future();
        final String query = "UPDATE " + tableName + " SET read=$1 WHERE id = $2 ";
        this.pool.preparedQuery(query, Tuple.of(read, mailId), r -> {
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

    protected Future<Void> insert(PgTransaction transaction, String tableName, final Map<String, Object> params) {
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
                tuple.addValue(Json.create(value));
            } else {
                tuple.addValue(value);
            }
            separator = ", ";
        }
        query.append(") VALUES (").append(values).append(") RETURNING id");
        transaction.preparedQuery(query.toString(), tuple, r -> {
            if (r.succeeded()) {
                future.complete();
            } else {
                future.fail(r.cause());
            }
        });
        return future;
    }
}
