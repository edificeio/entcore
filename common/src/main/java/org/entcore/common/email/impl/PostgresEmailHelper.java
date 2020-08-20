package org.entcore.common.email.impl;

import io.netty.util.internal.StringUtil;
import io.reactiverse.pgclient.*;
import io.reactiverse.pgclient.data.Json;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

public class PostgresEmailHelper {
    static Logger log = LoggerFactory.getLogger(PostgresEmailHelper.class);
    final PgPool pool;
    final String tableName;
    final String attachementTableName;

    public PostgresEmailHelper(Vertx vertx, JsonObject pgConfig) {
        final PgPoolOptions options = new PgPoolOptions()
                .setPort(pgConfig.getInteger("port", 5432))
                .setHost(pgConfig.getString("host"))
                .setDatabase(pgConfig.getString("database"))
                .setUser(pgConfig.getString("user"))
                .setPassword(pgConfig.getString("password"))
                .setMaxSize(pgConfig.getInteger("pool-size", 5));
        this.pool = PgClient.pool(vertx, options);
        this.tableName = pgConfig.getString("tablename", "mail.mail_events");
        this.attachementTableName = pgConfig.getString("attachment-tablename", "mail.mail_attachments");
    }


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

    public Future<Void> createWithAttachments(MailBuilder mailB, List<MailAttachmentBuilder> attachmentsB) {
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
                for (final MailAttachmentBuilder attB : attachmentsB) {
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
            if(value instanceof  JsonObject || value instanceof JsonArray){
                tuple.addValue(Json.create(value));
            }else{
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

    public static class MailBuilder {
        private final Map<String, Object> mail = new HashMap<>();

        MailBuilder() {
            mail.put("id", UUID.randomUUID());
            mail.put("date", LocalDateTime.now());
        }

        public MailBuilder withProfile(String profile) {
            mail.put("profile", profile);
            return this;
        }

        public MailBuilder withModule(String module) {
            mail.put("module", module);
            return this;
        }

        public MailBuilder withPlatformId(String platform_id) {
            mail.put("platform_id", platform_id);
            return this;
        }

        public MailBuilder withPlatformUrl(String platform_url) {
            mail.put("platform_url", platform_url);
            return this;
        }

        public MailBuilder withUserId(String user_id) {
            mail.put("user_id", user_id);
            return this;
        }

        public MailBuilder withFrom(String from) {
            return this.withFrom(from, null);
        }

        public MailBuilder withFrom(String from, String name) {
            mail.put("from_mail", from);
            if (!StringUtil.isNullOrEmpty(name)) {
                mail.put("from_name", name);
            }
            return this;
        }

        public MailBuilder withTo(String toMail) {
            return this.withTo(toMail, null);
        }

        public MailBuilder withTo(String toMail, String toName) {
            mail.putIfAbsent("receivers", new JsonArray());
            final JsonObject json = new JsonObject().put("email", toMail);
            if (!StringUtil.isNullOrEmpty(toName)) {
                json.put("name", toName);
            }
            ((JsonArray) mail.get("receivers")).add(json);
            return this;
        }

        public MailBuilder withCc(String cc) {
            return this.withCc(cc, null);
        }

        public MailBuilder withCc(String cc, String ccName) {
            mail.putIfAbsent("cc", new JsonArray());
            final JsonObject json = new JsonObject().put("email", cc);
            if (!StringUtil.isNullOrEmpty(ccName)) {
                json.put("name", ccName);
            }
            ((JsonArray) mail.get("cc")).add(json);
            return this;
        }

        public MailBuilder withBcc(String bcc) {
            return this.withBcc(bcc, null);
        }

        public MailBuilder withBcc(String bcc, String bccName) {
            mail.putIfAbsent("bcc", new JsonArray());
            final JsonObject json = new JsonObject().put("email", bcc);
            if (!StringUtil.isNullOrEmpty(bccName)) {
                json.put("name", bccName);
            }
            ((JsonArray) mail.get("bcc")).add(json);
            return this;
        }

        public MailBuilder withSubject(String subject) {
            mail.put("subject", subject);
            return this;
        }

        public MailBuilder withHeader(String name, String value) {
            mail.putIfAbsent("headers", new JsonArray());
            return withHeader(new JsonObject().put("name", value).put("value", value));
        }

        public MailBuilder withHeader(JsonObject header) {
            mail.putIfAbsent("headers", new JsonArray());
            ((JsonArray) mail.get("headers")).add(header);
            return this;
        }

        public MailBuilder withBody(String body) {
            mail.put("body", body);
            return this;
        }

        public MailBuilder withPriority(int priority) {
            mail.put("priority", priority);
            return this;
        }

        public Map<String, Object> getMail() {
            return mail;
        }
    }

    public static MailBuilder mail() {
        return new MailBuilder();
    }

    public static class MailAttachmentBuilder {
        private final Map<String, Object> attachment = new HashMap<>();

        MailAttachmentBuilder(UUID id, LocalDateTime date) {
            attachment.put("mail_id", id);
            attachment.put("date", date);
            attachment.put("id", UUID.randomUUID());
        }

        public MailAttachmentBuilder withName(String name) {
            attachment.put("name", name);
            return this;
        }

        public MailAttachmentBuilder withEncodedContent(String content) {
            attachment.put("content", content);
            return this;
        }

        public Map<String, Object> getAttachment() {
            return attachment;
        }
    }

    public static MailAttachmentBuilder attachment(MailBuilder mail) {
        return new MailAttachmentBuilder((UUID) mail.getMail().get("id"), (LocalDateTime) mail.getMail().get("date"));
    }
}
