package org.entcore.common.redis;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.*;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.impl.RedisAPIImpl;

import java.util.ArrayList;
import java.util.List;

public class RedisTransaction extends RedisClient {

    RedisTransaction(final RedisClient client) {
        super(client.client, client.redisOptions);
    }

    public RedisTransaction(RedisAPI redis, RedisOptions redisOptions) {
        super(redis, redisOptions);
    }

    public RedisTransaction begin() {
        final Promise<Response> promise = Promise.promise();
        this.client.multi(promise.future());
        return this;
    }

    public Future<List<Response>> commit() {
        final Promise<Response> promise = Promise.promise();
        this.client.exec(promise.future());
        return promise.future().map(response -> {
            final List<Response> responses = new ArrayList<>();
            for (int i = 0; i < response.size(); i++) {
                responses.add(response.get(i));
            }
            return responses;
        });
    }

    public Future<Void> rollback() {
        final Promise<Response> promise = Promise.promise();
        this.client.discard(promise.future());
        return promise.future().mapEmpty();
    }
}
