package org.entcore.common.cache;

import com.google.common.collect.Lists;
import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.RedisAPI;
import org.entcore.common.redis.Redis;
import org.entcore.common.user.UserInfos;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

public class RedisCacheService implements CacheService {
    final String GLOBAL_KEY = "global:";
    final String USER_KEY = "user:";
    final String LANG_KEY = "global:";
    final RedisAPI redis;

    public RedisCacheService(final RedisAPI redis) {
        this.redis = redis;
    }

    public RedisCacheService(Vertx vertx, JsonObject redisConfig) {
        if (redisConfig == null)
            throw new IllegalArgumentException("Could not create RedisCacheService because of missing redisConfig");
        Redis.getInstance().init(vertx, redisConfig);
        this.redis = Redis.getClient().getClient();

    }

    private void doSet(String key, String value, Integer ttl, Handler<AsyncResult<Void>> handler) {
        redis.set(newArrayList(key, value)).onComplete(res -> {
            if (res.succeeded()) {
                if (ttl != null && ttl > 0) {
                    redis.expire(newArrayList(key, String.valueOf(ttl))).onComplete(resTtl -> {
                        handler.handle(new DefaultAsyncResult<>(null));
                    });
                } else {
                    handler.handle(new DefaultAsyncResult<>(null));
                }
            } else {
                handler.handle(new DefaultAsyncResult<>(res.cause()));
            }
        });
    }

    private String globalKey(String key) {
        return GLOBAL_KEY + key;
    }

    private String langKey(String lang, String key) {
        return LANG_KEY + lang + ":" + key;
    }

    private String userKey(UserInfos user, String key) {
        return USER_KEY + user.getUserId() + ":" + key;
    }

    public void upsert(String key, String value, Integer ttl, Handler<AsyncResult<Void>> handler) {
        doSet(globalKey(key), value, ttl, handler);
    }

    public void upsertForUser(UserInfos user, String key, String value, Integer ttl, Handler<AsyncResult<Void>> handler) {
        doSet(userKey(user, key), value, ttl, handler);
    }

    public void upsertForLang(String lang, String key, String value, Integer ttl, Handler<AsyncResult<Void>> handler) {
        doSet(langKey(lang, key), value, ttl, handler);
    }

    private void doRemove(String key, Handler<AsyncResult<Void>> handler) {
        redis.del(newArrayList(key)).onComplete(res -> {
            if (res.succeeded()) {
                handler.handle(new DefaultAsyncResult<>(null));
            } else {
                handler.handle(new DefaultAsyncResult<>(res.cause()));
            }
        });
    }

    public void remove(String key, Handler<AsyncResult<Void>> handler) {
        doRemove(globalKey(key), handler);
    }

    public void removeForUser(UserInfos user, String key, Handler<AsyncResult<Void>> handler) {
        doRemove(userKey(user, key), handler);
    }

    public void removeForLang(String lang, String key, Handler<AsyncResult<Void>> handler) {
        doRemove(langKey(lang, key), handler);
    }

    private void doGet(String key, Handler<AsyncResult<Optional<String>>> handler) {
        redis.get(key).onComplete(ar -> {
            if (ar.succeeded()) {
                if (ar.result() != null) {
                    final String value = ar.result().toString(StandardCharsets.UTF_8);
                    handler.handle(new DefaultAsyncResult<>(Optional.ofNullable(value)));
                } else {
                    handler.handle(new DefaultAsyncResult<>(Optional.empty()));
                }
            } else {
                handler.handle(new DefaultAsyncResult<>(ar.cause()));
            }
        });
    }

    public void get(String key, Handler<AsyncResult<Optional<String>>> handler) {
        doGet(globalKey(key), handler);
    }

    public void getForUser(UserInfos user, String key, Handler<AsyncResult<Optional<String>>> handler) {
        doGet(userKey(user, key), handler);
    }

    public void getForLang(String lang, String key, Handler<AsyncResult<Optional<String>>> handler) {
        doGet(langKey(lang, key), handler);
    }

    public void getList(String key, Handler<AsyncResult<List<String>>> handler) {
        redis.lrange(key, "0", "-1").onComplete(resArray -> {
            handler.handle(resArray.map(jsonarray -> {
                final List<String> list = jsonarray.stream().map(a -> a.toString()).collect(Collectors.toList());
                return list;
            }));
        });
    }

    public void prependToList(String key, String value, Handler<AsyncResult<Long>> handler) {
        redis.lpush(newArrayList(key, value)).onComplete(res -> {
            handler.handle(new DefaultAsyncResult(res.succeeded() ? res.result() : res.cause()));
        });
    }

    public void removeLastFromList(String key, Handler<AsyncResult<String>> handler) {
        redis.rpop(Collections.singletonList(key)).onComplete(res -> {
            handler.handle(new DefaultAsyncResult(res.succeeded() ? res.result() : res.cause()));
        });
    }

    public void removeFromList(String key, String value, Handler<AsyncResult<Long>> handler){
        redis.lrem(key, "0", value).onComplete(res -> {
            handler.handle(new DefaultAsyncResult(res.succeeded() ? res.result() : res.cause()));
        });
    }

    public void getListLength(String key, Handler<AsyncResult<Long>> handler){
        redis.llen(key, (Handler) handler);
    }
}
