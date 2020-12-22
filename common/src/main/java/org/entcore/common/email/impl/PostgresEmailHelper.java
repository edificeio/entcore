package org.entcore.common.email.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.BaseServer;

import java.util.*;

public interface PostgresEmailHelper {
    String MAILER_ADDRESS = "org.entcore.email";
    Logger logger = LoggerFactory.getLogger(PostgresEmailHelper.class);

    Future<Void> setRead(boolean read, UUID uuid);

    Future<Void> createWithAttachments(PostgresEmailBuilder.EmailBuilder mailB, List<PostgresEmailBuilder.AttachmentBuilder> attachmentsB);

    static PostgresEmailHelper createDefault(final Vertx vertx, final JsonObject pgConfig){
        return new PostgresEmailHelperDefault(vertx, pgConfig);
    }

    static PostgresEmailHelper create(final Vertx vertx, final JsonObject pgConfig){
        if(vertx.getOrCreateContext().isWorkerContext()){
            //running multiple pgclient (stat...) on worker context cause issues (netty emit data to each inbound handler causing NPE)
            final String name = BaseServer.getModuleName();
            logger.info((name!=null ? name : "") + " init a bus mail sender");
            return new PostgresEmailHelperBus(vertx);
        }else{
            return new PostgresEmailHelperDefault(vertx, pgConfig);
        }
    }

}
