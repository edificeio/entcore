package org.entcore.common.cache;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.entcore.common.redis.Redis;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Optional;

public interface CacheService {

    static CacheService create(Vertx vertx){
        if(Redis.getClient() !=  null){
            return new RedisCacheService(Redis.getClient().getClient());
        } else{
            throw new IllegalStateException("CacheService.create : could not create cache because it is not initialized");
        }
    }

    static CacheService create(Vertx vertx, JsonObject config){
        if(Redis.getClient() !=  null){
            final Integer db = config.getInteger("redis-db");
            if(db != null){
                return new RedisCacheService(Redis.createClientForDb(vertx, db).getClient());
            }else{
                return new RedisCacheService(Redis.getClient().getClient());
            }
        } else{
            throw new IllegalStateException("CacheService.create : could not create cache because it is not initialized");
        }
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
