package org.entcore.common.redis;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;

import java.util.ArrayList;
import java.util.List;

public class RedisBatch extends RedisClient {
    private final List<Request> commands = new ArrayList<>();
    private final List<Handler<AsyncResult<Response>>> handlers = new ArrayList<>();

    RedisBatch(final RedisClient client) {
        super(client.client, client.redisOptions);
    }

    public RedisBatch reset() {
        handlers.clear();
        commands.clear();
        return this;
    }

    /**
     * start a transaction in the middle of a batch
     *
     * @return
     */
    public Future<Void> beginTransaction() {
        final Promise<Response> promise = Promise.promise();
        send(Request.cmd(Command.MULTI), promise.future());
        return promise.future().mapEmpty();
    }

    public Future<Void> rollbackTransaction() {
        final Promise<Response> promise = Promise.promise();
        send(Request.cmd(Command.DISCARD), promise.future());
        return promise.future().mapEmpty();
    }

    public Future<Void> commitTransaction() {
        final Promise<Response> promise = Promise.promise();
        send(Request.cmd(Command.EXEC), promise.future());
        return promise.future().mapEmpty();
    }

    public Future<List<Response>> end() {
        final Promise<List<Response>> promise = Promise.promise();
        client.batch(commands, promise.future());
        return promise.future().onSuccess(responses -> {
            final List<Handler<AsyncResult<Response>>> tmpHandlers = new ArrayList<>();
            for (int i = 0; i < responses.size(); i++) {
                final Response response = responses.get(i);
                final String responseStr = response.toString();
                final Handler<AsyncResult<Response>> handler = handlers.get(i);
                if ("QUEUED".equals(responseStr)) {
                    tmpHandlers.add(handler);
                } else {
                    if (tmpHandlers.isEmpty()) {
                        handler.handle(new DefaultAsyncResult<>(response));
                    } else {
                        for (int j = 0; j < tmpHandlers.size(); j++) {
                            tmpHandlers.get(j).handle(new DefaultAsyncResult<>(response.get(j)));
                        }
                    }
                    tmpHandlers.clear();
                }
            }
        });
    }

    @Override
    protected Redis send(final Request command, final Handler<AsyncResult<Response>> onSend) {
        commands.add(command);
        handlers.add(onSend);
        return super.client;
    }
}
