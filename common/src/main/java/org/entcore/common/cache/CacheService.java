package org.entcore.common.cache;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Optional;

public interface CacheService {



    static CacheService create(Vertx vertx){
        final LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
        return createFromConfig(vertx, server);
    }

    static CacheService createFromConfig(Vertx vertx, final LocalMap<Object, Object> server){
        final String redisConfig = (String)server.get("redisConfig");
        return new RedisCacheService(vertx, new JsonObject(redisConfig));
    }

    void upsert(String key, String value, Integer ttl, Handler<AsyncResult<Void>> handler);

    void upsertForUser(UserInfos user, String key, String value, Integer ttl, Handler<AsyncResult<Void>> handler);

    void upsertForLang(String lang, String key, String value, Integer ttl, Handler<AsyncResult<Void>> handler);

    void remove(String key, Handler<AsyncResult<Void>> handler);

    void removeForUser(UserInfos user, String key, Handler<AsyncResult<Void>> handler);

    void removeForLang(String lang, String key, Handler<AsyncResult<Void>> handler);

    void get(String key, Handler<AsyncResult<Optional<String>>> handler);

    void getForUser(UserInfos user, String key, Handler<AsyncResult<Optional<String>>> handler);

    void getForLang(String lang, String key, Handler<AsyncResult<Optional<String>>> handler);

    void getListLength(String key, Handler<AsyncResult<Long>> handler);

    void getList(String key, Handler<AsyncResult<List<String>>> handler);

    void removeFromList(String key, String value, Handler<AsyncResult<Long>> handler);

    void prependToList(String key, String value, Handler<AsyncResult<Long>> handler);

    void removeLastFromList(String key, Handler<AsyncResult<String>> handler);
}
