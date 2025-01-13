package org.entcore.common.email.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.user.UserUtils;

import java.util.*;

public interface PostgresEmailHelper {
    String MAILER_ADDRESS = "org.entcore.email";
    Logger logger = LoggerFactory.getLogger(PostgresEmailHelper.class);

    Future<Void> setRead(UUID uuid, JsonObject extraParams);

    default Future<Void> setRead(UUID uuid, EventBus bus, HttpServerRequest request, final JsonObject extraParams){
        if (request != null) {
            final Promise<Void> future = Promise.promise();
            final String ua = request.headers().get("User-Agent");
            if (ua != null) {
                extraParams.put("ua", ua);
            }
            UserUtils.getUserInfos(bus, request, user ->{
                if(user != null){
                    extraParams.put("user_id", user.getUserId());
                    if (user.getType() != null) {
                        extraParams.put("profile", user.getType());
                    }
                }
                setRead(uuid, extraParams).onComplete(future);
            });
            return future.future();
        } else {
            return setRead(uuid, extraParams);
        }
    }

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
