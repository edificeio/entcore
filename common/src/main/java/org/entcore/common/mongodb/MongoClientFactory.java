package org.entcore.common.mongodb;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class MongoClientFactory {
    public static MongoClient create(final Vertx vertx, final JsonObject config) throws Exception{
        if (config.getJsonObject("mongoConfig") != null) {
            final JsonObject mongoConfig = config.getJsonObject("mongoConfig");
            final MongoClient mongoClient = MongoClient.createShared(vertx, mongoConfig);
            return mongoClient;
        }else{
            final String mongoConfig = (String) vertx.sharedData().getLocalMap("server").get("mongoConfig");
            if(mongoConfig!=null){
                final MongoClient mongoClient = MongoClient.createShared(vertx, new JsonObject(mongoConfig));
                return mongoClient;
            }else{
                throw new Exception("Missing mongoConfig config");
            }
        }
    }
}
