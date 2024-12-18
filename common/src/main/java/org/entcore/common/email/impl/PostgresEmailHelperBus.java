package org.entcore.common.email.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class PostgresEmailHelperBus implements PostgresEmailHelper {
    static Logger log = LoggerFactory.getLogger(org.entcore.common.email.impl.PostgresEmailHelperBus.class);
    final EventBus eventBus;
    public PostgresEmailHelperBus(Vertx vertx) {
        this.eventBus = vertx.eventBus();
    }

    @Override
    public Future<Void> setRead(UUID mailId, final JsonObject extraParams) {
        final Promise<Void> future = Promise.promise();
        final JsonObject payload = new JsonObject();
        for(String key : extraParams.fieldNames()){
            payload.put(key, extraParams.getValue(key));
        }
        payload.put("action", "setRead");
        payload.put("mailId", mailId.toString());
        this.eventBus.request(MAILER_ADDRESS, payload, res -> {
            if(res.failed()){
                future.fail(res.cause());
                log.error("Failed to setRead mail with remote worker: ", res.cause());
            } else {
                future.complete();
            }
        });
        return future.future();
    }
    @Override
    public Future<Void> createWithAttachments(PostgresEmailBuilder.EmailBuilder mailB, List<PostgresEmailBuilder.AttachmentBuilder> attachmentsB) {
        final Promise<Void> future = Promise.promise();
        final JsonObject payload = new JsonObject();
        final JsonArray attachments = new JsonArray();
        if(attachmentsB != null){
            for(PostgresEmailBuilder.AttachmentBuilder a : attachmentsB){
                attachments.add(a.toJsonObject());
            }
        }
        payload.put("action", "send");
        payload.put("mail", mailB.toJsonObject());
        payload.put("attachments", attachments);
        this.eventBus.request(MAILER_ADDRESS, payload, res -> {
           if(res.failed()){
               future.fail(res.cause());
               log.error("Failed to send mail with remote worker: ", res.cause());
           } else {
               future.complete();
           }
        });
        return future.future();
    }
}
