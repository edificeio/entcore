package org.entcore.common.mongodb;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

public class MongoClientFactory {
    public static Future<MongoClient> create(final Vertx vertx, final JsonObject config) throws Exception{
        if (config.getJsonObject("mongoConfig") != null) {
            final JsonObject mongoConfig = config.getJsonObject("mongoConfig");
            final MongoClient mongoClient = MongoClient.create(vertx, mongoConfig);
            return succeededFuture(mongoClient);
        }else{
          return vertx.sharedData().<String, String>getAsyncMap("server")
            .flatMap(map -> map.get("mongoConfig"))
            .flatMap(mongoConfig -> {
              final Future<MongoClient> future;
              if (mongoConfig != null) {
                final MongoClient mongoClient = MongoClient.create(vertx, new JsonObject(mongoConfig));
                future = succeededFuture(mongoClient);
              } else {
                future = failedFuture("Missing mongoConfig config");
              }
              return future;
            });
        }
    }
}
