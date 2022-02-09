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

public class RedisTransaction extends RedisClient {
    private final List<Request> commands = new ArrayList<>();
    private final List<Handler<AsyncResult<Response>>> handlers = new ArrayList<>();

    RedisTransaction(final RedisClient client) {
        super(client.client, client.redisOptions);
    }

    public RedisTransaction begin() {
        handlers.clear();
        commands.clear();
        send(Request.cmd(Command.MULTI), e -> {
        });
        return this;
    }

    public Future<List<Response>> commit() {
        final Promise<List<Response>> promise = Promise.promise();
        send(Request.cmd(Command.EXEC), e -> {
        });
        client.batch(commands, promise.future());
        return promise.future().onSuccess(responses -> {
            for (int i = 0; i < responses.size(); i++) {
                handlers.get(i).handle(new DefaultAsyncResult<>(responses.get(i)));
            }
        });
    }

    public Future<Void> rollback() {
        final Promise<List<Response>> promise = Promise.promise();
        send(Request.cmd(Command.DISCARD), e -> {
        });
        client.batch(commands, promise.future());
        return promise.future().mapEmpty();
    }

    public RedisTransaction cancel() {
        for (final Handler<AsyncResult<Response>> h : handlers) {
            h.handle(new DefaultAsyncResult<>(new Exception("cancel")));
        }
        commands.clear();
        handlers.clear();
        return this;
    }

    @Override
    protected Redis send(final Request command, final Handler<AsyncResult<Response>> onSend) {
        commands.add(command);
        handlers.add(onSend);
        return super.client;
    }
}
