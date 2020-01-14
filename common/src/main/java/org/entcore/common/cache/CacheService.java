package org.entcore.common.cache;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.Optional;

public interface CacheService {

    void upsert(String key, String value, Integer ttl, Handler<AsyncResult<Void>> handler);

    void upsertForUser(UserInfos user, String key, String value, Integer ttl, Handler<AsyncResult<Void>> handler);

    void upsertForLang(String lang, String key, String value, Integer ttl, Handler<AsyncResult<Void>> handler);

    void remove(String key, Handler<AsyncResult<Void>> handler);

    void removeForUser(UserInfos user, String key, Handler<AsyncResult<Void>> handler);

    void removeForLang(String lang, String key, Handler<AsyncResult<Void>> handler);

    void get(String key, Handler<AsyncResult<Optional<String>>> handler);

    void getForUser(UserInfos user, String key, Handler<AsyncResult<Optional<String>>> handler);

    void getForLang(String lang, String key, Handler<AsyncResult<Optional<String>>> handler);
}
